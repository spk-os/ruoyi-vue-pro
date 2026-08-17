<!--
  IPD Agent 负载看板：12 Lead + 3 Verifier 的 running 合同数 + 死信/积压。
  底部叠加监控指标快照（核心计数器 + 当前积压）。
-->
<template>
  <div class="agent-load" v-loading="loading">
    <div class="summary">
      <el-tag type="danger">死信 {{ data?.deadLetterCount ?? 0 }}</el-tag>
      <el-button type="primary" link @click="load">刷新</el-button>
    </div>

    <div class="section-title">Lead Agents（{{ leads.length }}）</div>
    <el-table :data="leads" size="small" border>
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="code" label="编码" min-width="160" />
      <el-table-column prop="model" label="model" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'busy' ? 'warning' : 'info'">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="runningContracts" label="running 合同" width="140">
        <template #default="{ row }">
          <el-tag :type="loadType(row.runningContracts)">{{ row.runningContracts }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div class="section-title">Independent Verifiers（{{ verifiers.length }}）</div>
    <el-table :data="verifiers" size="small" border>
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="code" label="编码" min-width="160" />
      <el-table-column prop="model" label="model" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'busy' ? 'warning' : 'info'">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="runningContracts" label="running 合同" width="140">
        <template #default="{ row }">
          <el-tag :type="loadType(row.runningContracts)">{{ row.runningContracts }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div class="section-title">监控指标快照</div>
    <div class="metrics">
      <div v-for="(v, k) in metrics" :key="k" class="metric">
        <div class="metric-k">{{ k }}</div>
        <div class="metric-v">{{ v }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getAgentLoad, getMetricsSnapshot } from '@/api/spk/ipd/cockpit'
import { statusMap, labelText } from '@/views/spk/ipd/home/components/status'

const statusLabel = (s?: string) => labelText(statusMap, s)

const loading = ref(false)
const data = ref<any>(null)
const metrics = ref<Record<string, any>>({})

const leads = computed(() => data.value?.leads || [])
const verifiers = computed(() => data.value?.verifiers || [])

const loadType = (n: number): any => {
  if (n >= 3) return 'danger'
  if (n >= 1) return 'warning'
  return 'success'
}

const load = async () => {
  loading.value = true
  try {
    const [loadData, snap] = await Promise.all([getAgentLoad(), getMetricsSnapshot()])
    data.value = loadData
    metrics.value = snap || {}
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.summary {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.section-title {
  font-weight: 600;
  margin: 14px 0 8px;
}
.metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.metric {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 8px 14px;
  min-width: 180px;
  background: var(--el-bg-color-page);
}
.metric-k {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-family: monospace;
}
.metric-v {
  font-size: 20px;
  font-weight: 600;
}
</style>
