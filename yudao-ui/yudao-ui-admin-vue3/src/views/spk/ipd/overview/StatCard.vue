<template>
  <div class="stat-card" :class="typeClass">
    <div class="flex items-center justify-between">
      <span class="text-xs text-gray-500">{{ label }}</span>
      <Icon :icon="icon" class="text-lg opacity-70" />
    </div>
    <div class="text-2xl font-bold mt-6px">{{ value }}</div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'

const props = defineProps<{ label: string; value: number | string; icon?: string; type?: string }>()
const typeClass = computed(() => `is-${props.type || 'default'}`)
</script>

<style lang="scss" scoped>
.stat-card {
  padding: 12px 14px;
  border-radius: 8px;
  background: var(--el-bg-color, #fff);
  border: 1px solid var(--el-border-color-lighter, #eee);
  transition: box-shadow 0.2s;
  &:hover { box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08); }
}
.is-primary { border-left: 3px solid var(--el-color-primary); }
.is-success { border-left: 3px solid var(--el-color-success); }
.is-warning { border-left: 3px solid var(--el-color-warning); }
.is-danger  { border-left: 3px solid var(--el-color-danger); }
</style>
