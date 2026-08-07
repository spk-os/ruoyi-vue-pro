package cn.iocoder.yudao.module.spkdelivery.service.agent;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;
import cn.iocoder.yudao.module.spkdelivery.service.router.SpkRouteResult;

import java.util.List;

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
     * 按 Activity 定义派发任务（Cortext-IPD 路由路径）
     * <p>
     * 经 {@link cn.iocoder.yudao.module.spkdelivery.service.router.SpkTaskRouterService} 路由判决：
     * 加载 activity_def → 选 Lead → 冻结模型快照 → 装配 ContextManifest → 写 TaskContract →
     * 派发 → 落 Artifact/RunReceipt/(条件 VerificationReceipt)/Evidence 三件套。
     * 同步 runtime：派发即 done，回写 agentResult 变量并 trigger receiveTask 推进。
     *
     * @param activityId       Activity 业务标识
     * @param activityVersion  Activity 版本（可空，默认 1.0.0）
     * @param instanceId       BPM 流程实例 id
     * @param taskId           BPM task id（可空）
     * @param businessKey      业务 key（ipd_project_id，可空）
     * @param nodeKey          BPM 节点 key
     * @param inputRefs        输入产物 artifactId 列表（可空）
     * @return 路由执行结果（含 agentResult 供回写流程变量）
     */
    SpkRouteResult dispatchActivity(String activityId, String activityVersion, String instanceId,
                                    String taskId, String businessKey, String nodeKey, List<String> inputRefs);

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

    /**
     * 按流程实例查询全部 agent 任务（用于详情页 IPD 产物 tab）
     */
    List<SpkAgentTaskDO> getListByInstanceId(String instanceId);

    /**
     * 人工介入某 Activity 运行（Cockpit「介入」按钮）。
     * <ul>
     *   <li>rerun —— 按 contract 上的 activityId 重新路由派发（生成新 activityRunId + 三件套）。</li>
     *   <li>abort —— 标记当前合同 failed（死信，等人工兜底）。</li>
     *   <li>note —— 仅落 R7 反馈，不改变运行态。</li>
     * </ul>
     *
     * @param activityRunId Activity 运行实例编号
     * @param action        rerun / abort / note
     * @param note          介入备注（写反馈/失败原因）
     */
    SpkRouteResult intervene(String activityRunId, String action, String note);

}
