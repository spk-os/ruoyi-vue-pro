package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IPD 项目 创建 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 项目创建 Request VO")
@Data
public class SpkIpdProjectCreateReqVO {

    @Schema(description = "稳定短码，创建后不可改", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目短码不能为空")
    private String projectCode;

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目名称不能为空")
    private String name;

    @Schema(description = "背景和边界")
    private String description;

    @Schema(description = "成功目标", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "成功目标不能为空")
    private String objective;

    @Schema(description = "项目负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "负责人不能为空")
    private Long ownerUserId;

    @Schema(description = "计划开始时间")
    private LocalDateTime plannedStartAt;

    @Schema(description = "计划结束时间")
    private LocalDateTime plannedEndAt;
}
