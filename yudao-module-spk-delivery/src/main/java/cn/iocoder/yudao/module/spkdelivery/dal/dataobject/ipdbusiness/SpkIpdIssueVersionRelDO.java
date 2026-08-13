package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * IPD 问题-版本关联 DO。设计文档 §9.3.4。
 * relation_type: AFFECTS/FOUND_IN/FIXED_IN/VERIFIED_IN。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_issue_version_rel")
@KeySequence("spk_ipd_issue_version_rel_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdIssueVersionRelDO extends BaseDO {

    @TableId
    private Long id;
    private Long issueCaseId;
    private Long versionId;
    private String relationType;
}
