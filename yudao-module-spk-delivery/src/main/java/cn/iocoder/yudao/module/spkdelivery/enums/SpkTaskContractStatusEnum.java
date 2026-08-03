package cn.iocoder.yudao.module.spkdelivery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Task Contract 状态枚举
 *
 * @author SPK-OS
 */
@Getter
@AllArgsConstructor
public enum SpkTaskContractStatusEnum {

    QUEUED("queued", "已入队"),
    RUNNING("running", "执行中"),
    DONE("done", "完成"),
    FAILED("failed", "失败"),
    TIMEOUT("timeout", "超时"),
    CANCELLED("cancelled", "已取消");

    private final String label;
    private final String description;

    public static SpkTaskContractStatusEnum of(String label) {
        if (label == null) {
            return null;
        }
        for (SpkTaskContractStatusEnum e : values()) {
            if (e.label.equalsIgnoreCase(label)) {
                return e;
            }
        }
        return null;
    }

    public boolean isTerminal() {
        return this == DONE || this == FAILED || this == TIMEOUT || this == CANCELLED;
    }

    public boolean isRunningLike() {
        return this == RUNNING || this == QUEUED;
    }

}
