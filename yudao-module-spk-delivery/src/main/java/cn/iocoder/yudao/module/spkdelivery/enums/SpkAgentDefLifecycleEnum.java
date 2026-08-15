package cn.iocoder.yudao.module.spkdelivery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SPK-OS 智能体生命周期枚举（治理维度，与运行态 status=idle/busy/error 正交）
 *
 * <p>对标 §9.5 治理表面：注册 / 试运行 / 就绪 / 退役。
 * 不破坏现有 status 语义（status 表示瞬时运行态，lifecycle 表示生命周期阶段）。
 *
 * @author SPK-OS
 */
@Getter
@AllArgsConstructor
public enum SpkAgentDefLifecycleEnum {

    /** 注册：刚录入，未校验 */
    REGISTER("register"),
    /** 试运行：可唤醒做单轮验证，未上生产编队 */
    TRIAL("trial"),
    /** 就绪：通过试运行，可编入生产小队 */
    READY("ready"),
    /** 退役：只读归档，不再派发 */
    RETIRE("retire");

    private final String label;

    public static SpkAgentDefLifecycleEnum of(String label) {
        if (label == null) {
            return null;
        }
        for (SpkAgentDefLifecycleEnum e : values()) {
            if (e.label.equalsIgnoreCase(label)) {
                return e;
            }
        }
        return null;
    }

}
