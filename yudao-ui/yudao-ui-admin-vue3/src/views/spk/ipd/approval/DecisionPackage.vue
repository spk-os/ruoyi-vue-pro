<!--
  SPK-OS IPD 决策包组件（设计文档 §7.9 / §10.8）
  一次返回业务摘要、差异、证据、风险、历史决策、候选动作。
  Go/No-Go/Redirect/RETURN 复用原生 BPM approve/reject/return（经 /spk/ipd/approval-tasks/{id}/decisions 包装）。
  必需产物/门禁/证据未齐套时 Go 禁用并逐项说明；管理员不可绕过强阻断（仅在有证据豁免覆盖时 forceOverride）。
-->
<template>
  <div class="spk-decision-package" v-loading="loading">
    <el-empty v-if="!loading && !pkg" description="决策包不可用（任务可能已处理）" />
    <template v-if="pkg">
      <!-- 页头摘要 -->
      <el-descriptions :column="4" border size="small" class="mb-12px">
        <el-descriptions-item label="项目">{{ pkg.header?.projectName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ pkg.header?.versionLabel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ pkg.header?.versionType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="流程类型">{{ flowTypeLabel(pkg.header?.flowType) }}</el-descriptions-item>
        <el-descriptions-item label="阶段">{{ pkg.header?.stage || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前门禁">{{ pkg.header?.currentGate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="健康">
          <el-tag :type="healthTagType(pkg.header?.health)" size="small">{{ healthLabel(pkg.header?.health) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="等待时长">{{ formatWait(pkg.header?.waitDurationMs) }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ pkg.header?.approverNickname || pkg.header?.approverUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划开始">{{ pkg.header?.plannedStartAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划结束">{{ pkg.header?.plannedEndAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="阻断数">
          <el-tag :type="(pkg.blockingItems?.length || 0) ? 'danger' : 'success'" size="small">
            {{ pkg.blockingItems?.length || 0 }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 强阻断项 -->
      <el-alert
        v-if="pkg.blockingItems?.length"
        type="error"
        :closable="false"
        class="mb-12px"
        title="必需产物/门禁/证据未齐套，禁止 Go"
      >
        <div v-for="(b, i) in pkg.blockingItems" :key="i">• {{ b }}</div>
      </el-alert>

      <!-- 决策摘要六区 -->
      <el-row :gutter="8" class="mb-12px">
        <el-col :span="8" v-for="s in pkg.summary" :key="s.title">
          <el-card shadow="never" class="summary-card">
            <template #header><b>{{ s.title }}</b></template>
            <el-table :data="s.rows" size="small" :show-header="false" :border="false">
              <el-table-column prop="label" width="90" />
              <el-table-column>
                <template #default="{ row }">
                  <el-tag v-if="row.type" :type="tagType(row.type)" size="small">{{ row.value }}</el-tag>
                  <span v-else>{{ row.value }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <!-- 必需产物 -->
      <el-card shadow="never" class="mb-12px">
        <template #header><b>必需产物</b>（状态/签名/扫描/hash/是否阻断）</template>
        <el-table :data="pkg.requiredArtifacts" size="small">
          <el-table-column prop="ref" label="引用" min-width="160" />
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="签名" width="70">
            <template #default="{ row }">{{ row.signed ? '✓' : '-' }}</template>
          </el-table-column>
          <el-table-column prop="scanStatus" label="扫描" width="100" />
          <el-table-column label="hash" width="70">
            <template #default="{ row }">{{ row.hashOk ? '✓' : '✗' }}</template>
          </el-table-column>
          <el-table-column label="阻断" width="70">
            <template #default="{ row }">
              <el-tag v-if="row.blocking" type="danger" size="small">是</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 证据链 / 产物 / 门禁 / CCB / DCP 折叠 -->
      <el-collapse class="mb-12px">
        <el-collapse-item :title="`证据链（${pkg.evidence?.length || 0}）${pkg.chainValid ? '✓ 校验通过' : '✗ 链断裂'}`" name="evidence">
          <el-table :data="pkg.evidence" size="small" max-height="240">
            <el-table-column prop="evidenceType" label="类型" width="120" />
            <el-table-column prop="refId" label="引用" min-width="160" />
            <el-table-column prop="occurredAt" label="发生时间" width="170" />
            <el-table-column label="hash">
              <template #default="{ row }">{{ (row.rowHash || '').slice(0, 12) }}</template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
        <el-collapse-item :title="`产物（${pkg.artifacts?.length || 0}）`" name="artifacts">
          <el-table :data="pkg.artifacts" size="small" max-height="240">
            <el-table-column prop="artifactType" label="类型" width="120" />
            <el-table-column prop="artifactId" label="编号" min-width="160" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column label="hash">
              <template #default="{ row }">{{ (row.contentHash || '').slice(0, 12) }}</template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
        <el-collapse-item :title="`门禁（${pkg.gates?.length || 0}）`" name="gates">
          <el-table :data="pkg.gates" size="small" max-height="200">
            <el-table-column prop="gate" label="门禁" width="120" />
            <el-table-column label="结论" width="80">
              <template #default="{ row }">
                <el-tag :type="row.pass ? 'success' : 'danger'" size="small">{{ row.pass ? 'PASS' : 'FAIL' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="nodeKey" label="节点" min-width="140" />
          </el-table>
        </el-collapse-item>
        <el-collapse-item :title="`CCB 变更（${pkg.ccb?.length || 0}） / DCP 重定向（${pkg.dcpRedirects?.length || 0}）`" name="ccb">
          <el-table :data="pkg.ccb" size="small" max-height="160">
            <el-table-column prop="changeId" label="变更" width="140" />
            <el-table-column prop="decision" label="决策" width="120" />
            <el-table-column prop="impact" label="影响" min-width="160" />
          </el-table>
        </el-collapse-item>
        <el-collapse-item :title="`历史决策（${pkg.decisions?.length || 0}）`" name="decisions">
          <el-table :data="pkg.decisions" size="small" max-height="200">
            <el-table-column prop="decision" label="决策" width="100" />
            <el-table-column prop="reason" label="理由" min-width="200" />
            <el-table-column prop="deciderUserId" label="决策人" width="100" />
            <el-table-column prop="createTime" label="时间" width="170" />
          </el-table>
        </el-collapse-item>
      </el-collapse>

      <!-- 候选动作栏（C-14 铁律：禁止定义函数但不渲染操作控件） -->
      <div class="action-bar">
        <div class="action-bar__hash">
          <span class="action-bar__label">决策包哈希</span>
          <span class="font-mono text-xs">{{ pkg.decisionPackageHash || '—' }}</span>
        </div>
        <div class="action-bar__btns">
          <el-button
            v-for="a in pkg.candidateActions || []"
            :key="a.decision"
            :type="actionBtnType(a.decision)"
            :disabled="a.enabled === false || loading"
            @click="decide(a.decision)"
          >
            {{ actionLabel(a.decision) }}
            <span v-if="a.enabled === false" class="action-bar__reason">（{{ a.reason || '当前不可用' }}）</span>
          </el-button>
        </div>
        <div v-if="!pkg.candidateActions?.length" class="action-bar__empty">
          无候选动作（任务可能已被处理或为只读归档，不可再决策）
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as ApprovalApi from '@/api/spk/ipd/approval'
import type { SpkIpdDecisionPackageRespVO } from '@/api/spk/ipd/approval'
import { flowTypeMap, healthMap, statusMap, labelText } from '@/views/spk/ipd/home/components/status'

defineOptions({ name: 'SpkIpdDecisionPackage' })

const props = defineProps<{ taskId: string }>()
const emit = defineEmits<{ (e: 'decided'): void }>()

const loading = ref(false)
const pkg = ref<SpkIpdDecisionPackageRespVO>()

const load = async () => {
  if (!props.taskId) return
  loading.value = true
  try {
    pkg.value = await ApprovalApi.getDecisionPackage(props.taskId)
  } catch (e) {
    pkg.value = undefined
  } finally {
    loading.value = false
  }
}

onMounted(load)

const healthTagType = (h?: string) => {
  if (h === 'GOOD') return 'success'
  if (h === 'WARN') return 'warning'
  if (h === 'CRITICAL') return 'danger'
  return 'info'
}
const tagType = (t?: string) => (t === 'success' ? 'success' : t === 'warning' ? 'warning' : t === 'danger' ? 'danger' : 'info')
const statusTagType = (s?: string) => (s === 'PASS' ? 'success' : s === 'WARN' ? 'warning' : s === 'FAIL' ? 'danger' : 'info')
const formatWait = (ms?: number) => {
  if (!ms || ms <= 0) return '-'
  const h = Math.floor(ms / 3600000)
  const m = Math.floor((ms % 3600000) / 60000)
  return h > 0 ? `${h}h${m}m` : `${m}m`
}
const flowTypeLabel = (s?: string) => labelText(flowTypeMap, s)
const healthLabel = (s?: string) => labelText(healthMap, s)
const statusLabel = (s?: string) => labelText(statusMap, s)

const decide = (decision: string) => {
  const action = pkg.value?.candidateActions?.find((a) => a.decision === decision)
  if (action && action.enabled === false) {
    ElMessage.warning(action.reason || `${decision} 当前不可用`)
    return
  }
  ElMessageBox.prompt('请输入决策理由', `${actionLabel(decision)} 决策`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputValidator: (v) => (v && v.trim().length > 0) || '理由不能为空'
  }).then(async ({ value }) => {
    let redirectTargetTaskKey: string | undefined
    if (decision === 'REDIRECT' || decision === 'RETURN') {
      const r = await ElMessageBox.prompt('目标节点 key', `${actionLabel(decision)} 目标`, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValidator: (v) => (v && v.trim().length > 0) || '目标节点不能为空'
      }).catch(() => null)
      if (!r) return
      redirectTargetTaskKey = r.value
    }
    loading.value = true
    try {
      await ApprovalApi.createDecision(props.taskId, {
        decision,
        reason: value,
        decisionPackageHash: pkg.value?.decisionPackageHash,
        redirectTargetTaskKey
      })
      ElMessage.success('决策已提交')
      emit('decided')
      await load()
    } catch (e: any) {
      // 显式处理并发冲突：任务已被他人处理时不再当作未知错误吞掉
      const code = e?.code ?? e?.data?.code
      if (code === 1050116080 /* IPD_TASK_ALREADY_COMPLETED */) {
        ElMessage.warning('该审批任务已被他人处理，已刷新决策包显示实际结果')
        await load()
      } else {
        ElMessage.error(e?.message || '决策提交失败')
      }
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

// 候选动作按钮样式与文案（GO/NO_GO/REDIRECT/RETURN）
const actionBtnType = (d?: string): any =>
  ({ GO: 'success', NO_GO: 'danger', REDIRECT: 'warning', RETURN: 'info' } as any)[d || ''] || 'primary'
const actionLabel = (d?: string): string =>
  ({ GO: 'Go', NO_GO: 'No-Go', REDIRECT: 'Redirect', RETURN: 'Return' } as any)[d || ''] || d || ''
</script>

<style scoped>
.spk-decision-package { padding: 4px; }
.summary-card { margin-bottom: 8px; }
.summary-card :deep(.el-card__body) { padding: 8px; }
.action-bar {
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 10px 12px;
  margin-top: 8px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
}
.action-bar__hash {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-right: auto;
}
.action-bar__label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.action-bar__btns {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.action-bar__reason {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.action-bar__empty {
  width: 100%;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
