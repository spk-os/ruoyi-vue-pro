<template>
  <el-dialog v-model="visible" title="创建交付版本" width="560px" append-to-body>
    <el-form ref="formRef" v-loading="saving" :model="formData" :rules="rules" label-width="110px">
      <el-form-item label="版本类型" prop="versionType">
        <el-radio-group v-model="formData.versionType">
          <el-radio value="INCREMENT">增量（Vx.ss）</el-radio>
          <el-radio value="HOTFIX">热修</el-radio>
          <el-radio value="BASELINE">基线</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="小版本序号" prop="minorNo">
        <el-input-number v-model="formData.minorNo" :min="0" :max="99" />
        <span class="text-xs text-gray-400 ml-8px">留空由服务端分配</span>
      </el-form-item>
      <el-form-item label="主题" prop="name">
        <el-input v-model="formData.name" placeholder="如 多房间语音编排" />
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
      <el-form-item label="计划开始">
        <el-date-picker v-model="formData.plannedStartAt" class="!w-full" type="datetime" value-format="x" />
      </el-form-item>
      <el-form-item label="计划完成">
        <el-date-picker v-model="formData.plannedEndAt" class="!w-full" type="datetime" value-format="x" />
      </el-form-item>
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

defineOptions({ name: 'SpkIpdVersionForm' })
const message = useMessage()
const visible = ref(false)
const saving = ref(false)
const userList = ref<any[]>([])
const majorReleaseId = ref<number>()
const formRef = ref()
const formData = reactive({
  versionType: 'INCREMENT',
  minorNo: undefined as number | undefined,
  name: '',
  objective: '',
  scopeSummary: '',
  ownerUserId: undefined as number | undefined,
  plannedStartAt: null,
  plannedEndAt: null
})
const rules = {
  versionType: [{ required: true, message: '类型不能为空', trigger: 'change' }],
  objective: [{ required: true, message: '目标不能为空', trigger: 'blur' }],
  ownerUserId: [{ required: true, message: '负责人不能为空', trigger: 'change' }]
}
const reset = () => Object.assign(formData, { versionType: 'INCREMENT', minorNo: undefined, name: '', objective: '', scopeSummary: '', ownerUserId: undefined, plannedStartAt: null, plannedEndAt: null })
const open = async (mId: number) => {
  reset()
  majorReleaseId.value = mId
  userList.value = await getSimpleUserList()
  visible.value = true
}
const emit = defineEmits(['success'])
const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    await IpdBusinessApi.createVersion(majorReleaseId.value!, { ...formData })
    message.success('版本已创建（DRAFT）')
    emit('success')
    visible.value = false
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>
