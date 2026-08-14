package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdApprovalTaskPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdApprovalTaskRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdDecisionPackageRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdDecisionReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.approval.SpkIpdEvidenceWaiverReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkEvidenceRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdDecisionRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdEvidenceWaiverDO;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Cortext-IPD 审批与决策包 Controller（设计文档 §10.8 / §7.9）。
 * <p>
 * 在原生 BPM 待办/已办与 approve/reject/return 基础上做业务包装：
 * 决策包聚合、不可变决策记录、证据豁免。复用原生 BPM 语义，不改引擎。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD 审批与决策包")
@RestController
@RequestMapping("/spk/ipd")
@Validated
public class SpkIpdApprovalController {

    @Resource
    private SpkIpdApprovalService approvalService;

    @GetMapping("/approval-tasks")
    @Operation(summary = "IPD 审批待办/已办分页（type=todo/done）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<PageResult<SpkIpdApprovalTaskRespVO>> pageApprovalTasks(@Valid SpkIpdApprovalTaskPageReqVO req) {
        return success(approvalService.pageApprovalTasks(SecurityFrameworkUtils.getLoginUserId(), req));
    }

    @GetMapping("/approval-tasks/{taskId}/decision-package")
    @Operation(summary = "决策包：业务摘要、差异、证据、风险、历史决策、候选动作")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<SpkIpdDecisionPackageRespVO> getDecisionPackage(
            @Parameter(description = "Flowable 任务 id") @PathVariable("taskId") String taskId) {
        return success(approvalService.getDecisionPackage(taskId));
    }

    @PostMapping("/approval-tasks/{taskId}/decisions")
    @Operation(summary = "写不可变决策并调用原生 BPM approve/reject/return")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdDecisionRecordDO> createDecision(
            @PathVariable("taskId") String taskId, @Valid @RequestBody SpkIpdDecisionReqVO req) {
        return success(approvalService.createDecision(SecurityFrameworkUtils.getLoginUserId(), taskId, req));
    }

    @GetMapping("/flow-runs/{flowRunId}/decisions")
    @Operation(summary = "DCP/TR/干预历史（按 FlowRun）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<SpkIpdDecisionRecordDO>> listDecisions(
            @Parameter(description = "FlowRun id") @PathVariable("flowRunId") Long flowRunId) {
        return success(approvalService.listDecisionsByFlowRun(flowRunId));
    }

    @PostMapping("/approval-tasks/{taskId}/evidence-waivers")
    @Operation(summary = "证据豁免：时限、理由、补证责任人")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdEvidenceWaiverDO> createEvidenceWaiver(
            @PathVariable("taskId") String taskId, @Valid @RequestBody SpkIpdEvidenceWaiverReqVO req) {
        return success(approvalService.createEvidenceWaiver(SecurityFrameworkUtils.getLoginUserId(), taskId, req));
    }

    @GetMapping("/flow-runs/{flowRunId}/artifacts")
    @Operation(summary = "FlowRun 产物清单（hash/签名/扫描/验证状态）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<SpkArtifactManifestDO>> getArtifacts(
            @PathVariable("flowRunId") Long flowRunId) {
        return success(approvalService.getArtifacts(flowRunId));
    }

    @GetMapping("/flow-runs/{flowRunId}/evidence")
    @Operation(summary = "FlowRun 证据及缺口")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<SpkEvidenceRecordDO>> getEvidence(
            @PathVariable("flowRunId") Long flowRunId) {
        return success(approvalService.getEvidence(flowRunId));
    }
}
