package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 裁剪规则保存 Req。设计文档 §9.5.3 / §6.3。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 裁剪规则保存 Req")
@Data
public class SpkIpdTrimRuleSaveReqVO {

    @Schema(description = "主键，更新时必填")
    private Long id;

    @Schema(description = "Profile 版本 id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Profile 版本 id 不能为空")
    private Long profileVersionId;

    @Schema(description = "阶段；与 activityDefId 二选一")
    private String stage;

    @Schema(description = "Activity 定义 id；NULL 表示整阶段裁剪")
    private String activityDefId;

    @Schema(description = "裁剪条件", example = "flowType=INCREMENT_RELEASE")
    private String trimCondition;

    @Schema(description = "动作 SKIP/OPTIONAL/SIMPLIFY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "裁剪动作不能为空")
    private String action;

    @Schema(description = "理由")
    private String reason;
}
