package cn.iocoder.yudao.module.spkdelivery.framework.monitoring;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * SPK-OS IPD 监控指标（Cortext-IPD §11）
 * <p>
 * P1 采用内存计数器（零外部依赖，保证编译/运行稳定），经 {@code /spk/ipd/metrics} 端点暴露。
 * 后续如需 Prometheus，可在引入 micrometer-registry-prometheus 后适配为 MeterRegistry 写入，
 * 本类的 increment 调用点无需改动。
 *
 * @author SPK-OS
 */
@Slf4j
@Component
public class SpkIpdMetrics {

    public static final String M_ACTIVITY_RUN = "spk_ipd_activity_run_total";
    public static final String M_AGENT_TASK = "spk_ipd_agent_task_total";
    public static final String M_VERIFICATION = "spk_ipd_verification_total";
    public static final String M_ARTIFACT = "spk_ipd_artifact_total";
    public static final String M_DEAD_LETTER = "spk_ipd_dead_letter_total";

    /** 按 name#tagValue 维度的累加器 */
    private final Map<String, LongAdder> counters = new ConcurrentHashMap<>();

    @Resource
    private cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper contractMapper;

    public void incrementActivityRun(String outcome) {
        increment(M_ACTIVITY_RUN, outcome);
    }

    public void incrementAgentTask(String outcome) {
        increment(M_AGENT_TASK, outcome);
    }

    public void incrementVerification(String conclusion) {
        increment(M_VERIFICATION, conclusion);
    }

    public void incrementArtifact() {
        increment(M_ARTIFACT, "registered");
    }

    public void incrementDeadLetter() {
        increment(M_DEAD_LETTER, "failed");
    }

    /**
     * 当前积压（running 合同数）。
     */
    public long currentBacklog() {
        try {
            return contractMapper.selectListByStatus("running").size();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 快照全部计数器（供 metrics 端点）。
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        counters.forEach((k, v) -> m.put(k, v.sum()));
        m.put("task_contract_backlog", currentBacklog());
        return m;
    }

    private void increment(String name, String tagValue) {
        counters.computeIfAbsent(name + "#" + tagValue, k -> new LongAdder()).increment();
    }
}
