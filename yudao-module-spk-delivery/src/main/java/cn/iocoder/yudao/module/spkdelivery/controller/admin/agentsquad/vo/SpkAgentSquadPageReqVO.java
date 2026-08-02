package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentsquad.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 智能体编队 分页 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体编队分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SpkAgentSquadPageReqVO extends PageParam {

    @Schema(description = "编队名称")
    private String name;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "状态 active/disabled")
    private String status;

}
