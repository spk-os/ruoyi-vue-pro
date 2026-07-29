package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ccb;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * SPK-OS CCB 变更台账 DO
 *
 * @author SPK-OS
 */
@TableName("spk_ccb_record")
@KeySequence("spk_ccb_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkCcbRecordDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 变更编号
     */
    private String changeId;
    /**
     * BPM 流程实例 id
     */
    private String instanceId;
    /**
     * 变更请求（JSON）
     */
    private String changeRequest;
    /**
     * 影响分析（JSON）
     */
    private String impact;
    /**
     * 决议 approve/reject/defer
     */
    private String decision;

}
