package cn.iocoder.yudao.module.spkdelivery.service.router;

/**
 * Agent Activity 自动重试写入 Flowable 的运行时上下文。
 *
 * <p>变量按 activityId 隔离，避免失败意见污染同阶段的后续 Activity；成功后由调度服务清理。</p>
 */
public final class SpkActivityRetryVariables {

    public static final String ACTIVITY_ID = "spk_activity_retry_activity_id";
    public static final String REASON = "spk_activity_retry_reason";
    public static final String COUNT = "spk_activity_retry_count";
    public static final String MAX_ATTEMPTS = "spk_activity_retry_max_attempts";

    private SpkActivityRetryVariables() {
    }
}
