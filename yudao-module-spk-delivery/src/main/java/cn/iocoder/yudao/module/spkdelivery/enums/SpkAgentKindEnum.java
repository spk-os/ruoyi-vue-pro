package cn.iocoder.yudao.module.spkdelivery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 智能体种类枚举（Cortext-IPD §4.6）
 *
 * @author SPK-OS
 */
@Getter
@AllArgsConstructor
public enum SpkAgentKindEnum {

    LEAD("lead", "Lead Agent"),
    WORKER("worker", "Worker Agent"),
    VERIFIER("verifier", "Independent Verifier");

    private final String label;
    private final String description;

    public static SpkAgentKindEnum of(String label) {
        if (label == null) {
            return null;
        }
        for (SpkAgentKindEnum e : values()) {
            if (e.label.equalsIgnoreCase(label)) {
                return e;
            }
        }
        return null;
    }

}
