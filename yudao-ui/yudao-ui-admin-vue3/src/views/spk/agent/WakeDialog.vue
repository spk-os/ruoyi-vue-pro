<template>
  <Dialog v-model="dialogVisible" title="唤醒智能体（单轮对话）" width="640px">
    <el-form :model="formState" label-width="80px">
      <el-form-item label="智能体">
        <span>{{ formState.name }}（{{ formState.code }}）</span>
      </el-form-item>
      <el-form-item label="消息" required>
        <el-input
          v-model="formState.message"
          type="textarea"
          :rows="4"
          placeholder="请输入要发送给智能体的消息"
        />
      </el-form-item>
    </el-form>
    <el-divider content-position="left">回复内容</el-divider>
    <div v-loading="waking">
      <el-alert
        v-if="formState.error"
        :title="formState.error"
        type="error"
        :closable="false"
        show-icon
      />
      <div
        v-else-if="formState.content"
        class="wake-content"
      >
        {{ formState.content }}
      </div>
      <el-empty v-else description="尚未唤醒" />
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
import type { AgentDefVO, AgentDefWakeRespVO } from '@/api/spk/agentdef'
import * as AgentDefApi from '@/api/spk/agentdef'

defineOptions({ name: 'SpkAgentWakeDialog' })

const message = useMessage()

const dialogVisible = ref(false)
const waking = ref(false)
const formState = reactive({
  id: undefined as number | undefined,
  name: '',
  code: '',
  message: '',
  content: '',
  status: '',
  error: ''
})

/** 打开弹窗 */
const open = (row: AgentDefVO) => {
  formState.id = row.id
  formState.name = row.name
  formState.code = row.code
  formState.message = ''
  formState.content = ''
  formState.status = ''
  formState.error = ''
  dialogVisible.value = true
}
defineExpose({ open })

/** 唤醒 */
const handleWake = async () => {
  if (!formState.id || !formState.message) return
  waking.value = true
  formState.content = ''
  formState.error = ''
  try {
    const r: AgentDefWakeRespVO = await AgentDefApi.wakeAgentDef(formState.id, formState.message)
    formState.content = r.content
    formState.status = r.status
    message.success('唤醒成功，状态：' + (r.status || 'idle'))
  } catch (e) {
    formState.error = (e as Error)?.message || '唤醒失败'
  } finally {
    waking.value = false
  }
}
</script>

<style lang="scss" scoped>
.wake-content {
  padding: 8px 12px;
  min-height: 60px;
  white-space: pre-wrap;
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
}
</style>
