package cn.iocoder.yudao.module.spkdelivery.service.agent;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * SPK-OS Agent 任务派发结果
 *
 * @author SPK-OS
 */
@Data
@Accessors(chain = true)
public class SpkAgentDispatchResult {

    /**
     * 外部 runtime 任务 id
     */
    private String taskId;
    /**
     * 会话 id（yudao AiChatConversation）
     */
    private Long conversationId;
    /**
     * 产物（JSON，同步执行时可能立即返回）
     */
    private String result;
    /**
     * 状态 running/done/failed
     */
    private String status;

}
