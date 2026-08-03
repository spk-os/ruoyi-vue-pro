package cn.iocoder.yudao.module.spkdelivery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Artifact 状态枚举
 *
 * @author SPK-OS
 */
@Getter
@AllArgsConstructor
public enum SpkArtifactStatusEnum {

    DRAFT("draft", "草稿"),
    SIGNED("signed", "已签署"),
    SUPERSEDED("superseded", "已超版"),
    QUARANTINED("quarantined", "已隔离");

    private final String label;
    private final String description;

    public static SpkArtifactStatusEnum of(String label) {
        if (label == null) {
            return null;
        }
        for (SpkArtifactStatusEnum e : values()) {
            if (e.label.equalsIgnoreCase(label)) {
                return e;
            }
        }
        return null;
    }

}
