<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="780px">
    <div class="mb-10px">
      <el-button
        v-hasPermi="['spk-delivery:agent-squad:update']"
        type="primary"
        plain
        size="small"
        @click="openSelect"
      >
        <Icon class="mr-5px" icon="ep:plus" />
        添加成员
      </el-button>
      <el-button size="small" @click="loadMembers">
        <Icon class="mr-5px" icon="ep:refresh" />
        刷新
      </el-button>
    </div>
    <el-table v-loading="loading" :data="members" row-key="id">
      <el-table-column align="center" label="顺序" prop="sortOrder" width="80">
        <template #default="scope">
          <el-input-number
            v-model="scope.row.sortOrder"
            :min="0"
            :controls="false"
            style="width: 60px"
            @change="handleSortChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column
        align="center"
        label="智能体名"
        min-width="140"
        prop="agentName"
        show-overflow-tooltip
      />
      <el-table-column align="center" label="编码" min-width="140" prop="agentCode" />
      <el-table-column align="center" label="智能体角色" min-width="120" prop="agentRole" />
      <el-table-column align="center" label="运行时" prop="agentRuntimeType" width="100" />
      <el-table-column align="center" label="状态" prop="agentStatus" width="100">
        <template #default="scope">
          <el-tag :type="agentStatusType(scope.row.agentStatus)">
            {{ scope.row.agentStatus || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" label="编队内角色" min-width="140" prop="role">
        <template #default="scope">
          <el-input
            v-model="scope.row.role"
            placeholder="如 主评审"
            @change="handleRoleChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="right" label="操作" width="100">
        <template #default="scope">
          <el-button
            v-hasPermi="['spk-delivery:agent-squad:update']"
            link
            type="danger"
            @click="handleRemove(scope.row)"
          >
            移除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加成员：选择智能体（多选） -->
    <Dialog
      v-model="selectVisible"
      append-to-body
      title="选择智能体"
      width="680px"
    >
      <el-table
        ref="selectTableRef"
        :data="agentOptions"
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="40" :reserve-selection="true" />
        <el-table-column label="智能体名" min-width="140" prop="name" show-overflow-tooltip />
        <el-table-column label="编码" min-width="140" prop="code" />
        <el-table-column label="角色" min-width="120" prop="role" />
        <el-table-column label="运行时" prop="runtimeType" width="100" />
      </el-table>
      <template #footer>
        <el-button type="primary" :loading="adding" @click="handleAddMembers">
          添加选中
        </el-button>
        <el-button @click="selectVisible = false">取 消</el-button>
      </template>
    </Dialog>
  </Dialog>
</template>

<script lang="ts" setup>
import * as AgentSquadApi from '@/api/spk/agentsquad'
import type { AgentSquadMemberRespVO } from '@/api/spk/agentsquad'
import * as AgentDefApi from '@/api/spk/agentdef'
import type { AgentDefVO } from '@/api/spk/agentdef'

defineOptions({ name: 'SpkAgentSquadMemberManager' })

const message = useMessage()
const { t } = useI18n()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const loading = ref(false)
const squadId = ref<number>()
const squadName = ref('')
const members = ref<AgentSquadMemberRespVO[]>([])

const open = async (row: AgentSquadApi.AgentSquadVO) => {
  squadId.value = row.id
  squadName.value = row.name
  dialogTitle.value = '成员管理：' + row.name
  dialogVisible.value = true
  await loadMembers()
}
defineExpose({ open })

const loadMembers = async () => {
  if (!squadId.value) return
  loading.value = true
  try {
    members.value = await AgentSquadApi.getAgentSquadMembers(squadId.value)
  } finally {
    loading.value = false
  }
}

// 行内修改：编队内角色
const handleRoleChange = async (row: AgentSquadMemberRespVO) => {
  try {
    await AgentSquadApi.updateAgentSquadMember({
      id: row.id,
      squadId: row.squadId,
      agentId: row.agentId,
      role: row.role,
      sortOrder: row.sortOrder
    })
    message.success('已更新角色')
  } catch {
    await loadMembers()
  }
}

// 行内修改：顺序
const handleSortChange = async (row: AgentSquadMemberRespVO) => {
  try {
    await AgentSquadApi.updateAgentSquadMember({
      id: row.id,
      squadId: row.squadId,
      agentId: row.agentId,
      role: row.role,
      sortOrder: row.sortOrder
    })
  } catch {
    await loadMembers()
  }
}

const handleRemove = async (row: AgentSquadMemberRespVO) => {
  try {
    await message.delConfirm('是否确认移除该成员？')
    await AgentSquadApi.removeAgentSquadMember(row.id)
    message.success(t('common.delSuccess'))
    await loadMembers()
  } catch {}
}

const agentStatusType = (s?: string) => {
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

// ---------- 添加成员 ----------
const selectVisible = ref(false)
const agentOptions = ref<AgentDefVO[]>([])
const selectedAgents = ref<AgentDefVO[]>([])
const adding = ref(false)
const selectTableRef = ref()

const openSelect = async () => {
  selectedAgents.value = []
  agentOptions.value = []
  selectVisible.value = true
  try {
    agentOptions.value = await AgentDefApi.getAgentDefList()
    // 默认勾选已加入的成员
    const existingIds = new Set(members.value.map((m) => m.agentId))
    nextTick(() => {
      agentOptions.value.forEach((a) => {
        if (a.id && existingIds.has(a.id) && selectTableRef.value) {
          selectTableRef.value.toggleRowSelection(a, true)
        }
      })
    })
  } catch {}
}

const handleSelectionChange = (selection: AgentDefVO[]) => {
  selectedAgents.value = selection
}

const handleAddMembers = async () => {
  // 仅添加「当前未存在」的
  const toAdd = selectedAgents.value.filter(
    (a) => a.id && !members.value.some((m) => m.agentId === a.id)
  )
  if (toAdd.length === 0) {
    message.warning('没有可添加的智能体（已全部加入或未选择）')
    return
  }
  adding.value = true
  try {
    for (const a of toAdd) {
      await AgentSquadApi.addAgentSquadMember({
        squadId: squadId.value!,
        agentId: a.id!
      })
    }
    message.success('已添加 ' + toAdd.length + ' 个成员')
    selectVisible.value = false
    await loadMembers()
  } finally {
    adding.value = false
  }
}
</script>
