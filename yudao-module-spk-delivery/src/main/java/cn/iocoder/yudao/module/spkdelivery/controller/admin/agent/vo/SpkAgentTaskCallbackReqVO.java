package cn.iocoder.yudao.module.spkdelivery.controller.admin.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - SPK Agent 任务回调 Request VO")
@Data
public class SpkAgentTaskCallbackReqVO {

    @Schema(description = "外部 runtime 任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "任务编号不能为空")
    private String taskId;

    @Schema(description = "状态 running/done/failed", requiredMode = Schema.RequiredMode.REQUIRED, example = "done")
    @NotEmpty(message = "状态不能为空")
    private String status;

    @Schema(description = "产物（JSON）")
    private String result;

}
