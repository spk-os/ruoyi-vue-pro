<template>
  <div class="spk-action" :style="barStyle">
    <div class="spk-action__body">
      <div class="spk-action__head">
        <SpkBadge v-if="typeMeta" :meta="typeMeta" />
        <span class="spk-action__title">{{ item.title }}</span>
      </div>
      <div v-if="metaParts.length" class="spk-action__meta">
        <span v-for="(m, i) in metaParts" :key="i" class="spk-action__meta-item">{{ m }}</span>
      </div>
      <div v-if="item.detail" class="spk-action__detail">{{ item.detail }}</div>
    </div>
    <div class="spk-action__action">
      <slot name="action">
        <el-button v-if="item.action" size="small" :type="btnType" @click="$emit('act', item)">
          {{ btnText }}
        </el-button>
      </slot>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import SpkBadge from './SpkBadge.vue'
import { severityMap, type BadgeMeta } from './status'

interface ActionItemLike {
  type?: string
  severity?: string
  title: string
  detail?: string
  refId?: string | number
  refType?: string
  flowRunId?: string | number
  projectId?: string | number
  action?: string
  [k: string]: any
}

const props = defineProps<{ item: ActionItemLike }>()
defineEmits<{ (e: 'act', item: ActionItemLike): void }>()

const sevMeta = computed<BadgeMeta>(() =>
  props.item.severity ? severityMap[props.item.severity] || { color: 'gray', text: props.item.severity } : { color: 'gray', text: '' }
)

const typeMeta = computed<BadgeMeta | null>(() => {
  const t = props.item.type
  if (!t) return null
  const labelMap: Record<string, string> = {
    APPROVAL: '审批', RETRY: '重试', UNBLOCK: '解除阻塞', SYNC: '同步',
    DISPATCH: '派发', CCB: 'CCB', MY_TODO: '待办', AGENT_FAILURE: '失败',
    EVIDENCE_GAP: '证据补齐', SYNC_FAILURE: '同步失败', BLOCKED_FLOW: '阻塞',
    DCP_TR: '决策', PENDING_DECISION: '待决策', OVERDUE_VERSION: '逾期',
    SEVERE_ISSUE: '问题'
  }
  const text = labelMap[t] || t
  return { color: sevMeta.value.color, text }
})

const barStyle = computed(() => ({ '--sev': barColor.value }))

const barColor = computed(() => {
  const c = sevMeta.value.color
  const m: Record<string, string> = {
    red: '#a40e26', orange: '#fb8f44', yellow: '#d4a72c', green: '#4ac26b', blue: '#54aeff', purple: '#c297fc', gray: '#d0d7de'
  }
  return m[c] || '#d0d7de'
})

const metaParts = computed(() => {
  const parts: string[] = []
  if (props.item.refId) parts.push(`${props.item.refType || 'REF'}-${props.item.refId}`)
  if (props.item.flowRunId) parts.push(`FR-${props.item.flowRunId}`)
  if (props.item.projectId) parts.push(`PRJ-${props.item.projectId}`)
  return parts
})

const btnType = computed<'primary' | 'danger' | 'warning' | 'default'>(() => {
  if (sevMeta.value.color === 'red' || sevMeta.value.color === 'orange') return 'primary'
  return 'default'
})
const btnText = computed(() => {
  const a = props.item.action
  const map: Record<string, string> = {
    APPROVE: '审核', RETRY: '重试', UNBLOCK: '排查', SYNC: '立即同步',
    DISPATCH: '分配', REASSIGN: '改派', SUPPLEMENT: '补充证据'
  }
  return a ? map[a] || a : '处理'
})
</script>

<style scoped>
.spk-action {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #d0d7de;
  border-left: 4px solid var(--sev);
  border-radius: 6px;
  background: #fff;
}
.spk-action__body {
  flex: 1;
  min-width: 0;
}
.spk-action__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.spk-action__title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2328;
}
.spk-action__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  font-size: 12px;
  color: #656d76;
}
.spk-action__meta-item {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}
.spk-action__detail {
  font-size: 12px;
  color: #8c959f;
  margin-top: 4px;
}
.spk-action__action {
  flex-shrink: 0;
}
</style>
