package cn.iocoder.yudao.module.spkdelivery.controller.admin.sunset;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.sunset.SpkSunsetDO;
import cn.iocoder.yudao.module.spkdelivery.service.sunset.SpkSunsetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS R8 退市 Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK R8 退市")
@RestController
@RequestMapping("/spk/sunset")
@Validated
public class SpkSunsetController {

    @Resource
    private SpkSunsetService sunsetService;

    @PostMapping("/start")
    @Operation(summary = "启动 R8 退市归档")
    @PreAuthorize("@ss.hasPermission('spk-delivery:sunset:start')")
    public CommonResult<SpkSunsetDO> start(
            @RequestParam("processInstanceId") String processInstanceId,
            @RequestParam(value = "sunsetReport", required = false) String sunsetReport,
            @RequestParam(value = "archiveStatus", required = false) String archiveStatus) {
        return success(sunsetService.start(processInstanceId, sunsetReport, archiveStatus));
    }

}
