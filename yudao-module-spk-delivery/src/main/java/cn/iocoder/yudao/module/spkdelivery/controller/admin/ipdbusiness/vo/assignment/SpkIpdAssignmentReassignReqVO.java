package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * IPD 任务转派 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 任务转派 Request VO")
@Data
public class SpkIpdAssignmentReassignReqVO {

    @Schema(description = "新执行者类型 HUMAN/AGENT/SQUAD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "actorType 不能为空")
    private String actorType;

    @Schema(description = "新执行者 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "actorId 不能为空")
    private Long actorId;

    @Schema(description = "新责任人类型")
    private String accountableActorType;

    @Schema(description = "新责任人 ID")
    private Long accountableActorId;

    @Schema(description = "转派原因")
    private String reason;
}
