package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdactivity.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * IPD Activity 定义 分页 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD Activity 定义分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SpkIpdActivityDefPageReqVO extends PageParam {

    @Schema(description = "Activity 业务标识")
    private String activityId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "阶段 concept/plan/develop/qualify/launch/lifecycle")
    private String stage;

    @Schema(description = "状态 draft/active/deprecated")
    private String status;

    @Schema(description = "Lead Agent 编码")
    private String leadAgentCode;

}
