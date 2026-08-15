package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdmonitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * SPK-OS Cortext-IPD 项目维度监控 RespVO（设计文档 §9.3 / 诉求 §4 监控台）。
 * <p>
 * 项目维度聚合：流程列表（含产物/证据计数）+ 汇总 + 集成健康。
 * 复用既有 FlowRun/Artifact/Evidence Mapper 只读查询，不碰 Flowable 运行时。
 *
 * @author SPK-OS
 */
@Schema(description = "SPK IPD 项目维度监控 Response VO")
@Data
public class SpkIpdMonitorRespVO {

    @Schema(description = "汇总计数")
    private Map<String, Long> summary;

    @Schema(description = "流程列表（含每流程产物/证据计数）")
    private List<Map<String, Object>> flows;

    @Schema(description = "集成健康（Plane/Gitea/Omnigent）")
    private List<IntegrationHealth> integrations;

    @Schema(description = "决策记录（项目维度，含审批包哈希/决策结论/审批人）")
    private List<Map<String, Object>> decisions;

    @Schema(description = "门禁记录（项目维度，按 instance 聚合 pass/report）")
    private List<Map<String, Object>> gates;

    @Schema(description = "制品基线（项目维度，含哈希/签名状态/类型/摘要）")
    private List<Map<String, Object>> artifacts;

    @Data
    @Schema(description = "集成健康")
    public static class IntegrationHealth {
        private String name;
        private boolean healthy;
        private String detail;
        private String url;
    }
}
