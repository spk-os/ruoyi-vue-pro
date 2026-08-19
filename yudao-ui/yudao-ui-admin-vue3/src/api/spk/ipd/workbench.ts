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
  // v4.0 §4.4 三视图扩展（真实后端聚合，无前端造假）
  tasks?: WorkbenchTaskRow[]
  taskStats?: Record<string, number>
  kpis?: Record<string, any>
  globalParams?: Record<string, any>
  eventStream?: Array<Record<string, any>>
}

// v4.0 §4.4 任务视图：全状态扁平任务行
export interface WorkbenchTaskRow {
  activityRunId: string
  name: string
  projectId?: number
  projectName?: string
  flowRunId?: number
  flowRunNo?: string
  stage?: string
  ownerType?: string // HUMAN/AGENT/SQUAD/SYSTEM
  ownerName?: string
  status?: string // queued/running/done/failed/timeout/cancelled
  progress?: number
  durationSec?: number
  dueAt?: string
  sessionId?: string // Omnigent 会话 ID（run_receipt.conversation_id，可空）
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

// ==================== v4.0 §4.4 任务控制（停止/重新运行/再分配）====================
// 全部走真实后端端点，无前端 mock。停止=abort 介入、重新运行=rerun 介入、再分配=assignments reassign。

// 人工介入 Activity 运行：action=abort（停止/标记失败）/rerun（重新运行）/note（落反馈）
export const interveneActivity = (activityRunId: string, action: 'rerun' | 'abort' | 'note', note?: string) => {
  const params: any = { action }
  if (note) params.note = note
  return request.post({ url: `/spk/agent-task/${activityRunId}/intervene`, params })
}

// FlowRun 级停止（取消整个流程）与重跑，复用 business.ts 的 cancel/retry（幂等键防并发）
export const cancelFlowRun = (id: number, reason: string) => {
  return request.post({ url: `/spk/ipd/flow-runs/${id}/cancel`, data: { reason }, headers: { 'Idempotency-Key': `wb-cancel-${id}-${Date.now()}` } })
}
export const retryFlowRun = (id: number) => {
  return request.post({ url: `/spk/ipd/flow-runs/${id}/retry`, headers: { 'Idempotency-Key': `wb-retry-${id}-${Date.now()}` } })
}

// 任务再分配：assignmentId → 新执行者（人/Agent）
export interface ReassignReq {
  actorType: string
  actorId: number
  accountableActorType?: string
  accountableActorId?: number
  reason?: string
}
export const reassignAssignment = (assignmentId: number, data: ReassignReq) => {
  return request.post({ url: `/spk/ipd/assignments/${assignmentId}/reassign`, data })
}

// 分页查分派（按 flowRun/actor/status 过滤，用于"再分配给谁"前查当前归属）
export const pageAssignments = (params: any) => {
  return request.get({ url: '/spk/ipd/assignments', params })
}

// ==================== v4.0 §4.4 Omnigent 会话查看（复用既有 proxy，不重造）====================
// GET /spk/ipd/omnigent-proxy/session/{sid} 取会话 JSON；SSE /session/{sid}/stream 实时流。
// iframe 嵌入 Omnigent 原生会话页（SPA 路由 /c/{sid}，cookie 鉴权），同 cockpit ActivityDetail 取值源。

// Omnigent 基址（VITE_OMNIGENT_BASE_URL，缺省回环地址；与 ActivityDetail 一致）
export const OMNI_BASE_URL =
  (import.meta as any).env?.VITE_OMNIGENT_BASE_URL || 'http://192.168.56.101:6767'

export const getOmnigentSession = (sid: string) => {
  return request.get<string>({ url: `/spk/ipd/omnigent-proxy/session/${sid}` })
}

// 构造 Omnigent 原生会话页 iframe URL（/c/{sid}，Omnigent cookie 鉴权，前端不接触凭证）
export const omnigentSessionFrameUrl = (sid: string) => {
  if (!sid) return ''
  return `${OMNI_BASE_URL}/c/${sid}`
}

// SSE 实时流 URL（经后端 proxy 鉴权注入）
export const omnigentStreamUrl = (sid: string) => {
  const base = (import.meta as any).env?.VITE_BASE_URL || ''
  return `${base}/admin-api/spk/ipd/omnigent-proxy/session/${sid}/stream`
}
