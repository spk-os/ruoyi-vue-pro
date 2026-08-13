package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.project;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * IPD 项目 分页 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 项目分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SpkIpdProjectPageReqVO extends PageParam {

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "稳定短码")
    private String projectCode;

    @Schema(description = "负责人")
    private Long ownerUserId;

    @Schema(description = "状态 DRAFT/ACTIVE/PAUSED/ARCHIVED")
    private String status;

    @Schema(description = "健康度 UNKNOWN/GOOD/WARN/CRITICAL")
    private String health;
}
