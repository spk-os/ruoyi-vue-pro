<!--
  IPD Activity 详情：三件套全流程可视。
  1. 基本信息（Task Contract）2. 输入 ContextManifest URI 3. 产物 ArtifactManifest（含 hash 校验状态）
  4. 执行 RunReceipt 5. 验证 VerificationReceipt 6. 证据链 timeline + 哈希链校验结果
  铁律 4：三件套缺失不得 completed —— 若任一缺失，顶部告警。
-->
<template>
  <div class="activity-detail" v-loading="loading">
    <div class="bar">
      <el-input
        v-model="runId"
        placeholder="输入 ActivityRunId（如 run-xxxx...）"
        clearable
        style="width: 420px"
        @keyup.enter="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-alert v-if="errorMsg" type="error" :title="errorMsg" :closable="false" show-icon />

    <template v-if="data">
      <el-alert
        v-if="!hasThreePiece"
        type="warning"
        title="三件套不齐全（ContextManifest / ArtifactManifest / RunReceipt 任一缺失）—— 铁律 4：不得标记 completed"
        :closable="false"
        show-icon
        class="mt-10px"
      />

      <el-descriptions :column="2" border title="基本信息（Task Contract）" class="mt-10px">
        <el-descriptions-item label="activityRunId">{{ data.activityRunId }}</el-descriptions-item>
        <el-descriptions-item label="activityId">{{ data.contract?.activityId }}</el-descriptions-item>
        <el-descriptions-item label="阶段">{{ data.contract?.phase }}</el-descriptions-item>
        <el-descriptions-item label="Lead">{{ data.contract?.leadAgentCode }}</el-descriptions-item>
        <el-descriptions-item label="合同状态">
          <el-tag :type="contractTagType">{{ data.contract?.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="nodeKey">{{ data.contract?.nodeKey }}</el-descriptions-item>
        <el-descriptions-item label="contractId">{{ data.contract?.contractId }}</el-descriptions-item>
        <el-descriptions-item label="modelSnapshotId">{{ data.contract?.modelSnapshotId }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="1" border title="① 输入 ContextManifest" class="mt-10px">
        <el-descriptions-item label="contextManifestUri">
          {{ data.contextManifestUri || '—' }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="section-title">② 产物 ArtifactManifest（{{ (data.artifacts || []).length }}）</div>
      <el-table :data="data.artifacts || []" size="small" border>
        <el-table-column prop="artifactId" label="artifactId" min-width="180" />
        <el-table-column prop="artifactType" label="类型" width="160" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'signed' ? 'success' : row.status === 'quarantined' ? 'danger' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="hash" label="hash" min-width="220" show-overflow-tooltip />
        <el-table-column prop="signedBy" label="签署方" width="140" />
        <el-table-column prop="uri" label="uri" min-width="220" show-overflow-tooltip />
      </el-table>

      <el-descriptions :column="2" border title="③ 执行 RunReceipt" class="mt-10px">
        <el-descriptions-item label="runReceiptId">{{ data.runReceipt?.runId }}</el-descriptions-item>
        <el-descriptions-item label="provider/model">
          {{ data.runReceipt?.provider }} / {{ data.runReceipt?.model }}
        </el-descriptions-item>
        <el-descriptions-item label="startedAt">{{ fmt(data.runReceipt?.startedAt) }}</el-descriptions-item>
        <el-descriptions-item label="finishedAt">{{ fmt(data.runReceipt?.finishedAt) }}</el-descriptions-item>
        <el-descriptions-item label="conversationId">{{ data.runReceipt?.conversationId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ data.runReceipt?.status }}</el-descriptions-item>
      </el-descriptions>

      <div class="section-title">④ 验证 VerificationReceipt（{{ (data.verifications || []).length }}）</div>
      <el-table :data="data.verifications || []" size="small" border>
        <el-table-column prop="verifierCode" label="verifier" width="160" />
        <el-table-column prop="overallConclusion" label="结论" width="120">
          <template #default="{ row }">
            <el-tag :type="verdictType(row.overallConclusion)">{{ row.overallConclusion }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="240" show-overflow-tooltip />
        <el-table-column prop="pointsJson" label="points" min-width="240" show-overflow-tooltip />
      </el-table>

      <div class="section-title">
        ⑤ 证据链（{{ (data.evidenceChain || []).length }} 条）
        <el-tag :type="data.chainValid ? 'success' : 'danger'" class="ml-8px">
          哈希链 {{ data.chainValid ? '校验通过' : '断裂' }}
        </el-tag>
      </div>
      <el-timeline>
        <el-timeline-item
          v-for="(ev, i) in data.evidenceChain || []"
          :key="i"
          :timestamp="fmt(ev.createTime)"
          placement="top"
        >
          <el-tag size="small" type="info">{{ ev.evidenceType }}</el-tag>
          <span class="ev-ref">ref：{{ ev.refId }}</span>
          <div class="ev-hash">prev：{{ shortHash(ev.prevHash) }} → row：{{ shortHash(ev.rowHash) }}</div>
        </el-timeline-item>
      </el-timeline>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getActivityDetail } from '@/api/spk/ipd/cockpit'

const props = defineProps<{ activityRunId?: string }>()

const runId = ref(props.activityRunId || '')
const loading = ref(false)
const errorMsg = ref('')
const data = ref<any>(null)

const hasThreePiece = computed(() => {
  if (!data.value) return false
  return !!data.value.contextManifestUri && (data.value.artifacts || []).length > 0 && !!data.value.runReceipt
})
const contractTagType = computed(() => {
  const s = data.value?.contract?.status
  if (s === 'done') return 'success'
  if (s === 'failed') return 'danger'
  if (s === 'running') return 'warning'
  return 'info'
})

const load = async () => {
  if (!runId.value) {
    errorMsg.value = '请输入 ActivityRunId'
    return
  }
  loading.value = true
  errorMsg.value = ''
  data.value = null
  try {
    data.value = await getActivityDetail(runId.value)
  } catch (e: any) {
    errorMsg.value = e?.message || '查询失败'
  } finally {
    loading.value = false
  }
}

watch(
  () => props.activityRunId,
  (v) => {
    if (v) {
      runId.value = v
      load()
    }
  }
)

const verdictType = (v?: string): any => {
  if (v === 'PASS') return 'success'
  if (v === 'FAIL') return 'danger'
  return 'warning'
}
const fmt = (t?: string) => (t ? String(t).replace('T', ' ').slice(0, 19) : '—')
const shortHash = (h?: string) => (h ? h.slice(0, 12) + '…' : '—')
</script>

<style scoped>
.bar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.mt-10px {
  margin-top: 10px;
}
.section-title {
  font-weight: 600;
  margin: 14px 0 8px;
}
.ml-8px {
  margin-left: 8px;
}
.ev-ref {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.ev-hash {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
  font-family: monospace;
}
</style>
