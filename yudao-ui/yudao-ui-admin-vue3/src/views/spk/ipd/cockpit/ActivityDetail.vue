<!--
  IPD Activity 详情：以「任务信息 · 输入 · 输出 · 执行过程 · 凭证溯源」叙事，技术字段折叠。
  0 · 节点 hero（中文名/状态/provider/验证结论/哈希链，一眼看跑没跑通，不显 ACT-xx 编号）
  1 · 任务说明（contract.prompt 白话任务说明 + failureReason，MarkdownView 渲染）
  2 · 输入（上游依赖 inputRefs + 上下文清单 + 模型快照 + 执行配置 + 输出规格）
  3 · 输出（验证结论 PASS/FAIL + 要点；交付产物每个 artifact 卡片 metadata md 渲染报告全文 + Gitea 文档链接 ↗）
  4 · 执行过程（状态进度条 queued→running→done/failed + 证据链 timeline 业务事件流，payload 提摘要）
  5 · Plane 需求（本流程录入 Plane 的 issue 清单，需求来源上下文）
  6 · Omnigent 实时会话（iframe 嵌入原生界面 + SSE 流；原始 session JSON 不展示）
  7 · 凭证可信吗（默认折叠：三件套完整性 checklist + 哈希链 + 原始数据 ID/哈希/路径）
  文档链接由后端按 docs/{stage}/{activityRunId}.md 构造，不依赖 Flowable 历史变量。
  铁律 4：三件套缺失不得 completed —— 顶部告警。
-->
<template>
  <div class="activity-detail" v-loading="loading">
    <div v-if="!embedded" class="bar">
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
      <!-- 节点 hero：一眼看懂这是什么节点、什么状态 -->
      <div class="node-hero">
        <div class="hero-left">
          <div class="hero-activity">{{ data.activityName || data.contract?.activityId || data.activityRunId }}</div>
          <div class="hero-sub">
            {{ stageLabel(data.contract?.phase) }}阶段
            <span class="hero-lead">Lead：{{ data.contract?.leadAgentCode || '—' }}</span>
          </div>
        </div>
        <div class="hero-right">
          <el-tag :type="contractTagType" size="large">{{ statusLabel(data.contract?.status) }}</el-tag>
          <el-tag v-if="data.runReceipt?.provider" size="large" type="info">
            {{ data.runReceipt.provider }}
          </el-tag>
          <el-tag
            v-if="data.verifications?.length"
            :type="verdictType(data.verifications[0].overallConclusion)"
            size="large"
          >
            验证 {{ data.verifications[0].overallConclusion || '—' }}
          </el-tag>
          <el-tag
            v-if="data.chainValid !== undefined && data.chainValid !== null"
            :type="data.chainValid ? 'success' : 'danger'"
            size="large"
          >
            哈希链 {{ data.chainValid ? '完好' : '断裂' }}
          </el-tag>
        </div>
      </div>

      <el-alert
        v-if="!hasThreePiece"
        type="warning"
        title="三件套不齐全（ContextManifest / ArtifactManifest / RunReceipt 任一缺失）—— 铁律 4：不得标记 completed"
        :closable="false"
        show-icon
        class="mt-10px"
      />

      <!-- 1 · 这个节点在做什么 -->
      <div class="section-title">这个节点在做什么</div>
      <el-alert
        v-if="data.contract?.failureReason"
        type="error"
        :title="data.contract.failureReason"
        :closable="false"
        show-icon
        class="mb-10px"
      />
      <div v-if="data.contract?.prompt" class="prompt-box">
        <MarkdownView :content="data.contract.prompt" />
      </div>
      <el-empty v-else description="无任务说明" :image-size="60" />

      <!-- 2 · 输入：这个节点接收什么（上游依赖 + 上下文清单 + 模型快照 + 执行配置 + 输出规格） -->
      <div class="section-title">输入</div>
      <el-descriptions :column="2" border size="small" class="input-desc">
        <el-descriptions-item label="上游依赖" :span="2">
          <span v-if="inputRefsList.length" class="ref-tags">
            <el-tag
              v-for="(r, i) in inputRefsList"
              :key="i"
              size="small"
              type="info"
              class="ref-tag"
              @click="goToRef(r)"
            >
              {{ refLabel(r) }}
            </el-tag>
          </span>
          <span v-else class="muted">无上游引用（首节点 / 触发器未传入）</span>
        </el-descriptions-item>
        <el-descriptions-item label="输出规格">
          {{ artifactTypeLabel(data.contract?.outputSpec) }}
        </el-descriptions-item>
        <el-descriptions-item label="执行模式">
          {{ executionModeLabel(data.contract?.executionMode) }}
        </el-descriptions-item>
        <el-descriptions-item label="Worker / Verifier">
          {{ data.contract?.workerRequired ? '需 Worker' : '无 Worker' }}
          ·
          {{ data.contract?.verifierRequired ? '需 Verifier' : 'Lead 自签' }}
        </el-descriptions-item>
        <el-descriptions-item label="超时 / 重试">
          {{ data.contract?.timeoutSeconds || '—' }}s · {{ retryLabel(data.contract?.retryPolicy) }}
        </el-descriptions-item>
        <el-descriptions-item label="上下文清单" :span="2">
          <span class="mono">{{ data.contract?.contextManifestUri || '—' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="模型快照" :span="2">
          <span class="mono">{{ data.contract?.modelSnapshotId || '—' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 3 · 输出：跑出什么结果 -->
      <div class="section-title">输出 · 跑出什么结果</div>

      <!-- 验证结论 -->
      <div v-if="data.verifications?.length" class="verdict-card">
        <div class="verdict-head">
          <el-tag :type="verdictType(data.verifications[0].overallConclusion)" size="large">
            验证 {{ data.verifications[0].overallConclusion || '—' }}
          </el-tag>
          <span class="verdict-summary">{{ data.verifications[0].summary }}</span>
        </div>
        <div v-if="parsePoints(data.verifications[0].pointsJson).length" class="verdict-points">
          <div v-for="(p, i) in parsePoints(data.verifications[0].pointsJson)" :key="i" class="point-row">
            <el-tag size="small" :type="verdictType(p.verdict)">{{ p.verdict || '—' }}</el-tag>
            <span>{{ p.point }}</span>
          </div>
        </div>
      </div>
      <el-alert v-else type="info" title="未验证" :closable="false" show-icon class="mb-10px" />

      <!-- 交付产物：每个 artifact 一张卡片，metadata 直接 md 渲染报告全文 + Gitea 文档链接 -->
      <div class="section-title">产物</div>
      <div v-if="(data.artifacts || []).length" class="artifact-list">
        <div v-for="art in data.artifacts" :key="art.artifactId" class="artifact-card">
          <div class="artifact-head">
            <span class="artifact-type">{{ artifactTypeLabel(art.artifactType) }}</span>
            <span class="artifact-summary">{{ art.summary }}</span>
            <el-tag size="small" :type="art.status === 'signed' ? 'success' : art.status === 'quarantined' ? 'danger' : 'info'">
              {{ art.status }}
            </el-tag>
            <el-link
              v-if="art.giteaUrl"
              :href="art.giteaUrl"
              target="_blank"
              type="primary"
              :underline="false"
              class="doc-link"
            >
              查看文档 ↗
            </el-link>
            <span class="artifact-meta">v{{ art.version }} · {{ art.signedBy || '—' }} · {{ shortHash(art.contentHash) }}</span>
          </div>
          <div v-if="art.metadata" class="artifact-body">
            <MarkdownView :content="art.metadata" />
          </div>
        </div>
      </div>
      <el-empty v-else description="无交付产物" :image-size="60" />

      <!-- 4 · 执行过程：任务从入队到完成的事件流（状态进度条 + 证据链 timeline，payload 提业务摘要） -->
      <div class="section-title">
        执行过程
        <el-tag
          v-if="data.chainValid !== undefined && data.chainValid !== null"
          size="small"
          :type="data.chainValid ? 'success' : 'danger'"
          class="ml-8px"
        >
          哈希链 {{ data.chainValid ? '完好' : '断裂' }}
        </el-tag>
      </div>
      <div class="status-progress">
        <div class="step" :class="stepClass('queued')">
          <div class="step-dot"></div>
          <div class="step-label">入队</div>
          <div class="step-time">{{ fmt(data.contract?.queuedAt) }}</div>
        </div>
        <div class="step-line" :class="stepLineClass('running')"></div>
        <div class="step" :class="stepClass('running')">
          <div class="step-dot"></div>
          <div class="step-label">执行</div>
          <div class="step-time">{{ fmt(data.contract?.startedAt) }}</div>
        </div>
        <div class="step-line" :class="stepLineClass('done')"></div>
        <div class="step" :class="stepClass(data.contract?.status)">
          <div class="step-dot"></div>
          <div class="step-label">{{ statusLabel(data.contract?.status) }}</div>
          <div class="step-time">{{ fmt(data.contract?.finishedAt) }}</div>
        </div>
      </div>
      <el-timeline v-if="(data.evidenceChain || []).length" class="proc-timeline">
        <el-timeline-item
          v-for="(ev, i) in data.evidenceChain || []"
          :key="i"
          :timestamp="fmt(ev.createTime)"
          placement="top"
        >
          <el-tag size="small" type="info">{{ evidenceTypeLabel(ev.evidenceType) }}</el-tag>
          <span class="ev-ref">ref：{{ shortHash(ev.refId) }}</span>
          <div v-if="evidencePayloadSummary(ev.payload)" class="ev-summary">
            {{ evidencePayloadSummary(ev.payload) }}
          </div>
          <div class="ev-hash">prev：{{ shortHash(ev.prevHash) }} → row：{{ shortHash(ev.rowHash) }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="无执行过程记录" :image-size="60" />

      <!-- 7 · 凭证可信吗（默认折叠：三件套完整性 + 哈希链 + 原始数据） -->
      <el-collapse class="audit-collapse">
        <el-collapse-item title="凭证可信吗（三件套 · 哈希链）" name="audit">
          <div class="section-sub">
            三件套完整性
            <el-tag size="small" :type="hasThreePiece ? 'success' : 'danger'">
              {{ hasThreePiece ? '齐全' : '缺失' }}
            </el-tag>
          </div>
          <div class="checklist">
            <div class="check-row">
              <el-tag size="small" :type="data.contract?.contextManifestUri ? 'success' : 'danger'">
                {{ data.contract?.contextManifestUri ? '✓' : '✗' }}
              </el-tag>
              <span>ContextManifest 上下文清单</span>
              <span v-if="data.contract?.contextManifestUri" class="check-uri mono">
                {{ data.contract.contextManifestUri }}
              </span>
            </div>
            <div class="check-row">
              <el-tag
                size="small"
                :type="(data.artifacts || []).length ? 'success' : 'danger'"
              >
                {{ (data.artifacts || []).length ? '✓' : '✗' }}
              </el-tag>
              <span>ArtifactManifest 产物清单（{{ (data.artifacts || []).length }}）</span>
            </div>
            <div class="check-row">
              <el-tag size="small" :type="data.runReceipt ? 'success' : 'danger'">
                {{ data.runReceipt ? '✓' : '✗' }}
              </el-tag>
              <span>RunReceipt 执行回执</span>
              <span v-if="data.runReceipt?.runId" class="check-uri mono">
                {{ data.runReceipt.runId }}
              </span>
            </div>
          </div>
        </el-collapse-item>
        <el-collapse-item title="原始数据（ID / 哈希 / 路径）" name="raw">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="contractId">{{ data.contract?.contractId || '—' }}</el-descriptions-item>
            <el-descriptions-item label="modelSnapshotId">{{ data.contract?.modelSnapshotId || '—' }}</el-descriptions-item>
            <el-descriptions-item label="contextManifestUri">{{ data.contextManifestUri || '—' }}</el-descriptions-item>
            <el-descriptions-item label="nodeKey">{{ data.contract?.nodeKey || '—' }}</el-descriptions-item>
            <el-descriptions-item label="activityVersion">{{ data.contract?.activityVersion || '—' }}</el-descriptions-item>
            <el-descriptions-item label="leadAgentId">{{ data.contract?.leadAgentId || '—' }}</el-descriptions-item>
            <el-descriptions-item label="runReceiptId">{{ data.runReceipt?.runId || '—' }}</el-descriptions-item>
            <el-descriptions-item label="conversationId">{{ data.runReceipt?.conversationId || '—' }}</el-descriptions-item>
            <el-descriptions-item label="provider/model">{{ data.runReceipt?.provider || '—' }} / {{ data.runReceipt?.model || '—' }}</el-descriptions-item>
            <el-descriptions-item label="startedAt">{{ fmt(data.runReceipt?.startedAt) }}</el-descriptions-item>
            <el-descriptions-item label="finishedAt">{{ fmt(data.runReceipt?.finishedAt) }}</el-descriptions-item>
            <el-descriptions-item label="runStatus">{{ data.runReceipt?.status || '—' }}</el-descriptions-item>
          </el-descriptions>
          <div v-for="art in data.artifacts || []" :key="art.artifactId" class="raw-artifact">
            <div class="raw-artifact-head">[{{ art.artifactType }}] {{ art.artifactId }}</div>
            <div class="raw-artifact-uri">{{ art.uri }}</div>
          </div>
        </el-collapse-item>
      </el-collapse>

      <!-- Plane 需求：本流程在 Plane 录入的需求清单（IR/SR/AR），作为该节点任务的需求来源上下文 -->
      <div class="section-title">
        Plane 需求
        <el-button size="small" class="ml-8px" :loading="planeLoading" @click="loadPlaneIssues">刷新</el-button>
      </div>
      <div v-if="planeIssues.length" class="plane-list">
        <div v-for="iss in planeIssues" :key="iss.id" class="plane-item">
          <div class="plane-head">
            <el-tag size="small" type="info">{{ iss.sequence_id || '—' }}</el-tag>
            <span class="plane-name">{{ iss.name }}</span>
            <el-tag v-if="iss.state" size="small">{{ iss.state }}</el-tag>
          </div>
          <div v-if="iss.description" class="plane-desc">{{ stripHtml(iss.description) }}</div>
        </div>
      </div>
      <el-empty v-else description="未录入 Plane 需求（概念/计划阶段产出 IR/SR/AR 后录入）" :image-size="60" />

      <!-- Omnigent 会话视图：仅 omnigent provider 显示。
        主视图 iframe 嵌入 Omnigent 原生 session 详情页（路由 /c/{sid}），
        原始 session JSON 不再展示（按需求去除），保留 SSE 实时流。-->
      <template v-if="omniSid">
        <div class="section-title">
          Omnigent 实时会话
          <el-tag size="small" type="info" class="ml-8px">{{ omniSid }}</el-tag>
          <el-button size="small" :type="sseOpen ? 'danger' : 'primary'" @click="toggleSse">
            {{ sseOpen ? '断开 SSE' : '订阅会话流' }}
          </el-button>
          <el-link :href="omniFrameUrl" target="_blank" type="primary" class="ml-8px" :underline="false">
            新窗口打开 ↗
          </el-link>
        </div>
        <iframe
          v-if="omniFrameUrl"
          :src="omniFrameUrl"
          class="omni-frame"
          title="Omnigent session"
          referrerpolicy="no-referrer"
        />
        <el-alert
          v-else
          type="info"
          :closable="false"
          show-icon
          title="未配置 Omnigent base-url，无法嵌入原生界面（VITE_OMNIGENT_BASE_URL）"
        />
        <div v-if="sseEvents.length" class="sse-stream">
          <div v-for="ev in sseEvents" :key="ev.id" class="sse-line">
            <el-tag size="small" :type="ev.name === 'assistant' ? 'success' : 'info'">{{ ev.name }}</el-tag>
            <span class="sse-text">{{ ev.text }}</span>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { getActivityDetail } from '@/api/spk/ipd/cockpit'
import { getRequirements } from '@/api/spk/ipd/project'

const props = defineProps<{ activityRunId?: string; embedded?: boolean }>()

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

// 抽屉模式（embedded）：props.activityRunId 变化即加载；immediate 处理首次挂载。
watch(
  () => props.activityRunId,
  (v) => {
    if (v) {
      runId.value = v
      load()
    }
  },
  { immediate: true }
)

const verdictType = (v?: string): any => {
  if (v === 'PASS') return 'success'
  if (v === 'FAIL') return 'danger'
  return 'warning'
}
const statusLabel = (s?: string) => {
  const m: Record<string, string> = {
    queued: '已入队', running: '执行中', done: '完成', failed: '失败',
    timeout: '超时', cancelled: '已取消'
  }
  return m[s || ''] || s || '-'
}
const STAGE_LABEL: Record<string, string> = {
  concept: '概念', plan: '计划', develop: '开发', qualify: '验证',
  launch: '发布', lifecycle: '生命周期', support: '支撑', unknown: '未归类'
}
const stageLabel = (p?: string) => STAGE_LABEL[p || ''] || p || '—'
const fmt = (t?: string) => (t ? String(t).replace('T', ' ').slice(0, 19) : '—')
const shortHash = (h?: string) => (h ? h.slice(0, 12) + '…' : '—')

// 产物类型中文映射（未知类型回落原文）
const ARTIFACT_TYPE_LABEL: Record<string, string> = {
  'req-insight-report': '需求洞察报告',
  'concept-options-set': '概念选项集',
  'feasibility-report': '可行性分析报告',
  'business-case': '商业案例',
  'concept-decision-brief': '概念决策简报',
  'prs-baseline': '需求基线',
  'arch-baseline': '架构基线',
  'test-strategy': '测试策略'
}
const artifactTypeLabel = (t?: string) => ARTIFACT_TYPE_LABEL[t || ''] || t || '—'

// 证据类型中文映射
const EVIDENCE_TYPE_LABEL: Record<string, string> = {
  context_manifest: '上下文清单',
  artifact_signed: '产物签署',
  run_receipt: '执行回执',
  verification: '验证回执',
  gate_decision: '门禁决策'
}
const evidenceTypeLabel = (t?: string) => EVIDENCE_TYPE_LABEL[t || ''] || t || '—'

// 解析验证要点 JSON（失败降级空数组，不炸组件）
const parsePoints = (json?: string): { point?: string; verdict?: string }[] => {
  if (!json) return []
  try {
    const parsed = JSON.parse(json)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

// 输入区：解析 inputRefs JSON 字符串数组（route 调用方传入的上游产物引用）
const inputRefsList = computed<string[]>(() => {
  const raw = data.value?.contract?.inputRefs
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.map((x: any) => String(x)) : []
  } catch {
    return []
  }
})
const refLabel = (r: string) => {
  if (!r) return '—'
  // inputRefs 通常为上游 activityRunId（run-xxx），取前 16 位作短标签避免撑爆布局
  return r.length > 18 ? r.slice(0, 16) + '…' : r
}
const goToRef = (r: string) => {
  // 点击上游引用：若是 run-xxx 形式，就地加载该 ActivityRun 详情
  if (r && r.startsWith('run-')) {
    runId.value = r
    load()
  }
}
const executionModeLabel = (m?: string) => {
  const map: Record<string, string> = {
    task_system: '任务系统派发',
    lead_internal: 'Lead 内部执行',
    omnigent: 'Omnigent 执行',
    native_ai: '本地 AI 执行'
  }
  return map[m || ''] || m || '—'
}
const retryLabel = (p?: string) => {
  if (!p || p === '{}' || p === 'null') return '默认策略'
  try {
    const o = JSON.parse(p)
    const max = o.maxAttempts || o.max_attempts || o.retry
    return max ? `重试 ${max} 次` : '默认策略'
  } catch {
    return '默认策略'
  }
}

// 执行过程：状态进度条着色（queued → running → done/failed/timeout/cancelled）
const STEP_ORDER: Record<string, number> = {
  queued: 0, running: 1, done: 2, failed: 2, timeout: 2, cancelled: 2
}
const stepClass = (step?: string) => {
  const s = data.value?.contract?.status
  const target = STEP_ORDER[step || '']
  const cur = STEP_ORDER[s || '']
  if (target === undefined) return ''
  if (step === s) return 'active'
  return cur > target ? 'done' : ''
}
const stepLineClass = (step?: string) => {
  const s = data.value?.contract?.status
  const target = STEP_ORDER[step || '']
  const cur = STEP_ORDER[s || '']
  if (target === undefined) return ''
  return cur > target ? 'done' : ''
}

// 证据 payload 业务摘要：从 evidenceService.append 的 payload JSON 提取
// summary/conclusion/verdict/lead/status 等业务字段（不显引擎指标）
const evidencePayloadSummary = (payload?: string): string => {
  if (!payload) return ''
  try {
    const o = JSON.parse(payload)
    const parts: string[] = []
    if (o.summary) parts.push(`摘要：${o.summary}`)
    if (o.conclusion) parts.push(`结论：${o.conclusion}`)
    if (o.verificationConclusion) parts.push(`验证：${o.verificationConclusion}`)
    if (o.lead || o.leadAgentCode) parts.push(`Lead：${o.lead || o.leadAgentCode}`)
    if (o.error || o.reason) parts.push(`原因：${o.error || o.reason}`)
    if (!parts.length && o.status) parts.push(`状态：${o.status}`)
    return parts.join(' · ')
  } catch {
    return ''
  }
}

// ⑥ Omnigent 会话视图：仅当 provider=omnigent 且 contract.taskId 存在时启用。
// iframe 嵌入 Omnigent 原生 session 详情页（SPA 路由 /c/{sid}），JSON 折叠保留。
const OMNI_BASE_URL =
  import.meta.env.VITE_OMNIGENT_BASE_URL || 'http://192.168.56.101:6767'
const omniSid = computed(() => {
  if (!data.value) return ''
  const provider = data.value.runReceipt?.provider
  const sid = data.value.contract?.taskId
  if (provider === 'omnigent' && sid && !sid.startsWith('omnigent-fast')) {
    return sid
  }
  return ''
})
const omniFrameUrl = computed(() => {
  if (!omniSid.value) return ''
  return `${OMNI_BASE_URL}/c/${omniSid.value}`
})
const sseOpen = ref(false)
const sseEvents = ref<{ id: string; name: string; text: string }[]>([])
let evtSource: EventSource | null = null

// Plane 需求：本流程录入 Plane 的 issue 清单（listIssues 返回 JSON 字符串，前端解析）
const planeIssues = ref<any[]>([])
const planeLoading = ref(false)
const loadPlaneIssues = async () => {
  planeLoading.value = true
  try {
    const raw = await getRequirements(50)
    // getRequirements 返回 Plane issues JSON 字符串或数组；兼容两种
    const list = typeof raw === 'string' ? JSON.parse(raw || '[]') : (Array.isArray(raw) ? raw : [])
    planeIssues.value = Array.isArray(list) ? list : []
  } catch (e: any) {
    planeIssues.value = []
  } finally {
    planeLoading.value = false
  }
}
// Plane issue 的 description 是富文本 HTML，剥成纯文本预览
const stripHtml = (s?: string) => (s ? s.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim().slice(0, 200) : '')

const toggleSse = () => {
  if (sseOpen.value) {
    evtSource?.close()
    evtSource = null
    sseOpen.value = false
    return
  }
  if (!omniSid.value) return
  const url = `${import.meta.env.VITE_BASE_URL || ''}/admin-api/spk/ipd/omnigent-proxy/session/${omniSid.value}/stream`
  evtSource = new EventSource(url)
  sseOpen.value = true
  evtSource.addEventListener('assistant', (ev: any) => {
    sseEvents.value.push({ id: String(ev.lastEventId || Date.now()), name: 'assistant', text: ev.data })
    if (sseEvents.value.length > 200) sseEvents.value.shift()
  })
  evtSource.addEventListener('tick', (ev: any) => {
    sseEvents.value.push({ id: String(ev.lastEventId || Date.now()), name: 'tick', text: ev.data })
    if (sseEvents.value.length > 200) sseEvents.value.shift()
  })
  evtSource.onerror = () => {
    sseOpen.value = false
    evtSource?.close()
    evtSource = null
  }
}
// 首次加载数据后拉一次 Plane 需求
watch(
  () => data.value,
  (v) => {
    if (v) loadPlaneIssues()
  }
)
onUnmounted(() => {
  evtSource?.close()
})
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
.node-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  border-left: 4px solid var(--el-color-primary);
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.hero-activity {
  font-size: 18px;
  font-weight: 700;
}
.hero-sub {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.hero-lead {
  margin-left: 12px;
}
.hero-right {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
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
.json-box {
  background: var(--el-fill-color-light);
  border-radius: 4px;
  padding: 10px;
  max-height: 320px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
.omni-frame {
  width: 100%;
  height: 600px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: var(--el-fill-color-light);
}
.sse-stream {
  margin-top: 8px;
  max-height: 240px;
  overflow: auto;
  font-size: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 6px;
}
.sse-line {
  border-bottom: 1px dashed var(--el-border-color);
  padding: 3px 0;
  display: flex;
  align-items: flex-start;
  gap: 6px;
}
.sse-text {
  white-space: pre-wrap;
  word-break: break-all;
}
.mb-10px {
  margin-bottom: 10px;
}
.prompt-box {
  max-height: 200px;
  overflow: auto;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  border-left: 3px solid var(--el-color-primary);
  margin-bottom: 10px;
  font-size: 14px;
}
.verdict-card {
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  margin-bottom: 10px;
}
.verdict-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.verdict-summary {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.verdict-points {
  margin-top: 8px;
}
.point-row {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 13px;
  margin-bottom: 4px;
}
.artifact-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}
.artifact-card {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 10px 12px;
}
.artifact-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px dashed var(--el-border-color);
}
.artifact-type {
  font-weight: 600;
}
.artifact-summary {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.artifact-meta {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-family: monospace;
}
.artifact-body {
  max-height: 440px;
  overflow: auto;
  font-size: 14px;
  line-height: 1.6;
}
.artifact-body :deep(img) {
  max-width: 100%;
}
.audit-collapse {
  margin-top: 10px;
}
.doc-link {
  margin-left: auto;
  margin-right: 8px;
}
.plane-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}
.plane-item {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 8px 12px;
}
.plane-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.plane-name {
  font-weight: 600;
  font-size: 14px;
}
.plane-desc {
  margin-top: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.section-sub {
  font-weight: 600;
  margin: 8px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}
.raw-artifact {
  margin-top: 6px;
  font-size: 12px;
  font-family: monospace;
}
.raw-artifact-head {
  color: var(--el-text-color-secondary);
}
.raw-artifact-uri {
  color: var(--el-text-color-secondary);
  word-break: break-all;
}
/* 输入区 */
.input-desc {
  margin-bottom: 10px;
}
.ref-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.ref-tag {
  cursor: pointer;
}
.muted {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.mono {
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
}
/* 执行过程 · 状态进度条 */
.status-progress {
  display: flex;
  align-items: flex-start;
  margin: 10px 0 14px;
  padding: 14px 16px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}
.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 84px;
}
.step-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--el-border-color);
  border: 2px solid var(--el-border-color);
}
.step.active .step-dot {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 4px var(--el-color-primary-light-8);
}
.step.done .step-dot {
  background: var(--el-color-success);
  border-color: var(--el-color-success);
}
.step-label {
  font-size: 13px;
  font-weight: 600;
}
.step-time {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  font-family: monospace;
}
.step-line {
  flex: 1;
  height: 2px;
  background: var(--el-border-color);
  margin: 6px 4px 0;
}
.step-line.done {
  background: var(--el-color-success);
}
.proc-timeline {
  margin-bottom: 10px;
}
.ev-summary {
  font-size: 13px;
  margin: 4px 0;
  padding: 6px 8px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  border-left: 3px solid var(--el-color-primary);
  line-height: 1.5;
}
/* 三件套 checklist */
.checklist {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 8px 0;
}
.check-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  flex-wrap: wrap;
}
.check-uri {
  color: var(--el-text-color-secondary);
  word-break: break-all;
}
</style>
