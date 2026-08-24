package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentDefStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SPK-OS 智能体定义 DO
 *
 * <p>对标 Paddock {@code agents} 表：本地智能体定义（不依赖 openclaw）。
 * 与 {@code SpkAgentTaskDO}（任务执行实例）区分：本表是"智能体本身"的定义。
 *
 * @author SPK-OS
 */
@TableName("spk_agent_def")
@KeySequence("spk_agent_def_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkAgentDefDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 智能体名（业务主键，唯一）
     */
    private String name;
    /**
     * 智能体编码（程序引用，唯一）
     */
    private String code;
    /**
     * 智能体种类 lead/worker/verifier（Cortext-IPD §4.6）
     */
    private String agentKind;
    /**
     * Verifier 类型 TR/DCP/Audit，仅 verifier 用
     */
    private String verifierType;
    /**
     * 隔离级别 process/container/vm（P1 默认 process）
     */
    private String isolationLevel;
    /**
     * 角色（自由文本，如"代码评审官"）
     */
    private String role;
    /**
     * 能力标签数组 JSON，Task Router 路由依据（GAP-9）
     */
    private String capabilityTags;
    /**
     * 会话路由标识（可空，本地实现可由 code 派生）
     */
    private String sessionKey;
    /**
     * 灵魂内容 / system prompt（描述性副本；运行时以 role_id 关联的 AiChatRole 为准）
     */
    private String soulContent;
    /**
     * 工作记忆（JSON）
     */
    private String workingMemory;
    /**
     * 状态：offline / idle / busy / error，见 {@link SpkAgentDefStatusEnum}
     */
    private String status;
    /**
     * Agent 执行模型（模型名或 id）。mode=omnigent 时由 Cortex 作为 model_override
     * 显式传给 Omnigent，属于执行配置而非描述性默认值。
     */
    private String model;
    /**
     * 关联 yudao AiChatRoleDO.id，wake 单轮对话经此 role 走 NativeAi 内核
     */
    private Long roleId;
    /**
     * 关联 yudao AiChatConversationDO.id，首次 wake 后复用以携带上下文
     */
    private Long conversationId;
    /**
     * 工具白/黑名单配置（JSON：{@code {"allow":[],"deny":[]}}）
     */
    private String toolsConfig;
    /**
     * 杂项配置（JSON：identity/sandbox 等）
     */
    private String config;
    /**
     * 运行时类型：native / claude / codex / custom
     */
    private String runtimeType;
    /**
     * 来源：manual / template / import
     */
    private String source;
    /**
     * 是否隐藏（0 否，1 是）
     */
    private Integer hidden;
    /**
     * 最近心跳时间
     */
    private LocalDateTime lastSeen;
    /**
     * 最近活动描述
     */
    private String lastActivity;
    /**
     * 执行模式 local/omnigent（per-agent 决定走 NativeAiAdapter 还是 OmnigentAdapter）。
     * 与 executionLocation（系统执行 vs agent 内部执行）正交。默认 local。
     */
    private String mode;
    /**
     * 继承父智能体 id（运行时合并解析：capabilityTags 并集、soulContent/model/mode 子覆盖父）。
     * 为空表示无继承。不能指向自己或子孙（create/update 校验）。
     */
    private Long parentDefId;
    /**
     * Omnigent 侧 agent-id 映射（mode=omnigent 时 OmnigentAdapter 用此值创建会话，
     * 为空回退全局 spk-delivery.omnigent.agent-id）。
     */
    private String omnigentAgentId;

    // ==================== 治理字段（§7.1 / §9.5 治理表面） ====================
    /**
     * 模板版本号（治理产物，发布/回滚引用）
     */
    private Integer version;
    /**
     * 负责人（用户 id 字符串）
     */
    private String owner;
    /**
     * 安全级别 LOW/MEDIUM/HIGH/CRITICAL（影响 isolationLevel 与审批门）
     */
    private String securityLevel;
    /**
     * 生命周期：REGISTER/TRIAL/READY/RETIRE（与运行态 status=idle/busy/error 正交，不破坏现有语义）
     */
    private String lifecycle;
    /**
     * 锁版本（乐观锁，治理发布/回滚用）
     */
    private Integer lockVersion;

}
