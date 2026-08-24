package cn.iocoder.yudao.module.spkdelivery.service.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OmnigentAdapterTest {

    @Test
    void usesCortexAgentModelAsOmnigentSessionOverride() {
        SpkAgentDispatchReq req = new SpkAgentDispatchReq()
                .setMode("omnigent")
                .setLeadCode("lead-coding")
                .setModel("ali_glm-5.2");

        Map<String, Object> body = OmnigentAdapter.buildSessionCreateBody(
                req, "agent-uuid", "develop-agent", Map.of("type", "message"));

        assertEquals("ali_glm-5.2", body.get("model_override"));
        assertEquals("agent-uuid", body.get("agent_id"));
    }

    @Test
    void rejectsExplicitOmnigentRouteWithoutCortexAgentModel() {
        SpkAgentDispatchReq req = new SpkAgentDispatchReq().setMode("omnigent");

        assertThrows(IllegalStateException.class, () -> OmnigentAdapter.buildSessionCreateBody(
                req, "agent-uuid", "concept-agent", Map.of("type", "message")));
    }

    @Test
    void extractsOnlyLatestAssistantMessage() {
        String items = """
                {"data":[
                  {"type":"message","role":"assistant","content":[{"text":"我需要先读取文档"}]},
                  {"type":"function_call","name":"sys_os_read"},
                  {"type":"message","data":{"role":"assistant","content":[{"text":"{\\\"document\\\":{}"},{"text":"}"}]}}
                ]}
                """;

        assertEquals("{\"document\":{}\n}", OmnigentAdapter.extractAssistantText(items));
    }

    @Test
    void normalizesHarmlessPreambleButDoesNotRepairInvalidJson() {
        assertEquals("{\"document\":{},\"summary\":\"ok\"}",
                OmnigentAdapter.normalizeJsonEnvelope(
                        "已完成。\\n{\"document\":{},\"summary\":\"ok\"}"));
        assertNull(OmnigentAdapter.normalizeJsonEnvelope(
                "{\"document\":{\"summary\":\"unescaped \"quote\"\"}}"));
        assertNull(OmnigentAdapter.normalizeJsonEnvelope(
                "```json\\n{\"document\":{}}\\n```"));
    }

    @Test
    void readsOnlyFrozenValidOutputFile(@TempDir Path workspace) throws Exception {
        Path output = workspace.resolve(".ipd/output/run-1/report.json");
        assertEquals(output, OmnigentAdapter.validateExpectedOutputFile(workspace.toString(), output.toString()));
        Files.writeString(output, "{\"document\":{\"evidence\":[\"x\"]},\"summary\":\"ok\",\"conclusion\":\"PASS: verified\"}");
        assertEquals("{\"document\":{\"evidence\":[\"x\"]},\"summary\":\"ok\",\"conclusion\":\"PASS: verified\"}",
                OmnigentAdapter.readExpectedOutputEnvelope(output));
    }

    @Test
    void rejectsNarrativeOrEscapedOutput(@TempDir Path workspace) throws Exception {
        Path output = workspace.resolve(".ipd/output/run-1/report.json");
        OmnigentAdapter.validateExpectedOutputFile(workspace.toString(), output.toString());
        Files.writeString(output, "让我检查最终报告。");
        assertThrows(IllegalStateException.class, () -> OmnigentAdapter.readExpectedOutputEnvelope(output));
        assertThrows(IllegalStateException.class,
                () -> OmnigentAdapter.validateExpectedOutputFile(workspace.toString(),
                        workspace.resolve("../outside.json").toString()));
    }

    @Test
    void treatsBothOmnigentAlreadyBoundResponsesAsIdempotent() {
        assertTrue(OmnigentAdapter.isRunnerAlreadyBound(
                "POST /v1/hosts/h/runners -> 400: session already has a runner bound"));
        assertTrue(OmnigentAdapter.isRunnerAlreadyBound("POST runners -> 409"));
        assertFalse(OmnigentAdapter.isRunnerAlreadyBound("POST runners -> 400: workspace missing"));
    }
}
