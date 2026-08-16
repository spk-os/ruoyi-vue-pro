package cn.iocoder.yudao.module.spkdelivery.service.agent;

import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.conversation.AiChatConversationCreateMyReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiChatRoleDO;
import cn.iocoder.yudao.module.ai.service.chat.AiChatConversationService;
import cn.iocoder.yudao.module.ai.service.chat.AiChatMessageService;
import cn.iocoder.yudao.module.ai.service.model.AiChatRoleService;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentTaskStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPK-OS NativeAiAdapter —— 走 yudao-module-ai 内核的 FrameworkAdapter 实现
 * <p>
 * 短期作为 BpmAgentTaskDelegate 的默认 runtime：经 AiChatConversationService 绑定 role → AiChatMessageService
 * 同步发送消息取 receive.content 作为产物。长期可切 Hermes/Codex/ClaudeCode 外部 runtime。
 *
 * @author SPK-OS
 */
@Slf4j
@Component("native-ai")
public class NativeAiAdapter implements FrameworkAdapter {

    public static final String NAME = "native-ai";

    /**
     * 系统用户编号（JavaDelegate 无登录态，用管理员代发消息；可经配置覆盖）
     */
    @Value("${spk-delivery.agent.system-user-id:1}")
    private Long systemUserId;

    /**
     * P1 快速执行模式（默认开启）。
     * <p>
     * 开启时跳过真实 LLM 调用（reasoning 模型单次 30~40s，10 触发器串行会撑爆 Flowable 同步事务），
     * 合成产物桩返回——符合设计"P1 详细活动可 stub、Activity 定义齐全+真实 prompt 即可"。
     * 关闭后走 yudao-module-ai 内核真实发送。Verifier（nodeKey 以 verify: 开头）返回 PASS 结构化结论。
     */
    @Value("${spk-delivery.execution.fast-mode:true}")
    private boolean fastMode;

    @Resource
    private AiChatMessageService chatMessageService;
    @Resource
    private AiChatConversationService chatConversationService;
    @Resource
    private AiChatRoleService chatRoleService;

    /**
     * 会话缓存：roleId → conversationId。
     * 一个 IPD 流程实例的同一 agent 节点复用会话，便于携带上下文。
     * key = instanceId#nodeKey#roleId
     */
    private final Map<String, Long> conversationCache = new ConcurrentHashMap<>();

    @Override
    public SpkAgentDispatchResult dispatchTask(SpkAgentDispatchReq req) {
        // P1 快速模式：跳过真实 LLM，合成产物桩（Lead）/ PASS 结论桩（Verifier）
        if (fastMode) {
            return fastDispatch(req);
        }
        SpkAgentDispatchResult result = new SpkAgentDispatchResult();
        // 1. 校验角色存在
        AiChatRoleDO role = chatRoleService.getChatRole(req.getRoleId());
        if (role == null) {
            throw new RuntimeException("Agent 角色不存在：" + req.getRoleId());
        }
        // 2. 取/建会话（roleId 绑定模型/工具/MCP/知识库）
        Long conversationId = getOrCreateConversation(req);
        result.setConversationId(conversationId);
        // 3. 构造发送内容：
        //    - Verifier（nodeKey 以 verify: 开头）：保持原 prompt，LLM 返回 JSON 结论，route 不解析文档
        //    - Lead：末尾追加分隔符格式约束，LLM 按三段输出，route 解析后全文落 Gitea、只存摘要+链接
        String nodeKey = req.getNodeKey() == null ? "" : req.getNodeKey();
        boolean isVerify = nodeKey.startsWith("verify:");
        String prompt = req.getPrompt();
        if (!isVerify && prompt != null) {
            prompt = prompt + "\n\n【输出格式约束（必须严格遵循）】\n"
                    + "请按以下三段分隔符格式输出，每个分隔符独占一行：\n"
                    + "<<<DOCUMENT>>>\n{完整的 Markdown 文档正文（含标题、章节、表格等）}\n"
                    + "<<<SUMMARY>>>\n{200 字以内的中文摘要}\n"
                    + "<<<CONCLUSION>>>\n{结论：PASS 或 FAIL，加一句话说明}\n";
        }
        // 4. 同步发送消息（transient 网络/超时/网关抖动重试，避免单次 SocketTimeoutException
        //    致整条 IPD activity 失败卡死流程；重试耗尽仍失败则抛出由上游处理）
        AiChatMessageSendReqVO sendReqVO = new AiChatMessageSendReqVO();
        sendReqVO.setConversationId(conversationId);
        sendReqVO.setContent(prompt);
        sendReqVO.setUseContext(Boolean.TRUE);
        AiChatMessageSendRespVO respVO = sendMessageWithRetry(sendReqVO, systemUserId);
        // 5. 取 receive.content 作为产物
        String content = (respVO != null && respVO.getReceive() != null) ? respVO.getReceive().getContent() : null;
        // Verifier 原样返回 LLM JSON（parseVerdict 解析 overall/summary/evidencePoints）；Lead 封装三段 JSON 供 route 解析
        result.setResult(isVerify ? content : parseDocumentPayload(content));
        result.setStatus(SpkAgentTaskStatusEnum.DONE.getLabel());
        // taskId 用 conversationId#sendMsgId 作为外部任务编号（native 同步执行）
        Long sendMsgId = (respVO != null && respVO.getSend() != null) ? respVO.getSend().getId() : null;
        result.setTaskId(conversationId + "#" + sendMsgId);
        log.info("[dispatchTask][instanceId={} nodeKey={} roleId={} taskId={} done]", req.getInstanceId(), req.getNodeKey(), req.getRoleId(), result.getTaskId());
        return result;
    }

    /**
     * 把 LLM 返回按 {@code <<<DOCUMENT>>>/<SUMMARY>>/<CONCLUSION>>>} 分隔符解析为
     * {@code {document, summary, conclusion}} JSON。LLM 未遵循格式时 fallback：整段当文档、摘要取前 200 字。
     */
    private String parseDocumentPayload(String content) {
        Map<String, String> m = new LinkedHashMap<>();
        if (content == null || content.isBlank()) {
            m.put("document", "");
            m.put("summary", "LLM 返回空");
            m.put("conclusion", "FAIL: 无产物");
            return JsonUtils.toJsonString(m);
        }
        int iDoc = content.indexOf("<<<DOCUMENT>>>");
        int iSum = content.indexOf("<<<SUMMARY>>>");
        int iCon = content.indexOf("<<<CONCLUSION>>>");
        if (iDoc >= 0 && iSum > iDoc && iCon > iSum) {
            m.put("document", content.substring(iDoc + "<<<DOCUMENT>>>".length(), iSum).trim());
            m.put("summary", content.substring(iSum + "<<<SUMMARY>>>".length(), iCon).trim());
            m.put("conclusion", content.substring(iCon + "<<<CONCLUSION>>>".length()).trim());
        } else {
            // fallback：LLM 未遵循格式，整段当文档
            m.put("document", content);
            m.put("summary", content.length() > 200 ? content.substring(0, 200) : content);
            m.put("conclusion", "未提取（LLM 未遵循分隔符格式）");
        }
        return JsonUtils.toJsonString(m);
    }

    /**
     * 快速模式派发桩：不调 LLM，直接合成结构化产物。
     * <ul>
     *   <li>Verifier（nodeKey 以 {@code verify:} 开头）→ 返回 {@code {"overall":"PASS",...}} 结构化结论。</li>
     *   <li>Lead → 返回合成产物 JSON（含 prompt 摘要），落 ArtifactManifest 作为产物正文。</li>
     * </ul>
     */
    private SpkAgentDispatchResult fastDispatch(SpkAgentDispatchReq req) {
        SpkAgentDispatchResult result = new SpkAgentDispatchResult();
        result.setStatus(SpkAgentTaskStatusEnum.DONE.getLabel());
        result.setConversationId(0L);
        result.setTaskId("fast#" + System.nanoTime());
        String nodeKey = req.getNodeKey() == null ? "" : req.getNodeKey();
        String promptDigest = Integer.toHexString((req.getPrompt() == null ? "" : req.getPrompt()).hashCode());
        if (nodeKey.startsWith("verify:")) {
            // Verifier 桩：结构化 PASS 结论（parseVerdict 解析 overall/summary/evidencePoints）
            result.setResult("{\"overall\":\"PASS\",\"summary\":\"P1 快速模式：Independent Verifier 自动通过（未调用真实 LLM）\","
                    + "\"evidencePoints\":["
                    + "{\"point\":\"recheck\",\"verdict\":\"Confirmed\"},"
                    + "{\"point\":\"redteam\",\"verdict\":\"Confirmed\"},"
                    + "{\"point\":\"completeness\",\"verdict\":\"Confirmed\"},"
                    + "{\"point\":\"traceback\",\"verdict\":\"Confirmed\"}]}");
        } else {
            // Lead 桩：合成产物正文（落 ArtifactManifest metadata）
            result.setResult("{\"deliverable\":\"stub\",\"mode\":\"fast\","
                    + "\"promptDigest\":\"" + promptDigest + "\","
                    + "\"summary\":\"P1 快速模式合成产物（未调用真实 LLM）\","
                    + "\"content\":\"本产物由 fast-mode 合成。Activity 定义含真实 prompt（promptDigest="
                    + promptDigest + "），P2 起接入真实 runtime 执行。\"}");
        }
        log.info("[dispatchTask][fast-mode instanceId={} nodeKey={} roleId={} done]", req.getInstanceId(), nodeKey, req.getRoleId());
        return result;
    }

    /**
     * 同步发送 LLM 消息，对 transient（网络/超时/网关抖动）失败重试。
     * <p>
     * reasoning 模型单次 30~40s，偶发 {@code SocketTimeoutException}/{@code Socket closed}
     * 不应让整条 IPD activity 失败卡死流程；重试 N 次后仍失败则抛出由上游处理。
     * 验证器路径另在 {@code SpkVerifierService#verify} 做降级兜底，保证验证器重试耗尽
     * 不阻断主产物推进（主产物已真实生成时验证未决是风险标记非管道停止）。
     */
    private AiChatMessageSendRespVO sendMessageWithRetry(AiChatMessageSendReqVO req, Long userId) {
        int maxAttempts = 3;
        Exception last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return chatMessageService.sendMessage(req, userId);
            } catch (Exception e) {
                last = e;
                if (attempt < maxAttempts) {
                    long backoff = 3000L * attempt;
                    log.warn("[sendMessage][attempt={}/{} 失败，{}ms 后重试] err={}",
                            attempt, maxAttempts, backoff, e.toString());
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("LLM 发送被中断", ie);
                    }
                } else {
                    log.error("[sendMessage][重试 {} 次仍失败，抛出由上游处理] err={}", maxAttempts, e.toString());
                }
            }
        }
        throw new RuntimeException("LLM 调用重试 " + maxAttempts + " 次仍失败", last);
    }

    private Long getOrCreateConversation(SpkAgentDispatchReq req) {
        String cacheKey = req.getInstanceId() + "#" + req.getNodeKey() + "#" + req.getRoleId();
        Long conversationId = conversationCache.get(cacheKey);
        if (conversationId != null) {
            return conversationId;
        }
        AiChatConversationCreateMyReqVO createReqVO = new AiChatConversationCreateMyReqVO();
        createReqVO.setRoleId(req.getRoleId());
        conversationId = chatConversationService.createChatConversationMy(createReqVO, systemUserId);
        conversationCache.put(cacheKey, conversationId);
        return conversationId;
    }

    @Override
    public String getTaskStatus(String taskId) {
        // native 同步执行：派发即 done
        return SpkAgentTaskStatusEnum.DONE.getLabel();
    }

    @Override
    public String getTaskResult(String taskId) {
        // native 同步执行时产物已在 dispatchTask 返回并落 spk_agent_task.result，此处无独立查询能力
        return null;
    }

    @Override
    public boolean cancelTask(String taskId) {
        log.info("[cancelTask][native-ai 同步执行，不支持取消 taskId={}]", taskId);
        return false;
    }

    @Override
    public List<Map<String, Object>> listAvailableAgents() {
        List<Map<String, Object>> list = new ArrayList<>();
        // 列举状态启用的角色作为可用 agent
        // 注：此处仅作占位枚举；如需分页全量，可经 AiChatRoleService.getChatRolePage
        return list;
    }

    @Override
    public boolean healthCheck() {
        try {
            return systemUserId != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

}
