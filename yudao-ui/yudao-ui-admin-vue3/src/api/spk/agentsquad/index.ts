import request from '@/config/axios'

// SPK-OS 智能体编队（独立编队实体：头表 + 成员表）
// 与 Paddock 的差别：Paddock 的 squad 仅是 agent 卡片列表，本系统提供独立编队 + 成员顺序 + 串行唤醒

export interface AgentSquadVO {
  id?: number
  name: string
  code: string
  description?: string
  status?: string
  config?: string
  memberCount?: number
  createTime?: Date
}

export interface AgentSquadPageReqVO extends PageParam {
  name?: string
  code?: string
  status?: string
}

export interface AgentSquadMemberRespVO {
  id: number
  squadId: number
  agentId: number
  role?: string
  sortOrder?: number
  agentName?: string
  agentCode?: string
  agentRole?: string
  agentStatus?: string
  agentRuntimeType?: string
}

export interface AgentSquadMemberReqVO {
  id?: number
  squadId: number
  agentId: number
  role?: string
  sortOrder?: number
}

export interface SquadMemberWake {
  agentId: number
  agentName?: string
  conversationId?: number
  content?: string
  status?: string
  error?: string
}

export interface AgentSquadWakeRespVO {
  id: number
  name: string
  results: SquadMemberWake[]
}

// 分页查询编队
export const getAgentSquadPage = (params: AgentSquadPageReqVO) => {
  return request.get({ url: '/spk/agent-squad/page', params })
}

// 查询编队详情
export const getAgentSquad = (id: number) => {
  return request.get({ url: '/spk/agent-squad/get?id=' + id })
}

// 新增编队
export const createAgentSquad = (data: AgentSquadVO) => {
  return request.post({ url: '/spk/agent-squad/create', data })
}

// 修改编队
export const updateAgentSquad = (data: AgentSquadVO) => {
  return request.put({ url: '/spk/agent-squad/update', data })
}

// 删除编队
export const deleteAgentSquad = (id: number) => {
  return request.delete({ url: '/spk/agent-squad/delete?id=' + id })
}

// 编队列表
export const getAgentSquadList = () => {
  return request.get<AgentSquadVO[]>({ url: '/spk/agent-squad/list' })
}

// 获取编队成员列表
export const getAgentSquadMembers = (squadId: number) => {
  return request.get<AgentSquadMemberRespVO[]>({ url: '/spk/agent-squad/member/list', params: { squadId } })
}

// 新增编队成员
export const addAgentSquadMember = (data: AgentSquadMemberReqVO) => {
  return request.post({ url: '/spk/agent-squad/member/add', data })
}

// 更新编队成员
export const updateAgentSquadMember = (data: AgentSquadMemberReqVO) => {
  return request.put({ url: '/spk/agent-squad/member/update', data })
}

// 移除编队成员
export const removeAgentSquadMember = (id: number) => {
  return request.delete({ url: '/spk/agent-squad/member/remove?id=' + id })
}

// 唤醒编队（成员按顺序串行单轮对话）
export const wakeAgentSquad = (id: number, message: string) => {
  return request.post({ url: '/spk/agent-squad/wake', data: { id, message } })
}
