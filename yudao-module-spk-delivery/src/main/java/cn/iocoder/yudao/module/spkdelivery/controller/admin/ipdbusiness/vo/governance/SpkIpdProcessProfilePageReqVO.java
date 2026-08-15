package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程模板 Profile 分页 Req。设计文档 §7.2。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - 流程模板 Profile 分页 Req")
@Data
@EqualsAndHashCode(callSuper = true)
public class SpkIpdProcessProfilePageReqVO extends PageParam {

    @Schema(description = "流程类型")
    private String flowType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "编码/名称模糊")
    private String keyword;
}
