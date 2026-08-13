<template>
  <el-dialog v-model="visible" title="问题分诊" width="500px" append-to-body>
    <el-descriptions :column="1" border class="mb-12px">
      <el-descriptions-item label="编号">{{ row.caseNo }}</el-descriptions-item>
      <el-descriptions-item label="标题">{{ row.title }}</el-descriptions-item>
    </el-descriptions>
    <el-form ref="formRef" v-loading="saving" :model="formData" label-width="100px">
      <el-form-item label="严重度">
        <el-radio-group v-model="formData.severity">
          <el-radio v-for="s in SEVERITIES" :key="s" :value="s">{{ s }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="责任人">
        <el-select v-model="formData.ownerUserId" filterable clearable>
          <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="影响版本">
        <el-select v-model="formData.affectedVersionIds" multiple filterable placeholder="选择影响版本">
          <el-option v-for="v in versions" :key="v.id" :label="v.versionNo" :value="v.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button :loading="saving" type="primary" @click="submit">分诊</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import { getSimpleUserList } from '@/api/system/user'

defineOptions({ name: 'SpkIpdIssueTriageDialog' })
const message = useMessage()
const visible = ref(false)
const saving = ref(false)
const userList = ref<any[]>([])
const versions = ref<IpdBusinessApi.SpkIpdVersionVO[]>([])
const row = ref<IpdBusinessApi.SpkIpdIssueCaseVO>({})
const formData = reactive({ severity: '', ownerUserId: undefined as number | undefined, affectedVersionIds: [] as number[] })
const SEVERITIES = ['P0', 'P1', 'P2', 'P3']
const open = async (r: IpdBusinessApi.SpkIpdIssueCaseVO) => {
  row.value = r
  formData.severity = r.severity || 'P2'
  formData.ownerUserId = r.ownerUserId
  formData.affectedVersionIds = []
  userList.value = await getSimpleUserList()
  // 加载项目下全部版本作为影响版本候选
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
  saving.value = true
  try {
    await IpdBusinessApi.triageIssue(row.value.id!, { ...formData })
    message.success('分诊完成')
    emit('success')
    visible.value = false
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>
