<template>
  <div class="spk-evi">
    <div class="spk-evi__bar">
      <div class="spk-evi__fill" :style="{ width: pct + '%', background: color }" />
    </div>
    <span class="spk-evi__pct">{{ pct }}%</span>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
const props = defineProps<{ pct: number }>()
const color = computed(() => {
  if (props.pct >= 80) return '#4ac26b'
  if (props.pct >= 50) return '#d4a72c'
  return '#ff8182'
})
</script>

<style scoped>
.spk-evi { display: flex; align-items: center; gap: 6px; }
.spk-evi__bar { flex: 1; height: 6px; background: #eaeef2; border-radius: 3px; overflow: hidden; min-width: 60px; }
.spk-evi__fill { height: 100%; border-radius: 3px; transition: width 0.3s; }
.spk-evi__pct { font-size: 11px; font-family: ui-monospace, monospace; color: #656d76; width: 32px; text-align: right; }
</style>
