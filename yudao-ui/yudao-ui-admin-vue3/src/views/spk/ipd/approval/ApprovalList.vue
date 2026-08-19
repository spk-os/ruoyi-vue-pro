<!--
  审批待办/已办列表（参数化 type=todo|done）。从 approval/index.vue 抽出供 ①② Tab 复用。
  底层复用 BPM 待办/已办分页，列展示项目/版本/阶段/门禁/健康等业务摘要。
  点「决策」由父组件打开 DecisionPackage 抽屉。
-->
<template>
  <div>
    <el-form class="-mb-15px" :inline="true" :model="queryParams" label-width="80px">
      <el-form-item label="任务名" prop="name">
        <el-input v-model="queryParams.name" placeholder="任务名" clearable class="!w-200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list" size="small">
      <el-table-column label="任务名" prop="name" min-width="160" fixed="left" show-overflow-tooltip />
      <el-table-column label="项目" prop="projectName" min-width="140" show-overflow-tooltip />
      <el-table-column label="版本" prop="versionLabel" width="90" />
      <el-table-column label="阶段" prop="currentStage" width="90" />
      <el-table-column label="门禁" prop="currentGate" width="100" />
      <el-table-column label="健康" width="90">
        <template #default="{ row }">
          <el-tag :type="healthTag(row.health)" size="small">{{ healthLabel(row.health) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审批人" prop="assigneeNickname" width="100" />
      <el-table-column label="等待" width="90">
        <template #default="{ row }">{{ formatWait(row.waitDurationMs) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="170" :formatter="dateFormatter" />
      <el-table-column v-if="type === 'done'" label="结束时间" prop="endTime" width="170" :formatter="dateFormatter" />
      <el-table-column label="操作" fixed="right" width="150">
        <template #default="{ row }">
          <el-button v-if="type === 'todo'" link type="primary" @click="emit('openDecision', row)">决策</el-button>
          <el-button v-else link type="info" @click="emit('openDecision', row)">查看决策包</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { dateFormatter } from '@/utils/formatTime'
import * as ApprovalApi from '@/api/spk/ipd/approval'
import type { SpkIpdApprovalTaskRespVO } from '@/api/spk/ipd/approval'
import { healthMap, labelText } from '@/views/spk/ipd/home/components/status'

defineOptions({ name: 'ApprovalList' })
const props = defineProps<{ type: 'todo' | 'done' }>()
const emit = defineEmits<{
  (e: 'openDecision', row: SpkIpdApprovalTaskRespVO): void
  (e: 'update:total', total: number): void
}>()

const loading = ref(true)
const total = ref(0)
const list = ref<SpkIpdApprovalTaskRespVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  type: props.type,
  name: '',
  processDefinitionKey: 'spkIpdFlow'
})

watch(() => props.type, (t) => { queryParams.type = t; queryParams.pageNo = 1; getList() })

const getList = async () => {
  loading.value = true
  try {
    const data = await ApprovalApi.pageApprovalTasks(queryParams)
    list.value = data.list
    total.value = data.total || 0
    emit('update:total', total.value)
  } finally {
    loading.value = false
  }
}
const handleQuery = () => { queryParams.pageNo = 1; getList() }
const resetQuery = () => { queryParams.name = ''; handleQuery() }

const healthTag = (h?: string) => (h === 'GOOD' ? 'success' : h === 'WARN' ? 'warning' : h === 'CRITICAL' ? 'danger' : 'info')
const healthLabel = (s?: string) => labelText(healthMap, s)
const formatWait = (ms?: number) => {
  if (!ms || ms <= 0) return '-'
  const h = Math.floor(ms / 3600000)
  const m = Math.floor((ms % 3600000) / 60000)
  return h > 0 ? `${h}h${m}m` : `${m}m`
}

defineExpose({ refresh: getList, getTotal: () => total.value })
onMounted(getList)
</script>
