<template>
  <div v-loading="loading">
    <!-- 头部 -->
    <ContentWrap>
      <div class="flex justify-between items-start">
        <div>
          <div class="text-xs text-gray-400 mb-4px">{{ project.projectNo }} · {{ project.projectCode }} · {{ userLabel(project.ownerUserId) }}</div>
          <h2 class="text-lg font-bold m-0">{{ project.name }}</h2>
          <div class="text-sm text-gray-500 mt-4px">{{ project.objective }}</div>
        </div>
        <div class="flex items-center gap-8px">
          <el-tag :type="statusTagType(project.status)">{{ statusLabel(project.status) }}</el-tag>
          <el-tag :type="healthTagType(project.health)" effect="plain">{{ healthLabel(project.health) }}</el-tag>
          <el-button v-if="project.status === 'DRAFT'" v-hasPermi="['spk-delivery:ipd-project:update']" type="success" @click="handleActivate">激活项目</el-button>
          <el-button v-hasPermi="['spk-delivery:ipd-project:update']" @click="openProjectForm(project.id)">编辑</el-button>
          <el-button v-hasPermi="['spk-delivery:ipd-project:update']" type="primary" @click="openMajorForm">创建大版本</el-button>
        </div>
      </div>
    </ContentWrap>

    <ContentWrap>
      <el-tabs v-model="activeTab">
        <!-- 总览 -->
        <el-tab-pane label="总览" name="overview">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="当前大版本">{{ currentMajorLabel }}</el-descriptions-item>
            <el-descriptions-item label="版本数">{{ versionCount }}</el-descriptions-item>
            <el-descriptions-item label="活跃流程">{{ activeFlowCount }}</el-descriptions-item>
            <el-descriptions-item label="计划开始">{{ formatDate(project.plannedStartAt) }}</el-descriptions-item>
            <el-descriptions-item label="计划完成">{{ formatDate(project.plannedEndAt) }}</el-descriptions-item>
            <el-descriptions-item label="实际开始">{{ formatDate(project.actualStartAt) }}</el-descriptions-item>
          </el-descriptions>
          <div class="text-sm text-gray-500 mt-12px" v-if="project.description">
            <b>背景与边界：</b>{{ project.description }}
          </div>
        </el-tab-pane>

        <!-- 版本树 -->
        <el-tab-pane label="版本" name="versions">
          <div class="flex gap-12px">
            <!-- 左：大版本+版本树 -->
            <el-card class="!w-280px flex-shrink-0" shadow="never">
              <div class="flex justify-between items-center mb-8px">
                <b>版本树</b>
              </div>
              <el-empty v-if="!majorReleases.length" description="尚无大版本" :image-size="60" />
              <div v-for="m in majorReleases" :key="m.id" class="mb-8px">
                <div class="flex justify-between items-center px-6px py-4px font-semibold">
                  <span>V{{ m.majorNo }} · {{ m.name }}</span>
                  <el-tag size="small" :type="majorTagType(m.status)">{{ m.status }}</el-tag>
                </div>
                <div v-for="v in versionsOf(m.id!)" :key="v.id"
                  class="cursor-pointer px-6px py-6px ml-12px rounded text-sm"
                  :class="v.id === selectedVersionId ? 'bg-blue-50' : ''"
                  @click="selectVersion(v.id!)">
                  <span class="font-mono">{{ v.versionNo }}</span>
                  <el-tag class="ml-4px" size="small" effect="plain">{{ v.versionType }}</el-tag>
                  <el-tag class="ml-4px" size="small" :type="versionTagType(v.status)">{{ versionStatusLabel(v.status) }}</el-tag>
                </div>
              </div>
            </el-card>
            <!-- 右：版本详情 -->
            <el-card class="flex-1" shadow="never">
              <template v-if="selectedVersion">
                <div class="flex justify-between items-center mb-8px">
                  <div>
                    <span class="font-mono font-bold">{{ selectedVersion.versionNo }}</span>
                    <el-tag class="ml-8px" effect="plain">{{ selectedVersion.versionType }}{{ selectedVersion.baselineFlag ? ' · 基线' : '' }}</el-tag>
                    <el-tag class="ml-8px" :type="versionTagType(selectedVersion.status)">{{ versionStatusLabel(selectedVersion.status) }}</el-tag>
                  </div>
                  <div class="flex gap-8px">
                    <el-button size="small" @click="openVersionForm(selectedVersion.majorReleaseId)">创建增量</el-button>
                    <el-button size="small" @click="handleReadiness(selectedVersion)">就绪检查</el-button>
                    <el-button v-if="selectedVersion.status === 'DRAFT'" size="small" type="primary" @click="handleReadyVersion(selectedVersion)">置为 READY</el-button>
                    <el-button size="small" type="success" @click="openFlowWizard(selectedVersion)">启动流程</el-button>
                  </div>
                </div>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="主题">{{ selectedVersion.name || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="目标">{{ selectedVersion.objective || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="负责人">{{ userLabel(selectedVersion.ownerUserId) }}</el-descriptions-item>
                  <el-descriptions-item label="交付就绪">{{ selectedVersion.deliveryReadiness || '—' }}</el-descriptions-item>
                  <el-descriptions-item label="计划开始">{{ formatDate(selectedVersion.plannedStartAt) }}</el-descriptions-item>
                  <el-descriptions-item label="计划完成">{{ formatDate(selectedVersion.plannedEndAt) }}</el-descriptions-item>
                </el-descriptions>
                <div v-if="selectedVersion.scopeSummary" class="text-sm text-gray-500 mt-8px">
                  <b>范围：</b>{{ selectedVersion.scopeSummary }}
                </div>
              </template>
              <el-empty v-else description="选择左侧版本查看详情" :image-size="80" />
            </el-card>
          </div>
        </el-tab-pane>

        <!-- 问题 -->
        <el-tab-pane :label="`问题${issueTotal ? '(' + issueTotal + ')' : ''}`" name="issues">
          <div class="flex justify-between items-center mb-10px">
            <el-alert type="warning" :closable="false" show-icon class="flex-1 mr-10px">
              <template #title>问题流规则：进入修复实施前必须关联 FIXED_IN 版本；P0/P1 关闭前要求 VERIFIED_IN。</template>
            </el-alert>
            <el-button v-hasPermi="['spk-delivery:ipd-project:update']" type="primary" @click="openIssueForm">新建问题</el-button>
          </div>
          <el-table v-loading="issueLoading" :data="issueList">
            <el-table-column label="编号" prop="caseNo" width="150" />
            <el-table-column label="类型" prop="issueType" width="120" />
            <el-table-column label="严重度" prop="severity" width="90">
              <template #default="{ row }">
                <el-tag :type="row.severity === 'P0' || row.severity === 'P1' ? 'danger' : row.severity === 'P2' ? 'warning' : 'info'" size="small">{{ row.severity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="标题" min-width="180" prop="title" show-overflow-tooltip />
            <el-table-column label="状态" prop="status" width="110">
              <template #default="{ row }">
                <el-tag :type="issueTagType(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column align="center" fixed="right" label="操作" width="240">
              <template #default="{ row }">
                <el-button v-if="row.status === 'OPEN' || row.status === 'REOPENED'" link type="primary" @click="openTriage(row)">分诊</el-button>
                <el-button v-if="row.status === 'TRIAGED'" link type="primary" @click="openRelation(row)">关联版本</el-button>
                <el-button v-if="row.status === 'TRIAGED' || row.status === 'IN_PROGRESS'" link type="success" @click="handleResolve(row)">解决</el-button>
                <el-button v-if="row.status === 'RESOLVED'" link type="warning" @click="handleCloseIssue(row)">关闭</el-button>
                <el-button link type="primary" @click="handleStartIssueFlow(row)">启动问题流</el-button>
              </template>
            </el-table-column>
          </el-table>
          <Pagination v-model:limit="issueQuery.pageSize" v-model:page="issueQuery.pageNo" :total="issueTotal" @pagination="loadIssues" />
        </el-tab-pane>
      </el-tabs>
    </ContentWrap>

    <!-- 表单弹窗 -->
    <ProjectForm ref="projectFormRef" @success="loadProject" />
    <MajorReleaseForm ref="majorFormRef" :project-id="projectId" @success="loadMajors" />
    <VersionForm ref="versionFormRef" @success="loadMajors" />
    <IssueForm ref="issueFormRef" :project-id="projectId" @success="loadIssues" />
    <IssueTriageDialog ref="triageRef" @success="loadIssues" />
    <IssueRelationDialog ref="relationRef" @success="loadIssues" />
  </div>
</template>

<script lang="ts" setup>
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import { getSimpleUserList } from '@/api/system/user'
import ProjectForm from './ProjectForm.vue'
import MajorReleaseForm from './MajorReleaseForm.vue'
import VersionForm from './VersionForm.vue'
import IssueForm from './IssueForm.vue'
import IssueTriageDialog from './IssueTriageDialog.vue'
import IssueRelationDialog from './IssueRelationDialog.vue'

defineOptions({ name: 'SpkIpdProjectDetail' })

const props = defineProps<{ projectId?: number }>()
const message = useMessage()

const STATUS_OPTIONS = [{ label: '草稿', value: 'DRAFT' }, { label: '活跃', value: 'ACTIVE' }, { label: '暂停', value: 'PAUSED' }, { label: '归档', value: 'ARCHIVED' }]
const HEALTH_OPTIONS = [{ label: '未知', value: 'UNKNOWN' }, { label: '良好', value: 'GOOD' }, { label: '告警', value: 'WARN' }, { label: '严重', value: 'CRITICAL' }]
const statusLabel = (s?: string) => STATUS_OPTIONS.find((o) => o.value === s)?.label || s || '-'
const statusTagType = (s?: string) => ({ ACTIVE: 'success', PAUSED: 'warning', ARCHIVED: 'info' } as any)[s || ''] || ''
const healthLabel = (s?: string) => HEALTH_OPTIONS.find((o) => o.value === s)?.label || s || '-'
const healthTagType = (s?: string) => ({ GOOD: 'success', WARN: 'warning', CRITICAL: 'danger' } as any)[s || ''] || 'info'
const majorTagType = (s?: string) => ({ ACTIVE: 'success', MAINTENANCE: 'warning', CLOSED: 'info' } as any)[s || ''] || ''
const versionStatusLabel = (s?: string) => s || '-'
const versionTagType = (s?: string) => ({ RUNNING: 'success', VERIFYING: 'warning', RELEASED: 'success', CANCELLED: 'info', READY: 'primary' } as any)[s || ''] || ''
const issueTagType = (s?: string) => ({ OPEN: 'danger', TRIAGED: 'warning', IN_PROGRESS: 'warning', RESOLVED: 'info', CLOSED: 'info', REOPENED: 'danger' } as any)[s || ''] || ''
const formatDate = (v?: string) => v ? v.slice(0, 16).replace('T', ' ') : '—'

const loading = ref(false)
const project = ref<IpdBusinessApi.SpkIpdProjectVO>({})
const userList = ref<any[]>([])
const userLabel = (id?: number) => userList.value.find((u) => u.id === id)?.nickname || (id ? `用户#${id}` : '-')

const activeTab = ref('overview')
const majorReleases = ref<IpdBusinessApi.SpkIpdMajorReleaseVO[]>([])
const allVersions = ref<IpdBusinessApi.SpkIpdVersionVO[]>([])
const selectedVersionId = ref<number>()
const selectedVersion = computed(() => allVersions.value.find((v) => v.id === selectedVersionId.value))
const versionsOf = (majorId: number) => allVersions.value.filter((v) => v.majorReleaseId === majorId)
const currentMajorLabel = computed(() => {
  const m = majorReleases.value.find((x) => x.id === project.value.currentMajorReleaseId)
  return m ? `V${m.majorNo} · ${m.name}` : '—'
})
const versionCount = computed(() => allVersions.value.length)
const activeFlowCount = ref(0)

const issueLoading = ref(false)
const issueList = ref<IpdBusinessApi.SpkIpdIssueCaseVO[]>([])
const issueTotal = ref(0)
const issueQuery = reactive({ pageNo: 1, pageSize: 10 })

const loadProject = async () => {
  if (!props.projectId) return
  loading.value = true
  try {
    project.value = await IpdBusinessApi.get(props.projectId)
    await loadMajors()
    try {
      const data = await IpdBusinessApi.getFlowRunPage({ projectId: props.projectId, pageNo: 1, pageSize: 1, status: 'RUNNING' })
      activeFlowCount.value = data.total || 0
    } catch {}
  } finally {
    loading.value = false
  }
}
const loadMajors = async () => {
  if (!props.projectId) return
  majorReleases.value = await IpdBusinessApi.listMajorReleases(props.projectId)
  allVersions.value = []
  for (const m of majorReleases.value) {
    if (!m.id) continue
    const vs = await IpdBusinessApi.listVersions(m.id)
    allVersions.value.push(...vs)
  }
  if (!selectedVersionId.value && allVersions.value.length) {
    selectedVersionId.value = allVersions.value[0].id
  }
}
const selectVersion = (id: number) => { selectedVersionId.value = id }

const loadIssues = async () => {
  if (!props.projectId) return
  issueLoading.value = true
  try {
    const data = await IpdBusinessApi.pageIssues(props.projectId, { ...issueQuery })
    issueList.value = data.list
    issueTotal.value = data.total
  } finally {
    issueLoading.value = false
  }
}

watch(() => props.projectId, loadProject, { immediate: true })
watch(activeTab, (t) => { if (t === 'issues') loadIssues() })

const handleActivate = async () => {
  try {
    await message.confirm(`确认激活项目「${project.value.name}」？`)
    await IpdBusinessApi.activate(project.value.id!)
    message.success('激活成功')
    await loadProject()
  } catch {}
}

const projectFormRef = ref()
const openProjectForm = (id?: number) => projectFormRef.value?.open(id)
const majorFormRef = ref()
const openMajorForm = () => majorFormRef.value?.open()
const versionFormRef = ref()
const openVersionForm = (majorReleaseId: number) => versionFormRef.value?.open(majorReleaseId)
const issueFormRef = ref()
const openIssueForm = () => issueFormRef.value?.open()
const triageRef = ref()
const openTriage = (row: IpdBusinessApi.SpkIpdIssueCaseVO) => triageRef.value?.open(row)
const relationRef = ref()
const openRelation = (row: IpdBusinessApi.SpkIpdIssueCaseVO) => relationRef.value?.open(row)

const handleReadyVersion = async (v: IpdBusinessApi.SpkIpdVersionVO) => {
  try {
    await message.confirm(`确认将版本「${v.versionNo}」置为 READY？`)
    await IpdBusinessApi.readyVersion(v.id!)
    message.success('已置 READY')
    await loadMajors()
  } catch {}
}
const handleReadiness = async (v: IpdBusinessApi.SpkIpdVersionVO) => {
  const r: IpdBusinessApi.SpkIpdReadinessRespVO = await IpdBusinessApi.readiness(v.id!)
  const lines = (r.checks || []).map((c) => `[${c.status}] ${c.code}: ${c.message || ''}`)
  message[(r.ready ? 'success' : 'warning') as 'success' | 'warning'](`${r.ready ? '就绪 ✅' : '未就绪 ❌'}\n${lines.join('\n')}`)
}

const openFlowWizard = async (v: IpdBusinessApi.SpkIpdVersionVO) => {
  try {
    const req: IpdBusinessApi.SpkIpdFlowRunPreflightReqVO = {
      projectId: props.projectId!,
      versionId: v.id,
      flowType: v.baselineFlag ? 'FULL_RELEASE' : 'INCREMENT_RELEASE'
    }
    await message.confirm(`将为版本「${v.versionNo}」创建${v.baselineFlag ? '全量发布' : '增量发布'}流程并启动，确认？`)
    const pre: IpdBusinessApi.SpkIpdReadinessRespVO = await IpdBusinessApi.preflight(req)
    if (!pre.ready) {
      message.warning('预检未通过，请先修复阻断项：\n' + (pre.checks || []).filter((c) => c.status === 'BLOCK').map((c) => `${c.code}: ${c.message || ''}`).join('\n'))
      return
    }
    const run = await IpdBusinessApi.createFlowRun(req)
    const key = `ipd-flow-${run.id}-${Date.now()}`
    const r = await IpdBusinessApi.startFlowRun(run.id!, key)
    message.success(`流程已启动，运行号 ${run.runNo}，引擎实例 ${r.processInstanceId || '—'}`)
  } catch (e: any) {
    if (e?.message) message.error(e.message)
  }
}

const handleResolve = async (row: IpdBusinessApi.SpkIpdIssueCaseVO) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入解决结论', `解决 ${row.caseNo}`, { type: 'warning', inputType: 'textarea' })
    await IpdBusinessApi.resolveIssue(row.id!, value)
    message.success('已解决')
    await loadIssues()
  } catch {}
}
const handleCloseIssue = async (row: IpdBusinessApi.SpkIpdIssueCaseVO) => {
  try {
    await message.confirm(`确认关闭问题「${row.caseNo}」？P0/P1 要求已关联 VERIFIED_IN 版本。`)
    await IpdBusinessApi.closeIssue(row.id!)
    message.success('已关闭')
    await loadIssues()
  } catch {}
}
const handleStartIssueFlow = async (row: IpdBusinessApi.SpkIpdIssueCaseVO) => {
  try {
    await message.confirm(`为问题「${row.caseNo}」启动 ISSUE_RESOLUTION 流程？`)
    const key = `ipd-issue-${row.id}-${Date.now()}`
    const r = await IpdBusinessApi.startIssueFlow(row.id!, key)
    message.success(`问题流已启动，FlowRun ${r.flowRunId}`)
  } catch (e: any) {
    if (e?.message) message.error(e.message)
  }
}

onMounted(async () => { userList.value = await getSimpleUserList() })
</script>
