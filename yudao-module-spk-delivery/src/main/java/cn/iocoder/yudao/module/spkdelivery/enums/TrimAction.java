package cn.iocoder.yudao.module.spkdelivery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * IPD 裁剪规则动作枚举（D3）。
 * <p>
 * 取代 SpkIpdTrimRuleDO.action 的裸字符串约定，给运行时 {@code SpkIpdTrimRuleEvaluator}
 * 一个强类型判定依据。修 G6：裁剪规则此前是死数据，执行层从不读它。
 *
 * @author SPK-OS
 */
@Getter
@AllArgsConstructor
public enum TrimAction {

    /** 跳过该活动：节点直接置成功占位，不执行触发器/不派发任务 */
    SKIP("SKIP", "跳过"),
    /** 可选活动：按 trimCondition 判定是否执行 */
    OPTIONAL("OPTIONAL", "可选"),
    /** 简化执行：降级为最简路径（如免审批、合并 TR） */
    SIMPLIFY("SIMPLIFY", "简化");

    private final String code;
    private final String label;

    public static TrimAction of(String code) {
        if (code == null) {
            return null;
        }
        for (TrimAction e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return null;
    }

    /** 裸字符串是否合法动作值（saveTrimRule 校验用） */
    public static boolean isValid(String code) {
        return of(code) != null;
    }
}
