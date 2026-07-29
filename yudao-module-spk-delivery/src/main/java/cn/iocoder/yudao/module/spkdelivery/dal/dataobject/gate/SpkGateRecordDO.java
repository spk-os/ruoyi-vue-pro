package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.gate;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SPK-OS 门禁回调审计 DO
 * <p>
 * 记录 G1-G8 / TR2-6 门禁 CI 回调的 report 与 pass 结论。
 *
 * @author SPK-OS
 */
@TableName("spk_gate_record")
@KeySequence("spk_gate_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkGateRecordDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * BPM 流程实例 id
     */
    private String instanceId;
    /**
     * BPM 节点 key
     */
    private String nodeKey;
    /**
     * 门禁标识 g1..g8 / tr2..tr6
     */
    private String gate;
    /**
     * 门禁报告（JSON）
     */
    private String report;
    /**
     * 是否通过
     */
    private Boolean pass;
    /**
     * 回调时间
     */
    private LocalDateTime callbackTime;

}
