package cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdcockpit;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.service.cockpit.SpkIpdCockpitService;
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

import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS IPD Cockpit 聚合 Controller
 * <p>
 * 给 Dashboard 提供：泳道图、Activity 详情（三件套 + 证据链）、Agent 负载看板。
 *
 * @author SPK-OS
 */
@Tag(name = "SPK IPD Cockpit 监控台")
@RestController
@RequestMapping("/spk/ipd/cockpit")
@Validated
public class SpkIpdCockpitController {

    @Resource
    private SpkIpdCockpitService cockpitService;

    @GetMapping("/swimlane")
    @Operation(summary = "泳道图：按流程实例聚合阶段→Activity 卡片")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-cockpit:query')")
    public CommonResult<Map<String, Object>> swimlane(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId) {
        return success(cockpitService.swimlane(processInstanceId));
    }

    @GetMapping("/activity-detail")
    @Operation(summary = "Activity 详情：三件套 + 证据链哈希校验")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-cockpit:query')")
    public CommonResult<Map<String, Object>> activityDetail(
            @Parameter(description = "ActivityRun id") @RequestParam("activityRunId") String activityRunId) {
        return success(cockpitService.activityDetail(activityRunId));
    }

    @GetMapping("/agent-load")
    @Operation(summary = "Agent 负载看板：Lead+Verifier running 合同数 + 死信数")
    @PreAuthorize("@ss.hasPermission('spk-delivery:ipd-cockpit:query')")
    public CommonResult<Map<String, Object>> agentLoad() {
        return success(cockpitService.agentLoad());
    }

}
