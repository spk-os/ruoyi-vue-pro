package cn.iocoder.yudao.module.spkdelivery.job;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkEvidenceTypeEnum;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkTaskContractStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.framework.monitoring.SpkIpdMetrics;
import cn.iocoder.yudao.module.spkdelivery.service.agent.SpkAgentTaskService;
import cn.iocoder.yudao.module.spkdelivery.service.evidence.SpkEvidenceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SPK-OS Agent 超时 Job（设计 §9.3 + §15.5.3）。
 * <p>
 * 周期扫描 {@code spk_task_contract} 中 {@code status='running'} 且
 * {@code started_at + timeout_seconds < now()} 的合同，标记 {@code timeout}，
 * 按 {@code retry_policy.max_attempts} 决定：
 * <ul>
 *   <li>{@code attempt_no < max_attempts} → 调 {@link SpkAgentTaskService#intervene} 的 rerun
 *       生成新合同（attempt_no+1 / fencing_token+1），落地新一轮派发。</li>
 *   <li>重试耗尽 → 标记 {@code failed} + 死信（{@link SpkIpdMetrics#incrementDeadLetter}）
 *       + 证据，等人工兜底（Cockpit「介入」按钮）。</li>
 * </ul>
 * <b>不碰 Flowable 运行时</b>：rerun 由 {@code route()} 内部铁律不 setVariables，
 * 避免触发器事务互锁（记忆 lsn_fef1d30eabb1b735）。
 *
 * <p>当前路由为同步模型（route 一次完成 queued→done），running 合同极少；本 Job 主要服务
 * D8 Omnigent SSE 异步模式（合同停留 running 直到 SSE 终态回调）的兜底。
 *
 * @author SPK-OS
 */
@Slf4j
@Component
public class SpkAgentTimeoutJob {

    /** 默认最大重试次数（retry_policy 缺失/解析失败时兜底）。 */
    @Value("${spk-delivery.job.timeout.default-max-attempts:3}")
    private int defaultMaxAttempts;

    @Resource
    private SpkTaskContractMapper contractMapper;
    @Resource
    private SpkAgentTaskService agentTaskService;
    @Resource
    private SpkIpdMetrics metrics;
    @Resource
    private SpkEvidenceService evidenceService;

    /**
     * 每 60s 扫描一次超时合同。fixedDelay：上一轮跑完后再计时，避免重叠。
     * initialDelay 30s：等容器其他 bean 就绪。
     */
    @Scheduled(fixedDelayString = "${spk-delivery.job.timeout.scan-interval-ms:60000}",
            initialDelayString = "${spk-delivery.job.timeout.initial-delay-ms:30000}")
    public void scanTimeout() {
        // scheduling 线程（Quartz/spring-scheduling）无租户上下文，TenantLineInnerInterceptor 会抛
        // "不存在租户编号"。SPK IPD 当前单租户 tenant_id=1（[[spk-ipd-activity-def-tenant-filter]]），
        // 显式 setTenantId(1L) 覆盖 selectListByStatus 及后续 intervene→route 整个调用栈（同线程 ThreadLocal 传递）。
        Long prevTenant = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(1L);
        try {
            doScanTimeout();
        } finally {
            if (prevTenant != null) {
                TenantContextHolder.setTenantId(prevTenant);
            } else {
                TenantContextHolder.clear();
            }
        }
    }

    private void doScanTimeout() {
        List<SpkTaskContractDO> running = contractMapper.selectListByStatus(
                SpkTaskContractStatusEnum.RUNNING.getLabel());
        if (running == null || running.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int scanned = 0;
        int retried = 0;
        int deadLettered = 0;
        for (SpkTaskContractDO c : running) {
            if (c.getStartedAt() == null || c.getTimeoutSeconds() == null) {
                continue;
            }
            LocalDateTime deadline = c.getStartedAt().plusSeconds(c.getTimeoutSeconds());
            if (deadline.isAfter(now)) {
                continue; // 未超时
            }
            scanned++;
            int attemptNo = c.getAttemptNo() == null ? 0 : c.getAttemptNo();
            int maxAttempts = parseMaxAttempts(c.getRetryPolicy());
            // 标记 timeout（先落事实，再决定重试/死信）
            c.setStatus(SpkTaskContractStatusEnum.TIMEOUT.getLabel());
            c.setFinishedAt(now);
            c.setFailureReason("执行超时：started=" + c.getStartedAt()
                    + " timeout=" + c.getTimeoutSeconds() + "s attempt=" + attemptNo);
            contractMapper.updateById(c);
            metrics.incrementActivityRun("timeout");
            if (attemptNo < maxAttempts) {
                try {
                    agentTaskService.intervene(c.getActivityRunId(), "rerun", "timeout-retry#" + (attemptNo + 1));
                    retried++;
                    log.info("[scanTimeout][activityRunId={} attempt={}/{} 已 rerun 重试]",
                            c.getActivityRunId(), attemptNo + 1, maxAttempts);
                } catch (Exception e) {
                    deadLettered++;
                    markFailedDeadLetter(c, now, "rerun 失败：" + truncate(e.getMessage(), 200));
                    log.error("[scanTimeout][activityRunId={} rerun 失败，转死信]", c.getActivityRunId(), e);
                }
            } else {
                deadLettered++;
                markFailedDeadLetter(c, now, "重试耗尽（attempt=" + attemptNo + "/" + maxAttempts + "）");
                log.warn("[scanTimeout][activityRunId={} 重试耗尽转死信]", c.getActivityRunId());
            }
        }
        if (scanned > 0) {
            log.info("[scanTimeout][扫描超时合同={} retried={} deadLettered={}]", scanned, retried, deadLettered);
        }
    }

    private void markFailedDeadLetter(SpkTaskContractDO c, LocalDateTime now, String reason) {
        c.setStatus(SpkTaskContractStatusEnum.FAILED.getLabel());
        c.setFinishedAt(now);
        c.setFailureReason(truncate(reason, 500));
        contractMapper.updateById(c);
        metrics.incrementDeadLetter();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activityRunId", c.getActivityRunId());
        payload.put("contractId", c.getContractId());
        payload.put("status", "dead-letter");
        payload.put("reason", reason);
        evidenceService.append(c.getActivityRunId(), c.getProcessInstanceId(),
                SpkEvidenceTypeEnum.RUN.getLabel(), c.getContractId(), payload);
    }

    /**
     * 解析 retry_policy JSON 的 max_attempts；缺失/异常回退默认值。
     * 约定 retry_policy 形如 {@code {"max_attempts":3}}（route 建合同时写入）。
     */
    private int parseMaxAttempts(String retryPolicy) {
        if (retryPolicy == null || retryPolicy.isBlank() || "{}".equals(retryPolicy.trim())) {
            return defaultMaxAttempts;
        }
        try {
            JsonNode node = JsonUtils.parseTree(retryPolicy);
            JsonNode max = node.get("max_attempts");
            if (max != null && max.canConvertToInt()) {
                int v = max.asInt();
                return v > 0 ? v : defaultMaxAttempts;
            }
        } catch (Exception e) {
            log.warn("[parseMaxAttempts][retry_policy 解析失败回退默认：{}]", retryPolicy);
        }
        return defaultMaxAttempts;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
