package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * IPD 任务分派 分页 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 任务分派分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SpkIpdAssignmentPageReqVO extends PageParam {

    @Schema(description = "项目 ID")
    private Long projectId;

    @Schema(description = "版本 ID")
    private Long versionId;

    @Schema(description = "流程运行 ID")
    private Long flowRunId;

    @Schema(description = "工作项类型")
    private String workItemType;

    @Schema(description = "执行者类型")
    private String actorType;

    @Schema(description = "执行者 ID")
    private Long actorId;

    @Schema(description = "状态")
    private String status;
}
