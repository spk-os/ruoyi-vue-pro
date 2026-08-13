<template>
  <el-drawer v-model="visible" :before-close="close" :title="title" size="520px">
    <el-form ref="formRef" v-loading="saving" :model="formData" :rules="rules" label-width="100px">
      <el-form-item label="项目名称" prop="name">
        <el-input v-model="formData.name" placeholder="如 智能家居中控" />
      </el-form-item>
      <el-form-item label="项目编码" prop="projectCode">
        <el-input v-model="formData.projectCode" :disabled="isUpdate" placeholder="如 CORTEXT" />
        <div class="text-xs text-gray-400">稳定短码，创建后不可改；租户内唯一</div>
      </el-form-item>
      <el-form-item label="负责人" prop="ownerUserId">
        <el-select v-model="formData.ownerUserId" filterable placeholder="选择负责人">
          <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="成功目标" prop="objective">
        <el-input v-model="formData.objective" :rows="2" type="textarea" placeholder="如 形成可追溯的 AI-IPD 交付闭环" />
      </el-form-item>
      <el-form-item label="背景与边界" prop="description">
        <el-input v-model="formData.description" :rows="3" type="textarea" placeholder="说明项目边界与不在范围内的事项" />
      </el-form-item>
      <el-form-item label="计划开始" prop="plannedStartAt">
        <el-date-picker v-model="formData.plannedStartAt" class="!w-full" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
      </el-form-item>
      <el-form-item label="计划完成" prop="plannedEndAt">
        <el-date-picker v-model="formData.plannedEndAt" class="!w-full" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
      </el-form-item>
    </el-form>
    <el-alert class="mt-10px" type="info" :closable="false" show-icon>
      <template #title>「保存草稿」只创建 DRAFT 项目，<b>不自动启动流程</b>。外部绑定与团队角色可在项目详情中配置。</template>
    </el-alert>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button :loading="saving" type="primary" @click="submit">保存</el-button>
    </template>
  </el-drawer>
</template>

<script lang="ts" setup>
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import { getSimpleUserList } from '@/api/system/user'

defineOptions({ name: 'SpkIpdProjectForm' })

const message = useMessage()
const visible = ref(false)
const saving = ref(false)
const isUpdate = ref(false)
const title = computed(() => (isUpdate.value ? '编辑项目' : '新建项目'))
const userList = ref<any[]>([])
const formRef = ref()
const detail = ref<IpdBusinessApi.SpkIpdProjectVO>({})

const formData = reactive({
  name: '',
  projectCode: '',
  ownerUserId: undefined as number | undefined,
  objective: '',
  description: '',
  plannedStartAt: '',
  plannedEndAt: ''
})
const rules = {
  name: [{ required: true, message: '项目名称不能为空', trigger: 'blur' }],
  projectCode: [{ required: true, message: '项目短码不能为空', trigger: 'blur' }],
  ownerUserId: [{ required: true, message: '负责人不能为空', trigger: 'change' }],
  objective: [{ required: true, message: '成功目标不能为空', trigger: 'blur' }]
}

const open = async (id?: number) => {
  reset()
  userList.value = await getSimpleUserList()
  if (id) {
    isUpdate.value = true
    const d = await IpdBusinessApi.get(id)
    detail.value = d
    Object.assign(formData, {
      name: d.name, projectCode: d.projectCode, ownerUserId: d.ownerUserId,
      objective: d.objective || '', description: d.description || '',
      plannedStartAt: d.plannedStartAt || '', plannedEndAt: d.plannedEndAt || ''
    })
  } else {
    isUpdate.value = false
  }
  visible.value = true
}
const reset = () => {
  Object.assign(formData, {
    name: '', projectCode: '', ownerUserId: undefined,
    objective: '', description: '', plannedStartAt: '', plannedEndAt: ''
  })
}
const close = () => { visible.value = false; reset() }

const emit = defineEmits(['success'])
const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (isUpdate.value) {
      await IpdBusinessApi.update(detail.value.id!, {
        ...formData,
        lockVersion: detail.value.lockVersion
      } as any)
      message.success('修改成功')
    } else {
      await IpdBusinessApi.create(formData as any)
      message.success('新建成功（DRAFT 草稿，未启动流程）')
    }
    emit('success')
    close()
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>
