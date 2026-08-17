package cn.iocoder.yudao.module.spkdelivery.service.aegis;

import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.conversation.AiChatConversationCreateMyReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import cn.iocoder.yudao.module.ai.service.chat.AiChatConversationService;
import cn.iocoder.yudao.module.ai.service.chat.AiChatMessageService;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.aegis.SpkAegisReviewDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkVerificationReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.gate.SpkGateRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdactivity.SpkIpdActivityDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.aegis.SpkAegisReviewMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agent.SpkAgentTaskMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.gate.SpkGateRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdactivity.SpkIpdActivityDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.service.verifier.SpkVerifierService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPK-OS Aegis 审查服务
 * <p>
 * ADCP 前同步调用：聚合 G1-G6 门禁报告 + agent 产物 → 经 yudao-module-ai aegis-reviewer 角色
 * 做 LLM 裁决 → 落 spk_aegis_review。LLM 不可用时降级为人工经 /spk/aegis/review 出结论。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkAegisReviewService {

    @Value("${spk-delivery.aegis.reviewer-role-id:0}")
    private Long reviewerRoleId;
    @Value("${spk-delivery.agent.system-user-id:1}")
    private Long systemUserId;

    @Resource
    private SpkAegisReviewMapper aegisReviewMapper;
    @Resource
    private SpkGateRecordMapper gateRecordMapper;
    @Resource
    private SpkAgentTaskMapper agentTaskMapper;
    @Resource
    private AiChatMessageService chatMessageService;
    @Resource
    private AiChatConversationService chatConversationService;
    // 三层校验接入：FULL/INCR/ISSUE 流程的 TR 节点经 /spk/aegis/review 驱动，
    // 在此处按 activity def 的 useIndependentVerifier 标志对最近产物跑
    // SpkVerifierService.verify（Layer1 结构 + Layer2 验收 + Layer3 LLM 聚合），
    // 使完整 BPMN 流程真实触发三层校验（遗留风险#1 闭合）。
    @Resource
    private SpkVerifierService verifierService;
    @Resource
    private SpkTaskContractMapper taskContractMapper;
    @Resource
    private SpkArtifactManifestMapper artifactManifestMapper;
    @Resource
    private SpkAgentDefMapper agentDefMapper;
    @Resource
    private SpkIpdActivityDefMapper ipdActivityDefMapper;

    private final Map<String, Long> conversationCache = new ConcurrentHashMap<>();

    /**
     * 同步执行 Aegis 审查（由 SpkAegisReviewDelegate 或 /spk/aegis/review 调用）
     */
    public SpkAegisReviewDO review(String instanceId, String nodeKey) {
        // 1. 聚合证据：G1-G6 门禁报告 + agent 产物（旧路径 spk_agent_task；新路径产物经下方三层校验独立处理）
        List<SpkGateRecordDO> gates = gateRecordMapper.selectListByInstanceId(instanceId);
        List<SpkAgentTaskDO> tasks = agentTaskMapper.selectListByInstanceId(instanceId);
        StringBuilder evidence = new StringBuilder();
        evidence.append("## 门禁报告\n");
        gates.forEach(g -> evidence.append("- ").append(g.getGate()).append(": pass=").append(g.getPass())
                .append(" report=").append(g.getReport()).append('\n'));
        evidence.append("\n## Agent 产物\n");
        tasks.forEach(t -> evidence.append("- ").append(t.getNodeKey()).append(": ").append(t.getResult()).append('\n'));

        // 2. 三层独立核证：按最近 contract 的 activity def.useIndependentVerifier 标志触发
        //    SpkVerifierService.verify（Layer1 结构/schema + Layer2 验收/acceptance + Layer3 LLM 聚合）。
        //    FULL/INCR/ISSUE 流程的 TR 节点经 /spk/aegis/review 驱动 → 此处真实触发三层校验（遗留风险#1 闭合）。
        //    收据结论作为 TR 独立核证的主裁决，写入报告并驱动 verdict。
        VerifierOutcome outcome = runIndependentVerifier(instanceId);
        if (outcome != null) {
            evidence.append("\n## 三层独立核证（Layer1 结构 / Layer2 验收 / Layer3 语义）\n");
            evidence.append("- 产物类型：").append(outcome.artifactType).append('\n');
            evidence.append("- 收据ID：").append(outcome.receiptId).append('\n');
            evidence.append("- 结论：").append(outcome.conclusion).append('\n');
            evidence.append("- 摘要：").append(outcome.summary).append('\n');
            evidence.append("- 证据点：").append(outcome.pointsJson).append('\n');
        }

        // 3. LLM 裁决（reviewer 角色未配置则跳过，降级 conditional）
        String reviewerVerdict;
        String report;
        if (reviewerRoleId != null && reviewerRoleId > 0) {
            report = callReviewer(evidence.toString(), instanceId);
            reviewerVerdict = extractVerdict(report);
        } else {
            report = evidence.toString();
            reviewerVerdict = "conditional";
            log.warn("[review][未配置 aegis reviewer-role-id，降级 conditional verdict]");
        }

        // 4. verdict 聚合：三层收据结论为主裁决（若已触发），LLM reviewer 结论仅用于升级严重度。
        //    三层未触发时回落旧行为（reviewerVerdict）。
        String verdict;
        if (outcome != null) {
            String reviewerSide = (reviewerRoleId != null && reviewerRoleId > 0) ? reviewerVerdict : null;
            verdict = combineVerdict(outcome.verdictLabel, reviewerSide);
        } else {
            verdict = reviewerVerdict;
        }

        // 5. 落库
        SpkAegisReviewDO review = SpkAegisReviewDO.builder()
                .reviewId(UUID.randomUUID().toString())
                .instanceId(instanceId)
                .nodeKey(nodeKey)
                .verdict(verdict)
                .report(report)
                .evidence(evidence.toString())
                .build();
        aegisReviewMapper.insert(review);
        log.info("[review][instanceId={} verdict={} verifierTriggered={}]", instanceId, verdict, outcome != null);
        return review;
    }

    private String callReviewer(String evidence, String instanceId) {
        Long conversationId = conversationCache.computeIfAbsent(instanceId, k -> {
            AiChatConversationCreateMyReqVO vo = new AiChatConversationCreateMyReqVO();
            vo.setRoleId(reviewerRoleId);
            return chatConversationService.createChatConversationMy(vo, systemUserId);
        });
        AiChatMessageSendReqVO req = new AiChatMessageSendReqVO();
        req.setConversationId(conversationId);
        req.setContent("请对以下 IPD 阶段证据做 Aegis 审查，输出 verdict(pass/fail/conditional) 与报告：\n" + evidence);
        req.setUseContext(Boolean.TRUE);
        AiChatMessageSendRespVO resp = chatMessageService.sendMessage(req, systemUserId);
        return resp != null && resp.getReceive() != null ? resp.getReceive().getContent() : null;
    }

    private String extractVerdict(String report) {
        if (report == null) {
            return "conditional";
        }
        String lower = report.toLowerCase();
        if (lower.contains("pass") && !lower.contains("fail")) {
            return "pass";
        }
        if (lower.contains("fail")) {
            return "fail";
        }
        return "conditional";
    }

    /**
     * 人工复核（LLM 不可用时经 /spk/aegis/review 由人出结论）
     */
    public SpkAegisReviewDO manualReview(String instanceId, String nodeKey, String verdict, String report) {
        SpkAegisReviewDO review = SpkAegisReviewDO.builder()
                .reviewId(UUID.randomUUID().toString())
                .instanceId(instanceId)
                .nodeKey(nodeKey)
                .verdict(verdict)
                .report(report)
                .evidence("manual")
                .build();
        aegisReviewMapper.insert(review);
        return review;
    }

    /**
     * 按流程实例查询 Aegis 审查结论（用于详情页 IPD 产物 tab，取最近一条）
     */
    public SpkAegisReviewDO getByInstanceId(String instanceId) {
        return aegisReviewMapper.selectByInstanceId(instanceId);
    }

    // ==================== 三层独立核证接入（遗留风险#1） ====================

    /**
     * 对本实例最近一次 ActivityRun 的产物执行三层独立核证。
     * <p>触发条件：最近 contract 的 activity def {@code useIndependentVerifier=1}。
     * 镜像 {@code SpkVerifierDelegate} 的取数逻辑（最近 contract → 最近产物 → 按 verifierType 选 verifier），
     * 但 verifierType 取自 activity def（FULL/INCR/ISSUE 流程 TR 节点经 HTTP 触发器走 aegis review，
     * 无 Flowable 变量上下文，故从 def 派生而非读流程变量）。
     *
     * @return 三层收据结论；def 未开启独立核证 / 无产物 / 无 verifier 时返回 null（回落旧行为）
     */
    private VerifierOutcome runIndependentVerifier(String instanceId) {
        List<SpkTaskContractDO> contracts = taskContractMapper.selectListByProcessInstanceId(instanceId);
        if (contracts == null || contracts.isEmpty()) {
            log.debug("[runIndependentVerifier][无 contract 可核证 instanceId={}]", instanceId);
            return null;
        }
        SpkTaskContractDO contract = contracts.get(contracts.size() - 1);
        if (contract.getActivityRunId() == null) {
            return null;
        }
        SpkIpdActivityDefDO def = ipdActivityDefMapper.selectByActivityIdAndVersion(
                contract.getActivityId(), contract.getActivityVersion());
        if (def == null || def.getUseIndependentVerifier() == null || def.getUseIndependentVerifier() != 1) {
            log.debug("[runIndependentVerifier][activityId={} 未开启 useIndependentVerifier，跳过三层校验]",
                    contract.getActivityId());
            return null;
        }
        List<SpkArtifactManifestDO> arts = artifactManifestMapper.selectListByActivityRunId(contract.getActivityRunId());
        if (arts == null || arts.isEmpty()) {
            log.warn("[runIndependentVerifier][activityRunId={} 无产物可核证，跳过]", contract.getActivityRunId());
            return null;
        }
        SpkArtifactManifestDO artifact = arts.get(arts.size() - 1);
        SpkAgentDefDO verifier = pickVerifier(def.getVerifierType());
        if (verifier == null) {
            log.warn("[runIndependentVerifier][无可用 verifier verifierType={}，跳过]", def.getVerifierType());
            return null;
        }
        try {
            SpkVerificationReceiptDO receipt = verifierService.verify(verifier, artifact,
                    contract.getActivityRunId(), instanceId);
            VerifierOutcome o = new VerifierOutcome();
            o.receiptId = receipt.getReceiptId();
            o.artifactType = artifact.getArtifactType();
            o.conclusion = receipt.getOverallConclusion();
            o.verdictLabel = mapConclusionToVerdict(o.conclusion);
            o.summary = receipt.getSummary();
            o.pointsJson = receipt.getEvidencePoints();
            log.info("[runIndependentVerifier][instanceId={} activityRunId={} artifactType={} conclusion={}]",
                    instanceId, contract.getActivityRunId(), o.artifactType, o.conclusion);
            return o;
        } catch (Exception e) {
            // 三层校验失败不阻断流程，降级 conditional 并记日志（同 SpkVerifierDelegate 幂等降级语义）
            log.error("[runIndependentVerifier][instanceId={} activityRunId={} 三层校验异常，降级 conditional]",
                    instanceId, contract.getActivityRunId(), e);
            VerifierOutcome o = new VerifierOutcome();
            o.artifactType = artifact.getArtifactType();
            o.conclusion = "ERROR";
            o.verdictLabel = "conditional";
            o.summary = "三层校验异常降级 conditional："
                    + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            o.pointsJson = "[]";
            return o;
        }
    }

    /**
     * 按 verifierType（TR/SEC/RE/AUDIT）从 agent_def（agent_kind=verifier）选 verifier 角色；
     * 缺失回退任意 verifier。同 {@code SpkVerifierDelegate#pickVerifier}。
     */
    private SpkAgentDefDO pickVerifier(String verifierType) {
        List<SpkAgentDefDO> verifiers = agentDefMapper.selectListByAgentKind("verifier");
        if (verifiers == null || verifiers.isEmpty()) {
            return null;
        }
        if (verifierType != null && !verifierType.isBlank()) {
            String vt = verifierType.toUpperCase();
            for (SpkAgentDefDO v : verifiers) {
                if (vt.equals(v.getVerifierType())) {
                    return v;
                }
            }
        }
        return verifiers.get(0);
    }

    /** 三层收据结论（PASS/CONDITIONAL/FAIL/ERROR）→ aegis verdict（pass/conditional/fail）。 */
    private static String mapConclusionToVerdict(String conclusion) {
        if (conclusion == null) {
            return "conditional";
        }
        String c = conclusion.toUpperCase();
        if (c.contains("FAIL")) {
            return "fail";
        }
        // ERROR=验证器 LLM 不可用降级，确定性层未必 fail，按 conditional 待人复核
        if (c.contains("ERROR") || c.contains("CONDITIONAL")) {
            return "conditional";
        }
        if (c.contains("PASS")) {
            return "pass";
        }
        return "conditional";
    }

    /** 两 verdict 取较高严重度：fail &gt; conditional &gt; pass；null 视为最低。 */
    private static String combineVerdict(String a, String b) {
        return rankVerdict(a) >= rankVerdict(b) ? a : b;
    }

    private static int rankVerdict(String v) {
        if (v == null) {
            return 0;
        }
        if ("fail".equalsIgnoreCase(v)) {
            return 3;
        }
        if ("conditional".equalsIgnoreCase(v)) {
            return 2;
        }
        if ("pass".equalsIgnoreCase(v)) {
            return 1;
        }
        return 0;
    }

    /** 三层校验收据结论持有者。 */
    private static class VerifierOutcome {
        String receiptId;
        String artifactType;
        String conclusion;   // 原始 overallConclusion（PASS/CONDITIONAL/FAIL/ERROR）
        String verdictLabel;  // 映射后的 aegis verdict：pass/conditional/fail
        String summary;
        String pointsJson;
    }

}
