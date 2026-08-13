<template>
  <div class="dist-row flex items-center mb-6px">
    <span class="label w-100px text-xs text-gray-500 truncate">{{ label }}</span>
    <div class="bar-wrap flex-1 mx-8px">
      <div class="bar" :style="{ width: pct + '%' }" />
    </div>
    <span class="value w-40px text-right text-xs font-mono">{{ value }}</span>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
const props = defineProps<{ label: string; value: number; total: number }>()
const pct = computed(() => (props.total > 0 ? Math.round((props.value / props.total) * 100) : 0))
</script>

<style lang="scss" scoped>
.bar-wrap { height: 8px; background: var(--el-fill-color-light, #f5f5f5); border-radius: 4px; overflow: hidden; }
.bar { height: 100%; background: var(--el-color-primary); border-radius: 4px; transition: width 0.3s; }
</style>
