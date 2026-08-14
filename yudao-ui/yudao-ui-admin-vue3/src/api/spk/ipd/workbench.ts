import request from '@/config/axios'

// SPK-OS IPD 指挥工作台 API（/admin-api/spk/ipd/workbench/*）
// 设计文档 §7.5 / §10.9。聚合行动队列+阶段看板+自然语言命令（解析→预览→确认→执行幂等白名单）。

// ==================== 类型 ====================

export interface WorkbenchInboxItem {
  type: string // MY_TODO/AGENT_FAILURE/DCP_TR/EVIDENCE_GAP/SYNC_FAILURE/BLOCKED_FLOW
  severity: string
  title: string
  detail?: string
  refType?: string // APPROVAL/CONTRACT/FLOW_RUN/ISSUE/GATE
  refId?: string
  flowRunId?: number
  projectId?: number
  action?: string // APPROVE/RETRY/UNBLOCK/REASSIGN...
}

export interface WorkbenchBoardItem {
  id: string
  name: string
  status: string
  ownerType?: string
  ownerName?: string
  itemType?: string
  flowRunId?: number
  projectId?: number
  dueAt?: string
}

export interface WorkbenchBoardColumn {
  stage: string
  items: WorkbenchBoardItem[]
}

export interface WorkbenchRespVO {
  context?: Record<string, any>
  nextGate?: string
  refreshedAt?: string
  inbox?: WorkbenchInboxItem[]
  board?: WorkbenchBoardColumn[]
}

export interface CommandParseReq {
  text: string
  projectId?: number
  versionId?: number
}

export interface CommandParseResp {
  intent: string
  targets?: Record<string, any>[]
  actions?: Record<string, any>[]
  ambiguities?: string[]
  impact?: string
  idempotencyKey?: string
  executable?: boolean
}

export interface CommandExecuteReq {
  idempotencyKey: string
  intent: string
  params?: Record<string, any>
}

export interface CommandExecuteResp {
  intent: string
  status: string // SUCCESS/REJECTED/IDEMPOTENT/NOT_SUPPORTED
  message?: string
  result?: Record<string, any>
}

// ==================== API ====================

export const getWorkbenchSnapshot = async (projectId?: number, versionId?: number) => {
  return await request.get<WorkbenchRespVO>({
    url: '/spk/ipd/workbench/snapshot',
    params: { projectId, versionId }
  })
}

export const getWorkbenchInbox = async (projectId?: number) => {
  return await request.get<WorkbenchRespVO>({
    url: '/spk/ipd/workbench/inbox',
    params: { projectId }
  })
}

export const getWorkbenchBoard = async (projectId?: number) => {
  return await request.get<WorkbenchRespVO>({
    url: '/spk/ipd/workbench/board',
    params: { projectId }
  })
}

export const parseWorkbenchCommand = async (data: CommandParseReq) => {
  return await request.post<CommandParseResp>({
    url: '/spk/ipd/workbench/commands/parse',
    data
  })
}

export const executeWorkbenchCommand = async (data: CommandExecuteReq) => {
  return await request.post<CommandExecuteResp>({
    url: '/spk/ipd/workbench/commands/execute',
    data
  })
}
