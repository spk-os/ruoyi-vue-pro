package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IPD 项目 更新 Request VO（要求 If-Match lockVersion）
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 项目更新 Request VO")
@Data
public class SpkIpdProjectUpdateReqVO {

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "背景和边界")
    private String description;

    @Schema(description = "成功目标")
    private String objective;

    @Schema(description = "项目负责人")
    private Long ownerUserId;

    @Schema(description = "项目实际交付/开发工作区绝对路径")
    private String deliveryRoot;

    @Schema(description = "计划开始时间")
    private LocalDateTime plannedStartAt;

    @Schema(description = "计划结束时间")
    private LocalDateTime plannedEndAt;

    @Schema(description = "乐观锁版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "lockVersion 不能为空")
    private Integer lockVersion;
}
