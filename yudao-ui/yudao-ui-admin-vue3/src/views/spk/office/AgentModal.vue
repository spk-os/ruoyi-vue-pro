<template>
  <el-dialog
    v-model="visible"
    :title="agent ? agent.name : ''"
    width="420px"
    append-to-body
    class="office-agent-modal"
  >
    <template v-if="agent">
      <div class="flex items-start gap-3 mb-4">
        <div
          class="w-14 h-14 rounded-full flex items-center justify-center text-white font-bold text-lg ring-2 ring-offset-2 ring-offset-[#0f1623]"
          :class="[hashColor(agent.name), ringClass]"
        >{{ getInitials(agent.name) }}</div>
        <div class="flex-1">
          <h3 class="text-lg font-bold text-slate-100">{{ agent.name }}</h3>
          <p class="text-sm text-slate-400">{{ agent.role || '未分配' }}</p>
          <p class="text-[11px] text-slate-500 mt-1">
            {{ TXT.sessionLabel }}
            <code class="font-mono">{{ agent.sessionKey || '—' }}</code>
          </p>
        </div>
      </div>

      <div class="space-y-3 text-sm">
        <div class="flex items-center gap-2">
          <span class="w-3 h-3 rounded-full" :class="statusDot[agent.status]"></span>
          <span class="font-medium text-slate-100">{{ statusLabel[agent.status] }}</span>
          <span class="text-slate-400 ml-auto">{{ formatLastSeen(agent.lastSeen) }}</span>
        </div>

        <div v-if="agent.lastActivity" class="bg-white/5 rounded-lg p-3">
          <span class="text-xs text-slate-400 block mb-1">{{ TXT.currentActivity }}</span>
          <span class="text-slate-200 text-sm">{{ agent.lastActivity }}</span>
        </div>

        <div v-if="agent.model || agent.runtimeType" class="grid grid-cols-2 gap-2 text-[11px]">
          <div class="bg-white/5 rounded-lg p-2">
            <div class="text-slate-400">运行时</div>
            <div class="text-slate-200 font-mono">{{ agent.runtimeType || '—' }}</div>
          </div>
          <div class="bg-white/5 rounded-lg p-2">
            <div class="text-slate-400">模型</div>
            <div class="text-slate-200 font-mono truncate">{{ agent.model || '—' }}</div>
          </div>
        </div>

        <div class="pt-1">
          <div class="text-[10px] uppercase tracking-wider text-slate-400 mb-1.5">{{ TXT.quickActions }}</div>
          <div class="grid grid-cols-3 gap-1.5">
            <el-button size="small" @click="run('focus')">{{ TXT.actionFocus }}</el-button>
            <el-button size="small" @click="run('pair')">{{ TXT.actionPair }}</el-button>
            <el-button size="small" @click="run('break')">{{ TXT.actionBreak }}</el-button>
          </div>
        </div>

        <div class="pt-1">
          <el-button size="small" disabled class="w-full! text-xs">指挥台 Companion（未安装）</el-button>
          <div class="text-[10px] text-slate-500 mt-1">本地 Companion 客户端尚未安装，仅显示占位入口。</div>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { OfficeAction, OfficeAgent } from './types'
import { TXT, formatLastSeen, getInitials, hashColor, statusDot, statusLabel } from './office-config'
import type { useOfficeEngine } from './useOfficeEngine'

const props = defineProps<{
  engine: ReturnType<typeof useOfficeEngine>
  agent: OfficeAgent | null
}>()
const emit = defineEmits<{ (e: 'update:agent', v: OfficeAgent | null): void }>()

const visible = computed({
  get: () => props.agent != null,
  set: (v: boolean) => { if (!v) emit('update:agent', null) }
})

const ringClass = computed(() => {
  if (!props.agent) return ''
  const s = props.agent.status
  if (s === 'busy') return 'ring-amber-500'
  if (s === 'idle') return 'ring-emerald-500'
  if (s === 'error') return 'ring-rose-500'
  return 'ring-slate-500'
})

const run = (action: OfficeAction) => {
  if (props.agent) props.engine.executeAgentAction(props.agent, action)
}
</script>
