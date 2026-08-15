<!--
  研发驾驶舱 · 交付分析（设计文档 §4.4 / 原型 analytics.html）
  回答"为什么变好/变坏、如何改进"。主题：流动效率/质量治理/智能协同/工程效能/集成质量。
  数据：getMetricsSnapshot()。数据不足显式"未接入/样本不足"，不用 0 冒充（§4.4 铁律）。
-->
<template>
  <div v-loading="loading" class="spk-analytics">
    <div class="spk-filter mb-16px">
      <el-select v-model="range" size="small" class="spk-filter__select">
        <el-option label="最近 30 天" value="30d" />
        <el-option label="最近 90 天" value="90d" />
        <el-option label="本季度" value="quarter" />
      </el-select>
      <el-button size="small" @click="exportData">导出</el-button>
    </div>

    <!-- 三大指标卡 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-16px mb-16px">
      <div v-for="m in metricCards" :key="m.title" class="spk-mc">
        <div class="spk-mc__t">
          <span class="spk-mc__icon" :style="{ background: m.iconBg, color: m.iconFg }">{{ m.icon }}</span>
          {{ m.title }}
        </div>
        <div class="spk-mc__rows">
          <div v-for="r in m.rows" :key="r.label" class="spk-mc__row">
            <span class="spk-mc__label">{{ r.label }}</span>
            <span class="spk-mc__val">{{ r.value }}</span>
            <span v-if="r.delta" class="spk-mc__delta">{{ r.delta }}</span>
          </div>
          </div>
        <div class="spk-mc__spark">
          <SpkSparkline v-if="m.spark.length" :data="m.spark" :color="m.iconFg" :height="36" />
          <span v-else class="spk-mc__nadata">样本不足</span>
        </div>
      </div>
    </div>

    <!-- 智能体排行榜 + 阶段瓶颈 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-16px mb-16px">
      <div class="spk-card">
        <div class="spk-card__header">
          <div class="spk-card__title">智能体排行榜</div>
          <router-link :to="{ name: 'SpkIpdTeam' }" class="spk-card__link">团队与智能体 →</router-link>
        </div>
        <div class="spk-card__body spk-card__body--flush">
          <el-empty v-if="!agents.length" description="暂无智能体效能数据" :image-size="50" />
          <table v-else class="spk-lb">
            <thead>
              <tr><th>#</th><th>智能体</th><th>成功率</th><th>任务数</th><th>费用</th></tr>
            </thead>
            <tbody>
              <tr v-for="(a, i) in agents" :key="i" :class="{ 'spk-lb__row--bad': a.bad }">
                <td>{{ i + 1 }}</td>
                <td class="spk-lb__name">{{ a.name }}</td>
                <td class="spk-lb__pct" :class="{ 'spk-lb__pct--bad': a.bad }">{{ a.rate }}</td>
                <td>{{ a.tasks }}</td>
                <td>{{ a.cost }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="spk-card">
        <div class="spk-card__header">
          <div class="spk-card__title">阶段瓶颈分析</div>
          <span class="spk-card__sub">各阶段平均等待时间</span>
        </div>
        <div class="spk-card__body">
          <el-empty v-if="!stageBottleneck.length" description="样本不足：阶段平均耗时待 FlowRun 阶段聚合接入" :image-size="50" />
          <div v-for="b in stageBottleneck" :key="b.label" class="spk-bn">
            <span class="spk-bn__label">{{ b.label }}</span>
            <div class="spk-bn__bar"><div class="spk-bn__fill" :style="{ width: b.pct + '%', background: b.color }" /></div>
            <span class="spk-bn__val" :style="{ color: b.color, fontWeight: b.bold ? 700 : 400 }">{{ b.days }}</span>
          </div>
          <div v-if="bottleneckStage" class="spk-bn__alert">
            <b>瓶颈：{{ bottleneckStage.label }}（{{ bottleneckStage.days }}）</b>
            <span>该阶段平均耗时占比偏高，建议核查该阶段 Activity 重试率与失败智能体配置。</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 费用趋势 + 吞吐量 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-16px">
      <div class="spk-card">
        <div class="spk-card__header"><div class="spk-card__title">费用趋势</div></div>
        <div class="spk-card__body">
          <div class="spk-kpis">
            <div><div class="spk-kpis__label">总费用</div><div class="spk-kpis__val">{{ costSummary.total || '未接入' }}</div></div>
            <div><div class="spk-kpis__label">日均</div><div class="spk-kpis__val">{{ costSummary.daily || '未接入' }}</div></div>
          </div>
          <div class="spk-mc__spark"><SpkSparkline :data="costSpark" color="#0969da" :height="48" /></div>
        </div>
      </div>
      <div class="spk-card">
        <div class="spk-card__header"><div class="spk-card__title">吞吐量（FlowRun / 周）</div></div>
        <div class="spk-card__body">
          <div class="spk-kpis">
            <div><div class="spk-kpis__label">本周</div><div class="spk-kpis__val">{{ throughput.week || '—' }}</div></div>
            <div><div class="spk-kpis__label">4 周均值</div><div class="spk-kpis__val">{{ throughput.avg4 || '—' }}</div></div>
          </div>
          <div class="spk-mc__spark"><SpkSparkline :data="throughputSpark" color="#6639ba" :height="48" /></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { getMetricsSnapshot, getAgentLoad } from '@/api/spk/ipd/cockpit'
import SpkSparkline from './components/SpkSparkline.vue'

defineOptions({ name: 'SpkIpdHomeAnalytics' })

const message = useMessage()
const loading = ref(true)
const range = ref('30d')
const snap = ref<Record<string, any>>({})
const agentLoad = ref<Record<string, any>>({})

const metricCards = computed(() => {
  const s = snap.value
  const noData = (v: any) => (v === undefined || v === null ? '未接入' : v)
  return [
    {
      title: '流程效率', icon: '⚡', iconBg: '#ddf4ff', iconFg: '#0550ae', spark: [] as number[],
      rows: [
        { label: '端到端交付周期', value: noData(s.leadTime), delta: '' },
        { label: '开发周期', value: noData(s.developCycle), delta: '' },
        { label: 'WIP 在制品', value: noData(s.wip), delta: '' },
        { label: '返工率', value: noData(s.reworkRate), delta: '' }
      ]
    },
    {
      title: '质量治理', icon: '✓', iconBg: '#dafbe1', iconFg: '#116329', spark: [] as number[],
      rows: [
        { label: 'DCP 通过率', value: noData(s.dcpPassRate), delta: '' },
        { label: 'TR 首次通过率', value: noData(s.trFirstPassRate), delta: '' },
        { label: '证据完整度', value: noData(s.evidenceCompleteness), delta: '' },
        { label: '缺陷逃逸率', value: noData(s.defectEscapeRate), delta: '' }
      ]
    },
    {
      title: '智能协同', icon: '🤖', iconBg: '#fbefff', iconFg: '#6639ba', spark: [] as number[],
      rows: [
        { label: '智能体成功率', value: noData(s.agentSuccessRate), delta: '' },
        { label: '人工接管率', value: noData(s.humanTakeoverRate), delta: '' },
        { label: '重试率', value: noData(s.retryRate), delta: '' },
        { label: '日均费用', value: noData(s.dailyCost), delta: '' }
      ]
    }
  ]
})

const agents = computed<{ name: string; rate: string; tasks: number; cost: string; bad: boolean }[]>(() => {
  // getAgentLoad 返回 12 Lead + 3 Verifier 合同/死信计数，非排行榜；若后端无排行榜数据则空
  const load = agentLoad.value
  if (!load || !Object.keys(load).length) return []
  return []
})

// 阶段瓶颈：来源后端 metrics snapshot 的 stageBottleneck 字段（各阶段平均等待时间）。
// 后端尚未接入阶段耗时聚合时为空，显示"样本不足"，绝不灌硬编码天数（C-14 铁律）。
const stageBottleneck = computed(() => {
  const arr = snap.value.stageBottleneck
  return Array.isArray(arr) ? arr : []
})
const bottleneckStage = computed(() => stageBottleneck.value.find((s: any) => s.bold && s.pct >= 80) || null)

const costSummary = computed(() => {
  const c = snap.value
  if (c.dailyCost === undefined) return { total: '', daily: '' }
  return { total: c.dailyCost ? `$${(c.dailyCost * 30).toFixed(0)}` : '', daily: `$${c.dailyCost}` }
})
const costSpark = computed<number[]>(() => snap.value.costTrend || [])
const throughput = computed(() => ({
  week: snap.value.throughputWeek ?? '—',
  avg4: snap.value.throughputAvg4 ?? '—'
}))
const throughputSpark = computed<number[]>(() => snap.value.throughputTrend || [])

const exportData = () => message.info('导出功能待后端接入（§4.4 数据口径）')

const load = async () => {
  loading.value = true
  try {
    const [s, al] = await Promise.all([
      getMetricsSnapshot().catch(() => ({})),
      getAgentLoad().catch(() => ({}))
    ])
    snap.value = s || {}
    agentLoad.value = al || {}
  } catch (e: any) {
    if (e?.message) message.error(e.message)
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.spk-filter { display: flex; align-items: center; gap: 8px; }
.spk-filter__select { width: 140px; }
.spk-mc { background: #fff; border: 1px solid #d0d7de; border-radius: 8px; padding: 14px 16px; }
.spk-mc__t { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 700; color: #1f2328; margin-bottom: 10px; }
.spk-mc__icon { width: 24px; height: 24px; border-radius: 5px; display: flex; align-items: center; justify-content: center; font-size: 13px; }
.spk-mc__rows { display: flex; flex-direction: column; gap: 4px; }
.spk-mc__row { display: grid; grid-template-columns: 1fr auto auto; gap: 6px; font-size: 13px; align-items: center; }
.spk-mc__label { color: #656d76; }
.spk-mc__val { font-family: ui-monospace, monospace; font-weight: 700; color: #1f2328; }
.spk-mc__delta { font-size: 11px; color: #656d76; }
.spk-mc__spark { margin-top: 10px; min-height: 36px; display: flex; align-items: flex-end; }
.spk-mc__nadata { font-size: 11px; color: #8c959f; }
.spk-card { background: #fff; border: 1px solid #d0d7de; border-radius: 8px; display: flex; flex-direction: column; }
.spk-card__header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #eaeef2; }
.spk-card__title { font-size: 14px; font-weight: 600; color: #1f2328; }
.spk-card__sub { font-size: 12px; color: #8c959f; }
.spk-card__link { font-size: 12px; color: #0969da; text-decoration: none; }
.spk-card__body { padding: 12px 16px; }
.spk-card__body--flush { padding: 0; }
.spk-lb { width: 100%; border-collapse: collapse; font-size: 12px; }
.spk-lb th { background: #f6f8fa; color: #656d76; font-weight: 600; padding: 6px 12px; text-align: left; }
.spk-lb td { padding: 6px 12px; border-bottom: 1px solid #eaeef2; }
.spk-lb__name { font-weight: 500; }
.spk-lb__pct { font-family: ui-monospace, monospace; color: #116329; }
.spk-lb__pct--bad { color: #a40e26; font-weight: 700; }
.spk-lb__row--bad { background: #ffebe9; }
.spk-bn { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.spk-bn__label { width: 60px; font-size: 13px; color: #1f2328; }
.spk-bn__bar { flex: 1; height: 18px; background: #eaeef2; border-radius: 4px; overflow: hidden; }
.spk-bn__fill { height: 100%; border-radius: 4px; }
.spk-bn__val { width: 40px; font-family: ui-monospace, monospace; font-size: 12px; }
.spk-bn__alert { margin-top: 12px; padding: 10px 12px; background: #ffebe9; border: 1px solid #ff8182; border-radius: 6px; display: flex; flex-direction: column; gap: 4px; }
.spk-bn__alert b { color: #a40e26; font-size: 13px; }
.spk-bn__alert span { font-size: 12px; color: #656d76; }
.spk-kpis { display: flex; gap: 24px; margin-bottom: 10px; }
.spk-kpis__label { font-size: 12px; color: #656d76; font-weight: 600; }
.spk-kpis__val { font-size: 18px; font-weight: 700; font-family: ui-monospace, monospace; color: #1f2328; }
</style>
