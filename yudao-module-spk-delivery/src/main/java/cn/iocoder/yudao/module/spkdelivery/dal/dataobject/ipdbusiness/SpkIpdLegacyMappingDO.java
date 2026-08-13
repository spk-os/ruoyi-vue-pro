package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 旧实例到新业务对象的可审计映射 DO。设计文档 §9.6 / §12.9。
 * legacy_type: PROCESS_INSTANCE/ARTIFACT/CONTRACT 等；mapping_status: NEEDS_MAPPING/MAPPED/UNMAPPABLE。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_legacy_mapping")
@KeySequence("spk_ipd_legacy_mapping_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdLegacyMappingDO extends BaseDO {

    @TableId
    private Long id;
    private String legacyType;
    private String legacyId;
    private Long projectId;
    private Long versionId;
    private Long flowRunId;
    private String mappingStatus;
    private String ruleVersion;
    private String confidence;
    private String evidenceJson;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
}
