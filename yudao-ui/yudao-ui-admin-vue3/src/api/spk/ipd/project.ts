import request from '@/config/axios'

// SPK-OS Cortext-IPD 项目接口（P3 后端 /spk/ipd/project/* + Omnigent 代理）
// 复用 yudao CommonResult 剥壳（request 已自动解包 data）

// 发起 IPD 主流程
export const startProject = async (params: { businessKey?: string; projectName?: string; payload?: string }) => {
  return await request.post({ url: '/spk/ipd/project/start', params })
}

// 项目总览：泳道 + 六阶段进度
export const getProject = async (processInstanceId: string) => {
  return await request.get({ url: `/spk/ipd/project/${processInstanceId}` })
}

// 各阶段进度
export const getPhases = async (processInstanceId: string) => {
  return await request.get({ url: `/spk/ipd/project/${processInstanceId}/phases` })
}

// Plane 需求代理
export const getRequirements = async (limit = 50) => {
  return await request.get({ url: '/spk/ipd/project/requirements', params: { limit } })
}

// Gitea PR/CI 状态代理
export const getGitPr = async () => {
  return await request.get({ url: '/spk/ipd/project/git-pr' })
}

// Gitea Release 代理
export const getGitRelease = async () => {
  return await request.get({ url: '/spk/ipd/project/git-release' })
}

// Omnigent session 元数据代理（iframe 嵌入用）
export const getOmnigentSession = async (sid: string) => {
  return await request.get({ url: `/spk/ipd/omnigent-proxy/session/${sid}` })
}

// Agent 任务人工介入
export const interveneTask = async (activityRunId: string, action: string, note?: string) => {
  return await request.post({ url: `/spk/agent-task/${activityRunId}/intervene`, params: { action, note } })
}
