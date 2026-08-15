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
              <template #default="{ row }"><el-tag size="small">{{ row.status || '—' }}</el-tag></template>
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
              <template #default="{ row }"><el-tag size="small" :type="flowTag(row.status)">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column label="健康" prop="health" width="80" />
            <el-table-column label="产物" prop="artifactCount" width="60" align="center" />
            <el-table-column label="证据" prop="evidenceCount" width="60" align="center" />
            <el-table-column label="启动" prop="startedAt" width="150" :formatter="dateFormatter" />
            <el-table-column label="阻断原因" prop="blockReason" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default><el-button link type="primary" size="small" @click.stop="openCockpit">监控台</el-button></template>
            </el-table-column>
          </el-table>
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
import StatCard from '../overview/StatCard.vue'

defineOptions({ name: 'SpkIpdMonitor' })

const { push } = useRouter()
const loading = ref(true)
const activeTab = ref('decisions')
const projectId = ref<number | undefined>(undefined)
const data = ref<any>({})
const projects = ref<Array<{ id: number; name: string }>>([])

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
const openCockpit = () => push({ name: 'SpkIpdCockpit' })

const load = async () => {
  loading.value = true
  try {
    data.value = await IpdBusinessApi.getMonitor(projectId.value)
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
</style>
