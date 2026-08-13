package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.flowrun;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * IPD FlowRun 分页 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD FlowRun 分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SpkIpdFlowRunPageReqVO extends PageParam {

    @Schema(description = "项目 ID")
    private Long projectId;

    @Schema(description = "版本 ID")
    private Long versionId;

    @Schema(description = "问题 ID")
    private Long issueCaseId;

    @Schema(description = "流程类型 FULL_RELEASE/INCREMENT_RELEASE/ISSUE_RESOLUTION")
    private String flowType;

    @Schema(description = "运行状态")
    private String status;

    @Schema(description = "当前阶段")
    private String currentStage;
}
