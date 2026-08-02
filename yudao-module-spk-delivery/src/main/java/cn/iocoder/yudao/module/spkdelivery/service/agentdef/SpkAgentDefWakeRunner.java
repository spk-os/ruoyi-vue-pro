package cn.iocoder.yudao.module.spkdelivery.service.agentdef;

import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.conversation.AiChatConversationCreateMyReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiChatRoleDO;
import cn.iocoder.yudao.module.ai.service.chat.AiChatConversationService;
import cn.iocoder.yudao.module.ai.service.chat.AiChatMessageService;
import cn.iocoder.yudao.module.ai.service.model.AiChatRoleService;
import cn.iocoder.yudao.module.spkdelivery.dal.dataobject.agentdef.SpkAgentDefDO;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentDefStatusEnum;
import cn.iocoder.yudao.module.spkdelivery.enums.SpkAgentRuntimeTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * SPK-OS 智能体唤醒运行器（本地实现，不依赖 openclaw）
 *
 * <p>对标 Paddock {@code POST /api/agents/[id]/wake}。Paddock 经 openclaw gateway 发会话消息；
 * 本系统直接走 yudao-module-ai 内核：role → conversation（首次创建并持久化到 agent）→ sendMessage，
 * 取 receive.content 作为回复。复用 {@link cn.iocoder.yudao.module.spkdelivery.service.agent.NativeAiAdapter}
 * 的同款调用链，但面向"智能体定义"而非 BPM 任务实例。
 *
 * @author SPK-OS
 */
@Slf4j
@Component
public class SpkAgentDefWakeRunner {

    @Value("${spk-delivery.agent.system-user-id:1}")
    private Long systemUserId;

    @Resource
    private AiChatMessageService chatMessageService;
    @Resource
    private AiChatConversationService chatConversationService;
    @Resource
    private AiChatRoleService chatRoleService;

    /**
     * 单轮对话唤醒
     *
     * @param agent  智能体定义（需已校验）
     * @param message 用户消息
     * @return 回复内容
     */
    public WakeResult wake(SpkAgentDefDO agent, String message) {
        // 1. 运行时类型校验：仅 native 支持本地唤醒
        if (!SpkAgentRuntimeTypeEnum.supportsWake(agent.getRuntimeType())) {
            throw new IllegalStateException("运行时类型[" + agent.getRuntimeType() + "]不支持本地唤醒");
        }
        // 2. 角色校验
        if (agent.getRoleId() == null) {
            throw new IllegalStateException("智能体未关联 AI 角色，无法唤醒");
        }
        AiChatRoleDO role = chatRoleService.getChatRole(agent.getRoleId());
        if (role == null) {
            throw new IllegalStateException("AI 角色不存在：" + agent.getRoleId());
        }
        // 3. 取/建会话（首次唤醒创建并持久化到 agent.conversationId，之后复用携带上下文）
        Long conversationId = agent.getConversationId();
        if (conversationId == null) {
            AiChatConversationCreateMyReqVO createReqVO = new AiChatConversationCreateMyReqVO();
            createReqVO.setRoleId(agent.getRoleId());
            conversationId = chatConversationService.createChatConversationMy(createReqVO, systemUserId);
        }
        // 4. 同步发送消息
        AiChatMessageSendReqVO sendReqVO = new AiChatMessageSendReqVO();
        sendReqVO.setConversationId(conversationId);
        sendReqVO.setContent(message);
        sendReqVO.setUseContext(Boolean.TRUE);
        AiChatMessageSendRespVO respVO = chatMessageService.sendMessage(sendReqVO, systemUserId);
        String content = (respVO != null && respVO.getReceive() != null) ? respVO.getReceive().getContent() : null;
        log.info("[wake][agentId={} name={} conversationId={} done]", agent.getId(), agent.getName(), conversationId);
        return new WakeResult(conversationId, content, SpkAgentDefStatusEnum.IDLE.getLabel());
    }

    /**
     * 标记忙碌与心跳（唤醒前后调用）
     */
    public SpkAgentDefDO touchBusy(SpkAgentDefDO agent, String activity) {
        agent.setStatus(SpkAgentDefStatusEnum.BUSY.getLabel());
        agent.setLastSeen(LocalDateTime.now());
        agent.setLastActivity(activity);
        return agent;
    }

    public SpkAgentDefDO touchIdle(SpkAgentDefDO agent, String activity) {
        agent.setStatus(SpkAgentDefStatusEnum.IDLE.getLabel());
        agent.setLastSeen(LocalDateTime.now());
        agent.setLastActivity(activity);
        return agent;
    }

    public record WakeResult(Long conversationId, String content, String status) {
    }

}
