package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance.SpkIpdProcessProfilePageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance.SpkIpdProcessProfileSaveReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance.SpkIpdSnapshotSchemaRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.governance.SpkIpdTrimRuleSaveReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdEngineInstanceDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFailedJobDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdGovernanceAuditDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProcessProfileDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProcessProfileVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdTrimRuleDO;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdProcessProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Cortext-IPD 流程治理 Controller（§7.2 第 4 顶层表面）。
 * RBAC 权限前缀 spk-delivery:ipd-governance:*。前缀 /spk/ipd/admin/workflows。
 * <p>
 * 管理员配置产物：Profile 模板/版本/裁剪规则可建真实种子；引擎档案/失败作业/审计为运行时聚合，
 * 无数据时如实返回空，前端显式标注"未接入/样本不足"，绝不造假。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD 流程治理")
@RestController
@RequestMapping("/spk/ipd/admin/workflows")
@Validated
public class SpkIpdGovernanceController {

    @Resource
    private SpkIpdProcessProfileService profileService;

    // ==================== Profile 模板 ====================

    @PostMapping("/profiles")
    @Operation(summary = "创建流程模板 Profile")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:create')")
    public CommonResult<Long> createProfile(@Valid @RequestBody SpkIpdProcessProfileSaveReqVO req) {
        return success(profileService.createProfile(req));
    }

    @PutMapping("/profiles")
    @Operation(summary = "更新流程模板 Profile")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:update')")
    public CommonResult<Boolean> updateProfile(@Valid @RequestBody SpkIpdProcessProfileSaveReqVO req) {
        profileService.updateProfile(req);
        return success(true);
    }

    @GetMapping("/profiles")
    @Operation(summary = "Profile 分页")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:query')")
    public CommonResult<PageResult<SpkIpdProcessProfileDO>> pageProfile(@Valid SpkIpdProcessProfilePageReqVO req) {
        return success(profileService.pageProfile(req));
    }

    @GetMapping("/profiles/{id}")
    @Operation(summary = "Profile 详情")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:query')")
    public CommonResult<SpkIpdProcessProfileDO> getProfile(@PathVariable("id") Long id) {
        return success(profileService.getProfile(id));
    }

    @DeleteMapping("/profiles/{id}")
    @Operation(summary = "删除 Profile")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:delete')")
    public CommonResult<Boolean> deleteProfile(@PathVariable("id") Long id) {
        profileService.deleteProfile(id);
        return success(true);
    }

    // ==================== Profile 版本 ====================

    @PostMapping("/profiles/{profileId}/versions")
    @Operation(summary = "新建草稿版本（snapshot 不可变）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:update')")
    public CommonResult<Long> createVersion(@PathVariable("profileId") Long profileId,
                                            @RequestBody Map<String, String> body) {
        return success(profileService.createVersion(profileId, body.get("snapshotJson"), body.get("compatibilityHash")));
    }

    @PostMapping("/profiles/versions/{versionId}/publish")
    @Operation(summary = "发布版本：旧已发布置 SUPERSEDED")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:update')")
    public CommonResult<Boolean> publishVersion(@PathVariable("versionId") Long versionId) {
        profileService.publishVersion(versionId);
        return success(true);
    }

    @PostMapping("/profiles/versions/{versionId}/rollback")
    @Operation(summary = "回滚到指定已发布版本")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:update')")
    public CommonResult<Boolean> rollbackVersion(@PathVariable("versionId") Long versionId) {
        profileService.rollbackVersion(versionId);
        return success(true);
    }

    @GetMapping("/profiles/{profileId}/versions")
    @Operation(summary = "Profile 版本列表（按版本号倒序）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:query')")
    public CommonResult<List<SpkIpdProcessProfileVersionDO>> listVersions(@PathVariable("profileId") Long profileId) {
        return success(profileService.listVersions(profileId));
    }

    // ==================== 裁剪规则 ====================

    @PostMapping("/trim-rules")
    @Operation(summary = "保存裁剪规则（id 为空则新建）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:update')")
    public CommonResult<Long> saveTrimRule(@Valid @RequestBody SpkIpdTrimRuleSaveReqVO req) {
        return success(profileService.saveTrimRule(req));
    }

    @GetMapping("/trim-rules")
    @Operation(summary = "裁剪规则列表")
    @Parameter(name = "profileVersionId", description = "Profile 版本 id", required = true)
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:query')")
    public CommonResult<List<SpkIpdTrimRuleDO>> listTrimRules(@RequestParam("profileVersionId") Long profileVersionId) {
        return success(profileService.listTrimRules(profileVersionId));
    }

    @DeleteMapping("/trim-rules/{id}")
    @Operation(summary = "删除裁剪规则")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:delete')")
    public CommonResult<Boolean> deleteTrimRule(@PathVariable("id") Long id) {
        profileService.deleteTrimRule(id);
        return success(true);
    }

    // ==================== 治理查询 ====================

    @GetMapping("/failed-jobs")
    @Operation(summary = "失败作业列表（status 缺省=PENDING）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:query')")
    public CommonResult<List<SpkIpdFailedJobDO>> listFailedJobs(@RequestParam(value = "status", required = false) String status) {
        return success(profileService.listFailedJobs(status == null ? "PENDING" : status));
    }

    @GetMapping("/audit")
    @Operation(summary = "治理审计列表（append-only）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:query')")
    public CommonResult<List<SpkIpdGovernanceAuditDO>> listAudit(@RequestParam(value = "actionType", required = false) String actionType,
                                                                  @RequestParam(value = "refId", required = false) Long refId) {
        return success(profileService.listAudit(actionType, refId));
    }

    // ==================== 引擎档案 / 快照契约（D4/D2） ====================

    @GetMapping("/engine-instances")
    @Operation(summary = "引擎实例档案（按 flowRunId 查；无数据返回 null，前端标样本不足）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:query')")
    public CommonResult<SpkIpdEngineInstanceDO> getEngineInstance(@RequestParam("flowRunId") Long flowRunId) {
        return success(profileService.getEngineInstanceByFlowRunId(flowRunId));
    }

    @GetMapping("/snapshot-schema")
    @Operation(summary = "snapshotJson 结构契约（按 flowType 返回阶段/门/DCP/TR/活动）")
    @Parameter(name = "flowType", description = "FULL_RELEASE/INCREMENT_RELEASE/ISSUE_RESOLUTION", required = true)
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-governance:query')")
    public CommonResult<SpkIpdSnapshotSchemaRespVO> getSnapshotSchema(@RequestParam("flowType") String flowType) {
        return success(profileService.buildSnapshotSchema(flowType));
    }
}
