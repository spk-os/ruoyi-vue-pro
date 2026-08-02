package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 智能体唤醒（单轮对话）Response VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体唤醒 Response VO")
@Data
public class SpkAgentDefWakeRespVO {

    @Schema(description = "智能体编号")
    private Long id;
    @Schema(description = "智能体名")
    private String name;
    @Schema(description = "会话 id")
    private Long conversationId;
    @Schema(description = "回复内容")
    private String content;
    @Schema(description = "状态")
    private String status;

}
