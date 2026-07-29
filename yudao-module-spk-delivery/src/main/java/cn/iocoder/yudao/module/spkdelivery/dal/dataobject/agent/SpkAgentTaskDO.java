package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

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

}
