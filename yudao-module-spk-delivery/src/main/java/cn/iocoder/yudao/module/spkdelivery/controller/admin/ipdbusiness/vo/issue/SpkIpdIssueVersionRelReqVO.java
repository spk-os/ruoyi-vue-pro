package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * IPD 问题-版本关联 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 问题-版本关联 Request VO")
@Data
public class SpkIpdIssueVersionRelReqVO {

    @Schema(description = "版本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "versionId 不能为空")
    private Long versionId;

    @Schema(description = "关联类型 AFFECTS/FOUND_IN/FIXED_IN/VERIFIED_IN", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "relationType 不能为空")
    private String relationType;
}
