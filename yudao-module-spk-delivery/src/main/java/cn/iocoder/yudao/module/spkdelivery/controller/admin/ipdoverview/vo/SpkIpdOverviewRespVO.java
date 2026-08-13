package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdoverview.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * SPK-OS Cortext-IPD 总览快照 RespVO（设计文档 §9.2 总览 / 诉求 §2 总览统计）。
 * <p>
 * 聚合项目/大版本/交付版本/流程运行/问题/AI 成本六域计数 + 待办 + 路线图，供总览仪表盘首屏渲染。
 * 计数全部走 DB 聚合（selectCount），不依赖内存计数器，避免重启失真。
 *
 * @author SPK-OS
 */
@Schema(description = "SPK IPD 总览快照 Response VO")
@Data
public class SpkIpdOverviewRespVO {

    @Schema(description = "项目计数（按状态）")
    private Map<String, Long> projectCounts;

    @Schema(description = "交付版本计数（按状态）")
    private Map<String, Long> versionCounts;

    @Schema(description = "流程运行计数（按状态）")
    private Map<String, Long> flowRunCounts;

    @Schema(description = "问题计数（按状态）")
    private Map<String, Long> issueCounts;

    @Schema(description = "问题计数（按严重度）")
    private Map<String, Long> issueSeverityCounts;

    @Schema(description = "流程健康计数（HEALTH 维度）")
    private Map<String, Long> flowHealthCounts;

    @Schema(description = "AI 使用与成本聚合（agent_task_total / artifact_total / verification_total / 当前积压）")
    private Map<String, Object> aiUsage;

    @Schema(description = "活跃流程数（status=RUNNING）")
    private Long activeFlowCount;

    @Schema(description = "阻塞流程数（health=BLOCKED 或 status=BLOCKED）")
    private Long blockedFlowCount;

    @Schema(description = "待审问题数（OPEN/REOPENED/TRIAGED）")
    private Long openIssueCount;

    @Schema(description = "待办项（阻断流程/逾期版本/P0-P1 问题/待审决策）")
    private List<AttentionItem> attentionItems;

    @Schema(description = "最近活跃流程（最多 8 条，按启动时间倒序）")
    private List<Map<String, Object>> recentFlows;

    @Schema(description = "活跃项目路线图（最多 6 个 ACTIVE 项目，含大版本/版本骨架）")
    private List<Map<String, Object>> roadmap;

    @Data
    @Schema(description = "待办项")
    public static class AttentionItem {
        private String type;       // BLOCKED_FLOW / OVERDUE_VERSION / SEVERE_ISSUE / PENDING_DECISION
        private String severity;  // P0/P1/P2/P3 或 CRITICAL/WARN
        private Long refId;
        private String title;
        private String detail;
    }
}
