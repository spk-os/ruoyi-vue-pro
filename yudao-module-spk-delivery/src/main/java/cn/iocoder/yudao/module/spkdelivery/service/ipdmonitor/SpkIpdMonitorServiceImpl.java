package cn.iocoder.yudao.module.spkdelivery.service.ipdmonitor;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdmonitor.vo.SpkIpdMonitorRespVO;
import cn.iocoder.yudao.module.spkdelivery.controller.admin.ipdmonitor.vo.SpkIpdMonitorRespVO.IntegrationHealth;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkEvidenceRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.gate.SpkGateRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdDecisionRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdFlowRunDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdIssueCaseDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdbusiness.SpkIpdVersionDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkEvidenceRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.gate.SpkGateRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdDecisionRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdFlowRunMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdIssueCaseMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdbusiness.SpkIpdVersionMapper;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkGiteaIntegrationService;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkOmnigentProxyService;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkPlaneIntegrationService;
import cn.iocoder.yudao.module.spkdelivery.service.ipdbusiness.SpkStageResolver;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        return resp;
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
