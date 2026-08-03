import request from '@/config/axios'

// SPK-OS IPD Cockpit 聚合接口（Cortext-IPD §9）
// 复用 yudao CommonResult 剥壳（request 已自动解包 data）

// 泳道图：按流程实例聚合阶段→Activity 卡片
export const getSwimlane = async (processInstanceId: string) => {
  return await request.get({ url: '/spk/ipd/cockpit/swimlane', params: { processInstanceId } })
}

// Activity 详情：三件套（ContextManifest/ArtifactManifest/RunReceipt）+ VerificationReceipt + 证据链哈希校验
export const getActivityDetail = async (activityRunId: string) => {
  return await request.get({ url: '/spk/ipd/cockpit/activity-detail', params: { activityRunId } })
}

// Agent 负载看板：12 Lead + 3 Verifier running 合同数 + 死信数
export const getAgentLoad = async () => {
  return await request.get({ url: '/spk/ipd/cockpit/agent-load' })
}

// 监控指标快照：核心计数器 + 当前积压
export const getMetricsSnapshot = async () => {
  return await request.get({ url: '/spk/ipd/metrics/snapshot' })
}
