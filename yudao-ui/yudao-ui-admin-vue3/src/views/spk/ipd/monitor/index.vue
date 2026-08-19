<template>
  <div v-loading="loading">
    <!-- 顶部：项目选择器 + 汇总 -->
    <ContentWrap class="!mb-10px">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-8px">
          <Icon icon="ep:monitor" />
          <b>项目监控</b>
          <el-select
            v-model="projectId"
            class="!w-220px"
            filterable
            clearable
            placeholder="选择项目（空=全部）"
            @change="load"
          >
            <el-option :value="undefined" label="全部项目" />
            <el-option v-for="p in projects" :key="p.id" :value="p.id" :label="p.name" />
          </el-select>
          <el-button text @click="load"><Icon icon="ep:refresh" />刷新</el-button>
        </div>
        <div class="text-xs text-gray-400">整合 IPD 监控 + 监控台 · 决策审查/制品基线/全流程追溯</div>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-10px mt-10px">
        <StatCard v-for="c in summaryCards" :key="c.label" :label="c.label" :value="c.value" :icon="c.icon" :type="c.type" />
      </div>
    </ContentWrap>

    <ContentWrap>
      <el-tabs v-model="activeTab">
        <!-- 流程运行（§4.5 Tab2：并入 cockpit 泳道 + Agent 负载） -->
        <el-tab-pane label="流程运行" name="run">
          <el-row :gutter="12">
            <el-col :span="15">
              <div class="block-title"><Icon class="mr-4px" icon="ep:set-up" />流程泳道（阶段 → Activity → 状态/负责人/产物）</div>
              <el-empty v-if="!data.flows?.length" description="暂无流程运行" :image-size="60" />
              <div v-else class="run-panel">
                <el-select v-model="runPid" placeholder="选择流程查看泳道" size="small" class="!w-full mb-8px" @change="loadSwimlane">
                  <el-option v-for="f in data.flows" :key="f.id" :value="f.processInstanceId" :label="`${f.runNo || f.id} · ${f.flowType || ''} · ${f.currentStage || ''}`" />
                </el-select>
                <el-empty v-if="!swimlane?.length" description="选择上方流程查看阶段泳道（来自 cockpit 真实聚合）" :image-size="50" />
                <div v-else class="swimlane">
                  <div v-for="st in swimlane" :key="st.stage" class="swimlane__col">
                    <div class="swimlane__col-head">{{ st.stage }}<span class="swimlane__count">{{ (st.activities||[]).length }}</span></div>
                    <div v-for="a in (st.activities || [])" :key="a.activityRunId" class="swimlane__card" :class="'swimlane__card--' + (a.status||'idle')">
                      <div class="swimlane__card-name">{{ a.name || a.activityId || '—' }}</div>
                      <div class="swimlane__card-meta">
                        <el-tag size="small" effect="plain">{{ a.status || '—' }}</el-tag>
                        <span v-if="a.leadAgentName || a.leadAgentCode">{{ a.leadAgentName || a.leadAgentCode }}</span>
                      </div>
                      <div class="swimlane__card-prod">
                        <span v-if="a.artifactCount != null">产物 {{ a.artifactCount }}</span>
                        <span v-if="a.verificationConclusion">校验 {{ a.verificationConclusion }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="text-xs text-gray-400 mt-8px">点流程号进 <el-link type="primary" :underline="false" @click="openCockpit(selectedFlow)">监控台</el-link> 看完整泳道与 Activity 详情</div>
              </div>
            </el-col>
            <el-col :span="9">
              <div class="block-title">
                <Icon class="mr-4px" icon="ep:cpu" />Agent 负载（running 合同）
                <el-tag v-if="agentLoad?.deadLetterCount" size="small" type="danger" class="ml-8px">死信 {{ agentLoad.deadLetterCount }}</el-tag>
              </div>
              <el-empty v-if="!agentLoad?.leads?.length" description="Agent 负载样本不足" :image-size="50" />
              <div v-else class="agent-load">
                <div v-for="a in agentLoad.leads" :key="a.code" class="agent-load__row">
                  <span class="agent-load__name">{{ a.name || a.code }}</span>
                  <div class="agent-load__bar"><div class="agent-load__fill" :style="{ width: loadPct(a.runningContracts) + '%', background: loadPct(a.runningContracts) > 80 ? '#f56c6c' : loadPct(a.runningContracts) > 50 ? '#e6a23c' : '#67c23a' }" /></div>
                  <span class="agent-load__val">{{ a.runningContracts || 0 }}</span>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- 决策审查 -->
        <el-tab-pane :label="`决策审查${(data.decisions?.length||0) + (data.gates?.length||0) ? '' : ''}`" name="decisions">
          <el-row :gutter="12">
            <el-col :span="14">
              <div class="block-title"><Icon class="mr-4px" icon="ep:check" />决策记录</div>
              <el-empty v-if="!data.decisions?.length" description="决策样本不足：该项目暂无决策记录（跑流程经审批门后产生）" :image-size="60" />
              <el-table v-else :data="data.decisions" size="small">
                <el-table-column label="流程" prop="flowRunNo" width="150" />
                <el-table-column label="决策" prop="decision" width="100">
                  <template #default="{ row }"><el-tag size="small">{{ row.decision || '—' }}</el-tag></template>
                </el-table-column>
                <el-table-column label="审批包哈希" prop="decisionPackageHash" min-width="160" show-overflow-tooltip />
                <el-table-column label="原因" prop="reason" min-width="180" show-overflow-tooltip />
                <el-table-column label="审批人" prop="deciderUserId" width="90">
                  <template #default="{ row }">{{ row.deciderUserId ? `用户#${row.deciderUserId}` : '—' }}</template>
                </el-table-column>
              </el-table>
            </el-col>
            <el-col :span="10">
              <div class="block-title"><Icon class="mr-4px" icon="ep:lock" />门禁记录</div>
              <el-empty v-if="!data.gates?.length" description="门禁样本不足" :image-size="50" />
              <el-table v-else :data="data.gates" size="small">
                <el-table-column label="流程" prop="flowRunNo" width="130" />
                <el-table-column label="门禁" prop="gate" width="100" />
                <el-table-column label="结果" width="70">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.pass ? 'success' : 'danger'">{{ row.pass ? '通过' : '未过' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="报告" prop="report" min-width="140" show-overflow-tooltip />
              </el-table>
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- 制品基线 -->
        <el-tab-pane :label="`制品基线${data.artifacts?.length ? '(' + data.artifacts.length + ')' : ''}`" name="artifacts">
          <el-empty v-if="!data.artifacts?.length" description="制品样本不足：该项目暂无产物记录" :image-size="60" />
          <el-table v-else :data="data.artifacts" size="small">
            <el-table-column label="流程" prop="flowRunNo" width="140" />
            <el-table-column label="类型" prop="artifactType" width="120">
              <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.artifactType || '—' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="版本" prop="version" width="60" align="center" />
            <el-table-column label="内容哈希" prop="contentHash" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="font-mono text-xs">{{ row.contentHash || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="签名" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.signedBy" size="small" type="success">已签 {{ row.signedBy }}</el-tag>
                <el-tag v-else-if="row.signerRequired" size="small" type="warning">待签</el-tag>
                <el-tag v-else size="small" type="info">免签</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="100">
              <template #default="{ row }"><el-tag size="small">{{ statusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="大小" prop="bytes" width="90" align="right">
              <template #default="{ row }">{{ row.bytes ? Math.round(row.bytes / 1024) + 'KB' : '—' }}</template>
            </el-table-column>
            <el-table-column label="摘要" prop="summary" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>

        <!-- 全流程追溯 -->
        <el-tab-pane label="全流程追溯" name="trace">
          <el-empty v-if="!data.flows?.length" description="暂无流程运行" :image-size="60" />
          <el-table v-else :data="data.flows" size="small" @row-click="openCockpit">
            <el-table-column label="运行号" prop="runNo" width="150" />
            <el-table-column label="类型" prop="flowType" width="140" />
            <el-table-column label="阶段" prop="currentStage" width="90" />
            <el-table-column label="状态" prop="status" width="100">
              <template #default="{ row }"><el-tag size="small" :type="flowTag(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="健康" prop="health" width="80" />
            <el-table-column label="产物" prop="artifactCount" width="60" align="center" />
            <el-table-column label="证据" prop="evidenceCount" width="60" align="center" />
            <el-table-column label="启动" prop="startedAt" width="150" :formatter="dateFormatter" />
            <el-table-column label="阻断原因" prop="blockReason" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }"><el-button link type="primary" size="small" @click.stop="openCockpit(row)">监控台</el-button></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- AI 成本与会话（§4.5 Tab6：按模型/Activity/日趋势，真实 run_receipt 聚合） -->
        <el-tab-pane label="AI 成本与会话" name="costs">
          <el-empty v-if="!costs?.totals" description="成本数据未接入" :image-size="60" />
          <div v-else class="cost-panel">
            <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-10px mb-12px">
              <StatCard label="总会话数" :value="costs.totals.totalSessions" icon="ep:chat-dot-round" type="primary" />
              <StatCard label="成功次数" :value="costs.totals.successCount" icon="ep:circle-check" type="success" />
              <StatCard label="失败次数" :value="costs.totals.failedCount" icon="ep:circle-close" :type="(costs.totals.failedCount||0) > 0 ? 'danger' : ''" />
              <StatCard label="成功率" :value="costs.totals.successRate == null ? '—' : costs.totals.successRate + '%'" icon="ep:trend-charts" type="success" />
              <StatCard label="Token 总量" :value="costs.totals.totalTokens || 0" icon="ep:coin" type="" />
              <StatCard label="成本合计" :value="fmtCost(costs.totals.totalCost)" icon="ep:money" type="primary" />
            </div>
            <el-row :gutter="12">
              <el-col :span="14">
                <div class="block-title"><Icon class="mr-4px" icon="ep:histogram" />按模型聚合（会话/成功率/Token/成本）</div>
                <el-empty v-if="!costs.byModel?.length" description="暂无模型维度收据" :image-size="50" />
                <el-table v-else :data="costs.byModel" size="small">
                  <el-table-column label="模型" prop="model" min-width="140" show-overflow-tooltip />
                  <el-table-column label="会话" prop="sessions" width="70" align="center" />
                  <el-table-column label="成功" prop="successCount" width="60" align="center" />
                  <el-table-column label="失败" prop="failedCount" width="60" align="center">
                    <template #default="{ row }"><span :class="{ 'text-red-500': row.failedCount > 0 }">{{ row.failedCount }}</span></template>
                  </el-table-column>
                  <el-table-column label="成功率" width="80" align="center">
                    <template #default="{ row }">{{ row.successRate == null ? '—' : row.successRate + '%' }}</template>
                  </el-table-column>
                  <el-table-column label="Token" prop="tokens" width="100" align="right" />
                  <el-table-column label="成本" width="100" align="right">
                    <template #default="{ row }">{{ fmtCost(row.totalCost) }}</template>
                  </el-table-column>
                </el-table>
              </el-col>
              <el-col :span="10">
                <div class="block-title"><Icon class="mr-4px" icon="ep:list" />按 Activity 聚合</div>
                <el-empty v-if="!costs.byActivity?.length" description="暂无 Activity 维度收据" :image-size="50" />
                <el-table v-else :data="costs.byActivity" size="small" max-height="320">
                  <el-table-column label="Activity" prop="activity" min-width="100" show-overflow-tooltip />
                  <el-table-column label="会话" prop="sessions" width="70" align="center" />
                  <el-table-column label="Token" prop="tokens" width="90" align="right" />
                  <el-table-column label="成本" width="90" align="right">
                    <template #default="{ row }">{{ fmtCost(row.totalCost) }}</template>
                  </el-table-column>
                </el-table>
              </el-col>
            </el-row>
            <div class="block-title mt-12px"><Icon class="mr-4px" icon="ep:data-line" />日趋势（近 30 天会话/Token/成本）</div>
            <el-empty v-if="!costs.dailyTrend?.length" description="暂无趋势数据" :image-size="50" />
            <div v-else class="trend-bars">
              <div v-for="t in costs.dailyTrend" :key="t.date" class="trend-bars__col" :title="`${t.date} · 会话 ${t.sessions} · Token ${t.tokens} · 成本 ${fmtCost(t.cost)}`">
                <div class="trend-bars__bar" :style="{ height: trendHeight(t.sessions) + 'px' }" />
                <div class="trend-bars__lbl">{{ t.date.slice(5) }}</div>
              </div>
            </div>
            <div v-if="(costs.totals.totalCost || 0) === 0 && (costs.totals.totalTokens || 0) === 0" class="text-xs text-gray-400 mt-8px">
              注：会话数/模型/成功率/趋势为真实收据统计；成本与 Token 为 0 系 Lead 写收据时未回填 cost/tokenUsage（真实空值，非占位），待适配器接入用量回填后自动呈现。
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 集成健康（底部） -->
      <div class="block-title mt-12px"><Icon class="mr-4px" icon="ep:connection" />集成健康</div>
      <div class="grid grid-cols-3 gap-10px">
        <div v-for="i in data.integrations" :key="i.name" class="integration-card">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-sm font-medium">{{ i.name }}</div>
              <div class="text-xs text-gray-400">{{ i.detail }}</div>
            </div>
            <el-tag :type="i.healthy ? 'success' : 'danger'" size="small">
              <Icon class="mr-2px" :icon="i.healthy ? 'ep:circle-check' : 'ep:circle-close'" />{{ i.healthy ? '在线' : '离线' }}
            </el-tag>
          </div>
        </div>
        <el-empty v-if="!data.integrations?.length" description="无集成" :image-size="40" />
      </div>
    </ContentWrap>
  </div>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import { getSwimlane, getAgentLoad } from '@/api/spk/ipd/cockpit'
import StatCard from '../overview/StatCard.vue'
import { statusMap, labelText } from '@/views/spk/ipd/home/components/status'

defineOptions({ name: 'SpkIpdMonitor' })

const { push } = useRouter()
const loading = ref(true)
const activeTab = ref('run')
const projectId = ref<number | undefined>(undefined)
const data = ref<any>({})
const projects = ref<Array<{ id: number; name: string }>>([])
// 流程运行 Tab：真实泳道（cockpit getSwimlane）+ Agent 负载
const runPid = ref<string | undefined>(undefined)
const swimlane = ref<any[]>([])
const agentLoad = ref<any>({})
// AI 成本 Tab：真实 run_receipt 聚合
const costs = ref<any>(null)

const summaryCards = computed(() => {
  const s = data.value.summary || {}
  return [
    { label: '流程总数', value: s.flows || 0, icon: 'ep:list', type: '' },
    { label: '运行中', value: s.running || 0, icon: 'ep:loading', type: 'success' },
    { label: '阻断', value: s.blocked || 0, icon: 'ep:warning-filled', type: 'danger' },
    { label: '产物', value: s.artifacts || 0, icon: 'ep:folder', type: 'primary' },
    { label: '证据', value: s.evidence || 0, icon: 'ep:key', type: 'primary' },
    { label: '版本', value: s.versions || 0, icon: 'ep:copy-document', type: '' },
    { label: '问题', value: s.issues || 0, icon: 'ep:bell', type: 'warning' }
  ]
})

const flowTag = (s?: string) => ({ RUNNING: 'success', BLOCKED: 'danger', CANCELLED: 'info', COMPLETED: 'success', FAILED: 'danger' } as any)[s || ''] || ''
const statusLabel = (s?: string) => labelText(statusMap, s)
// C-14：监控行点击必须携带对象 ID（flowRunId + processInstanceId），不得丢上下文。
const openCockpit = (row: any) =>
  push({ name: 'SpkIpdCockpit', query: { flowRunId: row?.id, processInstanceId: row?.processInstanceId } })

// 流程运行 Tab：当前选中流程对象（供"进监控台"链接携带 ID）
const selectedFlow = computed(() => data.value.flows?.find((f: any) => f.processInstanceId === runPid.value) || null)

// 真实泳道：getSwimlane(processInstanceId) 返回 {stages: Map<stage, List<card>>}，归一化为数组渲染
const loadSwimlane = async () => {
  if (!runPid.value) { swimlane.value = []; return }
  try {
    const res = await getSwimlane(runPid.value)
    const stagesMap = (res && res.stages) || {}
    swimlane.value = Object.keys(stagesMap).map((stage) => ({ stage, activities: stagesMap[stage] || [] }))
  } catch { swimlane.value = [] }
}
// Agent 负载条百分比（按 runningContracts，无显式上限时以 5 为满载基准线性映射，上限 100%）
const loadPct = (n: number) => Math.min(100, Math.round(((n || 0) / 5) * 100))
// 成本格式化：BigDecimal 字符串 → 保留 4 位美元，0 显 0
const fmtCost = (v: any) => {
  if (v == null || v === '') return '—'
  const n = Number(v)
  if (!isFinite(n)) return String(v)
  return '$' + n.toFixed(4)
}
// 日趋势柱高（会话数映射，最大会话日满高 60px）
const trendHeight = (sessions: number) => {
  const max = Math.max(1, ...(costs.value?.dailyTrend || []).map((t: any) => t.sessions || 0))
  return Math.round(((sessions || 0) / max) * 60)
}

const load = async () => {
  loading.value = true
  try {
    data.value = await IpdBusinessApi.getMonitor(projectId.value)
    // 流程运行 / 成本 Tab 数据并行取（真实接口，失败降级空态）
    const [al, co] = await Promise.all([
      getAgentLoad().catch(() => ({})),
      IpdBusinessApi.getMonitorCosts(projectId.value).catch(() => null)
    ])
    agentLoad.value = al || {}
    costs.value = co
    // 默认选中首个流程泳道
    const first = data.value.flows?.find((f: any) => f.processInstanceId)
    if (first && !runPid.value) {
      runPid.value = first.processInstanceId
      loadSwimlane()
    }
  } finally {
    loading.value = false
  }
}
const loadProjects = async () => {
  try {
    const res = await IpdBusinessApi.getPage({ pageNo: 1, pageSize: 50 })
    projects.value = (res.list || []).map((p: any) => ({ id: p.id, name: p.name }))
  } catch {}
}
onMounted(async () => { await loadProjects(); await load() })
</script>

<style lang="scss" scoped>
.block-title {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
}
.integration-card {
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
}
/* 流程运行 Tab */
.run-panel { padding: 4px 0; }
.swimlane {
  display: flex;
  gap: 1px;
  background: var(--el-border-color-lighter);
  border-radius: 6px;
  overflow-x: auto;
}
.swimlane__col {
  background: #fff;
  flex: 1;
  min-width: 150px;
  padding: 8px;
}
.swimlane__col-head {
  font-size: 12px;
  font-weight: 600;
  color: #656d76;
  margin-bottom: 8px;
  display: flex;
  justify-content: space-between;
}
.swimlane__count {
  background: #f0f2f5;
  border-radius: 8px;
  padding: 0 6px;
  font-size: 11px;
}
.swimlane__card {
  background: #f6f8fa;
  border: 1px solid #eaeef2;
  border-left: 3px solid #d0d7de;
  border-radius: 5px;
  padding: 6px 8px;
  margin-bottom: 6px;
}
.swimlane__card--running, .swimlane__card--RUNNING { border-left-color: #67c23a; background: #f0f9eb; }
.swimlane__card--done, .swimlane__card--success, .swimlane__card--COMPLETED { border-left-color: #409eff; background: #ecf5ff; }
.swimlane__card--failed, .swimlane__card--BLOCKED { border-left-color: #f56c6c; background: #fef0f0; }
.swimlane__card-name { font-size: 13px; font-weight: 500; margin-bottom: 4px; }
.swimlane__card-meta { display: flex; align-items: center; gap: 6px; font-size: 11px; color: #656d76; }
.swimlane__card-prod { font-size: 11px; color: #909399; display: flex; gap: 8px; margin-top: 2px; }
.agent-load { display: flex; flex-direction: column; gap: 8px; }
.agent-load__row { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.agent-load__name { width: 110px; color: #303133; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.agent-load__bar { flex: 1; height: 12px; background: #f0f2f5; border-radius: 6px; overflow: hidden; }
.agent-load__fill { height: 100%; border-radius: 6px; transition: width .3s; }
.agent-load__val { width: 70px; text-align: right; color: #606266; font-family: ui-monospace, monospace; }
/* AI 成本 Tab */
.cost-panel { padding: 4px 0; }
.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 80px;
  padding: 8px 4px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.trend-bars__col { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; }
.trend-bars__bar { width: 60%; min-height: 2px; background: #409eff; border-radius: 3px 3px 0 0; }
.trend-bars__lbl { font-size: 9px; color: #909399; margin-top: 4px; transform: rotate(-45deg); transform-origin: center; white-space: nowrap; }
</style>
