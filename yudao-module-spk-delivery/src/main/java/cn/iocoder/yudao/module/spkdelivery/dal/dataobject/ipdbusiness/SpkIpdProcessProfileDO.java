package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 流程模板 Profile DO。设计文档 §9.5.1 / §7.2 第 4 顶层表面。
 * <p>
 * 管理员配置产物（非运行数据）。一个 flow_type 对应一个已发布 Profile；
 * current_version 指向 {@link SpkIpdProcessProfileVersionDO} 当前发布版本。
 * status: DRAFT/PUBLISHED/DEPRECATED。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_process_profile")
@KeySequence("spk_ipd_process_profile_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdProcessProfileDO extends BaseDO {

    @TableId
    private Long id;
    /** 模板编码，如 FULL_RELEASE_V1；唯一 */
    private String profileCode;
    private String name;
    /** FULL_RELEASE/INCREMENT_RELEASE/ISSUE_RESOLUTION */
    private String flowType;
    private String description;
    private String status;
    /** 当前发布版本号，指向 profile_version.version */
    private Integer currentVersion;
    private String publishedBy;
    private LocalDateTime publishedAt;
    private Integer lockVersion;
}
