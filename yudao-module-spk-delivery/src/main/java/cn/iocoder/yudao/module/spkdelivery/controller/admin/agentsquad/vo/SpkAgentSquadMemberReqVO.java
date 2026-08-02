package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 智能体编队成员 Request VO（新增/更新成员）
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体编队成员 Request VO")
@Data
public class SpkAgentSquadMemberReqVO {

    @Schema(description = "成员编号（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "编队 id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编队 id 不能为空")
    private Long squadId;

    @Schema(description = "智能体 id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "智能体 id 不能为空")
    private Long agentId;

    @Schema(description = "在编队中的角色", example = "主评审")
    private String role;

    @Schema(description = "顺序", example = "1")
    private Integer sortOrder;

}
