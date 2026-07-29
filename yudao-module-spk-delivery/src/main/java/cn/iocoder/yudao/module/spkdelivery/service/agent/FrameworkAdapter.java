package cn.iocoder.yudao.module.spkdelivery.service.agent;

import java.util.List;
import java.util.Map;

/**
 * SPK-OS FrameworkAdapter —— Agent 运行时适配接口
 * <p>
 * 借鉴 Paddock adapters/adapter.ts 的六方法契约，把 agent 派任务/查询/取消/列举/健康检查
 * 与具体 runtime（yudao-module-ai 内核 / Hermes / Codex / Claude Code）解耦。
 * <p>
 * SpkAgentTaskService 经此接口派任务，不与具体 runtime 硬耦合。
 *
 * @author SPK-OS
 */
public interface FrameworkAdapter {

    /**
     * 派发一个 agent 任务
     *
     * @param req 派发请求
     * @return 派发结果（含 taskId、状态）
     */
    SpkAgentDispatchResult dispatchTask(SpkAgentDispatchReq req);

    /**
     * 查询任务状态
     *
     * @param taskId 任务 id
     * @return 状态 running/done/failed/cancelled
     */
    String getTaskStatus(String taskId);

    /**
     * 获取任务产物
     *
     * @param taskId 任务 id
     * @return 产物（JSON 字符串）
     */
    String getTaskResult(String taskId);

    /**
     * 取消任务
     *
     * @param taskId 任务 id
     * @return 是否取消成功
     */
    boolean cancelTask(String taskId);

    /**
     * 列举可用 agent（角色）
     *
     * @return agent 标识与名称列表
     */
    List<Map<String, Object>> listAvailableAgents();

    /**
     * 健康检查
     *
     * @return 是否可用
     */
    boolean healthCheck();

    /**
     * 适配器名称（如 native-ai / hermes / codex / claude-code）
     *
     * @return 名称
     */
    String getName();

}
