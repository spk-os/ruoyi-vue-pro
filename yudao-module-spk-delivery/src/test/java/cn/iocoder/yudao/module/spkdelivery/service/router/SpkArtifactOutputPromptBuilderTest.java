package cn.iocoder.yudao.module.spkdelivery.service.router;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpkArtifactOutputPromptBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void freezesTypedDocumentToFileAndReturnsOnlySmallReceipt() {
        String output = SpkArtifactOutputPromptBuilder.outputFile(
                "/work/SPK-OS/dev/demo", "run-123", "req-insight-report");
        String prompt = SpkArtifactOutputPromptBuilder.append("base", "req-insight-report", output);
        assertTrue(prompt.contains("只能是一个有效 JSON 对象"));
        assertTrue(prompt.contains("document 必须是与产物类型"));
        assertTrue(prompt.contains("req-insight-report"));
        assertTrue(prompt.contains("不得使用 Markdown"));
        assertTrue(prompt.contains("JSON serializer 或 jq"));
        assertTrue(prompt.contains("jq -e"));
        assertTrue(prompt.contains(output));
        assertTrue(prompt.contains("唯一权威来源"));
        assertTrue(prompt.contains("\"artifact_uri\":\"file://" + output + "\""));
        assertTrue(prompt.contains("\"preflight_exit_code\":0"));
        assertTrue(prompt.contains("不得把主产物正文复制到最终响应"),
                "大产物正文已经冻结到文件，最终响应必须保持为小回执，避免 Claude 反复 cat/head/tail 后无法结束会话");
        assertTrue(!prompt.contains("把校验后文件的完整内容作为最终响应"));
        assertEquals("/work/SPK-OS/dev/demo/.ipd/output/run-123/req-insight-report.json", output);
        assertThrows(IllegalArgumentException.class,
                () -> SpkArtifactOutputPromptBuilder.outputFile("/work/demo", "../escape", "report"));
    }

    @Test
    void freezesVerifierContractAndFailsClosedWhenMissing() throws IOException {
        Path contractDir = Files.createDirectories(tempDir.resolve("req-insight-report"));
        Files.writeString(contractDir.resolve("schema.json"), "{\"required\":[\"req_id\"]}");
        Files.writeString(contractDir.resolve("acceptance.json"), "{\"rules\":[]}");

        String prompt = SpkArtifactOutputPromptBuilder.append("base", "req-insight-report", "/tmp/out.json",
                tempDir.toString(), true);
        assertTrue(prompt.contains("<schema.json>"));
        assertTrue(prompt.contains("req_id"));
        assertTrue(prompt.contains("<acceptance.json>"));
        assertThrows(IllegalStateException.class, () -> SpkArtifactOutputPromptBuilder.append(
                "base", "missing-contract", "/tmp/out.json", tempDir.toString(), true));
    }

    @Test
    void freezesCommercialProjectPlanExecutableRequirementContract() {
        String root = "/work/SPK-OS/soft/basic/ruoyi/resources/verifier-scripts";

        String prompt = SpkArtifactOutputPromptBuilder.append("base", "project-plan", "/tmp/out.json",
                root, true);

        assertTrue(prompt.contains("development_specification"));
        assertTrue(prompt.contains("acceptance_criteria"));
        assertTrue(prompt.contains("issue_mappings"));
        assertTrue(prompt.contains("不得用同义字段或 camelCase 别名替代"));
        assertTrue(prompt.contains("validate_artifact.py"),
                "Producer 返回前必须运行与 Cortex schema 同源的本地预检，不能等十几分钟后的 Verifier 才发现结构错误");
        assertTrue(prompt.contains("--artifact-type project-plan"));
        assertTrue(prompt.contains("--input /tmp/out.json"));
        assertTrue(prompt.contains("预检 exitCode 必须为 0"));
    }
}
