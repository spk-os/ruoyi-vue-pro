<template>
  <div v-loading="loading" class="spk-ipd-overview">
    <!-- 顶部统计卡 -->
    <ContentWrap class="!mb-10px">
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-10px">
        <StatCard v-for="c in topStats" :key="c.label" :label="c.label" :value="c.value" :icon="c.icon" :type="c.type" />
      </div>
    </ContentWrap>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-10px">
      <!-- 左：待办 + 最近流程 -->
      <ContentWrap class="lg:col-span-2 !mb-0" :bodyStyle="{ padding: '12px' }">
        <div class="flex justify-between items-center mb-10px">
          <b><Icon class="mr-4px" icon="ep:warning" />待办与告警</b>
          <el-tag v-if="data.attentionItems?.length" type="danger" size="small">{{ data.attentionItems.length }} 项</el-tag>
          <el-tag v-else type="success" size="small">无告警</el-tag>
        </div>
        <el-empty v-if="!data.attentionItems?.length" description="暂无待办，所有流程顺畅" :image-size="60" />
        <el-timeline v-else>
          <el-timeline-item v-for="(a, i) in data.attentionItems" :key="i" :type="attType(a.severity)" :timestamp="a.type" placement="top">
            <div class="flex items-center justify-between">
              <span class="text-sm">{{ a.title }}</span>
              <div>
                <el-tag size="small" :type="attType(a.severity)">{{ a.severity }}</el-tag>
                <el-button v-if="a.refId" link type="primary" size="small" class="ml-8px" @click="jumpAttention(a)">处理</el-button>
              </div>
            </div>
            <div class="text-xs text-gray-400 mt-2px">{{ a.detail }}</div>
          </el-timeline-item>
        </el-timeline>

        <el-divider content-position="left"><b>最近流程</b></el-divider>
        <el-table :data="data.recentFlows" size="small" @row-click="jumpFlow">
          <el-table-column label="运行号" prop="runNo" width="140" />
          <el-table-column label="项目" prop="projectName" min-width="140" show-overflow-tooltip />
          <el-table-column label="类型" prop="flowType" width="130" />
          <el-table-column label="当前阶段" prop="currentStage" width="100" />
          <el-table-column label="状态" prop="status" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="flowTagType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="健康" prop="health" width="80" />
          <el-table-column label="启动时间" prop="startedAt" width="150" :formatter="dateFormatter" />
        </el-table>
      </ContentWrap>

      <!-- 右：分布统计 -->
      <ContentWrap class="!mb-0" :bodyStyle="{ padding: '12px' }">
        <b><Icon class="mr-4px" icon="ep:data-analysis" />分布统计</b>
        <el-tabs v-model="distTab" class="mt-8px">
          <el-tab-pane label="项目" name="project">
            <DistRow v-for="(v, k) in data.projectCounts" :key="k" :label="k" :value="v" :total="totalOf(data.projectCounts)" />
          </el-tab-pane>
          <el-tab-pane label="版本" name="version">
            <DistRow v-for="(v, k) in data.versionCounts" :key="k" :label="k" :value="v" :total="totalOf(data.versionCounts)" />
          </el-tab-pane>
          <el-tab-pane label="流程" name="flow">
            <DistRow v-for="(v, k) in data.flowRunCounts" :key="k" :label="k" :value="v" :total="totalOf(data.flowRunCounts)" />
          </el-tab-pane>
          <el-tab-pane label="问题" name="issue">
            <DistRow v-for="(v, k) in data.issueSeverityCounts" :key="k" :label="k" :value="v" :total="totalOf(data.issueSeverityCounts)" />
          </el-tab-pane>
        </el-tabs>

        <el-divider content-position="left"><b>AI 资源</b></el-divider>
        <div class="text-sm space-y-6px">
          <div v-for="(v, k) in data.aiUsage" :key="k" class="flex justify-between">
            <span class="text-gray-500">{{ aiLabel(k) }}</span>
            <b>{{ v }}</b>
          </div>
          <el-empty v-if="!data.aiUsage || !Object.keys(data.aiUsage).length" description="暂无 AI 指标" :image-size="40" />
        </div>
      </ContentWrap>
    </div>

    <!-- 路线图 -->
    <ContentWrap class="mt-10px">
      <div class="flex justify-between items-center mb-10px">
        <b><Icon class="mr-4px" icon="ep:guide" />活跃项目路线图</b>
        <el-button type="primary" size="small" @click="goProjects">管理项目与版本</el-button>
      </div>
      <el-empty v-if="!data.roadmap?.length" description="暂无活跃项目，先去创建" :image-size="60" />
      <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-12px">
        <el-card v-for="p in data.roadmap" :key="p.projectId" shadow="hover" class="cursor-pointer" @click="openProject(p.projectId)">
          <div class="flex justify-between items-center">
            <b class="truncate">{{ p.projectName }}</b>
            <el-tag size="small" :type="healthTagType(p.health)">{{ p.health || '—' }}</el-tag>
          </div>
          <div v-for="m in p.majors" :key="m.majorReleaseId" class="mt-8px">
            <div class="text-xs text-gray-400">V{{ m.majorNo }} · {{ m.name }} <el-tag size="small" effect="plain">{{ m.status }}</el-tag></div>
            <div class="flex flex-wrap gap-4px mt-4px">
              <el-tag v-for="v in m.versions" :key="v.versionId" size="small" :type="versionTagType(v.status)" @click.stop="openProject(p.projectId)">
                {{ v.versionNo }}
              </el-tag>
              <span v-if="!m.versions?.length" class="text-xs text-gray-300">无版本</span>
            </div>
          </div>
        </el-card>
      </div>
    </ContentWrap>
  </div>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import StatCard from './StatCard.vue'
import DistRow from './DistRow.vue'

defineOptions({ name: 'SpkIpdOverview' })

const message = useMessage()
const { push } = useRouter()
const loading = ref(true)
const data = ref<IpdBusinessApi.SpkIpdOverviewVO>({})
const distTab = ref('project')

const topStats = computed(() => {
  const pc = data.value.projectCounts || {}
  const activeProjects = (pc.ACTIVE || 0)
  const totalProjects = totalOf(pc)
  return [
    { label: '活跃项目', value: activeProjects, icon: 'ep:folder-opened', type: 'primary' },
    { label: '项目总数', value: totalProjects, icon: 'ep:files', type: '' },
    { label: '运行中流程', value: data.value.activeFlowCount || 0, icon: 'ep:loading', type: 'success' },
    { label: '阻断流程', value: data.value.blockedFlowCount || 0, icon: 'ep:warning-filled', type: 'danger' },
    { label: '待审问题', value: data.value.openIssueCount || 0, icon: 'ep:bell', type: 'warning' },
    { label: '已发布版本', value: (data.value.versionCounts || {}).RELEASED || 0, icon: 'ep:promotion', type: 'success' }
  ]
})

const totalOf = (m?: Record<string, number>) => m ? Object.values(m).reduce((a, b) => a + b, 0) : 0
const attType = (s?: string) => (s === 'P0' || s === 'CRITICAL' ? 'danger' : s === 'P1' || s === 'WARN' ? 'warning' : 'info')
const flowTagType = (s?: string) => ({ RUNNING: 'success', BLOCKED: 'danger', CANCELLED: 'info', COMPLETED: 'success', FAILED: 'danger' } as any)[s || ''] || ''
const healthTagType = (s?: string) => ({ GOOD: 'success', WARN: 'warning', CRITICAL: 'danger' } as any)[s || ''] || 'info'
const versionTagType = (s?: string) => ({ RELEASED: 'success', RUNNING: 'primary', VERIFYING: 'warning', READY: 'primary', CANCELLED: 'info' } as any)[s || ''] || ''
const aiLabel = (k: string) => k.replace(/_/g, ' ').replace(/total|spk ipd/gi, '').trim() || k

const jumpFlow = (row: any) => row?.id && push({ name: 'SpkIpdCockpit' })
const openProject = (id: number) => push({ name: 'SpkIpdProjectDetail', query: { projectId: id } })
const goProjects = () => push({ name: 'SpkIpdProjects' })
const jumpAttention = (a: IpdBusinessApi.SpkIpdOverviewAttentionItem) => {
  if (a.type === 'BLOCKED_FLOW' || a.type === 'PENDING_DECISION') push({ name: 'SpkIpdCockpit' })
  else if (a.type === 'OVERDUE_VERSION' || a.type === 'SEVERE_ISSUE') push({ name: 'SpkIpdProjects' })
}

const load = async () => {
  loading.value = true
  try {
    data.value = await IpdBusinessApi.getOverview()
  } catch (e: any) {
    if (e?.message) message.error(e.message)
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
