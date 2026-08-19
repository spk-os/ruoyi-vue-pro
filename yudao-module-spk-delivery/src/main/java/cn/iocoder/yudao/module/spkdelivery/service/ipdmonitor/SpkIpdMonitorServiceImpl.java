package cn.iocoder.yudao.module.spkdelivery.service.ipdmonitor;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdmonitor.vo.SpkIpdMonitorRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdmonitor.vo.SpkIpdMonitorRespVO.IntegrationHealth;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkEvidenceRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkRunReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.gate.SpkGateRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdDecisionRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdIssueCaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkEvidenceRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkRunReceiptMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.gate.SpkGateRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdDecisionRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFlowRunMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdIssueCaseMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdVersionMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkGiteaIntegrationService;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkOmnigentProxyService;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkPlaneIntegrationService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkStageResolver;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SPK-OS Cortext-IPD 项目维度监控 Service 实现（设计文档 §9.3 / 诉求 §4）。
 * <p>
 * 聚合：流程列表（每流程按 processInstanceId 查产物/证据计数）+ 汇总 + 集成健康。
 * 集成健康走各出站适配器的 healthCheck（可能因未配置而 false，不抛异常）。
 *
 * @author SPK-OS
 */
@Service
public class SpkIpdMonitorServiceImpl implements SpkIpdMonitorService {

    @Resource
    private SpkIpdFlowRunMapper flowRunMapper;
    @Resource
    private SpkIpdVersionMapper versionMapper;
    @Resource
    private SpkIpdIssueCaseMapper issueCaseMapper;
    @Resource
    private SpkArtifactManifestMapper artifactMapper;
    @Resource
    private SpkEvidenceRecordMapper evidenceMapper;
    @Resource
    private SpkIpdDecisionRecordMapper decisionMapper;
    @Resource
    private SpkGateRecordMapper gateMapper;
    @Resource
    private SpkPlaneIntegrationService planeService;
    @Resource
    private SpkGiteaIntegrationService giteaService;
    @Resource
    private SpkOmnigentProxyService omnigentService;
    @Resource
    private SpkStageResolver stageResolver;
    @Resource
    private SpkTaskContractMapper taskContractMapper;
    @Resource
    private SpkRunReceiptMapper runReceiptMapper;

    @Override
    public SpkIpdMonitorRespVO monitor(Long projectId) {
        SpkIpdMonitorRespVO resp = new SpkIpdMonitorRespVO();

        LambdaQueryWrapperX<SpkIpdFlowRunDO> fw = new LambdaQueryWrapperX<SpkIpdFlowRunDO>()
                .orderByDesc(SpkIpdFlowRunDO::getStartedAt).last("limit 100");
        if (projectId != null) {
            fw.eq(SpkIpdFlowRunDO::getProjectId, projectId);
        }
        List<SpkIpdFlowRunDO> flows = flowRunMapper.selectList(fw);

        List<Map<String, Object>> flowList = new ArrayList<>();
        long running = 0, blocked = 0, totalArtifacts = 0, totalEvidence = 0;
        for (SpkIpdFlowRunDO f : flows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("runNo", f.getRunNo());
            m.put("flowType", f.getFlowType());
            m.put("status", f.getStatus());
            // Bug2-A：currentStage/currentActivity 实时算（DO 静态值永停 concept，读端覆盖；与 buildCard 同源 SpkStageResolver）
            m.put("currentStage", stageResolver.resolveCurrentStage(f.getProcessInstanceId(), f.getCurrentStage(), f.getStatus()));
            m.put("currentActivity", stageResolver.resolveCurrentActivity(f.getProcessInstanceId(), f.getCurrentActivity()));
            m.put("health", f.getHealth());
            m.put("startedAt", f.getStartedAt());
            m.put("endedAt", f.getEndedAt());
            m.put("processInstanceId", f.getProcessInstanceId());
            m.put("blockReason", f.getBlockReason());
            int ac = 0, ec = 0;
            if (f.getProcessInstanceId() != null) {
                try {
                    List<SpkArtifactManifestDO> arts = artifactMapper.selectListByProcessInstanceId(f.getProcessInstanceId());
                    ac = arts == null ? 0 : arts.size();
                    List<SpkEvidenceRecordDO> evs = evidenceMapper.selectListByProcessInstanceId(f.getProcessInstanceId());
                    ec = evs == null ? 0 : evs.size();
                } catch (Exception ignore) {
                    // 单流程聚合失败不影响整体
                }
            }
            m.put("artifactCount", ac);
            m.put("evidenceCount", ec);
            totalArtifacts += ac;
            totalEvidence += ec;
            if ("RUNNING".equals(f.getStatus())) running++;
            if ("BLOCKED".equals(f.getStatus())) blocked++;
            flowList.add(m);
        }
        resp.setFlows(flowList);

        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("flows", (long) flows.size());
        summary.put("running", running);
        summary.put("blocked", blocked);
        summary.put("artifacts", totalArtifacts);
        summary.put("evidence", totalEvidence);
        // 项目维度：版本数、问题数
        try {
            if (projectId != null) {
                summary.put("versions", (long) versionMapper.selectList(
                        new LambdaQueryWrapperX<SpkIpdVersionDO>().eq(SpkIpdVersionDO::getProjectId, projectId)).size());
                summary.put("issues", (long) issueCaseMapper.selectList(
                        new LambdaQueryWrapperX<SpkIpdIssueCaseDO>().eq(SpkIpdIssueCaseDO::getProjectId, projectId)).size());
            } else {
                summary.put("versions", (long) versionMapper.selectList(null).size());
                summary.put("issues", (long) issueCaseMapper.selectList(null).size());
            }
        } catch (Exception ignore) {
        }
        resp.setSummary(summary);

        resp.setIntegrations(buildIntegrations());

        // 项目维度 3 视图：决策审查 / 制品基线（按 processInstance 聚合）/ 门禁
        List<String> pids = new ArrayList<>();
        Map<String, String> pidToRunNo = new LinkedHashMap<>();
        for (SpkIpdFlowRunDO f : flows) {
            if (f.getProcessInstanceId() != null) {
                pids.add(f.getProcessInstanceId());
                pidToRunNo.put(f.getProcessInstanceId(), f.getRunNo());
            }
        }
        resp.setDecisions(buildDecisions(projectId, pidToRunNo));
        resp.setGates(buildGates(pids, pidToRunNo));
        resp.setArtifacts(buildArtifacts(pids, pidToRunNo));
        // Phase2 J：迭代×子流程矩阵视图（同源 SpkStageResolver，行=迭代，列=flowType）
        resp.setIterationMatrix(buildIterationMatrix(flows, projectId));
        return resp;
    }

    /**
     * AI 成本与会话聚合（设计文档 §4.5 Tab6「AI 成本与会话」）。
     * <p>
     * 真实三跳：flow_run.pid → task_contract.contractId → run_receipt。
     * projectId 为空时聚合全部 run_receipt（全局维度）。无收据返回空结构，不伪造。
     * 聚合维度：按模型（sessions/successRate/Token/成本）+ 按 Activity + 日趋势（近 30 天）。
     */
    @Override
    public Map<String, Object> costs(Long projectId) {
        Map<String, Object> resp = new LinkedHashMap<>();
        // 1. 取项目内 flow_run 的 processInstanceId 集合（项目维度收敛 receipt 范围）
        List<String> pids = new ArrayList<>();
        if (projectId != null) {
            LambdaQueryWrapperX<SpkIpdFlowRunDO> fw = new LambdaQueryWrapperX<SpkIpdFlowRunDO>()
                    .eq(SpkIpdFlowRunDO::getProjectId, projectId);
            for (SpkIpdFlowRunDO f : flowRunMapper.selectList(fw)) {
                if (f.getProcessInstanceId() != null) {
                    pids.add(f.getProcessInstanceId());
                }
            }
        }
        // 2. pid → contractId（task_contract 关联键 processInstanceId，无 flow_run_id 列）
        List<SpkTaskContractDO> contracts;
        if (projectId == null) {
            contracts = new ArrayList<>(); // 全局维度直接按 receipt 聚合，无需 contract 中转
        } else if (pids.isEmpty()) {
            contracts = new ArrayList<>();
        } else {
            contracts = taskContractMapper.selectList(new LambdaQueryWrapperX<SpkTaskContractDO>()
                    .in(SpkTaskContractDO::getProcessInstanceId, pids));
        }
        // contractId → activityId/phase（按 Activity 聚合标签用）
        Map<String, String> contractActivity = new HashMap<>();
        Map<String, String> contractPhase = new HashMap<>();
        for (SpkTaskContractDO c : contracts) {
            if (c.getContractId() != null) {
                contractActivity.put(c.getContractId(), c.getActivityId());
                contractPhase.put(c.getContractId(), c.getPhase());
            }
        }
        // 3. 收敛 receipt：项目维度按 contractId IN(...) 过滤；全局取全量
        LambdaQueryWrapperX<SpkRunReceiptDO> rw = new LambdaQueryWrapperX<SpkRunReceiptDO>()
                .orderByDesc(SpkRunReceiptDO::getStartedAt).last("limit 1000");
        if (projectId != null) {
            if (contractActivity.isEmpty()) {
                // 项目无任何合同 → 必然无 receipt，直接返回空结构（不取全量造假）
                return emptyCosts();
            }
            rw.in(SpkRunReceiptDO::getContractId, contractActivity.keySet());
        }
        List<SpkRunReceiptDO> receipts = runReceiptMapper.selectList(rw);

        // —— 全局维度补拉 contract 以填充 byActivity 标签（项目维度已有 contracts）——
        if (projectId == null && !receipts.isEmpty()) {
            List<String> cids = new ArrayList<>();
            for (SpkRunReceiptDO r : receipts) {
                if (r.getContractId() != null && !cids.contains(r.getContractId())) {
                    cids.add(r.getContractId());
                }
            }
            if (!cids.isEmpty()) {
                // 分批 in 查询，避免超长 IN 列表
                int batch = 500;
                for (int i = 0; i < cids.size(); i += batch) {
                    List<String> chunk = cids.subList(i, Math.min(i + batch, cids.size()));
                    for (SpkTaskContractDO c : taskContractMapper.selectList(
                            new LambdaQueryWrapperX<SpkTaskContractDO>().in(SpkTaskContractDO::getContractId, chunk))) {
                        if (c.getContractId() != null) {
                            contractActivity.put(c.getContractId(), c.getActivityId());
                            contractPhase.put(c.getContractId(), c.getPhase());
                        }
                    }
                }
            }
        }

        // 4. 聚合
        BigDecimal totalCost = BigDecimal.ZERO;
        long totalTokens = 0, totalSessions = 0, successCount = 0, failedCount = 0;
        Map<String, BigDecimal> modelCost = new HashMap<>();
        Map<String, long[]> modelStat = new HashMap<>(); // [sessions, tokens, success, failed]
        Map<String, BigDecimal> activityCost = new HashMap<>();
        Map<String, long[]> activityStat = new HashMap<>(); // [sessions, tokens]
        Map<LocalDate, BigDecimal> dailyCost = new LinkedHashMap<>();
        Map<LocalDate, long[]> dailyStat = new HashMap<>(); // [sessions, tokens]

        LocalDate today = LocalDate.now();
        LocalDate since30 = today.minusDays(29);

        for (SpkRunReceiptDO r : receipts) {
            totalSessions++;
            String model = r.getModel() != null ? r.getModel() : "未指定模型";
            long tk = parseTokens(r.getTokenUsage());
            BigDecimal cost = r.getCost() != null ? r.getCost() : BigDecimal.ZERO;
            totalCost = totalCost.add(cost);
            totalTokens += tk;
            String st = r.getStatus() == null ? "" : r.getStatus().toLowerCase();
            boolean ok = "done".equals(st) || "success".equals(st);
            boolean failed = "failed".equals(st) || "timeout".equals(st);
            if (ok) { successCount++; } else if (failed) { failedCount++; }

            // 按模型
            modelCost.merge(model, cost, BigDecimal::add);
            long[] ms = modelStat.computeIfAbsent(model, k -> new long[4]);
            ms[0]++; ms[1] += tk;
            if (ok) ms[2]++;
            if (failed) ms[3]++;

            // 按 Activity
            String cid = r.getContractId();
            String act = cid != null ? contractActivity.getOrDefault(cid, "—") : "—";
            String actLabel = cid != null && contractPhase.containsKey(cid) ? contractPhase.get(cid) : act;
            activityCost.merge(actLabel, cost, BigDecimal::add);
            long[] as = activityStat.computeIfAbsent(actLabel, k -> new long[2]);
            as[0]++; as[1] += tk;

            // 日趋势（近 30 天，超出窗口不纳入趋势但不影响总量）
            if (r.getStartedAt() != null) {
                LocalDate d = r.getStartedAt().toLocalDate();
                if (!d.isBefore(since30) && !d.isAfter(today)) {
                    dailyCost.merge(d, cost, BigDecimal::add);
                    long[] ds = dailyStat.computeIfAbsent(d, k -> new long[2]);
                    ds[0]++; ds[1] += tk;
                }
            }
        }

        // —— totals ——
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("totalCost", totalCost.setScale(4, RoundingMode.HALF_UP));
        totals.put("totalTokens", totalTokens);
        totals.put("totalSessions", totalSessions);
        totals.put("successCount", successCount);
        totals.put("failedCount", failedCount);
        totals.put("successRate", totalSessions == 0 ? null
                : BigDecimal.valueOf(successCount * 100L).divide(BigDecimal.valueOf(totalSessions), 1, RoundingMode.HALF_UP));
        resp.put("totals", totals);

        // —— byModel ——
        List<Map<String, Object>> byModel = new ArrayList<>();
        for (String model : modelCost.keySet()) {
            long[] ms = modelStat.get(model);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("model", model);
            row.put("sessions", ms[0]);
            row.put("tokens", ms[1]);
            row.put("successCount", ms[2]);
            row.put("failedCount", ms[3]);
            row.put("successRate", ms[0] == 0 ? null
                    : BigDecimal.valueOf(ms[2] * 100L).divide(BigDecimal.valueOf(ms[0]), 1, RoundingMode.HALF_UP));
            row.put("totalCost", modelCost.get(model).setScale(4, RoundingMode.HALF_UP));
            byModel.add(row);
        }
        byModel.sort(Comparator.<Map<String, Object>, BigDecimal>comparing(
                m -> (BigDecimal) m.get("totalCost"), Comparator.reverseOrder()));
        resp.put("byModel", byModel);

        // —— byActivity ——
        List<Map<String, Object>> byAct = new ArrayList<>();
        for (String act : activityCost.keySet()) {
            long[] as = activityStat.get(act);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("activity", act);
            row.put("sessions", as[0]);
            row.put("tokens", as[1]);
            row.put("totalCost", activityCost.get(act).setScale(4, RoundingMode.HALF_UP));
            byAct.add(row);
        }
        byAct.sort(Comparator.<Map<String, Object>, BigDecimal>comparing(
                m -> (BigDecimal) m.get("totalCost"), Comparator.reverseOrder()));
        resp.put("byActivity", byAct);

        // —— dailyTrend（近 30 天，补齐空档显 0，不造假——空档确无收据）——
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            BigDecimal c = dailyCost.getOrDefault(d, BigDecimal.ZERO);
            long[] ds = dailyStat.get(d);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d.toString());
            row.put("cost", c.setScale(4, RoundingMode.HALF_UP));
            row.put("tokens", ds == null ? 0 : ds[1]);
            row.put("sessions", ds == null ? 0 : ds[0]);
            trend.add(row);
        }
        resp.put("dailyTrend", trend);

        return resp;
    }

    /** 空成本结构（项目无合同/收据时返回，不伪造） */
    private Map<String, Object> emptyCosts() {
        Map<String, Object> resp = new LinkedHashMap<>();
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("totalCost", BigDecimal.ZERO);
        totals.put("totalTokens", 0L);
        totals.put("totalSessions", 0L);
        totals.put("successCount", 0L);
        totals.put("failedCount", 0L);
        totals.put("successRate", null);
        resp.put("totals", totals);
        resp.put("byModel", new ArrayList<>());
        resp.put("byActivity", new ArrayList<>());
        resp.put("dailyTrend", new ArrayList<>());
        return resp;
    }

    /** 解析 tokenUsage JSON {prompt,completion,total}，失败回退 total 字段或 0 */
    private long parseTokens(String tokenUsage) {
        if (tokenUsage == null || tokenUsage.isEmpty()) {
            return 0L;
        }
        try {
            JSONObject obj = JSONUtil.parseObj(tokenUsage);
            long total = obj.getLong("total", 0L);
            if (total > 0) {
                return total;
            }
            long prompt = obj.getLong("prompt", 0L);
            long completion = obj.getLong("completion", 0L);
            return prompt + completion;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Phase2 J：迭代×子流程矩阵。行=迭代（majorNo/versionNo，ISSUE_RESOLUTION 无版本归 "ISSUE" 行），
     * 列=flowType，单元格=FlowRun 状态+currentStage（SpkStageResolver 实时算）+产物/证据计数。
     */
    private List<Map<String, Object>> buildIterationMatrix(List<SpkIpdFlowRunDO> flows, Long projectId) {
        // 版本索引：versionId → VersionDO（取 majorNo/versionNo 标签）
        List<SpkIpdVersionDO> versions;
        try {
            versions = versionMapper.selectList(projectId != null
                    ? new LambdaQueryWrapperX<SpkIpdVersionDO>().eq(SpkIpdVersionDO::getProjectId, projectId)
                    : null);
        } catch (Exception e) {
            versions = new ArrayList<>();
        }
        java.util.Map<Long, SpkIpdVersionDO> vMap = new java.util.HashMap<>();
        if (versions != null) {
            for (SpkIpdVersionDO v : versions) {
                vMap.put(v.getId(), v);
            }
        }
        // 按迭代键聚合：有 versionId 用 "<majorNo>|<versionNo>"，ISSUE 无版本用 "ISSUE|<issueCaseId>"
        java.util.Map<String, Map<String, Object>> rowMap = new java.util.LinkedHashMap<>();
        for (SpkIpdFlowRunDO f : flows) {
            String key;
            String majorNo = null;
            String versionNo = null;
            if (f.getVersionId() != null) {
                SpkIpdVersionDO v = vMap.get(f.getVersionId());
                if (v != null) {
                    majorNo = v.getMajorNo() == null ? "?" : String.valueOf(v.getMajorNo());
                    versionNo = v.getVersionNo() == null ? "?" : v.getVersionNo();
                } else {
                    majorNo = "?";
                    versionNo = "v" + f.getVersionId();
                }
                key = majorNo + "|" + versionNo;
            } else {
                key = "ISSUE|" + (f.getIssueCaseId() == null ? "?" : f.getIssueCaseId());
            }
            Map<String, Object> row = rowMap.get(key);
            if (row == null) {
                row = new LinkedHashMap<>();
                row.put("iterationKey", key);
                row.put("majorNo", majorNo);
                row.put("versionNo", versionNo);
                row.put("versionId", f.getVersionId());
                row.put("issueCaseId", f.getIssueCaseId());
                row.put("projectId", f.getProjectId());
                row.put("cells", new LinkedHashMap<String, Map<String, Object>>());
                rowMap.put(key, row);
            }
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> cells = (Map<String, Map<String, Object>>) row.get("cells");
            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put("flowRunId", f.getId());
            cell.put("runNo", f.getRunNo());
            cell.put("status", f.getStatus());
            cell.put("currentStage", stageResolver.resolveCurrentStage(
                    f.getProcessInstanceId(), f.getCurrentStage(), f.getStatus()));
            cell.put("health", f.getHealth());
            int ac = 0, ec = 0;
            if (f.getProcessInstanceId() != null) {
                try {
                    List<SpkArtifactManifestDO> arts = artifactMapper.selectListByProcessInstanceId(f.getProcessInstanceId());
                    ac = arts == null ? 0 : arts.size();
                    List<SpkEvidenceRecordDO> evs = evidenceMapper.selectListByProcessInstanceId(f.getProcessInstanceId());
                    ec = evs == null ? 0 : evs.size();
                } catch (Exception ignore) {
                }
            }
            cell.put("artifactCount", ac);
            cell.put("evidenceCount", ec);
            cells.put(f.getFlowType(), cell);
        }
        return new ArrayList<>(rowMap.values());
    }

    /** 决策审查：decision_record 按 projectId 直查（DO 带 projectId），并带出所属流程号 */
    private List<Map<String, Object>> buildDecisions(Long projectId, Map<String, String> pidToRunNo) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (projectId == null) {
            return rows; // 全项目维度暂不聚合决策，避免跨项目混淆
        }
        List<SpkIpdDecisionRecordDO> list = decisionMapper.selectList(
                new LambdaQueryWrapperX<SpkIpdDecisionRecordDO>()
                        .eq(SpkIpdDecisionRecordDO::getProjectId, projectId)
                        .orderByDesc(SpkIpdDecisionRecordDO::getId));
        for (SpkIpdDecisionRecordDO d : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("taskId", d.getTaskId());
            m.put("flowRunId", d.getFlowRunId());
            m.put("flowRunNo", pidToRunNo.getOrDefault(d.getProcessInstanceId(), null));
            m.put("decision", d.getDecision());
            m.put("reason", d.getReason());
            m.put("decisionPackageHash", d.getDecisionPackageHash());
            m.put("deciderUserId", d.getDeciderUserId());
            m.put("redirectTargetTaskKey", d.getRedirectTargetTaskKey());
            rows.add(m);
        }
        return rows;
    }

    /** 门禁：gate_record 按 processInstance 聚合 */
    private List<Map<String, Object>> buildGates(List<String> pids, Map<String, String> pidToRunNo) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String pid : pids) {
            List<SpkGateRecordDO> list = gateMapper.selectListByInstanceId(pid);
            if (list == null || list.isEmpty()) {
                continue;
            }
            for (SpkGateRecordDO g : list) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", g.getId());
                m.put("flowRunNo", pidToRunNo.get(pid));
                m.put("processInstanceId", pid);
                m.put("nodeKey", g.getNodeKey());
                m.put("gate", g.getGate());
                m.put("pass", g.getPass());
                m.put("report", g.getReport());
                m.put("callbackTime", g.getCallbackTime());
                rows.add(m);
            }
        }
        return rows;
    }

    /** 制品基线：artifact_manifest 按 processInstance 聚合，含哈希/签名/类型 */
    private List<Map<String, Object>> buildArtifacts(List<String> pids, Map<String, String> pidToRunNo) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String pid : pids) {
            List<SpkArtifactManifestDO> list;
            try {
                list = artifactMapper.selectListByProcessInstanceId(pid);
            } catch (Exception ignore) {
                continue;
            }
            if (list == null || list.isEmpty()) {
                continue;
            }
            for (SpkArtifactManifestDO a : list) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", a.getId());
                m.put("artifactId", a.getArtifactId());
                m.put("artifactType", a.getArtifactType());
                m.put("flowRunNo", pidToRunNo.get(pid));
                m.put("activityRunId", a.getActivityRunId());
                m.put("contentHash", a.getContentHash());
                m.put("version", a.getVersion());
                m.put("status", a.getStatus());
                m.put("signerRequired", a.getSignerRequired());
                m.put("signedBy", a.getSignedBy());
                m.put("signedAt", a.getSignedAt());
                m.put("mime", a.getMime());
                m.put("bytes", a.getBytes());
                m.put("summary", a.getSummary());
                rows.add(m);
            }
        }
        return rows;
    }

    private List<IntegrationHealth> buildIntegrations() {
        List<IntegrationHealth> list = new ArrayList<>();
        list.add(health("Plane", () -> planeService.healthCheck(), "需求管理"));
        list.add(health("Gitea", () -> giteaService.healthCheck(), "代码/CI"));
        list.add(health("Omnigent", () -> omnigentHealth(), "Agent 运行时"));
        return list;
    }

    private boolean omnigentHealth() {
        // Omnigent 无显式 healthCheck，用获取会话探活（失败即不健康）
        try {
            omnigentService.getSession("healthcheck");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private IntegrationHealth health(String name, java.util.function.Supplier<Boolean> check, String detail) {
        IntegrationHealth h = new IntegrationHealth();
        h.setName(name);
        h.setDetail(detail);
        try {
            h.setHealthy(Boolean.TRUE.equals(check.get()));
        } catch (Exception e) {
            h.setHealthy(false);
            h.setDetail(detail + "：" + e.getMessage());
        }
        return h;
    }
}
