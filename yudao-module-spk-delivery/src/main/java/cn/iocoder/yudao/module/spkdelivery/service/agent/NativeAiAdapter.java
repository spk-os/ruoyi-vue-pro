package cn.iocoder.yudao.module.spkdelivery.service.agent;

import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.conversation.AiChatConversationCreateMyReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiChatRoleDO;
import cn.iocoder.yudao.module.ai.service.chat.AiChatConversationService;
import cn.iocoder.yudao.module.ai.service.chat.AiChatMessageService;
import cn.iocoder.yudao.module.ai.service.model.AiChatRoleService;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentTaskStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
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
@Component
public class NativeAiAdapter implements FrameworkAdapter {

    public static final String NAME = "native-ai";

    /**
     * 系统用户编号（JavaDelegate 无登录态，用管理员代发消息；可经配置覆盖）
     */
    @Value("${spk-delivery.agent.system-user-id:1}")
    private Long systemUserId;

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
        SpkAgentDispatchResult result = new SpkAgentDispatchResult();
        // 1. 校验角色存在
        AiChatRoleDO role = chatRoleService.getChatRole(req.getRoleId());
        if (role == null) {
            throw new RuntimeException("Agent 角色不存在：" + req.getRoleId());
        }
        // 2. 取/建会话（roleId 绑定模型/工具/MCP/知识库）
        Long conversationId = getOrCreateConversation(req);
        result.setConversationId(conversationId);
        // 3. 同步发送消息
        AiChatMessageSendReqVO sendReqVO = new AiChatMessageSendReqVO();
        sendReqVO.setConversationId(conversationId);
        sendReqVO.setContent(req.getPrompt());
        sendReqVO.setUseContext(Boolean.TRUE);
        AiChatMessageSendRespVO respVO = chatMessageService.sendMessage(sendReqVO, systemUserId);
        // 4. 取 receive.content 作为产物
        String content = (respVO != null && respVO.getReceive() != null) ? respVO.getReceive().getContent() : null;
        result.setResult(content);
        result.setStatus(SpkAgentTaskStatusEnum.DONE.getLabel());
        // taskId 用 conversationId#sendMsgId 作为外部任务编号（native 同步执行）
        Long sendMsgId = (respVO != null && respVO.getSend() != null) ? respVO.getSend().getId() : null;
        result.setTaskId(conversationId + "#" + sendMsgId);
        log.info("[dispatchTask][instanceId={} nodeKey={} roleId={} taskId={} done]", req.getInstanceId(), req.getNodeKey(), req.getRoleId(), result.getTaskId());
        return result;
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
