package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体编队 Response VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体编队 Response VO")
@Data
public class SpkAgentSquadRespVO {

    @Schema(description = "编号")
    private Long id;
    @Schema(description = "编队名称")
    private String name;
    @Schema(description = "编码")
    private String code;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "杂项配置")
    private String config;
    @Schema(description = "成员数量")
    private Integer memberCount;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
