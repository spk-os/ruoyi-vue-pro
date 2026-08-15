<template>
  <el-tabs v-model="activeTab" class="agent-tabs">
    <!-- Tab1 智能体定义 -->
    <el-tab-pane label="智能体定义" name="def">
      <!-- KPI 统计（真实计数，不造假） -->
      <div class="stats-grid">
        <StatCard
          v-for="c in statsCards"
          :key="c.label"
          :label="c.label"
          :value="c.value"
          :icon="c.icon"
          :type="c.type"
        />
      </div>

      <ContentWrap>
        <!-- 搜索工作栏 -->
        <el-form
          ref="queryFormRef"
          :inline="true"
          :model="queryParams"
          class="-mb-15px"
          label-width="80px"
        >
          <el-form-item label="智能体名" prop="name">
            <el-input
              v-model="queryParams.name"
              class="!w-200px"
              clearable
              placeholder="请输入智能体名"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="编码" prop="code">
            <el-input
              v-model="queryParams.code"
              class="!w-200px"
              clearable
              placeholder="请输入编码"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-input
              v-model="queryParams.role"
              class="!w-200px"
              clearable
              placeholder="请输入角色"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select
              v-model="queryParams.status"
              class="!w-200px"
              clearable
              placeholder="请选择状态"
            >
              <el-option
                v-for="opt in STATUS_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="运行时" prop="runtimeType">
            <el-select
              v-model="queryParams.runtimeType"
              class="!w-200px"
              clearable
              placeholder="请选择运行时"
            >
              <el-option
                v-for="opt in RUNTIME_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="是否隐藏" prop="hidden">
            <el-select
              v-model="queryParams.hidden"
              class="!w-160px"
              clearable
              placeholder="请选择"
            >
              <el-option label="全部" :value="(undefined as any)" />
              <el-option label="显示" :value="0" />
              <el-option label="隐藏" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="handleQuery">
              <Icon class="mr-5px" icon="ep:search" />
              搜索
            </el-button>
            <el-button @click="resetQuery">
              <Icon class="mr-5px" icon="ep:refresh" />
              重置
            </el-button>
            <el-button
              v-hasPermi="['spk-delivery:agent-def:create']"
              plain
              type="primary"
              @click="openForm('create')"
            >
              <Icon class="mr-5px" icon="ep:plus" />
              新增
            </el-button>
          </el-form-item>
        </el-form>
      </ContentWrap>

      <!-- 列表 -->
      <ContentWrap>
        <el-table v-loading="loading" :data="list">
          <el-table-column align="center" label="编号" prop="id" width="80" />
          <el-table-column
            align="center"
            label="智能体名"
            min-width="140"
            prop="name"
            show-overflow-tooltip
          />
          <el-table-column
            align="center"
            label="编码"
            min-width="140"
            prop="code"
            show-overflow-tooltip
          />
          <el-table-column
            align="center"
            label="角色"
            min-width="120"
            prop="role"
            show-overflow-tooltip
          />
          <el-table-column align="center" label="运行时" prop="runtimeType" width="100">
            <template #default="scope">
              <el-tag>{{ scope.row.runtimeType || 'native' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column align="center" label="默认模型" min-width="140" prop="model" />
          <el-table-column align="center" label="状态" prop="status" width="100">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)">
                {{ statusLabel(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column align="center" label="隐藏" prop="hidden" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.hidden === 1 ? 'info' : 'success'">
                {{ scope.row.hidden === 1 ? '隐藏' : '显示' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            align="center"
            label="创建时间"
            prop="createTime"
            width="170"
            :formatter="dateFormatter"
          />
          <el-table-column align="center" fixed="right" label="操作" min-width="240">
            <template #default="scope">
              <el-button
                v-hasPermi="['spk-delivery:agent-def:wake']"
                link
                type="primary"
                @click="openWake(scope.row)"
              >
                唤醒
              </el-button>
              <el-button
                v-hasPermi="['spk-delivery:agent-def:update']"
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
              >
                编辑
              </el-button>
              <el-button
                v-if="scope.row.hidden !== 1"
                v-hasPermi="['spk-delivery:agent-def:update']"
                link
                type="warning"
                @click="handleHide(scope.row, 1)"
              >
                隐藏
              </el-button>
              <el-button
                v-else
                v-hasPermi="['spk-delivery:agent-def:update']"
                link
                type="success"
                @click="handleHide(scope.row, 0)"
              >
                取消隐藏
              </el-button>
              <el-button
                v-hasPermi="['spk-delivery:agent-def:delete']"
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          v-model:limit="queryParams.pageSize"
          v-model:page="queryParams.pageNo"
          :total="total"
          @pagination="getList"
        />
      </ContentWrap>

      <!-- 表单弹窗：新增/修改 -->
      <AgentForm ref="formRef" @success="getList" />
      <!-- 唤醒弹窗：单轮对话 -->
      <WakeDialog ref="wakeRef" />
    </el-tab-pane>

    <!-- Tab2 智能体小队（原编队页降为组件，设计 §8 line549） -->
    <el-tab-pane label="智能体小队" name="squad" lazy>
      <component :is="SquadPage" />
    </el-tab-pane>

    <!-- Tab3 运行负载（§7.1 第 3 视图，真实聚合不造假） -->
    <el-tab-pane label="运行负载" name="load" lazy>
      <AgentLoadPanel />
    </el-tab-pane>
  </el-tabs>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as AgentDefApi from '@/api/spk/agentdef'
import AgentForm from './AgentForm.vue'
import WakeDialog from './WakeDialog.vue'
import AgentLoadPanel from './AgentLoadPanel.vue'
import StatCard from '../ipd/overview/StatCard.vue'
import { defineAsyncComponent } from 'vue'

defineOptions({ name: 'SpkAgent' })

// 异步加载编队页（降为 Tab2 组件，避免拆两处维护）
const SquadPage = defineAsyncComponent(() => import('../squad/index.vue'))

const message = useMessage()
const { t } = useI18n()

const activeTab = ref<'def' | 'squad' | 'load'>('def')

// 智能体状态选项（offline/idle/busy/error，本地枚举）
const STATUS_OPTIONS = [
  { label: '离线', value: 'offline' },
  { label: '空闲', value: 'idle' },
  { label: '忙碌', value: 'busy' },
  { label: '错误', value: 'error' }
]
// 运行时类型（仅 native 支持本地唤醒；claude/codex/custom 仅记录）
const RUNTIME_OPTIONS = [
  { label: 'native', value: 'native' },
  { label: 'claude', value: 'claude' },
  { label: 'codex', value: 'codex' },
  { label: 'custom', value: 'custom' }
]

const statusLabel = (s?: string) => STATUS_OPTIONS.find((o) => o.value === s)?.label || s || '-'
const statusTagType = (s?: string) => {
  switch (s) {
    case 'idle':
      return 'success'
    case 'busy':
      return 'warning'
    case 'error':
      return 'danger'
    default:
      return 'info'
  }
}

// KPI 统计：真实聚合（铁律不造假，0 即真实 0）
const stats = ref<Record<string, number>>({})
const statsCards = computed(() => [
  { label: '智能体总数', value: stats.value.totalAgents ?? 0, icon: 'ep:cpu', type: 'primary' },
  { label: '空闲 idle', value: stats.value.idle ?? 0, icon: 'ep:circle-check', type: 'success' },
  { label: '忙碌 busy', value: stats.value.busy ?? 0, icon: 'ep:loading', type: 'warning' },
  { label: '异常 error', value: stats.value.error ?? 0, icon: 'ep:warning-filled', type: 'danger' },
  { label: '编队数', value: stats.value.totalSquads ?? 0, icon: 'ep:user-filled', type: 'primary' },
  { label: '任务执行', value: stats.value.totalTasks ?? 0, icon: 'ep:list', type: 'default' }
])
const getStats = async () => {
  try {
    stats.value = (await AgentDefApi.getAgentDefStats()) || {}
  } catch {
    stats.value = {}
  }
}

const loading = ref(true)
const total = ref(0)
const list = ref<AgentDefApi.AgentDefVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  code: undefined,
  role: undefined,
  status: undefined,
  runtimeType: undefined,
  hidden: 0
})
const queryFormRef = ref()

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await AgentDefApi.getAgentDefPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 新增/修改 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 唤醒 */
const wakeRef = ref()
const openWake = (row: AgentDefApi.AgentDefVO) => {
  wakeRef.value.open(row)
}

/** 隐藏/取消隐藏 */
const handleHide = async (row: AgentDefApi.AgentDefVO, hidden: number) => {
  try {
    const text = hidden === 1 ? '隐藏' : '取消隐藏'
    await message.confirm('确认要"' + text + '""' + row.name + '"智能体吗?')
    if (hidden === 1) {
      await AgentDefApi.hideAgentDef(row.id!)
    } else {
      await AgentDefApi.unhideAgentDef(row.id!)
    }
    message.success(text + '成功')
    await getList()
    await getStats()
  } catch {}
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AgentDefApi.deleteAgentDef(id)
    message.success(t('common.delSuccess'))
    await getList()
    await getStats()
  } catch {}
}

onMounted(async () => {
  await Promise.all([getList(), getStats()])
})
</script>

<style lang="scss" scoped>
.agent-tabs {
  :deep(.el-tabs__content) {
    padding-top: 0;
  }
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
</style>
