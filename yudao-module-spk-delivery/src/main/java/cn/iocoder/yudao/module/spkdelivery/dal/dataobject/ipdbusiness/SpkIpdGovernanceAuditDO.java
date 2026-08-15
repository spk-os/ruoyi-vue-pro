package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * IPD 治理审计 DO。设计文档 §9.5.6。
 * <p>
 * Profile 发布/回滚、裁剪变更、引擎同步等治理动作的前后态审计（append-only）。
 * action_type: PUBLISH/ROLLBACK/TRIM_CHANGE/ENGINE_SYNC/FAILED_JOB。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_governance_audit")
@KeySequence("spk_ipd_governance_audit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdGovernanceAuditDO extends BaseDO {

    @TableId
    private Long id;
    private String actionType;
    private Long refId;
    private String operatorId;
    private String beforeJson;
    private String afterJson;
    private String remark;
    private Integer lockVersion;
}
