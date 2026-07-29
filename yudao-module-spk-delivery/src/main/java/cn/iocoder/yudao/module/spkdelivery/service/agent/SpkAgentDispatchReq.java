package cn.iocoder.yudao.module.spkdelivery.service.agent;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * SPK-OS Agent 任务派发请求
 * <p>
 * 经 FrameworkAdapter 派发，与具体 runtime 解耦。
 *
 * @author SPK-OS
 */
@Data
@Accessors(chain = true)
public class SpkAgentDispatchReq {

    /**
     * agent 角色 id（对应 yudao AiChatRoleDO，或外部 runtime 的 agent 标识）
     */
    private Long roleId;
    /**
     * 派发 prompt
     */
    private String prompt;
    /**
     * BPM 流程实例 id（用于回溯关联）
     */
    private String instanceId;
    /**
     * BPM 节点 key
     */
    private String nodeKey;
    /**
     * 紧随的 receiveTask key（用于回调 triggerTask 推进）
     */
    private String receiveTaskKey;
    /**
     * TTL 过期时间（毫秒时间戳，0 表示无）
     */
    private Long ttlExpireTime;

}
