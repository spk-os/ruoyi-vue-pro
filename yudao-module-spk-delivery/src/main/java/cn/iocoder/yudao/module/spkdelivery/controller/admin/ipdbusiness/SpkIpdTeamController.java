package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor.SpkIpdActorCandidateRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor.SpkIpdProjectActorPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.actor.SpkIpdProjectActorSaveReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment.SpkIpdAssignmentCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment.SpkIpdAssignmentPageReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.assignment.SpkIpdAssignmentReassignReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.team.SpkIpdTeamRespVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdAssignmentDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectActorDO;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdTeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Cortext-IPD 团队、参与者与分派 Controller（设计文档 §5.1 / §10.7）。
 * 新业务 API 前缀 /spk/ipd。复用原生 BPM transfer/delegate 由 Assignment 转派封装，不改 BPM 引擎。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD 团队与分派")
@RestController
@RequestMapping("/spk/ipd")
@Validated
public class SpkIpdTeamController {

    @Resource
    private SpkIpdTeamService teamService;

    // ---------- 团队统一页 ----------

    @GetMapping("/team")
    @Operation(summary = "团队统一页聚合：人/Agent/编队 + 负载与产出")
    @Parameter(name = "projectId", description = "项目 ID")
    @Parameter(name = "versionId", description = "版本作用域，空=项目级")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<SpkIpdTeamRespVO> team(@RequestParam(value = "projectId", required = false) Long projectId,
                                                @RequestParam(value = "versionId", required = false) Long versionId) {
        return success(teamService.team(projectId, versionId));
    }

    // ---------- 参与者 ----------

    @GetMapping("/projects/{projectId}/actors")
    @Operation(summary = "列出项目/版本作用域参与者（含显示名）")
    @Parameter(name = "versionId", description = "版本作用域，空=项目级")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<SpkIpdTeamRespVO.ActorRow>> listActors(@PathVariable("projectId") Long projectId,
                                                                    @RequestParam(value = "versionId", required = false) Long versionId) {
        return success(teamService.listActors(projectId, versionId));
    }

    @GetMapping("/actors/page")
    @Operation(summary = "分页查询参与者")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<PageResult<SpkIpdProjectActorDO>> pageActors(@Valid SpkIpdProjectActorPageReqVO reqVO) {
        return success(teamService.pageActors(reqVO));
    }

    @PostMapping("/projects/{projectId}/actors")
    @Operation(summary = "新增/维护项目参与者（不创建系统用户）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdProjectActorDO> saveActor(@PathVariable("projectId") Long projectId,
                                                        @Valid @RequestBody SpkIpdProjectActorSaveReqVO req) {
        req.setProjectId(projectId);
        return success(teamService.saveActor(req));
    }

    @DeleteMapping("/actors/{id}")
    @Operation(summary = "移除项目参与者")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<Boolean> deleteActor(@PathVariable("id") Long id) {
        teamService.deleteActor(id);
        return success(true);
    }

    @GetMapping("/actors/candidates")
    @Operation(summary = "候选检索：人/Agent/编队（用于分派下拉）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<SpkIpdActorCandidateRespVO>> candidates(
            @RequestParam(value = "actorType", required = false) String actorType,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "q", required = false) String q) {
        return success(teamService.candidates(actorType, role, q));
    }

    // ---------- 分派 ----------

    @GetMapping("/assignments")
    @Operation(summary = "按项目/版本/流程/执行者/状态查询统一分派")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<PageResult<SpkIpdAssignmentDO>> pageAssignments(@Valid SpkIpdAssignmentPageReqVO reqVO) {
        return success(teamService.pageAssignments(reqVO));
    }

    @PostMapping("/assignments")
    @Operation(summary = "绑定 BPM task/activity/Plane issue/Agent task 分派")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdAssignmentDO> createAssignment(@Valid @RequestBody SpkIpdAssignmentCreateReqVO req) {
        return success(teamService.createAssignment(req));
    }

    @PostMapping("/assignments/{assignmentId}/reassign")
    @Operation(summary = "转派并保留历史；BPM 人任务底层走原生 transfer/delegate")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdAssignmentDO> reassign(@PathVariable("assignmentId") Long assignmentId,
                                                     @Valid @RequestBody SpkIpdAssignmentReassignReqVO req) {
        return success(teamService.reassign(assignmentId, req));
    }
}
