package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IPD 证据豁免请求 VO（设计文档 §10.8）
 * <p>
 * 有权限的例外豁免；必须时限、理由、补证责任人。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 证据豁免请求 VO")
@Data
public class SpkIpdEvidenceWaiverReqVO {

    @Schema(description = "缺失证据/产物引用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "证据引用不能为空")
    private String evidenceRef;

    @Schema(description = "类型 ARTIFACT/GATE/TR/TEST/RELEASE")
    private String evidenceType;

    @Schema(description = "补证截止时间")
    private LocalDateTime dueAt;

    @Schema(description = "豁免理由", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "豁免理由不能为空")
    private String reason;

    @Schema(description = "补证责任人用户 id")
    private Long ownerUserId;
}
