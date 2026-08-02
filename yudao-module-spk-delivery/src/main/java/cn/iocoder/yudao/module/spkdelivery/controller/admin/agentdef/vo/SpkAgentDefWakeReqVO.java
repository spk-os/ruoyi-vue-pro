package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 智能体定义 唤醒（单轮对话）Request VO
 *
 * <p>本地实现，不依赖 openclaw：经 yudao-module-ai 内核走 role → conversation → sendMessage。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体唤醒（单轮对话）Request VO")
@Data
public class SpkAgentDefWakeReqVO {

    @Schema(description = "智能体编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "智能体编号不能为空")
    private Long id;

    @Schema(description = "对话消息", requiredMode = Schema.RequiredMode.REQUIRED, example = "请评审这段代码")
    @NotEmpty(message = "对话消息不能为空")
    private String message;

}
