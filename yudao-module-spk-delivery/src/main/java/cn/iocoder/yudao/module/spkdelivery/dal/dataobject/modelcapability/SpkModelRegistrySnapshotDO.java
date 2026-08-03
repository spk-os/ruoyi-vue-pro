package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.modelcapability;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Model Registry Snapshot DO —— 每次执行前冻结的模型快照
 * <p>
 * Task Router 在派发前冻结一份 capability→profile 全量 JSON，snapshot_id 写入 TaskContract，
 * 保证一次 ActivityRun 内模型路由一致性。依 §4.5。
 *
 * @author SPK-OS
 */
@TableName("spk_model_registry_snapshot")
@KeySequence("spk_model_registry_snapshot_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkModelRegistrySnapshotDO extends BaseDO {

    @TableId
    private Long id;
    private String snapshotId;
    /**
     * 冻结时刻全量 profile JSON
     */
    private String profileJson;
    private String frozenBy;
    private LocalDateTime validAt;
    private LocalDateTime expiresAt;

}
