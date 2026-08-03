package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdcockpit;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.framework.monitoring.SpkIpdMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS IPD 监控指标 Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD 监控指标")
@RestController
@RequestMapping("/spk/ipd/metrics")
@Validated
public class SpkIpdMetricsController {

    @Resource
    private SpkIpdMetrics metrics;

    @GetMapping("/snapshot")
    @Operation(summary = "IPD 核心计数器快照")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-cockpit:query')")
    public CommonResult<Map<String, Object>> snapshot() {
        return success(metrics.snapshot());
    }

}
