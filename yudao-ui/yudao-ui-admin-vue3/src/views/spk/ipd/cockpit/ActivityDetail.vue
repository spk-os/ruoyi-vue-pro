<!--
  IPD Activity 详情：业务叙事 + 技术数据折叠。
  0 · 节点 hero（中文名/状态/provider/验证结论/哈希链，一眼看跑没跑通，不显 ACT-xx 编号）
  1 · 这个节点在做什么（contract.prompt 白话任务说明 + failureReason，MarkdownView 渲染）
  2 · 跑出什么结果（验证结论 PASS/FAIL + 要点）
  3 · 产物（每个 artifact 卡片：metadata md 渲染报告全文 + Gitea 文档链接 ↗）
  4 · Plane 需求（本流程录入 Plane 的 issue 清单，需求来源上下文）
  5 · Omnigent 实时会话（iframe 嵌入原生界面 + SSE 流；原始 session JSON 不展示）
  6 · 凭证可信吗（默认折叠：证据链 + 哈希链 + 原始数据 ID/哈希/路径）
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

      <!-- 2 · 跑出什么结果 -->
      <div class="section-title">跑出什么结果</div>

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

      <!-- 3 · 凭证可信吗（默认折叠：证据链 + 哈希链 + 原始数据） -->
      <el-collapse class="audit-collapse">
        <el-collapse-item title="凭证可信吗（证据链 · 哈希链）" name="audit">
          <div class="section-sub">
            证据链（{{ (data.evidenceChain || []).length }} 条） · 哈希链
            <el-tag size="small" :type="data.chainValid ? 'success' : 'danger'">
              {{ data.chainValid ? '完好' : '断裂' }}
            </el-tag>
          </div>
          <el-timeline>
            <el-timeline-item
              v-for="(ev, i) in data.evidenceChain || []"
              :key="i"
              :timestamp="fmt(ev.createTime)"
              placement="top"
            >
              <el-tag size="small" type="info">{{ evidenceTypeLabel(ev.evidenceType) }}</el-tag>
              <span class="ev-ref">ref：{{ ev.refId }}</span>
              <div class="ev-hash">prev：{{ shortHash(ev.prevHash) }} → row：{{ shortHash(ev.rowHash) }}</div>
            </el-timeline-item>
          </el-timeline>
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
</style>
