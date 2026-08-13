package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * IPD 参与者候选 Response VO（人/Agent/编队统一形态，用于分派下拉）。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 参与者候选 Response VO")
@Data
public class SpkIpdActorCandidateRespVO {

    @Schema(description = "参与者类型 HUMAN/AGENT/SQUAD")
    private String actorType;

    @Schema(description = "参与者 ID")
    private Long actorId;

    @Schema(description = "显示名")
    private String name;

    @Schema(description = "副标题（部门/角色/状态）")
    private String subtitle;

    @Schema(description = "业务角色")
    private String businessRole;
}
