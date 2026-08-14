package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * IPD 决策记录 DO —— 一次 APPROVE/REJECT/REDIRECT/RETURN 的不可变审计行。
 * <p>
 * 包装原生 BPM approve/reject/return：先落本记录，再调用 BpmTaskService 对应方法。
 * 设计文档 §10.8「审批与决策包 API」：决策请求写不可变决策并调用 BPM，
 * 若任务已由他人处理返回 IPD_TASK_ALREADY_COMPLETED，不得生成孤立决策。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_decision_record")
@KeySequence("spk_ipd_decision_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdDecisionRecordDO extends BaseDO {

    @TableId
    private Long id;
    /** Flowable task id */
    private String taskId;
    /** Flowable 流程实例 id */
    private String processInstanceId;
    private Long flowRunId;
    private Long projectId;
    private Long versionId;
    /** APPROVE/REJECT/REDIRECT/RETURN */
    private String decision;
    private String reason;
    /** 跟进项 JSON 数组 */
    private String conditionsJson;
    /** sha256:... 决策包快照 hash */
    private String decisionPackageHash;
    /** 写入流程引擎的变量 JSON */
    private String flowableVariablesJson;
    /** REDIRECT/RETURN 目标节点 key */
    private String redirectTargetTaskKey;
    /** 决策人系统用户 id */
    private Long deciderUserId;
    private Integer lockVersion;

}
