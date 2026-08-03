package cn.iocoder.yudao.module.spkdelivery.framework.flowable.listener;

import cn.iocoder.yudao.module.spkdelivery.service.agent.SpkAgentTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * SPK-OS BpmAgentTaskDelegate —— agent 执行节点（异步派任务）的 Flowable JavaDelegate
 * <p>
 * 由 simpleModel AGENT_TASK 节点经 serviceTask(delegateExpression="${bpmAgentTaskDelegate}") 触发。
 * 读取流程变量 agentRoleId / taskPrompt，经 SpkAgentTaskService 派发任务。
 * <p>
 * 同步 runtime（如 native-ai）：dispatchTask 即 done，applyResultAndTrigger 自动写变量+推进 receiveTask。
 * 异步 runtime：dispatch 仅落 running，回调 /spk/agent-task/callback 再推进。
 *
 * @author SPK-OS
 */
@Slf4j
@Component("bpmAgentTaskDelegate")
public class BpmAgentTaskDelegate implements JavaDelegate {

    public static final String VAR_AGENT_ROLE_ID = "agentRoleId";
    public static final String VAR_TASK_PROMPT = "taskPrompt";
    public static final String VAR_RECEIVE_TASK_KEY = "receiveTaskKey";
    public static final String VAR_ACTIVITY_ID = "activityId";
    public static final String VAR_ACTIVITY_VERSION = "activityVersion";
    public static final String VAR_BUSINESS_KEY = "businessKey";

    @Resource
    private SpkAgentTaskService agentTaskService;

    @Override
    public void execute(DelegateExecution execution) {
        String instanceId = execution.getProcessInstanceId();
        String nodeKey = execution.getCurrentFlowElement() != null ? execution.getCurrentFlowElement().getId() : null;
        String activityId = Objects.toString(execution.getVariable(VAR_ACTIVITY_ID), null);
        // IPD 路由路径：按 Activity 定义派发（落三件套）
        if (activityId != null && !activityId.isBlank()) {
            String activityVersion = Objects.toString(execution.getVariable(VAR_ACTIVITY_VERSION), "1.0.0");
            String businessKey = Objects.toString(execution.getVariable(VAR_BUSINESS_KEY), null);
            log.info("[execute][IPD 路由派发 instanceId={} nodeKey={} activityId={}]", instanceId, nodeKey, activityId);
            agentTaskService.dispatchActivity(activityId, activityVersion, instanceId, null, businessKey, nodeKey, java.util.Collections.emptyList());
            return;
        }
        // 兼容旧路径：roleId + prompt 直派
        Long roleId = toLong(execution.getVariable(VAR_AGENT_ROLE_ID));
        String prompt = Objects.toString(execution.getVariable(VAR_TASK_PROMPT), null);
        String receiveTaskKey = Objects.toString(execution.getVariable(VAR_RECEIVE_TASK_KEY), null);
        log.info("[execute][派发 agent 任务 instanceId={} nodeKey={} roleId={}]", instanceId, nodeKey, roleId);
        agentTaskService.dispatch(roleId, prompt, instanceId, nodeKey, receiveTaskKey);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(value.toString());
    }

}
