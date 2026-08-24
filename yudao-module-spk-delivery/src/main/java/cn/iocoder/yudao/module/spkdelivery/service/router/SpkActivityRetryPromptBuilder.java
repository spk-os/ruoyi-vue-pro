package cn.iocoder.yudao.module.spkdelivery.service.router;

/** 将上一轮执行/独立核验失败意见注入同一 Activity 的自动重试 prompt。 */
public final class SpkActivityRetryPromptBuilder {

    private static final int MAX_FAILURE_REASON_CHARS = 3_000;

    private SpkActivityRetryPromptBuilder() {
    }

    public static String append(String prompt, String activityId, String retryActivityId,
                                String reason, int retryCount, int maxAttempts) {
        if (activityId == null || retryActivityId == null || !activityId.equals(retryActivityId)
                || reason == null || reason.isBlank() || retryCount <= 0) {
            return prompt;
        }
        String actionableReason = reason.trim();
        if (actionableReason.length() > MAX_FAILURE_REASON_CHARS) {
            actionableReason = actionableReason.substring(0, MAX_FAILURE_REASON_CHARS)
                    + "\n[已截断：仅保留本轮失败意见前 " + MAX_FAILURE_REASON_CHARS
                    + " 字；以本次冻结的 Schema/Acceptance 和预检输出为准]";
        }
        return prompt + "\n\n【Cortex 自动重试指令】\n"
                + "这是本 Activity 第 " + (retryCount + 1) + "/" + maxAttempts + " 次执行。"
                + "上一轮执行或独立核验未通过：" + actionableReason + "\n"
                + "必须针对上述意见修正主产物并重新提供证据；不得复用旧产物、旧哈希或旧核验结论。"
                + "本次由 Cortex 自动创建新的 Omnigent→Claude Code 执行并重新独立核验。";
    }
}
