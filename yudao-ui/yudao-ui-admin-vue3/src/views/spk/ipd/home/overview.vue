<!--
  研发驾驶舱 · 全景总览（设计文档 §4.1 / 原型 overview.html）
  回答"整体是否按计划交付"。区块固定顺序：
  1 组合健康摘要 2 需要关注+路线图 3 健康度矩阵+交付漏斗 4 集成健康 5 实时事件流
  数据：IpdBusinessApi.getOverview() + getMonitor()(集成)。数据不足显式"未接入"，不用 0 冒充（§4.4）。
-->
<template>
  <div v-loading="loading" class="spk-overview">
    <!-- 1. 组合健康摘要 -->
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-10px mb-16px">
      <SpkStatCard
        v-for="c in kpis"
        :key="c.label"
        :label="c.label"
        :value="c.value"
        :delta="c.delta"
        :delta-trend="c.trend"
        :good-when="c.goodWhen"
        :danger="c.danger"
      />
    </div>

    <!-- 2. 需要关注 + 路线图 -->
    <div class="grid grid-cols-1 lg:grid-cols-[1fr_420px] gap-16px mb-16px">
      <div class="spk-card">
        <div class="spk-card__header">
          <div class="spk-card__title">
            需要关注
            <SpkBadge v-if="attCount" :meta="{ color: 'red', text: attCount + ' 项' }" />
          </div>
          <router-link :to="{ name: 'SpkIpdHome', query: { view: 'actions' } }" class="spk-card__link">
            在协作中心查看全部 →
          </router-link>
        </div>
        <div class="spk-card__body">
          <el-empty v-if="!attCount" description="暂无待办，所有流程顺畅" :image-size="60" />
          <div v-else class="spk-att-list">
            <SpkActionItem
              v-for="(a, i) in attentionTop"
              :key="i"
              :item="a"
              @act="onAct"
            />
            <div v-if="attCount > attentionTop.length" class="spk-att-more">
              显示 {{ attentionTop.length }} / {{ attCount }} 项 ·
              <router-link :to="{ name: 'SpkIpdHome', query: { view: 'actions' } }">查看全部 →</router-link>
            </div>
          </div>
        </div>
      </div>

      <div class="spk-card">
        <div class="spk-card__header">
          <div class="spk-card__title">路线图与里程碑</div>
        </div>
        <div class="spk-card__body spk-card__body--flush">
          <el-empty v-if="!roadmap.length" description="暂无活跃项目路线图" :image-size="50" />
          <div v-else class="spk-roadmap">
            <div v-for="p in roadmap" :key="p.projectId" class="spk-roadmap__item" @click="openProject(p.projectId)">
              <div class="spk-roadmap__head">
                <b class="spk-roadmap__name">{{ p.projectName }}</b>
                <SpkBadge :map="healthMap" :value="p.health" fallback="—" />
              </div>
              <div v-for="m in p.majors" :key="m.majorReleaseId" class="spk-roadmap__major">
                <div class="spk-roadmap__major-head">
                  V{{ m.majorNo }} · {{ m.name }}
                  <SpkBadge :map="statusMap" :value="m.status" />
                </div>
                <div class="spk-roadmap__versions">
                  <el-tag v-for="v in m.versions" :key="v.versionId" size="small" effect="plain">
                    {{ v.versionNo }}
                  </el-tag>
                  <span v-if="!m.versions?.length" class="spk-roadmap__empty">无版本</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 健康度矩阵 + 交付漏斗 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-16px mb-16px">
      <div class="spk-card">
        <div class="spk-card__header">
          <div class="spk-card__title">项目 × 阶段 健康度矩阵</div>
          <router-link :to="{ name: 'SpkIpdHome', query: { view: 'monitor' } }" class="spk-card__link">打开运行监控 →</router-link>
        </div>
        <div class="spk-card__body spk-card__body--flush">
          <el-empty v-if="!matrixRows.length" description="暂无健康度数据" :image-size="50" />
          <table v-else class="spk-matrix">
            <thead>
              <tr>
                <th class="spk-matrix__name-col">项目</th>
                <th v-for="s in STAGES" :key="s">{{ s }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in matrixRows" :key="r.name">
                <td class="spk-matrix__name-col">{{ r.name }}</td>
                <td v-for="(c, i) in r.cells" :key="i" class="spk-matrix__cell" :class="'spk-matrix__cell--' + c"">
                  <span class="spk-matrix__mark">{{ matrixMark(c) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="spk-card">
        <div class="spk-card__header">
          <div class="spk-card__title">交付漏斗</div>
          <router-link :to="{ name: 'SpkIpdHome', query: { view: 'analytics' } }" class="spk-card__link">分析看板 →</router-link>
        </div>
        <div class="spk-card__body">
          <div v-if="!funnel.length" class="spk-empty-inline">数据未接入</div>
          <div v-else class="spk-funnel">
            <div v-for="(s, i) in funnel" :key="s.label" class="spk-funnel__stage" :class="'spk-funnel__stage--' + i">
              <div class="spk-funnel__label">{{ s.label }}</div>
              <div class="spk-funnel__value">{{ s.value }}</div>
            </div>
          </div>
          <div class="spk-funnel__summary">
            <span>转化率 <b>{{ convRate }}%</b></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. 集成健康度 -->
    <div class="spk-card mb-16px">
      <div class="spk-card__header">
        <div class="spk-card__title">集成健康度</div>
        <span class="spk-card__sub">所有外部连接状态</span>
      </div>
      <div class="spk-card__body">
        <el-empty v-if="!integrations.length" description="暂无集成状态" :image-size="40" />
        <div v-else class="spk-integ-strip">
          <div v-for="it in integrations" :key="it.name" class="spk-integ-chip">
            <span class="spk-integ__dot" :class="it.healthy ? 'spk-integ__dot--ok' : 'spk-integ__dot--bad'" />
            <span class="spk-integ__name">{{ it.name }}</span>
            <SpkBadge :meta="{ color: it.healthy ? 'green' : 'yellow', text: it.healthy ? '正常' : '异常' }" />
            <span v-if="it.detail" class="spk-integ__detail">{{ it.detail }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 5. 实时事件流 -->
    <div class="spk-card">
      <div class="spk-card__header">
        <div class="spk-card__title">实时事件流</div>
        <SpkFreshness minutes="live" />
      </div>
      <div class="spk-card__body">
        <el-empty v-if="!events.length" description="暂无实时事件" :image-size="50" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="(e, i) in events"
            :key="i"
            :type="eventColor(e)"
            :timestamp="e.time"
            placement="top"
          >
            <div class="spk-event__title">{{ e.title }}</div>
            <div v-if="e.meta" class="spk-event__meta">{{ e.meta }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { formatDate } from '@/utils/formatTime'
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import SpkStatCard from './components/SpkStatCard.vue'
import SpkBadge from './components/SpkBadge.vue'
import SpkActionItem from './components/SpkActionItem.vue'
import SpkFreshness from './components/SpkFreshness.vue'
import { healthMap, statusMap, flowTypeMap, labelText } from './components/status'

defineOptions({ name: 'SpkIpdHomeOverview' })

const STAGES = ['概念', '计划', '开发', '验证', '发布', '生命周期']
const message = useMessage()
const { push } = useRouter()
const loading = ref(true)
const data = ref<IpdBusinessApi.SpkIpdOverviewVO>({})
const integrations = ref<IpdBusinessApi.SpkIpdMonitorIntegration[]>([])

const totalOf = (m?: Record<string, number>) => (m ? Object.values(m).reduce((a, b) => a + b, 0) : 0)
const flowTypeLabel = (s?: string) => labelText(flowTypeMap, s)
const statusLabel = (s?: string) => labelText(statusMap, s)

// —— KPI ——
const kpis = computed(() => {
  const pc = data.value.projectCounts || {}
  const activeProjects = pc.ACTIVE ?? 0
  const vc = data.value.versionCounts
  const hasVer = vc && Object.keys(vc).length
  const activeVersions = hasVer ? (vc.IN_PROGRESS ?? 0) + (vc.VERIFYING ?? 0) : null
  const fhc = data.value.flowHealthCounts || {}
  const good = fhc.GOOD ?? 0
  const fhcTotal = totalOf(fhc)
  const normalRate = fhcTotal ? Math.round((good / fhcTotal) * 100) : null
  return [
    { label: '活跃项目', value: activeProjects, delta: null, trend: 'neutral' as const, goodWhen: 'up' as const },
    { label: '活跃版本', value: activeVersions === null ? '未接入' : activeVersions, delta: null, trend: 'neutral' as const, goodWhen: 'up' as const },
    { label: '正常率', value: normalRate === null ? '未接入' : normalRate + '%', danger: false, delta: null, trend: 'neutral' as const, goodWhen: 'up' as const },
    { label: '阻塞流程', value: data.value.blockedFlowCount ?? 0, danger: (data.value.blockedFlowCount ?? 0) > 0, delta: null, trend: 'neutral' as const, goodWhen: 'down' as const },
    { label: '待决策', value: data.value.openIssueCount ?? att.value.length, delta: null, trend: 'neutral' as const, goodWhen: 'down' as const },
    { label: '追溯覆盖率', value: '未接入', delta: null, trend: 'neutral' as const, goodWhen: 'up' as const }
  ]
})

// —— 需要关注 ——
const att = computed(() => data.value.attentionItems || [])
const attCount = computed(() => att.value.length)
const attentionTop = computed(() => att.value.slice(0, 5))
const onAct = (item: IpdBusinessApi.SpkIpdOverviewAttentionItem) => {
  if (item.type === 'BLOCKED_FLOW' || item.type === 'PENDING_DECISION') {
    push({ name: 'SpkIpdHome', query: { view: 'monitor' } })
  } else if (item.refId) {
    push({ name: 'SpkIpdProjectDetail', query: { projectId: item.refId } })
  } else {
    push({ name: 'SpkIpdHome', query: { view: 'actions' } })
  }
}

// —— 路线图 ——
const roadmap = computed(() => (data.value.roadmap || []) as any[])
const openProject = (id?: number) => id && push({ name: 'SpkIpdProjectDetail', query: { projectId: id } })

// —— 健康度矩阵（从 roadmap 项目 + flowHealth 派生；逐阶段数据未接入时按项目健康上色首阶段）——
const matrixRows = computed(() => {
  const rows = (data.value.roadmap || []) as any[]
  return rows.map((p: any) => {
    const cells = Array(STAGES.length).fill('none')
    const health = p.health || 'GOOD'
    const colorKey = healthMap[health]?.color || 'gray'
    // 仅在当前阶段上色（stageIndex 若有）
    const idx = typeof p.currentStageIndex === 'number' ? p.currentStageIndex : 0
    cells[idx] = colorKey === 'gray' ? 'idle' : colorKey
    return { name: p.projectName || p.projectCode || `PRJ-${p.projectId}`, cells }
  })
})
const matrixMark = (c: string) => (c === 'none' || c === 'idle' ? '' : '●')

// —— 交付漏斗（从 flowRunCounts 派生）——
const funnel = computed(() => {
  const fc = data.value.flowRunCounts || {}
  if (!Object.keys(fc).length) return []
  return [
    { label: '已入池', value: totalOf(fc) },
    { label: '已承诺', value: (fc.COMMITTED ?? 0) + (fc.PLANNING ?? 0) },
    { label: '开发中', value: (fc.RUNNING ?? 0) },
    { label: '验证中', value: (fc.VERIFYING ?? 0) },
    { label: '已交付', value: (fc.COMPLETED ?? 0) + (fc.DELIVERED ?? 0) }
  ]
})
const convRate = computed(() => {
  if (!funnel.value.length) return '—'
  const head = funnel.value[0].value || 0
  const tail = funnel.value[funnel.value.length - 1].value || 0
  return head ? Math.round((tail / head) * 100) : '—'
})

// —— 实时事件流（从 recentFlows + attentionItems 派生）——
const events = computed(() => {
  const evs: { time: string; title: string; meta?: string }[] = []
  for (const f of data.value.recentFlows || []) {
    evs.push({ time: f.startedAt ? formatDate(f.startedAt, 'MM-DD HH:mm') : '', title: `${f.runNo || f.flowRunId || ''} ${flowTypeLabel(f.flowType)} ${statusLabel(f.status)}`, meta: f.projectName })
  }
  for (const a of (data.value.attentionItems || []).slice(0, 3)) {
    evs.push({ time: '', title: a.title, meta: a.detail })
  }
  return evs.slice(0, 8)
})
const eventColor = (e: any) => {
  if (e.title?.includes('阻塞') || e.title?.includes('失败')) return 'danger'
  if (e.title?.includes('待') || e.title?.includes('等待')) return 'warning'
  if (e.title?.includes('完成') || e.title?.includes('通过')) return 'success'
  return 'primary'
}

// —— 装载 ——
const load = async () => {
  loading.value = true
  try {
    const [ov, mon] = await Promise.all([
      IpdBusinessApi.getOverview(),
      IpdBusinessApi.getMonitor().catch(() => ({}))
    ])
    data.value = (ov || {}) as IpdBusinessApi.SpkIpdOverviewVO
    integrations.value = ((mon as any)?.integrations) || []
  } catch (e: any) {
    if (e?.message) message.error(e.message)
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.spk-overview { }
.spk-card {
  background: #fff;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}
.spk-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #eaeef2;
}
.spk-card__title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2328;
  display: flex;
  align-items: center;
  gap: 8px;
}
.spk-card__sub {
  font-size: 12px;
  color: #8c959f;
}
.spk-card__link {
  font-size: 12px;
  color: #0969da;
  text-decoration: none;
}
.spk-card__link:hover { text-decoration: underline; }
.spk-card__body { padding: 12px 16px; }
.spk-card__body--flush { padding: 0; }
.spk-att-list { display: flex; flex-direction: column; gap: 8px; }
.spk-att-more { font-size: 12px; color: #656d76; margin-top: 8px; }
.spk-roadmap { display: flex; flex-direction: column; }
.spk-roadmap__item {
  padding: 10px 16px;
  border-bottom: 1px solid #eaeef2;
  cursor: pointer;
}
.spk-roadmap__item:hover { background: #f6f8fa; }
.spk-roadmap__item:last-child { border-bottom: none; }
.spk-roadmap__head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.spk-roadmap__name { font-size: 14px; }
.spk-roadmap__major { margin: 4px 0 8px; }
.spk-roadmap__major-head { font-size: 12px; color: #656d76; display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.spk-roadmap__versions { display: flex; flex-wrap: wrap; gap: 4px; }
.spk-roadmap__empty { font-size: 12px; color: #8c959f; }
.spk-matrix { width: 100%; border-collapse: collapse; font-size: 12px; }
.spk-matrix th, .spk-matrix td { padding: 6px 8px; text-align: center; border-bottom: 1px solid #eaeef2; }
.spk-matrix th { background: #f6f8fa; color: #656d76; font-weight: 600; }
.spk-matrix__name-col { text-align: left; width: 130px; font-weight: 500; }
.spk-matrix__cell { }
.spk-matrix__cell--green { background: #dafbe1; color: #116329; }
.spk-matrix__cell--yellow { background: #fff8c5; color: #6e5b02; }
.spk-matrix__cell--red { background: #ffebe9; color: #a40e26; }
.spk-matrix__cell--blue { background: #ddf4ff; color: #0550ae; }
.spk-matrix__cell--purple { background: #fbefff; color: #6639ba; }
.spk-matrix__mark { font-size: 14px; }
.spk-funnel { display: flex; gap: 8px; }
.spk-funnel__stage {
  flex: 1; padding: 12px 8px; text-align: center; border-radius: 6px;
  position: relative;
}
.spk-funnel__stage--0 { background: #f6f8fa; }
.spk-funnel__stage--1 { background: #ddf4ff; }
.spk-funnel__stage--2 { background: #fbefff; }
.spk-funnel__stage--3 { background: #fff8c5; }
.spk-funnel__stage--4 { background: #dafbe1; }
.spk-funnel__label { font-size: 12px; color: #656d76; }
.spk-funnel__value { font-size: 18px; font-weight: 700; font-family: ui-monospace, monospace; }
.spk-funnel__summary { margin-top: 10px; font-size: 12px; color: #656d76; }
.spk-empty-inline { font-size: 13px; color: #8c959f; text-align: center; padding: 20px; }
.spk-integ-strip { display: flex; flex-wrap: wrap; gap: 10px; }
.spk-integ-chip {
  display: flex; align-items: center; gap: 6px; padding: 6px 10px;
  border: 1px solid #d0d7de; border-radius: 6px; background: #f6f8fa;
}
.spk-integ__dot { width: 7px; height: 7px; border-radius: 50%; }
.spk-integ__dot--ok { background: #4ac26b; }
.spk-integ__dot--bad { background: #fb8f44; }
.spk-integ__name { font-size: 13px; font-weight: 500; }
.spk-integ__detail { font-size: 11px; color: #8c959f; }
.spk-event__title { font-size: 13px; color: #1f2328; }
.spk-event__meta { font-size: 12px; color: #656d76; margin-top: 2px; }
</style>
