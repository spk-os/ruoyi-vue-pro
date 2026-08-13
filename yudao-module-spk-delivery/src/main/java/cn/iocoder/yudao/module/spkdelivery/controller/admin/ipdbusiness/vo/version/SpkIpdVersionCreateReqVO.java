package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.version;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IPD 交付版本 创建 Request VO（增量/热修版本；服务端分配或校验 minorNo）
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 交付版本创建 Request VO")
@Data
public class SpkIpdVersionCreateReqVO {

    @Schema(description = "版本类型 BASELINE/INCREMENT/HOTFIX", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "versionType 不能为空")
    private String versionType;

    @Schema(description = "小版本序号 ss，可空（由服务端分配）")
    private Integer minorNo;

    @Schema(description = "版本主题")
    private String name;

    @Schema(description = "目标", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标不能为空")
    private String objective;

    @Schema(description = "范围摘要")
    private String scopeSummary;

    @Schema(description = "基于哪个已发布版本")
    private Long sourceVersionId;

    @Schema(description = "版本负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "负责人不能为空")
    private Long ownerUserId;

    @Schema(description = "计划开始时间")
    private LocalDateTime plannedStartAt;

    @Schema(description = "计划结束时间")
    private LocalDateTime plannedEndAt;
}
