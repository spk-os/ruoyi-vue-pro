package cn.iocoder.yudao.module.spkdelivery.controller.admin.agentdef.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 智能体定义 创建 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 智能体定义创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SpkAgentDefCreateReqVO extends SpkAgentDefBaseVO {

}
