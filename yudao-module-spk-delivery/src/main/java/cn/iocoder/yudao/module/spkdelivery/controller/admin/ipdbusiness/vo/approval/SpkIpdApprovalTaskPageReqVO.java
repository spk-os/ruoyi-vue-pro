package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * IPD 审批待办/已办分页 Request VO（设计文档 §10.8）
 * <p>
 * 底层复用 BPM 待办/已办分页接口，type=todo/done 切换。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 审批任务分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SpkIpdApprovalTaskPageReqVO extends PageParam {

    @Schema(description = "待办 todo / 已办 done", example = "todo")
    private String type;

    @Schema(description = "流程任务名", example = "TR5 审批")
    private String name;

    @Schema(description = "流程分类", example = "1")
    private String category;

    @Schema(description = "流程定义标识", example = "spk-ipd-flow")
    private String processDefinitionKey;

    @Schema(description = "审批状态（已办用）")
    private Integer status;

    @Schema(description = "创建时间范围")
    private LocalDateTime[] createTime;
}
