package cn.iocoder.yudao.module.spkdelivery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SPK-OS Agent 任务状态枚举（DB 存 label 字符串）
 *
 * @author SPK-OS
 */
@Getter
@AllArgsConstructor
public enum SpkAgentTaskStatusEnum {

    RUNNING("running"),
    DONE("done"),
    FAILED("failed"),
    CANCELLED("cancelled");

    private final String label;

    public static SpkAgentTaskStatusEnum of(String label) {
        if (label == null) {
            return null;
        }
        for (SpkAgentTaskStatusEnum e : values()) {
            if (e.label.equalsIgnoreCase(label)) {
                return e;
            }
        }
        return null;
    }

    public boolean isTerminal() {
        return this == DONE || this == FAILED || this == CANCELLED;
    }

}
