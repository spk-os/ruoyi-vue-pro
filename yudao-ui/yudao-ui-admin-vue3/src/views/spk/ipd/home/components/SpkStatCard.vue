<template>
  <div class="spk-stat" :class="{ 'spk-stat--clickable': clickable }">
    <div class="spk-stat__label">{{ label }}</div>
    <div class="spk-stat__value" :style="valueStyle">{{ value }}</div>
    <div v-if="delta || delta === 0" class="spk-stat__delta" :class="deltaClass">
      <span v-if="deltaTrend === 'up'">↑</span>
      <span v-else-if="deltaTrend === 'down'">↓</span>
      {{ deltaText }}
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'

const props = defineProps<{
  label: string
  value: string | number
  delta?: string | number | null
  deltaTrend?: 'up' | 'down' | 'neutral'
  /** 正向语义：up 为好（绿）/ down 为坏；负向语义反过来。默认按 trend+good 判断色。 */
  goodWhen?: 'up' | 'down'
  danger?: boolean
  clickable?: boolean
}>()

const valueStyle = computed(() =>
  props.danger ? { color: '#a40e26' } : {}
)

const deltaClass = computed(() => {
  const t = props.deltaTrend
  if (!t || t === 'neutral') return 'spk-stat__delta--neutral'
  const goodWhen = props.goodWhen || 'up'
  const isGood = t === goodWhen
  return isGood ? 'spk-stat__delta--up' : 'spk-stat__delta--down'
})

const deltaText = computed(() => {
  if (props.delta === undefined || props.delta === null) return ''
  return typeof props.delta === 'number' ? `${props.delta > 0 ? '+' : ''}${props.delta}` : props.delta
})
</script>

<style scoped>
.spk-stat {
  background: #fff;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.spk-stat--clickable {
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.spk-stat--clickable:hover {
  border-color: #54aeff;
  box-shadow: 0 2px 8px rgba(9, 105, 218, 0.12);
}
.spk-stat__label {
  font-size: 12px;
  color: #656d76;
  font-weight: 600;
}
.spk-stat__value {
  font-size: 24px;
  font-weight: 700;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: #1f2328;
  line-height: 1.2;
}
.spk-stat__delta {
  font-size: 12px;
  font-weight: 500;
}
.spk-stat__delta--up {
  color: #116329;
}
.spk-stat__delta--down {
  color: #a40e26;
}
.spk-stat__delta--neutral {
  color: #656d76;
}
</style>
