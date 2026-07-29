package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.sunset;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * SPK-OS R8 退市 DO
 *
 * @author SPK-OS
 */
@TableName("spk_sunset")
@KeySequence("spk_sunset_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkSunsetDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * BPM 流程实例 id
     */
    private String instanceId;
    /**
     * 退市报告（JSON）
     */
    private String sunsetReport;
    /**
     * 归档状态
     */
    private String archiveStatus;

}
