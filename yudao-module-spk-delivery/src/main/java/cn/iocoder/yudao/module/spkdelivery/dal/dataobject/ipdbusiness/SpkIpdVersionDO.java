package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 交付版本 Vx.ss DO。设计文档 §9.3.3。
 * version_type: BASELINE/INCREMENT/HOTFIX；baseline_flag=1 当且仅当 minor_no=0。
 * status: DRAFT/READY/RUNNING/RELEASED/CANCELLED（详见 §11.3）。
 * delivery_readiness: NOT_READY/TR5_PASSED/RELEASE_READY，与发布状态分开。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_version")
@KeySequence("spk_ipd_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdVersionDO extends BaseDO {

    @TableId
    private Long id;
    /** 冗余项目键用于权限和查询 */
    private Long projectId;
    private Long majorReleaseId;
    /** 结构化版本号，禁止只存字符串 */
    private Integer majorNo;
    private Integer minorNo;
    /** 规范展示值 V2.3 */
    private String versionNo;
    /** BASELINE/INCREMENT/HOTFIX */
    private String versionType;
    /** 仅 Vx.0 为 1（smallint） */
    private Integer baselineFlag;
    private String name;
    private String objective;
    private String scopeSummary;
    private Long ownerUserId;
    private String status;
    private String deliveryReadiness;
    private String health;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    private LocalDateTime releasedAt;
    /** 基于哪个已发布版本 */
    private Long sourceVersionId;
    private Integer lockVersion;
}
