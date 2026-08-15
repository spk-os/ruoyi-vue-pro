package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdProjectWorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Cortext-IPD 项目空间 Controller（设计文档 §9.3 / 诉求 §1 项目管理）。
 * <p>
 * 项目卡片网格 + 项目详情聚合（活跃版本/FlowRun/阶段时间戳/活动泳道/需求树/最近活动/问题计数）。
 * 纯只读聚合，复用 {@link SpkIpdProjectWorkspaceService}，不碰 Flowable 运行时。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD 项目空间")
@RestController
@RequestMapping("/spk/ipd/projects")
@Validated
public class SpkIpdProjectWorkspaceController {

    @Resource
    private SpkIpdProjectWorkspaceService workspaceService;

    @GetMapping("/cards")
    @Operation(summary = "项目卡片网格：currentStage/activeVersion/dueIn/聚合指标")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<Map<String, Object>>> cards() {
        return success(workspaceService.cards());
    }

    @GetMapping("/{projectId}/workspace")
    @Operation(summary = "项目详情一次性聚合：活跃版本/FlowRun/阶段时间戳/泳道/最近活动/需求树")
    @Parameter(name = "projectId", description = "项目 ID")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> workspace(@PathVariable("projectId") Long projectId) {
        return success(workspaceService.workspace(projectId));
    }

    @GetMapping("/{projectId}/stage-timestamps")
    @Operation(summary = "6 阶段进入时间与状态（按版本活跃 FlowRun 聚合）")
    @Parameter(name = "projectId", description = "项目 ID")
    @Parameter(name = "versionId", description = "版本 ID，为空取项目活跃版本")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<Map<String, Object>>> stageTimestamps(
            @PathVariable("projectId") Long projectId,
            @RequestParam(value = "versionId", required = false) Long versionId) {
        return success(workspaceService.stageTimestamps(projectId, versionId));
    }

    @GetMapping("/{projectId}/requirements-tree")
    @Operation(summary = "IR/SR/AR 需求追踪树（按版本过滤，无数据 sparse=true）")
    @Parameter(name = "projectId", description = "项目 ID")
    @Parameter(name = "versionId", description = "版本 ID，为空取项目级")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> requirementsTree(
            @PathVariable("projectId") Long projectId,
            @RequestParam(value = "versionId", required = false) Long versionId) {
        return success(workspaceService.requirementTree(projectId, versionId));
    }

    @GetMapping("/{projectId}/recent-activities")
    @Operation(summary = "项目维度跨 FlowRun 最近活动时间线")
    @Parameter(name = "projectId", description = "项目 ID")
    @Parameter(name = "limit", description = "条数，默认 10")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<Map<String, Object>>> recentActivities(
            @PathVariable("projectId") Long projectId,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return success(workspaceService.recentActivities(projectId, limit));
    }
}
