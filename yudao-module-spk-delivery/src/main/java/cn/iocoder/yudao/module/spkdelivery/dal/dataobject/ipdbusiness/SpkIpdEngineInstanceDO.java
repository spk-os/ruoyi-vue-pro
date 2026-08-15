package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 引擎实例档案 DO。设计文档 §9.5.4。
 * <p>
 * Flowable process_instance 与 FlowRun/ProfileVersion 的绑定关系 + 引擎健康度。
 * engine_health: UNKNOWN/HEALTHY/STUCK/FAILED。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_engine_instance")
@KeySequence("spk_ipd_engine_instance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdEngineInstanceDO extends BaseDO {

    @TableId
    private Long id;
    private String processInstanceId;
    private Long flowRunId;
    private Long profileVersionId;
    private String engineHealth;
    private LocalDateTime lastSyncedAt;
    private String metaJson;
    private Integer lockVersion;
}
