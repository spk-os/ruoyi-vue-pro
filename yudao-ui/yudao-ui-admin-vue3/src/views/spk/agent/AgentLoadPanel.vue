<template>
  <div class="load-panel">
    <!-- 顶部说明 + 刷新 -->
    <div class="load-header">
      <div class="load-title">
        <Icon icon="ep:data-analysis" class="mr-5px" />
        <span>运行负载（按智能体维度真实聚合 spk_task_contract）</span>
      </div>
      <el-button :loading="loading" type="primary" plain @click="load">
        <Icon class="mr-5px" icon="ep:refresh" />
        刷新
      </el-button>
    </div>

    <!-- 汇总 KPI（真实计数） -->
    <div class="stats-grid">
      <StatCard label="活跃智能体" :value="summary.agents" icon="ep:cpu" type="primary" />
      <StatCard label="运行中" :value="summary.running" icon="ep:loading" type="warning" />
      <StatCard label="成功" :value="summary.succeeded" icon="ep:circle-check" type="success" />
      <StatCard label="失败" :value="summary.failed" icon="ep:warning-filled" type="danger" />
      <StatCard label="人工介入" :value="summary.intervened" icon="ep:edit" type="info" />
      <StatCard label="任务总数" :value="summary.total" icon="ep:list" type="default" />
    </div>

    <ContentWrap>
      <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true">
        <el-table-column align="center" label="智能体" min-width="160">
          <template #default="{ row }">
            <span>{{ row.agentName || '-' }}</span>
            <div class="sub-text">{{ row.agentCode || row.agentDefId }}</div>
          </template>
        </el-table-column>
        <el-table-column align="center" label="运行中" prop="running" width="90" />
        <el-table-column align="center" label="成功" prop="succeeded" width="80" />
        <el-table-column align="center" label="失败" prop="failed" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.failed > 0" type="danger">{{ row.failed }}</el-tag>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column align="center" label="介入" prop="intervened" width="80" />
        <el-table-column align="center" label="任务总数" prop="total" width="90" />
        <el-table-column align="center" label="成功率" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.successRate === null || row.successRate === undefined" type="info">
              样本不足
            </el-tag>
            <span v-else>{{ (row.successRate * 100).toFixed(0) }}%</span>
          </template>
        </el-table-column>
        <el-table-column align="center" label="最近失败原因" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.lastError" class="error-text">{{ row.lastError }}</span>
            <span v-else class="sub-text">—</span>
          </template>
        </el-table-column>
        <el-table-column
          align="center"
          label="最近活动"
          prop="lastActivity"
          width="170"
          :formatter="dateFormatter"
        />
      </el-table>
      <!-- 数据稀疏诚实标注 -->
      <el-empty
        v-if="!loading && list.length === 0"
        description="暂无运行数据（样本不足 / IPD 尚未派发任务）"
      />
    </ContentWrap>
  </div>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { getLoadStats, type AgentLoadStats } from '@/api/spk/agent/task'
import StatCard from '../ipd/overview/StatCard.vue'

defineOptions({ name: 'AgentLoadPanel' })

const loading = ref(false)
const list = ref<AgentLoadStats[]>([])

const summary = computed(() => {
  const agents = list.value.length
  const sum = (f: (r: AgentLoadStats) => number | undefined) =>
    list.value.reduce((a, r) => a + (f(r) ?? 0), 0)
  return {
    agents,
    running: sum((r) => r.running),
    succeeded: sum((r) => r.succeeded),
    failed: sum((r) => r.failed),
    intervened: sum((r) => r.intervened),
    total: sum((r) => r.total)
  }
})

const load = async () => {
  loading.value = true
  try {
    list.value = (await getLoadStats()) || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.load-panel {
  .load-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }
  .load-title {
    font-size: 15px;
    font-weight: 500;
  }
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 16px;
  }
  @media (max-width: 1200px) {
    .stats-grid {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }
  .sub-text {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
  .error-text {
    color: var(--el-color-danger);
  }
}
</style>
