package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.feedback;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * SPK-OS R7 反馈 DO
 *
 * @author SPK-OS
 */
@TableName("spk_feedback")
@KeySequence("spk_feedback_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkFeedbackDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * BPM 流程实例 id
     */
    private String instanceId;
    /**
     * 反馈来源
     */
    private String source;
    /**
     * 反馈内容
     */
    private String content;
    /**
     * 反馈摘要（JSON）
     */
    private String summary;
    /**
     * 是否作为新章程种子
     */
    private Boolean newCharterSeed;

}
