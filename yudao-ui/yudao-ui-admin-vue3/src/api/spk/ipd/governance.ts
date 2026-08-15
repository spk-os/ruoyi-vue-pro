/**
 * SPK-OS Cortext-IPD 流程治理 API（§7.2 第 4 顶层表面）。
 * 后端：SpkIpdGovernanceController @ /spk/ipd/admin/workflows。
 * 数据铁律：运行时聚合（引擎档案/失败作业/审计）无数据时如实返回空，前端显式标注"未接入/样本不足"，绝不造假。
 */
import request from '@/config/axios'

// ==================== Profile 模板 ====================
export const pageProfiles = (params: any) => request.get({ url: '/spk/ipd/admin/workflows/profiles', params })
export const getProfile = (id: number) => request.get({ url: `/spk/ipd/admin/workflows/profiles/${id}` })
export const createProfile = (data: any) => request.post({ url: '/spk/ipd/admin/workflows/profiles', data })
export const updateProfile = (data: any) => request.put({ url: '/spk/ipd/admin/workflows/profiles', data })
export const deleteProfile = (id: number) => request.delete({ url: `/spk/ipd/admin/workflows/profiles/${id}` })

// ==================== Profile 版本 ====================
export const listVersions = (profileId: number) =>
  request.get({ url: `/spk/ipd/admin/workflows/profiles/${profileId}/versions` })
export const createVersion = (profileId: number, data: { snapshotJson: string; compatibilityHash?: string }) =>
  request.post({ url: `/spk/ipd/admin/workflows/profiles/${profileId}/versions`, data })
export const publishVersion = (versionId: number) =>
  request.post({ url: `/spk/ipd/admin/workflows/profiles/versions/${versionId}/publish` })
export const rollbackVersion = (versionId: number) =>
  request.post({ url: `/spk/ipd/admin/workflows/profiles/versions/${versionId}/rollback` })

// ==================== 裁剪规则 ====================
export const listTrimRules = (profileVersionId: number) =>
  request.get({ url: '/spk/ipd/admin/workflows/trim-rules', params: { profileVersionId } })
export const saveTrimRule = (data: any) => request.post({ url: '/spk/ipd/admin/workflows/trim-rules', data })
export const deleteTrimRule = (id: number) => request.delete({ url: `/spk/ipd/admin/workflows/trim-rules/${id}` })

// ==================== 治理查询（运行时聚合） ====================
export const listFailedJobs = (status = 'PENDING') =>
  request.get({ url: '/spk/ipd/admin/workflows/failed-jobs', params: { status } })
export const listAudit = (params?: { actionType?: string; refId?: number }) =>
  request.get({ url: '/spk/ipd/admin/workflows/audit', params })
