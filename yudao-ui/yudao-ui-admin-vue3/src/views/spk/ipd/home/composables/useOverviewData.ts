/**
 * useOverviewData — 研发总览 overview 数据共享单例（设计文档 §4.1 / §3.2 行动引导条）
 * 模块级 ref：同一会话内 shell（行动引导条计数）与 overview 视图共用一份 getOverview() 结果，
 * 避免双取。带 60s 软缓存与 load(force) 显式刷新。失败保留旧数据降级空，空态由各视图呈现。
 */
import { ref } from 'vue'
import * as IpdBusinessApi from '@/api/spk/ipd/business'

const data = ref<IpdBusinessApi.SpkIpdOverviewVO>({})
const loading = ref(false)
/** 行动引导条计数：待审批/待决策项数 + 阻断流程数（真实 overview 数据派生，无假数据） */
const summary = ref({ attentionCount: 0, blockedCount: 0 })
let lastFetchedAt = 0
const TTL = 60_000

const refreshSummary = () => {
  const att = data.value.attentionItems || []
  summary.value.attentionCount = att.length
  summary.value.blockedCount = data.value.blockedFlowCount ?? 0
}

const load = async (force = false) => {
  const now = Date.now()
  if (!force && loading.value) return
  if (!force && now - lastFetchedAt < TTL) {
    refreshSummary()
    return
  }
  loading.value = true
  try {
    data.value = ((await IpdBusinessApi.getOverview()) || {}) as IpdBusinessApi.SpkIpdOverviewVO
    lastFetchedAt = now
    refreshSummary()
  } catch {
    /* 失败保留旧数据，空态由视图呈现，不冒泡 */
  } finally {
    loading.value = false
  }
}

export function useOverviewData() {
  return { data, loading, load, summary, refreshSummary }
}
