package cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.gate.SpkGateRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdactivity.SpkIpdActivityDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdDecisionRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdMajorReleaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectActorDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdProjectDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdWorkItemLinkDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.gate.SpkGateRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdactivity.SpkIpdActivityDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdDecisionRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFlowRunMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdMajorReleaseMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdProjectActorMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdProjectMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdVersionMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdWorkItemLinkMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkActivityStageEnum;
import cn.iocoder.yudao.module.spkdelivery.service.cockpit.SpkIpdCockpitService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SPK-OS Cortext-IPD 项目空间只读聚合 Service（设计文档 §9.3 / 诉求 §1 项目管理）。
 * <p>
 * 项目卡片网格 + 项目详情一次性聚合（活跃版本/FlowRun/阶段时间戳/活动泳道/需求树/最近活动/问题计数）。
 * 纯只读，跨表真实聚合；库内无数据的字段返回 null 或空集，前端标注"样本不足"，绝不灌假（铁律 §4.4/§C-14）。
 * 复用 {@link SpkIpdCockpitService#swimlane} 做活动泳道，复用 task_contract 做阶段时间戳与最近活动。
 *
 * @author SPK-OS
 */
@Service
public class SpkIpdProjectWorkspaceService {

    @Resource
    private SpkIpdProjectMapper projectMapper;
    @Resource
    private SpkIpdMajorReleaseMapper majorReleaseMapper;
    @Resource
    private SpkIpdVersionMapper versionMapper;
    @Resource
    private SpkIpdFlowRunMapper flowRunMapper;
    @Resource
    private SpkIpdProjectActorMapper projectActorMapper;
    @Resource
    private SpkIpdWorkItemLinkMapper workItemLinkMapper;
    @Resource
    private SpkIpdDecisionRecordMapper decisionRecordMapper;
    @Resource
    private SpkGateRecordMapper gateRecordMapper;
    @Resource
    private SpkTaskContractMapper taskContractMapper;
    @Resource
    private SpkArtifactManifestMapper artifactMapper;
    @Resource
    private SpkIpdActivityDefMapper activityDefMapper;
    @Resource
    private SpkAgentDefMapper agentDefMapper;
    @Resource
    private SpkIpdCockpitService cockpitService;
    @Resource
    private SpkStageResolver stageResolver;

    private static final List<SpkActivityStageEnum> PIPELINE = List.of(
            SpkActivityStageEnum.CONCEPT, SpkActivityStageEnum.PLAN, SpkActivityStageEnum.DEVELOP,
            SpkActivityStageEnum.QUALIFY, SpkActivityStageEnum.LAUNCH, SpkActivityStageEnum.LIFECYCLE);

    /** epoch0 守卫区间下界（含）：1970-01-01 00:00。前端字符串日期反序列化回退 0 落此点。 */
    private static final LocalDateTime EPOCH_GUARD_START = LocalDateTime.of(1970, 1, 1, 0, 0);
    /** epoch0 守卫区间上界（不含）：2000-01-01。覆盖所有误传回退 0 的脏值，真实排期不会落此区间。 */
    private static final LocalDateTime EPOCH_GUARD_END = LocalDateTime.of(2000, 1, 1, 0, 0);

    /**
     * epoch0 守卫：plannedStartAt/EndAt 落 [1970-01-01, 2000-01-01) 视为未排期（字符串日期反序列化回退 0
     * 的脏值）输出 null，避免 dueIn 算出"逾期 20681 天"。真实排期保留原值。
     */
    private LocalDateTime guardEpoch(LocalDateTime t) {
        if (t == null) {
            return null;
        }
        if (!t.isBefore(EPOCH_GUARD_START) && t.isBefore(EPOCH_GUARD_END)) {
            return null;
        }
        return t;
    }

    // ==================== 1. 项目卡片网格 ====================

    /**
     * 非归档项目的卡片聚合：每项目 currentStage/activeVersion/dueIn/majorReleases/
     * blockedFlows/runningFlows/pendingDecisions/teamSize/freshness/issueCount。
     * 全部来自真实 DB 聚合，无记录则如实 0/null。
     */
    public List<Map<String, Object>> cards() {
        List<SpkIpdProjectDO> projects = projectMapper.selectList(new LambdaQueryWrapperX<SpkIpdProjectDO>()
                .ne(SpkIpdProjectDO::getStatus, "ARCHIVED")
                .orderByDesc(SpkIpdProjectDO::getUpdateTime));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SpkIpdProjectDO p : projects) {
            rows.add(buildCard(p));
        }
        return rows;
    }

    private Map<String, Object> buildCard(SpkIpdProjectDO p) {
        List<SpkIpdFlowRunDO> runs = flowRunMapper.selectList(new LambdaQueryWrapperX<SpkIpdFlowRunDO>()
                .eq(SpkIpdFlowRunDO::getProjectId, p.getId())
                .orderByDesc(SpkIpdFlowRunDO::getUpdateTime));
        // 活跃流优先 RUNNING/STARTING/BLOCKED，否则取最新
        SpkIpdFlowRunDO active = runs.stream()
                .filter(r -> "RUNNING".equals(r.getStatus()) || "STARTING".equals(r.getStatus())
                        || "BLOCKED".equals(r.getStatus()))
                .findFirst()
                .orElse(runs.isEmpty() ? null : runs.get(0));
        long blocked = runs.stream().filter(r -> "BLOCKED".equals(r.getStatus())).count();
        long running = runs.stream().filter(r -> "RUNNING".equals(r.getStatus())
                || "STARTING".equals(r.getStatus())).count();

        // 活跃版本：优先活跃流绑定的版本，否则 currentMajorRelease 的 baseline
        SpkIpdVersionDO activeVer = active != null && active.getVersionId() != null
                ? versionMapper.selectById(active.getVersionId()) : resolveBaselineVersion(p);
        long majorCount = majorReleaseMapper.selectListByProject(p.getId()).size();

        // 待决策：项目下 decision_record 中 decision 为空（未裁决）的记录数
        long pending = decisionRecordMapper.selectList(new LambdaQueryWrapperX<SpkIpdDecisionRecordDO>()
                .eq(SpkIpdDecisionRecordDO::getProjectId, p.getId())
                .isNull(SpkIpdDecisionRecordDO::getDecision)).size();

        // 项目级参与者（versionId is null）
        long team = projectActorMapper.selectList(new LambdaQueryWrapperX<SpkIpdProjectActorDO>()
                .eq(SpkIpdProjectActorDO::getProjectId, p.getId())
                .isNull(SpkIpdProjectActorDO::getVersionId)).size();

        // 问题计数：work_item_link 中 DEFECT 类型
        long issues = workItemLinkMapper.selectListByProject(p.getId()).stream()
                .filter(l -> "DEFECT".equalsIgnoreCase(l.getLinkType())).count();

        // 新鲜度：项目与最新 flow_run updateTime 的较大者
        LocalDateTime freshness = p.getUpdateTime();
        for (SpkIpdFlowRunDO r : runs) {
            if (r.getUpdateTime() != null && (freshness == null || r.getUpdateTime().isAfter(freshness))) {
                freshness = r.getUpdateTime();
            }
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("projectNo", p.getProjectNo());
        m.put("name", p.getName());
        m.put("status", p.getStatus());
        m.put("health", p.getHealth());
        m.put("ownerUserId", p.getOwnerUserId());
        // Bug1-C：epoch0 守卫——前端 date-picker 历史传字符串 ISO 日期，后端 TimestampLocalDateTimeDeserializer
        // 解析失败回退 0 → DB 落 1970-01-01 08:00:00 → dueIn 算出"逾期 20681 天"。该区间视为未排期输出 null。
        LocalDateTime pStart = guardEpoch(p.getPlannedStartAt());
        LocalDateTime pEnd = guardEpoch(p.getPlannedEndAt());
        m.put("plannedStartAt", pStart);
        m.put("plannedEndAt", pEnd);
        m.put("dueIn", pEnd == null ? null
                : ChronoUnit.DAYS.between(LocalDateTime.now(), pEnd));
        // Bug2-A：currentStage/currentActivity 实时算（DO 静态值永停 concept，读端覆盖）
        String cStage = active != null
                ? stageResolver.resolveCurrentStage(active.getProcessInstanceId(), active.getCurrentStage(), active.getStatus())
                : null;
        m.put("currentStage", cStage);
        m.put("currentStageLabel", stageLabel(cStage));
        m.put("currentActivity", active != null
                ? stageResolver.resolveCurrentActivity(active.getProcessInstanceId(), active.getCurrentActivity())
                : null);
        m.put("activeVersion", activeVer != null ? activeVer.getVersionNo() : null);
        m.put("activeVersionId", activeVer != null ? activeVer.getId() : null);
        m.put("activeFlowRunId", active != null ? active.getId() : null);
        m.put("activeFlowRunNo", active != null ? active.getRunNo() : null);
        m.put("majorReleases", majorCount);
        m.put("blockedFlows", blocked);
        m.put("runningFlows", running);
        m.put("pendingDecisions", pending);
        m.put("teamSize", team);
        m.put("issueCount", issues);
        m.put("freshness", freshness);
        return m;
    }

    // ==================== 2. 项目详情一次性聚合 ====================

    /**
     * 项目空间详情头部 + 各子结构一次性返回：活跃版本/活跃 FlowRun/阶段时间戳/活动泳道/
     * 最近活动/需求树根/问题计数。子列表限长，避免无界返回。
     */
    public Map<String, Object> workspace(Long projectId) {
        SpkIpdProjectDO p = projectMapper.selectById(projectId);
        if (p == null) {
            throw new IllegalArgumentException("项目不存在: " + projectId);
        }
        List<SpkIpdFlowRunDO> runs = flowRunMapper.selectList(new LambdaQueryWrapperX<SpkIpdFlowRunDO>()
                .eq(SpkIpdFlowRunDO::getProjectId, projectId)
                .orderByDesc(SpkIpdFlowRunDO::getUpdateTime));
        SpkIpdFlowRunDO active = runs.stream()
                .filter(r -> "RUNNING".equals(r.getStatus()) || "STARTING".equals(r.getStatus())
                        || "BLOCKED".equals(r.getStatus()))
                .findFirst()
                .orElse(runs.isEmpty() ? null : runs.get(0));
        SpkIpdVersionDO activeVer = active != null && active.getVersionId() != null
                ? versionMapper.selectById(active.getVersionId()) : resolveBaselineVersion(p);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("project", projectSummary(p));
        m.put("activeVersion", versionSummary(activeVer));
        m.put("activeFlowRun", flowRunSummary(active));
        m.put("stageTimestamps", stageTimestamps(projectId,
                activeVer != null ? activeVer.getId() : null));
        // 活动泳道：复用 cockpit.swimlane，无活跃流则返回空泳道
        if (active != null && active.getProcessInstanceId() != null) {
            m.put("swimlane", cockpitService.swimlane(active.getProcessInstanceId()));
        } else {
            m.put("swimlane", emptySwimlane());
        }
        m.put("recentActivities", recentActivities(projectId, 10));
        m.put("requirementRoots", requirementTree(projectId,
                activeVer != null ? activeVer.getId() : null));
        m.put("issueCount", workItemLinkMapper.selectListByProject(projectId).stream()
                .filter(l -> "DEFECT".equalsIgnoreCase(l.getLinkType())).count());
        return m;
    }

    private Map<String, Object> projectSummary(SpkIpdProjectDO p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("projectNo", p.getProjectNo());
        m.put("projectCode", p.getProjectCode());
        m.put("name", p.getName());
        m.put("description", p.getDescription());
        m.put("objective", p.getObjective());
        m.put("ownerUserId", p.getOwnerUserId());
        m.put("status", p.getStatus());
        m.put("health", p.getHealth());
        m.put("plannedStartAt", p.getPlannedStartAt());
        m.put("plannedEndAt", p.getPlannedEndAt());
        m.put("actualStartAt", p.getActualStartAt());
        m.put("actualEndAt", p.getActualEndAt());
        m.put("currentMajorReleaseId", p.getCurrentMajorReleaseId());
        return m;
    }

    private Map<String, Object> versionSummary(SpkIpdVersionDO v) {
        if (v == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("versionNo", v.getVersionNo());
        m.put("versionType", v.getVersionType());
        m.put("baselineFlag", v.getBaselineFlag());
        m.put("name", v.getName());
        m.put("objective", v.getObjective());
        m.put("status", v.getStatus());
        m.put("deliveryReadiness", v.getDeliveryReadiness());
        m.put("health", v.getHealth());
        m.put("majorReleaseId", v.getMajorReleaseId());
        m.put("plannedEndAt", v.getPlannedEndAt());
        return m;
    }

    private Map<String, Object> flowRunSummary(SpkIpdFlowRunDO r) {
        if (r == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("runNo", r.getRunNo());
        m.put("flowType", r.getFlowType());
        m.put("status", r.getStatus());
        // Bug2-A：currentStage/currentActivity 实时算覆盖 DO 静态值（永停 concept）
        String cs = stageResolver.resolveCurrentStage(r.getProcessInstanceId(), r.getCurrentStage(), r.getStatus());
        m.put("currentStage", cs);
        m.put("currentStageLabel", stageLabel(cs));
        m.put("currentActivity", stageResolver.resolveCurrentActivity(r.getProcessInstanceId(), r.getCurrentActivity()));
        m.put("health", r.getHealth());
        m.put("processInstanceId", r.getProcessInstanceId());
        m.put("versionId", r.getVersionId());
        m.put("startedAt", r.getStartedAt());
        m.put("endedAt", r.getEndedAt());
        m.put("blockReason", r.getBlockReason());
        return m;
    }

    // ==================== 3. 阶段时间戳（6 阶段） ====================

    /**
     * 按 (projectId, versionId) 返回 6 阶段进入时间与状态。聚合该版本活跃 FlowRun 的
     * task_contract.phase + queuedAt/finishedAt。无活跃流则 6 阶段均为 not_started。
     */
    public List<Map<String, Object>> stageTimestamps(Long projectId, Long versionId) {
        List<SpkIpdFlowRunDO> runs;
        if (versionId != null) {
            runs = flowRunMapper.selectList(new LambdaQueryWrapperX<SpkIpdFlowRunDO>()
                    .eq(SpkIpdFlowRunDO::getVersionId, versionId)
                    .orderByDesc(SpkIpdFlowRunDO::getStartedAt));
        } else {
            runs = flowRunMapper.selectList(new LambdaQueryWrapperX<SpkIpdFlowRunDO>()
                    .eq(SpkIpdFlowRunDO::getProjectId, projectId)
                    .orderByDesc(SpkIpdFlowRunDO::getStartedAt));
        }
        String pid = runs.stream()
                .filter(r -> r.getProcessInstanceId() != null
                        && ("RUNNING".equals(r.getStatus()) || "STARTING".equals(r.getStatus())
                        || "BLOCKED".equals(r.getStatus())))
                .map(SpkIpdFlowRunDO::getProcessInstanceId).findFirst()
                .orElse(runs.stream().map(SpkIpdFlowRunDO::getProcessInstanceId)
                        .filter(Objects::nonNull).findFirst().orElse(null));

        Map<String, List<Map<String, Object>>> byStage = new LinkedHashMap<>();
        if (pid != null) {
            Object swimObj = cockpitService.swimlane(pid);
            if (swimObj instanceof Map) {
                Object stages = ((Map<String, Object>) swimObj).get("stages");
                if (stages instanceof Map) {
                    byStage = (Map<String, List<Map<String, Object>>>) stages;
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (SpkActivityStageEnum e : PIPELINE) {
            List<Map<String, Object>> cards = matchStageCards(byStage, e);
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("stage", e.getCode());
            s.put("label", e.getLabel());
            if (cards.isEmpty()) {
                s.put("status", "not_started");
                s.put("enteredAt", null);
                s.put("finishedAt", null);
                s.put("activityCount", 0);
            } else {
                s.put("activityCount", cards.size());
                LocalDateTime entered = cards.stream()
                        .map(c -> (LocalDateTime) c.get("queuedAt"))
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo).orElse(null);
                s.put("enteredAt", entered);
                s.put("finishedAt", cards.stream()
                        .map(c -> (LocalDateTime) c.get("finishedAt"))
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo).orElse(null));
                s.put("status", deriveStageStatus(cards));
            }
            result.add(s);
        }
        return result;
    }

    /** 从泳道 stages 中忽略大小写匹配某阶段的活动卡片。 */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> matchStageCards(Map<String, List<Map<String, Object>>> byStage,
                                                       SpkActivityStageEnum e) {
        if (byStage == null) {
            return Collections.emptyList();
        }
        for (Map.Entry<String, List<Map<String, Object>>> en : byStage.entrySet()) {
            if (e.getCode().equalsIgnoreCase(en.getKey())) {
                return en.getValue() == null ? Collections.emptyList() : en.getValue();
            }
        }
        return Collections.emptyList();
    }

    private String deriveStageStatus(List<Map<String, Object>> cards) {
        boolean hasRunning = false, hasFailed = false, allDone = true;
        for (Map<String, Object> c : cards) {
            Object st = c.get("status");
            if (!(st instanceof String)) {
                continue;
            }
            String s = (String) st;
            if ("running".equalsIgnoreCase(s)) {
                hasRunning = true;
                allDone = false;
            } else if ("failed".equalsIgnoreCase(s) || "timeout".equalsIgnoreCase(s)) {
                hasFailed = true;
                allDone = false;
            } else if (!"done".equalsIgnoreCase(s)) {
                allDone = false;
            }
        }
        if (hasFailed) {
            return "blocked";
        }
        if (hasRunning) {
            return "running";
        }
        return allDone ? "done" : "in_progress";
    }

    // ==================== 4. 最近活动（跨 FlowRun） ====================

    /**
     * 项目维度跨 FlowRun/Activity 时间线：task_contract 按 queuedAt 倒序。
     * 每条含 activityName(中文名)、stage、status、leadAgentCode、产物数、flowRunNo。
     */
    public List<Map<String, Object>> recentActivities(Long projectId, int limit) {
        List<SpkIpdFlowRunDO> runs = flowRunMapper.selectList(new LambdaQueryWrapperX<SpkIpdFlowRunDO>()
                .eq(SpkIpdFlowRunDO::getProjectId, projectId));
        Map<String, String> pidToRunNo = new HashMap<>();
        List<String> pids = new ArrayList<>();
        for (SpkIpdFlowRunDO r : runs) {
            if (r.getProcessInstanceId() != null) {
                pids.add(r.getProcessInstanceId());
                pidToRunNo.put(r.getProcessInstanceId(), r.getRunNo());
            }
        }
        if (pids.isEmpty()) {
            return Collections.emptyList();
        }
        List<SpkTaskContractDO> contracts = taskContractMapper.selectList(
                new LambdaQueryWrapperX<SpkTaskContractDO>()
                        .in(SpkTaskContractDO::getProcessInstanceId, pids)
                        .orderByDesc(SpkTaskContractDO::getQueuedAt));
        if (contracts.isEmpty()) {
            return Collections.emptyList();
        }
        // activityId → 中文名批量
        Map<String, String> nameById = loadActivityNames(contracts);
        // leadAgentCode → 中文名批量（spk_agent_def.name）
        Map<String, String> nameByCode = loadAgentNames(contracts);
        // activityRunId → 产物数预聚合
        Map<String, Integer> artifactCount = new HashMap<>();
        List<SpkArtifactManifestDO> arts = artifactMapper.selectList(
                new LambdaQueryWrapperX<SpkArtifactManifestDO>()
                        .in(SpkArtifactManifestDO::getProcessInstanceId, pids));
        for (SpkArtifactManifestDO a : arts) {
            artifactCount.merge(a.getActivityRunId(), 1, Integer::sum);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        int n = Math.min(limit <= 0 ? 10 : limit, contracts.size());
        for (int i = 0; i < n; i++) {
            SpkTaskContractDO c = contracts.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("activityRunId", c.getActivityRunId());
            m.put("activityId", c.getActivityId());
            m.put("activityName", nameById.getOrDefault(c.getActivityId(), c.getActivityId()));
            m.put("stage", c.getPhase());
            m.put("stageLabel", stageLabel(c.getPhase()));
            m.put("nodeKey", c.getNodeKey());
            m.put("status", c.getStatus());
            m.put("leadAgentCode", c.getLeadAgentCode());
            m.put("leadAgentName", nameByCode.getOrDefault(c.getLeadAgentCode(), c.getLeadAgentCode()));
            m.put("queuedAt", c.getQueuedAt());
            m.put("finishedAt", c.getFinishedAt());
            m.put("flowRunNo", pidToRunNo.get(c.getProcessInstanceId()));
            m.put("artifactCount", artifactCount.getOrDefault(c.getActivityRunId(), 0));
            rows.add(m);
        }
        return rows;
    }

    private Map<String, String> loadActivityNames(List<SpkTaskContractDO> contracts) {
        Map<String, String> map = new HashMap<>();
        if (contracts == null || contracts.isEmpty()) {
            return map;
        }
        List<String> ids = new ArrayList<>();
        for (SpkTaskContractDO c : contracts) {
            if (c.getActivityId() != null && !ids.contains(c.getActivityId())) {
                ids.add(c.getActivityId());
            }
        }
        if (ids.isEmpty()) {
            return map;
        }
        for (SpkIpdActivityDefDO d : activityDefMapper.selectListByActivityIds(ids)) {
            map.putIfAbsent(d.getActivityId(), d.getName());
        }
        return map;
    }

    /**
     * 批量取 leadAgentCode → 中文名映射（spk_agent_def.name，展示名替代 lead-xxx 编号）。
     */
    private Map<String, String> loadAgentNames(List<SpkTaskContractDO> contracts) {
        Map<String, String> map = new HashMap<>();
        if (contracts == null || contracts.isEmpty()) {
            return map;
        }
        List<String> codes = new ArrayList<>();
        for (SpkTaskContractDO c : contracts) {
            if (c.getLeadAgentCode() != null && !codes.contains(c.getLeadAgentCode())) {
                codes.add(c.getLeadAgentCode());
            }
        }
        if (codes.isEmpty()) {
            return map;
        }
        for (SpkAgentDefDO d : agentDefMapper.selectListByCodes(codes)) {
            map.putIfAbsent(d.getCode(), d.getName());
        }
        return map;
    }

    // ==================== 5. 需求追踪树（IR/SR/AR） ====================

    /**
     * 按 (projectId, versionId) 过滤 IR/SR/AR 结构化树。
     * 数据源 work_item_link(linkType=REQUIREMENT) 的 lastSnapshotJson 快照。
     * 无数据返回 sparse=true，前端标注"样本不足"，不造假。
     */
    public Map<String, Object> requirementTree(Long projectId, Long versionId) {
        List<SpkIpdWorkItemLinkDO> links = workItemLinkMapper.selectListByProject(projectId);
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (SpkIpdWorkItemLinkDO l : links) {
            if (!"REQUIREMENT".equalsIgnoreCase(l.getLinkType())) {
                continue;
            }
            if (versionId != null && l.getVersionId() != null
                    && !versionId.equals(l.getVersionId())) {
                continue;
            }
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("linkId", l.getId());
            n.put("versionId", l.getVersionId());
            n.put("planeIssueSeq", l.getPlaneIssueSeq());
            n.put("planeIssueId", l.getPlaneIssueId());
            n.put("requirementType", parseSnapshotField(l.getLastSnapshotJson(), "type"));
            n.put("title", parseSnapshotField(l.getLastSnapshotJson(), "title"));
            n.put("status", parseSnapshotField(l.getLastSnapshotJson(), "status"));
            n.put("lastSyncedAt", l.getLastSyncedAt());
            nodes.add(n);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", projectId);
        result.put("versionId", versionId);
        result.put("total", nodes.size());
        result.put("sparse", nodes.isEmpty());
        result.put("nodes", nodes);
        return result;
    }

    /** 从 JSON 快照文本里取顶层字段值；非 JSON 或缺失返回 null。不引入强依赖。 */
    private String parseSnapshotField(String json, String field) {
        if (json == null || json.isBlank() || field == null) {
            return null;
        }
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) {
            return null;
        }
        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length()) {
            return null;
        }
        char ch = json.charAt(i);
        if (ch == '"') {
            int end = json.indexOf('"', i + 1);
            return end > 0 ? json.substring(i + 1, end) : null;
        }
        int end = i;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}'
                && json.charAt(end) != '\n') {
            end++;
        }
        return json.substring(i, end).trim();
    }

    // ==================== 工具 ====================

    private SpkIpdVersionDO resolveBaselineVersion(SpkIpdProjectDO p) {
        if (p.getCurrentMajorReleaseId() == null) {
            return null;
        }
        List<SpkIpdVersionDO> versions = versionMapper.selectListByMajorRelease(
                p.getCurrentMajorReleaseId());
        return versions.stream()
                .filter(v -> v.getBaselineFlag() != null && v.getBaselineFlag() == 1)
                .findFirst()
                .orElse(versions.isEmpty() ? null : versions.get(0));
    }

    private String stageLabel(String code) {
        SpkActivityStageEnum e = SpkActivityStageEnum.of(code);
        return e != null ? e.getLabel() : (code == null ? null : code);
    }

    private Map<String, Object> emptySwimlane() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("processInstanceId", null);
        m.put("stages", Collections.emptyMap());
        m.put("total", 0);
        return m;
    }
}
