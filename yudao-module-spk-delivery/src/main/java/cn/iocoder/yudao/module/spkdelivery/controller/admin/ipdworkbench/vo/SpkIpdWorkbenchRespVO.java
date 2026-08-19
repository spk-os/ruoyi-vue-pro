package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * SPK-OS IPD 指挥工作台 Response VO（设计文档 §7.5 / §10.9）。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 指挥工作台 Response VO")
@Data
public class SpkIpdWorkbenchRespVO {

    @Schema(description = "上下文摘要")
    private Map<String, Object> context;
    @Schema(description = "下一门禁")
    private String nextGate;
    @Schema(description = "刷新时间")
    private String refreshedAt;
    @Schema(description = "行动队列（统一行动项）")
    private List<InboxItem> inbox;
    @Schema(description = "阶段/任务看板列")
    private List<BoardColumn> board;

    // ==================== v4.0 三视图扩展（§4.4 任务/指挥/团队）====================

    @Schema(description = "任务视图：全状态扁平任务列表（按项目→流程分组前端聚合）")
    private List<TaskRow> tasks;
    @Schema(description = "任务状态统计：queued/running/failed/done/cancelled/timeout 计数")
    private Map<String, Long> taskStats;
    @Schema(description = "指挥视图 KPI：running/queued/todayDone/failureRate/avgDurationSec/agentUtilization")
    private Map<String, Object> kpis;
    @Schema(description = "全局执行参数：adapter/fastMode/defaultMode/omnigentTimeoutMs/pollIntervalMs（真实配置只读快照）")
    private Map<String, Object> globalParams;
    @Schema(description = "最近事件流（跨 FlowRun 时间线，复用 recent-activities）")
    private List<Map<String, Object>> eventStream;

    @Data
    public static class TaskRow {
        @Schema(description = "Activity 运行实例编号（task_contract.activity_run_id）")
        private String activityRunId;
        @Schema(description = "任务名（activity_def.name，缺省 activityId）")
        private String name;
        @Schema(description = "所属项目 ID")
        private Long projectId;
        @Schema(description = "所属项目名")
        private String projectName;
        @Schema(description = "所属流程 ID")
        private Long flowRunId;
        @Schema(description = "所属流程编号")
        private String flowRunNo;
        @Schema(description = "当前阶段（SpkStageResolver 读端算）")
        private String stage;
        @Schema(description = "执行者类型 HUMAN/AGENT/SQUAD/SYSTEM")
        private String ownerType;
        @Schema(description = "执行者名（Agent 编号或人名）")
        private String ownerName;
        @Schema(description = "状态 queued/running/done/failed/timeout/cancelled")
        private String status;
        @Schema(description = "进度百分比 0-100（按产物计数推算，无产物则 0/100 二态）")
        private Integer progress;
        @Schema(description = "已运行时长（秒，finished-now 或 started-now）")
        private Long durationSec;
        @Schema(description = "到期时间")
        private String dueAt;
        @Schema(description = "Omnigent 会话 ID（若有，用于 iframe 嵌入）")
        private String sessionId;
    }

    @Data
    public static class InboxItem {
        @Schema(description = "MY_TODO/AGENT_FAILURE/DCP_TR/EVIDENCE_GAP/SYNC_FAILURE/BLOCKED_FLOW")
        private String type;
        @Schema(description = "P0/P1/P2/P3 或 CRITICAL/WARN/INFO")
        private String severity;
        private String title;
        private String detail;
        @Schema(description = "APPROVAL/CONTRACT/FLOW_RUN/ISSUE/GATE")
        private String refType;
        private String refId;
        private Long flowRunId;
        private Long projectId;
        @Schema(description = "候选动作 APPROVE/RETRY/UNBLOCK/REASSIGN...")
        private String action;
    }

    @Data
    public static class BoardColumn {
        @Schema(description = "阶段名")
        private String stage;
        @Schema(description = "阶段工作项")
        private List<BoardItem> items;
    }

    @Data
    public static class BoardItem {
        private String id;
        private String name;
        private String status;
        private String ownerType; // HUMAN/AGENT/SQUAD/SYSTEM
        private String ownerName;
        private String itemType;  // ACTIVITY/ISSUE/APPROVAL
        private Long flowRunId;
        private Long projectId;
        private String dueAt;
    }
}
