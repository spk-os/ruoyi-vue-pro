<!--
  SPK-OS IPD 指挥工作台（Cortext-IPD §7.5 / §10.9）
  三栏：行动队列（左）/ 阶段任务看板（右）/ 自然语言命令条（底，解析→预览→确认→执行幂等白名单）。
  行动队列统一来源：原生 BPM 待办 + spk-delivery 同步失败 + 阻断/失败 FlowRun。
  候选动作 APPROVE 跳决策包页，RETRY/UNBLOCK 走命令执行（intervene rerun）。
-->
<template>
  <div class="spk-ipd-workbench" data-test="workbench-page">
    <!-- 上下文头 -->
    <el-card class="mb-10px" shadow="never">
      <div class="flex flex-wrap items-center gap-16px">
        <el-select v-model="projectId" placeholder="选择项目" clearable filterable class="!w-220px"
          @change="reload">
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-tag v-if="nextGate" type="warning" effect="plain">下一门禁：{{ nextGate }}</el-tag>
        <template v-if="context">
          <el-tag v-if="context.projectName" type="info" effect="plain">
            {{ context.projectName }}
          </el-tag>
          <el-tag v-if="context.versionNo" type="info" effect="plain">
            版本 {{ context.versionNo }}
          </el-tag>
          <el-tag v-if="context.projectStatus" type="success" effect="plain">
            {{ context.projectStatus }}
          </el-tag>
        </template>
        <el-button :loading="loading" @click="reload" type="primary" plain size="small">
          <Icon icon="ep:refresh" class="mr-4px" />刷新
        </el-button>
        <span class="text-gray-400 text-12px ml-auto">更新于 {{ refreshedAt }}</span>
      </div>
    </el-card>

    <el-row :gutter="10">
      <!-- 行动队列 -->
      <el-col :span="8">
        <el-card shadow="never" class="inbox-card" data-test="action-inbox">
          <template #header>
            <div class="flex items-center justify-between">
              <span><Icon icon="ep:bell" class="mr-4px" />行动队列</span>
              <el-tag size="small" type="danger">{{ inbox.length }}</el-tag>
            </div>
          </template>
          <el-empty v-if="!inbox.length" description="无待办行动" :image-size="60" />
          <div v-for="(it, i) in inbox" :key="i" class="inbox-item">
            <div class="flex items-center gap-8px mb-4px">
              <el-tag :type="severityType(it.severity)" size="small" effect="dark">
                {{ it.severity }}
              </el-tag>
              <el-tag size="small" effect="plain">{{ typeLabel(it.type) }}</el-tag>
              <span class="text-13px font-600 flex-1 ellipsis">{{ it.title }}</span>
            </div>
            <div class="text-gray-500 text-12px mb-6px">{{ it.detail }}</div>
            <div class="flex gap-6px">
              <el-button v-if="it.action === 'APPROVE'" size="small" type="primary"
                @click="goApprove(it)">去审批</el-button>
              <el-button v-if="it.action === 'RETRY'" size="small" type="warning"
                @click="quickCommand('INTERVENE', it, 'rerun')">重试</el-button>
              <el-button v-if="it.action === 'UNBLOCK'" size="small" type="danger"
                @click="quickCommand('UNBLOCK', it, 'rerun')">解除阻断</el-button>
              <el-button size="small" text @click="copyRef(it)">复制引用</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 阶段任务看板 -->
      <el-col :span="16">
        <el-card shadow="never" class="board-card" data-test="stage-task-board">
          <template #header>
            <div class="flex items-center justify-between">
              <span><Icon icon="ep:grid" class="mr-4px" />阶段任务看板</span>
              <span class="text-gray-400 text-12px">{{ boardCount }} 项</span>
            </div>
          </template>
          <el-empty v-if="!board.length" description="无活跃流程" :image-size="60" />
          <div class="board-cols">
            <div v-for="col in board" :key="col.stage" class="board-col">
              <div class="board-col__head">
                {{ col.stage }}
                <el-tag size="small" round>{{ col.items.length }}</el-tag>
              </div>
              <div v-for="bi in col.items" :key="bi.id" class="board-item"
                :class="statusClass(bi.status)">
                <div class="text-13px font-600 mb-2px ellipsis">{{ bi.name }}</div>
                <div class="flex items-center justify-between text-11px text-gray-500">
                  <span>{{ bi.ownerName || '未分派' }}</span>
                  <el-tag :type="statusType(bi.status)" size="small" effect="plain">
                    {{ statusLabel(bi.status) }}
                  </el-tag>
                </div>
                <div v-if="bi.dueAt" class="text-11px text-gray-400">截止 {{ bi.dueAt }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 自然语言命令条 -->
    <el-card class="mt-10px" shadow="never">
      <template #header>
        <span><Icon icon="ep:magic-stick" class="mr-4px" />自然语言指挥</span>
        <span class="text-gray-400 text-12px ml-8px">
          关键词：重试/介入/解除阻断/审批/CCB/分派/启动 — 仅解析预览，确认后执行幂等白名单
        </span>
      </template>
      <div class="flex gap-8px">
        <el-input v-model="cmdText" placeholder="例如：对 activity a1b2 重试" clearable
          class="flex-1" @keyup.enter="doParse" />
        <el-button type="primary" :loading="parsing" @click="doParse" data-test="command-parse-btn">
          解析
        </el-button>
      </div>

      <!-- 解析预览 -->
      <div v-if="preview" class="mt-12px command-preview" data-test="command-preview">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="意图">
            <el-tag :type="intentType(preview.intent)">{{ preview.intent }}</el-tag>
            <el-tag v-if="preview.executable" type="success" size="small" class="ml-8px">可执行</el-tag>
            <el-tag v-else type="info" size="small" class="ml-8px">不可执行</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="preview.targets && preview.targets.length" label="对象">
            <el-tag v-for="(t, i) in preview.targets" :key="i" size="small" class="mr-4px">
              {{ JSON.stringify(t) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="preview.actions && preview.actions.length" label="动作">
            <div v-for="(a, i) in preview.actions" :key="i" class="text-13px">
              {{ i + 1 }}. {{ a.action }} - {{ a.description }}
            </div>
          </el-descriptions-item>
          <el-descriptions-item v-if="preview.ambiguities && preview.ambiguities.length" label="歧义">
            <div v-for="(a, i) in preview.ambiguities" :key="i" class="text-orange-500 text-12px">
              · {{ a }}
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="影响">{{ preview.impact }}</el-descriptions-item>
        </el-descriptions>
        <div class="mt-8px flex gap-8px">
          <el-input v-model="preview.activityRunId" v-if="needsRunId(preview.intent)"
            placeholder="补全 activityRunId（必填）" class="!w-320px" />
          <el-button type="success" :loading="executing" :disabled="!preview.executable"
            @click="doExecute" data-test="command-execute-btn">
            确认执行
          </el-button>
          <el-button @click="preview = null">取消</el-button>
        </div>
      </div>

      <!-- 执行结果 -->
      <el-alert v-if="execResult" class="mt-10px" :title="execResult.message"
        :type="resultType(execResult.status)" :closable="false" show-icon />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as WorkbenchApi from '@/api/spk/ipd/workbench'
import { getPage as getPageProjects } from '@/api/spk/ipd/business'
import type {
  WorkbenchRespVO,
  WorkbenchInboxItem,
  WorkbenchBoardColumn,
  CommandParseResp,
  CommandExecuteResp
} from '@/api/spk/ipd/workbench'
import { statusMap, labelText } from '@/views/spk/ipd/home/components/status'

defineOptions({ name: 'SpkIpdWorkbench' })

const router = useRouter()
const loading = ref(false)
const projectId = ref<number | undefined>(undefined)
const projects = ref<any[]>([])
const context = ref<Record<string, any> | null>(null)
const nextGate = ref<string | undefined>(undefined)
const refreshedAt = ref<string | undefined>(undefined)
const inbox = ref<WorkbenchInboxItem[]>([])
const board = ref<WorkbenchBoardColumn[]>([])

const boardCount = computed(() =>
  board.value.reduce((s, c) => s + (c.items?.length || 0), 0)
)

const reload = async () => {
  loading.value = true
  try {
    const data = await WorkbenchApi.getWorkbenchSnapshot(projectId.value, undefined)
    context.value = data.context || null
    nextGate.value = data.nextGate
    refreshedAt.value = data.refreshedAt
    inbox.value = data.inbox || []
    board.value = data.board || []
  } finally {
    loading.value = false
  }
}

const loadProjects = async () => {
  try {
    const res = await getPageProjects({ pageNo: 1, pageSize: 50 } as any)
    projects.value = (res?.list || []).map((p: any) => ({ id: p.id, name: p.name || p.projectCode }))
  } catch {
    projects.value = []
  }
}

onMounted(async () => {
  await loadProjects()
  await reload()
})

// ===== 行动项动作 =====
const goApprove = (it: WorkbenchInboxItem) => {
  router.push({
    path: '/spk/ipd-approval',
    query: { taskId: it.refId, flowRunId: it.flowRunId }
  })
}

const copyRef = (it: WorkbenchInboxItem) => {
  navigator.clipboard?.writeText(`${it.refType}:${it.refId}`)
  ElMessage.success('已复制引用')
}

// ===== 命令解析→执行 =====
const cmdText = ref('')
const parsing = ref(false)
const executing = ref(false)
const preview = ref<(CommandParseResp & { activityRunId?: string }) | null>(null)
const execResult = ref<CommandExecuteResp | null>(null)

const doParse = async () => {
  if (!cmdText.value.trim()) {
    ElMessage.warning('请输入命令')
    return
  }
  parsing.value = true
  execResult.value = null
  try {
    const resp = await WorkbenchApi.parseWorkbenchCommand({
      text: cmdText.value,
      projectId: projectId.value
    })
    preview.value = { ...resp, activityRunId: extractRunId(cmdText.value) }
  } finally {
    parsing.value = false
  }
}

const needsRunId = (intent?: string) =>
  ['RETRY', 'INTERVENE', 'UNBLOCK'].includes(intent || '')

const doExecute = async () => {
  if (!preview.value) return
  const intent = preview.value.intent
  let params: Record<string, any> = {}
  if (needsRunId(intent)) {
    const runId = (preview.value as any).activityRunId
    if (!runId) {
      ElMessage.warning('该意图需要 activityRunId')
      return
    }
    params.activityRunId = runId
    params.action = intent === 'UNBLOCK' ? 'rerun' : 'rerun'
  }
  try {
    await ElMessageBox.confirm(
      `确认执行意图 ${intent}？该操作幂等，可重复提交。`,
      '命令确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  executing.value = true
  try {
    const resp = await WorkbenchApi.executeWorkbenchCommand({
      idempotencyKey: preview.value.idempotencyKey!,
      intent,
      params
    })
    execResult.value = resp
    if (resp.status === 'SUCCESS') {
      ElMessage.success(resp.message)
      reload()
    }
  } finally {
    executing.value = false
  }
}

// 从命令文本中抽取 activityRunId（简单 a1b2 形式或长串）
const extractRunId = (text: string): string | undefined => {
  const m = text.match(/([a-zA-Z0-9_-]{6,})/)
  return m ? m[1] : undefined
}

// 快捷动作（行动队列按钮直接发起命令）
const quickCommand = async (intent: string, it: WorkbenchInboxItem, action: string) => {
  try {
    await ElMessageBox.confirm(`确认对 ${it.refId} 执行 ${action}？`, '快捷操作', { type: 'warning' })
  } catch {
    return
  }
  const key = `quick-${intent}-${it.refId}-${action}`
  const resp = await WorkbenchApi.executeWorkbenchCommand({
    idempotencyKey: key,
    intent,
    params: { activityRunId: it.refId, action, targetType: it.refType }
  })
  if (resp.status === 'SUCCESS') {
    ElMessage.success(resp.message)
    reload()
  } else {
    ElMessage.warning(resp.message)
  }
}

// ===== 展示辅助 =====
const typeLabel = (t: string) => {
  const m: Record<string, string> = {
    MY_TODO: '待办', AGENT_FAILURE: 'Agent失败', DCP_TR: 'DCP/TR',
    EVIDENCE_GAP: '证据缺口', SYNC_FAILURE: '同步失败', BLOCKED_FLOW: '阻断流程'
  }
  return m[t] || t
}
const severityType = (s?: string) => {
  if (!s) return 'info'
  const u = s.toUpperCase()
  if (['P0', 'CRITICAL'].includes(u)) return 'danger'
  if (['P1', 'WARN'].includes(u)) return 'warning'
  return 'info'
}
const statusClass = (s?: string) => {
  if (!s) return ''
  if (s.toLowerCase() === 'failed' || s.toLowerCase() === 'timeout') return 'is-failed'
  if (s.toLowerCase() === 'running') return 'is-running'
  if (s.toLowerCase() === 'queued') return 'is-queued'
  return ''
}
const statusType = (s?: string) => {
  if (!s) return 'info'
  const u = s.toLowerCase()
  if (u === 'failed' || u === 'timeout') return 'danger'
  if (u === 'running') return 'warning'
  if (u === 'queued') return 'info'
  return 'success'
}
const statusLabel = (s?: string) => labelText(statusMap, s)
const intentType = (i?: string) => (i === 'UNKNOWN' ? 'info' : 'warning')
const resultType = (s?: string) => {
  if (s === 'SUCCESS' || s === 'IDEMPOTENT') return 'success'
  if (s === 'NOT_SUPPORTED') return 'info'
  return 'error'
}
</script>

<style scoped lang="scss">
.spk-ipd-workbench {
  padding: 0 0 10px;
}
.inbox-card,
.board-card {
  :deep(.el-card__body) {
    max-height: 560px;
    overflow-y: auto;
  }
}
.inbox-item {
  padding: 10px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
  &:last-child {
    border-bottom: none;
  }
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.board-cols {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 6px;
}
.board-col {
  flex: 0 0 220px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
  padding: 8px;
  &__head {
    font-weight: 600;
    font-size: 13px;
    margin-bottom: 8px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}
.board-item {
  background: var(--el-bg-color);
  border-radius: 6px;
  padding: 8px;
  margin-bottom: 8px;
  border-left: 3px solid var(--el-color-success);
  &.is-failed {
    border-left-color: var(--el-color-danger);
  }
  &.is-running {
    border-left-color: var(--el-color-warning);
  }
  &.is-queued {
    border-left-color: var(--el-color-info);
  }
}
.command-preview {
  background: var(--el-fill-color-light);
  border-radius: 6px;
  padding: 10px;
}
</style>
