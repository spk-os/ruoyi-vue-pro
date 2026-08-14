<template>
  <span class="spk-badge" :style="badgeStyle">
    <span class="spk-badge__dot" :style="dotStyle" />
    {{ meta.text }}
  </span>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { colorVars, resolveBadge, type BadgeMeta } from './status'

const props = defineProps<{
  meta?: BadgeMeta // 直接给 meta（优先）
  map?: Record<string, BadgeMeta> // 或给 map+key 查表
  value?: string // 查表 key
  fallback?: string
}>()

const meta = computed<BadgeMeta>(() => {
  if (props.meta) return props.meta
  if (props.map && props.value !== undefined) {
    return resolveBadge(props.map, props.value, props.fallback)
  }
  return { color: 'gray', text: props.fallback || props.value || '—' }
})

const badgeStyle = computed(() => {
  const v = colorVars[meta.value.color]
  return { color: v.fg, background: v.bg, borderColor: v.border }
})
const dotStyle = computed(() => ({ background: colorVars[meta.value.color].fg }))
</script>

<style scoped>
.spk-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 1px 8px;
  border: 1px solid;
  border-radius: 999px;
  font-size: 12px;
  line-height: 18px;
  font-weight: 500;
  white-space: nowrap;
}
.spk-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
