package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体定义 Response VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体定义 Response VO")
@Data
public class SpkAgentDefRespVO {

    @Schema(description = "编号")
    private Long id;
    @Schema(description = "智能体名")
    private String name;
    @Schema(description = "编码")
    private String code;
    @Schema(description = "角色")
    private String role;
    @Schema(description = "会话路由标识")
    private String sessionKey;
    @Schema(description = "灵魂内容")
    private String soulContent;
    @Schema(description = "工作记忆")
    private String workingMemory;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "Agent 执行模型")
    private String model;
    @Schema(description = "关联 AI 角色 id")
    private Long roleId;
    @Schema(description = "关联会话 id")
    private Long conversationId;
    @Schema(description = "工具配置")
    private String toolsConfig;
    @Schema(description = "杂项配置")
    private String config;
    @Schema(description = "运行时类型")
    private String runtimeType;
    @Schema(description = "来源")
    private String source;
    @Schema(description = "是否隐藏")
    private Integer hidden;
    @Schema(description = "最近心跳时间")
    private LocalDateTime lastSeen;
    @Schema(description = "最近活动")
    private String lastActivity;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // ===== 高级字段（供前端回显） =====

    @Schema(description = "智能体种类 lead/worker/verifier")
    private String agentKind;
    @Schema(description = "能力标签数组 JSON")
    private String capabilityTags;
    @Schema(description = "Verifier 类型 TR/RE/SEC")
    private String verifierType;
    @Schema(description = "隔离级别 process/container/vm")
    private String isolationLevel;
    @Schema(description = "执行模式 local/omnigent")
    private String mode;
    @Schema(description = "继承父智能体 id")
    private Long parentDefId;
    @Schema(description = "Omnigent 侧 agent-id 映射")
    private String omnigentAgentId;

}
