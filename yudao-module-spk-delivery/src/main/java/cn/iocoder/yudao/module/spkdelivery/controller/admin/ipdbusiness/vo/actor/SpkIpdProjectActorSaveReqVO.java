package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IPD 项目参与者 增改 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 项目参与者增改 Request VO")
@Data
public class SpkIpdProjectActorSaveReqVO {

    @Schema(description = "项目 ID（路径注入）", hidden = true)
    private Long projectId;

    @Schema(description = "版本作用域；null=项目级")
    private Long versionId;

    @Schema(description = "参与者类型 HUMAN/AGENT/SQUAD/SYSTEM", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "actorType 不能为空")
    private String actorType;

    @Schema(description = "参与者 ID（用户ID/agentDefId/squadId）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "actorId 不能为空")
    private Long actorId;

    @Schema(description = "业务角色 IPD-PM/PO/ARCH/DEV/QA/RELEASE 等", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "businessRole 不能为空")
    private String businessRole;

    @Schema(description = "是否该作用域该角色唯一 accountable（0/1）")
    private Integer accountableFlag;

    @Schema(description = "容量百分比 0-100")
    private Integer capacityPct;

    @Schema(description = "生效时间")
    private LocalDateTime effectiveFrom;

    @Schema(description = "失效时间")
    private LocalDateTime effectiveTo;

    @Schema(description = "状态 ACTIVE/INACTIVE")
    private String status;
}
