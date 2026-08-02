package cn.iocoder.yudao.module.spkdelivery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SPK-OS 智能体编队状态枚举（DB 存 label 字符串）
 *
 * @author SPK-OS
 */
@Getter
@AllArgsConstructor
public enum SpkAgentSquadStatusEnum {

    ACTIVE("active"),
    DISABLED("disabled");

    private final String label;

}
