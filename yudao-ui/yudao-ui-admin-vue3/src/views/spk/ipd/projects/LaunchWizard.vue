<template>
  <el-dialog v-model="visible" width="780px" title="发起项目" :close-on-click-modal="false" destroy-on-close>
    <el-steps :active="step" align-center finish-status="success">
      <el-step title="项目信息" />
      <el-step title="大版本" />
      <el-step title="版本" />
      <el-step title="首个流程" />
    </el-steps>

    <div class="mt-20px">
      <!-- 步骤1 项目信息 -->
      <el-form v-if="step === 0" ref="projFormRef" :model="projForm" :rules="projRules" label-width="100px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="projForm.name" placeholder="如 智能家居中控 V2" />
        </el-form-item>
        <el-form-item label="项目编码" prop="projectCode">
          <el-input v-model="projForm.projectCode" placeholder="如 SMART-HOME-V2" />
        </el-form-item>
        <el-form-item label="负责人" prop="ownerUserId">
          <el-select v-model="projForm.ownerUserId" filterable placeholder="选择负责人" class="!w-full">
            <el-option v-for="u in userList" :key="u.id" :label="u.nickname" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标">
          <el-input v-model="projForm.objective" type="textarea" :rows="2" placeholder="一句话目标" />
        </el-form-item>
        <el-form-item label="计划完成">
          <el-date-picker v-model="projForm.plannedEndAt" type="date" value-format="YYYY-MM-DD" class="!w-full" />
        </el-form-item>
      </el-form>

      <!-- 步骤2 大版本 -->
      <el-form v-else-if="step === 1" ref="mrFormRef" :model="mrForm" :rules="mrRules" label-width="110px">
        <el-form-item label="大版本号" prop="majorVersion">
          <el-input v-model="mrForm.majorVersion" placeholder="如 V1.0" />
        </el-form-item>
        <el-form-item label="版本名">
          <el-input v-model="mrForm.name" placeholder="如 首个正式大版本" />
        </el-form-item>
        <el-form-item label="里程碑">
          <el-date-picker v-model="mrForm.milestoneDate" type="date" value-format="YYYY-MM-DD" class="!w-full" />
        </el-form-item>
      </el-form>

      <!-- 步骤3 版本 -->
      <el-form v-else-if="step === 2" ref="verFormRef" :model="verForm" :rules="verRules" label-width="110px">
        <el-form-item label="版本号" prop="versionNo">
          <el-input v-model="verForm.versionNo" placeholder="如 V1.0.0" />
        </el-form-item>
        <el-form-item label="版本名">
          <el-input v-model="verForm.name" placeholder="如 初始版本" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="verForm.type" class="!w-full">
            <el-option label="主干" value="MAINLINE" />
            <el-option label="里程碑" value="MILESTONE" />
            <el-option label="补丁" value="PATCH" />
          </el-select>
        </el-form-item>
        <el-form-item label="就绪校验">
          <div v-if="readiness" class="w-full">
            <el-tag :type="readiness.ready ? 'success' : 'warning'" size="small">
              {{ readiness.ready ? '就绪' : '尚有缺口' }}
            </el-tag>
            <span class="ml-8px text-xs text-gray-500">{{ readiness.summary || '' }}</span>
          </div>
          <el-button v-else size="small" :loading="checking" @click="checkReadiness">执行就绪校验</el-button>
        </el-form-item>
      </el-form>

      <!-- 步骤4 首个流程 -->
      <div v-else-if="step === 3">
        <el-alert type="info" :closable="false" show-icon>
          首个 IPD 流程将基于刚创建的版本发起。选好流程类型与流程码后即可启动（幂等键自动生成）。
        </el-alert>
        <el-form class="mt-16px" label-width="110px">
          <el-form-item label="流程类型">
            <el-select v-model="flowForm.flowType" class="!w-full" @change="onFlowTypeChange">
              <el-option label="立项交付 (CONCEPT_DELIVERY)" value="CONCEPT_DELIVERY" />
              <el-option label="问题解决 (ISSUE_RESOLUTION)" value="ISSUE_RESOLUTION" />
            </el-select>
          </el-form-item>
          <el-form-item label="流程码">
            <el-input v-model="flowForm.flowCode" placeholder="自动带出，可改" />
          </el-form-item>
          <el-form-item label="幂等键">
            <el-input v-model="flowForm.idempotencyKey" disabled />
          </el-form-item>
          <el-form-item label="预检">
            <el-button :loading="preflighting" size="small" @click="doPreflight">执行预检</el-button>
            <span v-if="preflightMsg" class="ml-8px text-xs" :class="preflightOk ? 'text-green-600' : 'text-red-600'">{{ preflightMsg }}</span>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <template #footer>
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <el-button v-if="step < 3" type="primary" :loading="nexting" @click="next">下一步</el-button>
      <el-button v-else type="primary" :loading="launching" @click="launch">完成并启动</el-button>
      <el-button @click="visible = false">取消</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import * as BusinessApi from '@/api/spk/ipd/business'
import { getSimpleUserList, type UserVO } from '@/api/system/user'

const emit = defineEmits(['success'])
const message = useMessage()

const visible = ref(false)
const step = ref(0)
const userList = ref<UserVO[]>([])

const projFormRef = ref()
const mrFormRef = ref()
const verFormRef = ref()

const projForm = reactive({
  name: '',
  projectCode: '',
  ownerUserId: undefined as number | undefined,
  objective: '',
  plannedEndAt: ''
})
const projRules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectCode: [{ required: true, message: '请输入项目编码', trigger: 'blur' }],
  ownerUserId: [{ required: true, message: '请选择负责人', trigger: 'change' }]
}

const mrForm = reactive({ majorVersion: 'V1.0', name: '', milestoneDate: '' })
const mrRules = { majorVersion: [{ required: true, message: '请输入大版本号', trigger: 'blur' }] }

const verForm = reactive({ versionNo: 'V1.0.0', name: '', type: 'MAINLINE' })
const verRules = { versionNo: [{ required: true, message: '请输入版本号', trigger: 'blur' }] }

const flowForm = reactive({
  flowType: 'CONCEPT_DELIVERY',
  flowCode: 'spk-ipd-concept',
  idempotencyKey: ''
})

// 各阶段创建产物
const projectId = ref<number>()
const majorReleaseId = ref<number>()
const versionId = ref<number>()

const nexting = ref(false)
const checking = ref(false)
const readiness = ref<any>(null)
const preflighting = ref(false)
const preflightMsg = ref('')
const preflightOk = ref(false)
const launching = ref(false)

const open = async () => {
  step.value = 0
  visible.value = true
  projectId.value = undefined
  majorReleaseId.value = undefined
  versionId.value = undefined
  readiness.value = null
  preflightMsg.value = ''
  flowForm.idempotencyKey = ''
  Object.assign(projForm, { name: '', projectCode: '', ownerUserId: undefined, objective: '', plannedEndAt: '' })
  Object.assign(mrForm, { majorVersion: 'V1.0', name: '', milestoneDate: '' })
  Object.assign(verForm, { versionNo: 'V1.0.0', name: '', type: 'MAINLINE' })
  flowForm.flowType = 'CONCEPT_DELIVERY'
  flowForm.flowCode = 'spk-ipd-concept'
  if (userList.value.length === 0) userList.value = await getSimpleUserList()
}

defineExpose({ open })

const onFlowTypeChange = (v: string) => {
  flowForm.flowCode = v === 'CONCEPT_DELIVERY' ? 'spk-ipd-concept' : 'spk-ipd-issue-resolution'
}

const next = async () => {
  nexting.value = true
  try {
    if (step.value === 0) {
      await projFormRef.value.validate()
      const created = await BusinessApi.create(projForm as any)
      projectId.value = created.id
      // 创建首个大版本
      majorReleaseId.value = (await BusinessApi.createMajorRelease(created.id!, {
        majorVersion: mrForm.majorVersion,
        name: mrForm.name || undefined,
        milestoneDate: mrForm.milestoneDate || undefined
      } as any))?.id
    } else if (step.value === 1) {
      await mrFormRef.value.validate()
      // 大版本已在步骤1创建，此处仅校验
    } else if (step.value === 2) {
      await verFormRef.value.validate()
      const ver = await BusinessApi.createVersion(majorReleaseId.value!, {
        versionNo: verForm.versionNo,
        name: verForm.name || undefined,
        type: verForm.type
      } as any)
      versionId.value = ver.id
      flowForm.idempotencyKey = `launch-${projectId.value}-${Date.now()}`
    }
    step.value++
  } catch (e) {
    // 校验/接口失败不前进
  } finally {
    nexting.value = false
  }
}

const checkReadiness = async () => {
  if (!versionId.value) return
  checking.value = true
  try {
    readiness.value = await BusinessApi.readiness(versionId.value)
  } finally {
    checking.value = false
  }
}

const doPreflight = async () => {
  if (!versionId.value) return
  preflighting.value = true
  preflightMsg.value = ''
  try {
    const res = await BusinessApi.preflight({
      projectId: projectId.value,
      versionId: versionId.value,
      flowType: flowForm.flowType,
      flowCode: flowForm.flowCode
    } as any)
    preflightOk.value = !!res?.ready
    preflightMsg.value = res?.ready ? '预检通过，可启动' : `预检未通过：${res?.summary || res?.reason || '存在缺口'}`
  } catch (e: any) {
    preflightOk.value = false
    preflightMsg.value = `预检失败：${e?.message || e}`
  } finally {
    preflighting.value = false
  }
}

const launch = async () => {
  if (!preflightOk.value) {
    await message.confirm('预检未通过或未执行，仍要尝试发起流程？')
  }
  launching.value = true
  try {
    const fr = await BusinessApi.createFlowRun({
      projectId: projectId.value,
      versionId: versionId.value,
      flowType: flowForm.flowType,
      flowCode: flowForm.flowCode
    } as any)
    await BusinessApi.startFlowRun(fr.id, flowForm.idempotencyKey)
    message.success(`流程已启动（FlowRun #${fr.id}）`)
    visible.value = false
    emit('success')
  } catch (e: any) {
    message.error(`启动失败：${e?.message || e}`)
  } finally {
    launching.value = false
  }
}
</script>
