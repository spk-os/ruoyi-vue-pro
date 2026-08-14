package cn.iocoder.yudao.module.spkdelivery.service.ipdworkbench;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdCommandVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdWorkbenchRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdWorkbenchRespVO.BoardColumn;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdWorkbenchRespVO.BoardItem;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdworkbench.vo.SpkIpdWorkbenchRespVO.InboxItem;
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
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentTaskStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkTaskContractStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.service.agent.SpkAgentTaskService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdCommandService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdCommandService.CommandEnvelope;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdFlowRunService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkIpdProjectBusinessService;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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

    @Override
    public SpkIpdWorkbenchRespVO getWorkbench(Long userId, Long projectId, Long versionId) {
        SpkIpdWorkbenchRespVO resp = new SpkIpdWorkbenchRespVO();
        resp.setRefreshedAt(LocalDateTime.now().format(FMT));
        resp.setContext(buildContext(projectId, versionId));
        resp.setNextGate(detectNextGate(projectId, versionId));
        resp.setInbox(buildInbox(userId, projectId));
        resp.setBoard(buildBoard(projectId, versionId));
        return resp;
    }

    @Override
    public SpkIpdWorkbenchRespVO getInbox(Long userId, Long projectId) {
        SpkIpdWorkbenchRespVO resp = new SpkIpdWorkbenchRespVO();
        resp.setRefreshedAt(LocalDateTime.now().format(FMT));
        resp.setInbox(buildInbox(userId, projectId));
        return resp;
    }

    @Override
    public SpkIpdWorkbenchRespVO getBoard(Long projectId) {
        SpkIpdWorkbenchRespVO resp = new SpkIpdWorkbenchRespVO();
        resp.setRefreshedAt(LocalDateTime.now().format(FMT));
        resp.setBoard(buildBoard(projectId, null));
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
