<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="82px">
      <el-form-item label="项目名称" prop="name">
        <el-input v-model="queryParams.name" class="!w-200px" clearable placeholder="如 智能家居中控" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="项目编码" prop="projectCode">
        <el-input v-model="queryParams.projectCode" class="!w-180px" clearable placeholder="如 CORTEXT" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="负责人" prop="ownerUserId">
        <el-select v-model="queryParams.ownerUserId" class="!w-180px" clearable filterable placeholder="全部">
          <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
        </el-select>
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
      <el-form-item>
        <el-button @click="handleQuery"><Icon class="mr-5px" icon="ep:search" />搜索</el-button>
        <el-button @click="resetQuery"><Icon class="mr-5px" icon="ep:refresh" />重置</el-button>
        <el-button v-hasPermi="['spk-delivery:ipd-project:create']" plain type="primary" @click="openForm()">
          <Icon class="mr-5px" icon="ep:plus" />新建项目
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <div class="flex justify-between items-center mb-10px">
      <span class="text-sm text-gray-500">项目与大版本、交付版本、流程运行分离；点击行进入项目详情</span>
      <el-radio-group v-model="view" size="small">
        <el-radio-button value="table">表格</el-radio-button>
        <el-radio-button value="card">卡片</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 表格视图 -->
    <el-table v-if="view === 'table'" v-loading="loading" :data="list" @row-click="openDetail">
      <el-table-column label="项目" min-width="220" prop="name">
        <template #default="{ row }">
          <el-link type="primary" @click.stop="openDetail(row)">{{ row.name }}</el-link>
          <div class="text-xs text-gray-400">{{ row.projectNo }} · {{ row.projectCode }}</div>
        </template>
      </el-table-column>
      <el-table-column label="负责人" min-width="120">
        <template #default="{ row }">{{ userLabel(row.ownerUserId) }}</template>
      </el-table-column>
      <el-table-column align="center" label="状态" prop="status" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="健康" prop="health" width="100">
        <template #default="{ row }">
          <el-tag :type="healthTagType(row.health)" effect="plain">{{ healthLabel(row.health) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="计划完成" prop="plannedEndAt" width="160" :formatter="dateFormatter" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="160" :formatter="dateFormatter" />
      <el-table-column align="center" fixed="right" label="操作" width="240">
        <template #default="{ row }">
          <el-button v-hasPermi="['spk-delivery:ipd-project:update']" v-if="row.status === 'DRAFT'" link type="primary" @click.stop="handleActivate(row)">激活</el-button>
          <el-button v-hasPermi="['spk-delivery:ipd-project:update']" v-if="row.status === 'ACTIVE'" link type="warning" @click.stop="handlePause(row)">暂停</el-button>
          <el-button v-hasPermi="['spk-delivery:ipd-project:update']" v-if="row.status === 'PAUSED'" link type="success" @click.stop="handleActivate(row)">恢复</el-button>
          <el-button v-hasPermi="['spk-delivery:ipd-project:update']" link type="primary" @click.stop="openForm(row.id)">编辑</el-button>
          <el-button v-hasPermi="['spk-delivery:ipd-project:archive']" v-if="row.status !== 'ARCHIVED'" link type="danger" @click.stop="handleArchive(row)">归档</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 卡片视图 -->
    <div v-else v-loading="loading" class="grid grid-cols-3 gap-12px">
      <el-card v-for="row in list" :key="row.id" class="cursor-pointer" shadow="hover" @click="openDetail(row)">
        <div class="flex justify-between items-center">
          <b>{{ row.name }}</b>
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </div>
        <div class="text-xs text-gray-400 mt-4px">{{ row.projectNo }} · {{ userLabel(row.ownerUserId) }}</div>
        <div class="flex justify-between items-center mt-8px">
          <el-tag :type="healthTagType(row.health)" effect="plain" size="small">{{ healthLabel(row.health) }}</el-tag>
          <span class="text-xs text-gray-400">计划至 {{ formatDate(row.plannedEndAt) }}</span>
        </div>
      </el-card>
      <el-empty v-if="!loading && list.length === 0" description="暂无项目" />
    </div>

    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <!-- 新建/编辑抽屉 -->
  <ProjectForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import { getSimpleUserList } from '@/api/system/user'
import ProjectForm from './ProjectForm.vue'

defineOptions({ name: 'SpkIpdProjects' })

const message = useMessage()
const { t } = useI18n()

const STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '活跃', value: 'ACTIVE' },
  { label: '暂停', value: 'PAUSED' },
  { label: '归档', value: 'ARCHIVED' }
]
const HEALTH_OPTIONS = [
  { label: '未知', value: 'UNKNOWN' },
  { label: '良好', value: 'GOOD' },
  { label: '告警', value: 'WARN' },
  { label: '严重', value: 'CRITICAL' }
]
const statusLabel = (s?: string) => STATUS_OPTIONS.find((o) => o.value === s)?.label || s || '-'
const statusTagType = (s?: string) => {
  switch (s) {
    case 'ACTIVE': return 'success'
    case 'PAUSED': return 'warning'
    case 'ARCHIVED': return 'info'
    default: return ''
  }
}
const healthLabel = (s?: string) => HEALTH_OPTIONS.find((o) => o.value === s)?.label || s || '-'
const healthTagType = (s?: string) => {
  switch (s) {
    case 'GOOD': return 'success'
    case 'WARN': return 'warning'
    case 'CRITICAL': return 'danger'
    default: return 'info'
  }
}

const loading = ref(true)
const total = ref(0)
const list = ref<IpdBusinessApi.SpkIpdProjectVO[]>([])
const view = ref('table')
const userList = ref<any[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  projectCode: undefined,
  ownerUserId: undefined,
  status: undefined,
  health: undefined
})
const queryFormRef = ref()
const userLabel = (id?: number) => userList.value.find((u) => u.id === id)?.nickname || (id ? `用户#${id}` : '-')

const getList = async () => {
  loading.value = true
  try {
    const data = await IpdBusinessApi.getPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const handleQuery = () => { queryParams.pageNo = 1; getList() }
const resetQuery = () => { queryFormRef.value.resetFields(); handleQuery() }

const formatDate = (v?: string) => v ? v.slice(0, 10) : '—'

const openDetail = (row: IpdBusinessApi.SpkIpdProjectVO) => {
  if (!row?.id) return
  push({ name: 'SpkIpdProjectDetail', query: { projectId: row.id } })
}

const formRef = ref()
const openForm = (id?: number) => formRef.value.open(id)

const handleActivate = async (row: IpdBusinessApi.SpkIpdProjectVO) => {
  try {
    await message.confirm(`确认激活项目「${row.name}」？激活后进入 ACTIVE 状态。`)
    await IpdBusinessApi.activate(row.id!)
    message.success('激活成功')
    await getList()
  } catch {}
}
const handlePause = async (row: IpdBusinessApi.SpkIpdProjectVO) => {
  try {
    await message.confirm(`确认暂停项目「${row.name}」？`)
    await IpdBusinessApi.pause(row.id!)
    message.success('已暂停')
    await getList()
  } catch {}
}
const handleArchive = async (row: IpdBusinessApi.SpkIpdProjectVO) => {
  try {
    await message.confirm(`确认归档项目「${row.name}」？归档后不再出现在活跃视图。`)
    await IpdBusinessApi.archive(row.id!)
    message.success('已归档')
    await getList()
  } catch {}
}

const { push } = useRouter()

onMounted(async () => {
  userList.value = await getSimpleUserList()
  await getList()
})
</script>
