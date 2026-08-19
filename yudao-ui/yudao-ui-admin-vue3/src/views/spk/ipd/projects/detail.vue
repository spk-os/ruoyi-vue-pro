<template>
  <div v-loading="loading">
    <!-- 1. 头部 bar -->
    <ContentWrap>
      <div class="detail-header">
        <div class="detail-header__left">
          <el-button class="mr-8px" link @click="goBack"><Icon icon="ep:arrow-left" />返回</el-button>
          <div>
            <div class="text-xs text-gray-400">{{ project.projectNo }} · {{ project.projectCode }}</div>
            <h2 class="text-lg font-bold m-0">{{ project.name }}</h2>
            <div class="text-sm text-gray-500 mt-2px">{{ project.objective }}</div>
            <div v-if="nextAction" class="next-action mt-4px">
              <Icon icon="ep:guide" class="mr-4px" />
              <span class="text-xs text-gray-600">{{ nextAction }}</span>
            </div>
          </div>
        </div>
        <div class="detail-header__right">
          <el-tag :type="statusTagType(project.status)">{{ statusLabel(project.status) }}</el-tag>
          <el-tag :type="healthTagType(project.health)" effect="plain">{{ healthLabel(project.health) }}</el-tag>
          <span class="text-xs text-gray-500">负责人 {{ userLabel(project.ownerUserId) }}</span>
          <el-button link type="primary" @click="openVerModal">
            <Icon class="mr-4px" icon="ep:document" />版本
            <el-tag v-if="activeVersion" class="ml-4px" size="small" effect="plain">{{ activeVersion.versionNo }}</el-tag>
            <el-tag v-else class="ml-4px" size="small" type="info">未接入</el-tag>
          </el-button>
          <el-button v-if="project.status === 'DRAFT'" v-hasPermi="['spk-delivery:ipd-project:update']" type="success" @click="handleActivate">激活</el-button>
          <el-button v-hasPermi="['spk-delivery:ipd-project:update']" @click="openProjectForm(project.id)">编辑</el-button>
        </div>
      </div>
    </ContentWrap>

    <!-- 2. 阶段追踪器 -->
    <ContentWrap>
      <div class="block-title"><Icon class="mr-4px" icon="ep:flag" />阶段追踪器
        <span v-if="activeFlowRun" class="text-xs text-gray-400 ml-8px">活跃流程 {{ activeFlowRun.runNo }} · {{ activeFlowRun.currentStageLabel }}</span>
      </div>
      <div class="stage-tracker">
        <div
          v-for="st in stageTimestamps"
          :key="st.stage"
          class="stage-cell"
          :class="'stage-cell--' + st.status"
          @click="openStageDrawer(st)"
        >
          <div class="stage-cell__label">{{ st.label }}</div>
          <div class="stage-cell__status">{{ stageStatusText(st.status) }}</div>
          <div class="stage-cell__meta">
            <span>活动 {{ st.activityCount }}</span>
            <span v-if="st.enteredAt">{{ formatTime(st.enteredAt) }}</span>
            <span v-else class="text-gray-400">未进入</span>
          </div>
        </div>
      </div>
    </ContentWrap>

    <!-- 3. 活动泳道 -->
    <ContentWrap>
      <div class="block-title"><Icon class="mr-4px" icon="ep:grid" />活动泳道</div>
      <el-alert v-if="!activeFlowRun || !activeFlowRun.processInstanceId" type="info" :closable="false" show-icon>
        当前项目无活跃 FlowRun 引擎实例，活动泳道样本不足。
      </el-alert>
      <Swimlane v-else :external-pid="activeFlowRun.processInstanceId" />
    </ContentWrap>

    <!-- 4. 版本（Vx 折叠树 + 门禁 + 就绪/启动流程；原型 modal 内容提为内联区，modal 保留作全屏入口） -->
    <ContentWrap>
      <div class="flex justify-between items-center mb-10px">
        <div class="block-title m-0"><Icon class="mr-4px" icon="ep:document" />版本</div>
        <div class="flex gap-8px">
          <el-button v-hasPermi="['spk-delivery:ipd-project:update']" size="small" @click="openMajorForm()"><Icon class="mr-4px" icon="ep:plus" />大版本</el-button>
          <el-button size="small" @click="openVerModal"><Icon class="mr-4px" icon="ep:full-screen" />全屏</el-button>
        </div>
      </div>
      <el-empty v-if="!majorReleases.length" description="尚无大版本" :image-size="60" />
      <el-collapse v-else v-model="verCollapse" class="ver-collapse">
        <el-collapse-item v-for="m in majorReleases" :key="m.id" :name="m.id">
          <template #title>
            <span class="font-semibold">V{{ m.majorNo }} · {{ m.name }}</span>
            <el-tag class="ml-8px" size="small" :type="majorTagType(m.status)">{{ statusLabel(m.status) }}</el-tag>
            <span class="text-xs text-gray-400 ml-8px">{{ versionsOf(m.id).length }} 个版本</span>
          </template>
          <el-table :data="versionsOf(m.id)" size="small">
            <el-table-column label="版本号" prop="versionNo" width="120" />
            <el-table-column label="类型" prop="versionType" width="90" />
            <el-table-column label="状态" prop="status" width="100">
              <template #default="{ row }"><el-tag size="small" :type="versionTagType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="就绪" prop="deliveryReadiness" width="100" />
            <el-table-column label="计划完成" prop="plannedEndAt" width="150" :formatter="dateFormatter" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" link type="primary" @click="handleReadiness(row)">就绪检查</el-button>
                <el-button v-if="row.status === 'DRAFT'" size="small" link type="success" @click="handleReadyVersion(row)">置 READY</el-button>
                <el-button size="small" link type="primary" @click="openFlowWizard(row)">启动流程</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </ContentWrap>

    <!-- 5. 需求与计划（需求追踪树 + Plane 工作项同步表，§4.2 并入项） -->
    <ContentWrap>
      <el-tabs v-model="planTab" class="plan-tabs">
        <el-tab-pane name="req">
          <template #label><Icon class="mr-4px" icon="ep:connection" />需求追踪树（IR/SR/AR）</template>
          <el-empty v-if="reqTree?.sparse" description="需求样本不足：尚未从 Plane 同步该项目的需求链" :image-size="60" />
          <el-table v-else :data="reqTree?.nodes || []">
            <el-table-column label="序号" prop="planeIssueSeq" width="90" />
            <el-table-column label="类型" prop="requirementType" width="100">
              <template #default="{ row }"><el-tag size="small">{{ row.requirementType || '—' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="标题" min-width="240" prop="title" show-overflow-tooltip />
            <el-table-column label="状态" prop="status" width="120">
              <template #default="{ row }"><el-tag size="small" effect="plain">{{ statusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="最近同步" prop="lastSyncedAt" width="160" :formatter="dateFormatter" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane name="work" lazy>
          <template #label><Icon class="mr-4px" icon="ep:list" />Plane 工作项</template>
          <WorkItemPanel :project-id="projectId" />
        </el-tab-pane>
      </el-tabs>
    </ContentWrap>

    <!-- 6. 项目参与者（S5 下沉：项目级 Agent/编队/人员绑定落点） -->
    <ContentWrap :body-style="{ padding: '12px' }">
      <div class="block-title"><Icon class="mr-4px" icon="ep:user-filled" />项目参与者</div>
      <ProjectActorPanel :project-id="projectId" />
    </ContentWrap>

    <!-- 6. 问题 -->
    <ContentWrap>
      <div class="flex justify-between items-center mb-10px">
        <div class="block-title m-0"><Icon class="mr-4px" icon="ep:warning" />问题（{{ issueTotal }}）</div>
        <div class="flex gap-8px">
          <el-alert type="warning" :closable="false" show-icon class="flex-1 mr-10px">
            <template #title>修复实施前须关联 FIXED_IN；P0/P1 关闭前要求 VERIFIED_IN。</template>
          </el-alert>
          <el-button v-hasPermi="['spk-delivery:ipd-project:update']" type="primary" @click="openIssueForm">新建</el-button>
        </div>
      </div>
      <el-table v-loading="issueLoading" :data="issueList">
        <el-table-column label="编号" prop="caseNo" width="150" />
        <el-table-column label="严重度" prop="severity" width="90">
          <template #default="{ row }">
            <el-tag :type="row.severity === 'P0' || row.severity === 'P1' ? 'danger' : row.severity === 'P2' ? 'warning' : 'info'" size="small">{{ row.severity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="180" prop="title" show-overflow-tooltip />
        <el-table-column label="状态" prop="status" width="110">
          <template #default="{ row }"><el-tag :type="issueTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template>
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
    </ContentWrap>

    <!-- 8. 最近活动（移至末尾，§4.2 顺序：版本→需求与计划→参与者→问题→最近活动） -->
    <ContentWrap>
      <div class="block-title"><Icon class="mr-4px" icon="ep:clock" />最近活动</div>
      <el-empty v-if="!recentActivities.length" description="尚无活动记录" :image-size="60" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="a in recentActivities"
          :key="a.activityRunId"
          :timestamp="formatTime(a.queuedAt)"
          :type="actTagType(a.status)"
        >
          <div class="flex items-center gap-8px">
            <el-tag size="small" effect="plain">{{ a.stageLabel }}</el-tag>
            <b>{{ a.activityName }}</b>
            <el-tag size="small" :type="actTagType(a.status)">{{ actStatusLabel(a.status) }}</el-tag>
          </div>
          <div class="text-xs text-gray-500 mt-2px">
            流程 {{ a.flowRunNo }} · 智能体 {{ a.leadAgentName || a.leadAgentCode || '—' }} · 产物 {{ a.artifactCount }}
            <span v-if="a.finishedAt">· 完成 {{ formatTime(a.finishedAt) }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
    </ContentWrap>

    <!-- 版本列表 modal（全屏入口，与内联版本区同源数据） -->
    <el-dialog v-model="verModalVisible" title="版本列表" width="860px">
      <el-empty v-if="!majorReleases.length" description="尚无大版本" :image-size="60" />
      <div v-for="m in majorReleases" :key="m.id" class="mb-16px">
        <div class="flex justify-between items-center px-6px py-4px font-semibold bg-gray-50 rounded">
          <span>V{{ m.majorNo }} · {{ m.name }}</span>
          <el-tag size="small" :type="majorTagType(m.status)">{{ statusLabel(m.status) }}</el-tag>
        </div>
        <el-table :data="versionsOf(m.id)" size="small" class="mt-4px">
          <el-table-column label="版本号" prop="versionNo" width="120" />
          <el-table-column label="类型" prop="versionType" width="90" />
          <el-table-column label="状态" prop="status" width="100">
            <template #default="{ row }"><el-tag size="small" :type="versionTagType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="就绪" prop="deliveryReadiness" width="100" />
          <el-table-column label="计划完成" prop="plannedEndAt" width="150" :formatter="dateFormatter" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click="handleReadiness(row)">就绪检查</el-button>
              <el-button v-if="row.status === 'DRAFT'" size="small" link type="success" @click="handleReadyVersion(row)">置 READY</el-button>
              <el-button size="small" link type="primary" @click="openFlowWizard(row)">启动流程</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 阶段详情 drawer -->
    <el-drawer v-model="stageDrawerVisible" :title="`阶段详情 · ${stageDrawerData?.label}`" size="520px">
      <el-descriptions :column="1" border v-if="stageDrawerData">
        <el-descriptions-item label="状态">{{ stageStatusText(stageDrawerData.status) }}</el-descriptions-item>
        <el-descriptions-item label="进入时间">{{ formatTime(stageDrawerData.enteredAt) || '未进入' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatTime(stageDrawerData.finishedAt) || '未完成' }}</el-descriptions-item>
        <el-descriptions-item label="活动数">{{ stageDrawerData.activityCount }}</el-descriptions-item>
      </el-descriptions>
      <div class="block-title mt-12px">该阶段活动</div>
      <el-empty v-if="!stageDrawerActivities.length" description="该阶段暂无活动记录" :image-size="50" />
      <el-timeline v-else>
        <el-timeline-item v-for="a in stageDrawerActivities" :key="a.activityRunId" :timestamp="formatTime(a.queuedAt)" :type="actTagType(a.status)">
          <b>{{ a.activityName }}</b>
          <el-tag class="ml-4px" size="small" :type="actTagType(a.status)">{{ actStatusLabel(a.status) }}</el-tag>
          <div class="text-xs text-gray-500">智能体 {{ a.leadAgentName || a.leadAgentCode || '—' }} · 产物 {{ a.artifactCount }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>

    <!-- 表单弹窗 -->
    <ProjectForm ref="projectFormRef" @success="loadAll" />
    <MajorReleaseForm ref="majorFormRef" :project-id="projectId" @success="loadMajors" />
    <VersionForm ref="versionFormRef" @success="loadMajors" />
    <IssueForm ref="issueFormRef" :project-id="projectId" @success="loadIssues" />
    <IssueTriageDialog ref="triageRef" @success="loadIssues" />
    <IssueRelationDialog ref="relationRef" @success="loadIssues" />
  </div>
</template>

<script lang="ts" setup>
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import { getProjectWorkspace, getStageTimestamps, getRequirementsTree, getRecentActivities } from '@/api/spk/ipd/business'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { dateFormatter } from '@/utils/formatTime'
import { statusMap, healthMap, labelText } from '@/views/spk/ipd/home/components/status'
import Swimlane from '@/views/spk/ipd/cockpit/Swimlane.vue'
import ProjectForm from './ProjectForm.vue'
import MajorReleaseForm from './MajorReleaseForm.vue'
import VersionForm from './VersionForm.vue'
import IssueForm from './IssueForm.vue'
import IssueTriageDialog from './IssueTriageDialog.vue'
import IssueRelationDialog from './IssueRelationDialog.vue'
import WorkItemPanel from './WorkItemPanel.vue'
import ProjectActorPanel from './ProjectActorPanel.vue'

defineOptions({ name: 'SpkIpdProjectDetail' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const projectId = computed(() => Number(route.query.projectId) || 0)

const STATUS_OPTIONS = [{ label: '草稿', value: 'DRAFT' }, { label: '活跃', value: 'ACTIVE' }, { label: '暂停', value: 'PAUSED' }, { label: '归档', value: 'ARCHIVED' }]
const HEALTH_OPTIONS = [{ label: '未知', value: 'UNKNOWN' }, { label: '良好', value: 'GOOD' }, { label: '告警', value: 'WARN' }, { label: '严重', value: 'CRITICAL' }]
const statusLabel = (s?: string) => labelText(statusMap, s, STATUS_OPTIONS.find((o) => o.value === s)?.label)
const statusTagType = (s?: string) => ({ ACTIVE: 'success', PAUSED: 'warning', ARCHIVED: 'info', DRAFT: 'info' } as any)[s || ''] || ''
const healthLabel = (s?: string) => labelText(healthMap, s, HEALTH_OPTIONS.find((o) => o.value === s)?.label)
const healthTagType = (s?: string) => ({ GOOD: 'success', WARN: 'warning', CRITICAL: 'danger' } as any)[s || ''] || 'info'
const majorTagType = (s?: string) => ({ ACTIVE: 'success', MAINTENANCE: 'warning', CLOSED: 'info' } as any)[s || ''] || ''
const versionTagType = (s?: string) => ({ RUNNING: 'success', VERIFYING: 'warning', RELEASED: 'success', CANCELLED: 'info', READY: 'primary' } as any)[s || ''] || ''
const issueTagType = (s?: string) => ({ OPEN: 'danger', TRIAGED: 'warning', IN_PROGRESS: 'warning', RESOLVED: 'info', CLOSED: 'info', REOPENED: 'danger' } as any)[s || ''] || ''
const actTagType = (s?: string) => ({ done: 'success', running: 'primary', failed: 'danger', blocked: 'danger', timeout: 'warning', queued: 'info', cancelled: 'info', in_progress: 'warning' } as any)[s || ''] || 'info'
// 活动任务态中文（spk_task_contract.status 小写枚举），统一走集中标签模块
const actStatusLabel = (s?: string) => labelText(statusMap, s)

const formatTime = (v?: string | number) => {
  if (!v) return ''
  const s = String(v)
  return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
}
const stageStatusText = (s?: string) => ({ not_started: '未进入', running: '进行中', in_progress: '进行中', done: '已完成', blocked: '阻塞' } as any)[s || ''] || s || '-'

const loading = ref(false)
const project = ref<IpdBusinessApi.SpkIpdProjectVO>({})
const userList = ref<UserVO[]>([])
const userLabel = (id?: number) => userList.value.find((u) => u.id === id)?.nickname || (id ? `用户#${id}` : '-')

// workspace 数据
const activeVersion = ref<any>(null)
const activeFlowRun = ref<any>(null)
const stageTimestamps = ref<any[]>([])
const reqTree = ref<any>(null)
const recentActivities = ref<any[]>([])

// 版本列表 modal 数据
const verModalVisible = ref(false)
const verCollapse = ref<number[]>([]) // 内联版本折叠树展开项
const planTab = ref('req') // 需求与计划区子 Tab
const majorReleases = ref<IpdBusinessApi.SpkIpdMajorReleaseVO[]>([])
const allVersions = ref<IpdBusinessApi.SpkIpdVersionVO[]>([])
const versionsOf = (majorId: number) => allVersions.value.filter((v) => v.majorReleaseId === majorId)

// 问题
const issueLoading = ref(false)
const issueList = ref<IpdBusinessApi.SpkIpdIssueCaseVO[]>([])
const issueTotal = ref(0)
const issueQuery = reactive({ pageNo: 1, pageSize: 10 })

// 阶段 drawer
const stageDrawerVisible = ref(false)
const stageDrawerData = ref<any>(null)
const stageDrawerActivities = computed(() => {
  if (!stageDrawerData.value) return []
  return recentActivities.value.filter((a) => a.stage === stageDrawerData.value.stage)
})

const goBack = () => router.back()
const openVerModal = () => { verModalVisible.value = true }
const openStageDrawer = (st: any) => { stageDrawerData.value = st; stageDrawerVisible.value = true }

// 「下一步行动」引导：仅从真实 workspace 字段派生（project.status/activeFlowRun.status+blockReason+currentStageLabel/issueCount），不造假
const nextAction = computed(() => {
  const p = project.value
  const fr = activeFlowRun.value
  const st = p.status
  if (st === 'DRAFT') return '下一步：激活项目以启动 IPD 流程'
  if (st === 'PAUSED') return '下一步：恢复暂停的项目'
  if (st === 'ARCHIVED') return '项目已归档'
  // ACTIVE
  if (!fr) return '下一步：为版本启动发布流程（版本区 → 启动流程）'
  if (fr.blockReason) return `下一步：处理阻塞 — ${fr.blockReason}`
  if (fr.status === 'RUNNING' && fr.currentStageLabel) return `下一步：推进「${fr.currentStageLabel}」阶段`
  if (issueTotal.value > 0) return `下一步：处置 ${issueTotal.value} 个未决问题`
  return '项目运行中，无待办'
})

const loadWorkspace = async () => {
  if (!projectId.value) return
  try {
    const ws = await getProjectWorkspace(projectId.value)
    activeVersion.value = ws.activeVersion || null
    activeFlowRun.value = ws.activeFlowRun || null
    stageTimestamps.value = ws.stageTimestamps || []
    reqTree.value = ws.requirementRoots || null
    recentActivities.value = ws.recentActivities || []
  } catch (e) {
    // workspace 聚合失败不影响基础信息
  }
}

const loadProject = async () => {
  if (!projectId.value) return
  loading.value = true
  try {
    project.value = await IpdBusinessApi.get(projectId.value)
    await Promise.all([loadMajors(), loadWorkspace()])
  } finally {
    loading.value = false
  }
}

const loadMajors = async () => {
  if (!projectId.value) return
  majorReleases.value = await IpdBusinessApi.listMajorReleases(projectId.value)
  allVersions.value = []
  for (const m of majorReleases.value) {
    if (!m.id) continue
    const vs = await IpdBusinessApi.listVersions(m.id)
    allVersions.value.push(...vs)
  }
}

const loadIssues = async () => {
  if (!projectId.value) return
  issueLoading.value = true
  try {
    const data = await IpdBusinessApi.pageIssues(projectId.value, { ...issueQuery })
    issueList.value = data.list
    issueTotal.value = data.total
  } finally {
    issueLoading.value = false
  }
}

const loadAll = () => { loadProject(); loadIssues() }

watch(projectId, loadProject, { immediate: true })
onMounted(async () => {
  userList.value = await getSimpleUserList()
  loadIssues()
})

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
      projectId: projectId.value,
      versionId: v.id,
      flowType: v.baselineFlag ? 'FULL_RELEASE' : 'INCREMENT_RELEASE'
    }
    await message.confirm(`将为版本「${v.versionNo}」创建${v.baselineFlag ? '全量发布' : '增量发布'}流程并启动，确认？`)
    const pre: IpdBusinessApi.SpkIpdReadinessRespVO = await IpdBusinessApi.preflight(req)
    if (!pre.ready) {
      message.warning('预检未通过：\n' + (pre.checks || []).filter((c) => c.status === 'BLOCK').map((c) => `${c.code}: ${c.message || ''}`).join('\n'))
      return
    }
    const run = await IpdBusinessApi.createFlowRun(req)
    const key = `ipd-flow-${run.id}-${Date.now()}`
    const r = await IpdBusinessApi.startFlowRun(run.id!, key)
    message.success(`流程已启动，运行号 ${run.runNo}`)
    await loadWorkspace()
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
    await loadWorkspace()
  } catch (e: any) {
    if (e?.message) message.error(e.message)
  }
}
</script>

<style lang="scss" scoped>
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  &__left {
    display: flex;
    align-items: flex-start;
  }
  &__right {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }
}
.block-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}
.next-action {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  background: var(--el-color-primary-light-9);
  border-radius: 4px;
}
.ver-collapse :deep(.el-collapse-item__header) { font-size: 14px; }
.plan-tabs :deep(.el-tabs__header) { margin-bottom: 10px; }
.stage-tracker {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
}
.stage-cell {
  padding: 10px 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    border-color: var(--el-color-primary);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
  &--done { background: var(--el-color-success-light-9); border-color: var(--el-color-success-light-5); }
  &--running, &--in_progress { background: var(--el-color-primary-light-9); border-color: var(--el-color-primary-light-5); }
  &--blocked { background: var(--el-color-danger-light-9); border-color: var(--el-color-danger-light-5); }
  &__label { font-weight: 600; font-size: 14px; }
  &__status { font-size: 12px; color: var(--el-text-color-secondary); margin: 2px 0; }
  &__meta { font-size: 11px; color: var(--el-text-color-secondary); display: flex; justify-content: space-around; }
}
</style>
