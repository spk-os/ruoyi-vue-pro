package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdLegacyMappingDO;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdContextSelectorService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdLegacyMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Cortext-IPD 全局上下文选择器与旧实例映射 Controller。
 * 设计文档 §5.2 / §5.4。新业务 API 前缀 /spk/ipd。
 * <p>
 * 上下文选择器三级联动（项目/版本/流程）+ 最近上下文偏好；
 * 旧 process_instance_id 到新业务对象的可审计解析与人工确认。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD 上下文与旧实例映射")
@RestController
@RequestMapping("/spk/ipd")
@Validated
public class SpkIpdContextController {

    @Resource
    private SpkIpdContextSelectorService contextSelectorService;
    @Resource
    private SpkIpdLegacyMappingService legacyMappingService;

    // ==================== 上下文选择器 ====================

    @GetMapping("/context/projects")
    @Operation(summary = "选择器：项目轻量列表")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<Map<String, Object>>> pickerProjects() {
        return success(contextSelectorService.listProjectsForPicker());
    }

    @GetMapping("/context/versions")
    @Operation(summary = "选择器：按项目分组的版本列表（Vx.ss 全量）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<Map<String, Object>>> pickerVersions(
            @Parameter(description = "项目 ID（必选）") @RequestParam("projectId") Long projectId) {
        return success(contextSelectorService.listVersionsForPicker(projectId));
    }

    @GetMapping("/context/flow-runs")
    @Operation(summary = "选择器：按项目/版本过滤的流程列表（仅非终态）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<Map<String, Object>>> pickerFlowRuns(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "versionId", required = false) Long versionId) {
        return success(contextSelectorService.listFlowRunsForPicker(projectId, versionId));
    }

    @GetMapping("/context/recent")
    @Operation(summary = "用户最近 5 个上下文（个人偏好）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<Map<String, Object>>> recentContexts() {
        return success(contextSelectorService.recentContexts());
    }

    @PostMapping("/context/recent")
    @Operation(summary = "记录当前上下文到个人偏好")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<Map<String, Object>>> saveRecentContext(@RequestBody Map<String, Object> ctx) {
        return success(contextSelectorService.saveRecentContext(ctx));
    }

    // ==================== 旧实例映射 ====================

    @GetMapping("/legacy/resolve")
    @Operation(summary = "解析旧 process_instance_id 到新业务上下文")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> resolveLegacy(
            @Parameter(description = "旧 Flowable process_instance_id") @RequestParam("processInstanceId") String processInstanceId) {
        return success(legacyMappingService.resolve(processInstanceId));
    }

    @GetMapping("/legacy/needs-mapping")
    @Operation(summary = "待人工确认的未映射旧实例列表")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<SpkIpdLegacyMappingDO>> listNeedsMapping() {
        return success(legacyMappingService.listNeedsMapping());
    }

    @PostMapping("/legacy/{mappingId}/confirm")
    @Operation(summary = "人工确认旧实例到业务对象的映射")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdLegacyMappingDO> confirmMapping(
            @PathVariable("mappingId") Long mappingId,
            @RequestBody Map<String, Long> body) {
        return success(legacyMappingService.confirm(mappingId,
                body.get("projectId"), body.get("versionId"), body.get("flowRunId")));
    }
}
