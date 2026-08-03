package cn.iocoder.yudao.module.spkdelivery.controller.admin.evidence;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkVerificationReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkVerificationReceiptMapper;
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
 * Verification Receipt Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK Verification 验证收据")
@RestController
@RequestMapping("/spk/verification-receipt")
@Validated
public class SpkVerificationReceiptController {

    @Resource
    private SpkVerificationReceiptMapper receiptMapper;

    @GetMapping("/list-by-run")
    @Operation(summary = "按 ActivityRun 查询验证收据")
    @PreAuthorize("@ss.hasPermission('spk-delivery:verification-receipt:query')")
    public CommonResult<List<SpkVerificationReceiptDO>> listByRun(
            @Parameter(description = "activityRunId") @RequestParam("activityRunId") String activityRunId) {
        return success(receiptMapper.selectListByActivityRunId(activityRunId));
    }

    @GetMapping("/list-by-artifact")
    @Operation(summary = "按产物查询验证收据")
    @PreAuthorize("@ss.hasPermission('spk-delivery:verification-receipt:query')")
    public CommonResult<List<SpkVerificationReceiptDO>> listByArtifact(
            @Parameter(description = "artifactId") @RequestParam("artifactId") String artifactId) {
        return success(receiptMapper.selectByArtifactId(artifactId));
    }

}
