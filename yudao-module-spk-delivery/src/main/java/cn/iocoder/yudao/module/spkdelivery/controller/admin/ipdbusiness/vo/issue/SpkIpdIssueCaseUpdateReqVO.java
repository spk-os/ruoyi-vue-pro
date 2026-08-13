package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * IPD 问题 更新 Request VO（分类、负责人、根因等）
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 问题更新 Request VO")
@Data
public class SpkIpdIssueCaseUpdateReqVO {

    @Schema(description = "问题类型")
    private String issueType;

    @Schema(description = "严重度")
    private String severity;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "责任人")
    private Long ownerUserId;

    @Schema(description = "根因")
    private String rootCause;

    @Schema(description = "结论")
    private String resolution;

    @Schema(description = "乐观锁版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "lockVersion 不能为空")
    private Integer lockVersion;
}
