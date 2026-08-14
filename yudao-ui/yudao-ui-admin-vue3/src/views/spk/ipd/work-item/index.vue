<template>
  <div class="app-container" data-test="work-item-page">
    <el-card shadow="never" class="mb-12px">
      <div class="flex items-center gap-12px flex-wrap">
        <span class="font-bold">项目：</span>
        <el-select
          v-model="projectId"
          placeholder="选择 IPD 项目"
          filterable
          style="width: 280px"
          @change="handleProjectChange"
        >
          <el-option
            v-for="p in projects"
            :key="p.id"
            :label="p.name"
            :value="p.id"
          />
        </el-select>
        <el-button type="primary" :icon="Refresh" @click="loadWorkItems">刷新</el-button>
        <el-button type="success" :icon="Promotion" :loading="syncing" @click="handleSync">
          同步 Plane 工作项
        </el-button>
        <el-tag v-if="lastSyncedAt" type="info">最近同步：{{ lastSyncedAt }}</el-tag>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="workItems"
        :empty-text="projectId ? '暂无 Plane 工作项' : '请先选择项目'"
        stripe
        data-test="work-item-table"
      >
        <el-table-column label="Plane 序号" prop="planeIssueSeq" width="120" />
        <el-table-column label="名称" prop="name" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" prop="linkType" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="linkTypeTag(row.linkType)">{{ row.linkType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="同步状态" prop="syncStatus" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="syncStatusTag(row.syncStatus)">{{ row.syncStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="绑定" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.linked" size="small" type="success">已绑定</el-tag>
            <el-tag v-else size="small" type="info">未绑定</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="交付版本" width="110">
          <template #default="{ row }">{{ row.versionId || '-' }}</template>
        </el-table-column>
        <el-table-column label="流程实例" width="110">
          <template #default="{ row }">{{ row.flowRunId || '-' }}</template>
        </el-table-column>
        <el-table-column label="Activity" width="120">
          <template #default="{ row }">{{ row.activityCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openLinkDialog(row)">绑定</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="linkDialogVisible" title="绑定 Plane 工作项" width="560px" data-test="work-item-link-dialog">
      <el-form ref="linkFormRef" :model="linkForm" label-width="110px">
        <el-form-item label="Plane 序号">
          <span>{{ linkForm.planeIssueSeq || '-' }}（{{ linkForm.planeIssueId }}）</span>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="linkForm.linkType" style="width: 100%">
            <el-option label="需求 REQUIREMENT" value="REQUIREMENT" />
            <el-option label="任务 TASK" value="TASK" />
            <el-option label="缺陷 DEFECT" value="DEFECT" />
            <el-option label="里程碑 MILESTONE" value="MILESTONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="大版本">
          <el-select
            v-model="linkForm.majorReleaseId"
            placeholder="选择大版本"
            style="width: 100%"
            @change="handleMajorReleaseChange"
          >
            <el-option
              v-for="m in majorReleases"
              :key="m.id"
              :label="m.name"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="交付版本">
          <el-select v-model="linkForm.versionId" placeholder="选择交付版本" style="width: 100%">
            <el-option
              v-for="v in versions"
              :key="v.id"
              :label="v.name"
              :value="v.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="流程实例">
          <el-select v-model="linkForm.flowRunId" placeholder="选择 FlowRun（可选）" style="width: 100%" filterable clearable>
            <el-option
              v-for="f in flowRuns"
              :key="f.id"
              :label="f.runNo || `#${f.id}`"
              :value="f.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Activity 编码">
          <el-input v-model="linkForm.activityCode" placeholder="如 ACT-CONCEPT-1（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="linkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="linking" @click="submitLink">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Refresh, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getPage as getPageProjects,
  listMajorReleases,
  listVersions,
  getFlowRunPage
} from '@/api/spk/ipd/business'
import {
  getWorkItems,
  linkWorkItem,
  syncWorkItems,
  type WorkItemRespVO,
  type WorkItemLinkReqVO
} from '@/api/spk/ipd/work-item'

defineOptions({ name: 'SpkIpdWorkItem' })

const projects = ref<any[]>([])
const projectId = ref<number | undefined>(undefined)

const loading = ref(false)
const workItems = ref<WorkItemRespVO[]>([])
const lastSyncedAt = ref('')
const syncing = ref(false)

const linkDialogVisible = ref(false)
const linking = ref(false)
const majorReleases = ref<any[]>([])
const versions = ref<any[]>([])
const flowRuns = ref<any[]>([])
const linkForm = reactive<{
  planeIssueId: string
  planeIssueSeq?: string
  name?: string
  linkType: string
  majorReleaseId?: number
  versionId?: number
  flowRunId?: number
  activityCode?: string
}>({
  planeIssueId: '',
  planeIssueSeq: '',
  name: '',
  linkType: 'REQUIREMENT',
  majorReleaseId: undefined,
  versionId: undefined,
  flowRunId: undefined,
  activityCode: ''
})

const linkTypeTag = (t?: string) => {
  switch (t) {
    case 'REQUIREMENT':
      return 'primary'
    case 'TASK':
      return 'warning'
    case 'DEFECT':
      return 'danger'
    case 'MILESTONE':
      return 'success'
    default:
      return 'info'
  }
}
const syncStatusTag = (s?: string) => {
  switch (s) {
    case 'SYNCED':
    case 'LINKED':
      return 'success'
    case 'PENDING':
      return 'warning'
    case 'ORPHAN':
      return 'danger'
    default:
      return 'info'
  }
}

const loadProjects = async () => {
  const res = await getPageProjects({ pageNo: 1, pageSize: 50 } as any)
  projects.value = res?.list || []
  if (projects.value.length && projectId.value === undefined) {
    projectId.value = projects.value[0].id
    await loadWorkItems()
  }
}

const handleProjectChange = async () => {
  await loadWorkItems()
}

const loadWorkItems = async () => {
  if (!projectId.value) {
    workItems.value = []
    return
  }
  loading.value = true
  try {
    const res = await getWorkItems(projectId.value)
    workItems.value = res || []
    const synced = workItems.value
      .map((w) => w.lastSyncedAt)
      .filter(Boolean)
      .sort() as string[]
    lastSyncedAt.value = synced.length ? synced[synced.length - 1] : ''
  } finally {
    loading.value = false
  }
}

const handleSync = async () => {
  if (!projectId.value) return
  syncing.value = true
  try {
    const key = `plane-sync-${projectId.value}-${Date.now()}`
    const res = await syncWorkItems(projectId.value, key)
    if (res?.status === 'IDEMPOTENT') {
      ElMessage.info('幂等命中：同步任务已存在')
    } else if (res?.status === 'SUCCESS') {
      ElMessage.success(`同步完成：${res.syncedCount ?? 0} 项`)
    } else if (res?.status === 'FAILED') {
      ElMessage.error('同步失败，请查看后端日志')
    } else {
      ElMessage.success(`同步状态：${res?.status}`)
    }
    await loadWorkItems()
  } finally {
    syncing.value = false
  }
}

const openLinkDialog = async (row: WorkItemRespVO) => {
  linkForm.planeIssueId = row.planeIssueId
  linkForm.planeIssueSeq = row.planeIssueSeq
  linkForm.name = row.name
  linkForm.linkType = row.linkType || 'REQUIREMENT'
  linkForm.majorReleaseId = undefined
  linkForm.versionId = row.versionId
  linkForm.flowRunId = row.flowRunId
  linkForm.activityCode = row.activityCode
  versions.value = []
  flowRuns.value = []
  if (projectId.value) {
    try {
      majorReleases.value = (await listMajorReleases(projectId.value)) || []
    } catch {
      majorReleases.value = []
    }
    try {
      const fr = await getFlowRunPage({ pageNo: 1, pageSize: 50, projectId: projectId.value } as any)
      flowRuns.value = fr?.list || []
    } catch {
      flowRuns.value = []
    }
  }
  linkDialogVisible.value = true
}

const handleMajorReleaseChange = async () => {
  linkForm.versionId = undefined
  if (!linkForm.majorReleaseId) {
    versions.value = []
    return
  }
  try {
    versions.value = (await listVersions(linkForm.majorReleaseId)) || []
  } catch {
    versions.value = []
  }
}

const submitLink = async () => {
  if (!projectId.value) return
  linking.value = true
  try {
    const payload: WorkItemLinkReqVO = {
      planeIssueId: linkForm.planeIssueId,
      planeIssueSeq: linkForm.planeIssueSeq,
      name: linkForm.name,
      linkType: linkForm.linkType,
      versionId: linkForm.versionId,
      flowRunId: linkForm.flowRunId,
      activityCode: linkForm.activityCode
    }
    await linkWorkItem(projectId.value, payload)
    ElMessage.success('绑定成功')
    linkDialogVisible.value = false
    await loadWorkItems()
  } finally {
    linking.value = false
  }
}

onMounted(loadProjects)
</script>
