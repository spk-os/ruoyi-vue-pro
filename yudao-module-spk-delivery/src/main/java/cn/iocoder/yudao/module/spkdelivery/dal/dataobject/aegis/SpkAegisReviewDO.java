package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.aegis;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * SPK-OS Aegis 审查 DO
 * <p>
 * 记录 ADCP 前的同步 LLM 裁决结论、证据与报告。
 *
 * @author SPK-OS
 */
@TableName("spk_aegis_review")
@KeySequence("spk_aegis_review_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkAegisReviewDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 审查编号
     */
    private String reviewId;
    /**
     * BPM 流程实例 id
     */
    private String instanceId;
    /**
     * 裁决结论 pass/fail/conditional
     */
    private String verdict;
    /**
     * 审查报告（JSON）
     */
    private String report;
    /**
     * 证据（JSON）
     */
    private String evidence;
    /**
     * BPM 节点 key
     */
    private String nodeKey;

}
