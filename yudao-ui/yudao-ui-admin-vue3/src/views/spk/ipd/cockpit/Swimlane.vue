<!--
  IPD 流程图：自上而下 6 阶段，每阶段一行卡片，卡片间 → 串接、阶段间 ↓ 串接。
  卡片显示实际内容（activity def 中文名），不显示 ACT-xx 编号。
  状态色：queued 灰 / running 蓝 / done 绿 / failed 红 / timeout 橙 / cancelled 紫。
  顶部按流程实例筛选；卡片点击 → 抽屉式节点详情。轮询 5s 刷新。
-->
<template>
  <div class="swimlane" v-loading="loading">
    <div class="filter-bar">
      <el-input
        v-model="processInstanceId"
        placeholder="输入流程实例编号（processInstanceId）"
        clearable
        style="width: 360px"
        @keyup.enter="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-switch v-model="autoRefresh" active-text="自动刷新(5s)" />
      <span class="meta">共 {{ total }} 个 Activity</span>
    </div>

    <el-alert v-if="errorMsg" type="error" :title="errorMsg" :closable="false" show-icon />
    <el-empty v-else-if="!loading && total === 0" description="暂无 Activity（流程尚未推进或编号有误）" />

    <div v-else class="flow">
      <template v-for="(stage, si) in orderedStages" :key="stage.name">
        <div class="lane">
          <div class="lane-title">
            <span class="stage-name">{{ stage.name }}</span>
            <span class="stage-count">{{ stage.cards.length }}</span>
          </div>
          <div class="lane-body">
            <template v-for="(c, ci) in stage.cards" :key="c.activityRunId">
              <div
                class="card"
                :class="statusClass(c.status)"
                @click="emit('show-detail', c.activityRunId)"
              >
                <div class="card-title">{{ c.name || c.activityId }}</div>
                <div class="card-sub">Lead：{{ c.leadAgentCode || '-' }}</div>
                <div class="card-tags">
                  <el-tag size="small" :type="statusTagType(c.status)">{{ statusLabel(c.status) }}</el-tag>
                  <el-tag v-if="c.verificationConclusion" size="small" :type="verdictType(c.verificationConclusion)">
                    {{ c.verificationConclusion }}
                  </el-tag>
                  <el-tag size="small" type="info">产物 {{ c.artifactCount }}</el-tag>
                </div>
                <div class="card-time">{{ fmt(c.queuedAt) }} → {{ fmt(c.finishedAt) }}</div>
              </div>
              <span v-if="ci < stage.cards.length - 1" class="arrow-h">→</span>
            </template>
            <span v-if="stage.cards.length === 0" class="empty-cell">—</span>
          </div>
        </div>
        <div v-if="si < orderedStages.length - 1" class="arrow-v">↓</div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { getSwimlane } from '@/api/spk/ipd/cockpit'

const props = defineProps<{ externalPid?: string }>()

const emit = defineEmits<{ (e: 'show-detail', activityRunId: string): void }>()

const processInstanceId = ref('')

// 外部注入流程实例编号（项目 Cockpit 总览 tab 复用本组件时用）
watch(
  () => props.externalPid,
  (v) => {
    if (v) {
      processInstanceId.value = v
      load()
    }
  }
)
const loading = ref(false)
const errorMsg = ref('')
const stages = ref<Record<string, any[]>>({})
const total = ref(0)
const autoRefresh = ref(false)
let timer: any = null

const STAGE_ORDER = ['concept', 'plan', 'develop', 'qualify', 'launch', 'lifecycle', 'support', 'unknown']
const STAGE_LABEL: Record<string, string> = {
  concept: '概念', plan: '计划', develop: '开发', qualify: '验证',
  launch: '发布', lifecycle: '生命周期', support: '支撑', unknown: '未归类'
}

const orderedStages = computed(() =>
  STAGE_ORDER.filter((s) => stages.value[s]?.length).map((s) => ({
    name: STAGE_LABEL[s] || s,
    cards: stages.value[s] || []
  }))
)

const load = async () => {
  if (!processInstanceId.value) {
    errorMsg.value = '请输入流程实例编号'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const data: any = await getSwimlane(processInstanceId.value)
    stages.value = data?.stages || {}
    total.value = data?.total || 0
  } catch (e: any) {
    errorMsg.value = e?.message || '查询失败'
    stages.value = {}
    total.value = 0
  } finally {
    loading.value = false
  }
}

const statusLabel = (s?: string) => {
  const m: Record<string, string> = {
    queued: '已入队', running: '执行中', done: '完成', failed: '失败',
    timeout: '超时', cancelled: '已取消'
  }
  return m[s || ''] || s || '-'
}
const statusClass = (s?: string) => `st-${s || 'unknown'}`
const statusTagType = (s?: string): any => {
  const m: Record<string, any> = {
    queued: 'info', running: 'warning', done: 'success', failed: 'danger',
    timeout: 'warning', cancelled: 'info'
  }
  return m[s || ''] || 'info'
}
const verdictType = (v?: string): any => {
  if (!v) return 'info'
  if (v === 'PASS') return 'success'
  if (v === 'FAIL') return 'danger'
  return 'warning' // CONDITIONAL
}
const fmt = (t?: string) => (t ? String(t).slice(5, 16).replace('T', ' ') : '—')

onMounted(() => {
  if (processInstanceId.value) load()
  // 自动刷新开关变化时启停
  const tick = () => {
    if (autoRefresh.value && processInstanceId.value) load()
  }
  timer = setInterval(tick, 5000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.meta {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.lanes,
.flow {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.arrow-h {
  align-self: center;
  color: var(--el-text-color-placeholder);
  font-size: 16px;
}
.arrow-v {
  text-align: center;
  color: var(--el-text-color-placeholder);
  font-size: 16px;
  line-height: 1;
  margin: -2px 0;
}
.lane {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: var(--el-bg-color-page);
}
.lane-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: var(--el-fill-color-light);
  border-radius: 6px 6px 0 0;
  font-weight: 600;
}
.stage-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.lane-body {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px;
}
.card {
  width: 220px;
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  cursor: pointer;
  transition: box-shadow 0.15s;
}
.card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
.card-title {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 4px;
}
.card-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 4px;
}
.card-time {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.st-done {
  border-left: 3px solid var(--el-color-success);
}
.st-running {
  border-left: 3px solid var(--el-color-primary);
}
.st-failed {
  border-left: 3px solid var(--el-color-danger);
}
.st-timeout {
  border-left: 3px solid var(--el-color-warning);
}
.st-queued,
.st-cancelled {
  border-left: 3px solid var(--el-color-info);
}
.empty-cell {
  color: var(--el-text-color-placeholder);
  padding: 6px;
}
</style>
