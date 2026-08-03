package cn.iocoder.yudao.module.spkdelivery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 证据类型枚举
 *
 * @author SPK-OS
 */
@Getter
@AllArgsConstructor
public enum SpkEvidenceTypeEnum {

    RUN("run", "运行收据"),
    TOOL("tool", "工具收据"),
    VERIFICATION("verification", "验证收据"),
    HUMAN_APPROVAL("human_approval", "人工审批"),
    DECISION("decision", "决策"),
    GATE("gate", "门径");

    private final String label;
    private final String description;

    public static SpkEvidenceTypeEnum of(String label) {
        if (label == null) {
            return null;
        }
        for (SpkEvidenceTypeEnum e : values()) {
            if (e.label.equalsIgnoreCase(label)) {
                return e;
            }
        }
        return null;
    }

}
