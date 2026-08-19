<template>
  <div class="start-wizard">
    <el-steps :active="step" finish-status="success" align-center class="mb-20px">
      <el-step title="选项目与版本" />
      <el-step title="选模板" />
      <el-step title="范围裁剪" />
      <el-step title="预检与启动" />
    </el-steps>

    <!-- 步骤1：选项目 + 版本 + 流程类型 -->
    <el-card v-if="step === 0" shadow="never">
      <el-form :model="form" label-width="100px">
        <el-form-item label="项目" required>
          <el-select v-model="form.projectId" filterable placeholder="选择项目" class="!w-360px" @change="onProjectChange">
            <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本" required>
          <el-select v-model="form.versionId" filterable placeholder="选择版本（项目级可不选）" class="!w-360px" :disabled="!form.projectId">
            <el-option v-for="v in versions" :key="v.id" :label="v.versionLabel" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="流程类型" required>
          <el-radio-group v-model="form.flowType">
            <el-radio v-for="f in FLOW_TYPES" :key="f.value" :value="f.value">{{ f.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div class="step-actions">
        <el-button type="primary" :disabled="!form.projectId || !form.flowType" @click="step = 1">下一步</el-button>
      </div>
    </el-card>

    <!-- 步骤2：选模板（流程类型说明，简洁可懂） -->
    <el-card v-if="step === 1" shadow="never">
      <el-descriptions :column="1" border size="small" class="mb-12px">
        <el-descriptions-item label="流程类型">{{ flowTypeLabel(form.flowType) }}</el-descriptions-item>
        <el-descriptions-item label="说明">{{ flowTypeDesc(form.flowType) }}</el-descriptions-item>
        <el-descriptions-item label="绑定版本类型要求">FULL_RELEASE 仅绑 BASELINE；INCREMENT 配 FULL_RELEASE；跨验证独立</el-descriptions-item>
      </el-descriptions>
      <div class="step-actions">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" :disabled="!form.projectId || !form.flowType" @click="step = 2">下一步</el-button>
      </div>
    </el-card>

    <!-- 步骤3：范围裁剪（阶段/门禁 checkbox，用户可懂；映射到 tailoring 契约） -->
    <el-card v-if="step === 2" shadow="never">
      <el-form :model="form" label-width="100px">
        <el-form-item label="阶段裁剪">
          <el-checkbox-group v-model="form.keepStages" class="stage-gates">
            <el-checkbox v-for="s in STAGES" :key="s.key" :value="s.key" :disabled="s.fixed">{{ s.label }}</el-checkbox>
          </el-checkbox-group>
          <div class="form-hint">未勾选阶段将按 Profile 规则跳过或简化</div>
        </el-form-item>
        <el-form-item label="门禁裁剪">
          <el-checkbox-group v-model="form.keepGates" class="stage-gates">
            <el-checkbox v-for="g in GATES" :key="g.key" :value="g.key" :disabled="g.fixed">{{ g.label }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="架构模式">
          <el-select v-model="form.architectureMode" clearable placeholder="不裁剪（可选）" class="!w-280px">
            <el-option v-for="m in ARCH_MODES" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="裁剪理由">
          <el-input v-model="form.reason" type="textarea" :rows="2" placeholder="裁剪说明（可选）" />
        </el-form-item>
      </el-form>
      <div class="step-actions">
        <el-button @click="step = 1">上一步</el-button>
        <el-button type="primary" @click="step = 3">下一步</el-button>
      </div>
    </el-card>

    <!-- 步骤4：预检 + 创建/启动（预检结果就地显示，修复旧版跨步"点击无反应"） -->
    <el-card v-if="step === 3" shadow="never">
      <div class="mb-12px text-sm text-gray-500">目标：{{ form.flowType }} 流程，版本绑定后执行预检（不阻断创建），通过后创建并异步启动。</div>
      <el-button :loading="preflighting" type="primary" @click="doPreflight">
        <Icon class="mr-4px" icon="ep:check" />执行预检
      </el-button>
      <div v-if="preflightResult" class="preflight-box mt-12px">
        <div class="mb-8px">
          <el-tag :type="verdictTag(preflightVerdict)">{{ preflightVerdict }}</el-tag>
          <span class="ml-10px text-sm">{{ preflightSummary }}</span>
        </div>
        <el-table :data="preflightResult.checks" size="small" border>
          <el-table-column label="检查项" prop="code" min-width="160" />
          <el-table-column label="结果" width="100" align="center">
            <template #default="{ row }"><el-tag :type="verdictTag(row.status)" size="small">{{ row.status }}</el-tag></template>
          </el-table-column>
          <el-table-column label="说明" prop="message" min-width="280" show-overflow-tooltip />
        </el-table>
      </div>
      <div v-if="createdRun" class="mt-16px created-box">
        <el-alert type="success" :closable="false" show-icon>
          <template #title>FlowRun 已创建{{ createdRun.started ? '并已异步启动' : '（未启动）' }}</template>
          <div>编号：{{ createdRun.runNo }}（#{{ createdRun.id }}）· 状态：{{ createdRun.status }}</div>
        </el-alert>
        <el-button class="mt-8px" type="primary" @click="goToRun">前往流程运行查看</el-button>
      </div>
      <div class="step-actions">
        <el-button @click="step = 2">上一步</el-button>
        <el-button type="success" :disabled="!preflightResult" :loading="creating" @click="doCreate">创建并启动</el-button>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import * as IpdBusinessApi from '@/api/spk/ipd/business'
import { useRouter } from 'vue-router'

defineOptions({ name: 'StartFlowWizard' })

const router = useRouter()
const message = useMessage()
const step = ref(0)
const preflighting = ref(false)
const creating = ref(false)
const preflightResult = ref<any>(null)
const createdRun = ref<any>(null)

const FLOW_TYPES = [
  { value: 'FULL_RELEASE', label: '全量发布（基线）' },
  { value: 'INCREMENT_RELEASE', label: '增量发布' },
  { value: 'INTEGRATED_CROSS_VALIDATION', label: '集成交叉验证' }
]
const ARCH_MODES = ['MONOLITH', 'MICROSERVICE', 'EVENT_DRIVEN', 'LAYERED', 'HYBRID']
// 阶段/门禁裁剪选项（对齐原型；fixed 项不可取消）
const STAGES = [
  { key: 'concept', label: '概念', fixed: true },
  { key: 'plan', label: '计划', fixed: false },
  { key: 'develop', label: '开发', fixed: false },
  { key: 'qualify', label: '验证', fixed: false },
  { key: 'launch', label: '发布', fixed: false },
  { key: 'lifecycle', label: '生命周期', fixed: false }
]
const GATES = [
  { key: 'CDCP', label: 'CDCP', fixed: true },
  { key: 'PDCP', label: 'PDCP', fixed: false },
  { key: 'ADCP', label: 'ADCP', fixed: false },
  { key: 'GA', label: 'GA', fixed: false },
  { key: 'LDCP', label: 'LDCP', fixed: false }
]
const flowTypeLabel = (v?: string) => FLOW_TYPES.find(f => f.value === v)?.label || v
const flowTypeDesc = (v?: string) => ({
  FULL_RELEASE: '绑定 BASELINE 版本，走完整 concept→tr→verify→release 六阶段',
  INCREMENT_RELEASE: '版本须配置为 FULL_RELEASE，走增量交付裁剪流程',
  INTEGRATED_CROSS_VALIDATION: '独立跨项目集成交叉验证流程'
} as any)[v] || ''

const form = reactive({
  projectId: undefined as number | undefined,
  versionId: undefined as number | undefined,
  flowType: 'FULL_RELEASE',
  keepStages: ['concept', 'plan', 'develop', 'qualify', 'launch', 'lifecycle'] as string[],
  keepGates: ['CDCP', 'PDCP', 'ADCP'] as string[],
  architectureMode: undefined as string | undefined,
  reason: ''
})
// 裁剪 UI（keepStages/keepGates）映射到后端 tailoring 契约：未保留的阶段/门禁 → skipActivities
const tailoring = computed(() => {
  const skipActivities = [
    ...STAGES.filter(s => !s.fixed && !form.keepStages.includes(s.key)).map(s => s.key),
    ...GATES.filter(g => !g.fixed && !form.keepGates.includes(g.key)).map(g => g.key)
  ]
  return {
    architectureMode: form.architectureMode,
    endGate: form.keepGates.length ? form.keepGates[form.keepGates.length - 1] : '',
    skipActivities,
    reason: form.reason
  }
})
const projects = ref<Array<{ id: number; name: string }>>([])
const versions = ref<Array<{ id: number; versionLabel: string }>>([])

const loadProjects = async () => {
  try {
    const res = await IpdBusinessApi.getPage({ pageNo: 1, pageSize: 50 })
    projects.value = (res.list || []).map((p: any) => ({ id: p.id, name: p.name }))
  } catch {}
}
const onProjectChange = async () => {
  form.versionId = undefined
  versions.value = []
  if (form.projectId) {
    try {
      const majors = await IpdBusinessApi.listMajorReleases(form.projectId)
      const all: Array<{ id: number; versionLabel: string }> = []
      for (const m of majors || []) {
        const vs = await IpdBusinessApi.listVersions(m.id)
        for (const v of vs || []) all.push({ id: v.id, versionLabel: v.versionLabel })
      }
      versions.value = all
    } catch {}
  }
}

const verdictTag = (s?: string) => {
  if (s === 'PASS' || s === 'OK') return 'success'
  if (s === 'WARN') return 'warning'
  if (s === 'BLOCK' || s === 'FAIL' || s === 'ERROR') return 'danger'
  return 'info'
}

// 后端 preflight 响应契约：{ready, checks[{code,status,message,action}], resolvedProfileVersion}
// verdict/summary 由真实字段派生，不写死
const preflightVerdict = computed(() => {
  const r = preflightResult.value
  if (!r) return 'INFO'
  if (r.ready) return 'PASS'
  const checks = r.checks || []
  if (checks.some((c: any) => c.status === 'BLOCK')) return 'BLOCK'
  if (checks.some((c: any) => c.status === 'WARN')) return 'WARN'
  return 'BLOCK'
})
const preflightSummary = computed(() => {
  const r = preflightResult.value
  if (!r) return ''
  const checks = r.checks || []
  const pass = checks.filter((c: any) => c.status === 'PASS').length
  return `${checks.length} 项检查（${pass} 通过），ready=${r.ready}${r.resolvedProfileVersion ? ' · profile v' + r.resolvedProfileVersion : ''}`
})

const doPreflight = async () => {
  preflighting.value = true
  preflightResult.value = null
  try {
    preflightResult.value = await IpdBusinessApi.preflight({
      projectId: form.projectId,
      versionId: form.versionId,
      flowType: form.flowType,
      tailoring: tailoring.value
    })
  } catch (e: any) {
    message.error('预检失败：' + (e?.message || ''))
  } finally {
    preflighting.value = false
  }
}

const doCreate = async () => {
  creating.value = true
  try {
    const created = await IpdBusinessApi.createFlowRun({
      projectId: form.projectId,
      versionId: form.versionId,
      flowType: form.flowType,
      tailoring: tailoring.value
    })
    let started = false
    try {
      await IpdBusinessApi.startFlowRun(created.id, created.runNo || ('flow-start-' + created.id))
      started = true
    } catch (e: any) {
      message.warning('已创建，但异步启动失败：' + (e?.message || ''))
    }
    createdRun.value = { ...created, started }
    message.success('FlowRun 已创建')
  } catch (e: any) {
    message.error('创建失败：' + (e?.message || ''))
  } finally {
    creating.value = false
  }
}

const goToRun = () => {
  if (createdRun.value?.id) router.push({ path: '/spk/ipd-approval', query: { tab: 'runs', runId: createdRun.value.id } })
}

onMounted(loadProjects)
</script>

<style lang="scss" scoped>
.start-wizard { padding: 0 4px; }
.step-actions { margin-top: 16px; text-align: right; }
.stage-gates {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.form-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.preflight-box {
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}
.created-box {
  padding: 12px;
  border: 1px solid var(--el-color-success-light-7);
  border-radius: 6px;
}
</style>
