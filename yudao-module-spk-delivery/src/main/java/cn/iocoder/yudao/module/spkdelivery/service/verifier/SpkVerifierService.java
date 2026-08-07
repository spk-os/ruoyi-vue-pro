package cn.iocoder.yudao.module.spkdelivery.service.verifier;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkVerificationReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkVerificationReceiptMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkVerificationConclusionEnum;
import cn.iocoder.yudao.module.spkdelivery.service.agent.FrameworkAdapter;
import cn.iocoder.yudao.module.spkdelivery.service.agent.SpkAgentDispatchReq;
import cn.iocoder.yudao.module.spkdelivery.service.agent.SpkAgentDispatchResult;
import cn.iocoder.yudao.module.spkdelivery.service.artifact.SpkArtifactService;
import cn.iocoder.yudao.module.spkdelivery.service.evidence.SpkEvidenceService;
import cn.iocoder.yudao.module.spkdelivery.service.modelcapability.SpkModelCapabilityRegistry;
import tools.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.VERIFIER_NOT_EXISTS;

/**
 * SPK-OS Verifier Service —— Independent Verifier 验证
 * <p>
 * 物理隔离 P1 降级为逻辑隔离：独立 AiChatConversation（经 NativeAiAdapter 的 conversationCache
 * 按 instanceId#nodeKey#roleId 隔离）、独立 capability snapshot、独立 evidence 写入路径。
 * <p>
 * P1 验证方法桩：recheck/redteam/completeness/traceback 由 verifier agent 的 prompt 引导，
 * 结论从回包 JSON 解析。依 §5.7。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkVerifierService {

    @Resource
    private SpkVerificationReceiptMapper receiptMapper;
    @Resource
    private SpkArtifactService artifactService;
    @Resource
    private SpkEvidenceService evidenceService;
    @Resource
    private SpkModelCapabilityRegistry capabilityRegistry;
    @Resource(name = "native-ai")
    private FrameworkAdapter frameworkAdapter;

    /**
     * 触发对某产物的独立验证。
     *
     * @param verifier    Verifier agent 定义（agent_kind=verifier）
     * @param artifact    被验证产物
     * @param activityRunId ActivityRun id
     * @param processInstanceId 流程实例 id
     * @return 验证收据
     */
    public SpkVerificationReceiptDO verify(SpkAgentDefDO verifier, SpkArtifactManifestDO artifact,
                                           String activityRunId, String processInstanceId) {
        if (verifier == null || verifier.getRoleId() == null) {
            throw exception(VERIFIER_NOT_EXISTS);
        }
        // 1. 读产物正文
        String artifactContent = readArtifactContent(artifact);
        // 2. 冻结独立模型快照
        String snapshotId = capabilityRegistry.freezeSnapshot("verifier:" + verifier.getCode());
        // 3. 构造验证 prompt（四方法：recheck/redteam/completeness/traceback）
        String prompt = buildVerifyPrompt(verifier, artifact, artifactContent);
        SpkAgentDispatchReq req = new SpkAgentDispatchReq()
                .setRoleId(verifier.getRoleId())
                .setPrompt(prompt)
                .setInstanceId(processInstanceId)
                .setNodeKey("verify:" + artifact.getArtifactId());
        SpkAgentDispatchResult result = frameworkAdapter.dispatchTask(req);
        // 4. 解析结论
        ParsedVerdict verdict = parseVerdict(result.getResult());
        // 5. 写 verification receipt
        String receiptId = "vrf-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        SpkVerificationReceiptDO receipt = SpkVerificationReceiptDO.builder()
                .receiptId(receiptId)
                .artifactId(artifact.getArtifactId())
                .activityRunId(activityRunId)
                .verifierId(verifier.getId())
                .verifierCode(verifier.getCode())
                .verifierType(verifier.getVerifierType() != null ? verifier.getVerifierType() : "TR")
                .verificationMethod("recheck,redteam,completeness,traceback")
                .evidencePoints(verdict.pointsJson)
                .overallConclusion(verdict.conclusion)
                .modelSnapshotId(snapshotId)
                .summary(verdict.summary)
                .signedAt(LocalDateTime.now())
                .build();
        receiptMapper.insert(receipt);
        // 6. 写 evidence
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("receiptId", receiptId);
        payload.put("verifierCode", verifier.getCode());
        payload.put("verifierType", receipt.getVerifierType());
        payload.put("artifactId", artifact.getArtifactId());
        payload.put("conclusion", verdict.conclusion);
        payload.put("summary", verdict.summary);
        evidenceService.append(activityRunId, processInstanceId,
                cn.iocoder.yudao.module.spkdelivery.enums.SpkEvidenceTypeEnum.VERIFICATION.getLabel(),
                receiptId, payload);
        log.info("[verify][activityRunId={} artifactId={} verdict={} verifier={}]",
                activityRunId, artifact.getArtifactId(), verdict.conclusion, verifier.getCode());
        return receipt;
    }

    public List<SpkVerificationReceiptDO> listByActivityRunId(String activityRunId) {
        return receiptMapper.selectListByActivityRunId(activityRunId);
    }

    // ---------- 内部 ----------

    private String buildVerifyPrompt(SpkAgentDefDO verifier, SpkArtifactManifestDO artifact, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 Independent Verifier：").append(verifier.getName()).append("。\n");
        sb.append("请对以下产物执行独立验证（四方法）：recheck（复算关键结论）/ redteam（找反例）/ ");
        sb.append("completeness（查必填项缺失）/ traceback（追溯证据链）。\n\n");
        sb.append("产物类型：").append(artifact.getArtifactType()).append("\n");
        sb.append("产物摘要：").append(artifact.getSummary()).append("\n");
        sb.append("产物正文：\n").append(truncate(content, 8000)).append("\n\n");
        sb.append("请以 JSON 返回，格式：\n");
        sb.append("{\"overall\":\"PASS|CONDITIONAL|FAIL\",\"summary\":\"一句话结论\",");
        sb.append("\"evidencePoints\":[{\"point\":\"<检查点>\",\"verdict\":\"Confirmed|Challenged|Missing|Contradicted\"}]}");
        return sb.toString();
    }

    private ParsedVerdict parseVerdict(String result) {
        ParsedVerdict v = new ParsedVerdict();
        v.conclusion = SpkVerificationConclusionEnum.CONDITIONAL.getLabel();
        v.summary = "验证未返回结构化结论（P1 桩默认 CONDITIONAL）";
        v.pointsJson = "[]";
        if (result == null || result.isBlank()) {
            return v;
        }
        try {
            JsonNode node = JsonUtils.parseTree(result);
            JsonNode overall = node.get("overall");
            if (overall != null && !overall.isNull()) {
                SpkVerificationConclusionEnum c = SpkVerificationConclusionEnum.of(overall.asText());
                if (c != null) {
                    v.conclusion = c.getLabel();
                }
            }
            JsonNode summary = node.get("summary");
            if (summary != null && !summary.isNull()) {
                v.summary = summary.asText();
            }
            JsonNode points = node.get("evidencePoints");
            if (points != null && points.isArray()) {
                v.pointsJson = JsonUtils.toJsonString(points);
            }
        } catch (Exception e) {
            // 非 JSON：把整段当 summary
            v.summary = truncate(result, 500);
        }
        return v;
    }

    private String readArtifactContent(SpkArtifactManifestDO artifact) {
        // 优先用登记的 metadata（即产物正文），避免再读盘
        if (artifact.getMetadata() != null && !artifact.getMetadata().isBlank()) {
            return artifact.getMetadata();
        }
        return artifact.getSummary() != null ? artifact.getSummary() : "";
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static class ParsedVerdict {
        String conclusion;
        String summary;
        String pointsJson;
    }

}
