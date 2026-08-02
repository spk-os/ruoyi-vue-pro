package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 智能体编队 唤醒 Request VO
 *
 * <p>对编队内全部成员按 sortOrder 顺序串行执行单轮对话。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体编队唤醒 Request VO")
@Data
public class SpkAgentSquadWakeReqVO {

    @Schema(description = "编队编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编队编号不能为空")
    private Long id;

    @Schema(description = "对话消息", requiredMode = Schema.RequiredMode.REQUIRED, example = "请评审这段代码")
    @NotEmpty(message = "对话消息不能为空")
    private String message;

}
