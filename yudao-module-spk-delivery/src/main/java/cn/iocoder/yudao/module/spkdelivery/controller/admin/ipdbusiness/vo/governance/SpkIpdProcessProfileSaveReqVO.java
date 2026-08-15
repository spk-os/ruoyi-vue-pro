package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 流程模板 Profile 保存 Req。设计文档 §7.2 / §9.5.1。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 流程模板 Profile 保存 Req")
@Data
public class SpkIpdProcessProfileSaveReqVO {

    @Schema(description = "主键，更新时必填", example = "7001")
    private Long id;

    @Schema(description = "模板编码，唯一", requiredMode = Schema.RequiredMode.REQUIRED, example = "FULL_RELEASE_V1")
    @NotBlank(message = "Profile 编码不能为空")
    private String profileCode;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板名称不能为空")
    private String name;

    @Schema(description = "流程类型 FULL_RELEASE/INCREMENT_RELEASE/ISSUE_RESOLUTION", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "流程类型不能为空")
    private String flowType;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态 DRAFT/PUBLISHED/DEPRECATED", example = "DRAFT")
    private String status;
}
