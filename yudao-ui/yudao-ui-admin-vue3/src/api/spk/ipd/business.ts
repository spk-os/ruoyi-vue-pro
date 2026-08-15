import request from '@/config/axios'

// SPK-OS Cortext-IPD 4 级版本模型业务 API（/admin-api/spk/ipd/*）
// 设计文档 10.1-10.6 / 12.9。复用 yudao CommonResult 剥壳（request 自动解包 data）。
// 旧的单实例 /spk/ipd/project/* 见 ./project.ts，按 10.12 进入兼容期，新页面不再调用。

// ==================== 类型 ====================

export interface SpkIpdProjectVO {
  id?: number
  projectNo?: string
  projectCode?: string
  name: string
  description?: string
  objective?: string
  ownerUserId: number
  status?: string // DRAFT/ACTIVE/PAUSED/ARCHIVED
  health?: string // UNKNOWN/GOOD/WARN/CRITICAL
  plannedStartAt?: string
  plannedEndAt?: string
  actualStartAt?: string
  actualEndAt?: string
  currentMajorReleaseId?: number
  lockVersion?: number
  createTime?: string
  updateTime?: string
}

export interface SpkIpdProjectPageReqVO extends PageParam {
  name?: string
  projectCode?: string
  ownerUserId?: number
  status?: string
  health?: string
}

export interface SpkIpdMajorReleaseVO {
  id?: number
  projectId: number
  majorNo?: number
  versionLabel?: string
  name: string
  objective?: string
  scopeSummary?: string
  ownerUserId: number
  status?: string // PLANNING/ACTIVE/MAINTENANCE/CLOSED
  baselineVersionId?: number
  plannedStartAt?: string
  plannedEndAt?: string
  actualStartAt?: string
  actualEndAt?: string
  lockVersion?: number
}

export interface SpkIpdMajorReleaseCreateReqVO {
  majorNo: number
  name: string
  objective: string
  scopeSummary?: string
  ownerUserId: number
  createBaselineVersion?: boolean
  baselinePlan?: { plannedStartAt?: string; plannedEndAt?: string }
}

export interface SpkIpdVersionVO {
  id?: number
  projectId?: number
  majorReleaseId: number
  majorNo?: number
  minorNo?: number
  versionNo?: string
  versionType: string // BASELINE/INCREMENT/HOTFIX
  baselineFlag?: number
  name?: string
  objective?: string
  scopeSummary?: string
  ownerUserId: number
  status?: string // DRAFT/PLANNING/READY/RUNNING/VERIFYING/RELEASED/CANCELLED
  deliveryReadiness?: string
  health?: string
  plannedStartAt?: string
  plannedEndAt?: string
  actualStartAt?: string
  actualEndAt?: string
  releasedAt?: string
  sourceVersionId?: number
  lockVersion?: number
}

export interface SpkIpdVersionCreateReqVO {
  versionType: string
  minorNo?: number
  name?: string
  objective: string
  scopeSummary?: string
  sourceVersionId?: number
  ownerUserId: number
  plannedStartAt?: string
  plannedEndAt?: string
}

export interface SpkIpdReadinessCheckVO {
  code: string
  status: string // PASS/WARN/BLOCK
  message?: string
  action?: string
}

export interface SpkIpdReadinessRespVO {
  ready?: boolean
  resolvedProfileVersion?: number
  checks?: SpkIpdReadinessCheckVO[]
  effectiveStages?: string[]
}

export interface SpkIpdFlowRunVO {
  id?: number
  runNo?: string
  projectId?: number
  majorReleaseId?: number
  versionId?: number
  issueCaseId?: number
  flowType: string
  profileId?: number
  profileVersion?: number
  businessKey?: string
  processInstanceId?: string
  status?: string
  currentStage?: string
  currentActivity?: string
  health?: string
  blockReason?: string
  attemptNo?: number
  supersedesFlowRunId?: number
  startedAt?: string
  endedAt?: string
  lockVersion?: number
  createTime?: string
}

export interface SpkIpdFlowRunPageReqVO extends PageParam {
  projectId?: number
  versionId?: number
  issueCaseId?: number
  flowType?: string
  status?: string
  currentStage?: string
}

export interface SpkIpdFlowRunPreflightReqVO {
  projectId: number
  versionId?: number
  issueCaseId?: number
  flowType: string
  processProfileId?: number
  tailoring?: {
    architectureMode?: string
    endGate?: string
    skipActivities?: string[]
    reason?: string
  }
}

export interface SpkIpdIssueCaseVO {
  id?: number
  caseNo?: string
  projectId?: number
  issueType: string
  severity: string
  title: string
  description?: string
  source?: string
  externalSystem?: string
  externalId?: string
  externalUrl?: string
  ownerUserId?: number
  status?: string // OPEN/TRIAGED/IN_PROGRESS/RESOLVED/CLOSED/REOPENED
  rootCause?: string
  resolution?: string
  detectedAt?: string
  resolvedAt?: string
  closedAt?: string
  lockVersion?: number
  createTime?: string
}

export interface SpkIpdIssueCasePageReqVO extends PageParam {
  projectId?: number
  issueType?: string
  severity?: string
  status?: string
  ownerUserId?: number
}

// ==================== 项目 ====================

export const getPage = (params: SpkIpdProjectPageReqVO) => {
  return request.get({ url: '/spk/ipd/projects', params })
}
export const get = (id: number) => {
  return request.get({ url: '/spk/ipd/projects/' + id })
}
export const getRoadmap = (id: number) => {
  return request.get({ url: '/spk/ipd/projects/' + id + '/roadmap' })
}
export const create = (data: SpkIpdProjectVO) => {
  return request.post({ url: '/spk/ipd/projects', data })
}
export const update = (id: number, data: SpkIpdProjectVO) => {
  return request.put({ url: '/spk/ipd/projects/' + id, data })
}
export const activate = (id: number) => {
  return request.post({ url: '/spk/ipd/projects/' + id + '/activate' })
}
export const pause = (id: number) => {
  return request.post({ url: '/spk/ipd/projects/' + id + '/pause' })
}
export const archive = (id: number) => {
  return request.post({ url: '/spk/ipd/projects/' + id + '/archive' })
}

// ==================== 大版本 ====================

export const listMajorReleases = (projectId: number) => {
  return request.get({ url: '/spk/ipd/projects/' + projectId + '/major-releases' })
}
export const getMajorRelease = (id: number) => {
  return request.get({ url: '/spk/ipd/major-releases/' + id })
}
export const createMajorRelease = (projectId: number, data: SpkIpdMajorReleaseCreateReqVO) => {
  return request.post({ url: '/spk/ipd/projects/' + projectId + '/major-releases', data })
}
export const updateMajorRelease = (id: number, data: SpkIpdMajorReleaseVO) => {
  return request.put({ url: '/spk/ipd/major-releases/' + id, data })
}
export const closeMajorRelease = (id: number) => {
  return request.post({ url: '/spk/ipd/major-releases/' + id + '/close' })
}

// ==================== 交付版本 ====================

export const listVersions = (majorReleaseId: number) => {
  return request.get({ url: '/spk/ipd/major-releases/' + majorReleaseId + '/versions' })
}
export const getVersion = (id: number) => {
  return request.get({ url: '/spk/ipd/versions/' + id })
}
export const createVersion = (majorReleaseId: number, data: SpkIpdVersionCreateReqVO) => {
  return request.post({ url: '/spk/ipd/major-releases/' + majorReleaseId + '/versions', data })
}
export const updateVersion = (id: number, data: SpkIpdVersionVO) => {
  return request.put({ url: '/spk/ipd/versions/' + id, data })
}
export const readiness = (versionId: number) => {
  return request.get({ url: '/spk/ipd/versions/' + versionId + '/readiness' })
}
export const readyVersion = (versionId: number) => {
  return request.post({ url: '/spk/ipd/versions/' + versionId + '/ready' })
}
export const cancelVersion = (versionId: number) => {
  return request.post({ url: '/spk/ipd/versions/' + versionId + '/cancel' })
}
export const traceability = (versionId: number) => {
  return request.get({ url: '/spk/ipd/versions/' + versionId + '/traceability' })
}

// ==================== FlowRun ====================

export const preflight = (data: SpkIpdFlowRunPreflightReqVO) => {
  return request.post({ url: '/spk/ipd/flow-runs/preflight', data })
}
export const createFlowRun = (data: SpkIpdFlowRunPreflightReqVO) => {
  return request.post({ url: '/spk/ipd/flow-runs', data })
}
export const getFlowRunPage = (params: SpkIpdFlowRunPageReqVO) => {
  return request.get({ url: '/spk/ipd/flow-runs', params })
}
export const getFlowRun = (id: number) => {
  return request.get({ url: '/spk/ipd/flow-runs/' + id })
}
export const startFlowRun = (id: number, idempotencyKey: string) => {
  return request.post({ url: '/spk/ipd/flow-runs/' + id + '/start', headers: { 'Idempotency-Key': idempotencyKey } })
}
export const cancelFlowRun = (id: number, reason: string) => {
  // 后端 @RequestBody Map 读取 reason，故走 data 而非 params
  return request.post({ url: '/spk/ipd/flow-runs/' + id + '/cancel', data: { reason } })
}
export const retryFlowRun = (id: number) => {
  return request.post({ url: '/spk/ipd/flow-runs/' + id + '/retry' })
}
export const blockFlowRun = (id: number, data: { reason: string; impact?: string }) => {
  return request.post({ url: '/spk/ipd/flow-runs/' + id + '/block', data })
}
export const unblockFlowRun = (id: number) => {
  return request.post({ url: '/spk/ipd/flow-runs/' + id + '/unblock' })
}
export const timeline = (id: number) => {
  return request.get({ url: '/spk/ipd/flow-runs/' + id + '/timeline' })
}
export const activities = (id: number) => {
  return request.get({ url: '/spk/ipd/flow-runs/' + id + '/activities' })
}
export const diagram = (id: number) => {
  return request.get({ url: '/spk/ipd/flow-runs/' + id + '/diagram' })
}
export const engineering = (id: number) => {
  return request.get({ url: '/spk/ipd/flow-runs/' + id + '/engineering' })
}
export const getCommand = (flowRunId: number, commandId: number) => {
  return request.get({ url: `/spk/ipd/flow-runs/${flowRunId}/commands/${commandId}` })
}

// ==================== 问题 IssueCase ====================

export const createIssue = (projectId: number, data: SpkIpdIssueCaseVO) => {
  return request.post({ url: '/spk/ipd/projects/' + projectId + '/issues', data })
}
export const pageIssues = (projectId: number, params: SpkIpdIssueCasePageReqVO) => {
  return request.get({ url: '/spk/ipd/projects/' + projectId + '/issues', params })
}
export const getIssue = (id: number) => {
  return request.get({ url: '/spk/ipd/issues/' + id })
}
export const updateIssue = (id: number, data: SpkIpdIssueCaseVO) => {
  return request.put({ url: '/spk/ipd/issues/' + id, data })
}
export const triageIssue = (id: number, data: { severity?: string; ownerUserId?: number; affectedVersionIds?: number[] }) => {
  return request.post({ url: '/spk/ipd/issues/' + id + '/triage', data })
}
export const addVersionRelation = (id: number, data: { versionId: number; relationType: string }) => {
  return request.post({ url: '/spk/ipd/issues/' + id + '/version-relations', data })
}
export const listVersionRelations = (id: number) => {
  return request.get({ url: '/spk/ipd/issues/' + id + '/version-relations' })
}
export const startIssueFlow = (id: number, idempotencyKey: string) => {
  return request.post({ url: '/spk/ipd/issues/' + id + '/start-flow', headers: { 'Idempotency-Key': idempotencyKey } })
}
export const resolveIssue = (id: number, resolution: string) => {
  return request.post({ url: '/spk/ipd/issues/' + id + '/resolve', data: { resolution } })
}
export const closeIssue = (id: number) => {
  return request.post({ url: '/spk/ipd/issues/' + id + '/close' })
}
export const reopenIssue = (id: number, reason: string) => {
  return request.post({ url: '/spk/ipd/issues/' + id + '/reopen', data: { reason } })
}

// ==================== 上下文选择器 / 旧实例映射 ====================

export const pickerProjects = () => {
  return request.get({ url: '/spk/ipd/context/projects' })
}
export const pickerVersions = (projectId: number) => {
  return request.get({ url: '/spk/ipd/context/versions', params: { projectId } })
}
export const pickerFlowRuns = (versionId: number) => {
  return request.get({ url: '/spk/ipd/context/flow-runs', params: { versionId } })
}
export const recentContexts = () => {
  return request.get({ url: '/spk/ipd/context/recent' })
}
export const saveRecentContext = (ctx: object) => {
  return request.post({ url: '/spk/ipd/context/recent', data: ctx })
}
export const resolveLegacy = (processInstanceId: string) => {
  return request.get({ url: '/spk/ipd/legacy/resolve', params: { processInstanceId } })
}
export const listNeedsMapping = () => {
  return request.get({ url: '/spk/ipd/legacy/needs-mapping' })
}
export const confirmMapping = (mappingId: number, data: object) => {
  return request.post({ url: '/spk/ipd/legacy/' + mappingId + '/confirm', data })
}

// ==================== 总览 Overview ====================

export interface SpkIpdOverviewAttentionItem {
  type: string
  severity: string
  refId?: number
  title: string
  detail?: string
}
export interface SpkIpdOverviewVO {
  projectCounts?: Record<string, number>
  versionCounts?: Record<string, number>
  flowRunCounts?: Record<string, number>
  issueCounts?: Record<string, number>
  issueSeverityCounts?: Record<string, number>
  flowHealthCounts?: Record<string, number>
  aiUsage?: Record<string, any>
  activeFlowCount?: number
  blockedFlowCount?: number
  openIssueCount?: number
  attentionItems?: SpkIpdOverviewAttentionItem[]
  recentFlows?: Array<Record<string, any>>
  roadmap?: Array<Record<string, any>>
}

export const getOverview = () => {
  return request.get({ url: '/spk/ipd/overview' })
}

// ==================== 项目维度监控 Monitor ====================

export interface SpkIpdMonitorIntegration {
  name: string
  healthy: boolean
  detail?: string
  url?: string
}
export interface SpkIpdMonitorVO {
  summary?: Record<string, number>
  flows?: Array<Record<string, any>>
  integrations?: SpkIpdMonitorIntegration[]
}

export const getMonitor = (projectId?: number) => {
  return request.get({ url: '/spk/ipd/monitor', params: projectId ? { projectId } : {} })
}

// ==================== 团队、参与者与分派 Team ====================

export interface SpkIpdActorRow {
  id: number
  actorType: string
  actorId: number
  name: string
  subtitle?: string
  businessRole?: string
  accountableFlag?: number
  capacityPct?: number
  status?: string
  projectId?: number
  versionId?: number
}
export interface SpkIpdLoadRow {
  actorType: string
  actorId: number
  name: string
  total: number
  planned: number
  running: number
  blocked: number
  done: number
}
export interface SpkIpdTeamVO {
  projectId?: number
  versionId?: number
  people?: SpkIpdActorRow[]
  agents?: SpkIpdActorRow[]
  squads?: SpkIpdActorRow[]
  load?: SpkIpdLoadRow[]
  summary?: Record<string, number>
}
export interface SpkIpdActorCandidate {
  actorType: string
  actorId: number
  name: string
  subtitle?: string
  businessRole?: string
}
export interface SpkIpdActorSaveReqVO {
  projectId?: number
  versionId?: number
  actorType: string
  actorId: number
  businessRole: string
  accountableFlag?: number
  capacityPct?: number
  effectiveFrom?: string
  effectiveTo?: string
  status?: string
}
export interface SpkIpdAssignmentVO extends PageParam {
  projectId?: number
  versionId?: number
  flowRunId?: number
  workItemType?: string
  actorType?: string
  actorId?: number
  status?: string
  id?: number
  activityRunId?: string
  workItemId?: string
  accountableActorType?: string
  accountableActorId?: number
  plannedEffort?: number
  actualEffort?: number
  lockVersion?: number
  createTime?: string
}
export interface SpkIpdAssignmentCreateReqVO {
  projectId?: number
  versionId?: number
  flowRunId: number
  activityRunId?: string
  workItemType: string
  workItemId: string
  actorType: string
  actorId: number
  accountableActorType?: string
  accountableActorId?: number
  plannedEffort?: number
}
export interface SpkIpdAssignmentReassignReqVO {
  actorType: string
  actorId: number
  accountableActorType?: string
  accountableActorId?: number
  reason?: string
}

export const getTeam = (projectId?: number, versionId?: number) => {
  const params: any = {}
  if (projectId) params.projectId = projectId
  if (versionId) params.versionId = versionId
  return request.get({ url: '/spk/ipd/team', params })
}
export const listActors = (projectId: number, versionId?: number) => {
  const params: any = {}
  if (versionId) params.versionId = versionId
  return request.get({ url: `/spk/ipd/projects/${projectId}/actors`, params })
}
export const pageActors = (params: any) => {
  return request.get({ url: '/spk/ipd/actors/page', params })
}
export const saveActor = (projectId: number, data: SpkIpdActorSaveReqVO) => {
  return request.post({ url: `/spk/ipd/projects/${projectId}/actors`, data })
}
export const deleteActor = (id: number) => {
  return request.delete({ url: `/spk/ipd/actors/${id}` })
}
export const candidates = (actorType?: string, role?: string, q?: string) => {
  const params: any = {}
  if (actorType) params.actorType = actorType
  if (role) params.role = role
  if (q) params.q = q
  return request.get({ url: '/spk/ipd/actors/candidates', params })
}
export const pageAssignments = (params: any) => {
  return request.get({ url: '/spk/ipd/assignments', params })
}
export const createAssignment = (data: SpkIpdAssignmentCreateReqVO) => {
  return request.post({ url: '/spk/ipd/assignments', data })
}
export const reassign = (assignmentId: number, data: SpkIpdAssignmentReassignReqVO) => {
  return request.post({ url: `/spk/ipd/assignments/${assignmentId}/reassign`, data })
}

// ==================== 项目空间 BFF（SpkIpdProjectWorkspaceController） ====================
// 项目卡片网格 + 详情聚合（活跃版本/FlowRun/阶段时间戳/泳道/需求树/最近活动）
// 全真实 DB 聚合，无数据字段前端标注"样本不足"，不造假。

// 项目卡片网格：currentStage/activeVersion/dueIn/majorReleases/blockedFlows/pendingDecisions/teamSize/freshness
export const getProjectCards = () => {
  return request.get({ url: '/spk/ipd/projects/cards' })
}

// 项目详情一次性聚合：活跃版本/FlowRun/阶段时间戳/泳道/最近活动/需求树/问题计数
export const getProjectWorkspace = (projectId: number) => {
  return request.get({ url: `/spk/ipd/projects/${projectId}/workspace` })
}

// 6 阶段进入时间与状态（按版本活跃 FlowRun 聚合）
export const getStageTimestamps = (projectId: number, versionId?: number) => {
  return request.get({ url: `/spk/ipd/projects/${projectId}/stage-timestamps`, params: versionId ? { versionId } : {} })
}

// IR/SR/AR 需求追踪树（按版本过滤，无数据 sparse=true）
export const getRequirementsTree = (projectId: number, versionId?: number) => {
  return request.get({ url: `/spk/ipd/projects/${projectId}/requirements-tree`, params: versionId ? { versionId } : {} })
}

// 项目维度跨 FlowRun 最近活动时间线
export const getRecentActivities = (projectId: number, limit = 10) => {
  return request.get({ url: `/spk/ipd/projects/${projectId}/recent-activities`, params: { limit } })
}
