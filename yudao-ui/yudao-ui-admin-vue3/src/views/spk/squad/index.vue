<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="80px"
    >
      <el-form-item label="编队名称" prop="name">
        <el-input
          v-model="queryParams.name"
          class="!w-200px"
          clearable
          placeholder="请输入编队名称"
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
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          class="!w-160px"
          clearable
          placeholder="请选择状态"
        >
          <el-option label="启用" value="active" />
          <el-option label="停用" value="disabled" />
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
          v-hasPermi="['spk-delivery:agent-squad:create']"
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
        label="编队名称"
        min-width="160"
        prop="name"
        show-overflow-tooltip
      />
      <el-table-column
        align="center"
        label="编码"
        min-width="160"
        prop="code"
        show-overflow-tooltip
      />
      <el-table-column
        align="center"
        label="描述"
        min-width="200"
        prop="description"
        show-overflow-tooltip
      />
      <el-table-column align="center" label="成员数" prop="memberCount" width="90">
        <template #default="scope">
          <el-tag>{{ scope.row.memberCount || 0 }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="状态" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'">
            {{ scope.row.status === 'active' ? '启用' : '停用' }}
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
      <el-table-column align="center" fixed="right" label="操作" min-width="280">
        <template #default="scope">
          <el-button
            v-hasPermi="['spk-delivery:agent-squad:query']"
            link
            type="primary"
            @click="openMembers(scope.row)"
          >
            成员
          </el-button>
          <el-button
            v-hasPermi="['spk-delivery:agent-squad:wake']"
            link
            type="primary"
            @click="openWake(scope.row)"
          >
            唤醒
          </el-button>
          <el-button
            v-hasPermi="['spk-delivery:agent-squad:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            编辑
          </el-button>
          <el-button
            v-hasPermi="['spk-delivery:agent-squad:delete']"
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

  <!-- 表单弹窗 -->
  <SquadForm ref="formRef" @success="getList" />
  <!-- 成员管理弹窗 -->
  <MemberManager ref="memberRef" />
  <!-- 编队唤醒弹窗 -->
  <SquadWakeDialog ref="wakeRef" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as AgentSquadApi from '@/api/spk/agentsquad'
import SquadForm from './SquadForm.vue'
import MemberManager from './components/MemberManager.vue'
import SquadWakeDialog from './components/SquadWakeDialog.vue'

defineOptions({ name: 'SpkAgentSquad' })

const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const total = ref(0)
const list = ref<AgentSquadApi.AgentSquadVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  code: undefined,
  status: undefined
})
const queryFormRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await AgentSquadApi.getAgentSquadPage(queryParams)
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

const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const memberRef = ref()
const openMembers = (row: AgentSquadApi.AgentSquadVO) => {
  memberRef.value.open(row)
}

const wakeRef = ref()
const openWake = (row: AgentSquadApi.AgentSquadVO) => {
  wakeRef.value.open(row)
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AgentSquadApi.deleteAgentSquad(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

onMounted(async () => {
  await getList()
})
</script>
