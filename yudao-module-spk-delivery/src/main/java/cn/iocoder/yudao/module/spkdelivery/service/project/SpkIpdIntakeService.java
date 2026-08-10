package cn.iocoder.yudao.module.spkdelivery.service.project;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.conversation.AiChatConversationCreateMyReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import cn.iocoder.yudao.module.ai.service.chat.AiChatConversationService;
import cn.iocoder.yudao.module.ai.service.chat.AiChatMessageService;
import cn.iocoder.yudao.module.ai.service.model.AiChatRoleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SPK-OS Cortext-IPD 自然语言发起服务
 * <p>
 * 设计文档 §1.1 G1 要求"从市场机会到 GA 发布一键发起"；用户跨轮反复诉求"在 hermes 说一句话
 * 整个流程就开始运行"。本服务补齐该入口：接收自然语言诉求 → 经 LLM 抽取 projectName/payload
 * → 调 {@link SpkIpdProjectService#start} 发起 IPD 主流程。
 * <p>
 * <b>不复用 fast-mode</b>：fast-mode 跳过真实 LLM（合成产物桩），而 intake 的核心就是把人的
 * 白话诉求解析成结构化字段，必须真实调 LLM。LLM 失败/解析失败一律降级为「整段当 projectName、
 * 原文当 payload」，保证流程仍能发起、不因 LLM 抖动卡死入口。
 *
 * @author SPK-OS
 */
@Slf4j
@Service
@Validated
public class SpkIpdIntakeService {

    /** 系统用户编号（无登录态时代发消息，与 NativeAiAdapter 对齐） */
    @Value("${spk-delivery.agent.system-user-id:1}")
    private Long systemUserId;

    /** 抽取用角色 id（复用 IPD Lead 同一 chat role，保证可执行） */
    @Value("${spk-delivery.intake.role-id:1}")
    private Long intakeRoleId;

    @Resource
    private AiChatMessageService chatMessageService;
    @Resource
    private AiChatConversationService chatConversationService;
    @Resource
    private AiChatRoleService chatRoleService;
    @Resource
    private SpkIpdProjectService projectService;

    private static final String EXTRACTION_SYSTEM_PROMPT =
            "你是 SPK-OS IPD 流程发起助手。把用户的产品诉求解析为结构化字段，输出**严格 JSON**（不要 markdown 代码块、不要多余文字）：\n" +
            "{\"projectName\":\"<=30 字中文项目名\",\"payload\":\"结构化要点（目标用户/核心痛点/关键约束/期望交付，无则留空）\"}\n" +
            "示例：用户说『我想做一个智能家居中控，主打老人语音控制和家庭能源管理』→ " +
            "{\"projectName\":\"智能家居中控\",\"payload\":\"目标用户：老人；核心功能：语音控制、能源管理\"}";

    /**
     * 自然语言发起 IPD 主流程。
     *
     * @param request 诉求原文
     * @param mode    运行模式 test/product（透传给 projectService.start 写入流程变量 spk_mode）；
     *                空/null 归一为 test
     * @return {processInstanceId, businessKey, projectName, payload, intakeMode}
     *         intakeMode=real（LLM 抽取成功）/ fallback（LLM 失败降级）
     */
    public Map<String, Object> intake(String request, String mode) {
        if (request == null || request.isBlank()) {
            throw new IllegalArgumentException("诉求原文不能为空");
        }
        String normMode = (mode == null || mode.isBlank()) ? "test" : mode.trim().toLowerCase();
        String projectName;
        String payload;
        String intakeMode = "real";
        try {
            Map<String, Object> extracted = extractViaLlm(request);
            projectName = strValue(extracted.get("projectName"));
            payload = strValue(extracted.get("payload"));
            if (projectName.isBlank()) {
                // LLM 返回了但没给名字，降级
                projectName = truncate(request, 30);
                payload = request;
                intakeMode = "fallback";
            }
        } catch (Exception e) {
            // 打根因而非包装消息：原日志只显 "LLM 抽取执行异常，降级 fallback" 看不到真实失败原因
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            log.warn("[intake][LLM 抽取失败，降级 fallback：{}]", root.toString());
            projectName = truncate(request, 30);
            payload = request;
            intakeMode = "fallback";
        }
        // 发起流程（businessKey 留空让 projectService 自动生成；start 同步返 businessKey+processInstanceId，
        // type2 触发器后 ~0.06s 到首 receiveTask 即返）。intake 的 LLM 抽取 ≤30s 硬超时兜底，
        // 前端 intake 请求单独设 60s 超时覆盖 30s LLM（approve 仍原生 30s 不动）。
        Map<String, Object> startResult = projectService.start(null, projectName, payload, normMode);
        Map<String, Object> result = new LinkedHashMap<>(startResult);
        result.put("projectName", projectName);
        result.put("payload", payload);
        result.put("rawRequest", truncate(request, 200));
        result.put("intakeMode", intakeMode);
        log.info("[intake][mode={} projectName={} businessKey={} processInstanceId={} 同步发起完成]",
                normMode, projectName, startResult.get("businessKey"), startResult.get("processInstanceId"));
        return result;
    }

    /** 抽取调用最长等待秒数：上游模型不响应时强制降级 fallback，避免入口被 LLM 挂死。 */
    @Value("${spk-delivery.intake.llm-timeout-seconds:30}")
    private long llmTimeoutSeconds;

    /**
     * 调 yudao-module-ai 内核做一次性抽取：建会话 → 发消息 → 解析返回 JSON。
     * 复用 NativeAiAdapter 的 chat 装配模式，但不走 fast-mode。
     * <p>
     * <b>硬超时兜底</b>：用 CompletableFuture + timeout 包住 sendMessage，上游模型不响应时
     * 超时抛 TimeoutException → 由 {@link #intake} catch 降级 fallback，保证流程仍能发起。
     */
    private Map<String, Object> extractViaLlm(String request) {
        // 快速失败守卫：role/model 未配置（PG 两表空）时立即抛，由 intake() 降级 fallback，
        // 避免无模型时 chatMessageService 长时间挂起把入口卡死。
        if (chatRoleService.getChatRole(intakeRoleId) == null) {
            throw new RuntimeException("AI chat role 不存在（id=" + intakeRoleId + "），未配置模型，降级 fallback");
        }
        AiChatConversationCreateMyReqVO createReqVO = new AiChatConversationCreateMyReqVO();
        createReqVO.setRoleId(intakeRoleId);
        Long conversationId = chatConversationService.createChatConversationMy(createReqVO, systemUserId);

        String prompt = EXTRACTION_SYSTEM_PROMPT + "\n\n用户诉求：\n" + request;
        AiChatMessageSendReqVO sendReqVO = new AiChatMessageSendReqVO();
        sendReqVO.setConversationId(conversationId);
        sendReqVO.setContent(prompt);
        sendReqVO.setUseContext(Boolean.FALSE);

        String content;
        // 捕获主线程租户：CompletableFuture.supplyAsync 默认走 ForkJoinPool.commonPool，
        // 该线程池未包装 TTL，TenantContextHolder 不会自动传到子线程 → sendMessage 内部
        // 查 ai_model/ai_api_key（带 tenant 过滤）返回空 → ~14ms 内快速失败降级 fallback。
        // 这里在 lambda 内显式 setTenantId，finally 恢复，保证 LLM 抽取真能命中配置。
        final Long tenantId = TenantContextHolder.getTenantId();
        try {
            AiChatMessageSendRespVO respVO = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> {
                        Long prev = TenantContextHolder.getTenantId();
                        if (tenantId != null) {
                            TenantContextHolder.setTenantId(tenantId);
                        } else {
                            TenantContextHolder.clear();
                        }
                        try {
                            return chatMessageService.sendMessage(sendReqVO, systemUserId);
                        } finally {
                            if (prev != null) {
                                TenantContextHolder.setTenantId(prev);
                            } else {
                                TenantContextHolder.clear();
                            }
                        }
                    })
                    .get(llmTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            content = (respVO != null && respVO.getReceive() != null) ? respVO.getReceive().getContent() : null;
        } catch (java.util.concurrent.TimeoutException te) {
            throw new RuntimeException("LLM 抽取超时（>" + llmTimeoutSeconds + "s），降级 fallback", te);
        } catch (java.util.concurrent.ExecutionException ee) {
            throw new RuntimeException("LLM 抽取执行异常，降级 fallback", ee.getCause() != null ? ee.getCause() : ee);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("LLM 抽取被中断，降级 fallback", ie);
        }
        if (content == null || content.isBlank()) {
            throw new RuntimeException("LLM 返回空");
        }
        // 兼容 LLM 把 JSON 包进 markdown 代码块的情况
        String json = stripCodeFence(content).trim();
        Map<String, Object> parsed = JsonUtils.parseObject(json, Map.class);
        if (parsed == null) {
            throw new RuntimeException("LLM 返回非 JSON：" + truncate(content, 120));
        }
        return parsed;
    }

    private static String stripCodeFence(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }

    private static String strValue(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

}
