package cn.iocoder.yudao.module.spkdelivery.service.verifier;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.service.agent.SpkAgentDispatchReq;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkVerificationReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import org.flowable.engine.RuntimeService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SpkVerifierService 三层校验之 Layer 1（结构）/ Layer 2（验收）纯单元测试。
 * <p>
 * 加载仓内真实 {@code verifier-scripts/customer-need-brief/{schema,acceptance}.json}，
 * 对合法 / 非法 CustomerNeedBrief 样本求值，断言校验器真实工作（非桩）。
 * 不依赖 Spring / DB / LLM，可独立快速运行。
 *
 * @author SPK-OS
 */
class SpkVerifierLayerTest {

    private static final String SCRIPTS_ROOT = "/work/SPK-OS/soft/basic/ruoyi/resources/verifier-scripts";

    private JsonNode loadScript(String ref, String file) throws Exception {
        String content = Files.readString(Path.of(SCRIPTS_ROOT, ref, file), StandardCharsets.UTF_8);
        return JsonUtils.parseTree(content);
    }

    /** 合法 CustomerNeedBrief：高置信需求带反例、低置信需求 needs_review=true、样本声明齐。 */
    private static final String VALID = "{"
            + "\"need_brief_id\":\"cnb-20260817-001\","
            + "\"version\":\"1.0.0\","
            + "\"created_at\":\"2026-08-17T10:00:00Z\","
            + "\"created_by\":\"agent-market-insight-001\","
            + "\"statistics\":{\"total_needs\":2,\"high_priority\":1,\"counter_evidence_count\":1,\"needs_review_count\":1},"
            + "\"needs\":["
            + "  {\"need_id\":\"nd-001\",\"scenario\":\"语音控灯\",\"priority\":\"P0\","
            + "   \"customer_quote\":\"一句话关全屋灯\",\"customer_id_hash\":\"sha256:abc\","
            + "   \"counter_evidence\":{\"source\":\"工单#123\",\"summary\":\"同类诉求\"},"
            + "   \"confidence\":0.8,\"needs_review\":false,\"cluster_id\":\"cls-001\",\"summary\":\"语音\"},"
            + "  {\"need_id\":\"nd-002\",\"scenario\":\"离线\",\"priority\":\"P1\","
            + "   \"customer_quote\":\"断网本地控制\",\"customer_id_hash\":\"sha256:def\","
            + "   \"confidence\":0.5,\"needs_review\":true,\"cluster_id\":\"cls-001\",\"summary\":\"离线\"}"
            + "],"
            + "\"clusters\":[{\"cluster_id\":\"cls-001\",\"topic\":\"本地控制\",\"need_count\":2,\"trend\":\"rising\",\"representative_quote\":\"...\"}],"
            + "\"sample_limitation\":\"样本10户早期用户\""
            + "}";

    /** 非法：高置信需求(nd-001 confidence 0.8)缺 counter_evidence → 反例校验 fail。 */
    private static final String NO_COUNTER_EVIDENCE = "{"
            + "\"need_brief_id\":\"cnb-x\",\"version\":\"1.0.0\",\"created_at\":\"2026-08-17\",\"created_by\":\"a\","
            + "\"statistics\":{\"total_needs\":1},"
            + "\"needs\":[{\"need_id\":\"nd-001\",\"scenario\":\"x\",\"priority\":\"P0\","
            + "  \"customer_quote\":\"q\",\"customer_id_hash\":\"h\",\"confidence\":0.8,\"needs_review\":false}],"
            + "\"clusters\":[],\"sample_limitation\":\"无\""
            + "}";

    /** 结构非法：缺必填 needs 数组。 */
    private static final String STRUCTURALLY_INVALID = "{"
            + "\"need_brief_id\":\"cnb-y\",\"version\":\"1.0.0\",\"created_at\":\"2026-08-17\",\"created_by\":\"a\""
            + "}";

    @Test
    void layer1_validArtifact_hasNoStructuralErrors() throws Exception {
        JsonNode schema = loadScript("customer-need-brief", "schema.json");
        JsonNode instance = JsonUtils.parseTree(VALID);
        List<String> errors = SpkJsonSchemaValidator.validate(schema, instance);
        assertTrue(errors.isEmpty(), "合法产物不应有结构错误，实际：" + errors);
    }

    @Test
    void layer1_missingRequired_hasStructuralErrors() throws Exception {
        JsonNode schema = loadScript("customer-need-brief", "schema.json");
        JsonNode instance = JsonUtils.parseTree(STRUCTURALLY_INVALID);
        List<String> errors = SpkJsonSchemaValidator.validate(schema, instance);
        assertFalse(errors.isEmpty(), "缺必填字段须报结构错误");
        assertTrue(errors.stream().anyMatch(e -> e.contains("needs")), "须指出 needs 缺失");
    }

    @Test
    void layer1_projectPlanRejectsSummaryOnlyRequirementInsteadOfExecutableSpecs() throws Exception {
        JsonNode schema = loadScript("project-plan", "schema.json");
        JsonNode instance = JsonUtils.parseTree("""
                {
                  "plan_id":"plan-1","version":"1.0","created_at":"2026-08-22","created_by":"agent",
                  "requirements":[{
                    "requirementId":"REQ-01","name":"执行层重建","description":"摘要","level":"P0",
                    "test_specification":"单行测试摘要","completion_definition":"完成"
                  }],
                  "specifications":{"architecture":"单行架构摘要"},
                  "rtm":[],"wbs":[],"milestones":[],"resource_matrix":[],"critical_path":[],
                  "risk_register":[],"environment":{},
                  "plane_sync":{"status":"planned","gitea_project":"org/project"},
                  "tr2_pdcp_criteria":{"entry":[],"exit":[],"fail_conditions":[]},
                  "sources":[],"evidence":[]
                }
                """);

        List<String> errors = SpkJsonSchemaValidator.validate(schema, instance);

        assertTrue(errors.stream().anyMatch(e -> e.contains("/requirements/0/objective")),
                "每条需求必须有目标，不能只给摘要");
        assertTrue(errors.stream().anyMatch(e -> e.contains("/requirements/0/acceptance_criteria")),
                "每条需求必须有结构化验收标准");
        assertTrue(errors.stream().anyMatch(e -> e.contains("/requirements/0/development_specification")),
                "每条需求必须有可执行开发规格");
        assertTrue(errors.stream().anyMatch(e -> e.contains("/requirements/0/test_specification")
                        && e.contains("类型应为 object")),
                "测试规格必须是结构化用例而不是单行字符串");
        assertTrue(errors.stream().anyMatch(e -> e.contains("/specifications/architecture")
                        && e.contains("类型应为 object")),
                "阶段规格必须是结构化对象而不是单行字符串");
        assertTrue(errors.stream().anyMatch(e -> e.contains("/plane_sync/issue_mappings")),
                "Plane 同步必须保留 REQ 到 issue 的映射");
    }

    @Test
    void layer2_validArtifact_passesFailSeverityRules() throws Exception {
        JsonNode acceptance = loadScript("customer-need-brief", "acceptance.json");
        JsonNode instance = JsonUtils.parseTree(VALID);
        JsonNode rules = acceptance.path("rules");
        boolean anyFailRuleViolated = false;
        for (JsonNode rule : rules) {
            SpkAcceptanceRuleEngine.Result r = SpkAcceptanceRuleEngine.evaluate(rule, instance);
            if (!r.passed && r.isFail()) {
                anyFailRuleViolated = true;
            }
        }
        assertFalse(anyFailRuleViolated, "合法产物所有 fail 级验收规则应通过");
    }

    @Test
    void layer2_highConfidenceWithoutCounterEvidence_triggersFailRule() throws Exception {
        JsonNode acceptance = loadScript("customer-need-brief", "acceptance.json");
        JsonNode instance = JsonUtils.parseTree(NO_COUNTER_EVIDENCE);
        JsonNode rules = acceptance.path("rules");
        boolean counterEvidenceRuleFailed = false;
        for (JsonNode rule : rules) {
            SpkAcceptanceRuleEngine.Result r = SpkAcceptanceRuleEngine.evaluate(rule, instance);
            if ("counter_evidence".equals(r.id) && !r.passed && r.isFail()) {
                counterEvidenceRuleFailed = true;
            }
        }
        assertTrue(counterEvidenceRuleFailed, "高置信无反例须触发 counter_evidence fail 规则");
    }

    @Test
    void layer2_acceptanceDoesNotRequireHumanSignoffBeforeDcp() throws Exception {
        // Agent/Verifier 只提交待审产物；signed_by 由后续 DCP 人工审批生成，不能成为前置验收条件。
        JsonNode acceptance = loadScript("customer-need-brief", "acceptance.json");
        JsonNode rules = acceptance.path("rules");
        boolean containsPrematureSignoffRule = false;
        for (JsonNode rule : rules) {
            if ("signoff_complete".equals(rule.path("id").asText())) {
                containsPrematureSignoffRule = true;
            }
        }
        assertFalse(containsPrematureSignoffRule, "DCP 前的验收规则不得要求人工签署");
    }

    @Test
    void layer3_narrativeWrappedJson_preservesPassVerdict() throws Exception {
        Object verdict = parseVerdict("所有关键证据声明已通过独立源码复算验证。\n\n"
                + "{\"overall\":\"PASS\",\"summary\":\"证据链完整\","
                + "\"evidencePoints\":[{\"point\":\"REQ-01\",\"verdict\":\"Confirmed\"}]}");

        assertEquals("PASS", field(verdict, "conclusion"));
        assertEquals("证据链完整", field(verdict, "summary"));
        assertTrue(field(verdict, "pointsJson").contains("REQ-01"));
    }

    @Test
    void layer3_nonJson_keepsFailClosedConditional() throws Exception {
        Object verdict = parseVerdict("验证通过，但没有返回约定 JSON");

        assertEquals("CONDITIONAL", field(verdict, "conclusion"));
    }

    @Test
    void verifierInheritsFrozenCommercialFlowProjectContext() throws Exception {
        RuntimeService runtimeService = mock(RuntimeService.class);
        when(runtimeService.getVariable("process-1", "projectId")).thenReturn(40L);
        when(runtimeService.getVariable("process-1", "versionId")).thenReturn("38");
        when(runtimeService.getVariable("process-1", "projectRoot"))
                .thenReturn("/work/SPK-OS/dev/spk-infomation");
        SpkVerifierService service = serviceWithRuntime(runtimeService);

        SpkAgentDispatchReq req = service.inheritFlowProjectContext(
                new SpkAgentDispatchReq().setNodeKey("verify:artifact-1"),
                "process-1", "commercial-release");

        assertEquals(40L, req.getProjectId());
        assertEquals(38L, req.getVersionId());
        assertEquals("/work/SPK-OS/dev/spk-infomation", req.getWorkspace());
    }

    @Test
    void verifierRejectsMissingCommercialFlowProjectContext() throws Exception {
        SpkVerifierService service = serviceWithRuntime(mock(RuntimeService.class));

        assertThrows(IllegalStateException.class, () -> service.inheritFlowProjectContext(
                new SpkAgentDispatchReq().setNodeKey("verify:artifact-1"),
                "process-1", "commercial-release"));
    }

    @Test
    void reusesOnlySignedPassReceiptForSameArtifactRunAndVerifier() {
        SpkVerificationReceiptDO pass = SpkVerificationReceiptDO.builder()
                .receiptId("vrf-pass").verifierId(15L).activityRunId("run-1")
                .overallConclusion("PASS").signedAt(LocalDateTime.now()).build();
        SpkVerificationReceiptDO error = SpkVerificationReceiptDO.builder()
                .receiptId("vrf-error").verifierId(15L).activityRunId("run-1")
                .overallConclusion("ERROR").signedAt(LocalDateTime.now().plusSeconds(1)).build();

        assertEquals("vrf-pass", SpkVerifierService.selectReusablePassReceipt(
                List.of(pass, error), 15L, "run-1").getReceiptId());
        assertEquals(null, SpkVerifierService.selectReusablePassReceipt(
                List.of(pass), 15L, "run-2"));
    }

    @Test
    void deterministicFailSkipsSlowSemanticAgent() {
        SpkAcceptanceRuleEngine.Result failedRule = new SpkAcceptanceRuleEngine.Result();
        failedRule.severity = "fail";
        failedRule.passed = false;

        assertTrue(SpkVerifierService.shouldSkipSemantic(
                List.of("/plan_id 缺少必填字段"), List.of()));
        assertTrue(SpkVerifierService.shouldSkipSemantic(List.of(), List.of(failedRule)));
        assertFalse(SpkVerifierService.shouldSkipSemantic(List.of(), List.of()));
    }

    @Test
    void projectPlanVerifierUnderstandsFutureDeliverablesAreNotSourceEvidence() throws Exception {
        SpkVerifierService service = new SpkVerifierService();
        Method method = SpkVerifierService.class.getDeclaredMethod("buildVerifyPrompt",
                SpkAgentDefDO.class, SpkArtifactManifestDO.class, String.class);
        method.setAccessible(true);
        String prompt = (String) method.invoke(service,
                SpkAgentDefDO.builder().name("独立验证员").build(),
                SpkArtifactManifestDO.builder().artifactType("project-plan").summary("计划").build(),
                "{\"plan_id\":\"plan-1\"}");

        assertTrue(prompt.contains("计划阶段尚未进入开发"));
        assertTrue(prompt.contains("未来交付物"));
        assertTrue(prompt.contains("不得因其当前不存在而判 FAIL"));
        assertTrue(prompt.contains("权威来源证据"));
    }

    private static Object parseVerdict(String result) throws Exception {
        SpkVerifierService service = new SpkVerifierService();
        Method method = SpkVerifierService.class.getDeclaredMethod("parseVerdict", String.class);
        method.setAccessible(true);
        return method.invoke(service, result);
    }

    private static String field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(target);
    }

    private static SpkVerifierService serviceWithRuntime(RuntimeService runtimeService) throws Exception {
        SpkVerifierService service = new SpkVerifierService();
        Field field = SpkVerifierService.class.getDeclaredField("runtimeService");
        field.setAccessible(true);
        field.set(service, runtimeService);
        return service;
    }
}
