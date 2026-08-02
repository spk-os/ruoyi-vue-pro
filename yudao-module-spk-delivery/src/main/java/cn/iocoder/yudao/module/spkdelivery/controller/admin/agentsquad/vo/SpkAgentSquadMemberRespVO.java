package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 智能体编队成员 Response VO（含智能体快照信息）
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体编队成员 Response VO")
@Data
public class SpkAgentSquadMemberRespVO {

    @Schema(description = "成员编号")
    private Long id;
    @Schema(description = "编队 id")
    private Long squadId;
    @Schema(description = "智能体 id")
    private Long agentId;
    @Schema(description = "在编队中的角色")
    private String role;
    @Schema(description = "顺序")
    private Integer sortOrder;
    @Schema(description = "智能体名")
    private String agentName;
    @Schema(description = "智能体编码")
    private String agentCode;
    @Schema(description = "智能体角色")
    private String agentRole;
    @Schema(description = "智能体状态")
    private String agentStatus;
    @Schema(description = "运行时类型")
    private String agentRuntimeType;

}
