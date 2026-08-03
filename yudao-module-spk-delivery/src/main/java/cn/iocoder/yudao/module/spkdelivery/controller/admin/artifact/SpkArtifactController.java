package cn.iocoder.yudao.module.spkdelivery.controller.admin.artifact;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * Artifact Manifest Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK Artifact 产物")
@RestController
@RequestMapping("/spk/artifact")
@Validated
public class SpkArtifactController {

    @Resource
    private SpkArtifactManifestMapper artifactMapper;

    @GetMapping("/list-by-instance")
    @Operation(summary = "按流程实例查询产物")
    @PreAuthorize("@ss.hasPermission('spk-delivery:artifact:query')")
    public CommonResult<List<SpkArtifactManifestDO>> listByInstance(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId) {
        return success(artifactMapper.selectListByProcessInstanceId(processInstanceId));
    }

    @GetMapping("/list-by-run")
    @Operation(summary = "按 ActivityRun 查询产物")
    @PreAuthorize("@ss.hasPermission('spk-delivery:artifact:query')")
    public CommonResult<List<SpkArtifactManifestDO>> listByRun(
            @Parameter(description = "ActivityRun id") @RequestParam("activityRunId") String activityRunId) {
        return success(artifactMapper.selectListByActivityRunId(activityRunId));
    }

    @GetMapping("/get")
    @Operation(summary = "按 artifactId 查询")
    @PreAuthorize("@ss.hasPermission('spk-delivery:artifact:query')")
    public CommonResult<SpkArtifactManifestDO> get(
            @Parameter(description = "产物 id") @RequestParam("artifactId") String artifactId) {
        return success(artifactMapper.selectByArtifactId(artifactId));
    }

}
