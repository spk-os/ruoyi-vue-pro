package cn.iocoder.yudao.module.spkdelivery.service.project;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.service.cockpit.SpkIpdCockpitService;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkGiteaIntegrationService;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkPlaneIntegrationService;
import com.alibaba.ttl.threadpool.TtlExecutors;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * SPK-OS Cortext-IPD 项目服务
 * <p>
 * 项目的"身份证"是 businessKey（如 ipd-2026-0042）；运行态载体是 BPM 流程实例 processInstanceId。
 * 本服务提供：发起 IPD 主流程、查项目总览（复用 Cockpit 泳道）、查各阶段进度、
 * 以及代理 Plane/Gitea/Omnigent 集成产物的只读视图（设计文档 §4 集成层）。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkIpdProjectService {

    /** IPD 主流程定义 key（与 SpkIpdFlowDeployRunner / simpleModel 对齐） */
    public static final String IPD_FLOW_KEY = "spkIpdFlow";

    /** 六阶段固定顺序（与 spk_ipd_activity_def.stage 枚举对齐） */
    private static final List<String> STAGE_ORDER = Arrays.asList(
            "concept", "plan", "develop", "qualify", "launch", "lifecycle");

    @Resource
    private BpmProcessInstanceApi processInstanceApi;
    @Resource
    private SpkTaskContractMapper contractMapper;
    @Resource
    private SpkIpdCockpitService cockpitService;
    @Resource
    private SpkPlaneIntegrationService planeService;
    @Resource
    private SpkGiteaIntegrationService giteaService;
    @Resource
    private HistoryService historyService;

    /**
     * 后台发起 IPD 流程的线程池：用 {@link TtlExecutors} 包装，自动传播租户上下文
     * （{@code TenantContextHolder} 是 {@code TransmittableThreadLocal}）到后台线程，
     * 否则 Flowable 同步执行的服务任务查 spk_* 表会丢租户过滤返回空。
     * <p>
     * 单线程队列串行发起：dev/demo 流量足够，且避免并发 process start 对同一流程定义的部署竞争。
     */
    private final ExecutorService startExecutor = TtlExecutors.getTtlExecutorService(
            new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(200),
                    r -> { Thread t = new Thread(r, "spk-ipd-start-async"); t.setDaemon(true); return t; }));

    /** 发起中流程的状态表（businessKey → StartRecord）。内存态，进程重启即失。 */
    private final ConcurrentMap<String, StartRecord> startRecords = new ConcurrentHashMap<>();

    /**
     * 流程发起态记录。
     * <ul>
     *   <li>{@code starting} —— createProcessInstance 在后台线程同步执行至第一个 wait state（CDCP 门，约 3 分钟）。</li>
     *   <li>{@code done} —— 已到 wait state，{@link #processInstanceId} 可用。</li>
     *   <li>{@code failed} —— 后台发起异常，{@link #error} 为根因。</li>
     * </ul>
     */
    private static final class StartRecord {
        final String status;
        final String businessKey;
        final String processInstanceId;
        final String error;

        static StartRecord starting(String bk) { return new StartRecord("starting", bk, null, null); }
        static StartRecord done(String bk, String pid) { return new StartRecord("done", bk, pid, null); }
        static StartRecord failed(String bk, String err) { return new StartRecord("failed", bk, null, err); }

        private StartRecord(String status, String businessKey, String processInstanceId, String error) {
            this.status = status;
            this.businessKey = businessKey;
            this.processInstanceId = processInstanceId;
            this.error = error;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", status);
            m.put("businessKey", businessKey);
            m.put("processInstanceId", processInstanceId);
            if (error != null) {
                m.put("error", error);
            }
            return m;
        }
    }

    /**
     * 发起 IPD 主流程（异步）。
     * <p>
     * <b>为何异步</b>：{@code processInstanceApi.createProcessInstance} 调 Flowable
     * {@code startProcessInstanceByKey}，同步执行所有 serviceTask 直到第一个 wait state
     * （CDCP userTask assignee=1）才返回，concept 阶段 5 个服务任务约耗时 3 分钟。同步会让
     * 前端按钮转 3 分钟、网关 504。改为：立即返 businessKey，后台线程跑流，前端按
     * {@link #getByBusinessKey} 轮询，done 后取 processInstanceId 载入泳道。
     *
     * @param businessKey  项目业务 key（ipd-2026-0042）；为空则自动生成
     * @param projectName  项目名（写入流程变量，概念阶段 agent 可读）
     * @param payload      附加 payload（OR 池原始需求等，可空）
     * @param mode         运行模式 test/product（写入流程变量 spk_mode，route 内只读；
     *                      product=concept 阶段起执行真实交付动作；空=test）
     * @return {businessKey, status:"starting"} —— status 转 done 后 processInstanceId 见 {@link #getByBusinessKey}
     */
    public Map<String, Object> start(String businessKey, String projectName, String payload, String mode) {
        if (businessKey == null || businessKey.isBlank()) {
            businessKey = "ipd-" + System.currentTimeMillis();
        }
        final String bk = businessKey;
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            userId = 1L; // 兜底：system 用户（与 spk-delivery.self.system-user-id 对齐）
        }
        final Long starterId = userId;

        BpmProcessInstanceCreateReqDTO createReq = new BpmProcessInstanceCreateReqDTO();
        createReq.setProcessDefinitionKey(IPD_FLOW_KEY);
        createReq.setBusinessKey(bk);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("businessKey", bk);
        variables.put("projectName", projectName);
        if (payload != null && !payload.isBlank()) {
            variables.put("projectPayload", payload);
        }
        // spk_mode：test=桩仅测试 / product=真实交付。route 内只读 getVariable（触发器线程安全，
        // 不触写锁，遵守 [[flowable-sync-trigger-deadlock]] 铁律）。空/null 归一为 test。
        String normMode = (mode == null || mode.isBlank()) ? "test" : mode.trim().toLowerCase();
        variables.put("spk_mode", normMode);
        createReq.setVariables(variables);

        // 标记 starting，立即返回 businessKey；createProcessInstance 在后台线程跑（同步执行至 CDCP 门约 3 分钟）
        startRecords.put(bk, StartRecord.starting(bk));
        startExecutor.submit(() -> {
            try {
                String instanceId = processInstanceApi.createProcessInstance(starterId, createReq);
                startRecords.put(bk, StartRecord.done(bk, instanceId));
                log.info("[start-async][businessKey={} userId={} instanceId={} 已到 wait state]",
                        bk, starterId, instanceId);
            } catch (Throwable t) {
                startRecords.put(bk, StartRecord.failed(bk, t.getMessage()));
                log.error("[start-async][businessKey={} 发起失败]", bk, t);
            }
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessKey", bk);
        result.put("status", "starting");
        result.put("mode", normMode);
        result.put("hint", "IPD 流程后台发起中（同步执行至 CDCP 门约 3 分钟），轮询 GET /spk/ipd/project/by-key/"
                + bk + " 取 processInstanceId");
        log.info("[start][businessKey={} projectName={} mode={} userId={} 已提交后台发起]", bk, projectName, normMode, starterId);
        return result;
    }

    /**
     * 按 businessKey 查发起态：starting / done / failed / not_found。
     * <p>
     * 前端发起后轮询此接口，{@code done} 时取 {@code processInstanceId} 再载入泳道；
     * {@code failed} 时取 {@code error} 报错；{@code not_found} 说明内存态丢失（如服务重启），需重新发起。
     */
    public Map<String, Object> getByBusinessKey(String businessKey) {
        if (businessKey == null || businessKey.isBlank()) {
            throw new IllegalArgumentException("businessKey 不能为空");
        }
        StartRecord rec = startRecords.get(businessKey);
        if (rec != null) {
            return rec.toMap();
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "not_found");
        m.put("businessKey", businessKey);
        return m;
    }

    @PreDestroy
    public void shutdown() {
        startExecutor.shutdown();
        try {
            if (!startExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                startExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            startExecutor.shutdownNow();
        }
    }

    /**
     * 项目总览：泳道 + 汇总（复用 Cockpit）。
     */
    public Map<String, Object> getProject(String processInstanceId) {
        Map<String, Object> swimlane = cockpitService.swimlane(processInstanceId);
        // 附加六阶段进度
        swimlane.put("phases", getPhases(processInstanceId));
        return swimlane;
    }

    /**
     * 最新 IPD 流程实例（按发起时间倒序）。
     * <p>
     * 供「IPD 项目」/「IPD 监控台」进入界面默认载入：查 spkIpdFlow 流程定义下 start_time 最新的实例，
     * 返回 processInstanceId / businessKey / projectName / startTime / running。
     * 无实例时返回空 map（前端按 not_found 处理，不阻塞页面）。
     */
    public Map<String, Object> getLatest() {
        List<HistoricProcessInstance> list = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(IPD_FLOW_KEY)
                .orderByProcessInstanceStartTime().desc()
                .listPage(0, 1);
        Map<String, Object> m = new LinkedHashMap<>();
        if (list == null || list.isEmpty()) {
            m.put("status", "not_found");
            return m;
        }
        HistoricProcessInstance pi = list.get(0);
        String pid = pi.getId();
        // projectName 是 start/intake 时写入的流程变量（act_ru_variable / act_hi_varinst）
        String projectName = null;
        try {
            List<HistoricVariableInstance> vars = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(pid).variableName("projectName").list();
            if (vars != null && !vars.isEmpty() && vars.get(0).getValue() != null) {
                projectName = String.valueOf(vars.get(0).getValue());
            }
        } catch (Exception e) {
            log.warn("[getLatest][读 projectName 变量失败 pid={} {}]", pid, e.getMessage());
        }
        m.put("status", "ok");
        m.put("processInstanceId", pid);
        m.put("businessKey", pi.getBusinessKey());
        m.put("projectName", projectName);
        m.put("startTime", pi.getStartTime() == null ? null
                : java.time.LocalDateTime.ofInstant(pi.getStartTime().toInstant(), java.time.ZoneId.systemDefault()).toString());
        m.put("running", pi.getEndTime() == null);
        return m;
    }

    /**
     * 各阶段进度：按 phase 聚合合同 done/running/failed/total，并给出阶段状态。
     */
    public List<Map<String, Object>> getPhases(String processInstanceId) {
        List<SpkTaskContractDO> contracts = contractMapper.selectListByProcessInstanceId(processInstanceId);
        // 按 nodeKey 去重：intervene rerun 生成新合同（同 nodeKey 新 activityRunId），只统计每个节点最新合同，
        // 否则已 rerun 恢复的节点仍计旧 failed 合同，导致阶段状态误报 blocked。
        Map<String, SpkTaskContractDO> latestByNode = new LinkedHashMap<>();
        for (SpkTaskContractDO c : contracts) {
            String key = c.getNodeKey() != null ? c.getNodeKey() : c.getActivityRunId();
            latestByNode.put(key, c); // 后者覆盖前者 = 最新（contracts 已按 queuedAt ASC）
        }
        Map<String, int[]> acc = new LinkedHashMap<>(); // phase -> [done,running,failed,total]
        for (SpkTaskContractDO c : latestByNode.values()) {
            String phase = c.getPhase() != null ? c.getPhase() : "unknown";
            int[] a = acc.computeIfAbsent(phase, k -> new int[4]);
            a[3]++;
            String s = c.getStatus() == null ? "" : c.getStatus().toLowerCase();
            if (s.contains("done") || s.contains("completed") || s.contains("pass")) {
                a[0]++;
            } else if (s.contains("run")) {
                a[1]++;
            } else if (s.contains("fail")) {
                a[2]++;
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String stage : STAGE_ORDER) {
            int[] a = acc.getOrDefault(stage, new int[4]);
            rows.add(phaseRow(stage, a));
        }
        // 未归类的 unknown 阶段尾巴
        if (acc.containsKey("unknown")) {
            rows.add(phaseRow("unknown", acc.get("unknown")));
        }
        return rows;
    }

    private Map<String, Object> phaseRow(String stage, int[] a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("phase", stage);
        m.put("total", a[3]);
        m.put("done", a[0]);
        m.put("running", a[1]);
        m.put("failed", a[2]);
        String status;
        if (a[3] == 0) {
            status = "pending";
        } else if (a[0] == a[3]) {
            status = "completed";
        } else if (a[2] > 0) {
            status = "blocked";
        } else if (a[1] > 0 || a[0] > 0) {
            status = "running";
        } else {
            status = "pending";
        }
        m.put("status", status);
        return m;
    }

    /**
     * Plane 需求代理（Dashboard 需求 Tab）：返回 project 下 issue 列表 JSON。
     */
    public String getRequirements(int limit) {
        return planeService.listIssues(limit);
    }

    /**
     * Gitea PR/CI 代理：返回最近 CI 运行状态 + repo URL。
     */
    public Map<String, Object> getGitPr() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("repoUrl", giteaService.getRepoUrl());
        m.put("ciStatus", giteaService.getCiRunStatus());
        return m;
    }

    /**
     * Gitea Release 代理（占位：返回 repo releases 页 URL；后端不拉列表以省流量）。
     */
    public Map<String, Object> getGitRelease() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("releasesUrl", giteaService.getRepoUrl() + "/releases");
        return m;
    }

}
