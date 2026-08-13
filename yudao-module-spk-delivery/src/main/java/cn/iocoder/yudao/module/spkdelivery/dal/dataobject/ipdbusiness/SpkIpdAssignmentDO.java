package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * IPD 任务分派 DO
 * <p>
 * 将流程活动、Plane issue、BPM task 或 Agent task 分派给执行者（人/Agent/编队）。
 * 设计文档 §3.3 / §9.5。只做分派关系，不复制源任务正文。
 * 一个任务必须有唯一 accountableActor；BPM 审批任务只允许 HUMAN（Agent 不能伪装系统用户完成 DCP）。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_assignment")
@KeySequence("spk_ipd_assignment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdAssignmentDO extends BaseDO {

    @TableId
    private Long id;
    private Long projectId;
    private Long versionId;
    private Long flowRunId;
    /** 活动 run 号，可空 */
    private String activityRunId;
    /** ACTIVITY/PLANE_ISSUE/BPM_TASK/AGENT_TASK */
    private String workItemType;
    private String workItemId;
    /** 执行者类型 */
    private String actorType;
    private Long actorId;
    /** 唯一责任人 */
    private String accountableActorType;
    private Long accountableActorId;
    /** PLANNED/ASSIGNED/ACCEPTED/IN_PROGRESS/BLOCKED/DONE/CANCELLED */
    private String status;
    private BigDecimal plannedEffort;
    private BigDecimal actualEffort;
    private Integer lockVersion;
}
