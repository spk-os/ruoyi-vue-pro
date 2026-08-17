package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.module.spkdelivery.enums.TrimAction;

/**
 * IPD 裁剪规则运行时决策值对象（D3）。
 * <p>
 * 由 {@link SpkIpdTrimRuleEvaluator} 在触发器/任务派发入口产出，告诉执行层该活动应如何处理。
 * 修 G6：此前 TrimRule 是死数据，执行层从不读它。
 *
 * @author SPK-OS
 */
public record TrimDecision(TrimAction action, String reason, Long matchedRuleId) {

    /** 无裁剪：正常执行（through = 放行） */
    public static TrimDecision through() {
        return new TrimDecision(null, null, null);
    }

    public boolean shouldSkip() {
        return action == TrimAction.SKIP;
    }

    public boolean isOptional() {
        return action == TrimAction.OPTIONAL;
    }

    public boolean shouldSimplify() {
        return action == TrimAction.SIMPLIFY;
    }

    public boolean hasAction() {
        return action != null;
    }
}
