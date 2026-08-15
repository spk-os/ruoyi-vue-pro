package cn.iocoder.yudao.module.spkdelivery.service.agent;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessTaskApi;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.agent.vo.SpkAgentLoadStatsVO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agent.SpkAgentTaskMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentTaskStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkTaskContractStatusEnum;
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

    /**
     * fencing 令牌单调计数器（单实例足够；多实例需换 DB sequence）。
     * 每次 dispatch/claim 发一个新 token，回调回写时校验 presented token == 当前，
     * 不匹配（旧 token < 当前）视为陈旧回写，拒绝落库（设计 §15.5.4）。
     */
    private final java.util.concurrent.atomic.AtomicLong fencingCounter =
            new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());

    private long nextFencingToken() {
        return fencingCounter.incrementAndGet();
    }

    /**
     * 校验回调回写是否陈旧：presented token 小于任务当前 token → 已被后续 claim 覆盖，拒绝。
     * presented token 为 null（同步派发/无 token 回调）时不阻拦，兼容既有路径。
     */
    private boolean isStaleCallback(SpkAgentTaskDO task, Long presentedToken) {
        if (presentedToken == null || task.getFencingToken() == null) {
            return false;
        }
        return presentedToken < task.getFencingToken();
    }

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
                .attemptNo(1)
                .fencingToken(nextFencingToken())
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
                taskId, businessKey, nodeKey, null, inputRefs);
        // ⚠️ 不得在此处 runtimeService.setVariables 回写 agentResult：
        //   本方法由 BPM HTTP 触发器（type 15 sync serviceTask）经 /admin-api/spk/agent-task/run 同步回调，
        //   /run 处理线程（thread B）若 setVariables 会申请流程实例行锁，而 createProcessInstance 事务
        //   （thread A，触发器父命令）正持有该锁等待 HTTP 响应 → 互锁。
        //   agentResult 变量改由触发器 response 映射（spk-ipd-flow.json: response[{key=agentResult,value=result}]）
        //   在 thread A 自身事务内回写，锁安全。dispatchActivity 只返回产物，不碰 Flowable 运行时。
        return result;
    }

    @Override
    public void dispatchActivityAsync(String activityId, String activityVersion, String instanceId,
                                     String taskId, String businessKey, String nodeKey,
                                     String receiveTaskKey, List<String> inputRefs) {
        log.info("[dispatchActivityAsync][activityId={} instanceId={} nodeKey={} receiveTaskKey={}]",
                activityId, instanceId, nodeKey, receiveTaskKey);
        // 捕获主线程租户：supplyAsync 默认走 ForkJoinPool.commonPool，未包装 TTL，
        // TenantContextHolder 不自动传到子线程 → route 内部写库/查配置丢租户（[[spk-ipd-intake-three-fixes]]）。
        final Long tenantId = TenantContextHolder.getTenantId();
        // fire-and-forget：立即返回，LLM 派发到独立线程池后台跑。/run 线程（thread B）不阻塞 →
        // 触发器发请求即毫秒级返回 → 流程卡 receiveTask（wait state）→ complete 事务提交 → approve 接口毫秒级返回。
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            Long prev = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
            } else {
                TenantContextHolder.clear();
            }
            try {
                // 独立线程跑 route（同步 LLM 落三件套）。后台线程独立，setVariables+triggerTask 不与
                // 触发器线程互锁（[[flowable-sync-trigger-deadlock]]：互锁仅在触发器线程内 setVariables 时发生）。
                // complete 事务毫秒级提交，LLM 数十秒~3min，trigger 时 receiveTask 已落库可见。
                SpkRouteResult result = taskRouterService.route(activityId, activityVersion, instanceId,
                        taskId, businessKey, nodeKey, receiveTaskKey, inputRefs);
                // 流程严格性：仅 activity 成功才推进 receiveTask；failed 不 trigger，流程卡在该节点，
                // 等 scanTimeout 周期 rerun（attempt<max）或人工 intervene 兜底——避免"前面没完成就跑后面"。
                if (SpkTaskContractStatusEnum.FAILED.getLabel().equalsIgnoreCase(result.getStatus())) {
                    log.warn("[dispatchActivityAsync][activity failed status={} 不推进 receiveTask={} activityRunId={}，等 scanTimeout/人工介入]",
                            result.getStatus(), receiveTaskKey, result.getActivityRunId());
                } else {
                    applyResultAndTrigger(instanceId, receiveTaskKey, result.getAgentResult());
                    log.info("[dispatchActivityAsync][后台 route 完成 activityId={} activityRunId={} instanceId={}]",
                            activityId, result.getActivityRunId(), instanceId);
                }
                return result;
            } catch (Exception e) {
                // LLM 失败/超时：receiveTask 永久卡住，流程停滞，由 Cockpit「介入」(rerun/abort) 人工兜底
                // （[[cortext-ipd-d2-d8-gap-impl]] 的 TimeoutJob 兜底待后续补）。
                log.error("[dispatchActivityAsync][后台 route 失败 activityId={} instanceId={} nodeKey={}]",
                        activityId, instanceId, nodeKey, e);
                return null;
            } finally {
                if (prev != null) {
                    TenantContextHolder.setTenantId(prev);
                } else {
                    TenantContextHolder.clear();
                }
            }
        });
        // 立即返回，不等 LLM；receiveTask 卡住流程等本后台回调推进
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
                        .agentDefId(c.getLeadAgentId())
                        .squadId(c.getWorkerSquadId())
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
                // D5 fencing：新合同继承旧 fencing_token+1 / attempt_no+1，使旧合同的陈旧回调（token<current）被拒。
                long oldFence = contract.getFencingToken() == null ? 0L : contract.getFencingToken();
                int oldAttempt = contract.getAttemptNo() == null ? 0 : contract.getAttemptNo();
                SpkRouteResult reran = taskRouterService.route(contract.getActivityId(), contract.getActivityVersion(),
                        contract.getProcessInstanceId(), null, contract.getBusinessKey(),
                        contract.getNodeKey(), contract.getReceiveTaskKey(), java.util.Collections.emptyList());
                SpkTaskContractDO next = taskContractMapper.selectByActivityRunId(reran.getActivityRunId());
                if (next != null) {
                    next.setFencingToken(oldFence + 1);
                    next.setAttemptNo(oldAttempt + 1);
                    taskContractMapper.updateById(next);
                    log.info("[intervene][rerun activityRunId={} → newRunId={} fence={}→{} attempt={}→{}]",
                            activityRunId, reran.getActivityRunId(), oldFence, oldFence + 1,
                            oldAttempt, oldAttempt + 1);
                }
                // 恢复闭环：rerun 成功则推进卡住的 receiveTask；仍 failed 则继续卡住（严格流程，前面没完成不跑后面）。
                if (SpkTaskContractStatusEnum.DONE.getLabel().equalsIgnoreCase(reran.getStatus())) {
                    String effKey = resolveReceiveTaskKey(contract.getProcessInstanceId(),
                            next != null ? next.getReceiveTaskKey() : contract.getReceiveTaskKey());
                    if (effKey != null) {
                        applyResultAndTrigger(contract.getProcessInstanceId(), effKey, reran.getAgentResult());
                        log.info("[intervene][rerun 成功推进 receiveTask={} pid={}]", effKey, contract.getProcessInstanceId());
                    } else {
                        log.warn("[intervene][rerun 成功但未找到 pending receiveTask，无法推进 pid={}]", contract.getProcessInstanceId());
                    }
                } else {
                    log.warn("[intervene][rerun 仍 failed status={} 流程继续卡住 activityRunId={}]",
                            reran.getStatus(), activityRunId);
                }
                return reran;
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

    /**
     * 异步派发路径回写：独立线程跑完 route 后，用 instanceId/receiveTaskKey/agentResult 直接
     * 写流程变量并 trigger receiveTask 推进（不经 SpkAgentTaskDO，P1 新路径产物落在 contract/artifact）。
     */
    private void applyResultAndTrigger(String instanceId, String receiveTaskKey, String agentResult) {
        Map<String, Object> vars = new HashMap<>();
        vars.put(VAR_AGENT_RESULT, agentResult);
        runtimeService.setVariables(instanceId, vars);
        if (receiveTaskKey != null) {
            processTaskApi.triggerTask(instanceId, receiveTaskKey);
        }
    }

    /**
     * 解析用于推进的 receiveTask key：
     * <ul>
     *   <li>新实例：contract 持久化了 {@code receiveTaskKey}（type=2 HTTP_CALLBACK 触发器注入的 taskDefineKey）。</li>
     *   <li>legacy 实例（receiveTaskKey 未持久化，如修复前已卡死的 05116b7d）：查 Flowable runtime
     *       当前等待活动，IPD 顺序流唯一活动即卡住的 receiveTask，其 id 形如 {@code "Activity_<uuid>"}
     *       （{@code SimpleModelUtils:767}：{@code receiveTask.setId("Activity_"+UUID)}）。</li>
     * </ul>
     */
    private String resolveReceiveTaskKey(String processInstanceId, String storedKey) {
        if (storedKey != null && !storedKey.isBlank()) {
            return storedKey;
        }
        List<String> activeIds = runtimeService.getActiveActivityIds(processInstanceId);
        if (activeIds == null || activeIds.isEmpty()) {
            return null;
        }
        return activeIds.stream()
                .filter(id -> id != null && id.startsWith("Activity_"))
                .findFirst()
                .orElse(activeIds.get(0));
    }

    @Override
    public List<SpkAgentLoadStatsVO> getLoadStats() {
        // 真实聚合 spk_task_contract（IPD 每次运行都落行）；空即真实无数据，不造假
        List<SpkAgentLoadStatsVO> rows = agentTaskMapper.selectLoadStats();
        if (rows == null || rows.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // 补 successRate + lastError（best-effort，失败原因查不到则留空，不编造）
        for (SpkAgentLoadStatsVO r : rows) {
            int done = r.getSucceeded() == null ? 0 : r.getSucceeded();
            int total = r.getTotal() == null ? 0 : r.getTotal();
            // 样本不足（<3 次）不计算成功率，前端显式标"样本不足"
            r.setSuccessRate(total >= 3 ? (total == 0 ? 0.0 : (double) done / total) : null);
            try {
                r.setLastError(agentTaskMapper.selectLastErrorByDefId(r.getAgentDefId()));
            } catch (Exception ignore) {
                // 查询失败留空，不阻断聚合主流程
            }
        }
        return rows;
    }

}
