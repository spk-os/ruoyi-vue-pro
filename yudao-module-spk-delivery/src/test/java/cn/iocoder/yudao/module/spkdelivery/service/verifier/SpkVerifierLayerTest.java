package cn.iocoder.yudao.module.spkdelivery.service.verifier;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void layer2_validArtifact_signoffConditional_triggersConditional() throws Exception {
        // 合法产物无 signed_by → signoff_complete(conditional) 应未通过（属 conditional 非 fail）
        JsonNode acceptance = loadScript("customer-need-brief", "acceptance.json");
        JsonNode instance = JsonUtils.parseTree(VALID);
        JsonNode rules = acceptance.path("rules");
        boolean signoffConditionalViolated = false;
        for (JsonNode rule : rules) {
            SpkAcceptanceRuleEngine.Result r = SpkAcceptanceRuleEngine.evaluate(rule, instance);
            if ("signoff_complete".equals(r.id) && !r.passed && !r.isFail()) {
                signoffConditionalViolated = true;
            }
        }
        assertTrue(signoffConditionalViolated, "无签署须触发 signoff_complete conditional 规则");
    }
}
