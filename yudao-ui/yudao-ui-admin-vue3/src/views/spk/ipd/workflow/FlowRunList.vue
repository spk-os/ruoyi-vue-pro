<!-- 我的流程：FlowRun 列表（§7.8 业务用户）。聚合项目/版本/类型/阶段/健康/等待时长。 -->
<template>
  <div>
    <el-form class="-mb-15px" :inline="true" :model="queryParams" label-width="80px">
      <el-form-item label="项目" prop="projectId">
        <el-select v-model="queryParams.projectId" placeholder="全部" clearable filterable class="!w-180px"
          @change="handleQuery">
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型" prop="flowType">
        <el-select v-model="queryParams.flowType" placeholder="全部" clearable class="!w-180px" @change="handleQuery">
          <el-option label="完整发布" value="FULL_RELEASE" />
          <el-option label="增量发布" value="INCREMENT_RELEASE" />
          <el-option label="问题流" value="ISSUE_RESOLUTION" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-160px" @change="handleQuery">
          <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item label="阶段" prop="currentStage">
        <el-input v-model="queryParams.currentStage" placeholder="如 TR5" clearable class="!w-140px"
          @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery"><Icon icon="ep:search" class="mr-4px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-4px" />重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" class="mt-10px">
      <el-table-column label="运行号" prop="runNo" min-width="140" />
      <el-table-column label="项目" min-width="120">
        <template #default="{ row }">{{ projectName(row.projectId) }}</template>
      </el-table-column>
      <el-table-column label="类型" prop="flowType" width="130">
        <template #default="{ row }">{{ typeLabel(row.flowType) }}</template>
      </el-table-column>
      <el-table-column label="当前阶段/门禁" min-width="130">
        <template #default="{ row }">
          <span v-if="row.currentStage">{{ row.currentStage }}</span>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>
      <el-table-column label="健康" width="100">
        <template #default="{ row }">
          <el-tag :type="healthType(row.health)" size="small">{{ row.health || '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small" effect="plain">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="等待时长" width="120">
        <template #default="{ row }">{{ waitTime(row.startedAt, row.endedAt) }}</template>
      </el-table-column>
      <el-table-column label="发起时间" prop="startedAt" width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="goCockpit(row)">跟踪</el-button>
          <el-button v-if="row.status === 'BLOCKED'" link type="success" size="small"
            @click="onUnblock(row)">解除阻断</el-button>
          <el-button v-else-if="canBlock(row.status)" link type="warning" size="small"
            @click="onBlock(row)">阻断</el-button>
          <el-button v-if="row.status === 'RUNNING'" link type="danger" size="small"
            @click="onCancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getFlowRunPage,
  getPage as getPageProjects,
  cancelFlowRun,
  blockFlowRun,
  unblockFlowRun
} from '@/api/spk/ipd/business'
import type { SpkIpdFlowRunVO } from '@/api/spk/ipd/business'

defineOptions({ name: 'FlowRunList' })

const router = useRouter()
const loading = ref(false)
const list = ref<SpkIpdFlowRunVO[]>([])
const total = ref(0)
const projects = ref<any[]>([])
const statuses = ['DRAFT', 'READY', 'STARTING', 'RUNNING', 'BLOCKED', 'COMPLETED', 'FAILED', 'CANCELLED', 'SUPERSEDED']

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  projectId: undefined as number | undefined,
  flowType: undefined as string | undefined,
  status: undefined as string | undefined,
  currentStage: undefined as string | undefined
})

const getList = async () => {
  loading.value = true
  try {
    const res = await getFlowRunPage(queryParams as any)
    list.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryParams.projectId = undefined
  queryParams.flowType = undefined
  queryParams.status = undefined
  queryParams.currentStage = undefined
  handleQuery()
}

const loadProjects = async () => {
  try {
    const res = await getPageProjects({ pageNo: 1, pageSize: 50 } as any)
    projects.value = (res?.list || []).map((p: any) => ({ id: p.id, name: p.name || p.projectCode }))
  } catch {
    projects.value = []
  }
}

const projectName = (id?: number) => {
  const p = projects.value.find((x) => x.id === id)
  return p ? p.name : id ? `#${id}` : '—'
}

const typeLabel = (t?: string) => {
  const m: Record<string, string> = {
    FULL_RELEASE: '完整发布', INCREMENT_RELEASE: '增量发布', ISSUE_RESOLUTION: '问题流'
  }
  return m[t || ''] || t || '—'
}

const healthType = (h?: string) => {
  if (!h) return 'info'
  const u = h.toUpperCase()
  if (u === 'GREEN' || u === 'HEALTHY') return 'success'
  if (u === 'YELLOW' || u === 'WARN') return 'warning'
  return 'danger'
}
const statusType = (s?: string) => {
  if (!s) return 'info'
  const u = s.toUpperCase()
  if (u === 'COMPLETED') return 'success'
  if (u === 'RUNNING' || u === 'STARTING' || u === 'READY') return 'warning'
  if (u === 'FAILED' || u === 'BLOCKED' || u === 'CANCELLED') return 'danger'
  return 'info'
}
const canBlock = (s?: string) => ['RUNNING', 'READY'].includes((s || '').toUpperCase())

const waitTime = (start?: string, end?: string) => {
  if (!start) return '—'
  const s = new Date(start).getTime()
  const e = end ? new Date(end).getTime() : Date.now()
  const mins = Math.round((e - s) / 60000)
  if (mins < 60) return `${mins}m`
  if (mins < 1440) return `${Math.round(mins / 60)}h`
  return `${Math.round(mins / 1440)}d`
}

const goCockpit = (row: SpkIpdFlowRunVO) => {
  if (row.processInstanceId) {
    router.push({ path: '/spk/ipd-cockpit', query: { processInstanceId: row.processInstanceId } })
  } else {
    ElMessage.info('该运行尚未启动流程引擎，暂无流程图')
  }
}

const onBlock = async (row: SpkIpdFlowRunVO) => {
  let reason = ''
  try {
    const r = await ElMessageBox.prompt('请输入阻断理由', '人工阻断', {
      inputType: 'textarea', inputPlaceholder: '必须说明理由与影响'
    })
    reason = r.value
  } catch {
    return
  }
  if (!reason) {
    ElMessage.warning('阻断必须给理由')
    return
  }
  await blockFlowRun(row.id!, { reason, impact: 'workflow-center' })
  ElMessage.success('已阻断')
  getList()
}

const onUnblock = async (row: SpkIpdFlowRunVO) => {
  try {
    await ElMessageBox.confirm(`确认解除 ${row.runNo} 的阻断？`, '解除阻断', { type: 'warning' })
  } catch {
    return
  }
  await unblockFlowRun(row.id!)
  ElMessage.success('已解除阻断')
  getList()
}

const onCancel = async (row: SpkIpdFlowRunVO) => {
  let reason = ''
  try {
    const r = await ElMessageBox.prompt('请输入取消原因', '取消流程', { inputType: 'textarea' })
    reason = r.value
  } catch {
    return
  }
  if (!reason) {
    ElMessage.warning('取消必须给原因')
    return
  }
  await cancelFlowRun(row.id!, reason)
  ElMessage.success('已取消')
  getList()
}

onMounted(async () => {
  await loadProjects()
  await getList()
})
</script>
