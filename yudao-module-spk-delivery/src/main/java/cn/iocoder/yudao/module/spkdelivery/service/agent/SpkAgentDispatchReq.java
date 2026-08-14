package cn.iocoder.yudao.module.spkdelivery.service.agent;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * SPK-OS Agent 任务派发请求
 * <p>
 * 经 FrameworkAdapter 派发，与具体 runtime 解耦。
 *
 * @author SPK-OS
 */
@Data
@Accessors(chain = true)
public class SpkAgentDispatchReq {

    /**
     * agent 角色 id（对应 yudao AiChatRoleDO，或外部 runtime 的 agent 标识）
     */
    private Long roleId;
    /**
     * 派发 prompt
     */
    private String prompt;
    /**
     * BPM 流程实例 id（用于回溯关联）
     */
    private String instanceId;
    /**
     * BPM 节点 key
     */
    private String nodeKey;
    /**
     * 紧随的 receiveTask key（用于回调 triggerTask 推进）
     */
    private String receiveTaskKey;
    /**
     * TTL 过期时间（毫秒时间戳，0 表示无）
     */
    private Long ttlExpireTime;

    // ===== per-agent 路由上下文（让 OmnigentAdapter 拿到 def/lead 维度信息） =====

    /** lead 智能体 code（OmnigentAdapter 据此解析 def.omnigentAgentId） */
    private String leadCode;
    /** lead 智能体 id（adapter 取 def.omnigentAgentId 用） */
    private Long leadDefId;
    /** activityRunId（workspace 隔离路径分段 + 会话续跑 key） */
    private String activityRunId;
    /** 项目 id（workspace 隔离路径分段） */
    private Long projectId;
    /** 版本 id（workspace 隔离路径分段） */
    private Long versionId;
    /** 执行模式 local/omnigent（按 lead.mode 决定走哪个 adapter；与 executionLocation 正交） */
    private String mode;
    /** Omnigent 侧 agent-id（mode=omnigent 时 OmnigentAdapter 用此值，空回退全局配置） */
    private String omnigentAgentId;
    /** def 快照（stage/outputArtifactType/prompt 等，adapter 可选读，避免耦合 ActivityDef） */
    private String stage;
    private String outputArtifactType;

}
