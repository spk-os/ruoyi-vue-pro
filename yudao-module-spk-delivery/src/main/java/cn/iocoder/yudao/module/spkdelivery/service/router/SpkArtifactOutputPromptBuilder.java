package cn.iocoder.yudao.module.spkdelivery.service.router;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Cortex 与 Claude Code 之间的主产物输出协议。 */
public final class SpkArtifactOutputPromptBuilder {

    private SpkArtifactOutputPromptBuilder() {
    }

    public static String append(String prompt, String artifactType) {
        return append(prompt, artifactType, null);
    }

    public static String append(String prompt, String artifactType, String outputFile) {
        return append(prompt, artifactType, outputFile, null, false);
    }

    public static String append(String prompt, String artifactType, String outputFile,
                                String verifierScriptsRoot, boolean requireVerifierContract) {
        String type = artifactType == null || artifactType.isBlank() ? "activity-result" : artifactType.trim();
        String fileContract = outputFile == null || outputFile.isBlank() ? ""
                : "必须把完整 JSON 同时写入 Cortex 为本 ActivityRun 冻结的唯一主产物路径："
                + outputFile + "。不得改名、另存或仅在最终响应中返回；该文件是 Cortex 登记产物的唯一权威来源。";
        String verifierContract = loadVerifierContract(verifierScriptsRoot, type, outputFile,
                requireVerifierContract);
        String responseContract = outputFile == null || outputFile.isBlank()
                ? "最终响应只能是一个有效 JSON 对象，不得使用 Markdown、代码围栏或 JSON 前后解释文字。"
                    + "对象格式为 {\"document\":{...},\"summary\":\"...\",\"conclusion\":\"PASS|FAIL: ...\"}。"
                : "完成预检后，最终响应只能是一个有效 JSON 对象（小型回执），不得使用 Markdown、代码围栏或解释文字："
                    + "{\"artifact_uri\":\"file://" + outputFile
                    + "\",\"sha256\":\"<主产物文件的64位SHA-256>\",\"preflight_exit_code\":0,"
                    + "\"conclusion\":\"PASS\"}。必须用 sha256sum 计算真实哈希；不得把主产物正文复制到最终响应，"
                    + "也不得为了返回正文而再次 cat/head/tail 分段读取。Cortex 将直接读取并核验上述冻结文件。";
        return prompt + "\n\n【Cortex 主产物输出协议（强制）】\n" + fileContract
                + responseContract
                + "document 必须是与产物类型 " + type + " 对应的 JSON 对象，包含可追溯的 sources/evidence、"
                + "明确结论和可验证验收项；不得把 document 写成字符串化 Markdown。"
                + verifierContract
                + "必须使用 JSON serializer 或 jq 构造结果，不得手写未转义的双引号；"
                + "返回前必须用 jq -e 'type == \"object\" and (.document | type == \"object\")' 校验通过，"
                + "并确保回执中的 URI、SHA-256 与校验后的冻结文件完全一致。";
    }

    /**
     * 将与 Verifier 实际读取的同一份确定性契约冻结进任务 prompt，避免 Agent 产出字段与核验 Schema 漂移。
     */
    private static String loadVerifierContract(String root, String artifactType, String outputFile,
                                               boolean required) {
        if (root == null || root.isBlank()) {
            if (required) {
                throw new IllegalStateException("商用任务缺少 verifier scripts root 配置");
            }
            return "";
        }
        String type = safeSegment(artifactType, "artifactType");
        Path base = Path.of(root).toAbsolutePath().normalize();
        Path typeDir = base.resolve(type).normalize();
        if (!typeDir.startsWith(base)) {
            throw new IllegalArgumentException("artifactType 越过 verifier scripts root");
        }
        String schema = readContractFile(typeDir.resolve("schema.json"), required);
        String acceptance = readContractFile(typeDir.resolve("acceptance.json"), required);
        if (schema.isBlank() && acceptance.isBlank()) {
            return "";
        }
        String validator = base.getParent().resolve("skills/commercial-release/scripts/validate_artifact.py")
                .normalize().toString();
        String preflight = outputFile == null || outputFile.isBlank() ? "" :
                "\n完成写入后必须执行与 Cortex Layer1/Layer2 同源的真实预检：\n"
                        + "python3 " + shellArg(validator)
                        + " --contracts-root " + shellArg(base.toString())
                        + " --artifact-type " + shellArg(type)
                        + " --input " + shellArg(outputFile) + "\n"
                        + "预检 exitCode 必须为 0；非 0 时必须按输出逐项修正并重新运行，"
                        + "禁止登记未通过预检的文件或返回 PASS 回执。\n";
        return "document 必须逐字段通过以下 Cortex 确定性核验契约；不得用同义字段或 camelCase 别名替代："
                + "\n<schema.json>\n" + schema + "\n</schema.json>"
                + "\n<acceptance.json>\n" + acceptance + "\n</acceptance.json>\n"
                + preflight;
    }

    private static String readContractFile(Path path, boolean required) {
        if (!Files.isRegularFile(path)) {
            if (required) {
                throw new IllegalStateException("商用任务缺少确定性核验契约: " + path);
            }
            return "";
        }
        try {
            long size = Files.size(path);
            if (size <= 0 || size > 65_536) {
                throw new IllegalStateException("确定性核验契约大小非法: " + path + " bytes=" + size);
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取确定性核验契约失败: " + path, e);
        }
    }

    /** 生成受控且逐 ActivityRun 隔离的主产物路径。 */
    public static String outputFile(String projectRoot, String activityRunId, String artifactType) {
        if (projectRoot == null || projectRoot.isBlank()) {
            return null;
        }
        String run = safeSegment(activityRunId, "activityRunId");
        String type = safeSegment(artifactType == null || artifactType.isBlank()
                ? "activity-result" : artifactType.trim(), "artifactType");
        return Path.of(projectRoot).toAbsolutePath().normalize()
                .resolve(".ipd").resolve("output").resolve(run).resolve(type + ".json")
                .normalize().toString();
    }

    private static String safeSegment(String value, String field) {
        if (value == null || !value.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException(field + " 含非法路径字符");
        }
        return value;
    }

    private static String shellArg(String value) {
        if (value.matches("[A-Za-z0-9_./:-]+")) {
            return value;
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
