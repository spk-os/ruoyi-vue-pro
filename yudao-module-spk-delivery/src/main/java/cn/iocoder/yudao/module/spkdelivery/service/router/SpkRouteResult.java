package cn.iocoder.yudao.module.spkdelivery.service.router;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * SPK-OS Task Router 路由判决 + 执行结果
 * <p>
 * 由 {@link SpkTaskRouterService#route} 返回，供 dispatch 路径回写流程变量 agentResult、
 * 并把三件套 id 写回 spk_agent_task。
 *
 * @author SPK-OS
 */
@Data
@Accessors(chain = true)
public class SpkRouteResult {

    /** ActivityRun 实例 id */
    private String activityRunId;
    /** Task Contract id（UUID） */
    private String contractId;
    /** Lead Agent id */
    private Long leadAgentId;
    private String leadAgentCode;
    /** 模型快照 id */
    private String modelSnapshotId;
    /** ContextManifest URI */
    private String contextManifestUri;
    /** Agent 产物（写入流程变量 agentResult） */
    private String agentResult;
    /** 主产物 artifactId */
    private String artifactId;
    /** RunReceipt runId */
    private String runReceiptId;
    /** 模型 provider */
    private String provider;
    private String model;
    /** 验证结论 PASS/CONDITIONAL/FAIL/null（无验证则 null） */
    private String verificationConclusion;
    private String verifierCode;
    /** 独立核验摘要，供 Cortex 自动重试时原样反馈给下一次 Agent 执行。 */
    private String verificationSummary;
    /** 执行状态 done/failed */
    private String status;

}
