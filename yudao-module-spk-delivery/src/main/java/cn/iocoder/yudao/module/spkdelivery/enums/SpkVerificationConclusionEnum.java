package cn.iocoder.yudao.module.spkdelivery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 验证结论枚举
 *
 * @author SPK-OS
 */
@Getter
@AllArgsConstructor
public enum SpkVerificationConclusionEnum {

    PASS("PASS", "通过"),
    CONDITIONAL("CONDITIONAL", "有条件通过"),
    FAIL("FAIL", "不通过");

    private final String label;
    private final String description;

    public static SpkVerificationConclusionEnum of(String label) {
        if (label == null) {
            return null;
        }
        for (SpkVerificationConclusionEnum e : values()) {
            if (e.label.equalsIgnoreCase(label)) {
                return e;
            }
        }
        return null;
    }

}
