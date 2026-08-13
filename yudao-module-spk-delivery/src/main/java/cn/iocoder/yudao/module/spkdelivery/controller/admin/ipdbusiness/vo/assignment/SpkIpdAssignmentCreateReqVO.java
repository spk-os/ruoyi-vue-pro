package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * IPD 任务分派 新建 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 任务分派新建 Request VO")
@Data
public class SpkIpdAssignmentCreateReqVO {

    @Schema(description = "项目 ID")
    private Long projectId;

    @Schema(description = "版本 ID")
    private Long versionId;

    @Schema(description = "流程运行 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "flowRunId 不能为空")
    private Long flowRunId;

    @Schema(description = "活动 run 号")
    private String activityRunId;

    @Schema(description = "工作项类型 ACTIVITY/PLANE_ISSUE/BPM_TASK/AGENT_TASK", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "workItemType 不能为空")
    private String workItemType;

    @Schema(description = "工作项 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "workItemId 不能为空")
    private String workItemId;

    @Schema(description = "执行者类型 HUMAN/AGENT/SQUAD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "actorType 不能为空")
    private String actorType;

    @Schema(description = "执行者 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "actorId 不能为空")
    private Long actorId;

    @Schema(description = "唯一责任人类型（BPM 审批任务必须 HUMAN）")
    private String accountableActorType;

    @Schema(description = "唯一责任人 ID")
    private Long accountableActorId;

    @Schema(description = "计划工时")
    private BigDecimal plannedEffort;

    @Schema(description = "实际工时")
    private BigDecimal actualEffort;
}
