package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * IPD 项目参与者 分页 Request VO
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 项目参与者分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SpkIpdProjectActorPageReqVO extends PageParam {

    @Schema(description = "项目 ID")
    private Long projectId;

    @Schema(description = "版本作用域")
    private Long versionId;

    @Schema(description = "参与者类型 HUMAN/AGENT/SQUAD")
    private String actorType;

    @Schema(description = "参与者 ID")
    private Long actorId;

    @Schema(description = "业务角色")
    private String businessRole;

    @Schema(description = "状态")
    private String status;
}
