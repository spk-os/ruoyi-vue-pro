package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.majorrelease;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IPD 大版本 创建 Request VO。可同时创建 Vx.0 基线版本草稿。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 大版本创建 Request VO")
@Data
public class SpkIpdMajorReleaseCreateReqVO {

    @Schema(description = "大版本序号 x", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "majorNo 不能为空")
    private Integer majorNo;

    @Schema(description = "大版本主题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "主题不能为空")
    private String name;

    @Schema(description = "目标", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标不能为空")
    private String objective;

    @Schema(description = "范围摘要")
    private String scopeSummary;

    @Schema(description = "大版本负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "负责人不能为空")
    private Long ownerUserId;

    @Schema(description = "是否同时创建 Vx.0 基线版本草稿")
    private Boolean createBaselineVersion;

    @Schema(description = "基线版本计划区间")
    private BaselinePlan baselinePlan;

    @Data
    public static class BaselinePlan {
        @Schema(description = "计划开始时间")
        private LocalDateTime plannedStartAt;
        @Schema(description = "计划结束时间")
        private LocalDateTime plannedEndAt;
    }
}
