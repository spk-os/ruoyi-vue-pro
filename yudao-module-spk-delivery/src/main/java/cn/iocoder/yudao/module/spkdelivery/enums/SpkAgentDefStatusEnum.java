package cn.iocoder.yudao.module.spkdelivery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SPK-OS 智能体状态枚举（DB 存 label 字符串）
 *
 * <p>对标 Paddock agents.status：offline / idle / busy / error。
 *
 * @author SPK-OS
 */
@Getter
@AllArgsConstructor
public enum SpkAgentDefStatusEnum {

    OFFLINE("offline"),
    IDLE("idle"),
    BUSY("busy"),
    ERROR("error");

    private final String label;

    public static SpkAgentDefStatusEnum of(String label) {
        if (label == null) {
            return null;
        }
        for (SpkAgentDefStatusEnum e : values()) {
            if (e.label.equalsIgnoreCase(label)) {
                return e;
            }
        }
        return null;
    }

}
