package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdmonitor;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdmonitor.vo.SpkIpdMonitorRespVO;
import cn.iocoder.yudao.module.spkdelivery.service.ipdmonitor.SpkIpdMonitorService;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Cortext-IPD 项目维度监控 Controller（设计文档 §9.3 / 诉求 §4 监控台）。
 * <p>
 * 项目维度聚合：流程列表（含产物/证据计数）+ 汇总 + 集成健康。
 * 纯只读，复用既有 Service，不碰 Flowable 运行时。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD 项目维度监控")
@RestController
@RequestMapping("/spk/ipd/monitor")
@Validated
public class SpkIpdMonitorController {

    @Resource
    private SpkIpdMonitorService monitorService;

    @GetMapping
    @Operation(summary = "项目维度监控：流程列表(含产物/证据计数)+汇总+集成健康")
    @Parameter(name = "projectId", description = "项目 ID，为空则聚合全部")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-cockpit:query')")
    public CommonResult<SpkIpdMonitorRespVO> monitor(@RequestParam(value = "projectId", required = false) Long projectId) {
        return success(monitorService.monitor(projectId));
    }

}
