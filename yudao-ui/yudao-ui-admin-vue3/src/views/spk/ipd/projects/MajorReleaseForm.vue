<template>
  <el-dialog v-model="visible" :title="title" width="560px" append-to-body>
    <el-form ref="formRef" v-loading="saving" :model="formData" :rules="rules" label-width="110px">
      <el-form-item label="大版本序号" prop="majorNo">
        <el-input-number v-model="formData.majorNo" :min="1" :max="99" />
        <div class="text-xs text-gray-400">对应 Vx，如 1 → V1</div>
      </el-form-item>
      <el-form-item label="主题" prop="name">
        <el-input v-model="formData.name" placeholder="如 首个商用版本" />
      </el-form-item>
      <el-form-item label="目标" prop="objective">
        <el-input v-model="formData.objective" :rows="2" type="textarea" />
      </el-form-item>
      <el-form-item label="范围摘要" prop="scopeSummary">
        <el-input v-model="formData.scopeSummary" :rows="2" type="textarea" />
      </el-form-item>
      <el-form-item label="负责人" prop="ownerUserId">
        <el-select v-model="formData.ownerUserId" filterable placeholder="选择负责人">
          <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="基线版本">
        <el-switch v-model="formData.createBaselineVersion" />
        <span class="text-xs text-gray-400 ml-8px">同时创建 Vx.0 基线版本草稿</span>
      </el-form-item>
      <template v-if="formData.createBaselineVersion">
        <el-form-item label="基线计划开始">
          <el-date-picker v-model="formData.baselinePlan.plannedStartAt" class="!w-full" type="datetime" value-format="x" />
        </el-form-item>
        <el-form-item label="基线计划完成">
          <el-date-picker v-model="formData.baselinePlan.plannedEndAt" class="!w-full" type="datetime" value-format="x" />
        </el-form-item>
      </template>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button :loading="saving" type="primary" @click="submit">创建</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import { getSimpleUserList } from '@/api/system/user'

defineOptions({ name: 'SpkIpdMajorReleaseForm' })
const props = defineProps<{ projectId?: number }>()
const message = useMessage()
const visible = ref(false)
const saving = ref(false)
const title = '创建大版本'
const userList = ref<any[]>([])
const formRef = ref()
const formData = reactive({
  majorNo: 1,
  name: '',
  objective: '',
  scopeSummary: '',
  ownerUserId: undefined as number | undefined,
  createBaselineVersion: true,
  baselinePlan: { plannedStartAt: null, plannedEndAt: null }
})
const rules = {
  majorNo: [{ required: true, message: '序号不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '主题不能为空', trigger: 'blur' }],
  objective: [{ required: true, message: '目标不能为空', trigger: 'blur' }],
  ownerUserId: [{ required: true, message: '负责人不能为空', trigger: 'change' }]
}
const reset = () => Object.assign(formData, { majorNo: 1, name: '', objective: '', scopeSummary: '', ownerUserId: undefined, createBaselineVersion: true, baselinePlan: { plannedStartAt: '', plannedEndAt: '' } })
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
    await IpdBusinessApi.createMajorRelease(props.projectId!, { ...formData })
    message.success('大版本已创建')
    emit('success')
    visible.value = false
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>
