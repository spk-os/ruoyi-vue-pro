import request from '@/config/axios'

// SPK-OS 智能体定义（不依赖 openclaw，本地实现）
// 与 Paddock 的差别：Paddock 是云侧 agent 卡片，本系统为本地智能体定义 + 独立编队实体

export interface AgentDefVO {
  id?: number
  name: string
  code: string
  role: string
  sessionKey?: string
  soulContent?: string
  workingMemory?: string
  status?: string
  model?: string
  roleId?: number
  conversationId?: number
  toolsConfig?: string
  config?: string
  runtimeType?: string
  source?: string
  hidden?: number
  lastActivity?: string
  createTime?: Date
  // 高级字段（高级模式才显，通用模式默认）
  agentKind?: string // lead/worker/verifier
  capabilityTags?: string // JSON 数组，如 ["architecture","review"]
  verifierType?: string // TR/RE/SEC，仅 verifier
  isolationLevel?: string // process/container/vm
  mode?: string // local/omnigent（per-agent runtime）
  parentDefId?: number // 继承父智能体 id
  omnigentAgentId?: string // Omnigent 侧 agent-id 映射
}

export interface AgentDefPageReqVO extends PageParam {
  name?: string
  code?: string
  role?: string
  status?: string
  runtimeType?: string
  hidden?: number
}

export interface AgentDefWakeRespVO {
  id: number
  name: string
  conversationId: number
  content: string
  status: string
}

// 分页查询智能体
export const getAgentDefPage = (params: AgentDefPageReqVO) => {
  return request.get({ url: '/spk/agent-def/page', params })
}

// 查询详情
export const getAgentDef = (id: number) => {
  return request.get({ url: '/spk/agent-def/get?id=' + id })
}

// 新增智能体
export const createAgentDef = (data: AgentDefVO) => {
  return request.post({ url: '/spk/agent-def/create', data })
}

// 修改智能体
export const updateAgentDef = (data: AgentDefVO) => {
  return request.put({ url: '/spk/agent-def/update', data })
}

// 删除智能体
export const deleteAgentDef = (id: number) => {
  return request.delete({ url: '/spk/agent-def/delete?id=' + id })
}

// 智能体列表（非隐藏，编队选成员用）
export const getAgentDefList = () => {
  return request.get<AgentDefVO[]>({ url: '/spk/agent-def/list' })
}

// 变更智能体状态（idle/busy/offline/error）
export const changeAgentDefStatus = (id: number, status: string, lastActivity?: string) => {
  const data = { id, status, lastActivity }
  return request.put({ url: '/spk/agent-def/change-status', data })
}

// 隐藏智能体
export const hideAgentDef = (id: number) => {
  return request.post({ url: '/spk/agent-def/hide?id=' + id })
}

// 取消隐藏智能体
export const unhideAgentDef = (id: number) => {
  return request.delete({ url: '/spk/agent-def/hide?id=' + id })
}

// 唤醒智能体（单轮对话，本地实现）
export const wakeAgentDef = (id: number, message: string) => {
  return request.post({ url: '/spk/agent-def/wake', data: { id, message } })
}
