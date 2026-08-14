package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkitem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Plane 工作项 VO（设计文档 §10.7）。
 *
 * @author SPK-OS
 */
public class SpkIpdWorkItemVO {

    @Schema(description = "工作项快照响应")
    @Data
    public static class RespVO {
        private String planeIssueId;
        private String planeIssueSeq;
        private String name;
        private String linkType;
        private String syncStatus;
        private Long versionId;
        private Long flowRunId;
        private String activityCode;
        private LocalDateTime lastSyncedAt;
        @Schema(description = "是否已绑定")
        private Boolean linked;
    }

    @Schema(description = "工作项绑定请求")
    @Data
    public static class LinkReqVO {
        @Schema(description = "Plane issue id", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Plane issue id 不能为空")
        private String planeIssueId;
        private String planeIssueSeq;
        private String name;
        @Schema(description = "REQUIREMENT/TASK/DEFECT/MILESTONE")
        private String linkType;
        private Long versionId;
        private Long flowRunId;
        private String activityCode;
    }

    @Schema(description = "异步同步响应")
    @Data
    public static class SyncRespVO {
        private Long commandId;
        private String status;
        private Integer syncedCount;
        private LocalDateTime syncedAt;
    }
}
