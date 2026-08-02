package cn.iocoder.yudao.module.spkdelivery.controller.admin.gate;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.gate.SpkGateRecordDO;
import cn.iocoder.yudao.module.spkdelivery.service.gate.SpkGateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS 门禁 Controller
 * <p>
 * 门禁 CI（Gitea Actions）回调统一入口：写 <gate>_report/<gate>_pass 变量 + triggerTask 推进。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK 门禁")
@RestController
@RequestMapping("/spk/gate")
@Validated
public class SpkGateController {

    @Resource
    private SpkGateService gateService;

    @PostMapping("/dispatch")
    @PermitAll
    @Operation(summary = "门禁派发应答（BPM HTTP_CALLBACK 触发器调用，仅应答不推进；生产转发 Gitea Actions）")
    public CommonResult<Boolean> dispatch(
            @RequestHeader(value = "X-Spk-Token", required = false) String token,
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId,
            @Parameter(description = "门禁标识 g1..g8 / tr2..tr6") @RequestParam("gate") String gate,
            @Parameter(description = "触发器传入的 receiveTask key") @RequestParam(value = "taskDefineKey", required = false) String taskDefineKey,
            @Parameter(description = "派发附带上文") @RequestParam(value = "report", required = false) String report) {
        gateService.onDispatch(token, processInstanceId, gate, taskDefineKey, report);
        return success(true);
    }

    @PostMapping("/callback")
    @PermitAll
    @Operation(summary = "门禁回调统一入口（CI 完成后回写门禁结论并推进流程）")
    public CommonResult<Boolean> callback(
            @RequestHeader(value = "X-Spk-Token", required = false) String token,
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId,
            @Parameter(description = "门禁标识 g1..g8 / tr2..tr6") @RequestParam("gate") String gate,
            @Parameter(description = "BPM 节点 key（可空，空则自动取当前 receiveTask）") @RequestParam(value = "nodeKey", required = false) String nodeKey,
            @Parameter(description = "门禁报告 JSON") @RequestParam(value = "report", required = false) String report,
            @Parameter(description = "是否通过") @RequestParam(value = "pass", required = false) Boolean pass) {
        gateService.onCallback(token, processInstanceId, gate, nodeKey, report, pass);
        return success(true);
    }

    @GetMapping("/list-by-instance")
    @Operation(summary = "按流程实例查询门禁记录（IPD 产物 tab）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:gate:query')")
    public CommonResult<List<SpkGateRecordDO>> listByInstance(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId) {
        return success(gateService.getListByInstanceId(processInstanceId));
    }

}
