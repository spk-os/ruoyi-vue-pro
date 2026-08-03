package cn.iocoder.yudao.module.spkdelivery.service.cockpit;

import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkEvidenceRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkRunReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkVerificationReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkEvidenceRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkRunReceiptMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkVerificationReceiptMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentKindEnum;
import cn.iocoder.yudao.module.spkdelivery.service.evidence.SpkEvidenceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SPK-OS IPD Cockpit 聚合服务
 * <p>
 * 给前端 Dashboard 提供：泳道图（按流程实例聚合 stages→activities）、Activity 详情
 * （三件套 + 证据链）、Agent 负载看板。聚合自 spk_task_contract / artifact / run_receipt /
 * verification_receipt / evidence / agent_def，不依赖 Flowable 历史表（更稳）。依 §9。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkIpdCockpitService {

    @Resource
    private SpkTaskContractMapper contractMapper;
    @Resource
    private SpkArtifactManifestMapper artifactMapper;
    @Resource
    private SpkRunReceiptMapper runReceiptMapper;
    @Resource
    private SpkVerificationReceiptMapper verificationReceiptMapper;
    @Resource
    private SpkEvidenceRecordMapper evidenceMapper;
    @Resource
    private SpkAgentDefMapper agentDefMapper;
    @Resource
    private SpkEvidenceService evidenceService;

    /**
     * 泳道：按阶段聚合某流程实例的全部 Activity 合同。
     */
    public Map<String, Object> swimlane(String processInstanceId) {
        List<SpkTaskContractDO> contracts = contractMapper.selectListByProcessInstanceId(processInstanceId);
        List<SpkArtifactManifestDO> artifacts = artifactMapper.selectListByProcessInstanceId(processInstanceId);
        // 按 activityRunId 索引产物数与验证结论
        Map<String, Integer> artifactCount = new LinkedHashMap<>();
        Map<String, String> conclusion = new LinkedHashMap<>();
        for (SpkArtifactManifestDO a : artifacts) {
            artifactCount.merge(a.getActivityRunId(), 1, Integer::sum);
        }
        for (SpkTaskContractDO c : contracts) {
            List<SpkVerificationReceiptDO> rs = verificationReceiptMapper.selectListByActivityRunId(c.getActivityRunId());
            if (!rs.isEmpty()) {
                conclusion.put(c.getActivityRunId(), rs.get(0).getOverallConclusion());
            }
        }
        // 按阶段分组
        Map<String, List<Map<String, Object>>> byStage = new LinkedHashMap<>();
        for (SpkTaskContractDO c : contracts) {
            String stage = c.getPhase() != null ? c.getPhase() : "unknown";
            byStage.computeIfAbsent(stage, k -> new ArrayList<>()).add(activityCard(c,
                    artifactCount.getOrDefault(c.getActivityRunId(), 0),
                    conclusion.get(c.getActivityRunId())));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstanceId", processInstanceId);
        result.put("stages", byStage);
        result.put("total", contracts.size());
        return result;
    }

    /**
     * Activity 详情：三件套 + 证据链。
     */
    public Map<String, Object> activityDetail(String activityRunId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activityRunId", activityRunId);
        // Task Contract
        SpkTaskContractDO contract = contractMapper.selectByActivityRunId(activityRunId);
        result.put("contract", contract);
        // ContextManifest URI
        if (contract != null) {
            result.put("contextManifestUri", contract.getContextManifestUri());
        }
        // ArtifactManifest
        result.put("artifacts", artifactMapper.selectListByActivityRunId(activityRunId));
        // RunReceipt
        result.put("runReceipt", runReceiptMapper.selectByActivityRunId(activityRunId));
        // VerificationReceipt
        result.put("verifications", verificationReceiptMapper.selectListByActivityRunId(activityRunId));
        // Evidence chain
        List<SpkEvidenceRecordDO> chain = evidenceMapper.selectListByActivityRunId(activityRunId);
        result.put("evidenceChain", chain);
        result.put("chainValid", evidenceService.verifyChain(activityRunId));
        return result;
    }

    /**
     * Agent 负载看板：12 Lead + 3 Verifier 的 running 合同数。
     */
    public Map<String, Object> agentLoad() {
        List<SpkAgentDefDO> leads = agentDefMapper.selectListByAgentKind(SpkAgentKindEnum.LEAD.getLabel());
        List<SpkAgentDefDO> verifiers = agentDefMapper.selectListByAgentKind(SpkAgentKindEnum.VERIFIER.getLabel());
        List<Map<String, Object>> leadRows = new ArrayList<>();
        for (SpkAgentDefDO a : leads) {
            leadRows.add(agentRow(a));
        }
        List<Map<String, Object>> verifierRows = new ArrayList<>();
        for (SpkAgentDefDO a : verifiers) {
            verifierRows.add(agentRow(a));
        }
        // 死信/积压：failed 合同数
        List<SpkTaskContractDO> failed = contractMapper.selectListByStatus("failed");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("leads", leadRows);
        result.put("verifiers", verifierRows);
        result.put("deadLetterCount", failed.size());
        return result;
    }

    private Map<String, Object> activityCard(SpkTaskContractDO c, int artifacts, String verdict) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activityRunId", c.getActivityRunId());
        m.put("activityId", c.getActivityId());
        m.put("nodeKey", c.getNodeKey());
        m.put("stage", c.getPhase());
        m.put("leadAgentCode", c.getLeadAgentCode());
        m.put("status", c.getStatus());
        m.put("artifactCount", artifacts);
        m.put("verificationConclusion", verdict);
        m.put("queuedAt", c.getQueuedAt());
        m.put("finishedAt", c.getFinishedAt());
        return m;
    }

    private Map<String, Object> agentRow(SpkAgentDefDO a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("code", a.getCode());
        m.put("name", a.getName());
        m.put("agentKind", a.getAgentKind());
        m.put("status", a.getStatus());
        m.put("runningContracts", contractMapper.countRunningByLeadAgentId(a.getId()));
        m.put("model", a.getModel());
        return m;
    }

}
