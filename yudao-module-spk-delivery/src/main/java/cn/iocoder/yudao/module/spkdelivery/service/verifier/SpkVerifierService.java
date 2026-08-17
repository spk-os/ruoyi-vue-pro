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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.VERIFIER_NOT_EXISTS;

/**
 * SPK-OS Verifier Service —— Independent Verifier 验证（三层校验）。
 * <p>
 * <b>Layer 1 结构校验</b>：用 {@code verifier-scripts/<schemaRef>/schema.json}（JSON Schema 子集，
 * {@link SpkJsonSchemaValidator}）校验产物 JSON 结构（必填字段/类型/枚举/pattern/数值范围/数组项数）。
 * schemaRef 取产物 {@code artifactType}（概念阶段二者同名）。
 * <p>
 * <b>Layer 2 验收标准</b>：用 {@code verifier-scripts/<schemaRef>/acceptance.json} 的 {@code assert}
 * 谓词（{@link SpkAcceptanceRuleEngine}）做确定性业务规则检查（来源可追溯/反例校验/置信度标注等），
 * 规则 severity=fail 违反即 FAIL，severity=conditional 违反即 CONDITIONAL。
 * <p>
 * <b>Layer 3 语义复核</b>：经 NativeAiAdapter 调 Independent Verifier agent 做 recheck/redteam/
 * completeness/traceback 四方法语义复核（fast-mode 走 PASS 桩；真实 LLM 重试 3 次，耗尽降级 ERROR）。
 * <p>
 * <b>裁决聚合</b>：取 Layer1/2 确定性结论与 Layer3 语义结论的严重度较高者（FAIL &gt; CONDITIONAL &gt; PASS）；
 * Layer3 PASS 不能推翻结构/验收 FAIL。schema/acceptance 文件缺失则该 artifactType 未配置三层校验，
 * 仅走 Layer3（向后兼容，与旧行为一致）。
 * <p>
 * 物理隔离 P1 降级为逻辑隔离：独立 AiChatConversation（经 NativeAiAdapter 的 conversationCache
 * 按 instanceId#nodeKey#roleId 隔离）、独立 capability snapshot、独立 evidence 写入路径。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkVerifierService {

    /**
     * verifier-scripts 根目录（schemaRef 子目录下放 schema.json/acceptance.json）。
     * 默认指向仓内 resources/verifier-scripts，与 spk-delivery.skill.root 同构。
     */
    @Value("${spk-delivery.verifier.scripts-root:/work/SPK-OS/soft/basic/ruoyi/resources/verifier-scripts}")
    private String verifierScriptsRoot;

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
     * 触发对某产物的独立验证（三层校验：结构 / 验收 / 语义）。
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
        // 0. 读产物正文 + 解析 schemaRef（= artifactType）；加载 schema/acceptance
        String artifactContent = readArtifactContent(artifact);
        String schemaRef = artifact.getArtifactType();
        JsonNode schema = loadScript(schemaRef, "schema.json");
        JsonNode acceptance = loadScript(schemaRef, "acceptance.json");
        boolean hasDeterministic = schema != null || acceptance != null;

        // Layer 1 结构校验
        List<String> structuralErrors = new ArrayList<>();
        JsonNode artifactJson = extractJsonObject(artifactContent);
        if (schema != null) {
            if (artifactJson != null) {
                structuralErrors = SpkJsonSchemaValidator.validate(schema, artifactJson);
            } else {
                structuralErrors.add("产物正文无法解析为 JSON 对象（结构校验前提失败）");
            }
        }
        // Layer 2 验收标准（仅结构可解析时执行）
        List<SpkAcceptanceRuleEngine.Result> ruleResults = new ArrayList<>();
        if (acceptance != null && artifactJson != null) {
            JsonNode rules = acceptance.path("rules");
            if (rules.isArray()) {
                for (JsonNode rule : rules) {
                    ruleResults.add(SpkAcceptanceRuleEngine.evaluate(rule, artifactJson));
                }
            }
        }

        // Layer 3 语义复核（LLM；NativeAiAdapter 已重试 transient，耗尽降级 ERROR）
        String snapshotId = capabilityRegistry.freezeSnapshot("verifier:" + verifier.getCode());
        String prompt = buildVerifyPrompt(verifier, artifact, artifactContent);
        SpkAgentDispatchReq req = new SpkAgentDispatchReq()
                .setRoleId(verifier.getRoleId())
                .setPrompt(prompt)
                .setInstanceId(processInstanceId)
                .setNodeKey("verify:" + artifact.getArtifactId());
        ParsedVerdict llm;
        try {
            SpkAgentDispatchResult result = frameworkAdapter.dispatchTask(req);
            llm = parseVerdict(result.getResult());
        } catch (Exception e) {
            log.error("[verify][activityRunId={} artifactId={} 验证器 LLM 调用失败（重试耗尽），"
                    + "降级 ERROR 推进，主产物已真实生成]", activityRunId, artifact.getArtifactId(), e);
            llm = new ParsedVerdict();
            llm.conclusion = "ERROR";
            llm.summary = "验证器 LLM 调用失败（已重试）：" + truncate(e.getMessage(), 200);
            llm.pointsJson = "[]";
        }

        // 裁决聚合：确定性（Layer1/2）与语义（Layer3）取较高严重度
        AggregatedVerdict av = aggregate(hasDeterministic, structuralErrors, ruleResults, llm);

        // 写 receipt
        String receiptId = "vrf-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        SpkVerificationReceiptDO receipt = SpkVerificationReceiptDO.builder()
                .receiptId(receiptId)
                .artifactId(artifact.getArtifactId())
                .activityRunId(activityRunId)
                .verifierId(verifier.getId())
                .verifierCode(verifier.getCode())
                .verifierType(verifier.getVerifierType() != null ? verifier.getVerifierType() : "TR")
                .verificationMethod("schema,acceptance,recheck,redteam,completeness,traceback")
                .evidencePoints(av.pointsJson)
                .overallConclusion(av.conclusion)
                .modelSnapshotId(snapshotId)
                .summary(av.summary)
                .signedAt(LocalDateTime.now())
                .build();
        receiptMapper.insert(receipt);

        // 写 evidence（含结构错误 + 验收规则明细，可还原三层校验全过程）
        List<Map<String, Object>> ruleEvidence = new ArrayList<>();
        for (SpkAcceptanceRuleEngine.Result rr : ruleResults) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rr.id);
            m.put("name", rr.name);
            m.put("severity", rr.severity);
            m.put("passed", rr.passed);
            m.put("reason", rr.reason);
            ruleEvidence.add(m);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("receiptId", receiptId);
        payload.put("verifierCode", verifier.getCode());
        payload.put("verifierType", receipt.getVerifierType());
        payload.put("artifactId", artifact.getArtifactId());
        payload.put("schemaRef", schemaRef);
        payload.put("conclusion", av.conclusion);
        payload.put("summary", av.summary);
        payload.put("structuralErrors", structuralErrors);
        payload.put("ruleResults", ruleEvidence);
        evidenceService.append(activityRunId, processInstanceId,
                cn.iocoder.yudao.module.spkdelivery.enums.SpkEvidenceTypeEnum.VERIFICATION.getLabel(),
                receiptId, payload);
        log.info("[verify][activityRunId={} artifactId={} schemaRef={} verdict={} verifier={}]",
                activityRunId, artifact.getArtifactId(), schemaRef, av.conclusion, verifier.getCode());
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

    // ---------- 三层校验 helper ----------

    /**
     * 裁决聚合：确定性结论（Layer1 结构 + Layer2 验收）与语义结论（Layer3 LLM）取较高严重度。
     * 严重度序：FAIL(3) &gt; ERROR(2) &gt; CONDITIONAL(1) &gt; PASS(0)。Layer3 PASS 不能推翻结构/验收 FAIL。
     */
    private AggregatedVerdict aggregate(boolean hasDeterministic, List<String> structuralErrors,
                                        List<SpkAcceptanceRuleEngine.Result> ruleResults, ParsedVerdict llm) {
        AggregatedVerdict av = new AggregatedVerdict();
        List<Map<String, Object>> points = new ArrayList<>();
        boolean anyFail = false, anyConditional = false;

        // Layer 1 结构校验
        if (hasDeterministic && (structuralErrors != null && !structuralErrors.isEmpty())) {
            anyFail = true;
            for (String e : structuralErrors) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("point", "结构校验");
                p.put("verdict", "Contradicted");
                p.put("detail", truncate(e, 300));
                points.add(p);
            }
        } else if (hasDeterministic) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("point", "结构校验");
            p.put("verdict", "Confirmed");
            points.add(p);
        }
        // Layer 2 验收规则
        for (SpkAcceptanceRuleEngine.Result rr : ruleResults) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("point", "验收:" + (rr.name != null ? rr.name : rr.id));
            if (!rr.passed) {
                if (rr.isFail()) anyFail = true; else anyConditional = true;
                p.put("verdict", rr.isFail() ? "Contradicted" : "Challenged");
                p.put("detail", truncate(rr.reason, 300));
            } else {
                p.put("verdict", "Confirmed");
            }
            points.add(p);
        }
        // Layer 3 语义复核 points 合并
        if (llm.pointsJson != null && !llm.pointsJson.isBlank()) {
            try {
                JsonNode lp = JsonUtils.parseTree(llm.pointsJson);
                if (lp.isArray()) {
                    for (JsonNode e : lp) {
                        Map<String, Object> p = new LinkedHashMap<>();
                        p.put("point", "语义:" + textOr(e, "point"));
                        p.put("verdict", textOr(e, "verdict"));
                        points.add(p);
                    }
                }
            } catch (Exception ignore) {
                // 非结构化 LLM points 跳过
            }
        }

        String det = anyFail ? "FAIL" : (anyConditional ? "CONDITIONAL" : "PASS");
        av.conclusion = maxSeverity(det, llm.conclusion);
        StringBuilder sb = new StringBuilder();
        if (anyFail) {
            sb.append("结构/验收存在 fail 级问题");
            if (structuralErrors != null && !structuralErrors.isEmpty()) {
                sb.append("（结构错误 ").append(structuralErrors.size()).append(" 条）");
            }
        } else if (anyConditional) {
            sb.append("验收存在 conditional 级问题");
        } else if (hasDeterministic) {
            sb.append("结构与验收确定性检查通过");
        }
        sb.append("；语义复核=").append(llm.conclusion);
        if (llm.summary != null && !llm.summary.isBlank()) {
            sb.append("；LLM：").append(truncate(llm.summary, 300));
        }
        av.summary = sb.toString();
        av.pointsJson = JsonUtils.toJsonString(points);
        return av;
    }

    private static String maxSeverity(String a, String b) {
        return rank(a) >= rank(b) ? a : b;
    }

    private static int rank(String c) {
        if ("FAIL".equalsIgnoreCase(c)) return 3;
        if ("ERROR".equalsIgnoreCase(c)) return 2;
        if ("CONDITIONAL".equalsIgnoreCase(c)) return 1;
        return 0; // PASS / 未知
    }

    /**
     * 从 verifier-scripts 根加载 schema.json / acceptance.json。文件缺失返回 null（该 artifactType
     * 未配置三层校验，仅走 Layer3 语义复核，向后兼容）。
     */
    private JsonNode loadScript(String schemaRef, String file) {
        if (schemaRef == null || schemaRef.isBlank()) {
            return null;
        }
        try {
            java.nio.file.Path p = java.nio.file.Path.of(verifierScriptsRoot, schemaRef, file);
            if (!java.nio.file.Files.exists(p)) {
                return null;
            }
            String content = java.nio.file.Files.readString(p, StandardCharsets.UTF_8);
            return JsonUtils.parseTree(content);
        } catch (Exception e) {
            log.warn("[loadScript][schemaRef={} file={} 读取失败降级：{}]", schemaRef, file, e.getMessage());
            return null;
        }
    }

    /**
     * 从产物正文（可能是裸 JSON / ```json 围栏 / 带散文）鲁棒抽取首个 JSON 对象。
     * 解析失败返回 null。供 Layer1/2 确定性校验使用。
     */
    private JsonNode extractJsonObject(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        JsonNode direct = tryParse(content);
        if (direct != null && direct.isObject()) {
            return direct;
        }
        // ```json 围栏
        int fs = content.indexOf("```json");
        if (fs >= 0) {
            int s = content.indexOf('\n', fs);
            int e = content.indexOf("```", s + 1);
            if (s > 0 && e > s) {
                JsonNode n = tryParse(content.substring(s + 1, e));
                if (n != null && n.isObject()) return n;
            }
        }
        // 任意 ``` 围栏
        int f2 = content.indexOf("```");
        if (f2 >= 0) {
            int nl = content.indexOf('\n', f2);
            int e2 = content.indexOf("```", nl + 1);
            if (nl > 0 && e2 > nl) {
                JsonNode n = tryParse(content.substring(nl + 1, e2));
                if (n != null && n.isObject()) return n;
            }
        }
        // 花括号配对（首个 { 到配对 }），处理裸 JSON 前后带散文
        int lb = content.indexOf('{');
        if (lb >= 0) {
            String sub = content.substring(lb);
            int end = braceMatch(sub);
            if (end > 0) {
                JsonNode n = tryParse(sub.substring(0, end + 1));
                if (n != null && n.isObject()) return n;
            }
        }
        return null;
    }

    private static int braceMatch(String s) {
        int depth = 0;
        boolean inStr = false, esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inStr) {
                if (esc) esc = false;
                else if (c == '\\') esc = true;
                else if (c == '"') inStr = false;
            } else {
                if (c == '"') inStr = true;
                else if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private JsonNode tryParse(String s) {
        try {
            return JsonUtils.parseTree(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String textOr(JsonNode node, String field) {
        JsonNode n = node == null ? null : node.path(field);
        return (n == null || n.isMissingNode() || n.isNull()) ? "" : n.asText();
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

    /** 三层校验聚合结论。 */
    private static class AggregatedVerdict {
        String conclusion;
        String summary;
        String pointsJson;
    }

}
