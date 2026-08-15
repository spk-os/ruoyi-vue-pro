/**
 * SPK-OS 智能体任务 / 运行负载 API（§7.1 第 3 视图）
 * 后端：SpkAgentTaskController @ /spk/agent-task
 * 数据铁律：聚合无数据时如实返回空数组，前端显式标注"样本不足"，绝不造假
 */
import request from '@/config/axios'

export interface AgentLoadStats {
  agentDefId?: number
  agentName?: string
  agentCode?: string
  running?: number
  succeeded?: number
  failed?: number
  intervened?: number
  totalTokens?: number
  total?: number
  successRate?: number | null
  lastError?: string
  lastActivity?: string
}

/** 智能体运行负载聚合（按 agent_def 维度真实聚合 spk_task_contract） */
export const getLoadStats = () => request.get({ url: '/spk/agent-task/load-stats' })
