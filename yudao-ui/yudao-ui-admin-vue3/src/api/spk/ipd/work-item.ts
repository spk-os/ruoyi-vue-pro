import request from '@/config/axios'

// SPK-OS IPD Plane 工作项 API（/admin-api/spk/ipd/projects/{projectId}/work-items/*）
// 设计文档 §10.7。聚合 Plane 需求/任务/缺陷快照 + 绑定到版本/流程/Activity + 异步幂等同步。

// ==================== 类型 ====================

export interface WorkItemRespVO {
  planeIssueId: string
  planeIssueSeq?: string
  name?: string
  linkType?: string // REQUIREMENT/TASK/DEFECT/MILESTONE
  syncStatus?: string // PENDING/LINKED/SYNCED/ORPHAN
  versionId?: number
  flowRunId?: number
  activityCode?: string
  lastSyncedAt?: string
  linked?: boolean
}

export interface WorkItemLinkReqVO {
  planeIssueId: string
  planeIssueSeq?: string
  name?: string
  linkType?: string
  versionId?: number
  flowRunId?: number
  activityCode?: string
}

export interface WorkItemSyncRespVO {
  commandId?: number
  status: string // SUCCESS/IDEMPOTENT/FAILED
  syncedCount?: number
  syncedAt?: string
}

// ==================== API ====================

export const getWorkItems = async (projectId: number) => {
  return await request.get<WorkItemRespVO[]>({
    url: `/spk/ipd/projects/${projectId}/work-items`
  })
}

export const linkWorkItem = async (projectId: number, data: WorkItemLinkReqVO) => {
  return await request.post<WorkItemRespVO>({
    url: `/spk/ipd/projects/${projectId}/work-items/link`,
    data
  })
}

export const syncWorkItems = async (projectId: number, idempotencyKey?: string) => {
  return await request.post<WorkItemSyncRespVO>({
    url: `/spk/ipd/projects/${projectId}/work-items/sync`,
    params: { idempotencyKey }
  })
}
