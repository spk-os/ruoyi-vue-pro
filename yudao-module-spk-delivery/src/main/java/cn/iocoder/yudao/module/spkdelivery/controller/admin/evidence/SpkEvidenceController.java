package cn.iocoder.yudao.module.spkdelivery.controller.admin.evidence;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkEvidenceRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkEvidenceRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.service.evidence.SpkEvidenceService;
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
 * Evidence Record Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK Evidence 证据链")
@RestController
@RequestMapping("/spk/evidence")
@Validated
public class SpkEvidenceController {

    @Resource
    private SpkEvidenceRecordMapper evidenceMapper;
    @Resource
    private SpkEvidenceService evidenceService;

    @GetMapping("/list-by-run")
    @Operation(summary = "按 ActivityRun 查询证据链")
    @PreAuthorize("@ss.hasPermission('spk-delivery:evidence:query')")
    public CommonResult<List<SpkEvidenceRecordDO>> listByRun(
            @Parameter(description = "activityRunId") @RequestParam("activityRunId") String activityRunId) {
        return success(evidenceMapper.selectListByActivityRunId(activityRunId));
    }

    @GetMapping("/list-by-instance")
    @Operation(summary = "按流程实例查询证据链")
    @PreAuthorize("@ss.hasPermission('spk-delivery:evidence:query')")
    public CommonResult<List<SpkEvidenceRecordDO>> listByInstance(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId) {
        return success(evidenceMapper.selectListByProcessInstanceId(processInstanceId));
    }

    @GetMapping("/verify-chain")
    @Operation(summary = "回放校验证据哈希链")
    @PreAuthorize("@ss.hasPermission('spk-delivery:evidence:query')")
    public CommonResult<Boolean> verifyChain(
            @Parameter(description = "activityRunId") @RequestParam("activityRunId") String activityRunId) {
        return success(evidenceService.verifyChain(activityRunId));
    }

}
