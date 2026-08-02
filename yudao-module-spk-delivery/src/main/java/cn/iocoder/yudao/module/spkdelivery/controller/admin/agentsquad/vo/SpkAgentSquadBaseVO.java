package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 智能体编队 Base VO
 *
 * @author SPK-OS
 */
@Data
public class SpkAgentSquadBaseVO {

    @Schema(description = "编队名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "IPD 评审编队")
    @NotEmpty(message = "编队名称不能为空")
    @Size(max = 100, message = "编队名称长度不能超过 100")
    private String name;

    @Schema(description = "编队编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ipd-review-squad")
    @NotEmpty(message = "编队编码不能为空")
    @Size(max = 64, message = "编队编码长度不能超过 64")
    private String code;

    @Schema(description = "描述", example = "IPD 流程评审用编队")
    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;

    @Schema(description = "状态 active/disabled", example = "active")
    private String status;

    @Schema(description = "杂项配置 JSON")
    private String config;

}
