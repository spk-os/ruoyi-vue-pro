package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IPD 问题 登记 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 问题登记 Request VO")
@Data
public class SpkIpdIssueCaseCreateReqVO {

    @Schema(description = "项目 ID（由路径 /projects/{projectId}/issues 注入，无需前端传）")
    private Long projectId;

    @Schema(description = "问题类型 DEFECT/INCIDENT/CUSTOMER_ISSUE/TECH_DEBT/SECURITY/COMPLIANCE/CHANGE_REQUEST", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "issueType 不能为空")
    private String issueType;

    @Schema(description = "严重度 P0/P1/P2/P3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "severity 不能为空")
    private String severity;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "来源 MANUAL/PLANE/GITEA/MONITOR/AGENT")
    private String source;

    @Schema(description = "外部系统")
    private String externalSystem;

    @Schema(description = "外部 ID")
    private String externalId;

    @Schema(description = "外部 URL")
    private String externalUrl;

    @Schema(description = "责任人")
    private Long ownerUserId;

    @Schema(description = "发现时间")
    private LocalDateTime detectedAt;
}
