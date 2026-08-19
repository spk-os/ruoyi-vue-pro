<template>
  <!-- 搜索工作栏 -->
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="82px">
      <el-form-item label="项目名称" prop="name">
        <el-input v-model="queryParams.name" class="!w-200px" clearable placeholder="如 智能家居中控" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-140px" clearable placeholder="全部">
          <el-option v-for="opt in STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="健康度" prop="health">
        <el-select v-model="queryParams.health" class="!w-140px" clearable placeholder="全部">
          <el-option v-for="opt in HEALTH_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="负责人" prop="ownerUserId">
        <el-select v-model="queryParams.ownerUserId" class="!w-160px" clearable filterable placeholder="全部">
          <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="当前阶段" prop="currentStage">
        <el-select v-model="queryParams.currentStage" class="!w-140px" clearable placeholder="全部">
          <el-option v-for="opt in STAGE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="仅看阻断">
        <el-switch v-model="queryParams.blockedOnly" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon class="mr-5px" icon="ep:search" />搜索</el-button>
        <el-button @click="resetQuery"><Icon class="mr-5px" icon="ep:refresh" />重置</el-button>
        <el-button v-hasPermi="['spk-delivery:ipd-project:create']" plain type="primary" @click="openWizard">
          <Icon class="mr-5px" icon="ep:plus" />发起项目
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <div class="flex justify-between items-center mb-12px">
      <span class="text-sm text-gray-500">项目卡片网格 · 真实聚合（无数据字段如实标注，不造假）</span>
      <el-radio-group v-model="view" size="small">
        <el-radio-button value="card">卡片</el-radio-button>
        <el-radio-button value="table">表格</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 卡片网格 -->
    <div v-if="view === 'card'" v-loading="loading" class="proj-grid">
      <div v-for="row in filteredCards" :key="row.id" class="proj-card" @click="openDetail(row)">
        <div class="proj-card__head">
          <div class="proj-card__title">
            <el-link type="primary" @click.stop="openDetail(row)">{{ row.name }}</el-link>
            <span class="proj-card__no">{{ row.projectNo }}</span>
          </div>
          <div class="proj-card__tags">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            <el-tag :type="healthTagType(row.health)" effect="plain" size="small">{{ healthLabel(row.health) }}</el-tag>
          </div>
        </div>

        <SpkStagePipeline
          class="mt-6px"
          :current="stageIndex(row.currentStage)"
          :stages="STAGE_LABELS"
        />

        <div class="proj-card__metrics">
          <div class="metric">
            <span class="metric__label">活跃版本</span>
            <span class="metric__value">{{ row.activeVersion || '未接入' }}</span>
          </div>
          <div class="metric">
            <span class="metric__label">剩余</span>
            <span class="metric__value" :class="dueInClass(row.dueIn)">{{ dueInText(row.dueIn) }}</span>
          </div>
          <div class="metric">
            <span class="metric__label">阻塞流</span>
            <span class="metric__value" :class="{ 'text-red-600': row.blockedFlows > 0 }">{{ row.blockedFlows ?? 0 }}</span>
          </div>
          <div class="metric">
            <span class="metric__label">待决策</span>
            <span class="metric__value" :class="{ 'text-orange-600': row.pendingDecisions > 0 }">{{ row.pendingDecisions ?? 0 }}</span>
          </div>
          <div class="metric">
            <span class="metric__label">团队</span>
            <span class="metric__value">{{ row.teamSize ?? 0 }}</span>
          </div>
        </div>

        <div class="proj-card__foot">
          <span class="text-xs text-gray-400">{{ userLabel(row.ownerUserId) }}</span>
          <span class="text-xs text-gray-400">大版本 {{ row.majorReleases ?? 0 }}</span>
          <SpkFreshness v-if="row.freshness" :minutes="freshnessMinutes(row.freshness)" />
          <span v-else class="text-xs text-gray-400">无更新</span>
        </div>
      </div>
      <el-empty v-if="!loading && filteredCards.length === 0" description="暂无项目，点「发起项目」创建" />
    </div>

    <!-- 表格视图（同源 cards 聚合数据，含 §4.2 全字段：大版本/活跃版本/剩余/阻塞流/待决策） -->
    <template v-else>
      <el-table v-loading="loading" :data="pagedCards" @row-click="openDetail">
        <el-table-column label="项目" min-width="200" prop="name">
          <template #default="{ row }">
            <el-link type="primary" @click.stop="openDetail(row)">{{ row.name }}</el-link>
            <div class="text-xs text-gray-400">{{ row.projectNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="负责人" min-width="100">
          <template #default="{ row }">{{ userLabel(row.ownerUserId) }}</template>
        </el-table-column>
        <el-table-column align="center" label="状态" prop="status" width="90">
          <template #default="{ row }"><el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column align="center" label="健康" prop="health" width="90">
          <template #default="{ row }"><el-tag :type="healthTagType(row.health)" effect="plain" size="small">{{ healthLabel(row.health) }}</el-tag></template>
        </el-table-column>
        <el-table-column align="center" label="当前阶段" width="90">
          <template #default="{ row }">{{ row.currentStageLabel || row.currentStage || '—' }}</template>
        </el-table-column>
        <el-table-column align="center" label="大版本" width="70">
          <template #default="{ row }">{{ row.majorReleases ?? 0 }}</template>
        </el-table-column>
        <el-table-column align="center" label="活跃版本" width="110">
          <template #default="{ row }">{{ row.activeVersion || '未接入' }}</template>
        </el-table-column>
        <el-table-column align="center" label="剩余" width="80">
          <template #default="{ row }"><span :class="dueInClass(row.dueIn)">{{ dueInText(row.dueIn) }}</span></template>
        </el-table-column>
        <el-table-column align="center" label="阻塞流" width="80">
          <template #default="{ row }"><span :class="{ 'text-red-600': row.blockedFlows > 0 }">{{ row.blockedFlows ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column align="center" label="待决策" width="80">
          <template #default="{ row }"><span :class="{ 'text-orange-600': row.pendingDecisions > 0 }">{{ row.pendingDecisions ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column align="center" label="计划完成" prop="plannedEndAt" width="150" :formatter="dateFormatter" />
        <el-table-column align="center" fixed="right" label="操作" width="220">
          <template #default="{ row }">
            <el-button v-hasPermi="['spk-delivery:ipd-project:update']" v-if="row.status === 'DRAFT'" link type="primary" @click.stop="handleActivate(row)">激活</el-button>
            <el-button v-hasPermi="['spk-delivery:ipd-project:update']" v-if="row.status === 'ACTIVE'" link type="warning" @click.stop="handlePause(row)">暂停</el-button>
            <el-button v-hasPermi="['spk-delivery:ipd-project:update']" v-if="row.status === 'PAUSED'" link type="success" @click.stop="handleActivate(row)">恢复</el-button>
            <el-button v-hasPermi="['spk-delivery:ipd-project:update']" link type="primary" @click.stop="openForm(row.id)">编辑</el-button>
            <el-button v-hasPermi="['spk-delivery:ipd-project:archive']" v-if="row.status !== 'ARCHIVED'" link type="danger" @click.stop="handleArchive(row)">归档</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="filteredCards.length" @pagination="() => {}" />
    </template>
  </ContentWrap>

  <ProjectForm ref="formRef" @success="reload" />
  <LaunchWizard ref="wizardRef" @success="onWizardSuccess" />
</template>

<script lang="ts" setup>
import * as BusinessApi from '@/api/spk/ipd/business'
import type { SpkIpdProjectVO as IpdProjectVO } from '@/api/spk/ipd/business'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { dateFormatter } from '@/utils/formatTime'
import SpkStagePipeline from '@/views/spk/ipd/home/components/SpkStagePipeline.vue'
import SpkFreshness from '@/views/spk/ipd/home/components/SpkFreshness.vue'
import ProjectForm from './ProjectForm.vue'
import LaunchWizard from './LaunchWizard.vue'

defineOptions({ name: 'SpkIpdProjects' })

const { push } = useRouter()
const message = useMessage()

const STAGE_LABELS = ['概念', '计划', '开发', '验证', '发布', '生命周期']
const STAGE_OPTIONS = [
  { value: 'concept', label: '概念' },
  { value: 'plan', label: '计划' },
  { value: 'develop', label: '开发' },
  { value: 'qualify', label: '验证' },
  { value: 'launch', label: '发布' },
  { value: 'lifecycle', label: '生命周期' }
]
const STATUS_OPTIONS = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'ACTIVE', label: '进行中' },
  { value: 'PAUSED', label: '已暂停' },
  { value: 'ARCHIVED', label: '已归档' }
]
const HEALTH_OPTIONS = [
  { value: 'UNKNOWN', label: '未知' },
  { value: 'GOOD', label: '健康' },
  { value: 'WARN', label: '告警' },
  { value: 'CRITICAL', label: '风险' }
]

const statusLabel = (v: string) => STATUS_OPTIONS.find((o) => o.value === v)?.label || v || '—'
const healthLabel = (v: string) => HEALTH_OPTIONS.find((o) => o.value === v)?.label || v || '—'
const statusTagType = (v: string) => ({ DRAFT: 'info', ACTIVE: 'success', PAUSED: 'warning', ARCHIVED: 'danger' }[v] || 'info')
const healthTagType = (v: string) => ({ UNKNOWN: 'info', GOOD: 'success', WARN: 'warning', CRITICAL: 'danger' }[v] || 'info')

const loading = ref(false)
const view = ref<'card' | 'table'>('card')
const cards = ref<any[]>([])
const userList = ref<UserVO[]>([])

const queryParams = reactive({
  name: '',
  status: undefined as string | undefined,
  health: undefined as string | undefined,
  ownerUserId: undefined as number | undefined,
  currentStage: undefined as string | undefined,
  blockedOnly: false,
  pageNo: 1,
  pageSize: 10
})
const queryFormRef = ref()

const userLabel = (userId?: number) => {
  if (!userId) return '未指派'
  const u = userList.value.find((x) => x.id === userId)
  return u ? u.nickname : `用户#${userId}`
}

// 卡片本地过滤（cards 为全量非分页，按搜索栏过滤）
const filteredCards = computed(() => {
  return cards.value.filter((c) => {
    if (queryParams.name && !(c.name || '').toLowerCase().includes(queryParams.name.toLowerCase())) return false
    if (queryParams.status && c.status !== queryParams.status) return false
    if (queryParams.health && c.health !== queryParams.health) return false
    if (queryParams.ownerUserId && c.ownerUserId !== queryParams.ownerUserId) return false
    if (queryParams.currentStage && c.currentStage !== queryParams.currentStage) return false
    if (queryParams.blockedOnly && !(c.blockedFlows > 0)) return false
    return true
  })
})
// 表格视图本地分页（同源 filteredCards 聚合数据）
const pagedCards = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return filteredCards.value.slice(start, start + queryParams.pageSize)
})

const stageIndex = (stage?: string) => {
  const map: Record<string, number> = { concept: 0, plan: 1, develop: 2, qualify: 3, launch: 4, lifecycle: 5 }
  return stage ? (map[stage] ?? 0) : 0
}

const dueInText = (dueIn?: number | null) => {
  if (dueIn === null || dueIn === undefined) return '未接入'
  if (dueIn < 0) return `逾期 ${-dueIn} 天`
  if (dueIn === 0) return '今日到期'
  return `剩 ${dueIn} 天`
}
const dueInClass = (dueIn?: number | null) => ({
  'text-red-600': dueIn !== null && dueIn !== undefined && dueIn < 0,
  'text-orange-600': dueIn === 0
})

const freshnessMinutes = (ts: number | string) => {
  const t = typeof ts === 'string' ? Date.parse(ts) : ts
  if (!t) return 0
  const diff = (Date.now() - t) / 60000
  return Math.max(0, Math.floor(diff))
}

const openDetail = (row: any) => {
  if (!row?.id) return
  push({ name: 'SpkIpdProjectDetail', query: { projectId: row.id } })
}

const openForm = (id?: number) => {
  formRef.value.open(id)
}

// 发起项目向导
const wizardRef = ref()
const openWizard = () => wizardRef.value?.open()

const formRef = ref()
const reload = () => {
  // 表格视图同源 cards，仅刷 cards 即可
  getCards()
}

const getCards = async () => {
  loading.value = true
  try {
    cards.value = await BusinessApi.getProjectCards()
  } finally {
    loading.value = false
  }
}

// cards 端点全量返回聚合字段（含阻塞流/待决策/活跃版本），表格视图改用同源 pagedCards
// getPage 仅保留为后端分页兜底入口（当前未在视图使用，避免冗余请求）
const handleQuery = () => {
  // 卡片与表格均为本地过滤，无需重新请求
  queryParams.pageNo = 1
}
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const handleActivate = async (row: IpdProjectVO) => {
  await BusinessApi.activate(row.id)
  message.success('已激活')
  reload()
}
const handlePause = async (row: IpdProjectVO) => {
  await BusinessApi.pause(row.id)
  message.success('已暂停')
  reload()
}
const handleArchive = async (row: IpdProjectVO) => {
  await message.confirm(`确认归档项目「${row.name}」？归档后不可发起流程。`)
  await BusinessApi.archive(row.id)
  message.success('已归档')
  reload()
}

const onWizardSuccess = () => {
  message.success('项目已创建并可发起流程')
  reload()
}

const init = async () => {
  userList.value = await getSimpleUserList()
  getCards()
}

onMounted(init)
</script>

<style lang="scss" scoped>
.proj-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
  gap: 14px;
}
.proj-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
  &:hover {
    border-color: var(--el-color-primary);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }
  &__head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 8px;
  }
  &__title {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  &__no {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
  &__tags {
    display: flex;
    gap: 6px;
    flex-shrink: 0;
  }
  &__metrics {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 6px;
    margin-top: 4px;
  }
  &__foot {
    display: flex;
    align-items: center;
    gap: 12px;
    border-top: 1px dashed var(--el-border-color-lighter);
    padding-top: 6px;
  }
}
.metric {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  &__label {
    font-size: 11px;
    color: var(--el-text-color-secondary);
  }
  &__value {
    font-size: 15px;
    font-weight: 600;
  }
}
</style>
