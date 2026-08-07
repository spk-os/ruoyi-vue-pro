package cn.iocoder.yudao.module.spkdelivery.service.agent;

import cn.iocoder.yudao.module.bpm.api.task.BpmProcessTaskApi;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agent.SpkAgentTaskMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentTaskStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.service.router.SpkRouteResult;
import cn.iocoder.yudao.module.spkdelivery.service.router.SpkTaskRouterService;
import cn.iocoder.yudao.module.spkdelivery.service.feedback.SpkFeedbackService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Resource(name = "native-ai")
    private FrameworkAdapter frameworkAdapter;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private BpmProcessTaskApi processTaskApi;
    @Resource
    private SpkTaskRouterService taskRouterService;
    @Resource
    private SpkTaskContractMapper taskContractMapper;
    @Resource
    private SpkArtifactManifestMapper artifactManifestMapper;
    @Resource
    private SpkAgentDefMapper agentDefMapper;
    @Resource
    private SpkFeedbackService feedbackService;

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
    public SpkRouteResult dispatchActivity(String activityId, String activityVersion, String instanceId,
                                          String taskId, String businessKey, String nodeKey, List<String> inputRefs) {
        log.info("[dispatchActivity][activityId={} instanceId={} nodeKey={}]", activityId, instanceId, nodeKey);
        SpkRouteResult result = taskRouterService.route(activityId, activityVersion, instanceId,
                taskId, businessKey, nodeKey, inputRefs);
        // ⚠️ 不得在此处 runtimeService.setVariables 回写 agentResult：
        //   本方法由 BPM HTTP 触发器（type 15 sync serviceTask）经 /admin-api/spk/agent-task/run 同步回调，
        //   /run 处理线程（thread B）若 setVariables 会申请流程实例行锁，而 createProcessInstance 事务
        //   （thread A，触发器父命令）正持有该锁等待 HTTP 响应 → 互锁。
        //   agentResult 变量改由触发器 response 映射（spk-ipd-flow.json: response[{key=agentResult,value=result}]）
        //   在 thread A 自身事务内回写，锁安全。dispatchActivity 只返回产物，不碰 Flowable 运行时。
        return result;
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
        // P1 新路径：Agent 产物落在 spk_task_contract + spk_artifact_manifest，spk_agent_task 不再写入。
        // 为兼容前端「IPD 产物」tab（按 SpkAgentTaskDO 形态消费 nodeKey/roleId/status/taskId/result），
        // 这里把 contract + artifact 聚合成 SpkAgentTaskDO 列表返回；同时并入旧路径 spk_agent_task 行（按 nodeKey 去重）。
        List<SpkTaskContractDO> contracts = taskContractMapper.selectListByProcessInstanceId(instanceId);
        List<SpkAgentTaskDO> rows = new ArrayList<>();
        if (contracts != null && !contracts.isEmpty()) {
            // 批量解析 Lead Agent -> roleId
            List<Long> leadIds = contracts.stream()
                    .map(SpkTaskContractDO::getLeadAgentId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, SpkAgentDefDO> leadMap = new HashMap<>();
            if (!leadIds.isEmpty()) {
                List<SpkAgentDefDO> leads = agentDefMapper.selectBatchIds(leadIds);
                if (leads != null) {
                    for (SpkAgentDefDO d : leads) {
                        leadMap.put(d.getId(), d);
                    }
                }
            }
            for (SpkTaskContractDO c : contracts) {
                // 该 activityRunId 下的产物清单
                List<SpkArtifactManifestDO> arts = artifactManifestMapper.selectListByActivityRunId(c.getActivityRunId());
                String resultText = null;
                String artifactUris = null;
                if (arts != null && !arts.isEmpty()) {
                    resultText = arts.stream()
                            .map(a -> a.getSummary() != null ? a.getSummary() : a.getUri())
                            .collect(Collectors.joining(" | "));
                    artifactUris = arts.stream()
                            .map(SpkArtifactManifestDO::getUri)
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.joining(","));
                }
                Long roleId = null;
                if (c.getLeadAgentId() != null) {
                    SpkAgentDefDO lead = leadMap.get(c.getLeadAgentId());
                    if (lead != null) {
                        roleId = lead.getRoleId();
                    }
                }
                SpkAgentTaskDO t = SpkAgentTaskDO.builder()
                        .activityRunId(c.getActivityRunId())
                        .contractId(c.getContractId())
                        .activityId(c.getActivityId())
                        .roleId(roleId != null ? roleId : c.getLeadAgentId())
                        .prompt(c.getPrompt())
                        .status(c.getStatus())
                        .result(resultText)
                        .artifactUris(artifactUris)
                        .instanceId(c.getProcessInstanceId())
                        .nodeKey(c.getNodeKey())
                        .taskId(c.getTaskId() != null ? c.getTaskId() : null)
                        .build();
                rows.add(t);
            }
        }
        // 并入旧路径 spk_agent_task 行（按 nodeKey 去重，新路径优先）
        List<SpkAgentTaskDO> legacy = agentTaskMapper.selectListByInstanceId(instanceId);
        if (legacy != null && !legacy.isEmpty()) {
            java.util.Set<String> seen = rows.stream()
                    .map(SpkAgentTaskDO::getNodeKey)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            for (SpkAgentTaskDO t : legacy) {
                if (t.getNodeKey() == null || !seen.contains(t.getNodeKey())) {
                    rows.add(t);
                }
            }
        }
        return rows;
    }

    @Override
    public SpkRouteResult intervene(String activityRunId, String action, String note) {
        SpkTaskContractDO contract = taskContractMapper.selectByActivityRunId(activityRunId);
        if (contract == null) {
            throw exception(AGENT_TASK_NOT_EXISTS);
        }
        log.info("[intervene][activityRunId={} action={} note={}]", activityRunId, action, note);
        String act = action == null ? "note" : action.toLowerCase();
        switch (act) {
            case "rerun": {
                // 按 contract 上的 activityId 重新路由派发（生成新 activityRunId + 三件套）。
                // 注意：本方法不在 BPM 触发器事务上下文，setVariables 由 route 内部不触碰 Flowable 运行时（同 dispatchActivity 铁律）。
                return taskRouterService.route(contract.getActivityId(), contract.getActivityVersion(),
                        contract.getProcessInstanceId(), null, contract.getBusinessKey(),
                        contract.getNodeKey(), java.util.Collections.emptyList());
            }
            case "abort": {
                contract.setStatus("failed");
                if (note != null && !note.isBlank()) {
                    contract.setFailureReason(note);
                }
                taskContractMapper.updateById(contract);
                feedbackService.collect(contract.getProcessInstanceId(), "intervene-abort",
                        "abort " + activityRunId + "：" + note, note, false);
                return null;
            }
            case "note":
            default: {
                feedbackService.collect(contract.getProcessInstanceId(), "intervene-note",
                        "note " + activityRunId + "：" + note, note, false);
                return null;
            }
        }
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
