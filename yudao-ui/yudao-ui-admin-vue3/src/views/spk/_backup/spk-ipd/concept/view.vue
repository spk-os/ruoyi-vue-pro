<!--
  [归档 2026-08-14] 旧版 IPD 立项摘要视图（formCustomViewPath='/spk-ipd/concept/view'）。
  1:1 新版对应物 = views/spk/ipd/view.vue（同基线 5ecc4de0b9 引入副本，formCustomViewPath
  改为 '/spk/ipd/view'，9 字段 + 5 API 逐字相同）——信息层面无缺失，旧路径已被新路径替代，
  故旧文件迁此备份。门禁/裁决信息另散见于 spk/ipd/cockpit（ActivityDetail 的 gate_decision
  / verifications[].overallConclusion + Swimlane STAGE_ORDER），但非聚合立项摘要单页。
  注意：新旧两个 view 均靠 bpm_form.conf 的 formCustomViewPath(form_type=20) 动态加载，
  当前 bpm_form 表全空 → 两者运行时都未渲染，需建 form_type=20 表单配路径才激活。
  原路径：src/views/spk-ipd/concept/view.vue（baseline 5ecc4de0b9 引入）。
  ──────────────────────────────────────────────────────────────────────────
  SPK-OS IPD 立项摘要视图（form_type=20 业务表单组件）
  由 bpm/processInstance/detail/index.vue 的 BusinessFormComponent 加载，
  formCustomViewPath='/spk-ipd/concept/view' 经 routerHelper.registerComponent 子串命中本文件。
  IPD 流程 REST 发起无 businessKey（service 写死 null），故不依赖 :id，改用 :process-instance-id 取数。
-->
<template>
  <div class="spk-concept-view" v-loading="loading">
    <el-alert
      v-if="errorMsg"
      type="error"
      :title="errorMsg"
      :closable="false"
      show-icon
    />
    <el-empty
      v-else-if="!loading && !hasAny"
      description="暂无 IPD 立项摘要数据（流程尚未推进到产出门禁）"
    />
    <el-descriptions v-else :column="2" border size="default" title="IPD 立项摘要">
      <el-descriptions-item label="流程实例">{{ processInstanceId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="门禁进度">
        <el-tag v-if="gateSummary" :type="gateSummary.allPass ? 'success' : 'warning'">
          {{ gateSummary.passed }}/{{ gateSummary.total }} 通过
        </el-tag>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item label="门禁明细" :span="2">
        <el-tag
          v-for="g in gateList"
          :key="g.id"
          :type="g.pass === true ? 'success' : g.pass === false ? 'danger' : 'info'"
          class="mr-8px mb-4px"
        >
          {{ g.gate }}：{{ g.pass === true ? '通过' : g.pass === false ? '失败' : '待回调' }}
        </el-tag>
        <span v-if="!gateList.length">-</span>
      </el-descriptions-item>
      <el-descriptions-item label="Agent 产物">
        <el-tag :type="agentDoneCount === agentList.length ? 'success' : 'warning'">
          {{ agentDoneCount }}/{{ agentList.length }} 完成
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="Aegis 裁决">
        <el-tag v-if="aegis" :type="verdictType(aegis.verdict)">{{ aegis.verdict }}</el-tag>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item v-if="aegis" label="审查报告" :span="2">
        <pre class="json-pre">{{ aegis.report }}</pre>
      </el-descriptions-item>
      <el-descriptions-item label="R8 退市">
        <el-tag v-if="sunset">{{ sunset.archiveStatus || '-' }}</el-tag>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item label="R7 反馈">
        <el-tag v-if="feedbackCount" type="warning">{{ feedbackCount }} 条</el-tag>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item v-if="sunset" label="退市报告" :span="2">
        <pre class="json-pre">{{ sunset.sunsetReport }}</pre>
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script lang="ts" setup>
import * as SpkDeliveryApi from '@/api/spk/delivery'

defineOptions({ name: 'SpkIpdConceptView' })

const props = defineProps<{
  id?: number // businessKey（IPD 流程必空，保留以兼容 form_type=20 约定）
  processInstanceId?: string // 流程实例编号（detail/index.vue 传入，取数主键）
}>()

const loading = ref(false)
const errorMsg = ref('')
const gateList = ref<any[]>([])
const agentList = ref<any[]>([])
const aegis = ref<any>(null)
const sunset = ref<any>(null)
const feedbackList = ref<any[]>([])

const hasAny = computed(
  () =>
    !!gateList.value.length ||
    !!agentList.value.length ||
    !!aegis.value ||
    !!sunset.value ||
    !!feedbackList.value.length
)

const gateSummary = computed(() => {
  if (!gateList.value.length) return null
  const passed = gateList.value.filter((g) => g.pass === true).length
  return { passed, total: gateList.value.length, allPass: passed === gateList.value.length }
})

const agentDoneCount = computed(
  () => agentList.value.filter((a) => a.status === 'done').length
)

const feedbackCount = computed(() => feedbackList.value.length)

const verdictType = (v?: string) => {
  if (v === 'pass') return 'success'
  if (v === 'fail') return 'danger'
  return 'warning'
}

const loadAll = async () => {
  if (!props.processInstanceId) {
    errorMsg.value = '缺少流程实例编号，无法加载 IPD 立项摘要'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const [gate, agent, aeg, sun, fb] = await Promise.all([
      SpkDeliveryApi.getGateListByInstance(props.processInstanceId),
      SpkDeliveryApi.getAgentTaskListByInstance(props.processInstanceId),
      SpkDeliveryApi.getAegisByInstance(props.processInstanceId),
      SpkDeliveryApi.getSunsetByInstance(props.processInstanceId),
      SpkDeliveryApi.getFeedbackListByInstance(props.processInstanceId)
    ])
    gateList.value = gate || []
    agentList.value = agent || []
    aegis.value = aeg || null
    sunset.value = sun || null
    feedbackList.value = fb || []
  } catch (e: any) {
    errorMsg.value = 'IPD 立项摘要加载失败：' + (e?.message || String(e))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
})
</script>

<style lang="scss" scoped>
.spk-concept-view {
  padding: 4px 0;
}
.json-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 160px;
  overflow: auto;
  font-size: 12px;
  background: var(--el-fill-color-light);
  padding: 6px;
  border-radius: 4px;
}
.mr-8px {
  margin-right: 8px;
}
.mb-4px {
  margin-bottom: 4px;
}
</style>
