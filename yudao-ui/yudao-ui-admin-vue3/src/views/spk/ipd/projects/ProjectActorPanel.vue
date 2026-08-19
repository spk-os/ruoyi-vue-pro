<!--
  项目级参与者绑定面板（UCD §4.3 + S5 下沉落点）。
  team/index.vue 的全局管理为主；此面板把项目级绑定能力下沉到项目详情。
  HUMAN/AGENT/SQUAD 三类候选检索 + 业务角色/Accountable/容量/状态。
  全真实后端聚合（getTeam/saveActor/deleteActor/candidates），无造假。
-->
<template>
  <div v-loading="loading" class="project-actor-panel">
    <div class="flex justify-between items-center mb-10px">
      <span class="text-sm text-gray-500">
        项目作用域参与者（{{ versionId ? '版本级' : '项目级' }}）：
        人员 {{ stats.people }} · Agent {{ stats.agents }} · 编队 {{ stats.squads }}
      </span>
      <div class="flex gap-8px">
        <el-button v-hasPermi="['spk-delivery:ipd-project:update']" size="small" @click="openAdd('HUMAN')"><Icon class="mr-4px" icon="ep:user" />添加人员</el-button>
        <el-button v-hasPermi="['spk-delivery:ipd-project:update']" size="small" @click="openAdd('AGENT')"><Icon class="mr-4px" icon="ep:cpu" />绑定 Agent</el-button>
        <el-button v-hasPermi="['spk-delivery:ipd-project:update']" size="small" @click="openAdd('SQUAD')"><Icon class="mr-4px" icon="ep:s-flag" />绑定编队</el-button>
        <el-button size="small" @click="load"><Icon icon="ep:refresh" /></el-button>
      </div>
    </div>
    <el-table :data="actors" size="small" border>
      <el-table-column label="参与者" min-width="160">
        <template #default="{ row }"><b>{{ row.name }}</b><div class="text-xs text-gray-400">{{ row.subtitle }}</div></template>
      </el-table-column>
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }"><el-tag size="small">{{ row.actorType }}</el-tag></template>
      </el-table-column>
      <el-table-column label="业务角色" prop="businessRole" width="140" />
      <el-table-column label="Accountable" width="100" align="center">
        <template #default="{ row }"><el-tag v-if="row.accountableFlag" type="success" size="small">是</el-tag><span v-else class="text-gray-400">否</span></template>
      </el-table-column>
      <el-table-column label="容量" width="80" align="center">
        <template #default="{ row }">{{ row.capacityPct }}%</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right" align="center">
        <template #default="{ row }">
          <el-button v-hasPermi="['spk-delivery:ipd-project:update']" link type="danger" size="small" @click="onRemove(row)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !actors.length" description="暂无项目级参与者，点上方按钮添加" :image-size="50" />

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

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import { labelText, statusMap } from '@/views/spk/ipd/home/components/status'

defineOptions({ name: 'ProjectActorPanel' })
const props = defineProps<{ projectId: number; versionId?: number }>()
const message = useMessage()

const loading = ref(true)
const people = ref<any[]>([])
const agents = ref<any[]>([])
const squads = ref<any[]>([])

const actors = computed(() => [...people.value, ...agents.value, ...squads.value])
const stats = computed(() => ({ people: people.value.length, agents: agents.value.length, squads: squads.value.length }))
const statusLabel = (s?: string) => labelText(statusMap, s)

const load = async () => {
  if (!props.projectId) return
  loading.value = true
  try {
    const t = await IpdBusinessApi.getTeam(props.projectId, props.versionId)
    people.value = (t as any).people || []
    agents.value = (t as any).agents || []
    squads.value = (t as any).squads || []
  } catch (e: any) {
    message.error('加载参与者失败：' + (e?.message || ''))
  } finally {
    loading.value = false
  }
}

// ---------- 添加/移除参与者（复用 team/index.vue 同款后端契约） ----------
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
const addDialogTitle = computed(() => ({ HUMAN: '添加人员', AGENT: '绑定 Agent', SQUAD: '绑定编队' } as any)[addForm.actorType])
const candidateSelected = computed(() => addForm.selectedKey !== undefined && candidateList.value[addForm.selectedKey])

const openAdd = (type: string) => {
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
  await IpdBusinessApi.saveActor(props.projectId, {
    projectId: props.projectId,
    versionId: props.versionId,
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

watch(() => [props.projectId, props.versionId], () => load())
onMounted(load)
defineExpose({ refresh: load })
</script>

<style lang="scss" scoped>
.project-actor-panel { padding: 0 2px; }
</style>
