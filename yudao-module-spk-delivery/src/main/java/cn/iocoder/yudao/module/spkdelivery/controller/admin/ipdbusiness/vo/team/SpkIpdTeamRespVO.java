package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.team;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * IPD 团队与智能体统一页 Response VO（设计文档 §5.1 / 诉求团队态势）。
 * <p>
 * 聚合：人员参与关系、Agent 定义、编队、负载与产出（每人/Agent 的任务数/状态）。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 团队与智能体统一页 Response VO")
@Data
public class SpkIpdTeamRespVO {

    @Schema(description = "项目 ID（空=全局）")
    private Long projectId;

    @Schema(description = "版本 ID（空=项目级）")
    private Long versionId;

    @Schema(description = "人员参与关系（含显示名/部门）")
    private List<ActorRow> people;

    @Schema(description = "Agent 参与关系（项目级绑定的 Agent）")
    private List<ActorRow> agents;

    @Schema(description = "编队参与关系")
    private List<ActorRow> squads;

    @Schema(description = "负载与产出：actorType:actorId -> {total,planned,running,blocked,done}")
    private List<LoadRow> load;

    @Schema(description = "汇总")
    private Map<String, Long> summary;

    @Data
    @Schema(description = "参与者行（人/Agent/编队统一）")
    public static class ActorRow {
        private Long id;
        private String actorType;
        private Long actorId;
        private String name;
        private String subtitle;
        private String businessRole;
        private Integer accountableFlag;
        private Integer capacityPct;
        private String status;
        private Long projectId;
        private Long versionId;
    }

    @Data
    @Schema(description = "负载行")
    public static class LoadRow {
        private String actorType;
        private Long actorId;
        private String name;
        private Long total;
        private Long planned;
        private Long running;
        private Long blocked;
        private Long done;
    }
}
