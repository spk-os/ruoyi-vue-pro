<template>
  <div v-loading="loading" class="spk-ipd-team">
    <ContentWrap class="!mb-10px">
      <div class="flex items-center justify-between flex-wrap gap-10px">
        <div class="flex items-center gap-10px">
          <span class="text-sm text-gray-500">项目作用域</span>
          <el-select v-model="projectId" filterable clearable placeholder="全部项目（全局）" class="!w-240px" @change="onProjectChange">
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-select v-if="projectId" v-model="versionId" filterable clearable placeholder="项目级（全部版本）" class="!w-220px" @change="load">
            <el-option v-for="v in versions" :key="v.id" :label="v.versionLabel" :value="v.id" />
          </el-select>
          <el-button text @click="load"><Icon icon="ep:refresh" />刷新</el-button>
          <span class="text-xs text-gray-400 ml-10px">人员/容量按项目作用域聚合；Agent/编队为全局注册表管理</span>
        </div>
        <div class="flex gap-10px">
          <StatCard v-for="c in summaryCards" :key="c.label" :label="c.label" :value="c.value" :icon="c.icon" :type="c.type" />
        </div>
      </div>
    </ContentWrap>

    <ContentWrap :body-style="{ padding: '12px' }">
      <el-tabs v-model="tab">
        <!-- 人员 -->
        <el-tab-pane name="people">
          <template #label><Icon class="mr-4px" icon="ep:user" />人员 {{ data.summary?.people || 0 }}</template>
          <div class="flex justify-between items-center mb-10px">
            <span class="text-sm text-gray-500">项目成员来自系统用户，姓名/组织/岗位只读同步；SPK 只存业务角色/容量/有效期</span>
            <el-button v-hasPermi="['spk-delivery:ipd-project:update']" type="primary" size="small" @click="openAdd('HUMAN')">
              <Icon class="mr-4px" icon="ep:plus" />添加人员
            </el-button>
          </div>
          <el-table :data="data.people" size="small">
            <el-table-column label="成员" min-width="140">
              <template #default="{ row }"><b>{{ row.name }}</b><div class="text-xs text-gray-400">{{ row.subtitle }}</div></template>
            </el-table-column>
            <el-table-column label="业务角色" prop="businessRole" width="140" />
            <el-table-column label="Accountable" width="110" align="center">
              <template #default="{ row }"><el-tag v-if="row.accountableFlag" type="success" size="small">是</el-tag><span v-else class="text-gray-400">否</span></template>
            </el-table-column>
            <el-table-column label="容量" prop="capacityPct" width="80" align="center">
              <template #default="{ row }">{{ row.capacityPct }}%</template>
            </el-table-column>
            <el-table-column label="计划中" width="80" align="center">
              <template #default="{ row }">{{ taskOf(row)?.planned ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="进行中" width="80" align="center">
              <template #default="{ row }"><el-tag v-if="taskOf(row)?.running" size="small" type="warning">{{ taskOf(row).running }}</el-tag><span v-else>0</span></template>
            </el-table-column>
            <el-table-column label="已完成" width="80" align="center">
              <template #default="{ row }">{{ taskOf(row)?.done ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="阻断" width="80" align="center">
              <template #default="{ row }"><el-tag v-if="taskOf(row)?.blocked" size="small" type="danger">{{ taskOf(row).blocked }}</el-tag><span v-else>0</span></template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ statusLabel(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button v-hasPermi="['spk-delivery:ipd-project:update']" link type="danger" size="small" @click="onRemove(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!data.people?.length" description="暂无人员" :image-size="60" />
        </el-tab-pane>

        <!-- Agent（全局注册表管理，设计 §4.6 line345：编号/运行时/默认模型/状态/操作+负载。项目级绑定已下沉至项目空间 S8） -->
        <el-tab-pane name="agents" lazy>
          <template #label><Icon class="mr-4px" icon="ep:cpu" />Agent</template>
          <div class="text-sm text-gray-500 mb-10px">全局智能体定义注册表（复用 AgentForm/WakeDialog）；下方为运行负载（按 spk_task_contract 真实聚合）</div>
          <AgentDefPanel />
          <div class="load-sep">运行负载</div>
          <AgentLoadPanel />
        </el-tab-pane>

        <!-- 编队（全局 squad 管理，设计 §4.6 line346：编号/成员数/状态/操作，复用 SquadForm。项目级绑定已下沉至项目空间 S8） -->
        <el-tab-pane name="squads" lazy>
          <template #label><Icon class="mr-4px" icon="ep:suitcase" />编队</template>
          <div class="text-sm text-gray-500 mb-10px">全局智能体编队注册表（复用 SquadForm）；编队内部由 Task Router 选择执行 Agent</div>
          <component :is="SquadPage" />
        </el-tab-pane>

        <!-- 容量视图（增强热力图，设计 §4.6 line347：人+Agent 统一负载热力图，瓶颈角色高亮） -->
        <el-tab-pane name="load">
          <template #label><Icon class="mr-4px" icon="ep:data-line" />容量视图</template>
          <div class="text-sm text-gray-500 mb-10px">人/Agent/编队统一负载热力图（按 spk_task_contract 分派计数，单元格深浅=负载强度，阻断>0 行高亮为瓶颈）</div>
          <el-table :data="data.load" size="small" :row-class-name="bottleneckRow">
            <el-table-column label="参与者" min-width="160">
              <template #default="{ row }"><b>{{ row.name }}</b><el-tag class="ml-8px" size="small">{{ row.actorType }}</el-tag></template>
            </el-table-column>
            <el-table-column label="计划中" prop="planned" width="90" align="center">
              <template #default="{ row }"><span class="heat-cell" :style="heatStyle(row.planned, row.total)">{{ row.planned ?? 0 }}</span></template>
            </el-table-column>
            <el-table-column label="进行中" prop="running" width="90" align="center">
              <template #default="{ row }"><span class="heat-cell" :style="heatStyle(row.running, row.total, true)">{{ row.running ?? 0 }}</span></template>
            </el-table-column>
            <el-table-column label="阻断" prop="blocked" width="80" align="center">
              <template #default="{ row }"><span class="heat-cell" :style="heatStyle(row.blocked, row.total, false, true)">{{ row.blocked ?? 0 }}</span></template>
            </el-table-column>
            <el-table-column label="已完成" prop="done" width="90" align="center">
              <template #default="{ row }"><span class="heat-cell" :style="heatStyle(row.done, row.total)">{{ row.done ?? 0 }}</span></template>
            </el-table-column>
            <el-table-column label="总数" prop="total" width="80" align="center" />
            <el-table-column label="负载热度" min-width="180">
              <template #default="{ row }">
                <el-progress :percentage="row.total ? Math.round((row.done / row.total) * 100) : 0" :status="row.blocked ? 'exception' : ''" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!data.load?.length" description="暂无负载数据（项目尚未派发任务或未选项目作用域）" :image-size="60" />
        </el-tab-pane>

        <!-- 拓扑视图（诚实缺口：后端无组织拓扑/在线态势数据源，不画假图，待后续阶段补端点） -->
        <el-tab-pane name="topology" lazy>
          <template #label><Icon class="mr-4px" icon="ep:share" />拓扑视图</template>
          <el-empty description="拓扑视图暂无数据源">
            <template #description>
              <p>拓扑视图暂无数据源</p>
              <p class="text-xs text-gray-400">后端尚无组织关系/在线态势专用端点，设计稿 §4.6 line348 标注本 Tab 为可选并入项。为遵守"不造假"铁律，此处不绘制虚构拓扑图，待后续阶段补齐端点后接入。</p>
            </template>
          </el-empty>
        </el-tab-pane>
      </el-tabs>
    </ContentWrap>

    <!-- 添加参与者弹窗 -->
    <el-dialog v-model="addDialog.visible" :title="addDialogTitle" width="640px" append-to-body>
      <el-form :model="addForm" label-width="100px" class="mb-10px">
        <el-form-item label="候选检索">
          <el-input v-model="addForm.keyword" placeholder="输入名称关键字（人按昵称模糊查）" clearable class="!w-360px" @keyup.enter="searchCandidates">
            <template #append><el-button @click="searchCandidates"><Icon icon="ep:search" /></el-button></template>
          </el-input>
        </el-form-item>
      </el-form>
      <el-radio-group v-model="addForm.selectedKey" class="mb-10px">
        <el-radio v-for="(c, i) in candidateList" :key="i" :value="i" class="!mr-20px !mb-10px">
          <el-tag size="small" class="mr-6px">{{ c.actorType }}</el-tag>{{ c.name }}
          <span class="text-xs text-gray-400 ml-6px">{{ c.subtitle }}</span>
        </el-radio>
      </el-radio-group>
      <el-empty v-if="!candidateList.length" description="输入关键字检索候选人/Agent/编队" :image-size="40" />
      <el-form v-if="candidateSelected" :model="addForm" label-width="100px">
        <el-form-item label="业务角色">
          <el-select v-model="addForm.businessRole" filterable allow-create placeholder="如 IPD-PM/PO/ARCH/DEV/QA/RELEASE" class="!w-full">
            <el-option v-for="r in ROLE_OPTIONS" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="Accountable">
          <el-switch v-model="addForm.accountable" :active-value="1" :inactive-value="0" />
          <span class="text-xs text-gray-400 ml-10px">同一作用域同一角色只能一个 accountable</span>
        </el-form-item>
        <el-form-item label="容量">
          <el-input-number v-model="addForm.capacityPct" :min="0" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="!candidateSelected || !addForm.businessRole" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import StatCard from '../overview/StatCard.vue'
import { statusMap, labelText } from '@/views/spk/ipd/home/components/status'
import AgentDefPanel from '../../agent/AgentDefPanel.vue'
import AgentLoadPanel from '../../agent/AgentLoadPanel.vue'
import { defineAsyncComponent } from 'vue'

defineOptions({ name: 'SpkIpdTeam' })

// 编队页降为组件复用（与 agent/index.vue Tab2 同源，避免两处维护）
const SquadPage = defineAsyncComponent(() => import('../../squad/index.vue'))

const message = useMessage()
const loading = ref(true)
const tab = ref('people')
const projectId = ref<number | undefined>(undefined)
const versionId = ref<number | undefined>(undefined)
const data = ref<IpdBusinessApi.SpkIpdTeamVO>({})
const projects = ref<Array<{ id: number; name: string }>>([])
const versions = ref<Array<{ id: number; versionLabel: string }>>([])

const summaryCards = computed(() => {
  const s = data.value.summary || {}
  return [
    { label: '人员', value: s.people || 0, icon: 'ep:user', type: 'primary' },
    { label: 'Agent', value: s.agents || 0, icon: 'ep:cpu', type: 'success' },
    { label: '编队', value: s.squads || 0, icon: 'ep:suitcase', type: 'warning' },
    { label: '参与者', value: s.actors || 0, icon: 'ep:avatar', type: '' }
  ]
})
const statusLabel = (s?: string) => labelText(statusMap, s)

// 人员 Tab 任务计数：按 (actorType, actorId) 关联 load 行（后端 ActorRow/LoadRow 均带此键）
const loadMap = computed(() => {
  const m = new Map<string, any>()
  for (const r of data.value.load || []) {
    m.set(`${r.actorType}:${r.actorId}`, r)
  }
  return m
})
const taskOf = (row: any) => loadMap.value.get(`${row.actorType}:${row.actorId}`)

// 容量热力图：单元格背景随负载比例加深；进行中=橙、阻断=红、其余=蓝；阻断>0 行标瓶颈
const heatStyle = (val: number, total: number, isRunning = false, isBlocked = false) => {
  const v = val || 0
  const t = total || v || 1
  const ratio = Math.min(v / t, 1)
  if (isBlocked) return v > 0 ? { background: 'rgba(245,108,108,0.55)', color: '#fff' } : {}
  if (isRunning) return v > 0 ? { background: `rgba(230,162,60,${0.2 + ratio * 0.5})` } : {}
  return v > 0 ? { background: `rgba(64,158,255,${0.15 + ratio * 0.4})` } : {}
}
const bottleneckRow = ({ row }: any) => (row.blocked > 0 ? 'heat-bottleneck' : '')

const load = async () => {
  loading.value = true
  try {
    data.value = await IpdBusinessApi.getTeam(projectId.value, versionId.value)
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
const onProjectChange = async () => {
  versionId.value = undefined
  versions.value = []
  if (projectId.value) {
    try {
      const majors = await IpdBusinessApi.listMajorReleases(projectId.value)
      const all: Array<{ id: number; versionLabel: string }> = []
      for (const m of majors || []) {
        const vs = await IpdBusinessApi.listVersions(m.id)
        for (const v of vs || []) all.push({ id: v.id, versionLabel: v.versionLabel })
      }
      versions.value = all
    } catch {}
  }
  await load()
}

// ---------- 添加/移除参与者 ----------
const ROLE_OPTIONS = ['IPD-PM', 'PO', 'ARCH', 'DEV', 'QA', 'RELEASE', 'DCP_DECIDER', 'VERIFIER', 'OBSERVER']
const addDialog = reactive({ visible: false })
const addForm = reactive({
  actorType: 'HUMAN' as string,
  keyword: '',
  selectedKey: undefined as number | undefined,
  businessRole: '',
  accountable: 0,
  capacityPct: 100
})
const candidateList = ref<Array<IpdBusinessApi.SpkIpdActorCandidate>>([])

const addDialogTitle = computed(() => ({
  HUMAN: '添加人员',
  AGENT: '绑定 Agent',
  SQUAD: '绑定编队'
} as any)[addForm.actorType])
const candidateSelected = computed(() => addForm.selectedKey !== undefined && candidateList.value[addForm.selectedKey])

const openAdd = (type: string) => {
  if (!projectId.value) {
    message.warning('请先选择项目作用域')
    return
  }
  addForm.actorType = type
  addForm.keyword = ''
  addForm.selectedKey = undefined
  addForm.businessRole = ''
  addForm.accountable = 0
  addForm.capacityPct = 100
  candidateList.value = []
  addDialog.visible = true
}
const searchCandidates = async () => {
  candidateList.value = await IpdBusinessApi.candidates(addForm.actorType, undefined, addForm.keyword)
}
const submitAdd = async () => {
  const c = candidateList.value[addForm.selectedKey!]
  if (!c || !addForm.businessRole) return
  await IpdBusinessApi.saveActor(projectId.value!, {
    projectId: projectId.value,
    versionId: versionId.value,
    actorType: c.actorType,
    actorId: c.actorId,
    businessRole: addForm.businessRole,
    accountableFlag: addForm.accountable,
    capacityPct: addForm.capacityPct,
    status: 'ACTIVE'
  })
  message.success('已添加')
  addDialog.visible = false
  await load()
}
const onRemove = async (row: any) => {
  await message.delConfirm(row.name)
  await IpdBusinessApi.deleteActor(row.id)
  message.success('已移除')
  await load()
}

onMounted(async () => { await loadProjects(); await load() })
</script>

<style lang="scss" scoped>
.heat-cell {
  display: inline-block;
  min-width: 28px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}
:deep(.heat-bottleneck) {
  background: rgba(245, 108, 108, 0.08);
}
.load-sep {
  margin: 16px 0 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
  border-left: 3px solid var(--el-color-primary);
  padding-left: 8px;
}
</style>
