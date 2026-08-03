package cn.iocoder.yudao.module.spkdelivery.controller.admin.taskcontract;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
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
 * Task Contract Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK Task Contract")
@RestController
@RequestMapping("/spk/task-contract")
@Validated
public class SpkTaskContractController {

    @Resource
    private SpkTaskContractMapper contractMapper;

    @GetMapping("/list-by-instance")
    @Operation(summary = "按流程实例查询 Task Contract")
    @PreAuthorize("@ss.hasPermission('spk-delivery:task-contract:query')")
    public CommonResult<List<SpkTaskContractDO>> listByInstance(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId) {
        return success(contractMapper.selectListByProcessInstanceId(processInstanceId));
    }

    @GetMapping("/list-by-status")
    @Operation(summary = "按状态查询 Task Contract")
    @PreAuthorize("@ss.hasPermission('spk-delivery:task-contract:query')")
    public CommonResult<List<SpkTaskContractDO>> listByStatus(
            @Parameter(description = "状态") @RequestParam("status") String status) {
        return success(contractMapper.selectListByStatus(status));
    }

    @GetMapping("/get")
    @Operation(summary = "按 contractId 查询")
    @PreAuthorize("@ss.hasPermission('spk-delivery:task-contract:query')")
    public CommonResult<SpkTaskContractDO> get(
            @Parameter(description = "合同 id") @RequestParam("contractId") String contractId) {
        return success(contractMapper.selectByContractId(contractId));
    }

}
