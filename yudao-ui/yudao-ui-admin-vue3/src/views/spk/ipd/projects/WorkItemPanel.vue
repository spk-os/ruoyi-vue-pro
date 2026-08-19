<!--
  Plane 工作项同步表（UCD §4.2 需求与计划区并入项；§10.7）。
  自包含：getWorkItems 列表 + syncWorkItems 异步幂等同步 + linkWorkItem 绑定到版本/流程/Activity。
  Plane 工作项 6950 备份独立页降为此 Tab（不独立菜单）。全真实后端聚合无造假。
-->
<template>
  <div v-loading="loading" class="work-item-panel">
    <div class="flex justify-between items-center mb-10px">
      <span class="text-sm text-gray-500">Plane 工作项同步快照（REQUIREMENT/TASK/DEFECT/MILESTONE），同步后可绑定到版本/流程/Activity</span>
      <div class="flex gap-8px">
        <el-button v-hasPermi="['spk-delivery:ipd-project:update']" size="small" type="primary" :loading="syncing" @click="doSync">
          <Icon class="mr-4px" icon="ep:refresh" />同步 Plane
        </el-button>
        <el-button size="small" @click="load"><Icon class="mr-4px" icon="ep:search" />刷新</el-button>
      </div>
    </div>
    <el-table :data="list" size="small" border>
      <el-table-column label="序号" prop="planeIssueSeq" width="90" />
      <el-table-column label="标题" prop="name" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" prop="linkType" width="110">
        <template #default="{ row }"><el-tag size="small">{{ row.linkType || '—' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="同步状态" prop="syncStatus" width="100" align="center">
        <template #default="{ row }"><el-tag size="small" :type="syncTag(row.syncStatus)">{{ row.syncStatus || '—' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="绑定版本" width="120" align="center">
        <template #default="{ row }">{{ versionLabel(row.versionId) }}</template>
      </el-table-column>
      <el-table-column label="Activity" prop="activityCode" width="120" />
      <el-table-column label="最近同步" prop="lastSyncedAt" width="160" :formatter="dateFormatter" />
      <el-table-column label="操作" width="100" fixed="right" align="center">
        <template #default="{ row }">
          <el-button v-hasPermi="['spk-delivery:ipd-project:update']" link type="primary" size="small" @click="openLink(row)">绑定</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !list.length" description="暂无 Plane 工作项，点「同步 Plane」拉取" :image-size="50" />

    <!-- 绑定弹窗：选版本/流程/Activity -->
    <el-dialog v-model="linkDlg.visible" title="绑定工作项到版本/流程/Activity" width="520px" append-to-body>
      <el-form :model="linkForm" label-width="90px">
        <el-form-item label="工作项">
          <span class="text-sm"><el-tag size="small" class="mr-6px">{{ linkForm.linkType }}</el-tag>{{ linkForm.name || linkForm.planeIssueSeq }}</span>
        </el-form-item>
        <el-form-item label="绑定版本">
          <el-select v-model="linkForm.versionId" clearable filterable placeholder="选版本（可空=项目级）" class="!w-full">
            <el-option v-for="v in versions" :key="v.id" :label="v.versionLabel" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联流程">
          <el-select v-model="linkForm.flowRunId" clearable filterable placeholder="选 FlowRun（可空）" class="!w-full">
            <el-option v-for="r in flowRuns" :key="r.id" :label="r.runNo + ' · ' + (r.status || '')" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Activity">
          <el-input v-model="linkForm.activityCode" placeholder="如 ACT-REQ-COLLECT（可空）" class="!w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="linkDlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="linking" @click="doLink">确定绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { dateFormatter } from '@/utils/formatTime'
import * as WorkItemApi from '@/api/spk/ipd/work-item'
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import type { WorkItemRespVO, WorkItemLinkReqVO } from '@/api/spk/ipd/work-item'

defineOptions({ name: 'WorkItemPanel' })
const props = defineProps<{ projectId: number }>()
const message = useMessage()

const loading = ref(true)
const syncing = ref(false)
const linking = ref(false)
const list = ref<WorkItemRespVO[]>([])

// 版本与流程候选（绑定弹窗用，按 projectId 聚合，全真实后端）
const versions = ref<Array<{ id: number; versionLabel: string }>>([])
const flowRuns = ref<Array<{ id: number; runNo: string; status?: string }>>([])

const load = async () => {
  if (!props.projectId) return
  loading.value = true
  try {
    list.value = (await WorkItemApi.getWorkItems(props.projectId)) || []
  } catch (e: any) {
    message.error('加载工作项失败：' + (e?.message || ''))
  } finally {
    loading.value = false
  }
}

const loadCandidates = async () => {
  if (!props.projectId) return
  try {
    // 版本：major releases 扁平
    const majors = await IpdBusinessApi.listMajorReleases(props.projectId)
    const all: Array<{ id: number; versionLabel: string }> = []
    for (const m of majors || []) {
      const vs = await IpdBusinessApi.listVersions(m.id)
      for (const v of (vs || [])) all.push({ id: v.id, versionLabel: v.versionLabel })
    }
    versions.value = all
    // FlowRun：分页拉取本项目运行
    const rres = await IpdBusinessApi.getFlowRunPage({ projectId: props.projectId, pageNo: 1, pageSize: 100 })
    flowRuns.value = ((rres as any)?.list || []).map((r: any) => ({ id: r.id, runNo: r.runNo, status: r.status }))
  } catch {}
}

const syncTag = (s?: string) => {
  if (s === 'LINKED' || s === 'SYNCED') return 'success'
  if (s === 'ORPHAN') return 'warning'
  if (s === 'FAILED') return 'danger'
  return 'info'
}
const versionLabel = (vid?: number) => versions.value.find(v => v.id === vid)?.versionLabel || (vid ? `#${vid}` : '—')

const doSync = async () => {
  if (!props.projectId) return
  syncing.value = true
  try {
    const res = await WorkItemApi.syncWorkItems(props.projectId, 'sync-' + props.projectId + '-' + Date.now())
    message.success(`同步完成：${res.syncedCount ?? 0} 项，状态 ${res.status}`)
    await load()
  } catch (e: any) {
    message.error('同步失败：' + (e?.message || ''))
  } finally {
    syncing.value = false
  }
}

// 绑定弹窗
const linkDlg = reactive<{ visible: boolean }>({ visible: false })
const linkForm = reactive<WorkItemLinkReqVO>({
  planeIssueId: '', planeIssueSeq: '', name: '', linkType: '',
  versionId: undefined, flowRunId: undefined, activityCode: ''
})

const openLink = (row: WorkItemRespVO) => {
  linkForm.planeIssueId = row.planeIssueId
  linkForm.planeIssueSeq = row.planeIssueSeq
  linkForm.name = row.name
  linkForm.linkType = row.linkType
  linkForm.versionId = row.versionId
  linkForm.flowRunId = row.flowRunId
  linkForm.activityCode = row.activityCode || ''
  linkDlg.visible = true
}

const doLink = async () => {
  linking.value = true
  try {
    await WorkItemApi.linkWorkItem(props.projectId, { ...linkForm })
    message.success('绑定成功')
    linkDlg.visible = false
    await load()
  } catch (e: any) {
    message.error('绑定失败：' + (e?.message || ''))
  } finally {
    linking.value = false
  }
}

watch(() => props.projectId, () => { load(); loadCandidates() })
onMounted(() => { load(); loadCandidates() })
defineExpose({ refresh: load })
</script>

<style lang="scss" scoped>
.work-item-panel { padding: 0 2px; }
</style>
