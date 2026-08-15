package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SPK-OS Agent 任务执行实例 DO
 * <p>
 * 记录 agent 节点派发的执行实例、状态、result 产物，关联 BPM 流程实例与节点。
 *
 * @author SPK-OS
 */
@TableName("spk_agent_task")
@KeySequence("spk_agent_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkAgentTaskDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * ActivityRun id（Cortext-IPD §4.6，一次 Activity 执行的唯一标识）
     */
    private String activityRunId;
    /**
     * Task Contract id
     */
    private String contractId;
    /**
     * Activity 定义 id
     */
    private String activityId;
    /**
     * 尝试次数（重试递增）
     */
    private Integer attemptNo;
    /**
     * 认领租约 id（fencing token 防陈旧写入）
     */
    private String claimId;
    /**
     * Fencing token（单调递增）
     */
    private Long fencingToken;
    /**
     * 父 activity_run_id，Worker 用
     */
    private String workerOf;
    /**
     * 业务 taskId（外部 runtime 的任务 id）
     */
    private String taskId;
    /**
     * agent 角色 id（对应 AiChatRoleDO id）
     */
    private Long roleId;
    /**
     * 会话 id（AiChatConversation id）
     */
    private Long conversationId;
    /**
     * 派发 prompt
     */
    private String prompt;
    /**
     * 状态
     * <p>
     * 枚举 running/done/failed/cancelled
     */
    private String status;
    /**
     * agent 产物（JSON）
     */
    private String result;
    /**
     * 产物 URI 列表 JSON（Artifact Registry 链接）
     */
    private String artifactUris;
    /**
     * RunReceipt id
     */
    private String runReceiptId;
    /**
     * 验证结论 PASS/CONDITIONAL/FAIL
     */
    private String verificationConclusion;
    /**
     * BPM 流程实例 id
     */
    private String instanceId;
    /**
     * BPM 节点 key
     */
    private String nodeKey;
    /**
     * 紧随 agent 节点的 receiveTask key（用于回调 triggerTask 推进）
     */
    private String receiveTaskKey;
    /**
     * TTL 过期时间（毫秒时间戳，0 表示无）
     */
    private Long ttlExpireTime;

    // ==================== 运行负载聚合字段（§7.1 第 3 视图） ====================
    /**
     * 智能体定义 ID（聚合维度，dispatch 时回填）
     */
    private Long agentDefId;
    /**
     * 小队 ID（聚合维度，dispatch 时回填）
     */
    private Long squadId;
    /**
     * 实际开始执行时间
     */
    private LocalDateTime startedAt;
    /**
     * 实际结束时间
     */
    private LocalDateTime finishedAt;
    /**
     * 本次运行消耗 token 数（运行时回填，成本聚合）
     */
    private Integer costTokens;

}
