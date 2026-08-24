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
import cn.iocoder.yudao.module.spkdelivery.service.flowconfig.SpkSkillConfigService;
import cn.iocoder.yudao.module.spkdelivery.service.modelcapability.SpkModelCapabilityRegistry;
import cn.iocoder.yudao.module.spkdelivery.service.router.SpkCommercialDeliveryGuard;
import tools.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.flowable.engine.RuntimeService;
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
import static cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdBusinessConstants.VAR_PROJECT_ID;
import static cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdBusinessConstants.VAR_PROJECT_ROOT;
import static cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdBusinessConstants.VAR_VERSION_ID;

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
 * <b>Layer 3 语义复核</b>：经 Omnigent→Claude Code 调 Independent Verifier agent 做 recheck/redteam/
 * completeness/traceback 四方法语义复核；每次用独立 activityRunId 创建隔离会话，调用失败记 ERROR。
 * <p>
 * <b>裁决聚合</b>：取 Layer1/2 确定性结论与 Layer3 语义结论的严重度较高者（FAIL &gt; CONDITIONAL &gt; PASS）；
 * Layer3 PASS 不能推翻结构/验收 FAIL。schema/acceptance 文件缺失则该 artifactType 未配置三层校验，
 * 仅走 Layer3（向后兼容，与旧行为一致）。
 * <p>
 * 物理隔离 P1 降级为逻辑隔离：独立 Omnigent 会话、独立 capability snapshot、独立 evidence 写入路径。
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
    @Resource(name = "omnigent")
    private FrameworkAdapter frameworkAdapter;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private SpkSkillConfigService skillConfigService;

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
        // Router 已在 Activity 完成前执行一次 Independent Verifier；阶段 TR/Aegis 随后会对
        // 同一不可变产物再请求核证。对相同 artifact/run/verifier 的已签 PASS 收据直接复用，
        // 防止重复绑定同一个 Omnigent runner、重复消耗模型并产生互相矛盾的收据。
        // ERROR/CONDITIONAL/FAIL 不复用，后续自动返工的新产物仍会正常重新验证。
        SpkVerificationReceiptDO reusable = selectReusablePassReceipt(
                receiptMapper.selectByArtifactId(artifact.getArtifactId()), verifier.getId(), activityRunId);
        if (reusable != null) {
            log.info("[verify][复用已签 PASS 收据 receiptId={} activityRunId={} artifactId={} verifier={}]",
                    reusable.getReceiptId(), activityRunId, artifact.getArtifactId(), verifier.getCode());
            return reusable;
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

        // Layer 3 语义复核。Layer1/2 已经确定 FAIL 时不再启动昂贵的 Claude 会话：
        // 语义判断不能推翻确定性失败，重复调度只会延迟自动返工并制造噪声回执。
        String snapshotId;
        ParsedVerdict llm;
        if (shouldSkipSemantic(structuralErrors, ruleResults)) {
            snapshotId = "deterministic-fail";
            llm = new ParsedVerdict();
            llm.conclusion = "PASS";
            llm.summary = "Layer1/Layer2 已确定 FAIL，跳过不能改变裁决的语义复核";
            llm.pointsJson = "[]";
            log.info("[verify][activityRunId={} artifactId={} 确定性失败，跳过语义复核]",
                    activityRunId, artifact.getArtifactId());
        } else {
            snapshotId = capabilityRegistry.freezeSnapshot("verifier:" + verifier.getCode());
            String prompt = buildVerifyPrompt(verifier, artifact, artifactContent);
            String skillEnv = resolveSkillEnv(processInstanceId);
            String skillPath = resolveVerifierSkillPath(skillEnv);
            SpkCommercialDeliveryGuard.requireSkill(skillEnv, "spk-ipd-verifier", skillPath,
                    "verify:" + artifact.getArtifactId());
            SpkCommercialDeliveryGuard.requireOmnigentClaudeCodeRoute(skillEnv, frameworkAdapter.getName(),
                    verifier.getModel(), verifier.getRuntimeType(), verifier.getOmnigentAgentId(), verifier.getCode());
            SpkAgentDispatchReq req = new SpkAgentDispatchReq()
                    .setRoleId(verifier.getRoleId())
                    .setPrompt(prompt)
                    .setInstanceId(processInstanceId)
                    .setNodeKey("verify:" + artifact.getArtifactId())
                    .setLeadCode(verifier.getCode())
                    .setLeadDefId(verifier.getId())
                    .setActivityRunId(activityRunId + "-verify")
                    .setMode("omnigent")
                    .setOmnigentAgentId(verifier.getOmnigentAgentId())
                    .setModel(verifier.getModel())
                    .setSkillName("spk-ipd-verifier")
                    .setSkillPath(skillPath);
            inheritFlowProjectContext(req, processInstanceId, skillEnv);
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

    static SpkVerificationReceiptDO selectReusablePassReceipt(List<SpkVerificationReceiptDO> receipts,
                                                               Long verifierId, String activityRunId) {
        if (receipts == null || verifierId == null || activityRunId == null) {
            return null;
        }
        return receipts.stream()
                .filter(r -> verifierId.equals(r.getVerifierId()))
                .filter(r -> activityRunId.equals(r.getActivityRunId()))
                .filter(r -> "PASS".equalsIgnoreCase(r.getOverallConclusion()))
                .filter(r -> r.getSignedAt() != null)
                .max(java.util.Comparator.comparing(SpkVerificationReceiptDO::getSignedAt))
                .orElse(null);
    }

    public List<SpkVerificationReceiptDO> listByActivityRunId(String activityRunId) {
        return receiptMapper.selectListByActivityRunId(activityRunId);
    }

    private String resolveSkillEnv(String processInstanceId) {
        try {
            Object value = runtimeService.getVariable(processInstanceId, "spk_skill_env");
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        } catch (Exception e) {
            log.warn("[resolveSkillEnv][processInstanceId={} 读取失败，使用默认环境：{}]",
                    processInstanceId, truncate(e.getMessage(), 160));
        }
        return skillConfigService.getConfig().getDefaultEnv();
    }

    private String resolveVerifierSkillPath(String skillEnv) {
        Path path = Path.of(skillConfigService.getConfig().getSkillsRoot(), skillEnv,
                "spk-ipd-verifier", "SKILL.md");
        return Files.isRegularFile(path) ? path.toString() : null;
    }

    /**
     * Verifier 与被核证 Activity 必须共享 FlowRun 冻结的项目/版本工作区，但仍以独立
     * activityRunId 创建独立 Omnigent 会话。商用环境读取失败或变量缺失时 fail-closed，
     * 不允许 OmnigentAdapter 回退到历史全局 workspace。
     */
    SpkAgentDispatchReq inheritFlowProjectContext(SpkAgentDispatchReq req, String processInstanceId,
                                                   String skillEnv) {
        Long projectId = null;
        Long versionId = null;
        String projectRoot = null;
        try {
            projectId = longValue(runtimeService.getVariable(processInstanceId, VAR_PROJECT_ID));
            versionId = longValue(runtimeService.getVariable(processInstanceId, VAR_VERSION_ID));
            projectRoot = stringValue(runtimeService.getVariable(processInstanceId, VAR_PROJECT_ROOT));
        } catch (Exception e) {
            if (SpkCommercialDeliveryGuard.isCommercialRelease(skillEnv)) {
                throw new IllegalStateException("读取 commercial-release FlowRun 项目上下文失败: processInstanceId="
                        + processInstanceId, e);
            }
            log.warn("[inheritFlowProjectContext][processInstanceId={} 读取失败，非商用流程保留兼容回退：{}]",
                    processInstanceId, truncate(e.getMessage(), 160));
        }
        SpkCommercialDeliveryGuard.requireProjectExecutionContext(skillEnv, projectId, versionId,
                projectRoot, req.getNodeKey());
        return req.setProjectId(projectId).setVersionId(versionId).setWorkspace(projectRoot);
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.valueOf(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    // ---------- 内部 ----------

    private String buildVerifyPrompt(SpkAgentDefDO verifier, SpkArtifactManifestDO artifact, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 Independent Verifier：").append(verifier.getName()).append("。\n");
        sb.append("请对以下产物执行独立验证（四方法）：recheck（复算关键结论）/ redteam（找反例）/ ");
        sb.append("completeness（查必填项缺失）/ traceback（追溯证据链）。\n\n");
        sb.append("产物类型：").append(artifact.getArtifactType()).append("\n");
        sb.append("产物摘要：").append(artifact.getSummary()).append("\n");
        sb.append("权威产物 URI：").append(artifact.getUri() == null ? "(未登记，禁止猜测路径)" : artifact.getUri())
                .append("\n");
        sb.append("权威产物 SHA-256：").append(artifact.getContentHash()).append("\n");
        sb.append("必须读取上述唯一冻结文件并核对 SHA-256；只允许以该文件和 Cortex 冻结的项目上下文为权威，")
                .append("不得扫描、猜测或改用其他目录中的旧产物，也不得把摘要当作正文。\n");
        if ("project-plan".equals(artifact.getArtifactType())) {
            sb.append("计划阶段尚未进入开发。计划中声明的未来交付物、测试报告、ADR、代码、部署证据")
                    .append("不得因其当前不存在而判 FAIL；仅核验计划本身的完整性、可执行性与追溯关系。")
                    .append("权威来源证据仅限冻结项目上下文列出的设计文档、需求章节、原型与 API 文档，")
                    .append("不得用当前实现状态替代计划阶段验收。\n");
        }
        sb.append("\n");
        sb.append("请以 JSON 返回，格式：\n");
        sb.append("{\"overall\":\"PASS|CONDITIONAL|FAIL\",\"summary\":\"一句话结论\",");
        sb.append("\"evidencePoints\":[{\"point\":\"<检查点>\",\"verdict\":\"Confirmed|Challenged|Missing|Contradicted\"}]}");
        return sb.toString();
    }

    static boolean shouldSkipSemantic(List<String> structuralErrors,
                                      List<SpkAcceptanceRuleEngine.Result> ruleResults) {
        if (structuralErrors != null && !structuralErrors.isEmpty()) {
            return true;
        }
        if (ruleResults == null) {
            return false;
        }
        return ruleResults.stream().anyMatch(result -> result != null && !result.passed && result.isFail());
    }

    private ParsedVerdict parseVerdict(String result) {
        ParsedVerdict v = new ParsedVerdict();
        v.conclusion = SpkVerificationConclusionEnum.CONDITIONAL.getLabel();
        v.summary = "验证未返回结构化结论，默认 CONDITIONAL";
        v.pointsJson = "[]";
        if (result == null || result.isBlank()) {
            return v;
        }
        JsonNode node = extractJsonObject(result);
        if (node == null) {
            // Omnigent/Claude Code 没有返回任何可解析 JSON 时保持 fail-closed。
            v.summary = truncate(result, 500);
            return v;
        }
        JsonNode overall = node.get("overall");
        if (overall == null || overall.isNull()) {
            // 兼容已发布 verifier 的字段名；未知值仍保持 CONDITIONAL。
            overall = node.get("overallConclusion");
        }
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
        String trimmed = content.trim();
        if (trimmed.startsWith("{")) {
            JsonNode direct = tryParse(trimmed);
            if (direct != null && direct.isObject()) {
                return direct;
            }
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
