<template>
  <div v-loading="loading">
    <ContentWrap class="!mb-10px">
      <div class="flex items-center justify-between">
        <el-radio-group v-model="projectId" @change="load">
          <el-radio-button :value="undefined">全部项目</el-radio-button>
          <el-radio-button v-for="p in projects" :key="p.id" :value="p.id">{{ p.name }}</el-radio-button>
        </el-radio-group>
        <div class="text-xs text-gray-400">项目维度监控：流程/产物/证据/集成健康聚合</div>
      </div>
    </ContentWrap>

    <!-- 汇总卡 -->
    <ContentWrap class="!mb-10px">
      <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-10px">
        <StatCard v-for="c in summaryCards" :key="c.label" :label="c.label" :value="c.value" :icon="c.icon" :type="c.type" />
      </div>
    </ContentWrap>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-10px">
      <!-- 流程表 -->
      <ContentWrap class="lg:col-span-2 !mb-0">
        <div class="flex justify-between items-center mb-10px">
          <b><Icon class="mr-4px" icon="ep:list" />流程运行（含产物/证据）</b>
          <el-button text @click="load"><Icon icon="ep:refresh" />刷新</el-button>
        </div>
        <el-table :data="data.flows" size="small" @row-click="openCockpit">
          <el-table-column label="运行号" prop="runNo" width="140" />
          <el-table-column label="类型" prop="flowType" width="130" />
          <el-table-column label="阶段" prop="currentStage" width="90" />
          <el-table-column label="状态" prop="status" width="100">
            <template #default="{ row }"><el-tag size="small" :type="flowTag(row.status)">{{ row.status }}</el-tag></template>
          </el-table-column>
          <el-table-column label="健康" prop="health" width="80" />
          <el-table-column label="产物" prop="artifactCount" width="70" align="center" />
          <el-table-column label="证据" prop="evidenceCount" width="70" align="center" />
          <el-table-column label="启动" prop="startedAt" width="150" :formatter="dateFormatter" />
          <el-table-column label="阻断原因" prop="blockReason" min-width="140" show-overflow-tooltip />
        </el-table>
        <el-empty v-if="!data.flows?.length" description="暂无流程运行" :image-size="60" />
      </ContentWrap>

      <!-- 集成健康 -->
      <ContentWrap class="!mb-0">
        <b><Icon class="mr-4px" icon="ep:connection" />集成健康</b>
        <div class="mt-10px space-y-10px">
          <div v-for="i in data.integrations" :key="i.name" class="flex items-center justify-between border-b pb-8px">
            <div>
              <div class="text-sm font-medium">{{ i.name }}</div>
              <div class="text-xs text-gray-400">{{ i.detail }}</div>
            </div>
            <el-tag :type="i.healthy ? 'success' : 'danger'" size="small">
              <Icon class="mr-2px" :icon="i.healthy ? 'ep:circle-check' : 'ep:circle-close'" />{{ i.healthy ? '在线' : '离线' }}
            </el-tag>
          </div>
          <el-empty v-if="!data.integrations?.length" description="无集成" :image-size="40" />
        </div>
      </ContentWrap>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import StatCard from '../overview/StatCard.vue'

defineOptions({ name: 'SpkIpdMonitor' })

const { push } = useRouter()
const loading = ref(true)
const projectId = ref<number | undefined>(undefined)
const data = ref<IpdBusinessApi.SpkIpdMonitorVO>({})
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
