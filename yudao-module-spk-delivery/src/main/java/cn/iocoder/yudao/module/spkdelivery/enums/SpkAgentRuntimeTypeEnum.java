package cn.iocoder.yudao.module.spkdelivery.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SPK-OS 智能体运行时类型枚举
 *
 * <p>对标 Paddock runtime_type：native（走 yudao-module-ai 内核，不依赖 openclaw）/ claude / codex / custom。
 * 本期仅 native 支持本地 wake 单轮对话。
 *
 * @author SPK-OS
 */
@Getter
@AllArgsConstructor
public enum SpkAgentRuntimeTypeEnum {

    NATIVE("native", "yudao-module-ai 内核（本地）"),
    CLAUDE("claude", "Claude Code 外部 runtime"),
    CODEX("codex", "Codex 外部 runtime"),
    CUSTOM("custom", "自定义 runtime");

    private final String type;
    private final String label;

    public static boolean supportsWake(String type) {
        return NATIVE.type.equals(type);
    }

}
