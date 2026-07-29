package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.intellect;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * SPK-OS OR 池需求队列 DO
 *
 * @author SPK-OS
 */
@TableName("spk_intellect_queue")
@KeySequence("spk_intellect_queue_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIntellectQueueDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 需求来源
     */
    private String source;
    /**
     * 需求编号
     */
    private String reqId;
    /**
     * 状态 pending/picked/processing/done
     */
    private String status;
    /**
     * 去重哈希
     */
    private String dedupHash;
    /**
     * 载荷（JSON）
     */
    private String payload;

}
