<template>
  <el-dialog v-model="visible" title="登记问题" width="560px" append-to-body>
    <el-form ref="formRef" v-loading="saving" :model="formData" :rules="rules" label-width="100px">
      <el-form-item label="问题类型" prop="issueType">
        <el-select v-model="formData.issueType" placeholder="选择类型">
          <el-option v-for="o in ISSUE_TYPES" :key="o" :label="o" :value="o" />
        </el-select>
      </el-form-item>
      <el-form-item label="严重度" prop="severity">
        <el-radio-group v-model="formData.severity">
          <el-radio v-for="s in SEVERITIES" :key="s" :value="s">{{ s }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="formData.description" :rows="3" type="textarea" />
      </el-form-item>
      <el-form-item label="责任人" prop="ownerUserId">
        <el-select v-model="formData.ownerUserId" filterable clearable placeholder="可稍后分诊">
          <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button :loading="saving" type="primary" @click="submit">登记</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import { getSimpleUserList } from '@/api/system/user'

defineOptions({ name: 'SpkIpdIssueForm' })
const props = defineProps<{ projectId?: number }>()
const message = useMessage()
const visible = ref(false)
const saving = ref(false)
const userList = ref<any[]>([])
const formRef = ref()
const ISSUE_TYPES = ['DEFECT', 'INCIDENT', 'CUSTOMER_ISSUE', 'TECH_DEBT', 'SECURITY', 'COMPLIANCE', 'CHANGE_REQUEST']
const SEVERITIES = ['P0', 'P1', 'P2', 'P3']
const formData = reactive({
  issueType: 'DEFECT',
  severity: 'P2',
  title: '',
  description: '',
  ownerUserId: undefined as number | undefined,
  projectId: undefined as number | undefined
})
const rules = {
  issueType: [{ required: true, message: '类型不能为空', trigger: 'change' }],
  severity: [{ required: true, message: '严重度不能为空', trigger: 'change' }],
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }]
}
const reset = () => Object.assign(formData, { issueType: 'DEFECT', severity: 'P2', title: '', description: '', ownerUserId: undefined, projectId: props.projectId })
const open = async () => {
  reset()
  userList.value = await getSimpleUserList()
  visible.value = true
}
const emit = defineEmits(['success'])
const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    await IpdBusinessApi.createIssue(props.projectId!, { ...formData })
    message.success('问题已登记')
    emit('success')
    visible.value = false
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>
