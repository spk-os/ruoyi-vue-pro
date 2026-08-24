package cn.iocoder.yudao.module.spkdelivery.service.router;

import java.util.Map;

/** 将 FlowRun 启动时冻结的项目/版本上下文注入每个 Agent Activity。 */
final class SpkProjectPromptBuilder {

    private SpkProjectPromptBuilder() {
    }

    static String append(String prompt, Map<String, Object> context) {
        if (context == null || context.get("projectId") == null) {
            return prompt;
        }
        StringBuilder out = new StringBuilder(prompt == null ? "" : prompt);
        out.append("\n\n## Cortex 冻结的项目执行上下文\n")
                .append("以下字段是本 FlowRun 的权威输入；必须在指定工作区执行，不得替换项目、路径、模型或环境。\n");
        append(out, "项目ID", context.get("projectId"));
        append(out, "项目短码", context.get("projectCode"));
        append(out, "项目名称", context.get("projectName"));
        append(out, "项目工作区", context.get("projectRoot"));
        append(out, "项目背景与边界", context.get("projectDescription"));
        append(out, "项目成功目标", context.get("projectObjective"));
        append(out, "版本ID", context.get("versionId"));
        append(out, "版本号", context.get("versionNo"));
        append(out, "版本目标", context.get("versionObjective"));
        append(out, "版本范围", context.get("versionScope"));
        return out.toString();
    }

    private static void append(StringBuilder out, String label, Object value) {
        if (value != null && !value.toString().isBlank()) {
            out.append("- ").append(label).append("：").append(value).append('\n');
        }
    }
}
