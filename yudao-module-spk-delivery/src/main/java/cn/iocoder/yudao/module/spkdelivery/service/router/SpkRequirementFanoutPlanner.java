package cn.iocoder.yudao.module.spkdelivery.service.router;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 商用开发阶段需求分片规划器。
 *
 * <p>需求集合取自 FlowRun 启动时冻结的版本范围，不允许 Agent 在运行中自行决定分片。
 * 每个分片由 Cortex 创建独立 TaskContract/activityRunId，因而 Omnigent 会创建独立会话。</p>
 */
final class SpkRequirementFanoutPlanner {

    private static final Pattern REQUIREMENT_LINE = Pattern.compile(
            "(?m)^\\s*(REQ-[A-Z0-9]+(?:-[A-Z0-9]+)*)\\s+(.+?)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final int MAX_REQUIREMENTS = 50;

    private SpkRequirementFanoutPlanner() {
    }

    static List<RequirementSlice> parseFrozenScope(String scope) {
        if (scope == null || scope.isBlank()) {
            throw new IllegalStateException("商用开发阶段缺少冻结的 versionScope，禁止需求分发");
        }
        Matcher matcher = REQUIREMENT_LINE.matcher(scope);
        List<RequirementSlice> slices = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        while (matcher.find()) {
            String id = matcher.group(1).toUpperCase();
            String statement = matcher.group(2).trim();
            if (!ids.add(id)) {
                throw new IllegalStateException("冻结 versionScope 存在重复需求编号: " + id);
            }
            if (statement.isBlank()) {
                throw new IllegalStateException("冻结 versionScope 的需求说明为空: " + id);
            }
            slices.add(new RequirementSlice(id, statement));
            if (slices.size() > MAX_REQUIREMENTS) {
                throw new IllegalStateException("需求分片超过上限 " + MAX_REQUIREMENTS);
            }
        }
        if (slices.isEmpty()) {
            throw new IllegalStateException("冻结 versionScope 未找到结构化 REQ-* 需求，禁止需求分发");
        }
        return List.copyOf(slices);
    }

    static String requirementPrompt(RequirementSlice slice, int ordinal, int total, int approvalReworkCount) {
        return "\n\n【Cortex 自动需求分片：REQUIREMENT】\n"
                + "fanoutApprovalCycle: " + approvalReworkCount + "\n"
                + "这是 Cortex 创建的第 " + ordinal + "/" + total + " 个独立需求合同。\n"
                + "requirementId: " + slice.id() + "\n"
                + "frozenStatement: " + slice.statement() + "\n"
                + "只实现并验证本 requirementId；不得接管其他 REQ，不得充当集成 Agent。"
                + "必须使用独立分支、提交、测试和 RTM 证据，并在主产物中回填 requirementId、commit SHA 与测试结果。"
                + "当前会话由 Cortex 通过 Omnigent 自动创建；不得改写 runtime/model 配置。";
    }

    static String requirementMarker(int approvalReworkCount) {
        return "fanoutApprovalCycle: " + approvalReworkCount + "\n"
                + "这是 Cortex 创建的第 ";
    }

    static boolean matchesRequirementContract(String prompt, RequirementSlice slice, int approvalReworkCount) {
        return prompt != null
                && prompt.contains(requirementMarker(approvalReworkCount))
                && prompt.contains("requirementId: " + slice.id() + "\n");
    }

    static String integrationPrompt(List<RequirementSlice> slices, List<String> artifactIds) {
        String ids = slices.stream().map(RequirementSlice::id).reduce((a, b) -> a + "," + b).orElse("");
        return "\n\n【Cortex 自动需求分片：INTEGRATION】\n"
                + "这是独立集成合同，不是需求实现合同。全部需求合同已独立完成。\n"
                + "requirements: " + ids + "\n"
                + "requirementArtifacts: " + String.join(",", artifactIds) + "\n"
                + "逐项核验分支/提交/测试/RTM 后进行集成、冲突处理、全量构建与集成冒烟。"
                + "任何需求证据缺失或集成失败，结论必须为 FAIL；不得用集成结果掩盖单需求缺陷。";
    }

    record RequirementSlice(String id, String statement) {
    }
}
