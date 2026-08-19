package cn.iocoder.yudao.module.spkdelivery.service.ipdworkbench;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdCommandVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdWorkbenchRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdWorkbenchRespVO.BoardColumn;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdWorkbenchRespVO.BoardItem;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdWorkbenchRespVO.InboxItem;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdWorkbenchRespVO.TaskRow;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agent.SpkAgentTaskDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkRunReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdMajorReleaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agent.SpkAgentTaskMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkRunReceiptMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFlowRunMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdProjectMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentTaskStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkTaskContractStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.service.agent.SpkAgentTaskService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdCommandService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdCommandService.CommandEnvelope;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdFlowRunService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdProjectBusinessService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkStageResolver;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * IPD 指挥工作台实现（设计文档 §7.5 / §10.9）。
 * <p>
 * 聚合原生 BPM 待办 + spk-delivery 异常/阻断/证据缺口，统一成行动队列；
 * 阶段看板按 Task Contract 的 phase 分列；自然语言命令只解析预览，确认后走白名单幂等执行。
 * 不改 BPM 引擎语义。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkIpdWorkbenchServiceImpl implements SpkIpdWorkbenchService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private SpkIpdProjectBusinessService projectBusinessService;
    @Resource
    private SpkIpdFlowRunService flowRunService;
    @Resource
    private SpkIpdFlowRunMapper flowRunMapper;
    @Resource
    private SpkTaskContractMapper taskContractMapper;
    @Resource
    private SpkAgentTaskMapper agentTaskMapper;
    @Resource
    private SpkAgentDefMapper agentDefMapper;
    @Resource
    private SpkRunReceiptMapper runReceiptMapper;
    @Resource
    private SpkIpdCommandService commandService;
    @Resource
    private SpkAgentTaskService agentTaskService;
    @Resource
    private BpmTaskService bpmTaskService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private SpkStageResolver stageResolver;
    @Resource
    private SpkIpdProjectMapper projectMapper;

    // v4.0 §4.4 全局执行参数：真实配置只读快照（@Value 注入，非写死）
    @Value("${spk-delivery.execution.adapter:native-ai}")
    private String cfgAdapter;
    @Value("${spk-delivery.execution.fast-mode:true}")
    private boolean cfgFastMode;
    @Value("${spk-delivery.execution.default-mode:test}")
    private String cfgDefaultMode;
    @Value("${spk-delivery.omnigent.timeout-ms:120000}")
    private long cfgOmnigentTimeoutMs;
    @Value("${spk-delivery.omnigent.poll-interval-ms:2000}")
    private long cfgOmnigentPollMs;

    @Override
    public SpkIpdWorkbenchRespVO getWorkbench(Long userId, Long projectId, Long versionId) {
        SpkIpdWorkbenchRespVO resp = new SpkIpdWorkbenchRespVO();
        resp.setRefreshedAt(LocalDateTime.now().format(FMT));
        resp.setContext(buildContext(projectId, versionId));
        resp.setNextGate(detectNextGate(projectId, versionId));
        resp.setInbox(buildInbox(userId, projectId));
        resp.setBoard(buildBoard(projectId, versionId));
        // v4.0 §4.4 三视图：任务/指挥/团队数据同源注入
        resp.setTasks(buildTasks(projectId));
        resp.setTaskStats(buildTaskStats(resp.getTasks()));
        resp.setKpis(buildKpis(projectId, resp.getTasks()));
        resp.setGlobalParams(buildGlobalParams());
        resp.setEventStream(buildEventStream(projectId));
        return resp;
    }

    @Override
    public SpkIpdWorkbenchRespVO getInbox(Long userId, Long projectId) {
        SpkIpdWorkbenchRespVO resp = new SpkIpdWorkbenchRespVO();
        resp.setRefreshedAt(LocalDateTime.now().format(FMT));
        resp.setInbox(buildInbox(userId, projectId));
        // v4.0 §4.4：inbox 端点亦承载任务视图（设计文档关键 API 即 /workbench/inbox）
        resp.setTasks(buildTasks(projectId));
        resp.setTaskStats(buildTaskStats(resp.getTasks()));
        resp.setKpis(buildKpis(projectId, resp.getTasks()));
        resp.setGlobalParams(buildGlobalParams());
        resp.setEventStream(buildEventStream(projectId));
        return resp;
    }

    @Override
    public SpkIpdWorkbenchRespVO getBoard(Long projectId) {
        SpkIpdWorkbenchRespVO resp = new SpkIpdWorkbenchRespVO();
        resp.setRefreshedAt(LocalDateTime.now().format(FMT));
        resp.setBoard(buildBoard(projectId, null));
        // v4.0 §4.4：board 端点亦补任务列表，供指挥视图复用
        resp.setTasks(buildTasks(projectId));
        resp.setTaskStats(buildTaskStats(resp.getTasks()));
        resp.setKpis(buildKpis(projectId, resp.getTasks()));
        resp.setGlobalParams(buildGlobalParams());
        return resp;
    }

    // ===== 行动队列 =====

    private List<InboxItem> buildInbox(Long userId, Long projectId) {
        List<InboxItem> items = new ArrayList<>();
        // 1) 原生 BPM 待办（IPD 流程）
        items.addAll(buildMyTodo(userId));
        // 2) Agent 失败 / 同步失败（task contract failed）
        items.addAll(buildSyncFailures(projectId));
        // 3) 阻断的 FlowRun
        items.addAll(buildBlockedFlows(projectId));
        return items;
    }

    private List<InboxItem> buildMyTodo(Long userId) {
        List<InboxItem> items = new ArrayList<>();
        if (userId == null) {
            return items;
        }
        try {
            BpmTaskPageReqVO req = new BpmTaskPageReqVO();
            req.setPageNo(1);
            req.setPageSize(PageParam.PAGE_SIZE_NONE);
            req.setProcessDefinitionKey(cn.iocoder.yudao.module.spkdelivery.service.project.SpkIpdProjectService.IPD_FLOW_KEY);
            PageResult<Task> page = bpmTaskService.getTaskTodoPage(userId, req);
            if (page.getList() == null) {
                return items;
            }
            for (Task tk : page.getList()) {
                InboxItem it = new InboxItem();
                it.setType("MY_TODO");
                it.setSeverity("P1");
                it.setTitle("待审批：" + (tk.getName() == null ? tk.getId() : tk.getName()));
                it.setDetail("Flowable 任务 " + tk.getId() + "，等待人工决策");
                it.setRefType("APPROVAL");
                it.setRefId(tk.getId());
                SpkIpdFlowRunDO run = flowRunService.getByProcessInstanceId(tk.getProcessInstanceId());
                if (run != null) {
                    it.setFlowRunId(run.getId());
                    it.setProjectId(run.getProjectId());
                }
                it.setAction("APPROVE");
                items.add(it);
            }
        } catch (Exception e) {
            log.warn("[buildMyTodo][查询 BPM 待办失败 userId={} err={}]", userId, e.getMessage());
        }
        return items;
    }

    private List<InboxItem> buildSyncFailures(Long projectId) {
        List<InboxItem> items = new ArrayList<>();
        List<SpkTaskContractDO> failed = taskContractMapper.selectListByStatus(
                SpkTaskContractStatusEnum.FAILED.getLabel());
        if (failed == null) {
            return items;
        }
        Map<Long, SpkAgentDefDO> defMap = lookupAgentDefs(failed);
        for (SpkTaskContractDO c : failed) {
            if (projectId != null) {
                SpkIpdFlowRunDO run = c.getProcessInstanceId() == null ? null
                        : flowRunService.getByProcessInstanceId(c.getProcessInstanceId());
                if (run == null || !projectId.equals(run.getProjectId())) {
                    continue;
                }
            }
            InboxItem it = new InboxItem();
            it.setType("SYNC_FAILURE");
            it.setSeverity("P0");
            it.setTitle("Activity 执行失败：" + safe(c.getActivityId()));
            it.setDetail("Activity " + safe(c.getActivityRunId()) + " 由 "
                    + agentName(defMap, c.getLeadAgentId()) + " 执行失败，需重试或人工介入");
            it.setRefType("CONTRACT");
            it.setRefId(safe(c.getActivityRunId()));
            it.setAction("RETRY");
            SpkIpdFlowRunDO run = c.getProcessInstanceId() == null ? null
                    : flowRunService.getByProcessInstanceId(c.getProcessInstanceId());
            if (run != null) {
                it.setFlowRunId(run.getId());
                it.setProjectId(run.getProjectId());
            }
            items.add(it);
        }
        return items;
    }

    private List<InboxItem> buildBlockedFlows(Long projectId) {
        List<InboxItem> items = new ArrayList<>();
        List<SpkIpdFlowRunDO> runs;
        if (projectId != null) {
            // 取项目下所有版本，再取活跃/阻断
            runs = new ArrayList<>();
            for (SpkIpdMajorReleaseDO m : projectBusinessService.listMajorReleases(projectId)) {
                for (SpkIpdVersionDO v : projectBusinessService.listVersions(m.getId())) {
                    List<SpkIpdFlowRunDO> sub = flowRunMapper.selectActiveByVersion(v.getId());
                    if (sub != null) {
                        runs.addAll(sub);
                    }
                }
            }
        } else {
            runs = new ArrayList<>();
        }
        for (SpkIpdFlowRunDO run : runs) {
            if (!"BLOCKED".equalsIgnoreCase(run.getStatus()) && !"FAILED".equalsIgnoreCase(run.getStatus())) {
                continue;
            }
            InboxItem it = new InboxItem();
            it.setType("BLOCKED_FLOW");
            it.setSeverity("FAILED".equalsIgnoreCase(run.getStatus()) ? "P0" : "P1");
            it.setTitle("流程" + ("FAILED".equalsIgnoreCase(run.getStatus()) ? "失败" : "阻断")
                    + "：" + safe(run.getRunNo()));
            it.setDetail(safe(run.getBlockReason()));
            it.setRefType("FLOW_RUN");
            it.setRefId(String.valueOf(run.getId()));
            it.setFlowRunId(run.getId());
            it.setProjectId(run.getProjectId());
            it.setAction("UNBLOCK");
            items.add(it);
        }
        return items;
    }

    // ===== 阶段看板 =====

    private List<BoardColumn> buildBoard(Long projectId, Long versionId) {
        // 收集 FlowRun
        List<SpkIpdFlowRunDO> runs = new ArrayList<>();
        if (versionId != null) {
            List<SpkIpdFlowRunDO> sub = flowRunMapper.selectActiveByVersion(versionId);
            if (sub != null) {
                runs.addAll(sub);
            }
        } else if (projectId != null) {
            for (SpkIpdMajorReleaseDO m : projectBusinessService.listMajorReleases(projectId)) {
                for (SpkIpdVersionDO v : projectBusinessService.listVersions(m.getId())) {
                    List<SpkIpdFlowRunDO> sub = flowRunMapper.selectActiveByVersion(v.getId());
                    if (sub != null) {
                        runs.addAll(sub);
                    }
                }
            }
        }
        if (runs.isEmpty()) {
            return Collections.emptyList();
        }
        // 取这些 FlowRun 的活跃 Task Contract
        Set<String> instIds = runs.stream()
                .map(SpkIpdFlowRunDO::getProcessInstanceId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, SpkIpdFlowRunDO> pid2run = runs.stream()
                .filter(r -> r.getProcessInstanceId() != null)
                .collect(Collectors.toMap(SpkIpdFlowRunDO::getProcessInstanceId, r -> r, (a, b) -> a));
        Map<String, SpkAgentDefDO> defCache = new HashMap<>();
        // 按 phase 分组
        Map<String, List<BoardItem>> phaseItems = new LinkedHashMap<>();
        Set<Long> agentIds = new java.util.HashSet<>();
        for (String pid : instIds) {
            List<SpkTaskContractDO> contracts = taskContractMapper.selectListByProcessInstanceId(pid);
            if (contracts == null) {
                continue;
            }
            for (SpkTaskContractDO c : contracts) {
                if (SpkTaskContractStatusEnum.DONE.getLabel().equalsIgnoreCase(c.getStatus())) {
                    continue; // 已完成不占看板
                }
                BoardItem bi = toBoardItem(c, pid2run.get(pid));
                phaseItems.computeIfAbsent(phaseName(safe(c.getPhase())), k -> new ArrayList<>()).add(bi);
                if (c.getLeadAgentId() != null) {
                    agentIds.add(c.getLeadAgentId());
                }
            }
        }
        // 填充 owner 名
        Map<Long, SpkAgentDefDO> defMap = agentIds.isEmpty() ? Collections.emptyMap()
                : lookupAgentDefsByIds(agentIds);
        for (List<BoardItem> list : phaseItems.values()) {
            for (BoardItem bi : list) {
                // ownerName 已在 toBoardItem 设为 leadAgentCode；若有 id 映射则补名
            }
        }
        List<BoardColumn> cols = new ArrayList<>();
        for (Map.Entry<String, List<BoardItem>> e : phaseItems.entrySet()) {
            BoardColumn col = new BoardColumn();
            col.setStage(e.getKey());
            col.setItems(e.getValue());
            cols.add(col);
        }
        return cols;
    }

    // ==================== v4.0 §4.4 三视图：任务 / 指挥 / 团队 ====================

    private List<TaskRow> buildTasks(Long projectId) {
        List<SpkIpdFlowRunDO> runs = collectFlowRuns(projectId, null);
        if (runs.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, SpkIpdFlowRunDO> pid2run = runs.stream()
                .filter(r -> r.getProcessInstanceId() != null)
                .collect(Collectors.toMap(SpkIpdFlowRunDO::getProcessInstanceId, r -> r, (a, b) -> a));
        Set<Long> projIds = runs.stream().map(SpkIpdFlowRunDO::getProjectId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> projName = lookupProjectNames(projIds);
        Set<Long> agentIds = new java.util.HashSet<>();
        for (String pid : pid2run.keySet()) {
            List<SpkTaskContractDO> cs = taskContractMapper.selectListByProcessInstanceId(pid);
            if (cs != null) {
                for (SpkTaskContractDO c : cs) {
                    if (c.getLeadAgentId() != null) {
                        agentIds.add(c.getLeadAgentId());
                    }
                }
            }
        }
        Map<Long, SpkAgentDefDO> defMap = lookupAgentDefsByIds(agentIds);
        List<TaskRow> rows = new ArrayList<>();
        for (Map.Entry<String, SpkIpdFlowRunDO> en : pid2run.entrySet()) {
            String pid = en.getKey();
            SpkIpdFlowRunDO run = en.getValue();
            String resolvedStage = safe(stageResolver.resolveCurrentStage(
                    pid, run.getCurrentStage(), run.getStatus()));
            List<SpkTaskContractDO> contracts = taskContractMapper.selectListByProcessInstanceId(pid);
            if (contracts == null || contracts.isEmpty()) {
                continue;
            }
            for (SpkTaskContractDO c : contracts) {
                rows.add(toTaskRow(c, run, resolvedStage, projName, defMap));
            }
        }
        rows.sort(Comparator.comparing(
                (TaskRow r) -> r.getStatus() == null ? "" : r.getStatus()).reversed());
        return rows;
    }

    private TaskRow toTaskRow(SpkTaskContractDO c, SpkIpdFlowRunDO run, String stage,
                               Map<Long, String> projName, Map<Long, SpkAgentDefDO> defMap) {
        TaskRow tr = new TaskRow();
        tr.setActivityRunId(safe(c.getActivityRunId()));
        tr.setName(safe(c.getActivityId()));
        tr.setProjectId(run.getProjectId());
        tr.setProjectName(projName == null ? "" : projName.getOrDefault(run.getProjectId(), ""));
        tr.setFlowRunId(run.getId());
        tr.setFlowRunNo(safe(run.getRunNo()));
        tr.setStage(stage);
        tr.setOwnerType("AGENT");
        tr.setOwnerName(agentName(defMap, c.getLeadAgentId()));
        tr.setStatus(c.getStatus());
        tr.setProgress(progressOf(c));
        tr.setDurationSec(durationSecOf(c));
        if (run.getPlannedEndAt() != null) {
            tr.setDueAt(run.getPlannedEndAt().format(FMT));
        }
        // Omnigent 会话 ID：task_contract.task_id 即 Omnigent session id（同 cockpit ActivityDetail 取值源，真实落库）
        tr.setSessionId(safe(c.getTaskId()));
        return tr;
    }

    /** 进度：终态 100，运行中 50，已入队 0（按状态真实推算，非凭空写死数值） */
    private Integer progressOf(SpkTaskContractDO c) {
        SpkTaskContractStatusEnum st = SpkTaskContractStatusEnum.of(c.getStatus());
        if (st == null) {
            return 0;
        }
        if (st.isTerminal()) {
            return 100;
        }
        if (st == SpkTaskContractStatusEnum.RUNNING) {
            return 50;
        }
        return 0;
    }

    /** 已运行时长（秒）：finishedAt-startedAt 或 startedAt-now，无则 null */
    private Long durationSecOf(SpkTaskContractDO c) {
        LocalDateTime start = c.getStartedAt();
        if (start == null) {
            return null;
        }
        LocalDateTime end = c.getFinishedAt() != null ? c.getFinishedAt() : LocalDateTime.now();
        return Duration.between(start, end).getSeconds();
    }

    private Map<String, Long> buildTaskStats(List<TaskRow> tasks) {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("queued", 0L);
        stats.put("running", 0L);
        stats.put("failed", 0L);
        stats.put("done", 0L);
        stats.put("timeout", 0L);
        stats.put("cancelled", 0L);
        if (tasks == null) {
            return stats;
        }
        for (TaskRow t : tasks) {
            String s = t.getStatus() == null ? "" : t.getStatus().toLowerCase();
            if (stats.containsKey(s)) {
                stats.put(s, stats.get(s) + 1);
            }
        }
        return stats;
    }

    private Map<String, Object> buildKpis(Long projectId, List<TaskRow> tasks) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        long running = 0, queued = 0, done = 0, failed = 0;
        long totalDur = 0;
        long durCnt = 0;
        long todayDone = 0;
        LocalDate today = LocalDate.now();
        if (tasks != null) {
            for (TaskRow t : tasks) {
                String s = t.getStatus() == null ? "" : t.getStatus().toLowerCase();
                if ("running".equals(s)) {
                    running++;
                } else if ("queued".equals(s)) {
                    queued++;
                } else if ("done".equals(s)) {
                    done++;
                } else if ("failed".equals(s) || "timeout".equals(s)) {
                    failed++;
                }
                if (t.getDurationSec() != null && "done".equals(s)) {
                    totalDur += t.getDurationSec();
                    durCnt++;
                }
            }
        }
        // 今日完成：按 run_receipt.finished_at 今日（真实收据时间）
        try {
            List<SpkIpdFlowRunDO> runs = collectFlowRuns(projectId, null);
            Set<String> pids = runs.stream().map(SpkIpdFlowRunDO::getProcessInstanceId)
                    .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
            for (String pid : pids) {
                List<SpkRunReceiptDO> rs = runReceiptMapper.selectListByProcessInstanceId(pid);
                if (rs == null) {
                    continue;
                }
                for (SpkRunReceiptDO r : rs) {
                    if (r.getFinishedAt() != null && r.getFinishedAt().toLocalDate().isEqual(today)) {
                        todayDone++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[buildKpis][今日完成统计失败 err={}]", e.getMessage());
        }
        long total = done + failed;
        Double failureRate = total == 0 ? null
                : Math.round(failed * 10000.0 / total) / 100.0;
        Long avgDur = durCnt == 0 ? null : totalDur / durCnt;
        kpis.put("running", running);
        kpis.put("queued", queued);
        kpis.put("todayDone", todayDone);
        kpis.put("failureRate", failureRate);
        kpis.put("avgDurationSec", avgDur);
        return kpis;
    }

    private Map<String, Object> buildGlobalParams() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("adapter", cfgAdapter);
        p.put("fastMode", cfgFastMode);
        p.put("defaultMode", cfgDefaultMode);
        p.put("omnigentTimeoutMs", cfgOmnigentTimeoutMs);
        p.put("pollIntervalMs", cfgOmnigentPollMs);
        return p;
    }

    /** 事件流：跨 FlowRun 最近 task_contract 状态变更（按 finishedAt/startedAt/updateTime 降序，真实数据） */
    private List<Map<String, Object>> buildEventStream(Long projectId) {
        List<SpkIpdFlowRunDO> runs = collectFlowRuns(projectId, null);
        if (runs.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, SpkIpdFlowRunDO> pid2run = runs.stream()
                .filter(r -> r.getProcessInstanceId() != null)
                .collect(Collectors.toMap(SpkIpdFlowRunDO::getProcessInstanceId, r -> r, (a, b) -> a));
        List<Map<String, Object>> events = new ArrayList<>();
        for (Map.Entry<String, SpkIpdFlowRunDO> en : pid2run.entrySet()) {
            SpkIpdFlowRunDO run = en.getValue();
            List<SpkTaskContractDO> cs = taskContractMapper.selectListByProcessInstanceId(en.getKey());
            if (cs == null) {
                continue;
            }
            for (SpkTaskContractDO c : cs) {
                LocalDateTime ts = c.getFinishedAt() != null ? c.getFinishedAt()
                        : (c.getStartedAt() != null ? c.getStartedAt() : c.getQueuedAt());
                if (ts == null) {
                    continue;
                }
                Map<String, Object> ev = new LinkedHashMap<>();
                ev.put("at", ts.format(FMT));
                ev.put("flowRunNo", safe(run.getRunNo()));
                ev.put("activity", safe(c.getActivityId()));
                ev.put("status", c.getStatus());
                ev.put("agent", safe(c.getLeadAgentCode()));
                ev.put("sortTs", ts);
                events.add(ev);
            }
        }
        events.sort(Comparator.comparing(m -> (LocalDateTime) m.get("sortTs"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> ev : events) {
            ev.remove("sortTs");
            out.add(ev);
            if (out.size() >= 20) {
                break;
            }
        }
        return out;
    }

    /** 复用 buildBoard 的 FlowRun 收集逻辑（项目/全局） */
    private List<SpkIpdFlowRunDO> collectFlowRuns(Long projectId, Long versionId) {
        List<SpkIpdFlowRunDO> runs = new ArrayList<>();
        if (versionId != null) {
            List<SpkIpdFlowRunDO> sub = flowRunMapper.selectActiveByVersion(versionId);
            if (sub != null) {
                runs.addAll(sub);
            }
        } else if (projectId != null) {
            for (SpkIpdMajorReleaseDO m : projectBusinessService.listMajorReleases(projectId)) {
                for (SpkIpdVersionDO v : projectBusinessService.listVersions(m.getId())) {
                    List<SpkIpdFlowRunDO> sub = flowRunMapper.selectActiveByVersion(v.getId());
                    if (sub != null) {
                        runs.addAll(sub);
                    }
                }
            }
        } else {
            // 全局：取全部活跃 flow_run（status 非 COMPLETED/CANCELLED）
            try {
                runs = flowRunMapper.selectList(
                        new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<SpkIpdFlowRunDO>()
                                .notIn(SpkIpdFlowRunDO::getStatus,
                                        java.util.List.of("COMPLETED", "CANCELLED")));
            } catch (Exception e) {
                log.warn("[collectFlowRuns][全局 flow_run 查询失败 err={}]", e.getMessage());
            }
        }
        return runs;
    }

    private Map<Long, String> lookupProjectNames(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<SpkIpdProjectDO> ps = projectMapper.selectBatchIds(ids);
            if (ps == null) {
                return Collections.emptyMap();
            }
            return ps.stream().collect(Collectors.toMap(SpkIpdProjectDO::getId,
                    p -> safe(p.getName()), (a, b) -> a));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private BoardItem toBoardItem(SpkTaskContractDO c, SpkIpdFlowRunDO run) {
        BoardItem bi = new BoardItem();
        bi.setId(safe(c.getActivityRunId()));
        bi.setName(safe(c.getActivityId()));
        bi.setStatus(c.getStatus());
        bi.setOwnerType("AGENT");
        bi.setOwnerName(safe(c.getLeadAgentCode()));
        bi.setItemType("ACTIVITY");
        if (run != null) {
            bi.setFlowRunId(run.getId());
            bi.setProjectId(run.getProjectId());
            if (run.getPlannedEndAt() != null) {
                bi.setDueAt(run.getPlannedEndAt().format(FMT));
            }
        }
        return bi;
    }

    private String phaseName(String phase) {
        if (phase == null || phase.isBlank()) {
            return "未分阶段";
        }
        return phase;
    }

    // ===== 上下文 =====

    private Map<String, Object> buildContext(Long projectId, Long versionId) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        if (projectId != null) {
            try {
                SpkIpdProjectDO p = projectBusinessService.getProject(projectId);
                if (p != null) {
                    ctx.put("projectName", p.getName());
                    ctx.put("projectCode", p.getProjectCode());
                    ctx.put("projectStatus", p.getStatus());
                    ctx.put("projectHealth", p.getHealth());
                }
            } catch (Exception e) {
                ctx.put("projectError", e.getMessage());
            }
        }
        if (versionId != null) {
            try {
                SpkIpdVersionDO v = projectBusinessService.getVersion(versionId);
                if (v != null) {
                    ctx.put("versionNo", v.getVersionNo());
                    ctx.put("versionType", v.getVersionType());
                    ctx.put("baseline", v.getBaselineFlag());
                    ctx.put("scopeSummary", v.getScopeSummary());
                }
            } catch (Exception e) {
                ctx.put("versionError", e.getMessage());
            }
        }
        return ctx;
    }

    private String detectNextGate(Long projectId, Long versionId) {
        // 简化：取最新活跃 FlowRun 的 currentStage 作为下一门禁线索
        try {
            if (versionId == null && projectId == null) {
                return null;
            }
            List<SpkIpdFlowRunDO> runs = new ArrayList<>();
            if (versionId != null) {
                List<SpkIpdFlowRunDO> sub = flowRunMapper.selectActiveByVersion(versionId);
                if (sub != null) {
                    runs.addAll(sub);
                }
            } else {
                for (SpkIpdMajorReleaseDO m : projectBusinessService.listMajorReleases(projectId)) {
                    for (SpkIpdVersionDO v : projectBusinessService.listVersions(m.getId())) {
                        List<SpkIpdFlowRunDO> sub = flowRunMapper.selectActiveByVersion(v.getId());
                        if (sub != null) {
                            runs.addAll(sub);
                        }
                    }
                }
            }
            for (SpkIpdFlowRunDO r : runs) {
                if ("RUNNING".equalsIgnoreCase(r.getStatus()) && r.getCurrentStage() != null) {
                    return r.getCurrentStage();
                }
            }
        } catch (Exception e) {
            log.debug("[detectNextGate] {}", e.getMessage());
        }
        return null;
    }

    // ===== 自然语言命令：解析为只读预览 =====

    @Override
    public SpkIpdCommandVO.ParseResp parseCommand(SpkIpdCommandVO.ParseReq req) {
        SpkIpdCommandVO.ParseResp resp = new SpkIpdCommandVO.ParseResp();
        String text = req.getText() == null ? "" : req.getText().trim();
        String lower = text.toLowerCase();
        List<Map<String, Object>> targets = new ArrayList<>();
        List<Map<String, Object>> actions = new ArrayList<>();
        List<String> ambiguities = new ArrayList<>();

        String intent;
        if (containsAny(lower, "重试", "retry", "重新执行")) {
            intent = "RETRY";
            actions.add(step("对失败 Activity 重新派发", "INTERVENE", "rerun"));
        } else if (containsAny(lower, "介入", "人工", "intervene", "接手")) {
            intent = "INTERVENE";
            String action = containsAny(lower, "中止", "终止", "abort", "放弃") ? "abort" : "note";
            actions.add(step("人工介入", "INTERVENE", action));
        } else if (containsAny(lower, "阻断", "恢复", "unblock", "解除")) {
            intent = "UNBLOCK";
            actions.add(step("解除流程阻断", "UNBLOCK", "retry"));
        } else if (containsAny(lower, "发起审批", "审批", "approve", "go")) {
            intent = "APPROVE";
            actions.add(step("发起/完成审批", "APPROVAL", "approve"));
            ambiguities.add("审批须在决策包页面操作，此处仅提示");
        } else if (containsAny(lower, "发起ccb", "变更", "ccb")) {
            intent = "CCB";
            actions.add(step("发起 CCB 变更评审", "CCB", "create"));
            ambiguities.add("CCB 须指定影响范围与决策");
        } else if (containsAny(lower, "分派", "派给", "assign", "转派")) {
            intent = "ASSIGN";
            actions.add(step("任务转派", "ASSIGN", "reassign"));
            ambiguities.add("须指定目标用户 id");
        } else if (containsAny(lower, "启动", "agent", "唤醒", "跑")) {
            intent = "START_AGENT";
            actions.add(step("启动/唤醒 Agent 执行", "AGENT", "start"));
            ambiguities.add("须指定 Activity 或需求");
        } else {
            intent = "UNKNOWN";
            ambiguities.add("无法识别意图，请使用：重试/介入/解除阻断/审批/CCB/分派/启动");
        }

        // 简单对象抽取：版本号 / 项目名
        if (req.getProjectId() != null) {
            targets.add(target("PROJECT", "projectId", req.getProjectId()));
        }
        if (req.getVersionId() != null) {
            targets.add(target("VERSION", "versionId", req.getVersionId()));
        }
        java.util.regex.Matcher mv = java.util.regex.Pattern.compile("(\\d+)\\.(\\d+)").matcher(text);
        if (mv.find()) {
            targets.add(target("VERSION", "versionNo", mv.group()));
        }

        resp.setIntent(intent);
        resp.setTargets(targets);
        resp.setActions(actions);
        resp.setAmbiguities(ambiguities);
        resp.setImpact("仅解析预览，执行需用户确认幂等键");
        resp.setIdempotencyKey(generateKey(intent, req));
        resp.setExecutable(!"UNKNOWN".equals(intent) && !actions.isEmpty());
        return resp;
    }

    private boolean containsAny(String s, String... keys) {
        for (String k : keys) {
            if (s.contains(k.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> step(String desc, String type, String action) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("description", desc);
        m.put("type", type);
        m.put("action", action);
        return m;
    }

    private Map<String, Object> target(String type, String key, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put(key, value);
        return m;
    }

    private String generateKey(String intent, SpkIpdCommandVO.ParseReq req) {
        Long uid = currentUserId();
        return "wb-" + (uid == null ? 0 : uid) + "-" + intent + "-"
                + Integer.toHexString((intent + ":" + (req == null ? "" : req.getText())).hashCode());
    }

    // ===== 命令执行：白名单 + 幂等 =====

    @Override
    public SpkIpdCommandVO.ExecuteResp executeCommand(Long userId, SpkIpdCommandVO.ExecuteReq req) {
        SpkIpdCommandVO.ExecuteResp resp = new SpkIpdCommandVO.ExecuteResp();
        resp.setIntent(req.getIntent());
        Map<String, Object> params = req.getParams() == null ? Collections.emptyMap() : req.getParams();
        String targetId = str(params.get("activityRunId"));
        String targetType = str(params.getOrDefault("targetType", "CONTRACT"));
        try {
            // 幂等登记
            CommandEnvelope env = commandService.enlist(req.getIdempotencyKey(),
                    req.getIntent(), targetType, targetId, cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(params));
            commandService.markRunning(env.commandId());
            if (!env.isNew()) {
                resp.setStatus("IDEMPOTENT");
                resp.setMessage("命令已处理过，幂等返回");
                resp.setResult(Collections.singletonMap("commandId", env.commandId()));
                return resp;
            }
            // 白名单分发
            switch (req.getIntent()) {
                case "RETRY":
                case "INTERVENE": {
                    String action = str(params.getOrDefault("action", "rerun"));
                    if (targetId == null || targetId.isBlank()) {
                        throw new IllegalArgumentException("缺少 activityRunId");
                    }
                    agentTaskService.intervene(targetId, action,
                            str(params.get("note")));
                    resp.setStatus("SUCCESS");
                    resp.setMessage("已对 Activity " + targetId + " 执行 " + action);
                    break;
                }
                case "UNBLOCK": {
                    // 阻断恢复复用 intervene rerun
                    if (targetId == null || targetId.isBlank()) {
                        throw new IllegalArgumentException("缺少 activityRunId");
                    }
                    agentTaskService.intervene(targetId, "rerun", "workbench-unblock");
                    resp.setStatus("SUCCESS");
                    resp.setMessage("已尝试解除阻断：" + targetId);
                    break;
                }
                default:
                    resp.setStatus("NOT_SUPPORTED");
                    resp.setMessage("意图 " + req.getIntent() + " 暂不支持在此执行，请到对应页面操作");
            }
            commandService.markSuccess(env.commandId(),
                    cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(Collections.singletonMap("status", resp.getStatus())));
        } catch (Exception e) {
            log.warn("[executeCommand] intent={} err={}", req.getIntent(), e.getMessage());
            resp.setStatus("REJECTED");
            resp.setMessage(e.getMessage());
            markFailedSafe(req, "EXEC_FAIL", e.getMessage());
        }
        return resp;
    }

    /** 失败兜底：命令登记可能尚未建立，独立再登记一条失败记录 */
    private void markFailedSafe(SpkIpdCommandVO.ExecuteReq req, String code, String msg) {
        try {
            commandService.markFailed(commandService.enlist(req.getIdempotencyKey(),
                    req.getIntent(), "CONTRACT", str(req.getParams() == null
                            ? null : req.getParams().get("activityRunId")),
                    cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(req.getParams())).commandId(), code, msg);
        } catch (Exception ignore) {
            // 兜底，忽略
        }
    }

    // ===== 小工具 =====

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Long currentUserId() {
        try {
            return cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<Long, SpkAgentDefDO> lookupAgentDefs(List<SpkTaskContractDO> contracts) {
        Set<Long> ids = contracts.stream()
                .map(SpkTaskContractDO::getLeadAgentId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        return lookupAgentDefsByIds(ids);
    }

    private Map<Long, SpkAgentDefDO> lookupAgentDefsByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<SpkAgentDefDO> defs = agentDefMapper.selectBatchIds(ids);
            if (defs == null) {
                return Collections.emptyMap();
            }
            return defs.stream().collect(Collectors.toMap(SpkAgentDefDO::getId, d -> d, (a, b) -> a));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String agentName(Map<Long, SpkAgentDefDO> defMap, Long agentId) {
        if (agentId == null) {
            return "未分派";
        }
        SpkAgentDefDO d = defMap == null ? null : defMap.get(agentId);
        return d == null ? ("Agent#" + agentId) : safe(d.getName());
    }
}
