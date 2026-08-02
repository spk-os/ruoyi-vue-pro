package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 智能体编队 唤醒 Response VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体编队唤醒 Response VO")
@Data
public class SpkAgentSquadWakeRespVO {

    @Schema(description = "编队编号")
    private Long id;
    @Schema(description = "编队名称")
    private String name;
    @Schema(description = "各成员唤醒结果")
    private List<MemberWake> results;

    @Data
    public static class MemberWake {
        private Long agentId;
        private String agentName;
        private Long conversationId;
        private String content;
        private String status;
        private String error;
    }

}
