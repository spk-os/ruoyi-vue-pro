<template>
  <div class="spk-spark" :style="{ height: height + 'px' }">
    <div
      v-for="(b, i) in bars"
      :key="i"
      class="spk-spark__bar"
      :style="barStyle(b, i)"
    />
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  /** 0-100 数值数组 */
  data: number[]
  color?: string
  height?: number
}>(), {
  color: '#0969da',
  height: 48
})

const bars = computed(() => props.data.length ? props.data : [0])

const max = computed(() => Math.max(...bars.value, 1))

const barStyle = (b: number, i: number) => {
  const h = Math.max(8, (b / max.value) * (props.height - 6))
  const opacity = 0.4 + (i / Math.max(bars.value.length - 1, 1)) * 0.6
  return {
    height: h + 'px',
    background: props.color,
    opacity: String(opacity)
  }
}
</script>

<style scoped>
.spk-spark {
  display: flex;
  align-items: flex-end;
  gap: 3px;
}
.spk-spark__bar {
  flex: 1;
  min-width: 4px;
  border-radius: 2px 2px 0 0;
  transition: height 0.3s;
}
</style>
