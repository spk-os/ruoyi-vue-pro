package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.version.SpkIpdReadinessRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.version.SpkIpdVersionCreateReqVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdbusiness.vo.version.SpkIpdVersionUpdateReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdVersionDO;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdProjectBusinessService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Cortext-IPD 交付版本 Controller（4 级业务模型第 3 层：Vx.ss）。
 * 设计文档 §10.3。新业务 API 前缀 /spk/ipd。
 * <p>
 * 版本号结构化存整数（majorNo/minorNo），禁止字符串拆分；就绪度冻结后进入 READY 才允许启动 FlowRun。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD 交付版本")
@RestController
@RequestMapping("/spk/ipd")
@Validated
public class SpkIpdVersionController {

    @Resource
    private SpkIpdProjectBusinessService businessService;

    @GetMapping("/major-releases/{majorReleaseId}/versions")
    @Operation(summary = "查询 Vx.0 与所有 Vx.ss")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<List<SpkIpdVersionDO>> listVersions(
            @PathVariable("majorReleaseId") Long majorReleaseId) {
        return success(businessService.listVersions(majorReleaseId));
    }

    @PostMapping("/major-releases/{majorReleaseId}/versions")
    @Operation(summary = "创建增量/热修版本；服务端分配或校验 minorNo")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdVersionDO> createVersion(
            @PathVariable("majorReleaseId") Long majorReleaseId,
            @Valid @RequestBody SpkIpdVersionCreateReqVO req) {
        // projectId 由大版本反查，避免调用方传错
        return success(businessService.createVersion(
                businessService.getMajorRelease(majorReleaseId).getProjectId(),
                majorReleaseId, req));
    }

    @GetMapping("/versions/{versionId}")
    @Operation(summary = "版本详情和当前主流程摘要")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<SpkIpdVersionDO> getVersion(@PathVariable("versionId") Long versionId) {
        return success(businessService.getVersion(versionId));
    }

    @PutMapping("/versions/{versionId}")
    @Operation(summary = "更新版本范围/计划/负责人（乐观锁）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdVersionDO> updateVersion(@PathVariable("versionId") Long versionId,
                                                       @Valid @RequestBody SpkIpdVersionUpdateReqVO req) {
        return success(businessService.updateVersion(versionId, req));
    }

    @GetMapping("/versions/{versionId}/readiness")
    @Operation(summary = "版本启动就绪项和阻断项")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<SpkIpdReadinessRespVO> readiness(@PathVariable("versionId") Long versionId) {
        return success(businessService.readiness(versionId));
    }

    @PostMapping("/versions/{versionId}/ready")
    @Operation(summary = "冻结本次启动范围并进入 READY")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdVersionDO> readyVersion(@PathVariable("versionId") Long versionId) {
        return success(businessService.readyVersion(versionId));
    }

    @PostMapping("/versions/{versionId}/cancel")
    @Operation(summary = "取消未发布版本")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:update')")
    public CommonResult<SpkIpdVersionDO> cancelVersion(@PathVariable("versionId") Long versionId) {
        return success(businessService.cancelVersion(versionId));
    }

    @GetMapping("/versions/{versionId}/traceability")
    @Operation(summary = "需求—设计—代码/PR—测试—证据—发布三向追溯（S1 返回摘要骨架）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-project:query')")
    public CommonResult<Map<String, Object>> traceability(@PathVariable("versionId") Long versionId) {
        // S1 摘要骨架：完整三向追溯在 S4 接入 Plane/Gitea/证据链后补全
        SpkIpdVersionDO v = businessService.getVersion(versionId);
        return success(java.util.Map.of(
                "versionId", versionId,
                "versionNo", v.getVersionNo(),
                "stage", "S1_SKELETON",
                "note", "完整三向追溯将在 S4 接入 Plane/Gitea/证据链后提供"));
    }
}
