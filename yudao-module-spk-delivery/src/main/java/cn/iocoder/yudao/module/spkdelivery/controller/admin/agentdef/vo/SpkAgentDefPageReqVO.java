package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 智能体定义 分页 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体定义分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SpkAgentDefPageReqVO extends PageParam {

    @Schema(description = "智能体名")
    private String name;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "状态 offline/idle/busy/error")
    private String status;

    @Schema(description = "运行时类型")
    private String runtimeType;

    @Schema(description = "是否隐藏 0/1")
    private Integer hidden;

}
