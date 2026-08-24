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

    @Schema(description = "Agent 执行模型；Omnigent 路由将作为 model_override 显式传递", example = "ali_glm-5.2")
    @Size(max = 100, message = "Agent 执行模型长度不能超过 100")
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

    // ===== 高级字段（高级模式才显，通用模式默认） =====

    @Schema(description = "智能体种类 lead/worker/verifier", example = "lead")
    private String agentKind;

    @Schema(description = "能力标签数组 JSON，Task Router 路由依据", example = "[\"architecture\"]")
    private String capabilityTags;

    @Schema(description = "Verifier 类型 TR/RE/SEC，仅 verifier 用", example = "TR")
    private String verifierType;

    @Schema(description = "隔离级别 process/container/vm", example = "process")
    private String isolationLevel;

    @Schema(description = "执行模式 local/omnigent（per-agent 决定走 NativeAiAdapter 还是 OmnigentAdapter）", example = "local")
    private String mode;

    @Schema(description = "继承父智能体 id（运行时合并解析：capabilityTags 并集、soulContent/model/mode 子覆盖父）", example = "1")
    private Long parentDefId;

    @Schema(description = "Omnigent 侧 agent-id 映射（mode=omnigent 时用，空回退全局配置）", example = "spk-architect")
    private String omnigentAgentId;

}
