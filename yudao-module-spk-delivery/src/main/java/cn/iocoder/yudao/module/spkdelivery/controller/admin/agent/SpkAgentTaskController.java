package cn.iocoder.yudao.module.spkdelivery.controller.admin.agent;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agent.vo.SpkAgentTaskCallbackReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;
import cn.iocoder.yudao.module.spkdelivery.service.agent.SpkAgentTaskService;
import cn.iocoder.yudao.module.spkdelivery.service.router.SpkRouteResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * SPK-OS Agent 任务 Controller
 *
 * @author SPK-OS
 */
@Tag(name = "SPK Agent 任务")
@RestController
@RequestMapping("/spk/agent-task")
@Validated
public class SpkAgentTaskController {

    @Resource
    private SpkAgentTaskService agentTaskService;

    @PostMapping("/run")
    @PermitAll
    @ApiAccessLog(operateModule = "SPK IPD", operateName = "派发Agent任务")
    @Operation(summary = "派发 Agent 任务（BPM HTTP_CALLBACK 触发器调用，异步派发即返回，LLM 后台跑完回调推进 receiveTask）")
    public CommonResult<Map<String, Object>> run(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId,
            @Parameter(description = "agent 角色编号") @RequestParam(value = "roleId", required = false) Long roleId,
            @Parameter(description = "派发 prompt") @RequestParam(value = "prompt", required = false) String prompt,
            @Parameter(description = "BPM 节点 key") @RequestParam(value = "nodeKey", required = false) String nodeKey,
            @Parameter(description = "紧随的 receiveTask key（异步 runtime 回调推进用）") @RequestParam(value = "receiveTaskKey", required = false) String receiveTaskKey,
            @Parameter(description = "HTTP_CALLBACK 触发器自动注入的 receiveTask key（异步派发回调推进用）") @RequestParam(value = "taskDefineKey", required = false) String taskDefineKey,
            @Parameter(description = "IPD Activity 业务标识（Cortext-IPD 路由路径）") @RequestParam(value = "activityId", required = false) String activityId,
            @Parameter(description = "IPD Activity 版本") @RequestParam(value = "activityVersion", required = false) String activityVersion,
            @Parameter(description = "业务 key") @RequestParam(value = "businessKey", required = false) String businessKey) {
        Map<String, Object> data = new HashMap<>();
        // IPD 路由路径：按 Activity 定义异步派发，落三件套
        if (activityId != null && !activityId.isBlank()) {
            // 方案 A 异步派发：type=2 HTTP_CALLBACK 触发器发请求即卡 receiveTask 等回调推进；
            // 立即返回 dispatched，LLM 后台跑（不阻塞审批接口）。agentResult 由后台线程写变量 + trigger。
            agentTaskService.dispatchActivityAsync(activityId, activityVersion, processInstanceId,
                    null, businessKey, nodeKey, taskDefineKey, java.util.Collections.emptyList());
            data.put("status", "dispatched");
            data.put("activityId", activityId);
            data.put("nodeKey", nodeKey);
            return success(data);
        }
        // 兼容旧路径：roleId + prompt 直派
        SpkAgentTaskDO task = agentTaskService.dispatch(roleId, prompt, processInstanceId, nodeKey, receiveTaskKey);
        data.put("result", task.getResult());
        data.put("taskId", task.getTaskId());
        data.put("status", task.getStatus());
        return success(data);
    }

    @PostMapping("/callback")
    @PermitAll
    @Operation(summary = "Agent 任务回调（外部 runtime 完成后回写状态/产物并推进流程）")
    public CommonResult<Boolean> callback(@Valid @RequestBody SpkAgentTaskCallbackReqVO reqVO) {
        agentTaskService.callback(reqVO.getTaskId(), reqVO.getStatus(), reqVO.getResult());
        return success(true);
    }

    @GetMapping("/list-by-instance")
    @Operation(summary = "按流程实例查询 Agent 任务产物（IPD 产物 tab）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent:query')")
    public CommonResult<List<SpkAgentTaskDO>> listByInstance(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId) {
        return success(agentTaskService.getListByInstanceId(processInstanceId));
    }

    @PostMapping("/{id}/intervene")
    @ApiAccessLog(operateModule = "SPK IPD", operateName = "人工介入Activity")
    @Operation(summary = "人工介入 Activity 运行（rerun 重新派发 / abort 标记失败 / note 落反馈）")
    @PreAuthorize("@ss.hasPermission('spk-delivery:agent:intervene')")
    public CommonResult<Map<String, Object>> intervene(
            @Parameter(description = "Activity 运行实例编号") @PathVariable("id") String activityRunId,
            @Parameter(description = "介入动作 rerun/abort/note") @RequestParam(value = "action", defaultValue = "note") String action,
            @Parameter(description = "介入备注") @RequestParam(value = "note", required = false) String note) {
        SpkRouteResult result = agentTaskService.intervene(activityRunId, action, note);
        Map<String, Object> data = new HashMap<>();
        if (result != null) {
            data.put("activityRunId", result.getActivityRunId());
            data.put("contractId", result.getContractId());
            data.put("artifactId", result.getArtifactId());
            data.put("status", result.getStatus());
            data.put("result", result.getAgentResult());
        } else {
            data.put("action", action);
            data.put("activityRunId", activityRunId);
        }
        return success(data);
    }

}
