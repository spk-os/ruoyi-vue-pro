package cn.iocoder.yudao.module.spkdelivery.service.router;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.artifact.SpkArtifactManifestDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkRunReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.evidence.SpkVerificationReceiptDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.ipdactivity.SpkIpdActivityDefDO;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.taskcontract.SpkTaskContractDO;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.agentdef.SpkAgentDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.evidence.SpkRunReceiptMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.ipdactivity.SpkIpdActivityDefMapper;
import cn.iocoder.yudao.module.spkdelivery.dal.mysql.taskcontract.SpkTaskContractMapper;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentKindEnum;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkEvidenceTypeEnum;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkTaskContractStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.service.agent.FrameworkAdapter;
import cn.iocoder.yudao.module.spkdelivery.service.agent.SpkAgentDispatchReq;
import cn.iocoder.yudao.module.spkdelivery.service.agent.SpkAgentDispatchResult;
import cn.iocoder.yudao.module.spkdelivery.service.artifact.SpkArtifactService;
import cn.iocoder.yudao.module.spkdelivery.service.context.SpkContextBuilderService;
import cn.iocoder.yudao.module.spkdelivery.service.evidence.SpkEvidenceService;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkGiteaIntegrationService;
import cn.iocoder.yudao.module.spkdelivery.service.modelcapability.SpkModelCapabilityRegistry;
import cn.iocoder.yudao.module.spkdelivery.service.verifier.SpkVerifierService;
import cn.iocoder.yudao.module.spkdelivery.framework.monitoring.SpkIpdMetrics;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.spkdelivery.enums.ErrorCodeConstants.*;

/**
 * SPK-OS Task Router —— IPD Activity 路由判决 + 三件套执行编排
 * <p>
 * 铁律 2：一个 Activity 只由一个 Lead Agent 承接。本类按 capability_tags 全匹配 + agent_kind=lead
 * + 负载最低选 Lead，冻结模型快照，装配 ContextManifest，写 TaskContract(queued)，经
 * FrameworkAdapter 派发，落 ArtifactManifest + RunReceipt（+条件 VerificationReceipt）+ Evidence。
 * 铁律 4：三件套缺失不得 completed。依 §5.1~§5.7。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
public class SpkTaskRouterService {

    @Resource
    private SpkIpdActivityDefMapper activityDefMapper;
    @Resource
    private SpkAgentDefMapper agentDefMapper;
    @Resource
    private SpkTaskContractMapper contractMapper;
    @Resource
    private SpkRunReceiptMapper runReceiptMapper;
    @Resource
    private SpkModelCapabilityRegistry capabilityRegistry;
    @Resource
    private SpkContextBuilderService contextBuilder;
    @Resource
    private SpkArtifactService artifactService;
    @Resource
    private SpkEvidenceService evidenceService;
    @Resource
    private SpkVerifierService verifierService;
    @Resource
    private SpkGiteaIntegrationService giteaService;
    @Autowired
    private List<FrameworkAdapter> adapterList;
    /** 适配器名 → 适配器实例（按 FrameworkAdapter.getName() 索引；PostConstruct 装配） */
    private final Map<String, FrameworkAdapter> adapterMap = new java.util.concurrent.ConcurrentHashMap<>();
    @Value("${spk-delivery.execution.adapter:native-ai}")
    private String defaultAdapterName;
    @Resource
    private SpkIpdMetrics metrics;

    /**
     * 路由 + 执行一次 Activity（铁律：一 Activity 一 Lead，三件套齐全才记 done）。
     */
    public SpkRouteResult route(String activityId, String activityVersion,
                                String processInstanceId, String taskId, String businessKey,
                                String nodeKey, List<String> inputRefs) {
        // 1. 加载 Activity 定义
        SpkIpdActivityDefDO def = activityDefMapper.selectByActivityIdAndVersion(activityId,
                activityVersion != null ? activityVersion : "1.0.0");
        if (def == null) {
            throw exception(IPD_ACTIVITY_NOT_EXISTS);
        }
        if (!"active".equals(def.getStatus())) {
            throw exception(IPD_ACTIVITY_DISABLED);
        }
        // 2. 生成 activityRunId + 选 Lead
        String activityRunId = "run-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        SpkAgentDefDO lead = selectLead(def);
        if (lead == null) {
            throw exception(TASK_ROUTER_NO_LEAD);
        }
        // 3. 冻结模型快照
        String snapshotId = capabilityRegistry.freezeSnapshot("router:" + lead.getCode());
        // 4. 装配 ContextManifest
        String contextUri = contextBuilder.build(activityRunId, def, inputRefs);
        // 5. 写 TaskContract(queued)
        String contractId = "ctc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        SpkTaskContractDO contract = SpkTaskContractDO.builder()
                .contractId(contractId)
                .activityRunId(activityRunId)
                .activityId(activityId)
                .activityVersion(def.getVersion())
                .processInstanceId(processInstanceId)
                .taskId(taskId)
                .businessKey(businessKey)
                .phase(def.getStage())
                .nodeKey(nodeKey)
                .executionMode(def.getExecutionLocation())
                .leadAgentId(lead.getId())
                .leadAgentCode(lead.getCode())
                .workerRequired(def.getUseWorkerAgent())
                .verifierRequired(def.getUseIndependentVerifier())
                .verifierType(def.getVerifierType())
                .modelSnapshotId(snapshotId)
                .contextManifestUri(contextUri)
                .inputRefs(JsonUtils.toJsonString(inputRefs))
                .outputSpec(def.getOutputArtifactType())
                .timeoutSeconds(600)
                .retryPolicy("{}")
                .prompt(def.getPromptTemplate())
                .status(SpkTaskContractStatusEnum.QUEUED.getLabel())
                .queuedAt(LocalDateTime.now())
                .build();
        contractMapper.insert(contract);
        // 6. 派发 Lead（标记 running）
        markContractRunning(contract);
        SpkRouteResult result = new SpkRouteResult()
                .setActivityRunId(activityRunId)
                .setContractId(contractId)
                .setLeadAgentId(lead.getId())
                .setLeadAgentCode(lead.getCode())
                .setModelSnapshotId(snapshotId)
                .setContextManifestUri(contextUri);
        try {
            SpkAgentDispatchReq req = new SpkAgentDispatchReq()
                    .setRoleId(lead.getRoleId())
                    .setPrompt(def.getPromptTemplate())
                    .setInstanceId(processInstanceId)
                    .setNodeKey(nodeKey);
            FrameworkAdapter adapter = selectAdapter(def);
            SpkAgentDispatchResult dispatch = adapter.dispatchTask(req);
            result.setProvider(adapter.getName());
            // 持久化 adapter 返回的 taskId（omnigent=conv_xxx 会话 id / native-ai=convId#sendMsgId）
            // 到 contract.taskId，供 Activity 详情按 activityRunId 解析出 Omnigent session 嵌入会话视图。
            // 零 schema 变更：contract.taskId 已是 varchar 列，IPD 路径原本存 null。
            if (dispatch.getTaskId() != null && !dispatch.getTaskId().isBlank()) {
                contract.setTaskId(dispatch.getTaskId());
                contractMapper.updateById(contract);
            }
            // 7. 解析 Lead 产物 {document, summary, conclusion}（P6：长文档不进流程变量）
            Payload payload = parsePayload(dispatch.getResult());
            // 8. 落 Artifact（全文）+ RunReceipt + Evidence（三件套）
            SpkArtifactManifestDO artifact = artifactService.register(activityRunId, processInstanceId,
                    def.getOutputArtifactType(), payload.document,
                    truncate(def.getName() + " 产物", 200));
            result.setArtifactId(artifact.getArtifactId());
            // 9. 长文档提交 Gitea，失败不阻断 Activity done（agentResult 存 giteaUrl:null + error）
            String giteaUrl = null;
            String giteaError = null;
            try {
                if (payload.document != null && !payload.document.isBlank()) {
                    String path = "docs/" + def.getStage() + "/" + activityRunId + ".md";
                    giteaUrl = giteaService.createFile(path, payload.document, "main",
                            "docs(ipd): " + def.getName());
                }
            } catch (Exception ge) {
                giteaError = truncate(ge.getMessage(), 200);
                log.warn("[route][activityRunId={} gitea createFile 失败：{}]", activityRunId, giteaError);
            }
            // 10. agentResult 只存摘要+结论+链接（不存长文档，避免 Flowable 变量膨胀）
            Map<String, Object> agentResultObj = new LinkedHashMap<>();
            agentResultObj.put("summary", payload.summary);
            agentResultObj.put("conclusion", payload.conclusion);
            agentResultObj.put("giteaUrl", giteaUrl);
            agentResultObj.put("artifactId", artifact.getArtifactId());
            if (giteaError != null) {
                agentResultObj.put("giteaError", giteaError);
            }
            result.setAgentResult(JsonUtils.toJsonString(agentResultObj));
            SpkRunReceiptDO receipt = writeRunReceipt(activityRunId, contractId, lead, dispatch,
                    artifact, processInstanceId, def, adapter.getName());
            result.setRunReceiptId(receipt.getRunId()).setProvider(adapter.getName()).setModel(receipt.getModel());
            metrics.incrementArtifact();
            // 8. 条件验证
            if (def.getUseIndependentVerifier() != null && def.getUseIndependentVerifier() == 1) {
                SpkAgentDefDO verifier = selectVerifier(def);
                if (verifier != null) {
                    SpkVerificationReceiptDO v = verifierService.verify(verifier, artifact,
                            activityRunId, processInstanceId);
                    result.setVerificationConclusion(v.getOverallConclusion())
                            .setVerifierCode(verifier.getCode());
                    artifact = artifactService.sign(artifact.getArtifactId(), verifier.getCode());
                    metrics.incrementVerification(v.getOverallConclusion());
                }
            } else {
                artifact = artifactService.sign(artifact.getArtifactId(), lead.getCode());
            }
            result.setArtifactId(artifact.getArtifactId());
            // 9. 标记 done
            markContractDone(contract);
            result.setStatus(SpkTaskContractStatusEnum.DONE.getLabel());
            metrics.incrementActivityRun("done");
            metrics.incrementAgentTask("done");
            // 10. 总证据
            evidenceService.append(activityRunId, processInstanceId,
                    SpkEvidenceTypeEnum.RUN.getLabel(), receipt.getRunId(),
                    evidencePayload(result, def));
            log.info("[route][activityRunId={} activityId={} lead={} verdict={} done]",
                    activityRunId, activityId, lead.getCode(), result.getVerificationConclusion());
            return result;
        } catch (Exception e) {
            log.error("[route][activityRunId={} fail]", activityRunId, e);
            markContractFailed(contract, truncate(e.getMessage(), 500));
            result.setStatus(SpkTaskContractStatusEnum.FAILED.getLabel());
            metrics.incrementActivityRun("failed");
            metrics.incrementAgentTask("failed");
            metrics.incrementDeadLetter();
            evidenceService.append(activityRunId, processInstanceId,
                    SpkEvidenceTypeEnum.RUN.getLabel(), contractId,
                    failPayload(result, e));
            return result;
        }
    }

    // ===== Adapter 选择：按 spk-delivery.execution.adapter 配置（native-ai / omnigent） =====
    @PostConstruct
    private void initAdapters() {
        if (adapterList == null || adapterList.isEmpty()) {
            log.warn("[initAdapters][未发现任何 FrameworkAdapter 实现，路由将无法派发]");
            return;
        }
        for (FrameworkAdapter a : adapterList) {
            adapterMap.put(a.getName(), a);
        }
        log.info("[initAdapters][已注册 adapter={}，默认={}]",
                adapterMap.keySet(), defaultAdapterName);
        if (!adapterMap.containsKey(defaultAdapterName)) {
            log.warn("[initAdapters][配置的默认 adapter={} 不存在，回退 native-ai]", defaultAdapterName);
        }
    }

    /**
     * 选择当前 Activity 派发所用 adapter。
     * <p>
     * P2：按全局配置 spk-delivery.execution.adapter 选（默认 native-ai）。
     * 未来可按 def.getExecutionLocation() / def 额外字段做 per-Activity 路由。
     * 选不到时回退 native-ai，再选不到抛 TASK_ROUTER_NO_LEAD 同类异常。
     */
    private FrameworkAdapter selectAdapter(SpkIpdActivityDefDO def) {
        FrameworkAdapter a = adapterMap.get(defaultAdapterName);
        if (a != null) {
            return a;
        }
        a = adapterMap.get("native-ai");
        if (a != null) {
            return a;
        }
        // 兜底：任一可用 adapter
        if (!adapterMap.isEmpty()) {
            return adapterMap.values().iterator().next();
        }
        throw exception(TASK_ROUTER_NO_LEAD);
    }

    // ===== Lead 选择：capability_tags 全匹配 + agent_kind=lead + 负载最低 =====
    private SpkAgentDefDO selectLead(SpkIpdActivityDefDO def) {
        List<String> caps = parseArray(def.getModelCapabilities());
        if (caps.isEmpty()) {
            // 无能力要求：按 leadAgentCode 精确取
            return def.getLeadAgentCode() != null ? agentDefMapper.selectByCode(def.getLeadAgentCode()) : null;
        }
        List<SpkAgentDefDO> leads = agentDefMapper.selectListByAgentKind(SpkAgentKindEnum.LEAD.getLabel());
        SpkAgentDefDO best = null;
        long bestLoad = Long.MAX_VALUE;
        for (SpkAgentDefDO a : leads) {
            List<String> tags = parseArray(a.getCapabilityTags());
            if (tags.containsAll(caps)) {
                long load = contractMapper.countRunningByLeadAgentId(a.getId());
                if (load < bestLoad) {
                    bestLoad = load;
                    best = a;
                }
            }
        }
        if (best == null) {
            // 兜底：直接按 leadAgentCode 取，不强制能力匹配
            return def.getLeadAgentCode() != null ? agentDefMapper.selectByCode(def.getLeadAgentCode()) : null;
        }
        return best;
    }

    private SpkAgentDefDO selectVerifier(SpkIpdActivityDefDO def) {
        String vt = def.getVerifierType() != null ? def.getVerifierType() : "TR";
        List<SpkAgentDefDO> list = agentDefMapper.selectListByAgentKindAndVerifierType(
                SpkAgentKindEnum.VERIFIER.getLabel(), vt);
        return list.isEmpty() ? null : list.get(0);
    }

    private SpkRunReceiptDO writeRunReceipt(String activityRunId, String contractId, SpkAgentDefDO lead,
                                           SpkAgentDispatchResult dispatch, SpkArtifactManifestDO artifact,
                                           String processInstanceId, SpkIpdActivityDefDO def, String providerName) {
        String runId = "rr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        // provider 取实际选中的 adapter 名（native-ai / omnigent）；model 从 lead 描述字段取
        SpkRunReceiptDO receipt = SpkRunReceiptDO.builder()
                .runId(runId)
                .activityRunId(activityRunId)
                .contractId(contractId)
                .leadAgentId(lead.getId())
                .leadAgentCode(lead.getCode())
                .modelSnapshotId(null)
                .capabilityId(def.getModelCapabilities())
                .provider(providerName)
                .model(lead.getModel())
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .tokenUsage("{}")
                .cost(java.math.BigDecimal.ZERO)
                .latencyMs(0)
                .status("done")
                .artifactUris("[\"" + artifact.getUri() + "\"]")
                .workerLinks("[]")
                .conversationId(dispatch.getConversationId())
                .build();
        runReceiptMapper.insert(receipt);
        // run 证据
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("runId", runId);
        p.put("lead", lead.getCode());
        p.put("artifactId", artifact.getArtifactId());
        p.put("conversationId", dispatch.getConversationId());
        evidenceService.append(activityRunId, processInstanceId,
                SpkEvidenceTypeEnum.RUN.getLabel(), runId, p);
        return receipt;
    }

    private void markContractRunning(SpkTaskContractDO c) {
        c.setStatus(SpkTaskContractStatusEnum.RUNNING.getLabel());
        c.setStartedAt(LocalDateTime.now());
        contractMapper.updateById(c);
    }

    private void markContractDone(SpkTaskContractDO c) {
        c.setStatus(SpkTaskContractStatusEnum.DONE.getLabel());
        c.setFinishedAt(LocalDateTime.now());
        contractMapper.updateById(c);
    }

    private void markContractFailed(SpkTaskContractDO c, String reason) {
        c.setStatus(SpkTaskContractStatusEnum.FAILED.getLabel());
        c.setFinishedAt(LocalDateTime.now());
        c.setFailureReason(reason);
        contractMapper.updateById(c);
    }

    private Map<String, Object> evidencePayload(SpkRouteResult r, SpkIpdActivityDefDO def) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activityId", def.getActivityId());
        m.put("lead", r.getLeadAgentCode());
        m.put("artifactId", r.getArtifactId());
        m.put("runReceiptId", r.getRunReceiptId());
        m.put("verifier", r.getVerifierCode());
        m.put("conclusion", r.getVerificationConclusion());
        m.put("status", r.getStatus());
        return m;
    }

    private Map<String, Object> failPayload(SpkRouteResult r, Exception e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activityRunId", r.getActivityRunId());
        m.put("contractId", r.getContractId());
        m.put("status", "failed");
        m.put("error", truncate(e.getMessage(), 500));
        return m;
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseArray(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Object> list = JsonUtils.parseObject(json, List.class);
            List<String> res = new ArrayList<>();
            for (Object o : list) {
                res.add(String.valueOf(o));
            }
            return res;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    /**
     * 解析 Lead 派发产物为 {document, summary, conclusion}。
     * <ul>
     *   <li>真实模式：{@link cn.iocoder.yudao.module.spkdelivery.service.agent.NativeAiAdapter} 返回
     *       {@code {document, summary, conclusion}} JSON。</li>
     *   <li>fast 桩模式回退：{@code {deliverable, content, summary}} → document=content。</li>
     *   <li>非 JSON：整段当 document。</li>
     * </ul>
     */
    private static Payload parsePayload(String raw) {
        Payload p = new Payload();
        if (raw == null || raw.isBlank()) {
            p.document = "";
            p.summary = "";
            p.conclusion = "FAIL: 无产物";
            return p;
        }
        try {
            JsonNode node = JsonUtils.parseTree(raw);
            JsonNode doc = node.get("document");
            if (doc != null && !doc.isNull()) {
                p.document = doc.asText();
                JsonNode s = node.get("summary");
                p.summary = s == null || s.isNull() ? "" : s.asText();
                JsonNode c = node.get("conclusion");
                p.conclusion = c == null || c.isNull() ? "未提取" : c.asText();
                return p;
            }
            // 兼容 fast 桩：{deliverable, content, summary}
            JsonNode content = node.get("content");
            if (content != null && !content.isNull()) {
                p.document = content.asText();
                JsonNode s = node.get("summary");
                p.summary = s == null || s.isNull() ? "" : s.asText();
                p.conclusion = "stub";
                return p;
            }
        } catch (Exception ignore) {
            // 非 JSON，fallback
        }
        // 非结构化 JSON：按 spk-reporter 的 markdown 报告格式提取 摘要/结论。
        // 报告形如：
        //   # <标题>
        //   ## 摘要
        //   <摘要正文>
        //   ## 关键交付件
        //   ...
        //   ## 结论
        //   PASS|FAIL|需人工评审 — <一句话理由>
        p.document = raw;
        String summary = extractMarkdownSection(raw, "摘要");
        p.summary = (summary != null && !summary.isBlank()) ? summary
                : (raw.length() > 200 ? raw.substring(0, 200) : raw);
        String conclusion = extractMarkdownSection(raw, "结论");
        p.conclusion = (conclusion != null && !conclusion.isBlank()) ? conclusion : "未提取";
        return p;
    }

    /**
     * 从 markdown 文本中提取某个二级标题（## <section>）下的正文，直到下一个二级标题。
     * 大小写不敏感；返回去掉前后空白的多行正文。找不到返回 null。
     */
    private static String extractMarkdownSection(String md, String section) {
        if (md == null || md.isBlank()) {
            return null;
        }
        // 匹配 "## 摘要" 或 "## 摘要：" 起到下一个 "## " 之前。
        // 注意：不用 \b 词边界——中文字符非 \w，"\b" 在中文后不触发会导致整段匹配失败。
        // 改用 \s*[：:]*\s*$ 收束标题行，既兼容中英文/全半角冒号，又能避免 "## 摘要" 误匹配 "## 摘要补充"。
        java.util.regex.Pattern pat = java.util.regex.Pattern.compile(
                "^##\\s+" + java.util.regex.Pattern.quote(section) + "\\s*[：:]*\\s*$\\s*(.*?)(?=^##\\s+|\\z)",
                java.util.regex.Pattern.MULTILINE | java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = pat.matcher(md);
        if (!m.find()) {
            return null;
        }
        return m.group(1).trim();
    }

    private static class Payload {
        String document;
        String summary;
        String conclusion;
    }
}
