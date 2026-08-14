<!--
  研发驾驶舱 · 运行监控（设计文档 §4.3 / 原型 monitor.html）
  回答"现在发生了什么、为什么"。四区：汇总指标 + 筛选 + FlowRun 列表(可展开)。
  全局→项目→运行→活动下钻。数据：getMonitor()(summary/flows/integrations) + getMetricsSnapshot()。
-->
<template>
  <div v-loading="loading" class="spk-monitor">
    <!-- 汇总指标 -->
    <div class="grid grid-cols-2 md:grid-cols-5 gap-10px mb-16px">
      <SpkStatCard label="活跃运行" :value="summary.active ?? '未接入'" />
      <SpkStatCard label="已阻塞" :value="summary.blocked ?? '未接入'" :danger="(summary.blocked || 0) > 0" />
      <SpkStatCard label="待决策/问题" :value="summary.waiting ?? '未接入'" />
      <SpkStatCard label="今日完成" :value="summary.doneToday ?? '未接入'" />
      <SpkStatCard label="平均证据完整度" :value="eviPct" />
    </div>

    <!-- 筛选栏 -->
    <div class="spk-filter mb-12px">
      <el-input v-model="kw" placeholder="搜索 FlowRun ID 或项目..." clearable size="small" class="spk-filter__input" />
      <el-select v-model="fStatus" placeholder="所有状态" size="small" clearable class="spk-filter__select">
        <el-option label="运行中" value="RUNNING" />
        <el-option label="已阻塞" value="BLOCKED" />
        <el-option label="等待审批" value="WAITING_APPROVAL" />
        <el-option label="已完成" value="COMPLETED" />
      </el-select>
      <el-select v-model="fType" placeholder="所有类型" size="small" clearable class="spk-filter__select">
        <el-option label="全量" value="FULL" />
        <el-option label="增量" value="INCREMENT" />
        <el-option label="问题" value="ISSUE" />
      </el-select>
      <span class="spk-filter__count">共 {{ flowsFiltered.length }} 个运行</span>
    </div>

    <!-- FlowRun 列表 -->
    <div v-if="!flowsFiltered.length" class="spk-empty">暂无 FlowRun</div>
    <div v-else class="spk-flowlist">
      <!-- 表头 -->
      <div class="spk-flowrow spk-flowrow--head">
        <div class="spk-flowrow__id">FlowRun</div>
        <div class="spk-flowrow__stage">阶段流水线</div>
        <div class="spk-flowrow__evi">证据</div>
        <div class="spk-flowrow__status">状态</div>
        <div class="spk-flowrow__wait">等待</div>
        <div class="spk-flowrow__op"></div>
      </div>

      <template v-for="fr in flowsFiltered" :key="fr.id">
        <div class="spk-flowrow" @click="toggle(fr.id)">
          <div class="spk-flowrow__id">
            <div class="spk-flowrow__idno">{{ fr.runNo || fr.id }}</div>
            <div class="spk-flowrow__sub">{{ fr.flowType }} · {{ fr.currentStage || '—' }} · {{ fr.health || '—' }}</div>
          </div>
          <div class="spk-flowrow__stage">
            <SpkStagePipeline :states="pipelineStates(fr)" />
          </div>
          <div class="spk-flowrow__evi">
            <SpkEvidenceBar :pct="fr.evidenceCompleteness ?? 0" />
          </div>
          <div class="spk-flowrow__status">
            <SpkBadge :map="statusMap" :value="fr.status" />
          </div>
          <div class="spk-flowrow__wait" :class="{ 'spk-flowrow__wait--bad': fr.waitDurationBad }">
            {{ fr.waitDuration || '—' }}
          </div>
          <div class="spk-flowrow__op">
            <span class="spk-flowrow__toggle">{{ openId === fr.id ? '↑' : '↓' }}</span>
          </div>
        </div>

        <!-- 展开详情面板 -->
        <el-collapse-transition>
          <div v-show="openId === fr.id" class="spk-detail">
            <!-- 阻塞/决策摘要 -->
            <div class="spk-detail__sum">
              <div class="spk-detail__row"><span class="spk-detail__label">当前阶段</span><span>{{ fr.currentStage || '—' }}</span></div>
              <div class="spk-detail__row"><span class="spk-detail__label">健康度</span><span>{{ fr.health || '—' }}</span></div>
              <div class="spk-detail__row"><span class="spk-detail__label">启动时间</span><span>{{ fmt(fr.startedAt) }}</span></div>
              <div class="spk-detail__row"><span class="spk-detail__label">证据/产物</span><span>{{ fr.evidenceCount ?? 0 }} / {{ fr.artifactCount ?? 0 }}</span></div>
              <div v-if="fr.blockReason" class="spk-detail__row spk-detail__row--bad">
                <span class="spk-detail__label">阻塞原因</span><span>{{ fr.blockReason }}</span>
              </div>
            </div>
            <div class="spk-detail__actions">
              <el-button type="primary" size="small" @click.stop="openFlow(fr)">打开 FlowRun 详情</el-button>
              <el-button v-if="fr.status === 'BLOCKED'" size="small" @click.stop="goActions">排查解除</el-button>
              <el-button v-if="fr.status === 'WAITING_APPROVAL'" type="warning" size="small" @click.stop="goApproval">审核决策包</el-button>
            </div>
          </div>
        </el-collapse-transition>
      </template>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { formatDate } from '@/utils/formatTime'
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import { getMetricsSnapshot } from '@/api/spk/ipd/cockpit'
import SpkStatCard from './components/SpkStatCard.vue'
import SpkBadge from './components/SpkBadge.vue'
import SpkStagePipeline from './components/SpkStagePipeline.vue'
import SpkEvidenceBar from './components/SpkEvidenceBar.vue'
import { statusMap } from './components/status'

defineOptions({ name: 'SpkIpdHomeMonitor' })

const message = useMessage()
const { push } = useRouter()
const loading = ref(true)
const summary = ref<Record<string, number>>({})
const flows = ref<any[]>([])

const kw = ref('')
const fStatus = ref('')
const fType = ref('')
const openId = ref<string | number | null>(null)

const eviPct = computed(() => {
  const v = summary.value.avgEvidence
  if (v === undefined || v === null) return '未接入'
  return Math.round(v) + '%'
})

// 时间戳 → 可读时间；空值显示占位
const fmt = (ts?: number | string | null) => (ts ? formatDate(ts, 'YYYY-MM-DD HH:mm') : '—')

const flowsFiltered = computed(() => {
  return flows.value.filter((fr) => {
    if (kw.value) {
      const k = kw.value.toLowerCase()
      if (!(`${fr.runNo || ''} ${fr.id} ${fr.projectName || ''}`.toLowerCase().includes(k))) return false
    }
    if (fStatus.value && fr.status !== fStatus.value) return false
    if (fType.value && fr.flowType !== fType.value) return false
    return true
  })
})

const STAGE_ORDER = ['CONCEPT', 'PLAN', 'DEVELOP', 'QUALIFY', 'LAUNCH', 'LIFECYCLE']
const pipelineStates = (fr: any): string[] => {
  const idx = STAGE_ORDER.indexOf((fr.currentStageKey || fr.currentStage || '').toUpperCase())
  const cur = idx >= 0 ? idx : 0
  const out: string[] = []
  for (let i = 0; i < 6; i++) {
    if (fr.status === 'BLOCKED' && i === cur) out.push('blocked')
    else if (fr.status === 'FAILED') out.push(i < cur ? 'done' : 'failed')
    else if (i < cur) out.push('done')
    else if (i === cur) out.push(fr.status === 'COMPLETED' ? 'done' : 'active')
    else out.push('pending')
  }
  return out
}

const toggle = (id: string | number) => {
  openId.value = openId.value === id ? null : id
}
const openFlow = (fr: any) => push({ name: 'SpkIpdCockpit', query: { processInstanceId: fr.processInstanceId || fr.id } })
const goActions = () => push({ name: 'SpkIpdHome', query: { view: 'actions' } })
const goApproval = () => push({ name: 'SpkIpdApproval' })

const load = async () => {
  loading.value = true
  try {
    const [mon, snap] = await Promise.all([
      IpdBusinessApi.getMonitor(),
      getMetricsSnapshot().catch(() => ({}))
    ])
    flows.value = ((mon as any)?.flows) || []
    const s = (mon as any)?.summary || {}
    summary.value = {
      active: s.running ?? (snap as any)?.activeFlowCount,
      blocked: s.blocked,
      waiting: s.issues,
      doneToday: s.doneToday,
      avgEvidence: s.avgEvidence
    }
    if (flows.value.length && openId.value === null) openId.value = flows.value[0].id
  } catch (e: any) {
    if (e?.message) message.error(e.message)
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.spk-filter { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.spk-filter__input { width: 220px; }
.spk-filter__select { width: 130px; }
.spk-filter__count { margin-left: auto; font-size: 12px; color: #656d76; }
.spk-empty { text-align: center; padding: 40px; color: #8c959f; font-size: 13px; }
.spk-flowlist { background: #fff; border: 1px solid #d0d7de; border-radius: 8px; overflow: hidden; }
.spk-flowrow {
  display: grid; grid-template-columns: 180px 1fr 140px 110px 90px 40px;
  align-items: center; gap: 12px; padding: 10px 16px; border-bottom: 1px solid #eaeef2;
  cursor: pointer; transition: background 0.15s;
}
.spk-flowrow:hover { background: #f6f8fa; }
.spk-flowrow--head { background: #f6f8fa; cursor: default; font-size: 12px; color: #656d76; font-weight: 600; }
.spk-flowrow--head:hover { background: #f6f8fa; }
.spk-flowrow__idno { font-family: ui-monospace, monospace; color: #0969da; font-weight: 600; font-size: 13px; }
.spk-flowrow__sub { font-size: 11px; color: #8c959f; }
.spk-flowrow__wait { font-family: ui-monospace, monospace; font-size: 12px; color: #656d76; }
.spk-flowrow__wait--bad { color: #a40e26; font-weight: 700; }
.spk-flowrow__toggle { color: #656d76; }
.spk-detail { padding: 14px 20px; background: #f6f8fa; border-bottom: 1px solid #d0d7de; }
.spk-detail__sum { display: grid; grid-template-columns: 1fr 1fr; gap: 6px 24px; margin-bottom: 12px; }
.spk-detail__row { display: flex; gap: 8px; font-size: 13px; }
.spk-detail__row--bad { color: #a40e26; }
.spk-detail__label { color: #8c959f; width: 80px; flex-shrink: 0; }
.spk-detail__actions { display: flex; gap: 8px; }
</style>
