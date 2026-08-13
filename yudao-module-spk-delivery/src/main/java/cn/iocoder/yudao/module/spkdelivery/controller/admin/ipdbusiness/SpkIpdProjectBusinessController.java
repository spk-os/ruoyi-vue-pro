package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.majorrelease.SpkIpdMajorReleaseCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.majorrelease.SpkIpdMajorReleaseUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.project.SpkIpdProjectCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.project.SpkIpdProjectPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.project.SpkIpdProjectUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdMajorReleaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectDO;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdProjectBusinessService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Cortext-IPD 项目与大版本 Controller（4 级业务模型第 1-2 层）。
 * 设计文档 §10.2。新业务 API 前缀 /spk/ipd，资源名复数。
 * <p>
 * 旧路由 /spk/ipd/project/* 单实例接口保留（见 controller/admin/project/SpkIpdProjectController），
 * 本控制器承担新业务骨架的 Project/MajorRelease CRUD + 状态机 + 路线图。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD 项目与大版本")
@RestController
@RequestMapping("/spk/ipd")
@Validated
public class SpkIpdProjectBusinessController {

    @Resource
    private SpkIpdProjectBusinessService businessService;

    // ==================== 项目 ====================

    @PostMapping("/projects")
    @Operation(summary = "创建项目草稿，不自动启动流程")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:create')")
    public CommonResult<SpkIpdProjectDO> createProject(@Valid @RequestBody SpkIpdProjectCreateReqVO req) {
        return success(businessService.createProject(req));
    }

    @GetMapping("/projects")
    @Operation(summary = "项目分页查询（名称/负责人/状态/健康度）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<PageResult<SpkIpdProjectDO>> pageProjects(@Valid SpkIpdProjectPageReqVO req) {
        return success(businessService.pageProjects(req));
    }

    @GetMapping("/projects/{projectId}")
    @Operation(summary = "项目摘要")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<SpkIpdProjectDO> getProject(@PathVariable("projectId") Long projectId) {
        return success(businessService.getProject(projectId));
    }

    @GetMapping("/projects/{projectId}/roadmap")
    @Operation(summary = "大版本—版本—主流程路线图聚合")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> roadmap(@PathVariable("projectId") Long projectId) {
        return success(businessService.roadmap(projectId));
    }

    @PutMapping("/projects/{projectId}")
    @Operation(summary = "更新项目草稿/计划（要求乐观锁）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdProjectDO> updateProject(@PathVariable("projectId") Long projectId,
                                                       @Valid @RequestBody SpkIpdProjectUpdateReqVO req) {
        return success(businessService.updateProject(projectId, req));
    }

    @PostMapping("/projects/{projectId}/activate")
    @Operation(summary = "激活项目")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdProjectDO> activate(@PathVariable("projectId") Long projectId) {
        return success(businessService.activate(projectId));
    }

    @PostMapping("/projects/{projectId}/pause")
    @Operation(summary = "暂停项目（不强制终止运行实例）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdProjectDO> pause(@PathVariable("projectId") Long projectId) {
        return success(businessService.pause(projectId));
    }

    @PostMapping("/projects/{projectId}/archive")
    @Operation(summary = "归档项目（仅无活跃版本/流程时允许）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:archive')")
    public CommonResult<SpkIpdProjectDO> archive(@PathVariable("projectId") Long projectId) {
        return success(businessService.archive(projectId));
    }

    // ==================== 大版本 Vx ====================

    @GetMapping("/projects/{projectId}/major-releases")
    @Operation(summary = "大版本列表及 baseline 状态")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<SpkIpdMajorReleaseDO>> listMajorReleases(
            @PathVariable("projectId") Long projectId) {
        return success(businessService.listMajorReleases(projectId));
    }

    @PostMapping("/projects/{projectId}/major-releases")
    @Operation(summary = "创建大版本 Vx（可同时创建 Vx.0 基线草稿）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdMajorReleaseDO> createMajorRelease(
            @PathVariable("projectId") Long projectId,
            @Valid @RequestBody SpkIpdMajorReleaseCreateReqVO req) {
        return success(businessService.createMajorRelease(projectId, req));
    }

    @GetMapping("/major-releases/{majorReleaseId}")
    @Operation(summary = "大版本详情、版本摘要、DCP 摘要")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<SpkIpdMajorReleaseDO> getMajorRelease(
            @PathVariable("majorReleaseId") Long majorReleaseId) {
        return success(businessService.getMajorRelease(majorReleaseId));
    }

    @PutMapping("/major-releases/{majorReleaseId}")
    @Operation(summary = "更新大版本目标/负责人/计划")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdMajorReleaseDO> updateMajorRelease(
            @PathVariable("majorReleaseId") Long majorReleaseId,
            @Valid @RequestBody SpkIpdMajorReleaseUpdateReqVO req) {
        return success(businessService.updateMajorRelease(majorReleaseId, req));
    }

    @PostMapping("/major-releases/{majorReleaseId}/close")
    @Operation(summary = "关闭大版本（所有增量结束并完成生命周期检查后）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdMajorReleaseDO> closeMajorRelease(
            @PathVariable("majorReleaseId") Long majorReleaseId) {
        return success(businessService.closeMajorRelease(majorReleaseId));
    }
}
