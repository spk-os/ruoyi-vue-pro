package cn.iocoder.yudao.module.spkdelivery.controller.admin.project;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.project.vo.SpkIpdIntakeReqVO;
import cn.iocoder.yudao.module.spkdelivery.service.project.SpkIpdIntakeService;
import cn.iocoder.yudao.module.spkdelivery.service.project.SpkIpdProjectService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Cortext-IPD 项目 Controller
 * <p>
 * 端点前缀 /spk/ipd/project（经 axios 全局注入 /admin-api）：
 * <ul>
 *   <li>POST /start —— 发起 IPD 主流程（businessKey + projectName）。</li>
 *   <li>POST /intake —— 自然语言发起（hermes「说一句话就开跑」入口，LLM 抽取 projectName/payload 后转 /start）。</li>
 *   <li>GET  /{id} —— 项目总览（泳道 + 六阶段进度），id=processInstanceId。</li>
 *   <li>GET  /{id}/phases —— 各阶段 done/running/failed/total + 阶段状态。</li>
 *   <li>GET  /requirements —— Plane 需求代理（Dashboard 需求 Tab）。</li>
 *   <li>GET  /git-pr —— Gitea PR/CI 状态代理。</li>
 *   <li>GET  /git-release —— Gitea Release 代理。</li>
 *   <li>GET  /omnigent-session —— Omnigent session iframe 代理入口（见 SpkOmnigentProxyController）。</li>
 * </ul>
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD 项目")
@RestController
@RequestMapping("/spk/ipd/project")
@Validated
public class SpkIpdProjectController {

    @Resource
    private SpkIpdProjectService projectService;

    @Resource
    private SpkIpdIntakeService intakeService;

    @PostMapping("/intake")
    @Operation(summary = "自然语言发起 IPD 主流程（hermes「说一句话就开跑」入口）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:create')")
    public CommonResult<Map<String, Object>> intake(@Valid @RequestBody SpkIpdIntakeReqVO reqVO) {
        return success(intakeService.intake(reqVO.getRequest(), reqVO.getMode()));
    }

    @PostMapping("/start")
    @Operation(summary = "发起 IPD 主流程（businessKey + projectName）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:create')")
    public CommonResult<Map<String, Object>> start(
            @Parameter(description = "项目业务 key") @RequestParam(value = "businessKey", required = false) String businessKey,
            @Parameter(description = "项目名") @RequestParam(value = "projectName", required = false) String projectName,
            @Parameter(description = "附加 payload") @RequestParam(value = "payload", required = false) String payload,
            @Parameter(description = "运行模式 test/product") @RequestParam(value = "mode", required = false) String mode) {
        return success(projectService.start(businessKey, projectName, payload, mode));
    }

    @GetMapping("/by-key/{businessKey}")
    @Operation(summary = "按 businessKey 查发起态")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> getByBusinessKey(
            @Parameter(description = "项目业务 key") @PathVariable("businessKey") String businessKey) {
        return success(projectService.getByBusinessKey(businessKey));
    }

    @GetMapping("/latest")
    @Operation(summary = "最新 IPD 流程实例（项目页/监控台进入界面默认载入）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> getLatest() {
        return success(projectService.getLatest());
    }

    @GetMapping("/{id}")
    @Operation(summary = "项目总览：泳道 + 六阶段进度（id=processInstanceId）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> getProject(
            @Parameter(description = "流程实例编号") @PathVariable("id") String processInstanceId) {
        return success(projectService.getProject(processInstanceId));
    }

    @GetMapping("/{id}/phases")
    @Operation(summary = "各阶段进度：done/running/failed/total + 阶段状态")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<java.util.List<Map<String, Object>>> getPhases(
            @Parameter(description = "流程实例编号") @PathVariable("id") String processInstanceId) {
        return success(projectService.getPhases(processInstanceId));
    }

    @GetMapping("/requirements")
    @Operation(summary = "Plane 需求代理（Dashboard 需求 Tab）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<String> requirements(
            @Parameter(description = "拉取条数") @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return success(projectService.getRequirements(limit));
    }

    @GetMapping("/git-pr")
    @Operation(summary = "Gitea PR/CI 状态代理")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> gitPr() {
        return success(projectService.getGitPr());
    }

    @GetMapping("/git-release")
    @Operation(summary = "Gitea Release 代理")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> gitRelease() {
        return success(projectService.getGitRelease());
    }

}
