package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.contract;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * SPK-OS Workflow Contract 版本治理 DO
 * <p>
 * 对 IPD 流程定义 simpleModel 做稳定化哈希与版本 diff，治理流程演进。
 *
 * @author SPK-OS
 */
@TableName("spk_contract_version")
@KeySequence("spk_contract_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkContractVersionDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 流程模型 key
     */
    private String modelKey;
    /**
     * simpleModel 稳定化哈希 SHA256
     */
    private String hash;
    /**
     * 版本号
     */
    private Integer version;
    /**
     * diff（JSON patch，相对上一版本）
     */
    private String diffJson;
    /**
     * simpleModel 快照（JSON）
     */
    private String snapshotJson;

}
