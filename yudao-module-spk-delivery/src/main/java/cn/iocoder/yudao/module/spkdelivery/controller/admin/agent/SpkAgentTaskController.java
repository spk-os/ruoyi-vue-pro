package cn.iocoder.yudao.module.spkdelivery.controller.admin.agent;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agent.vo.SpkAgentTaskCallbackReqVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;
import cn.iocoder.yudao.module.spkdelivery.service.agent.SpkAgentTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
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
    @Operation(summary = "派发 Agent 任务（BPM HTTP_REQUEST 触发器调用，同步执行并回写 agentResult 变量）")
    public CommonResult<Map<String, Object>> run(
            @Parameter(description = "流程实例编号") @RequestParam("processInstanceId") String processInstanceId,
            @Parameter(description = "agent 角色编号") @RequestParam(value = "roleId", required = false) Long roleId,
            @Parameter(description = "派发 prompt") @RequestParam(value = "prompt", required = false) String prompt,
            @Parameter(description = "BPM 节点 key") @RequestParam(value = "nodeKey", required = false) String nodeKey,
            @Parameter(description = "紧随的 receiveTask key（异步 runtime 回调推进用）") @RequestParam(value = "receiveTaskKey", required = false) String receiveTaskKey) {
        SpkAgentTaskDO task = agentTaskService.dispatch(roleId, prompt, processInstanceId, nodeKey, receiveTaskKey);
        // 返回 CommonResult<Map>，供 HTTP 触发器 response 设置回写为流程变量
        Map<String, Object> data = new HashMap<>();
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

}
