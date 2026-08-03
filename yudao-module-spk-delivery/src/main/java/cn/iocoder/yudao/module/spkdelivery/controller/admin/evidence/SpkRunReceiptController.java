package cn.iocoder.yudao.module.spkdelivery.controller.admin.evidence;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkRunReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkRunReceiptMapper;
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
 * Run Receipt Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK Run Receipt 运行收据")
@RestController
@RequestMapping("/spk/run-receipt")
@Validated
public class SpkRunReceiptController {

    @Resource
    private SpkRunReceiptMapper runReceiptMapper;

    @GetMapping("/get-by-run")
    @Operation(summary = "按 runId 查询运行收据")
    @PreAuthorize("@ss.hasPermission('spk-delivery:run-receipt:query')")
    public CommonResult<SpkRunReceiptDO> getByRun(
            @Parameter(description = "runId") @RequestParam("runId") String runId) {
        return success(runReceiptMapper.selectByRunId(runId));
    }

    @GetMapping("/get-by-activity-run")
    @Operation(summary = "按 ActivityRun 查询运行收据")
    @PreAuthorize("@ss.hasPermission('spk-delivery:run-receipt:query')")
    public CommonResult<SpkRunReceiptDO> getByActivityRun(
            @Parameter(description = "activityRunId") @RequestParam("activityRunId") String activityRunId) {
        return success(runReceiptMapper.selectByActivityRunId(activityRunId));
    }

}
