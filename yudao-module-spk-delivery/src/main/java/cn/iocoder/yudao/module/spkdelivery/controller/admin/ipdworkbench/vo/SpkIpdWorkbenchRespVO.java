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
