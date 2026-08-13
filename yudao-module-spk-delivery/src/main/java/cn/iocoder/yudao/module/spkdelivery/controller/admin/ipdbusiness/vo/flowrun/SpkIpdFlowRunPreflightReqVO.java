package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.flowrun;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * IPD FlowRun 预检/创建 Request VO。设计文档 §10.5。
 * 包含版本、类型、档案、裁剪项。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD FlowRun 预检/创建 Request VO")
@Data
public class SpkIpdFlowRunPreflightReqVO {

    @Schema(description = "项目 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "projectId 不能为空")
    private Long projectId;

    @Schema(description = "版本 ID（FULL_RELEASE/INCREMENT_RELEASE 必填）")
    private Long versionId;

    @Schema(description = "问题 ID（ISSUE_RESOLUTION 必填）")
    private Long issueCaseId;

    @Schema(description = "流程类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "flowType 不能为空")
    private String flowType;

    @Schema(description = "流程档案 ID")
    private Long processProfileId;

    @Schema(description = "裁剪项")
    private Tailoring tailoring;

    @Data
    public static class Tailoring {
        @Schema(description = "架构模式 REFRESH 等")
        private String architectureMode;
        @Schema(description = "终点门禁 TR5 等")
        private String endGate;
        @Schema(description = "跳过的 Activity")
        private java.util.List<String> skipActivities;
        @Schema(description = "裁剪理由")
        private String reason;
    }
}
