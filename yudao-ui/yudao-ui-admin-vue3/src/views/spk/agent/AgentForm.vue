<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="智能体名" prop="name">
        <el-input v-model="formData.name" placeholder="请输入智能体名" />
      </el-form-item>
      <el-form-item label="编码" prop="code">
        <el-input v-model="formData.code" placeholder="请输入编码（如 aegis-reviewer）" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-input v-model="formData.role" placeholder="请输入角色（如 代码评审官）" />
      </el-form-item>
      <el-form-item label="运行时类型" prop="runtimeType">
        <el-select v-model="formData.runtimeType" placeholder="请选择运行时" class="!w-full">
          <el-option
            v-for="opt in RUNTIME_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="默认模型" prop="model">
        <el-input v-model="formData.model" placeholder="如 ali_glm-5.2" />
      </el-form-item>
      <el-form-item label="关联 AI 角色" prop="roleId">
        <el-input-number
          v-model="formData.roleId"
          :min="0"
          controls-position="right"
          placeholder="yudao AiChatRoleDO.id（唤醒必填）"
          class="!w-full"
        />
      </el-form-item>
      <el-form-item label="会话路由标识" prop="sessionKey">
        <el-input v-model="formData.sessionKey" placeholder="选填，会话路由用" />
      </el-form-item>
      <el-form-item label="灵魂内容" prop="soulContent">
        <el-input
          v-model="formData.soulContent"
          type="textarea"
          :rows="4"
          placeholder="system prompt / 智能体人设"
        />
      </el-form-item>
      <el-form-item label="工作记忆" prop="workingMemory">
        <el-input
          v-model="formData.workingMemory"
          type="textarea"
          :rows="3"
          placeholder="JSON 字符串，选填"
        />
      </el-form-item>
      <el-form-item label="工具配置" prop="toolsConfig">
        <el-input
          v-model="formData.toolsConfig"
          type="textarea"
          :rows="2"
          placeholder="工具白/黑名单 JSON，选填"
        />
      </el-form-item>
      <el-form-item label="杂项配置" prop="config">
        <el-input
          v-model="formData.config"
          type="textarea"
          :rows="2"
          placeholder="JSON 字符串，选填"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="formData.status" placeholder="请选择状态" class="!w-full">
          <el-option
            v-for="opt in STATUS_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import * as AgentDefApi from '@/api/spk/agentdef'

defineOptions({ name: 'SpkAgentForm' })

const { t } = useI18n()
const message = useMessage()

const STATUS_OPTIONS = [
  { label: '离线', value: 'offline' },
  { label: '空闲', value: 'idle' },
  { label: '忙碌', value: 'busy' },
  { label: '错误', value: 'error' }
]
const RUNTIME_OPTIONS = [
  { label: 'native（本地唤醒）', value: 'native' },
  { label: 'claude', value: 'claude' },
  { label: 'codex', value: 'codex' },
  { label: 'custom', value: 'custom' }
]

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref({
  id: undefined,
  name: undefined,
  code: undefined,
  role: undefined,
  sessionKey: undefined,
  soulContent: undefined,
  workingMemory: undefined,
  status: 'offline',
  model: undefined,
  roleId: undefined,
  toolsConfig: undefined,
  config: undefined,
  runtimeType: 'native',
  source: 'manual'
})
const formRules = reactive({
  name: [{ required: true, message: '智能体名不能为空', trigger: 'blur' }],
  code: [{ required: true, message: '编码不能为空', trigger: 'blur' }],
  role: [{ required: true, message: '角色不能为空', trigger: 'blur' }]
})
const formRef = ref()

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AgentDefApi.getAgentDef(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

/** 提交表单 */
const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data = formData.value as unknown as AgentDefApi.AgentDefVO
    if (formType.value === 'create') {
      await AgentDefApi.createAgentDef(data)
      message.success(t('common.createSuccess'))
    } else {
      await AgentDefApi.updateAgentDef(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    name: undefined,
    code: undefined,
    role: undefined,
    sessionKey: undefined,
    soulContent: undefined,
    workingMemory: undefined,
    status: 'offline',
    model: undefined,
    roleId: undefined,
    toolsConfig: undefined,
    config: undefined,
    runtimeType: 'native',
    source: 'manual'
  }
  formRef.value?.resetFields()
}
</script>
