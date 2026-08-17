package cn.iocoder.yudao.module.spkdelivery.service.cockpit;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkEvidenceRecordDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkRunReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkVerificationReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdactivity.SpkIpdActivityDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.artifact.SpkArtifactManifestMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkEvidenceRecordMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkRunReceiptMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkVerificationReceiptMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdactivity.SpkIpdActivityDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentKindEnum;
import cn.iocoder.yudao.module.spkdelivery.service.evidence.SpkEvidenceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
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
    @Resource
    private SpkIpdActivityDefMapper activityDefMapper;

    /** Gitea 文档链接构造（与 SpkGiteaIntegrationService 同源配置；Cockpit 只读拼接，不调 API）。
     *  文档 path 规则见 SpkTaskRouterService L179：docs/{stage}/{activityRunId}.md */
    @Value("${spk-delivery.gitea.base-url:http://192.168.56.101:3000}")
    private String giteaBaseUrl;
    @Value("${spk-delivery.gitea.owner:spk-os}")
    private String giteaOwner;
    @Value("${spk-delivery.gitea.repo:smart-home-hub}")
    private String giteaRepo;

    /**
     * 泳道：按阶段聚合某流程实例的全部 Activity 合同。
     */
    public Map<String, Object> swimlane(String processInstanceId) {
        List<SpkTaskContractDO> contracts = contractMapper.selectListByProcessInstanceId(processInstanceId);
        List<SpkArtifactManifestDO> artifacts = artifactMapper.selectListByProcessInstanceId(processInstanceId);
        // activityId → 中文名（spk_ipd_activity_def.name），卡片显示实际内容而非 ACT-xx 编号
        Map<String, String> nameByActivityId = loadActivityNames(contracts);
        // leadAgentCode → 中文名（spk_agent_def.name），卡片显示智能体名而非 lead-xxx 编号
        Map<String, String> nameByAgentCode = loadAgentNames(contracts);
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
        // 按 nodeKey 去重：intervene rerun 会生成新合同（新 activityRunId，同 nodeKey），
        // 只保留每个节点最新的合同，避免已 rerun 恢复的节点仍显示旧的 failed 卡片造成误判。
        // contracts 已按 queuedAt ASC 排序，后者覆盖前者 = 最新；LinkedHashMap 保留首次出现顺序（= 流程编排顺序）。
        Map<String, SpkTaskContractDO> latestByNode = new LinkedHashMap<>();
        for (SpkTaskContractDO c : contracts) {
            String key = c.getNodeKey() != null ? c.getNodeKey() : c.getActivityRunId();
            latestByNode.put(key, c);
        }
        // 按阶段分组
        Map<String, List<Map<String, Object>>> byStage = new LinkedHashMap<>();
        for (SpkTaskContractDO c : latestByNode.values()) {
            String stage = c.getPhase() != null ? c.getPhase() : "unknown";
            byStage.computeIfAbsent(stage, k -> new ArrayList<>()).add(activityCard(c,
                    artifactCount.getOrDefault(c.getActivityRunId(), 0),
                    conclusion.get(c.getActivityRunId()),
                    nameByActivityId.getOrDefault(c.getActivityId(), c.getActivityId()),
                    nameByAgentCode.getOrDefault(c.getLeadAgentCode(), c.getLeadAgentCode())));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstanceId", processInstanceId);
        result.put("stages", byStage);
        result.put("total", latestByNode.size());
        return result;
    }

    /**
     * Activity 详情：三件套 + 证据链 + 文档链接。
     */
    public Map<String, Object> activityDetail(String activityRunId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activityRunId", activityRunId);
        // Task Contract
        SpkTaskContractDO contract = contractMapper.selectByActivityRunId(activityRunId);
        result.put("contract", contract);
        // 中文名（spk_ipd_activity_def.name）—— 替代 ACT-xx 编号显示
        if (contract != null) {
            SpkIpdActivityDefDO def = activityDefMapper.selectByActivityIdAndVersion(
                    contract.getActivityId(), contract.getActivityVersion());
            result.put("activityName", def != null ? def.getName() : contract.getActivityId());
            // leadAgentCode → 中文名（spk_agent_def.name），单点查；失败兜底回落 code 原文
            SpkAgentDefDO agent = contract.getLeadAgentCode() == null ? null
                    : agentDefMapper.selectByCode(contract.getLeadAgentCode());
            result.put("leadAgentName", agent != null ? agent.getName() : contract.getLeadAgentCode());
            result.put("contextManifestUri", contract.getContextManifestUri());
        }
        // ArtifactManifest —— 附 Gitea 文档链接
        // product 模式文档落在 concept/{runId} 分支，真值在 delivery evidence（giteaDocUrl/prUrl/branch）；
        // test 模式文档落在 main，无 delivery evidence → fallback composeDocUrl(main)。
        List<SpkArtifactManifestDO> artifacts = artifactMapper.selectListByActivityRunId(activityRunId);
        // Evidence chain 提前查，供 artifactListWithDocUrl 取 delivery 真值
        List<SpkEvidenceRecordDO> chain = evidenceMapper.selectListByActivityRunId(activityRunId);
        result.put("artifacts", artifactListWithDocUrl(artifacts, contract, chain));
        // RunReceipt
        result.put("runReceipt", runReceiptMapper.selectByActivityRunId(activityRunId));
        // VerificationReceipt
        result.put("verifications", verificationReceiptMapper.selectListByActivityRunId(activityRunId));
        // Evidence chain
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

    private Map<String, Object> activityCard(SpkTaskContractDO c, int artifacts, String verdict, String activityName, String leadAgentName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activityRunId", c.getActivityRunId());
        m.put("activityId", c.getActivityId());
        m.put("name", activityName);
        m.put("nodeKey", c.getNodeKey());
        m.put("stage", c.getPhase());
        m.put("leadAgentCode", c.getLeadAgentCode());
        m.put("leadAgentName", leadAgentName);
        m.put("status", c.getStatus());
        m.put("artifactCount", artifacts);
        m.put("verificationConclusion", verdict);
        m.put("queuedAt", c.getQueuedAt());
        m.put("finishedAt", c.getFinishedAt());
        return m;
    }

    /**
     * 批量取 activityId → 中文名映射（同 activityId 多版本保留首条，Cockpit 只需展示名）。
     */
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
        for (SpkIpdActivityDefDO d : activityDefMapper.selectListByActivityIds(ids)) {
            map.putIfAbsent(d.getActivityId(), d.getName());
        }
        return map;
    }

    /**
     * 批量取 leadAgentCode → 中文名映射（spk_agent_def.name，Cockpit 只需展示名）。
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

    /**
     * 构造每个 artifact 的 Gitea 文档链接。
     * 一个 activityRunId 只产一篇 md（router L179 用 activityRunId 命名），多 artifact 共享同链。
     * product 模式文档在 concept/{runId} 分支，真值取自 delivery evidence（route doProductDelivery 落）；
     * test 模式文档在 main 分支，无 delivery evidence → fallback composeDocUrl(main)。
     */
    private List<Map<String, Object>> artifactListWithDocUrl(List<SpkArtifactManifestDO> arts,
                                                              SpkTaskContractDO contract,
                                                              List<SpkEvidenceRecordDO> chain) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (arts == null) {
            return list;
        }
        String stage = contract != null ? contract.getPhase() : null;
        String runId = contract != null ? contract.getActivityRunId() : null;
        // product 模式真值：从 delivery evidence payload 取 giteaDocUrl/prUrl/branch
        String docUrl = null;
        String prUrl = null;
        String branch = null;
        if (chain != null) {
            for (SpkEvidenceRecordDO e : chain) {
                String p = e.getPayload();
                if (p == null || !p.contains("giteaDocUrl")) {
                    continue;
                }
                try {
                    Map<String, Object> d = JsonUtils.parseObject(p, Map.class);
                    if (d != null && d.get("giteaDocUrl") instanceof String s && !s.isBlank()) {
                        docUrl = s;
                        if (d.get("prUrl") instanceof String ps && !ps.isBlank()) {
                            prUrl = ps;
                        }
                        if (d.get("branch") instanceof String bs && !bs.isBlank()) {
                            branch = bs;
                        }
                        break;
                    }
                } catch (Exception ignore) {
                    // payload 非 JSON，跳过本条
                }
            }
        }
        // fallback: test 模式文档在 main，按 docs/{stage}/{runId}.md 构造
        if (docUrl == null && stage != null && runId != null) {
            docUrl = composeDocUrl(stage, runId);
        }
        for (SpkArtifactManifestDO a : arts) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("artifactId", a.getArtifactId());
            m.put("artifactType", a.getArtifactType());
            m.put("summary", a.getSummary());
            m.put("metadata", a.getMetadata());
            m.put("uri", a.getUri());
            m.put("status", a.getStatus());
            m.put("version", a.getVersion());
            m.put("signedBy", a.getSignedBy());
            m.put("contentHash", a.getContentHash());
            m.put("giteaUrl", docUrl);
            if (prUrl != null) {
                m.put("prUrl", prUrl);
            }
            if (branch != null) {
                m.put("branch", branch);
            }
            list.add(m);
        }
        return list;
    }

    private String composeDocUrl(String stage, String activityRunId) {
        return giteaBaseUrl + "/" + giteaOwner + "/" + giteaRepo
                + "/src/branch/main/docs/" + stage + "/" + activityRunId + ".md";
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
