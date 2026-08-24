package cn.iocoder.yudao.module.spkdelivery.service.router;

/** 把审批驳回意见转换为下一轮 Agent 可执行的返工指令。 */
final class SpkReworkPromptBuilder {

    private SpkReworkPromptBuilder() {
    }

    static String append(String basePrompt, String activityStage, String reworkStage,
                         String gate, String reason, int count) {
        if (activityStage == null || reworkStage == null
                || !activityStage.equalsIgnoreCase(reworkStage)) {
            return basePrompt;
        }
        String safeReason = reason == null ? "未提供" : reason.trim();
        if (safeReason.length() > 4000) {
            safeReason = safeReason.substring(0, 4000);
        }
        return basePrompt + "\n\n## 自动返工指令\n"
                + "本 Activity 因 " + (gate == null ? "审批门" : gate)
                + " 驳回进入第 " + Math.max(count, 1) + " 次自动返工。\n"
                + "审批意见：" + safeReason + "\n"
                + "必须逐项修订、补齐可验证证据，并在修订完成后重新提交审批；不得忽略、弱化或伪造通过。";
    }
}
