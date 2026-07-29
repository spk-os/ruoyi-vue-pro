package cn.iocoder.yudao.module.spkdelivery.controller.admin.contract;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.contract.SpkContractVersionDO;
import cn.iocoder.yudao.module.spkdelivery.service.contract.SpkContractVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Workflow Contract 版本治理 Controller
 * <p>
 * 对 IPD 流程定义 simpleModel 做哈希/diff/快照治理。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK 流程契约")
@RestController
@RequestMapping("/spk/contract")
@Validated
public class SpkContractController {

    @Resource
    private SpkContractVersionService contractVersionService;

    @GetMapping("/diff")
    @Operation(summary = "查询某流程模型相对上一版本的契约 diff")
    @PreAuthorize("@ss.hasPermission('spk-delivery:contract:query')")
    public CommonResult<SpkContractVersionService.ContractDiff> diff(
            @Parameter(description = "流程模型 key") @RequestParam("modelKey") String modelKey,
            @Parameter(description = "当前 simpleModel JSON（可选）") @RequestParam(value = "simpleModel", required = false) String simpleModel) {
        return success(contractVersionService.diff(modelKey, simpleModel));
    }

    @PostMapping("/snapshot")
    @Operation(summary = "记录一版流程契约快照（部署后调用，落 hash/版本/diff）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:contract:snapshot')")
    public CommonResult<SpkContractVersionDO> snapshot(
            @Parameter(description = "流程模型 key") @RequestParam("modelKey") String modelKey,
            @Parameter(description = "simpleModel JSON 快照") @RequestParam("simpleModel") String simpleModel) {
        return success(contractVersionService.record(modelKey, simpleModel));
    }

}
