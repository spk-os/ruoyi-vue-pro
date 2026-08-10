import request from '@/config/axios'

// SPK-OS Cortext-IPD 项目接口（P3 后端 /spk/ipd/project/* + Omnigent 代理）
// 复用 yudao CommonResult 剥壳（request 已自动解包 data）

// 发起 IPD 主流程（同步返 processInstanceId；type2 触发器后 ~0.06s 到首 receiveTask）
// mode: 'test'=fast 桩仅测试不落真实交付物；'product'=真实建 Gitea 分支/PR + Plane 需求（concept 阶段起）
export const startProject = async (params: { businessKey?: string; projectName?: string; payload?: string; mode?: string }) => {
  return await request.post({ url: '/spk/ipd/project/start', params })
}

// 一句话发起 IPD 主流程（hermes「说一句话就开跑」入口）
// 后端 SpkIpdIntakeService：LLM 抽取 projectName/payload（≤30s 硬超时兜底）→ 同步启动 spkIpdFlow
// 注意：参数名勿用 request，否则遮蔽 import 的 axios 实例 request → request.post is not a function
// mode 透传：test/product（默认 test）
// 超时 60s：intake 同步含 LLM 抽取（最长 30s）+ 同步 start（type2 后 ~0.06s），覆盖 30s LLM + 余量。
// 仅此入口上调；approve 仍用原生 axios 默认 30s 不动（复用原生审批约束）。
export const intakeProject = async (req: string, mode: string = 'test') => {
  return await request.post({ url: '/spk/ipd/project/intake', data: { request: req, mode }, timeout: 60000 })
}

// 项目总览：泳道 + 六阶段进度
export const getProject = async (processInstanceId: string) => {
  return await request.get({ url: `/spk/ipd/project/${processInstanceId}` })
}

// 最新 IPD 流程实例（进入项目页/监控台默认载入）：status=ok/not_found + processInstanceId/businessKey/projectName/startTime/running
export const getLatestProject = async () => {
  return await request.get({ url: '/spk/ipd/project/latest' })
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
