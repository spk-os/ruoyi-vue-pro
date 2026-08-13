<template>
  <el-dialog v-model="visible" title="关联版本" width="500px" append-to-body>
    <el-descriptions :column="1" border class="mb-12px">
      <el-descriptions-item label="编号">{{ row.caseNo }}</el-descriptions-item>
      <el-descriptions-item label="标题">{{ row.title }}</el-descriptions-item>
    </el-descriptions>
    <el-form v-loading="saving" :model="formData" label-width="100px">
      <el-form-item label="关系类型">
        <el-radio-group v-model="formData.relationType">
          <el-radio value="FIXED_IN">修复于（FIXED_IN）</el-radio>
          <el-radio value="VERIFIED_IN">验证于（VERIFIED_IN）</el-radio>
          <el-radio value="AFFECTS">影响（AFFECTS）</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="目标版本">
        <el-select v-model="formData.versionId" filterable placeholder="选择版本">
          <el-option v-for="v in versions" :key="v.id" :label="v.versionNo" :value="v.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <el-alert type="info" :closable="false" show-icon>
      <template #title>FIXED_IN 是进入修复实施的前置；P0/P1 关闭前要求 VERIFIED_IN。</template>
    </el-alert>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button :loading="saving" type="primary" @click="submit">关联</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import * as IpdBusinessApi from '@/api/spk/ipd/business'

defineOptions({ name: 'SpkIpdIssueRelationDialog' })
const message = useMessage()
const visible = ref(false)
const saving = ref(false)
const versions = ref<IpdBusinessApi.SpkIpdVersionVO[]>([])
const row = ref<IpdBusinessApi.SpkIpdIssueCaseVO>({})
const formData = reactive({ relationType: 'FIXED_IN', versionId: undefined as number | undefined })
const open = async (r: IpdBusinessApi.SpkIpdIssueCaseVO) => {
  row.value = r
  formData.relationType = 'FIXED_IN'
  formData.versionId = undefined
  const pid = r.projectId
  if (pid) {
    const majors = await IpdBusinessApi.listMajorReleases(pid)
    const all: IpdBusinessApi.SpkIpdVersionVO[] = []
    for (const m of majors) { if (m.id) all.push(...(await IpdBusinessApi.listVersions(m.id))) }
    versions.value = all
  }
  visible.value = true
}
const emit = defineEmits(['success'])
const submit = async () => {
  if (!formData.versionId) { message.warning('请选择目标版本'); return }
  saving.value = true
  try {
    await IpdBusinessApi.addVersionRelation(row.value.id!, { ...formData })
    message.success('已关联')
    emit('success')
    visible.value = false
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>
