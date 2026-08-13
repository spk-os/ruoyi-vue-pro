package cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * IPD 项目 DO
 * <p>
 * 4 级业务模型的顶层容器，跨多个大版本/增量/问题。设计文档 §9.3.1。
 * status: DRAFT/ACTIVE/PAUSED/ARCHIVED；health 由聚合规则计算，永不直接涂色。
 *
 * @author SPK-OS
 */
@TableName("spk_ipd_project")
@KeySequence("spk_ipd_project_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpkIpdProjectDO extends BaseDO {

    @TableId
    private Long id;
    /** 展示编号 PRJ-2026-0012，租户内唯一 */
    private String projectNo;
    /** 稳定短码，租户内唯一，创建后不可改 */
    private String projectCode;
    private String name;
    private String description;
    /** 成功目标 */
    private String objective;
    /** 项目负责人，引用系统用户 */
    private Long ownerUserId;
    /** DRAFT/ACTIVE/PAUSED/ARCHIVED */
    private String status;
    /** UNKNOWN/GOOD/WARN/CRITICAL */
    private String health;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    /** 当前主版本快捷引用，不作为唯一事实源 */
    private Long currentMajorReleaseId;
    /** 乐观锁版本 */
    private Integer lockVersion;
}
