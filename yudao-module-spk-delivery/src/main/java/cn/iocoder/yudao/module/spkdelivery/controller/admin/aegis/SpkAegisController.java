package cn.iocoder.yudao.module.spkdelivery.controller.admin.aegis;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessTaskApi;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.aegis.SpkAegisReviewDO;
import cn.iocoder.yudao.module.spkdelivery.framework.flowable.listener.SpkAegisReviewDelegate;
import cn.iocoder.yudao.module.spkdelivery.service.aegis.SpkAegisReviewService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.TrimDecision;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdTrimRuleEvaluator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Aegis 审查 Controller
 * <p>
 * /review：BPM type2 HTTP_REQUEST 触发器同步调用。review 完成后异步 trigger 紧随的 receiveTask
 * 推进流程（复刻 dispatchActivityAsync 模式，避开同步回调内 setVariables/trigger 互锁，坑#4）。
 * /manual：LLM 不可用时人工出结论。
 *
 * @author SPK-OS
 */
@Slf4j
@Tag(name = "SPK Aegis 审查")
@RestController
@RequestMapping("/spk/aegis")
@Validated
public class SpkAegisController {

    @Resource
    private SpkAegisReviewService aegisReviewService;
    @Resource
    private SpkIpdTrimRuleEvaluator trimRuleEvaluator;
    @Resource
    private BpmProcessTaskApi processTaskApi;
    @Resource
    private RuntimeService runtimeService;

    @PostMapping("/review")
    @PermitAll
    @ApiAccessLog(operateModule = "SPK IPD", operateName = "Aegis异步审查")
    @Operation(summary = "Aegis 异步审查（BPM type2 HTTP_REQUEST 触发器调用，异步 review+setVariables+trigger 推进 receiveTask）")
    public CommonResult<Map<String, Object>> review(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId,
            @Parameter(description = "BPM 节点 key") @RequestParam(value = "nodeKey", required = false) String nodeKey) {
        // D3：前置裁剪规则评估（修 G6）。nodeKey 作为 activityDefId 匹配；SKIP 占位 PASS，但仍需异步 trigger 推进 receiveTask（否则流程卡死）。
        TrimDecision trim = trimRuleEvaluator.evaluate(processInstanceId, null, nodeKey);
        final boolean skip = trim.shouldSkip();
        final String skipReason = skip ? (trim.reason() == null ? "" : trim.reason()) : null;
        final Long matchedRuleId = trim.matchedRuleId();
        // 捕获主线程租户：supplyAsync 走 ForkJoinPool.commonPool 不传 TTL，丢租户致子线程写库失败（坑#12）。
        final Long tenantId = TenantContextHolder.getTenantId();
        final String pid = processInstanceId;
        final String nk = nodeKey;
        // fire-and-forget：立即返回，review+setVariables+trigger 在独立线程跑，避免 type2 触发器回调线程内
        // setVariables/trigger 与 start 线程互锁（坑#4 同步触发器死锁）。receiveTask 卡住流程等本后台回调推进。
        CompletableFuture.supplyAsync(() -> {
            Long prev = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
            } else {
                TenantContextHolder.clear();
            }
            try {
                String verdict;
                String report;
                if (skip) {
                    verdict = "PASS";
                    report = "裁剪规则 SKIP：" + skipReason;
                } else {
                    SpkAegisReviewDO r = aegisReviewService.review(pid, nk);
                    verdict = r.getVerdict();
                    report = r.getReport();
                }
                // 先解析卡住的 receiveTask key：type2 触发器生成的 receiveTask id 形如 Activity_<uuid>。
                // aegis review 快（未配 reviewer-role-id 时降级 conditional 无 LLM）→ 异步线程可能早于触发器
                // 事务提交，此时 receiveTask 行不可见 → 轮询等其落库可见（即触发器事务已提交、流程实例行锁
                // 释放），再 setVariables+trigger，避免与未提交事务互锁（坑#4；dispatchActivityAsync 靠 LLM
                // 慢自然避开此竞态，aegis 快则需显式轮询）。
                String rtKey = resolveAegisReceiveTaskKey(pid);
                if (rtKey != null) {
                    // 回写流程变量（与 SpkAegisReviewDelegate 同款 VAR_AEGIS_VERDICT/REPORT，单一变量名定义点）
                    Map<String, Object> vars = new HashMap<>();
                    vars.put(SpkAegisReviewDelegate.VAR_AEGIS_VERDICT, verdict);
                    vars.put(SpkAegisReviewDelegate.VAR_AEGIS_REPORT, report);
                    runtimeService.setVariables(pid, vars);
                    processTaskApi.triggerTask(pid, rtKey);
                    log.info("[aegis/review][异步推进 receiveTask={} pid={} nodeKey={} verdict={}]", rtKey, pid, nk, verdict);
                } else {
                    log.warn("[aegis/review][未找到 pending receiveTask，无法推进 pid={} nodeKey={}]", pid, nk);
                }
                return verdict;
            } catch (Exception e) {
                // review/trigger 失败：receiveTask 永久卡住，流程停滞，由 Cockpit「介入」(rerun/abort) 人工兜底（同 dispatchActivityAsync 失败语义）
                log.error("[aegis/review][异步推进失败 pid={} nodeKey={}]", pid, nk, e);
                return null;
            } finally {
                if (prev != null) {
                    TenantContextHolder.setTenantId(prev);
                } else {
                    TenantContextHolder.clear();
                }
            }
        });
        // 立即返回 PENDING 占位（review 在异步线程跑）；BPMN 未配 response 映射，变量由 setVariables 回写。
        Map<String, Object> data = new HashMap<>();
        data.put("aegisVerdict", "PENDING");
        data.put("aegisReport", "异步审查已派发，receiveTask 将由后台推进");
        if (skip) {
            data.put("skipped", true);
            data.put("matchedRuleId", matchedRuleId);
        }
        return success(data);
    }

    /**
     * 解析卡住的 receiveTask key：type2 HTTP 触发器生成的 receiveTask id 形如 "Activity_<uuid>"
     * （SimpleModelUtils:767）。aegis review 快时异步线程可能早于触发器事务提交，receiveTask 行不可见，
     * 故轮询（最多 10s）等其落库可见（即触发器事务已提交、流程实例行锁释放）。
     */
    private String resolveAegisReceiveTaskKey(String processInstanceId) {
        for (int i = 0; i < 100; i++) { // 最多等 10s
            List<String> activeIds = runtimeService.getActiveActivityIds(processInstanceId);
            if (activeIds != null) {
                String rt = activeIds.stream()
                        .filter(id -> id != null && id.startsWith("Activity_"))
                        .findFirst()
                        .orElse(null);
                if (rt != null) {
                    return rt;
                }
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // 兜底：最后一次查
        List<String> activeIds = runtimeService.getActiveActivityIds(processInstanceId);
        if (activeIds == null || activeIds.isEmpty()) {
            return null;
        }
        return activeIds.stream()
                .filter(id -> id != null && id.startsWith("Activity_"))
                .findFirst()
                .orElse(null);
    }

    @PostMapping("/manual")
    @PermitAll
    @Operation(summary = "Aegis 人工复核（LLM 不可用时出结论）")
    public CommonResult<Map<String, Object>> manual(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId,
            @Parameter(description = "BPM 节点 key") @RequestParam(value = "nodeKey", required = false) String nodeKey,
            @Parameter(description = "裁决 pass/fail/conditional") @RequestParam("verdict") String verdict,
            @Parameter(description = "审查报告") @RequestParam(value = "report", required = false) String report) {
        SpkAegisReviewDO review = aegisReviewService.manualReview(processInstanceId, nodeKey, verdict, report);
        Map<String, Object> data = new HashMap<>();
        data.put("aegisVerdict", review.getVerdict());
        data.put("aegisReport", review.getReport());
        return success(data);
    }

    @GetMapping("/get-by-instance")
    @Operation(summary = "按流程实例查询 Aegis 审查结论（IPD 产物 tab）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:aegis:query')")
    public CommonResult<SpkAegisReviewDO> getByInstance(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId) {
        return success(aegisReviewService.getByInstanceId(processInstanceId));
    }

}
