package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 智能体定义 状态变更 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体定义状态变更 Request VO")
@Data
public class SpkAgentDefStatusReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "状态 offline/idle/busy/error", requiredMode = Schema.RequiredMode.REQUIRED, example = "idle")
    @NotEmpty(message = "状态不能为空")
    private String status;

    @Schema(description = "最近活动描述")
    private String lastActivity;

}
