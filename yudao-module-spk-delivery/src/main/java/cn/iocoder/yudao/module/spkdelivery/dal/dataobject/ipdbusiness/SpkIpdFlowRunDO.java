package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 流程运行 FlowRun DO。设计文档 §9.4.2。
 * <p>
 * FlowRun 是业务运行，process_instance_id 是引擎运行；一条 FlowRun 最多绑定一个 Flowable 实例。
 * business_key = IPD:{runNo}。三类流程：FULL_RELEASE/INCREMENT_RELEASE/ISSUE_RESOLUTION。
 * status: DRAFT/READY/STARTING/RUNNING/COMPLETED/CANCELLED/FAILED/SUPERSEDED/BLOCKED（详见 §11.4）。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_flow_run")
@KeySequence("spk_ipd_flow_run_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdFlowRunDO extends BaseDO {

    @TableId
    private Long id;
    /** 展示编号 FR-20260813-000042 */
    private String runNo;
    private Long projectId;
    /** FULL_RELEASE/INCREMENT_RELEASE 必填；ISSUE_RESOLUTION 可在分诊时为空 */
    private Long majorReleaseId;
    private Long versionId;
    /** 问题流必填，其余为空 */
    private Long issueCaseId;
    /** FULL_RELEASE/INCREMENT_RELEASE/ISSUE_RESOLUTION */
    private String flowType;
    private Long profileId;
    private Integer profileVersion;
    /** 定义、角色、表单等不可变快照（JSON 文本） */
    private String profileSnapshotJson;
    /** 本次裁剪、理由、批准人和必需证据（JSON 文本） */
    private String tailoringSnapshotJson;
    /** IPD:{runNo} */
    private String businessKey;
    /** 启动成功后回填，唯一 */
    private String processInstanceId;
    private String status;
    private String currentStage;
    private String currentActivity;
    private String health;
    private String blockReason;
    /** 同一业务目标、同类流程的尝试次数 */
    private Integer attemptNo;
    /** 重跑替代关系 */
    private Long supersedesFlowRunId;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime lastEngineSyncAt;
    private Integer lockVersion;
}
