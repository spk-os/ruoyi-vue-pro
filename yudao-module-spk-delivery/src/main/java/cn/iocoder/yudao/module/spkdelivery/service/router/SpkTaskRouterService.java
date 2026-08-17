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
import cn.iocoder.yudao.module.spkdelivery.service.agentdef.SpkAgentDefService;
import cn.iocoder.yudao.module.spkdelivery.service.artifact.SpkArtifactService;
import cn.iocoder.yudao.module.spkdelivery.service.context.SpkContextBuilderService;
import cn.iocoder.yudao.module.spkdelivery.service.delivery.DeliveryPathResolver;
import cn.iocoder.yudao.module.spkdelivery.service.delivery.FlowStateWriter;
import cn.iocoder.yudao.module.spkdelivery.service.evidence.SpkEvidenceService;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkGiteaIntegrationService;
import cn.iocoder.yudao.module.spkdelivery.service.integration.SpkPlaneIntegrationService;
import cn.iocoder.yudao.module.spkdelivery.service.modelcapability.SpkModelCapabilityRegistry;
import cn.iocoder.yudao.module.spkdelivery.service.verifier.SpkVerifierService;
import cn.iocoder.yudao.module.spkdelivery.framework.monitoring.SpkIpdMetrics;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
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
    @Resource
    private SpkPlaneIntegrationService planeService;
    @Resource
    private RuntimeService runtimeService;
    @Autowired
    private List<FrameworkAdapter> adapterList;
    /** 适配器名 → 适配器实例（按 FrameworkAdapter.getName() 索引；PostConstruct 装配） */
    private final Map<String, FrameworkAdapter> adapterMap = new java.util.concurrent.ConcurrentHashMap<>();
    @Value("${spk-delivery.execution.adapter:native-ai}")
    private String defaultAdapterName;
    @Resource
    private SpkAgentDefService agentDefService;
    @Resource
    private SpkIpdMetrics metrics;
    @Resource
    private FlowStateWriter flowStateWriter;
    @Resource
    private DeliveryPathResolver deliveryPathResolver;

    /**
     * 路由 + 执行一次 Activity（铁律：一 Activity 一 Lead，三件套齐全才记 done）。
     */
    public SpkRouteResult route(String activityId, String activityVersion,
                                String processInstanceId, String taskId, String businessKey,
                                String nodeKey, String receiveTaskKey, List<String> inputRefs) {
        // 1. 加载 Activity 定义
        SpkIpdActivityDefDO def = activityDefMapper.selectByActivityIdAndVersion(activityId,
                activityVersion != null ? activityVersion : "1.0.0");
        if (def == null) {
            throw exception(IPD_ACTIVITY_NOT_EXISTS);
        }
        if (!"active".equals(def.getStatus())) {
            throw exception(IPD_ACTIVITY_DISABLED);
        }
        // 1.1 读运行模式（test/product）并按模式装配派发 prompt：
        //   product → 真实 prompt（剥离 DB 里可能残留的【测试场景】轻量化后缀，让 agent 做完整真实交付）；
        //   test → 真实 prompt + 轻量化后缀（≤400字、不分发子智能体，控成本/时延）。
        //   铁律：prompt 必须随模式分叉——否则 product 仍拿 DB 里的测试桩 prompt 产浅报告，与 test 无区别。
        String mode = readMode(processInstanceId);
        boolean product = "product".equalsIgnoreCase(mode);
        String prompt = buildPrompt(def.getPromptTemplate(), product);
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
                .receiveTaskKey(receiveTaskKey)
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
                .retryPolicy("{\"max_attempts\":3}")
                .prompt(prompt)
                .status(SpkTaskContractStatusEnum.QUEUED.getLabel())
                .queuedAt(LocalDateTime.now())
                .fencingToken(0L)
                .attemptNo(0)
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
            // 角色继承：对 lead 调 resolveEffective 合并父链（capabilityTags 并集、soulContent/model/mode 子覆盖父）。
            // 注：selectLead 的能力匹配仍用原 def（避免继承改变选 lead 结果）；effective 仅用于派发参数。
            SpkAgentDefDO effectiveLead = agentDefService.resolveEffective(lead.getId());
            if (effectiveLead == null) {
                effectiveLead = lead;
            }
            SpkAgentDispatchReq req = new SpkAgentDispatchReq()
                    .setRoleId(effectiveLead.getRoleId() != null ? effectiveLead.getRoleId() : lead.getRoleId())
                    .setPrompt(prompt)
                    .setInstanceId(processInstanceId)
                    .setNodeKey(nodeKey)
                    // per-agent 路由上下文（OmnigentAdapter 据 leadDefId 解析 omnigentAgentId、按 activityRunId 隔离 workspace）
                    .setLeadCode(lead.getCode())
                    .setLeadDefId(lead.getId())
                    .setActivityRunId(activityRunId)
                    .setMode(effectiveLead.getMode())
                    .setOmnigentAgentId(effectiveLead.getOmnigentAgentId())
                    .setStage(def.getStage())
                    .setOutputArtifactType(def.getOutputArtifactType());
            // G：节点 skill 绑定注入（Phase1 最小闭环——adapter 前置 skill 指令块进 prompt）
            String skillName = resolveSkill(def);
            if (skillName != null) {
                req.setSkillName(skillName);
                req.setSkillPath("/root/.claude/skills/" + skillName + "/SKILL.md");
            }
            FrameworkAdapter adapter = selectAdapter(def, effectiveLead);
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
            // E：产物全文镜像到 <root>/asset/<stage>/<artifactId>.md（DB artifact 之外的可还原本地副本，失败降级）
            try {
                String root = deliveryPathResolver.resolveProjectRootByBusinessKey(processInstanceId, businessKey);
                flowStateWriter.mirrorArtifact(root, def.getStage(), artifact.getArtifactId(), payload.document);
            } catch (Exception me) {
                log.warn("[route][activityRunId={} 镜像产物失败降级：{}]", activityRunId, truncate(me.getMessage(), 200));
            }
            // 9. 长文档提交 Gitea，失败不阻断 Activity done（agentResult 存 giteaUrl:null + error）
            //    test 模式：commit 到 main（既有路径）。
            //    product 模式：建 concept 分支 + commit 到分支 + 开 PR + Plane 录入需求（见 doProductDelivery），
            //    不重复 commit main 以免 PR 为空。每个交付动作独立 try/catch，失败降级不阻断 Activity done。
            //    mode/product 在方法顶部已读（供 prompt 装配与本处交付分支共用，触发器线程只读安全）。
            String giteaUrl = null;
            String giteaError = null;
            try {
                if (!product && payload.document != null && !payload.document.isBlank()) {
                    String path = "docs/" + def.getStage() + "/" + activityRunId + ".md";
                    giteaUrl = giteaService.createFile(path, payload.document, "main",
                            "docs(ipd): " + def.getName());
                }
            } catch (Exception ge) {
                giteaError = truncate(ge.getMessage(), 200);
                log.warn("[route][activityRunId={} gitea createFile 失败：{}]", activityRunId, giteaError);
            }
            // 9.1 product 模式 concept 阶段真实交付动作（建 Gitea 分支/PR + Plane 需求）
            Map<String, Object> delivery = null;
            if (product) {
                delivery = doProductDelivery(def, activityRunId, processInstanceId, payload);
                Object docUrl = delivery.get("giteaDocUrl");
                if (docUrl instanceof String s && !s.isBlank()) {
                    giteaUrl = s;
                }
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
            if (delivery != null) {
                agentResultObj.put("delivery", delivery);
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
            // E：状态追加写 .flow/（可还原全流程，失败降级不阻断）
            try {
                Map<String, Object> state = new LinkedHashMap<>();
                state.put("stage", def.getStage());
                state.put("activityId", activityId);
                state.put("activityRunId", activityRunId);
                state.put("contractId", contractId);
                state.put("artifactId", artifact.getArtifactId());
                state.put("evidenceRunId", result.getRunReceiptId());
                state.put("verdict", result.getVerificationConclusion());
                state.put("status", result.getStatus());
                state.put("operator", lead.getCode());
                state.put("timestamp", System.currentTimeMillis());
                flowStateWriter.appendState(processInstanceId, businessKey, state);
            } catch (Exception se) {
                log.warn("[route][activityRunId={} 写 .flow 状态失败降级：{}]",
                        activityRunId, truncate(se.getMessage(), 200));
            }
            // 10. 总证据
            evidenceService.append(activityRunId, processInstanceId,
                    SpkEvidenceTypeEnum.RUN.getLabel(), receipt.getRunId(),
                    evidencePayload(result, def));
            log.info("[route][activityRunId={} activityId={} lead={} verdict={} done]",
                    activityRunId, activityId, lead.getCode(), result.getVerificationConclusion());
            result.setStatus(SpkTaskContractStatusEnum.DONE.getLabel());
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
     * 选择当前 Activity 派发所用 adapter（per-agent 路由核心）。
     * <p>
     * "用哪个 runtime"是 agent 的属性（lead.mode），不是 activity 的属性：
     * <ul>
     *   <li>lead.mode=omnigent → 走 OmnigentAdapter（真实云沙箱 + 多智能体 fanout + 会话持久化）</li>
     *   <li>lead.mode=local（或空）→ 走 NativeAiAdapter（本地 yudao-module-ai 内核 LLM）</li>
     * </ul>
     * 与 def.executionLocation（系统执行 vs agent 内部执行）正交：executionLocation 写 contract.executionMode，
     * mode 决定 adapter。选不到指定 adapter 时回退 defaultAdapterName → native-ai → 任一可用。
     * mode=omnigent 但 omnigent adapter 不在线（:6767 不监听）由 OmnigentAdapter 内部捕获并降级，此处不拦截。
     */
    private FrameworkAdapter selectAdapter(SpkIpdActivityDefDO def, SpkAgentDefDO lead) {
        // per-agent 按 lead.mode 选 adapter；lead 无 mode 回退全局 defaultAdapterName
        String preferred = (lead != null && lead.getMode() != null && !lead.getMode().isBlank())
                ? lead.getMode() : defaultAdapterName;
        String adapterName = "omnigent".equalsIgnoreCase(preferred) ? "omnigent"
                : "local".equalsIgnoreCase(preferred) ? "native-ai" : defaultAdapterName;
        FrameworkAdapter a = adapterMap.get(adapterName);
        if (a != null) {
            return a;
        }
        a = adapterMap.get(defaultAdapterName);
        if (a != null) {
            log.warn("[selectAdapter][preferred={} 不存在，回退默认 {}]", adapterName, defaultAdapterName);
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

    /**
     * 只读流程变量 spk_mode（test/product）。触发器线程内 getVariable 只读不持写锁，
     * 遵守 [[flowable-sync-trigger-deadlock]] 互锁铁律（仅禁 setVariables）。读取失败/空归一为 test。
     */
    private String readMode(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return "test";
        }
        try {
            Object m = runtimeService.getVariable(processInstanceId, "spk_mode");
            if (m == null) {
                return "test";
            }
            String s = m.toString().trim().toLowerCase();
            return s.isBlank() ? "test" : s;
        } catch (Exception e) {
            log.warn("[readMode][processInstanceId={} 读取 spk_mode 失败，归 test：{}]",
                    processInstanceId, truncate(e.getMessage(), 200));
            return "test";
        }
    }

    /** test 模式附加的轻量化指令（控成本/时延）：≤400 字、不分发子智能体、直接作答。 */
    private static final String TEST_PROMPT_OVERRIDE =
            "\n\n【测试场景】请直接给出简洁快速的简要报告（中文，不超过 400 字，3-5 段，结构清晰）。"
            + "不要分发或调用子智能体/伙伴，不要异步派发，直接作答即可。";

    /**
     * 匹配 DB prompt_template 里可能残留的【测试场景】…直接作答即可。轻量化后缀
     * （旧版 init.sql 曾把该后缀烘入正文，导致 product 模式仍走测试桩）。运行时剥离，恢复真实 prompt。
     * 非贪婪匹配到首个「直接作答即可。」收尾，不会误伤正文（正文不含该收束句）。
     */
    private static final java.util.regex.Pattern TEST_OVERRIDE_PATTERN = java.util.regex.Pattern.compile(
            "\\s*【测试场景】[\\s\\S]*?直接作答即可。", java.util.regex.Pattern.DOTALL);

    /**
     * 按运行模式装配派发 prompt。
     * <p>
     * DB 的 prompt_template 在旧版 init.sql 里曾把【测试场景】轻量化后缀烘入正文（导致 product 模式
     * 仍拿测试桩 prompt、产出浅报告，与 test 无区别）。这里先剥离该残留，再按模式决定是否追加：
     * <ul>
     *   <li>product：真实 prompt，agent 做完整交付（WBS/甘特/架构… 详尽产物）。</li>
     *   <li>test：真实 prompt + 轻量化后缀（≤400 字、不分发），快速冒烟。</li>
     * </ul>
     */
    private String buildPrompt(String template, boolean product) {
        String base = template == null ? "" : TEST_OVERRIDE_PATTERN.matcher(template).replaceAll("");
        return product ? base : base + TEST_PROMPT_OVERRIDE;
    }

    /**
     * product 模式 concept 阶段真实交付动作：建 Gitea concept 分支 → 报告文档 commit 到该分支 →
     * 开 PR（head=concept/{runId} base=main）→ Plane 录入需求清单。每个动作独立 try/catch，
     * 失败记 *Error 降级、不阻断 Activity done（同既有 gitea createFile 降级模式）。
     * <p>
     * 不重复 commit main：test 模式已走 main 路径，product 走分支+PR，避免 PR diff 为空被 Gitea 拒。
     */
    private Map<String, Object> doProductDelivery(SpkIpdActivityDefDO def, String activityRunId,
                                                   String processInstanceId, Payload payload) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("mode", "product");
        d.put("stage", def.getStage());
        String branch = "concept/" + activityRunId;
        // 1) 建 concept 分支（Gitea 仓库须先有 main 与初始 commit，见 [[plane-gitea-integration-contract]]）
        try {
            giteaService.createBranch(branch, "main");
            d.put("branch", branch);
        } catch (Exception e) {
            d.put("branchError", truncate(e.getMessage(), 200));
            log.warn("[doProductDelivery][activityRunId={} 建 branch={} 失败：{}]",
                    activityRunId, branch, truncate(e.getMessage(), 200));
        }
        // 2) 报告文档 commit 到该分支
        try {
            if (payload.document != null && !payload.document.isBlank()) {
                String path = "docs/" + def.getStage() + "/" + activityRunId + ".md";
                String docUrl = giteaService.createFile(path, payload.document, branch,
                        "docs(ipd-product): " + def.getName());
                d.put("giteaDocUrl", docUrl);
            }
        } catch (Exception e) {
            d.put("docError", truncate(e.getMessage(), 200));
            log.warn("[doProductDelivery][activityRunId={} commit doc 失败：{}]",
                    activityRunId, truncate(e.getMessage(), 200));
        }
        // 3) 开 PR head=concept/{runId} base=main
        try {
            String title = "feat(" + def.getStage() + "): " + def.getName() + " " + activityRunId;
            String body = (payload.summary != null && !payload.summary.isBlank())
                    ? payload.summary
                    : ("IPD " + def.getStage() + " 产物 " + activityRunId);
            String prUrl = giteaService.createPR(branch, "main", title, body);
            d.put("prUrl", prUrl);
        } catch (Exception e) {
            d.put("prError", truncate(e.getMessage(), 200));
            log.warn("[doProductDelivery][activityRunId={} 开 PR 失败：{}]",
                    activityRunId, truncate(e.getMessage(), 200));
        }
        // 4) Plane 录入需求（concept 阶段需求洞察报告末尾 ```requirements JSON 块；projectId 配置就绪时）
        try {
            List<SpkPlaneIntegrationService.PlaneRequirementReq> reqs = parseRequirements(payload.document);
            if (!reqs.isEmpty()) {
                Map<String, String> issueMap = planeService.importRequirements(reqs);
                d.put("planeIssues", issueMap);
            } else {
                d.put("planeSkipped", "无 requirements 块或为空");
            }
        } catch (Exception e) {
            d.put("planeError", truncate(e.getMessage(), 200));
            log.warn("[doProductDelivery][activityRunId={} Plane 录入失败：{}]",
                    activityRunId, truncate(e.getMessage(), 200));
        }
        // 记 delivery 证据（失败不阻断）
        try {
            evidenceService.append(activityRunId, processInstanceId,
                    SpkEvidenceTypeEnum.RUN.getLabel(), activityRunId, d);
        } catch (Exception e) {
            log.warn("[doProductDelivery][activityRunId={} 记 delivery 证据失败：{}]",
                    activityRunId, truncate(e.getMessage(), 200));
        }
        log.info("[doProductDelivery][activityRunId={} stage={} product 交付完成 keys={}]",
                activityRunId, def.getStage(), d.keySet());
        return d;
    }

    /**
     * 从 Lead 产物 markdown 中提取 ```requirements 代码块的 JSON 需求清单，
     * 反序列化为 {@link SpkPlaneIntegrationService.PlaneRequirementReq}（level 默认 IR）。
     * 解析失败 / 无块 / 非数组一律返回空列表（降级，不阻断 PR/Activity done）。
     * 约定：concept 阶段需求洞察 Activity prompt 末尾要求输出该块（见 spk_ipd_activity_def_init.sql）。
     */
    private static List<SpkPlaneIntegrationService.PlaneRequirementReq> parseRequirements(String document) {
        List<SpkPlaneIntegrationService.PlaneRequirementReq> res = new ArrayList<>();
        if (document == null || document.isBlank()) {
            return res;
        }
        java.util.regex.Pattern pat = java.util.regex.Pattern.compile(
                "```requirements\\s*([\\s\\S]*?)```", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = pat.matcher(document);
        if (!m.find()) {
            return res;
        }
        String json = m.group(1).trim();
        JsonNode arr;
        try {
            arr = JsonUtils.parseTree(json);
        } catch (Exception e) {
            return res;
        }
        if (arr == null || !arr.isArray()) {
            return res;
        }
        for (JsonNode n : arr) {
            String name = n.has("name") ? n.get("name").asText() : null;
            if (name == null || name.isBlank()) {
                continue;
            }
            String key = n.has("key") && !n.get("key").isNull() ? n.get("key").asText() : null;
            String level = n.has("level") && !n.get("level").isNull() ? n.get("level").asText() : "IR";
            String desc = n.has("description") && !n.get("description").isNull() ? n.get("description").asText() : null;
            res.add(new SpkPlaneIntegrationService.PlaneRequirementReq(key, level, name, desc, null));
        }
        return res;
    }

    private static class Payload {
        String document;
        String summary;
        String conclusion;
    }

    /**
     * stage → 默认 skill 回退映射（对齐设计文档 §7 / DeliveryPathResolver.DEFAULT_SKILL_BINDINGS）。
     * def.skills 为空时按 stage 取，保证每个节点都有 skill 可执行。
     */
    private static final java.util.Map<String, String> STAGE_SKILL_FALLBACK = java.util.Map.of(
            "concept", "spk-ipd-concept",
            "plan", "spk-ipd-plan",
            "develop", "spk-ipd-develop",
            "qualify", "spk-ipd-verify",
            "launch", "spk-ipd-launch",
            "lifecycle", "spk-ipd-tr-gate");

    /**
     * 解析节点绑定 skill：优先 activity_def.skills（JSON 数组首元素，流程配置页可改），
     * 空则按 stage 回退常量映射。返回 skill 名（如 spk-ipd-concept）或 null。
     */
    private String resolveSkill(SpkIpdActivityDefDO def) {
        if (def.getSkills() != null && !def.getSkills().isBlank()) {
            List<String> skills = parseArray(def.getSkills());
            if (!skills.isEmpty()) {
                return skills.get(0);
            }
        }
        return def.getStage() != null ? STAGE_SKILL_FALLBACK.get(def.getStage()) : null;
    }
}
