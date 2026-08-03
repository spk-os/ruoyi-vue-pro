package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Task Contract DO —— Task Router 与 Lead Agent 之间的执行合同
 * <p>
 * 承载一次 ActivityRun 的路由判决结果：Lead 选择、Worker/Verifier 判决、模型快照、
 * 上下文 URI、超时/重试策略。依 SPK-OS-Cortext-IPD.md §4.2。
 *
 * @author SPK-OS
 */
@TableName("spk_task_contract")
@KeySequence("spk_task_contract_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkTaskContractDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 合同 UUID
     */
    private String contractId;
    /**
     * ActivityRun 实例 id（一次 Activity 执行的唯一标识）
     */
    private String activityRunId;
    private String activityId;
    private String activityVersion;
    /**
     * Flowable 流程实例 id
     */
    private String processInstanceId;
    /**
     * Flowable task id
     */
    private String taskId;
    /**
     * 业务 key（ipd_project_id）
     */
    private String businessKey;
    /**
     * 阶段
     */
    private String phase;
    /**
     * BPM 节点 key
     */
    private String nodeKey;
    /**
     * 执行模式 task_system/lead_internal
     */
    private String executionMode;
    /**
     * Lead Agent id（spk_agent_def.id）
     */
    private Long leadAgentId;
    private String leadAgentCode;
    /**
     * 是否需要 Worker（0 否 1 是）
     */
    private Integer workerRequired;
    private Long workerSquadId;
    /**
     * 是否需要 Verifier（0 否 1 是）
     */
    private Integer verifierRequired;
    private String verifierType;
    private String modelSnapshotId;
    private String contextManifestUri;
    /**
     * 输入引用 JSON
     */
    private String inputRefs;
    /**
     * 输出规格 JSON
     */
    private String outputSpec;
    private Integer timeoutSeconds;
    /**
     * 重试策略 JSON
     */
    private String retryPolicy;
    /**
     * 派发 prompt
     */
    private String prompt;
    /**
     * 状态 queued/running/done/failed/timeout/cancelled
     */
    private String status;
    private LocalDateTime queuedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String failureReason;

}
