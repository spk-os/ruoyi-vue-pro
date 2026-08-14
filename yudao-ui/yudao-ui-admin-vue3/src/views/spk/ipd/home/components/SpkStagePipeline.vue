<template>
  <div class="spk-pipeline">
    <div
      v-for="(s, i) in stages"
      :key="i"
      class="spk-pipeline__lane"
      :class="`spk-pipeline__lane--${stateAt(i)}`"
    >
      <span class="spk-pipeline__name">{{ s }}</span>
      <span class="spk-pipeline__mark">{{ markAt(i) }}</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'

const props = defineProps<{
  /** 当前阶段索引（0 起）；i<current 视为 done */
  current?: number
  /** 覆盖逐阶段状态：done/active/blocked/failed/pending */
  states?: string[]
  stages?: string[]
}>()

const DEFAULT_STAGES = ['概念', '计划', '开发', '验证', '发布', '生命周期']
const stages = computed(() => props.stages || DEFAULT_STAGES)

const stateAt = (i: number): string => {
  if (props.states && props.states[i]) return props.states[i]
  if (props.current === undefined) return 'pending'
  if (i < props.current) return 'done'
  if (i === props.current) return 'active'
  return 'pending'
}

const markAt = (i: number): string => {
  const s = stateAt(i)
  if (s === 'done') return '✓'
  if (s === 'active') return '●'
  if (s === 'blocked') return '✕'
  if (s === 'failed') return '✕'
  return '○'
}
</script>

<style scoped>
.spk-pipeline {
  display: flex;
  align-items: stretch;
  gap: 0;
}
.spk-pipeline__lane {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 4px 10px;
  border: 1px solid #d0d7de;
  border-radius: 4px;
  background: #f6f8fa;
  position: relative;
  flex: 1;
}
.spk-pipeline__lane + .spk-pipeline__lane {
  margin-left: 6px;
}
.spk-pipeline__name {
  font-size: 12px;
  color: #656d76;
  font-weight: 500;
}
.spk-pipeline__mark {
  font-size: 11px;
}
.spk-pipeline__lane--done {
  background: #dafbe1;
  border-color: #4ac26b;
  color: #116329;
}
.spk-pipeline__lane--done .spk-pipeline__name,
.spk-pipeline__lane--done .spk-pipeline__mark {
  color: #116329;
}
.spk-pipeline__lane--active {
  background: #ddf4ff;
  border-color: #54aeff;
}
.spk-pipeline__lane--active .spk-pipeline__name,
.spk-pipeline__lane--active .spk-pipeline__mark {
  color: #0550ae;
}
.spk-pipeline__lane--blocked,
.spk-pipeline__lane--failed {
  background: #ffebe9;
  border-color: #ff8182;
}
.spk-pipeline__lane--blocked .spk-pipeline__name,
.spk-pipeline__lane--blocked .spk-pipeline__mark,
.spk-pipeline__lane--failed .spk-pipeline__name,
.spk-pipeline__lane--failed .spk-pipeline__mark {
  color: #a40e26;
}
</style>
