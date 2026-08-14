<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <!-- 高级/通用模式开关 -->
      <el-form-item label="高级模式">
        <el-switch v-model="advanced" />
        <span class="ml-8px text-12px text-gray-400">
          {{ advanced ? '暴露全参数（agentKind/能力标签/隔离级别/omnigent 映射等）' : '通用模式：仅基础 + 执行模式 + 继承' }}
        </span>
      </el-form-item>

      <el-form-item label="智能体名" prop="name">
        <el-input v-model="formData.name" placeholder="请输入智能体名" />
      </el-form-item>
      <el-form-item label="编码" prop="code">
        <el-input v-model="formData.code" placeholder="请输入编码（如 aegis-reviewer）" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-input v-model="formData.role" placeholder="请输入角色（如 代码评审官）" />
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
      <!-- 执行模式（per-agent runtime：local 本地 LLM / omnigent 真实沙箱） -->
      <el-form-item label="执行模式" prop="mode">
        <el-select v-model="formData.mode" placeholder="请选择执行模式" class="!w-full">
          <el-option label="local（本地 LLM，走 NativeAiAdapter）" value="local" />
          <el-option label="omnigent（真实云沙箱，走 OmnigentAdapter）" value="omnigent" />
        </el-select>
        <span class="text-12px text-gray-400">
          per-agent 决定 runtime；omnigent 模式须在高级模式填「Omnigent agent 映射」
        </span>
      </el-form-item>
      <!-- 继承父智能体（运行时合并：能力标签并集、soul/model/mode 子覆盖父） -->
      <el-form-item label="继承父智能体" prop="parentDefId">
        <el-select
          v-model="formData.parentDefId"
          placeholder="无继承（选填）"
          clearable
          filterable
          class="!w-full"
        >
          <el-option
            v-for="a in agentList"
            :key="a.id"
            :label="`${a.name}（${a.code}）`"
            :value="a.id"
            :disabled="a.id === formData.id"
          />
        </el-select>
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

      <!-- ===== 高级字段（仅高级模式显） ===== -->
      <template v-if="advanced">
        <el-form-item label="Omnigent 映射" prop="omnigentAgentId">
          <el-input
            v-model="formData.omnigentAgentId"
            :disabled="formData.mode !== 'omnigent'"
            placeholder="mode=omnigent 时填 Omnigent agent-id（如 spk-architect），空回退全局配置"
          />
        </el-form-item>
        <el-form-item label="智能体种类" prop="agentKind">
          <el-select v-model="formData.agentKind" placeholder="请选择种类" class="!w-full" clearable>
            <el-option label="lead（主智能体）" value="lead" />
            <el-option label="worker（工作智能体）" value="worker" />
            <el-option label="verifier（独立评审）" value="verifier" />
          </el-select>
        </el-form-item>
        <el-form-item label="能力标签" prop="capabilityTagsInput">
          <el-input
            v-model="capabilityTagsInput"
            placeholder="逗号分隔，如 architecture,review（存为 JSON 数组）"
          />
        </el-form-item>
        <el-form-item label="Verifier 类型" prop="verifierType">
          <el-select v-model="formData.verifierType" placeholder="仅 verifier 用" class="!w-full" clearable>
            <el-option label="TR 技术评审" value="TR" />
            <el-option label="RE 需求评审" value="RE" />
            <el-option label="SEC 安全评审" value="SEC" />
          </el-select>
        </el-form-item>
        <el-form-item label="隔离级别" prop="isolationLevel">
          <el-select v-model="formData.isolationLevel" placeholder="请选择隔离级别" class="!w-full" clearable>
            <el-option label="process（进程级）" value="process" />
            <el-option label="container（容器级）" value="container" />
            <el-option label="vm（虚拟机级）" value="vm" />
          </el-select>
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
      </template>
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
// 高级/通用模式开关（默认关=通用模式）
const advanced = ref(false)
// 父智能体候选列表
const agentList = ref<AgentDefApi.AgentDefVO[]>([])
// 能力标签逗号输入（双向转换 capabilityTags JSON 数组）
const capabilityTagsInput = ref('')
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
  source: 'manual',
  // 高级字段
  agentKind: undefined,
  capabilityTags: undefined,
  verifierType: undefined,
  isolationLevel: undefined,
  mode: 'local',
  parentDefId: undefined,
  omnigentAgentId: undefined
})
const formRules = reactive({
  name: [{ required: true, message: '智能体名不能为空', trigger: 'blur' }],
  code: [{ required: true, message: '编码不能为空', trigger: 'blur' }],
  role: [{ required: true, message: '角色不能为空', trigger: 'blur' }]
})
const formRef = ref()

/** 加载父智能体候选列表（排除自己） */
const loadAgentList = async () => {
  try {
    agentList.value = (await AgentDefApi.getAgentDefList()) || []
  } catch {
    agentList.value = []
  }
}

/** capabilityTags JSON 数组 ↔ 逗号输入串 转换 */
const tagsToArray = (input: string): string[] | undefined => {
  if (!input || !input.trim()) return undefined
  return input.split(',').map((s) => s.trim()).filter(Boolean)
}
const arrayToTagsInput = (tags?: string) => {
  if (!tags) return ''
  try {
    const arr = JSON.parse(tags)
    return Array.isArray(arr) ? arr.join(', ') : ''
  } catch {
    return ''
  }
}

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await loadAgentList()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AgentDefApi.getAgentDef(id)
      // 回显能力标签逗号串；若该 agent 有高级字段则自动展开高级模式
      capabilityTagsInput.value = arrayToTagsInput(formData.value.capabilityTags)
      if (formData.value.agentKind || formData.value.verifierType
          || formData.value.omnigentAgentId || formData.value.mode === 'omnigent'
          || formData.value.parentDefId) {
        advanced.value = true
      }
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
    // 能力标签逗号串 → JSON 数组字符串
    const tags = tagsToArray(capabilityTagsInput.value)
    const data = { ...formData.value, capabilityTags: tags ? JSON.stringify(tags) : undefined } as unknown as AgentDefApi.AgentDefVO
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
  advanced.value = false
  capabilityTagsInput.value = ''
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
    source: 'manual',
    agentKind: undefined,
    capabilityTags: undefined,
    verifierType: undefined,
    isolationLevel: undefined,
    mode: 'local',
    parentDefId: undefined,
    omnigentAgentId: undefined
  }
  formRef.value?.resetFields()
}
</script>
