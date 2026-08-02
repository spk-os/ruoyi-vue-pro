package cn.iocoder.yudao.module.spkdelivery.service.agent;

import cn.iocoder.yudao.module.bpm.api.task.BpmProcessTaskApi;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agent.SpkAgentTaskMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentTaskStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.AGENT_TASK_ALREADY_DONE;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.AGENT_TASK_CALLBACK_FAIL;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.AGENT_TASK_DISPATCH_FAIL;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.AGENT_TASK_NOT_EXISTS;

/**
 * SPK-OS Agent 任务服务实现
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkAgentTaskServiceImpl implements SpkAgentTaskService {

    /** 流程变量名：agent 产物 */
    public static final String VAR_AGENT_RESULT = "agentResult";
    /** 流程变量名：agent 角色 id */
    public static final String VAR_AGENT_ROLE_ID = "agentRoleId";
    /** 流程变量名：派发 prompt */
    public static final String VAR_TASK_PROMPT = "taskPrompt";

    @Resource
    private SpkAgentTaskMapper agentTaskMapper;
    @Resource
    private FrameworkAdapter frameworkAdapter;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private BpmProcessTaskApi processTaskApi;

    @Override
    public SpkAgentTaskDO dispatch(Long roleId, String prompt, String instanceId, String nodeKey, String receiveTaskKey) {
        SpkAgentDispatchReq req = new SpkAgentDispatchReq()
                .setRoleId(roleId)
                .setPrompt(prompt)
                .setInstanceId(instanceId)
                .setNodeKey(nodeKey)
                .setReceiveTaskKey(receiveTaskKey);
        // 先落 running 实例（失败可追溯）
        SpkAgentTaskDO task = SpkAgentTaskDO.builder()
                .roleId(roleId)
                .prompt(prompt)
                .status(SpkAgentTaskStatusEnum.RUNNING.getLabel())
                .instanceId(instanceId)
                .nodeKey(nodeKey)
                .receiveTaskKey(receiveTaskKey)
                .build();
        agentTaskMapper.insert(task);
        try {
            SpkAgentDispatchResult result = frameworkAdapter.dispatchTask(req);
            // native 同步执行：派发即 done，回写产物并推进流程
            task.setTaskId(result.getTaskId());
            task.setConversationId(result.getConversationId());
            task.setResult(result.getResult());
            task.setStatus(result.getStatus());
            agentTaskMapper.updateById(task);
            // 若同步已完成，立即写变量 + trigger receiveTask 推进
            if (SpkAgentTaskStatusEnum.DONE.getLabel().equalsIgnoreCase(result.getStatus())) {
                applyResultAndTrigger(task);
            }
            return task;
        } catch (Exception e) {
            log.error("[dispatch][派发失败 instanceId={} nodeKey={} roleId={}]", instanceId, nodeKey, roleId, e);
            task.setStatus(SpkAgentTaskStatusEnum.FAILED.getLabel());
            agentTaskMapper.updateById(task);
            throw exception(AGENT_TASK_DISPATCH_FAIL);
        }
    }

    @Override
    public void callback(String taskId, String status, String result) {
        SpkAgentTaskDO task = agentTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            throw exception(AGENT_TASK_NOT_EXISTS);
        }
        SpkAgentTaskStatusEnum st = SpkAgentTaskStatusEnum.of(task.getStatus());
        if (st != null && st.isTerminal()) {
            throw exception(AGENT_TASK_ALREADY_DONE);
        }
        task.setStatus(status);
        task.setResult(result);
        agentTaskMapper.updateById(task);
        try {
            // 写流程变量 + 推进 receiveTask
            if (SpkAgentTaskStatusEnum.DONE.getLabel().equalsIgnoreCase(status)) {
                applyResultAndTrigger(task);
            }
        } catch (Exception e) {
            log.error("[callback][推进失败 taskId={} instanceId={}]", taskId, task.getInstanceId(), e);
            throw exception(AGENT_TASK_CALLBACK_FAIL);
        }
    }

    @Override
    public SpkAgentTaskDO getByInstanceIdAndNodeKey(String instanceId, String nodeKey) {
        SpkAgentTaskDO task = agentTaskMapper.selectByInstanceIdAndNodeKey(instanceId, nodeKey);
        if (task == null) {
            throw exception(AGENT_TASK_NOT_EXISTS);
        }
        return task;
    }

    @Override
    public List<SpkAgentTaskDO> getListByInstanceId(String instanceId) {
        return agentTaskMapper.selectListByInstanceId(instanceId);
    }

    /**
     * 把 agentResult 写入流程实例变量，并 trigger 紧随的 receiveTask 推进流程
     */
    private void applyResultAndTrigger(SpkAgentTaskDO task) {
        Map<String, Object> vars = new HashMap<>();
        vars.put(VAR_AGENT_RESULT, task.getResult());
        runtimeService.setVariables(task.getInstanceId(), vars);
        if (task.getReceiveTaskKey() != null) {
            processTaskApi.triggerTask(task.getInstanceId(), task.getReceiveTaskKey());
        }
    }

}
