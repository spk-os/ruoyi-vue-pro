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
    /**
     * 项目实际工作区绝对路径。治理 FlowRun 启动时解析并冻结，Omnigent→Claude Code
     * 必须直接在该目录执行；为空时才使用历史的按 run 隔离产物目录。
     */
    private String workspace;
    /** 执行模式 local/omnigent（按 lead.mode 决定走哪个 adapter；与 executionLocation 正交） */
    private String mode;
    /** Omnigent 侧 agent-id（mode=omnigent 时 OmnigentAdapter 用此值，空回退全局配置） */
    private String omnigentAgentId;
    /**
     * Cortex Agent 定义中冻结的模型标识。显式 Omnigent 路由必须把该值作为
     * model_override 传入会话，禁止由 Claude Code/Omnigent 静默选择默认模型。
     */
    private String model;
    /** def 快照（stage/outputArtifactType/prompt 等，adapter 可选读，避免耦合 ActivityDef） */
    private String stage;
    private String outputArtifactType;
    /**
     * Cortex 为本次 ActivityRun 冻结的唯一主产物文件。Omnigent Agent 必须写入该绝对路径，
     * Adapter 在会话终态后按精确路径读取并校验，禁止把过程性 assistant 文本登记为产物。
     */
    private String outputFile;

    // ===== 节点 skill 绑定（Phase1 G：prompt 注入 skill 指令最小闭环） =====

    /** 绑定的 skill 名（如 spk-ipd-concept），来自 activity_def.skills 或 Profile.defaultSkillBindings[stage] */
    private String skillName;
    /** skill 文件路径（如 /root/.claude/skills/spk-ipd-concept/SKILL.md），adapter 可读其内容前置进 prompt */
    private String skillPath;

}
