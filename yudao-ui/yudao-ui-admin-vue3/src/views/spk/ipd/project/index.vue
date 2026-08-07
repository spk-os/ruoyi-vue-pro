<!--
  SPK-OS Cortext-IPD 项目 Cockpit（P4 统一入口）
  Tabs：总览(发起+六阶段进度+泳道) / Activity 详情(三件套+介入) / 集成 iframe / Gate 评审
  iframe 经后端代理注入鉴权，前端不接触 Omnigent/Plane/Gitea 凭证。
-->
<template>
  <div class="spk-ipd-project">
    <el-card class="top-bar" shadow="never">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="业务 Key">
          <el-input v-model="form.businessKey" placeholder="ipd-2026-0042" style="width: 220px" />
        </el-form-item>
        <el-form-item label="项目名">
          <el-input v-model="form.projectName" placeholder="智能家居中控" style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="starting" @click="onStart">发起 IPD</el-button>
        </el-form-item>
        <el-form-item label="流程实例">
          <el-input v-model="instanceId" placeholder="processInstanceId" style="width: 300px" @keyup.enter="loadProject" />
          <el-button class="ml-8px" @click="loadProject">载入</el-button>
          <el-switch v-model="autoRefresh" active-text="自动刷新(5s)" class="ml-8px" />
        </el-form-item>
      </el-form>
      <el-alert v-if="startMsg" :type="startOk ? 'success' : 'error'" :title="startMsg" :closable="false" show-icon class="mt-8px" />
    </el-card>

    <el-tabs v-model="activeTab" type="card" class="mt-10px">
      <!-- 总览 -->
      <el-tab-pane label="总览" name="overview">
        <div v-loading="loadingPhases">
          <div class="phase-row">
            <div v-for="p in phases" :key="p.phase" class="phase-cell">
              <div class="phase-head">
                <span>{{ phaseLabel(p.phase) }}</span>
                <el-tag size="small" :type="phaseTagType(p.status)">{{ p.status }}</el-tag>
              </div>
              <el-progress
                :percentage="p.total ? Math.round((p.done / p.total) * 100) : 0"
                :status="p.status === 'completed' ? 'success' : p.status === 'blocked' ? 'exception' : ''"
              />
              <div class="phase-meta">done {{ p.done }}/{{ p.total }} · running {{ p.running }} · failed {{ p.failed }}</div>
            </div>
          </div>
          <Swimlane v-if="instanceId" :external-pid="instanceId" @show-detail="onShowDetail" class="mt-10px" />
          <el-empty v-else description="载入流程实例后展示泳道图" />
        </div>
      </el-tab-pane>

      <!-- Activity 详情 + 介入 -->
      <el-tab-pane label="Activity 详情" name="detail">
        <ActivityDetail v-if="activeTab === 'detail'" :activity-run-id="detailRunId" />
        <div class="intervene-bar mt-10px">
          <el-input v-model="interveneForm.runId" placeholder="ActivityRunId" style="width: 280px" />
          <el-select v-model="interveneForm.action" style="width: 120px">
            <el-option label="重新派发" value="rerun" />
            <el-option label="标记失败" value="abort" />
            <el-option label="落反馈" value="note" />
          </el-select>
          <el-input v-model="interveneForm.note" placeholder="介入备注" style="width: 260px" />
          <el-button type="warning" @click="onIntervene">人工介入</el-button>
        </div>
      </el-tab-pane>

      <!-- 集成 iframe -->
      <el-tab-pane label="集成 iframe" name="iframe">
        <el-tabs v-model="iframeTab" type="border-card">
          <el-tab-pane label="Plane 需求" name="plane">
            <div class="iframe-bar">
              <el-input v-model="planeSid" placeholder="Plane 看板地址（workspace/project）" style="width: 380px" />
              <el-button @click="loadRequirements">拉需求 JSON</el-button>
              <el-link :href="planeUrl" target="_blank" type="primary" class="ml-8px">在新页打开 Plane</el-link>
            </div>
            <pre class="json-box">{{ reqJson || '点击「拉需求 JSON」加载（后端代理 Plane API）' }}</pre>
          </el-tab-pane>
          <el-tab-pane label="Gitea PR/CI" name="pr">
            <div class="iframe-bar">
              <el-button @click="loadGitPr">刷新 CI 状态</el-button>
              <el-link :href="gitPrUrl" target="_blank" type="primary" class="ml-8px">在新页打开 Gitea PR</el-link>
            </div>
            <pre class="json-box">{{ gitPrJson || '—' }}</pre>
          </el-tab-pane>
          <el-tab-pane label="Gitea Release" name="release">
            <div class="iframe-bar">
              <el-button @click="loadGitRelease">刷新 Release</el-button>
              <el-link :href="gitReleaseUrl" target="_blank" type="primary" class="ml-8px">在新页打开 Release</el-link>
            </div>
            <pre class="json-box">{{ gitReleaseJson || '—' }}</pre>
          </el-tab-pane>
          <el-tab-pane label="Omnigent session" name="omnigent">
            <div class="iframe-bar">
              <el-input v-model="omniSid" placeholder="Omnigent session id" style="width: 320px" />
              <el-button @click="loadOmniSession">载入 session</el-button>
              <el-button @click="toggleSse">{{ sseOpen ? '断开 SSE' : '订阅 SSE 流' }}</el-button>
            </div>
            <pre class="json-box">{{ omniJson || '—' }}</pre>
            <div class="sse-stream">
              <div v-for="ev in sseEvents" :key="ev.id" class="sse-line">{{ ev.name }}: {{ ev.text }}</div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-tab-pane>

      <!-- Gate 评审 -->
      <el-tab-pane label="Gate 评审" name="gate">
        <el-alert type="info" :closable="false" show-icon title="门禁 G1-G8 / TR2-TR6 由 BPM HTTP_CALLBACK 触发器发起；CI 完成后回调 /spk/gate/callback 推进。此处展示当前流程实例已落门禁记录与介入入口。" />
        <div class="intervene-bar mt-10px">
          <el-input v-model="interveneForm.runId" placeholder="Gate 关联 ActivityRunId" style="width: 280px" />
          <el-select v-model="interveneForm.action" style="width: 120px">
            <el-option label="重新派发" value="rerun" />
            <el-option label="标记失败" value="abort" />
            <el-option label="落反馈" value="note" />
          </el-select>
          <el-input v-model="interveneForm.note" placeholder="评审意见/介入备注" style="width: 320px" />
          <el-button type="warning" @click="onIntervene">评审介入</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import Swimlane from '../cockpit/Swimlane.vue'
import ActivityDetail from '../cockpit/ActivityDetail.vue'
import {
  startProject,
  getProject,
  getRequirements,
  getGitPr,
  getGitRelease,
  getOmnigentSession,
  interveneTask
} from '@/api/spk/ipd/project'

defineOptions({ name: 'SpkIpdProject' })

const activeTab = ref('overview')
const iframeTab = ref('plane')
const form = reactive({ businessKey: '', projectName: '' })
const starting = ref(false)
const startMsg = ref('')
const startOk = ref(false)
const instanceId = ref('')
const detailRunId = ref('')
const phases = ref<any[]>([])
const loadingPhases = ref(false)
const autoRefresh = ref(false)
let timer: any = null

// iframe 集成状态
const planeSid = ref('')
const reqJson = ref('')
const gitPrJson = ref('')
const gitReleaseJson = ref('')
const omniSid = ref('')
const omniJson = ref('')
const sseOpen = ref(false)
const sseEvents = ref<{ id: string; name: string; text: string }[]>([])
let evtSource: EventSource | null = null

const planeUrl = computed(() => '/spk/ipd/project/requirements')
const gitPrUrl = computed(() => '/spk/ipd/project/git-pr')
const gitReleaseUrl = computed(() => '/spk/ipd/project/git-release')

const PHASE_LABEL: Record<string, string> = {
  concept: '概念', plan: '计划', develop: '开发', qualify: '验证',
  launch: '发布', lifecycle: '生命周期', unknown: '未归类'
}
const phaseLabel = (p?: string) => PHASE_LABEL[p || ''] || p || '-'
const phaseTagType = (s?: string): any => {
  if (s === 'completed') return 'success'
  if (s === 'blocked') return 'danger'
  if (s === 'running') return 'warning'
  return 'info'
}

const onStart = async () => {
  starting.value = true
  startMsg.value = ''
  try {
    const data: any = await startProject({
      businessKey: form.businessKey || undefined,
      projectName: form.projectName || undefined
    })
    instanceId.value = data?.processInstanceId || ''
    startOk.value = true
    startMsg.value = `已发起 IPD 流程：businessKey=${data?.businessKey} processInstanceId=${data?.processInstanceId}`
    ElMessage.success('IPD 流程已发起')
    loadProject()
  } catch (e: any) {
    startOk.value = false
    startMsg.value = e?.message || '发起失败'
  } finally {
    starting.value = false
  }
}

const loadProject = async () => {
  if (!instanceId.value) return
  loadingPhases.value = true
  try {
    const data: any = await getProject(instanceId.value)
    phases.value = data?.phases || []
  } catch (e: any) {
    ElMessage.error(e?.message || '载入失败')
  } finally {
    loadingPhases.value = false
  }
}

const onShowDetail = (runId: string) => {
  detailRunId.value = runId
  activeTab.value = 'detail'
}

const interveneForm = reactive({ runId: '', action: 'note', note: '' })
const onIntervene = async () => {
  if (!interveneForm.runId) {
    ElMessage.warning('请填 ActivityRunId')
    return
  }
  try {
    await interveneTask(interveneForm.runId, interveneForm.action, interveneForm.note)
    ElMessage.success('介入已提交')
  } catch (e: any) {
    ElMessage.error(e?.message || '介入失败')
  }
}

const loadRequirements = async () => {
  try {
    reqJson.value = JSON.stringify(await getRequirements(50), null, 2)
  } catch (e: any) {
    reqJson.value = '加载失败：' + (e?.message || '')
  }
}
const loadGitPr = async () => {
  try {
    gitPrJson.value = JSON.stringify(await getGitPr(), null, 2)
  } catch (e: any) {
    gitPrJson.value = '加载失败：' + (e?.message || '')
  }
}
const loadGitRelease = async () => {
  try {
    gitReleaseJson.value = JSON.stringify(await getGitRelease(), null, 2)
  } catch (e: any) {
    gitReleaseJson.value = '加载失败：' + (e?.message || '')
  }
}
const loadOmniSession = async () => {
  if (!omniSid.value) {
    ElMessage.warning('请填 Omnigent session id')
    return
  }
  try {
    omniJson.value = JSON.stringify(await getOmnigentSession(omniSid.value), null, 2)
  } catch (e: any) {
    omniJson.value = '加载失败：' + (e?.message || '')
  }
}

const toggleSse = () => {
  if (sseOpen.value) {
    evtSource?.close()
    evtSource = null
    sseOpen.value = false
    return
  }
  if (!omniSid.value) {
    ElMessage.warning('请先填 Omnigent session id')
    return
  }
  // 后端 SSE 端点需带 token：复用 axios 同源 cookie/Authorization 由 EventSource 无法注入，
  // 故改用 fetch+ReadableStream 流式拉取（与 axios 共享鉴权头）
  // 简化：这里用 EventSource 直连后端代理（要求后端代理免 token 或走 cookie 鉴权）
  const url = `${import.meta.env.VITE_BASE_URL || ''}/admin-api/spk/ipd/omnigent-proxy/session/${omniSid.value}/stream`
  evtSource = new EventSource(url)
  sseOpen.value = true
  evtSource.addEventListener('assistant', (ev: any) => {
    sseEvents.value.push({ id: String(Date.now()), name: 'assistant', text: ev.data })
    if (sseEvents.value.length > 200) sseEvents.value.shift()
  })
  evtSource.addEventListener('tick', (ev: any) => {
    sseEvents.value.push({ id: String(Date.now()), name: 'tick', text: ev.data })
    if (sseEvents.value.length > 200) sseEvents.value.shift()
  })
  evtSource.onerror = () => {
    sseOpen.value = false
    evtSource?.close()
    evtSource = null
  }
}

onMounted(() => {
  const tick = () => {
    if (autoRefresh.value && instanceId.value) loadProject()
  }
  timer = setInterval(tick, 5000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
  evtSource?.close()
})
</script>

<style scoped>
.spk-ipd-project { padding: 12px; }
.mt-10px { margin-top: 10px; }
.mt-8px { margin-top: 8px; }
.ml-8px { margin-left: 8px; }
.phase-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.phase-cell {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 10px;
}
.phase-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-weight: 600;
}
.phase-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.intervene-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.iframe-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.json-box {
  background: var(--el-fill-color-light);
  border-radius: 4px;
  padding: 10px;
  max-height: 360px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
.sse-stream {
  margin-top: 8px;
  max-height: 240px;
  overflow: auto;
  font-size: 12px;
}
.sse-line {
  border-bottom: 1px dashed var(--el-border-color);
  padding: 2px 0;
}
</style>
