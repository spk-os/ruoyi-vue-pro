package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.version;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IPD 交付版本 更新 Request VO（范围/计划/负责人，要求乐观锁）
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 交付版本更新 Request VO")
@Data
public class SpkIpdVersionUpdateReqVO {

    @Schema(description = "版本主题")
    private String name;

    @Schema(description = "目标")
    private String objective;

    @Schema(description = "范围摘要")
    private String scopeSummary;

    @Schema(description = "版本负责人")
    private Long ownerUserId;

    @Schema(description = "计划开始时间")
    private LocalDateTime plannedStartAt;

    @Schema(description = "计划结束时间")
    private LocalDateTime plannedEndAt;

    @Schema(description = "乐观锁版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "lockVersion 不能为空")
    private Integer lockVersion;
}
