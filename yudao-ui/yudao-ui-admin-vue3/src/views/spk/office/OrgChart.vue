<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div class="text-sm text-slate-400">
        {{ TXT.segmentedBy }}
        <span class="font-medium text-slate-100">{{ currentSegmentLabel }}</span>
      </div>
      <div class="flex rounded-md overflow-hidden border border-white/10">
        <button
          v-for="m in modes"
          :key="m.key"
          type="button"
          class="px-3 py-1.5 text-xs rounded-none"
          :class="segmentMode === m.key ? 'bg-cyan-400/20 text-cyan-300' : 'bg-white/5 text-slate-300 hover:bg-white/10'"
          @click="segmentMode = m.key"
        >{{ m.label }}</button>
      </div>
    </div>

    <div v-for="[segment, members] in groups" :key="segment" class="bg-black/30 border border-white/10 rounded-lg p-5">
      <div class="flex items-center gap-2 mb-4">
        <div class="w-1 h-6 bg-cyan-400 rounded-full"></div>
        <h3 class="font-semibold text-slate-100">{{ segment }}</h3>
        <span class="text-xs text-slate-400 ml-1">({{ members.length }})</span>
      </div>
      <div class="flex flex-wrap gap-3">
        <div
          v-for="agent in members"
          :key="agent.id"
          class="flex items-center gap-2 px-3 py-2 rounded-lg border cursor-pointer transition-all hover:scale-[1.02] bg-black/40"
          :class="statusGlow[agent.status]"
          @click="emit('select', agent)"
        >
          <div class="w-8 h-8 rounded-full flex items-center justify-center text-white font-bold text-xs" :class="hashColor(agent.name)">{{ getInitials(agent.name) }}</div>
          <div>
            <div class="text-sm font-medium text-slate-100">{{ agent.name }}</div>
            <div class="flex items-center gap-1 text-xs text-slate-400">
              <span class="w-1.5 h-1.5 rounded-full" :class="statusDot[agent.status]"></span>
              {{ agent.status === 'idle' ? TXT.legendStandby : agent.status === 'busy' ? TXT.legendActive : statusLabel[agent.status] }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import type { OfficeAgent, OrgSegmentMode } from './types'
import { TXT, getInitials, hashColor, statusDot, statusGlow, statusLabel } from './office-config'
import type { useOfficeEngine } from './useOfficeEngine'

const props = defineProps<{ engine: ReturnType<typeof useOfficeEngine> }>()
const emit = defineEmits<{ (e: 'select', agent: OfficeAgent): void }>()

const segmentMode = ref<OrgSegmentMode>('category')

const modes = [
  { key: 'category' as OrgSegmentMode, label: TXT.segmentCategory },
  { key: 'role' as OrgSegmentMode, label: TXT.segmentRole },
  { key: 'status' as OrgSegmentMode, label: TXT.segmentStatus }
]

const currentSegmentLabel = computed(() => {
  const m = modes.find((i) => i.key === segmentMode.value)
  return m ? m.label : ''
})

const groups = computed(() => {
  if (segmentMode.value === 'role') return props.engine.roleGroups.value
  if (segmentMode.value === 'status') return props.engine.statusGroups.value
  return props.engine.categoryGroups.value
})
</script>
