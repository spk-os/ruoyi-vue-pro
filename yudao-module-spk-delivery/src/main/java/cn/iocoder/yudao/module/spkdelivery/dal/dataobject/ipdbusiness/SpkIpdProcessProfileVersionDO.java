package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD Profile 版本 DO。设计文档 §9.5.2。
 * <p>
 * 版本化不可变快照：发布/回滚/兼容检查。snapshot_json 含阶段序列、门径、默认裁剪、门禁产物。
 * status: DRAFT/PUBLISHED/SUPERSEDED；compatibility_hash 用于版本兼容性比对。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_process_profile_version")
@KeySequence("spk_ipd_process_profile_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdProcessProfileVersionDO extends BaseDO {

    @TableId
    private Long id;
    private Long profileId;
    private Integer version;
    /** 不可变快照（阶段/门径/裁剪/门禁产物 JSON 文本） */
    private String snapshotJson;
    private String status;
    private String compatibilityHash;
    private String publishedBy;
    private LocalDateTime publishedAt;
    /** 被本版本替代的上一版本 id */
    private Long supersedesVersionId;
    private Integer lockVersion;
}
