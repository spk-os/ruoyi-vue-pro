package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 智能体编队 更新 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体编队更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SpkAgentSquadUpdateReqVO extends SpkAgentSquadBaseVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编号不能为空")
    private Long id;

}
