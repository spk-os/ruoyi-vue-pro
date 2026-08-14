import request from '@/config/axios'

// SPK-OS Cortext-IPD 审批与决策包 API（/admin-api/spk/ipd/*）
// 设计文档 §10.8 / §7.9。复用 yudao CommonResult 剥壳（request 自动解包 data）。
// 决策动作 APPROVE/REJECT/REDIRECT/RETURN 最终仍调用原生 BPM approve/reject/return，仅业务包装。

// ==================== 类型 ====================

export interface SpkIpdApprovalTaskPageReqVO extends PageParam {
  type?: string // todo / done
  name?: string
  category?: string
  processDefinitionKey?: string
  status?: number
  createTime?: string[]
}

export interface SpkIpdApprovalTaskRespVO {
  taskId: string
  name?: string
  taskDefinitionKey?: string
  processInstanceId?: string
  processDefinitionId?: string
  createTime?: string
  endTime?: string
  assigneeUserId?: number
  assigneeNickname?: string
  ownerUserId?: number
  ownerNickname?: string
  suspended?: boolean
  projectId?: number
  projectName?: string
  versionId?: number
  versionLabel?: string
  flowRunId?: number
  runNo?: string
  flowType?: string
  currentStage?: string
  currentGate?: string
  health?: string
  businessKey?: string
  waitDurationMs?: number
}

export interface SpkIpdDecisionReqVO {
  decision: string // APPROVE/REJECT/REDIRECT/RETURN
  reason?: string
  conditions?: any[]
  decisionPackageHash?: string
  flowableVariables?: Record<string, any>
  nextAssignees?: Record<string, number[]>
  redirectTargetTaskKey?: string
  forceOverride?: boolean
}

export interface SpkIpdSummaryRow {
  label: string
  value: string
  type?: string // info|warning|danger|success
}
export interface SpkIpdSummarySection {
  title: string
  rows: SpkIpdSummaryRow[]
}
export interface SpkIpdRequiredArtifact {
  ref: string
  type: string // ARTIFACT/GATE/TR/EVIDENCE
  status: string // PASS/MISSING/WARN/FAIL
  signed?: boolean
  scanStatus?: string
  verifier?: string
  hashOk?: boolean
  blocking?: boolean
}
export interface SpkIpdCandidateAction {
  decision: string
  enabled?: boolean
  reason?: string
}
export interface SpkIpdDecisionPackageRespVO {
  header?: Record<string, any>
  summary?: SpkIpdSummarySection[]
  requiredArtifacts?: SpkIpdRequiredArtifact[]
  gates?: any[]
  evidence?: any[]
  chainValid?: boolean
  artifacts?: any[]
  ccb?: any[]
  dcpRedirects?: any[]
  decisions?: any[]
  waivers?: any[]
  blockingItems?: string[]
  candidateActions?: SpkIpdCandidateAction[]
  decisionPackageHash?: string
  builtAt?: string
}

export interface SpkIpdEvidenceWaiverReqVO {
  evidenceRef: string
  evidenceType?: string
  dueAt?: string
  reason: string
  ownerUserId?: number
}

// ==================== 接口 ====================

export const pageApprovalTasks = (params: SpkIpdApprovalTaskPageReqVO) => {
  return request.get({ url: '/spk/ipd/approval-tasks', params })
}

export const getDecisionPackage = (taskId: string) => {
  return request.get({ url: '/spk/ipd/approval-tasks/' + taskId + '/decision-package' })
}

export const createDecision = (taskId: string, data: SpkIpdDecisionReqVO) => {
  return request.post({ url: '/spk/ipd/approval-tasks/' + taskId + '/decisions', data })
}

export const listDecisionsByFlowRun = (flowRunId: number) => {
  return request.get({ url: '/spk/ipd/flow-runs/' + flowRunId + '/decisions' })
}

export const createEvidenceWaiver = (taskId: string, data: SpkIpdEvidenceWaiverReqVO) => {
  return request.post({ url: '/spk/ipd/approval-tasks/' + taskId + '/evidence-waivers', data })
}

export const getFlowRunArtifacts = (flowRunId: number) => {
  return request.get({ url: '/spk/ipd/flow-runs/' + flowRunId + '/artifacts' })
}

export const getFlowRunEvidence = (flowRunId: number) => {
  return request.get({ url: '/spk/ipd/flow-runs/' + flowRunId + '/evidence' })
}
