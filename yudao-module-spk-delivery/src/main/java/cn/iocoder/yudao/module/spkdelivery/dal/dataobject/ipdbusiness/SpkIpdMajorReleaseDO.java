package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 大版本 Vx DO。设计文档 §9.3.2。
 * status: PLANNING/ACTIVE/MAINTENANCE/CLOSED/CANCELLED。
 * 一个大版本只有一个 baseline version（Vx.0）。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_major_release")
@KeySequence("spk_ipd_major_release_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdMajorReleaseDO extends BaseDO {

    @TableId
    private Long id;
    private Long projectId;
    /** 大版本序号 x */
    private Integer majorNo;
    /** 展示值 V2 */
    private String versionLabel;
    private String name;
    private String objective;
    private String scopeSummary;
    private Long ownerUserId;
    private String status;
    /** 完整流程对应的 Vx.0；启动前可空 */
    private Long baselineVersionId;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    private Integer lockVersion;
}
