<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="编队名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入编队名称" />
      </el-form-item>
      <el-form-item label="编码" prop="code">
        <el-input v-model="formData.code" placeholder="请输入编码（如 ipd-review-squad）" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="2"
          placeholder="请输入描述"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio value="active">启用</el-radio>
          <el-radio value="disabled">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="杂项配置" prop="config">
        <el-input
          v-model="formData.config"
          type="textarea"
          :rows="2"
          placeholder="JSON 字符串，选填"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import * as AgentSquadApi from '@/api/spk/agentsquad'

defineOptions({ name: 'SpkAgentSquadForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref({
  id: undefined,
  name: undefined,
  code: undefined,
  description: undefined,
  status: 'active',
  config: undefined
})
const formRules = reactive({
  name: [{ required: true, message: '编队名称不能为空', trigger: 'blur' }],
  code: [{ required: true, message: '编码不能为空', trigger: 'blur' }]
})
const formRef = ref()

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AgentSquadApi.getAgentSquad(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open})

const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data = formData.value as unknown as AgentSquadApi.AgentSquadVO
    if (formType.value === 'create') {
      await AgentSquadApi.createAgentSquad(data)
      message.success(t('common.createSuccess'))
    } else {
      await AgentSquadApi.updateAgentSquad(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    id: undefined,
    name: undefined,
    code: undefined,
    description: undefined,
    status: 'active',
    config: undefined
  }
  formRef.value?.resetFields()
}
</script>
