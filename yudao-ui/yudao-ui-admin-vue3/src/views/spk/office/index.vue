<template>
  <ContentWrap class="office-page" :body-style="{ padding: '12px' }">
    <!-- 顶部标题 + 计数 + 视图切换 + 刷新 -->
    <div class="flex items-center justify-between mb-3 flex-wrap gap-2">
      <div>
        <h2 class="text-base font-semibold text-slate-100 m-0">{{ TXT.title }}</h2>
        <p class="text-xs text-slate-400 m-0 mt-0.5">{{ TXT.subtitle }}</p>
      </div>
      <div class="flex items-center gap-2 flex-wrap">
        <div class="flex items-center gap-1 text-[11px] font-mono">
          <span class="px-2 py-1 rounded bg-amber-400/15 text-amber-300 border border-amber-400/30">{{ formatText(TXT.activeCount, { count: counts.busy }) }}</span>
          <span class="px-2 py-1 rounded bg-emerald-400/15 text-emerald-300 border border-emerald-400/30">{{ formatText(TXT.standbyCount, { count: counts.idle }) }}</span>
          <span class="px-2 py-1 rounded bg-rose-500/15 text-rose-300 border border-rose-500/30">{{ formatText(TXT.alertCount, { count: counts.error }) }}</span>
          <span class="px-2 py-1 rounded bg-slate-500/15 text-slate-300 border border-slate-500/30">{{ formatText(TXT.offlineCount, { count: counts.offline }) }}</span>
        </div>
        <div class="flex rounded-md overflow-hidden border border-white/10">
          <button
            type="button"
            class="px-3 py-1.5 text-xs"
            :class="engine.viewMode.value === 'office' ? 'bg-cyan-400/20 text-cyan-300' : 'bg-white/5 text-slate-300 hover:bg-white/10'"
            @click="engine.viewMode.value = 'office'"
          >{{ TXT.buttonDeck }}</button>
          <button
            type="button"
            class="px-3 py-1.5 text-xs"
            :class="engine.viewMode.value === 'org-chart' ? 'bg-cyan-400/20 text-cyan-300' : 'bg-white/5 text-slate-300 hover:bg-white/10'"
            @click="engine.viewMode.value = 'org-chart'"
          >{{ TXT.buttonCrewChart }}</button>
        </div>
        <el-button size="small" :loading="loading" @click="loadAgents">
          <Icon icon="ep:refresh" class="mr-1" />{{ TXT.refresh }}
        </el-button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading && displayAgents.length === 0" class="text-center text-slate-400 py-12 text-sm">{{ TXT.loadingOffice }}</div>
    <!-- 空状态 -->
    <div v-else-if="displayAgents.length === 0" class="text-center py-12">
      <p class="text-lg text-slate-300">{{ TXT.emptyDeck }}</p>
      <p class="text-sm text-slate-400 mt-1">{{ TXT.emptyDeckSubtitle }}</p>
    </div>
    <!-- 平面图视图 -->
    <OfficeFloor
      v-else-if="engine.viewMode.value === 'office'"
      :engine="engine"
      :theme="theme"
      @select="onSelectAgent"
    />
    <!-- 组织架构视图 -->
    <OrgChart v-else :engine="engine" @select="onSelectAgent" />

    <!-- 成员详情弹窗 -->
    <AgentModal :engine="engine" :agent="selectedAgent" @update:agent="selectedAgent = $event" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, onMounted, onUnmounted, ref, shallowRef } from 'vue'
import * as AgentDefApi from '@/api/spk/agentdef'
import type { AgentStatus, OfficeAgent } from './types'
import { TXT, formatText } from './office-config'
import { useOfficeEngine } from './useOfficeEngine'
import { useTimeTheme } from './useTimeTheme'
import OfficeFloor from './OfficeFloor.vue'
import OrgChart from './OrgChart.vue'
import AgentModal from './AgentModal.vue'

defineOptions({ name: 'SpkOffice' })

const loading = ref(true)
const displayAgents = shallowRef<OfficeAgent[]>([])

const engine = useOfficeEngine(displayAgents as any)
const theme = useTimeTheme()

const selectedAgent = ref<OfficeAgent | null>(null)
const onSelectAgent = (agent: OfficeAgent) => { selectedAgent.value = agent }

const counts = computed(() => engine.counts.value)

const VALID_STATUS: AgentStatus[] = ['offline', 'idle', 'busy', 'error']

const mapAgent = (vo: AgentDefApi.AgentDefVO): OfficeAgent => {
  const raw = vo.status
  const status: AgentStatus = (VALID_STATUS as string[]).includes(raw || '') ? (raw as AgentStatus) : 'offline'
  const created = vo.createTime ? Math.floor(new Date(vo.createTime).getTime() / 1000) : 0
  return {
    id: vo.id!,
    name: vo.name,
    role: vo.role || '',
    status,
    lastSeen: created,
    lastActivity: vo.lastActivity || '',
    sessionKey: vo.sessionKey || '',
    model: vo.model,
    runtimeType: vo.runtimeType
  }
}

const loadAgents = async () => {
  loading.value = true
  try {
    const list = await AgentDefApi.getAgentDefList()
    displayAgents.value = (list || []).map(mapAgent)
  } finally {
    loading.value = false
  }
}

let pollTimer: ReturnType<typeof setInterval> | null = null
onMounted(() => {
  loadAgents()
  pollTimer = setInterval(loadAgents, 15_000)
})
onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.office-page :deep(.el-card__body) {
  background: #0a0f18;
}
</style>
