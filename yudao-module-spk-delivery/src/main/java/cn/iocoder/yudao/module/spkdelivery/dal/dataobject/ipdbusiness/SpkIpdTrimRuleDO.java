package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * IPD 裁剪规则 DO。设计文档 §9.5.3 / §6.3 裁剪。
 * <p>
 * 绑定 ProfileVersion。按 stage/activity_def_id 定义裁剪条件与动作。
 * action: SKIP/OPTIONAL/SIMPLIFY。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_trim_rule")
@KeySequence("spk_ipd_trim_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdTrimRuleDO extends BaseDO {

    @TableId
    private Long id;
    private Long profileVersionId;
    private String stage;
    /** 关联 spk_ipd_activity_def.activity_id；NULL 表示整阶段裁剪 */
    private String activityDefId;
    private String trimCondition;
    private String action;
    private String reason;
    private Integer lockVersion;
}
