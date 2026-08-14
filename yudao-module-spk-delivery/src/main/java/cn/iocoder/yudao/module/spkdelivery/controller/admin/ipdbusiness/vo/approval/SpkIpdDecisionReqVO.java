package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * IPD 决策请求 VO（设计文档 §10.8）
 * <p>
 * APPROVE/REJECT/REDIRECT/RETURN：写不可变决策并调用原生 BPM approve/reject/return。
 *
 * @author SPK-OS
 */
@Schema(description = "管理后台 - IPD 决策请求 VO")
@Data
public class SpkIpdDecisionReqVO {

    @Schema(description = "决策 APPROVE/REJECT/REDIRECT/RETURN", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "决策不能为空")
    private String decision;

    @Schema(description = "决策理由")
    private String reason;

    @Schema(description = "跟进项：类型/责任人/截止/说明")
    private List<Map<String, Object>> conditions;

    @Schema(description = "决策包 hash（前端原样回传，服务端重新计算校验；APPROVE 必填）")
    private String decisionPackageHash;

    @Schema(description = "写入流程引擎的变量")
    private Map<String, Object> flowableVariables;

    @Schema(description = "下一节点审批人（APPROVE 用，nodeId→userIds）")
    private Map<String, List<Long>> nextAssignees;

    @Schema(description = "REDIRECT/RETURN 目标节点 key")
    private String redirectTargetTaskKey;

    @Schema(description = "是否确认强阻断仍放行（仅管理员，需证据豁免覆盖）")
    private Boolean forceOverride;
}
