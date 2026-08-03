package cn.iocoder.yudao.module.spkdelivery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * IPD Activity 阶段枚举
 *
 * @author SPK-OS
 */
@Getter
@AllArgsConstructor
public enum SpkActivityStageEnum {

    CONCEPT("concept", "概念"),
    PLAN("plan", "计划"),
    DEVELOP("develop", "开发"),
    QUALIFY("qualify", "验证"),
    LAUNCH("launch", "发布"),
    LIFECYCLE("lifecycle", "生命周期"),
    SUPPORT("support", "支撑");

    private final String code;
    private final String label;

    public static SpkActivityStageEnum of(String code) {
        if (code == null) {
            return null;
        }
        for (SpkActivityStageEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return null;
    }

}
