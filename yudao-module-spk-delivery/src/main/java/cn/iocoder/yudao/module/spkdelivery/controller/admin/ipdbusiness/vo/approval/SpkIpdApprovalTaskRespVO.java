package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IPD 审批任务 Response VO（设计文档 §10.8）
 * <p>
 * 在原生 BPM 待办/已办基础上增加项目、版本、阶段、门禁、负责人、健康等业务摘要。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 审批任务 Response VO")
@Data
public class SpkIpdApprovalTaskRespVO {

    @Schema(description = "任务编号")
    private String taskId;

    @Schema(description = "任务名")
    private String name;

    @Schema(description = "任务定义 key")
    private String taskDefinitionKey;

    @Schema(description = "流程实例编号")
    private String processInstanceId;

    @Schema(description = "流程定义 id")
    private String processDefinitionId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "结束时间（已办）")
    private LocalDateTime endTime;

    @Schema(description = "审批人用户 id")
    private Long assigneeUserId;

    @Schema(description = "审批人昵称")
    private String assigneeNickname;

    @Schema(description = "委托人用户 id")
    private Long ownerUserId;

    @Schema(description = "委托人昵称")
    private String ownerNickname;

    @Schema(description = "是否挂起")
    private Boolean suspended;

    // ---------- IPD 业务摘要 ----------

    @Schema(description = "项目 id")
    private Long projectId;

    @Schema(description = "项目名")
    private String projectName;

    @Schema(description = "版本 id")
    private Long versionId;

    @Schema(description = "版本标签")
    private String versionLabel;

    @Schema(description = "FlowRun id")
    private Long flowRunId;

    @Schema(description = "运行编号")
    private String runNo;

    @Schema(description = "流程类型")
    private String flowType;

    @Schema(description = "当前阶段")
    private String currentStage;

    @Schema(description = "当前门禁")
    private String currentGate;

    @Schema(description = "健康")
    private String health;

    @Schema(description = "业务 key")
    private String businessKey;

    @Schema(description = "等待时长毫秒")
    private Long waitDurationMs;
}
