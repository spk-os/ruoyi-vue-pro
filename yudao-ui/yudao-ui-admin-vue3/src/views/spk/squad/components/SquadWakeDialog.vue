<template>
  <Dialog v-model="dialogVisible" title="唤醒编队（成员按顺序串行单轮对话）" width="780px">
    <el-form :model="formState" label-width="80px">
      <el-form-item label="编队">
        <span>{{ formState.name }}（成员 {{ memberCount }}）</span>
      </el-form-item>
      <el-form-item label="消息" required>
        <el-input
          v-model="formState.message"
          type="textarea"
          :rows="4"
          placeholder="该消息会依次发送给编队内全部成员"
        />
      </el-form-item>
    </el-form>
    <el-divider content-position="left">各成员唤醒结果</el-divider>
    <div v-loading="waking">
      <el-empty v-if="!formState.results.length" description="尚未唤醒" />
      <el-table v-else :data="formState.results" row-key="agentId">
        <el-table-column label="智能体" min-width="140" prop="agentName" show-overflow-tooltip />
        <el-table-column label="状态" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="resultTagType(scope.row)">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回复内容" min-width="320" prop="content" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.error" class="text-red">{{ scope.row.error }}</span>
            <span v-else>{{ scope.row.content }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <template #footer>
      <el-button
        :loading="waking"
        type="primary"
        :disabled="!formState.message"
        @click="handleWake"
      >
        唤醒
      </el-button>
      <el-button @click="dialogVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import type { AgentSquadVO, SquadMemberWake } from '@/api/spk/agentsquad'
import * as AgentSquadApi from '@/api/spk/agentsquad'

defineOptions({ name: 'SpkAgentSquadWakeDialog' })

const message = useMessage()

const dialogVisible = ref(false)
const waking = ref(false)
const memberCount = ref(0)
const formState = reactive({
  id: undefined as number | undefined,
  name: '',
  message: '',
  results: [] as SquadMemberWake[]
})

const open = async (row: AgentSquadVO) => {
  formState.id = row.id
  formState.name = row.name
  formState.message = ''
  formState.results = []
  dialogVisible.value = true
  // 拉取成员数展示
  try {
    const ms = await AgentSquadApi.getAgentSquadMembers(row.id!)
    memberCount.value = ms?.length || 0
    if (memberCount.value === 0) {
      message.warning('该编队没有成员，无法唤醒')
    }
  } catch {}
}
defineExpose({ open })

const handleWake = async () => {
  if (!formState.id || !formState.message) return
  waking.value = true
  formState.results = []
  try {
    const r = await AgentSquadApi.wakeAgentSquad(formState.id, formState.message)
    formState.results = r.results || []
    const ok = formState.results.filter((x) => x.status && x.status !== 'error').length
    message.success(`唤醒完成，成功 ${ok}/${formState.results.length}`)
  } finally {
    waking.value = false
  }
}

const resultTagType = (r: SquadMemberWake) => {
  if (r.status === 'error' || r.error) return 'danger'
  if (r.status === 'idle') return 'success'
  if (r.status === 'busy') return 'warning'
  return 'info'
}
</script>

<style lang="scss" scoped>
.text-red {
  color: var(--el-color-danger);
}
</style>
