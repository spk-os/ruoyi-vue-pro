<template>
  <span class="spk-fresh" :style="{ color: textColor }">
    <span class="spk-fresh__dot" :style="{ background: dotColor }" />
    {{ text }}
  </span>
</template>

<script lang="ts" setup>
import { computed } from 'vue'

const props = defineProps<{
  /** 数据新鲜度：分钟数；传 'live' 表示实时 */
  minutes?: number | string
  label?: string
}>()

const { dotColor, textColor, text } = computed(() => {
  if (props.minutes === 'live' || props.label === '实时') {
    return { dotColor: '#4ac26b', textColor: '#656d76', text: '实时' }
  }
  const m = typeof props.minutes === 'number' ? props.minutes : 999
  if (m <= 5) return { dotColor: '#4ac26b', textColor: '#656d76', text: '刚刚更新' }
  if (m <= 30) return { dotColor: '#4ac26b', textColor: '#656d76', text: `${m} 分钟前更新` }
  if (m <= 60) return { dotColor: '#d4a72c', textColor: '#6e5b02', text: `${m} 分钟前更新` }
  return { dotColor: '#ff8182', textColor: '#a40e26', text: `已过期（${m} 分钟）` }
}).value
</script>

<style scoped>
.spk-fresh {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
}
.spk-fresh__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
