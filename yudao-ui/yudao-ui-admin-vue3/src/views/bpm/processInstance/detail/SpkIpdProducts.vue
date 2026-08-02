<template>
  <div class="spk-ipd-products" v-loading="loading">
    <el-alert v-if="errorMsg" type="error" :title="errorMsg" :closable="false" show-icon />
    <el-empty v-else-if="!loading && emptyAll" description="暂无 IPD 产物数据" />
    <el-collapse v-model="activeNames" v-else>
      <!-- 门禁记录 -->
      <el-collapse-item title="门禁记录（G1-G8 / TR）" name="gate">
        <el-table :data="gateList" size="small" border>
          <el-table-column label="门禁" prop="gate" width="90" />
          <el-table-column label="节点 key" prop="nodeKey" width="160" />
          <el-table-column label="通过" prop="pass" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.pass === true" type="success">通过</el-tag>
              <el-tag v-else-if="row.pass === false" type="danger">失败</el-tag>
              <el-tag v-else type="info">待回调</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="回调时间" prop="callbackTime" width="170" />
          <el-table-column label="报告" prop="report" show-overflow-tooltip />
        </el-table>
      </el-collapse-item>

      <!-- Agent 产物 -->
      <el-collapse-item title="Agent 产物" name="agent">
        <el-table :data="agentList" size="small" border>
          <el-table-column label="节点 key" prop="nodeKey" width="160" />
          <el-table-column label="角色 id" prop="roleId" width="90" />
          <el-table-column label="状态" prop="status" width="100">
            <template #default="{ row }">
              <el-tag :type="agentStatusType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="taskId" prop="taskId" width="160" />
          <el-table-column label="产物" prop="result" show-overflow-tooltip />
        </el-table>
      </el-collapse-item>

      <!-- Aegis 裁决 -->
      <el-collapse-item title="Aegis 裁决" name="aegis">
        <el-descriptions v-if="aegis" :column="2" border size="small">
          <el-descriptions-item label="审查编号">{{ aegis.reviewId }}</el-descriptions-item>
          <el-descriptions-item label="节点 key">{{ aegis.nodeKey }}</el-descriptions-item>
          <el-descriptions-item label="裁决">
            <el-tag :type="verdictType(aegis.verdict)">{{ aegis.verdict }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审查报告">{{ aegis.report }}</el-descriptions-item>
          <el-descriptions-item label="证据" :span="2">
            <pre class="json-pre">{{ aegis.evidence }}</pre>
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无 Aegis 裁决" :image-size="60" />
      </el-collapse-item>

      <!-- CCB 台账 -->
      <el-collapse-item title="CCB 变更台账" name="ccb">
        <el-table :data="ccbList" size="small" border>
          <el-table-column label="变更编号" prop="changeId" width="160" />
          <el-table-column label="决议" prop="decision" width="100">
            <template #default="{ row }">
              <el-tag :type="decisionType(row.decision)">{{ row.decision }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="变更请求" prop="changeRequest" show-overflow-tooltip />
          <el-table-column label="影响分析" prop="impact" show-overflow-tooltip />
        </el-table>
      </el-collapse-item>

      <!-- DCP 回退日志 -->
      <el-collapse-item title="DCP 回退日志" name="dcp">
        <el-table :data="dcpList" size="small" border>
          <el-table-column label="DCP" prop="dcp" width="90" />
          <el-table-column label="已回退次数" prop="redirectCount" width="110" align="center" />
          <el-table-column label="回退目标节点" prop="targetNode" width="180" />
        </el-table>
      </el-collapse-item>

      <!-- R7 反馈 -->
      <el-collapse-item title="R7 反馈" name="feedback">
        <el-table :data="feedbackList" size="small" border>
          <el-table-column label="来源" prop="source" width="120" />
          <el-table-column label="新章程种子" prop="newCharterSeed" width="110" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.newCharterSeed" type="warning">是</el-tag>
              <span v-else>否</span>
            </template>
          </el-table-column>
          <el-table-column label="内容" prop="content" show-overflow-tooltip />
          <el-table-column label="摘要" prop="summary" show-overflow-tooltip />
        </el-table>
      </el-collapse-item>

      <!-- R8 退市 -->
      <el-collapse-item title="R8 退市" name="sunset">
        <el-descriptions v-if="sunset" :column="2" border size="small">
          <el-descriptions-item label="归档状态">
            <el-tag>{{ sunset.archiveStatus }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="退市报告" :span="2">
            <pre class="json-pre">{{ sunset.sunsetReport }}</pre>
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂无 R8 退市记录" :image-size="60" />
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script lang="ts" setup>
import * as SpkDeliveryApi from '@/api/spk/delivery'

defineOptions({ name: 'SpkIpdProducts' })

const props = defineProps<{
  processInstanceId: string // BPM 流程实例编号
}>()

const loading = ref(false)
const errorMsg = ref('')
const activeNames = ref<string[]>(['gate', 'agent', 'aegis'])

const gateList = ref<any[]>([])
const agentList = ref<any[]>([])
const aegis = ref<any>(null)
const ccbList = ref<any[]>([])
const dcpList = ref<any[]>([])
const feedbackList = ref<any[]>([])
const sunset = ref<any>(null)

const emptyAll = computed(
  () =>
    !gateList.value.length &&
    !agentList.value.length &&
    !aegis.value &&
    !ccbList.value.length &&
    !dcpList.value.length &&
    !feedbackList.value.length &&
    !sunset.value
)

const agentStatusType = (s?: string) => {
  if (s === 'done') return 'success'
  if (s === 'failed') return 'danger'
  if (s === 'running') return 'warning'
  return 'info'
}
const verdictType = (v?: string) => {
  if (v === 'pass') return 'success'
  if (v === 'fail') return 'danger'
  return 'warning'
}
const decisionType = (d?: string) => {
  if (d === 'approve') return 'success'
  if (d === 'reject') return 'danger'
  return 'info'
}

const loadAll = async () => {
  if (!props.processInstanceId) return
  loading.value = true
  errorMsg.value = ''
  try {
    // 用 allSettled：单个接口失败不静默吞，标记 errorMsg 但其余组照常展示
    const results = await Promise.allSettled([
      SpkDeliveryApi.getGateListByInstance(props.processInstanceId),
      SpkDeliveryApi.getAgentTaskListByInstance(props.processInstanceId),
      SpkDeliveryApi.getAegisByInstance(props.processInstanceId),
      SpkDeliveryApi.getCcbListByInstance(props.processInstanceId),
      SpkDeliveryApi.getDcpListByInstance(props.processInstanceId),
      SpkDeliveryApi.getFeedbackListByInstance(props.processInstanceId),
      SpkDeliveryApi.getSunsetByInstance(props.processInstanceId)
    ])
    const failed = results.filter((r) => r.status === 'rejected')
    if (failed.length) {
      errorMsg.value = `${failed.length} 个产物接口查询失败（详情见 console）`
      failed.forEach((r) => console.error('[SpkIpdProducts] 查询失败', (r as PromiseRejectedResult).reason))
    }
    const v = (i: number, dft: any) =>
      results[i].status === 'fulfilled' ? (results[i] as PromiseFulfilledResult<any>).value : dft
    gateList.value = v(0, []) || []
    agentList.value = v(1, []) || []
    aegis.value = v(2, null) || null
    ccbList.value = v(3, []) || []
    dcpList.value = v(4, []) || []
    feedbackList.value = v(5, []) || []
    sunset.value = v(6, null) || null
  } catch (e: any) {
    errorMsg.value = 'IPD 产物查询失败：' + (e?.message || String(e))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
})
</script>

<style lang="scss" scoped>
.spk-ipd-products {
  padding: 0 10px;
}
.json-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 240px;
  overflow: auto;
  font-size: 12px;
  background: var(--el-fill-color-light);
  padding: 6px;
  border-radius: 4px;
}
</style>
