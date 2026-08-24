package cn.iocoder.yudao.module.spkdelivery.service.router;

import java.nio.file.Path;

/** 商用交付的 fail-closed 规则，集中在纯函数中便于单元测试。 */
public final class SpkCommercialDeliveryGuard {

    private SpkCommercialDeliveryGuard() {
    }

    public static boolean isCommercialRelease(String skillEnv) {
        return "commercial-release".equalsIgnoreCase(skillEnv);
    }

    public static void requireSkill(String skillEnv, String skillName, String skillPath, String activityId) {
        if ("commercial-release".equalsIgnoreCase(skillEnv)
                && (skillName == null || skillName.isBlank()
                || skillPath == null || skillPath.isBlank())) {
            throw new IllegalStateException("commercial-release skill 缺失，禁止执行 activity=" + activityId);
        }
    }

    public static void requireOmnigentClaudeCodeRoute(String skillEnv, String adapterName, String model,
                                                       String runtimeType, String omnigentAgentId,
                                                       String agentCode) {
        if (!"commercial-release".equalsIgnoreCase(skillEnv)) {
            return;
        }
        if (!"omnigent".equalsIgnoreCase(adapterName)) {
            throw new IllegalStateException("commercial-release 必须经 Omnigent→Claude Code 执行，禁止 adapter="
                    + adapterName + " agent=" + agentCode);
        }
        if (!"ali_glm-5.2".equalsIgnoreCase(model)) {
            throw new IllegalStateException("commercial-release 必须使用 new-api/ali_glm-5.2，禁止 model="
                    + model + " agent=" + agentCode);
        }
        if (!"claude".equalsIgnoreCase(runtimeType)) {
            throw new IllegalStateException("commercial-release 必须使用 Claude Code runtime，禁止 runtimeType="
                    + runtimeType + " agent=" + agentCode);
        }
        String expectedAgentId;
        if (agentCode != null && agentCode.startsWith("verifier-")) {
            expectedAgentId = "spk-ipd-verifier";
        } else if ("lead-arch-design".equals(agentCode) || "lead-sys-arch".equals(agentCode)) {
            expectedAgentId = "spk-architect";
        } else {
            expectedAgentId = "spk-ipd-executor";
        }
        if (!expectedAgentId.equalsIgnoreCase(omnigentAgentId)) {
            throw new IllegalStateException("commercial-release Agent 能力绑定错误，expected="
                    + expectedAgentId + " actual=" + omnigentAgentId + " agent=" + agentCode);
        }
    }

    /**
     * 商用流程的 Agent（包括 Independent Verifier）必须在 FlowRun 启动时冻结的项目目录中执行。
     * 禁止缺失上下文后回退到 Adapter 的历史全局 workspace，否则核证结果会被其它项目污染。
     */
    public static void requireProjectExecutionContext(String skillEnv, Long projectId, Long versionId,
                                                       String workspace, String activityId) {
        if (!isCommercialRelease(skillEnv)) {
            return;
        }
        if (projectId == null || versionId == null || workspace == null || workspace.isBlank()) {
            throw new IllegalStateException("commercial-release 项目执行上下文不完整，禁止回退全局 workspace: activity="
                    + activityId + " projectId=" + projectId + " versionId=" + versionId
                    + " workspace=" + workspace);
        }
        try {
            if (!Path.of(workspace).isAbsolute()) {
                throw new IllegalStateException("commercial-release 项目 workspace 必须是绝对路径: " + workspace);
            }
        } catch (RuntimeException e) {
            if (e instanceof IllegalStateException) {
                throw e;
            }
            throw new IllegalStateException("commercial-release 项目 workspace 非法: " + workspace, e);
        }
    }

    public static void requireVerificationPassed(boolean required, String verifierCode, String conclusion) {
        if (!required) {
            return;
        }
        if (verifierCode == null || verifierCode.isBlank()) {
            throw new IllegalStateException("独立核证器缺失，禁止完成 Activity");
        }
        if (!"PASS".equalsIgnoreCase(conclusion)) {
            throw new IllegalStateException("独立核证未通过，禁止完成 Activity: " + conclusion);
        }
    }
}
