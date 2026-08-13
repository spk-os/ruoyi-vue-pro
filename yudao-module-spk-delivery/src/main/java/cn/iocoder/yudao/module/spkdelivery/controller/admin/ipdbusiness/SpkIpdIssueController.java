package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue.SpkIpdIssueCaseCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue.SpkIpdIssueCasePageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue.SpkIpdIssueCaseUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue.SpkIpdIssueTriageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.issue.SpkIpdIssueVersionRelReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdIssueCaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdIssueVersionRelDO;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdIssueCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Cortext-IPD 问题流程 Controller（IPD 第三类流程 ISSUE_RESOLUTION 的业务侧）。
 * 设计文档 §10.6。新业务 API 前缀 /spk/ipd。
 * <p>
 * 问题登记/分诊/关联/处置/关闭/重开 + 启动问题流（内部复用 FlowRunService 的预检与启动命令）。
 * P0/P1 关闭前要求 VERIFIED_IN 验证关联；FIXED_IN 是进入实施修复的前置。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD 问题流程")
@RestController
@RequestMapping("/spk/ipd")
@Validated
public class SpkIpdIssueController {

    @Resource
    private SpkIpdIssueCaseService issueService;

    @PostMapping("/projects/{projectId}/issues")
    @Operation(summary = "登记问题（支持外部引用去重）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdIssueCaseDO> createIssue(@PathVariable("projectId") Long projectId,
                                                       @Valid @RequestBody SpkIpdIssueCaseCreateReqVO req) {
        req.setProjectId(projectId);
        return success(issueService.createIssue(req));
    }

    @GetMapping("/projects/{projectId}/issues")
    @Operation(summary = "分页查询项目问题")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<PageResult<SpkIpdIssueCaseDO>> pageIssues(
            @PathVariable("projectId") Long projectId,
            @Valid SpkIpdIssueCasePageReqVO req) {
        req.setProjectId(projectId);
        return success(issueService.pageIssues(req));
    }

    @GetMapping("/issues/{issueCaseId}")
    @Operation(summary = "问题详情")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<SpkIpdIssueCaseDO> getIssue(@PathVariable("issueCaseId") Long issueCaseId) {
        return success(issueService.getIssue(issueCaseId));
    }

    @PutMapping("/issues/{issueCaseId}")
    @Operation(summary = "更新问题分类、负责人、根因（乐观锁）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdIssueCaseDO> updateIssue(@PathVariable("issueCaseId") Long issueCaseId,
                                                        @Valid @RequestBody SpkIpdIssueCaseUpdateReqVO req) {
        return success(issueService.updateIssue(issueCaseId, req));
    }

    @PostMapping("/issues/{issueCaseId}/triage")
    @Operation(summary = "分诊严重性、影响版本、时限、是否启动问题流")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdIssueCaseDO> triage(@PathVariable("issueCaseId") Long issueCaseId,
                                                  @Valid @RequestBody SpkIpdIssueTriageReqVO req) {
        return success(issueService.triage(issueCaseId, req));
    }

    @PostMapping("/issues/{issueCaseId}/version-relations")
    @Operation(summary = "关联 FOUND_IN/AFFECTS/FIXED_IN/VERIFIED_IN")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdIssueVersionRelDO> addVersionRelation(
            @PathVariable("issueCaseId") Long issueCaseId,
            @Valid @RequestBody SpkIpdIssueVersionRelReqVO req) {
        return success(issueService.addVersionRelation(issueCaseId, req));
    }

    @GetMapping("/issues/{issueCaseId}/version-relations")
    @Operation(summary = "问题-版本关联列表")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<SpkIpdIssueVersionRelDO>> listVersionRelations(
            @PathVariable("issueCaseId") Long issueCaseId) {
        return success(issueService.listVersionRelations(issueCaseId));
    }

    @PostMapping("/issues/{issueCaseId}/start-flow")
    @Operation(summary = "创建 ISSUE_RESOLUTION FlowRun，内部复用预检和启动命令")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:create')")
    public CommonResult<Map<String, Object>> startFlow(@PathVariable("issueCaseId") Long issueCaseId,
                                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return success(issueService.startFlow(issueCaseId, idempotencyKey));
    }

    @PostMapping("/issues/{issueCaseId}/resolve")
    @Operation(summary = "解决问题；必须有验证结论，P0/P1 还要求独立验证证据")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdIssueCaseDO> resolve(@PathVariable("issueCaseId") Long issueCaseId,
                                                   @RequestBody Map<String, String> body) {
        return success(issueService.resolve(issueCaseId, body.get("resolution")));
    }

    @PostMapping("/issues/{issueCaseId}/close")
    @Operation(summary = "发布/验证完成后关闭")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdIssueCaseDO> close(@PathVariable("issueCaseId") Long issueCaseId) {
        return success(issueService.closeIssue(issueCaseId));
    }

    @PostMapping("/issues/{issueCaseId}/reopen")
    @Operation(summary = "重开：保留旧运行，创建新的处理 attempt")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdIssueCaseDO> reopen(@PathVariable("issueCaseId") Long issueCaseId,
                                                  @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? "重开" : body.getOrDefault("reason", "重开");
        return success(issueService.reopen(issueCaseId, reason));
    }
}
