package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * IPD 问题 分页 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 问题分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SpkIpdIssueCasePageReqVO extends PageParam {

    @Schema(description = "项目 ID")
    private Long projectId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "问题类型 DEFECT/INCIDENT/...")
    private String issueType;

    @Schema(description = "严重度 P0/P1/P2/P3")
    private String severity;

    @Schema(description = "状态")
    private String status;
}
