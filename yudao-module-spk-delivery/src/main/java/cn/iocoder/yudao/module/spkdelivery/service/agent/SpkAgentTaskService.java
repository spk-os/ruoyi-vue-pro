package cn.iocoder.yudao.module.spkdelivery.service.agent;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;

/**
 * SPK-OS Agent 任务服务
 * <p>
 * 经 {@link FrameworkAdapter} 派发 agent 任务，落 spk_agent_task 实例，
 * 回调时更新状态/产物并推进 BPM 流程（triggerTask）。
 *
 * @author SPK-OS
 */
public interface SpkAgentTaskService {

    /**
     * 派发 agent 任务（由 BpmAgentTaskDelegate 调用）
     *
     * @param roleId          agent 角色编号
     * @param prompt          派发 prompt
     * @param instanceId      BPM 流程实例编号
     * @param nodeKey         BPM 节点 key
     * @param receiveTaskKey  紧随的 receiveTask key（回调 triggerTask 推进）
     * @return agent 任务实例
     */
    SpkAgentTaskDO dispatch(Long roleId, String prompt, String instanceId, String nodeKey, String receiveTaskKey);

    /**
     * 回调更新 agent 任务（由 /spk/agent-task/callback 调用）
     *
     * @param taskId    外部 runtime 任务编号
     * @param status    状态 running/done/failed
     * @param result    产物（JSON）
     */
    void callback(String taskId, String status, String result);

    /**
     * 根据流程实例与节点查询 agent 任务
     */
    SpkAgentTaskDO getByInstanceIdAndNodeKey(String instanceId, String nodeKey);

}
