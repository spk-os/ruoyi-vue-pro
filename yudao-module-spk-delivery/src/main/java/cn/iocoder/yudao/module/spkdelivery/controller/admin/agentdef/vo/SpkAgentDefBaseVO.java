package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 智能体定义 Base VO（创建/更新共用字段）
 *
 * @author SPK-OS
 */
@Data
public class SpkAgentDefBaseVO {

    @Schema(description = "智能体名", requiredMode = Schema.RequiredMode.REQUIRED, example = "Aegis 评审官")
    @NotEmpty(message = "智能体名不能为空")
    @Size(max = 100, message = "智能体名长度不能超过 100")
    private String name;

    @Schema(description = "智能体编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "aegis-reviewer")
    @NotEmpty(message = "智能体编码不能为空")
    @Size(max = 64, message = "智能体编码长度不能超过 64")
    private String code;

    @Schema(description = "角色", requiredMode = Schema.RequiredMode.REQUIRED, example = "代码评审官")
    @NotEmpty(message = "角色不能为空")
    @Size(max = 100, message = "角色长度不能超过 100")
    private String role;

    @Schema(description = "会话路由标识", example = "aegis-reviewer")
    @Size(max = 128, message = "会话路由标识长度不能超过 128")
    private String sessionKey;

    @Schema(description = "灵魂内容 / system prompt")
    private String soulContent;

    @Schema(description = "工作记忆 JSON")
    private String workingMemory;

    @Schema(description = "状态 offline/idle/busy/error", example = "offline")
    private String status;

    @Schema(description = "默认模型", example = "ali_glm-5.2")
    @Size(max = 100, message = "默认模型长度不能超过 100")
    private String model;

    @Schema(description = "关联 yudao AI 角色 id（wake 必填）", example = "1")
    private Long roleId;

    @Schema(description = "工具白/黑名单配置 JSON")
    private String toolsConfig;

    @Schema(description = "杂项配置 JSON")
    private String config;

    @Schema(description = "运行时类型 native/claude/codex/custom", example = "native")
    private String runtimeType;

    @Schema(description = "来源", example = "manual")
    private String source;

    @Schema(description = "是否隐藏 0/1", example = "0")
    private Integer hidden;

    @Schema(description = "最近活动描述")
    private String lastActivity;

}
