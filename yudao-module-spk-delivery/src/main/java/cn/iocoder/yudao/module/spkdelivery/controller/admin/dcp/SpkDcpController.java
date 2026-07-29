package cn.iocoder.yudao.module.spkdelivery.controller.admin.dcp;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.service.dcp.SpkDcpRedirectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * SPK-OS DCP 决策门 Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK DCP 决策门")
@RestController
@RequestMapping("/spk/dcp")
@Validated
public class SpkDcpController {

    @Resource
    private SpkDcpRedirectService dcpRedirectService;

    @PostMapping("/redirect")
    @Operation(summary = "DCP Redirect 回退（把当前活动移回目标节点，rCount++）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:dcp:redirect')")
    public CommonResult<Boolean> redirect(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId,
            @Parameter(description = "DCP 标识 cdc/pdc/adc/ldc") @RequestParam("dcp") String dcp,
            @Parameter(description = "回退目标节点 key") @RequestParam("targetNode") String targetNode) {
        dcpRedirectService.redirect(processInstanceId, dcp, targetNode);
        return success(true);
    }

}
