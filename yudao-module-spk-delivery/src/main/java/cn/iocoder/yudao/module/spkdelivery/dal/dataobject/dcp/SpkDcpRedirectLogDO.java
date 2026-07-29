package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.dcp;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * SPK-OS DCP 回退日志 DO
 *
 * @author SPK-OS
 */
@TableName("spk_dcp_redirect_log")
@KeySequence("spk_dcp_redirect_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkDcpRedirectLogDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * BPM 流程实例 id
     */
    private String instanceId;
    /**
     * DCP 标识 cdc/pdc/adc/ldc
     */
    private String dcp;
    /**
     * 已回退次数
     */
    private Integer redirectCount;
    /**
     * 回退目标节点 key
     */
    private String targetNode;

}
