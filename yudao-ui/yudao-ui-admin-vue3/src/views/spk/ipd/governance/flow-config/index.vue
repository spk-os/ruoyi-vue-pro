<!--
  SPK-OS IPD 流程配置页（菜单 6987，§B；对齐原型 ipd-workflow.html panel-config config-grid 合一卡片）
  四卡片单页并列（不再拆 Tab，消除"阶段产物与校验"和"skill 与环境"重复）：
    1. 工作流模板：当前 flowType 的已发布 Profile 概览（只读 + 状态）。
    2. 交付目录结构：可编辑（项目根路径/节点默认环境/目录结构模板/默认 skill 映射），保存调 PUT /profiles。
    3. 阶段规范（产物与校验 + Skill 与环境 合一）：按 stage 折叠，每 activity 一行，
       列含产物类型/是否验证/验证器/执行位置/skill/命中状态/环境要求，行内保存调 PUT /flow-config/activity-def/{id}。
    4. Skill 全局配置：skillsRoot + 全局默认 env，保存调 PUT /flow-config/skill-config。
  数据铁律：Profile 未接入治理时如实标"未接入"并禁用编辑，绝不造假；skill 目录扫描失败用默认兜底。
-->
<template>
  <div class="spk-flow-config">
    <el-alert type="info" :closable="false" show-icon title="流程管理"
      description="一站式配置工作流模板、交付目录结构、各阶段产物与校验、节点环境要求与智能体执行技能。启动项目时严格按此规则执行。"
      class="mb-12px" />

    <div class="toolbar mb-12px">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="流程类型">
          <el-select v-model="flowType" style="width: 240px" @change="loadSnapshot">
            <el-option label="完整发布 FULL_RELEASE" value="FULL_RELEASE" />
            <el-option label="增量发布 INCREMENT_RELEASE" value="INCREMENT_RELEASE" />
            <el-option label="问题解决 ISSUE_RESOLUTION" value="ISSUE_RESOLUTION" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadSnapshot"><Icon class="mr-4px" icon="ep:refresh" />刷新</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-loading="loading" class="config-grid">
      <!-- 卡片 1：工作流模板（只读 Profile 概览） -->
      <div class="config-card">
        <div class="config-card-title"><span class="dot dot-blue"></span>工作流模板</div>
        <el-alert v-if="!snapshot.profile" type="warning" :closable="false" show-icon
          title="未接入治理" description="该 flowType 尚无已发布 Profile，项目启动走默认 BPM key + 默认目录模板。" class="mb-8px" />
        <el-descriptions v-else :column="1" border size="small">
          <el-descriptions-item label="编码">{{ snapshot.profile.profileCode }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ snapshot.profile.name }}</el-descriptions-item>
          <el-descriptions-item label="BPM Key">{{ snapshot.profile.processDefinitionKey }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(snapshot.profile.status)" size="small">{{ profileStatusLabel(snapshot.profile.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前版本">v{{ snapshot.profile.currentVersion }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ snapshot.profile.description || '—' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 卡片 2：交付目录结构（可编辑，保存调 PUT /profiles） -->
      <div class="config-card">
        <div class="config-card-title"><span class="dot dot-green"></span>交付目录结构</div>
        <el-form label-width="118px" size="small" :disabled="!snapshot.profile">
          <el-form-item label="项目根路径">
            <el-input v-model="dirForm.defaultProjectRootPattern" placeholder="{businessKey} 模板" />
            <div class="hint">启动项目按此渲染；强制 /work/SPK-OS/Delivery/ 前缀，禁 .. 路径注入</div>
          </el-form-item>
          <el-form-item label="节点默认环境">
            <el-select v-model="dirForm.envProfile" style="width: 100%">
              <el-option v-for="e in ENV_OPTIONS" :key="e.value" :label="e.label" :value="e.value" />
            </el-select>
            <div class="hint">activity_def 环境为空时继承此默认</div>
          </el-form-item>
          <el-form-item label="目录结构模板">
            <el-input v-model="dirForm.deliveryDirTemplate" type="textarea" :rows="4" placeholder='JSON 数组，如 [{"name":".flow","kind":"flow","desc":"..."}]' />
          </el-form-item>
          <el-form-item label="默认 skill 映射">
            <el-input v-model="dirForm.defaultSkillBindings" type="textarea" :rows="2" placeholder="stage→skill 映射" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :disabled="!snapshot.profile" :loading="savingDir" @click="onSaveDir">
              <Icon class="mr-4px" icon="ep:folder-checked" />保存目录配置
            </el-button>
          </el-form-item>
        </el-form>
        <div v-if="!snapshot.profile" class="hint">未接入治理：以下为默认值只读，创建并发布 Profile 后可编辑</div>
        <div class="dir-tree">
          <div v-for="n in dirTree" :key="n.name" class="tree-node">
            <span class="tree-folder">{{ n.name }}/</span>
            <span class="tree-desc">{{ n.desc }}</span>
          </div>
        </div>
      </div>
      <!-- 卡片 3：阶段规范（产物与校验 + Skill 与环境 合一，行内编辑） -->
      <div class="config-card config-card--wide">
        <div class="config-card-title"><span class="dot dot-orange"></span>阶段规范（产物与校验 + Skill 与环境）</div>
        <div class="hint mb-8px">各阶段 activity 的产物类型 / 验证规则 / skill / 环境合一编辑，切换或失焦即保存。命中：绿=命中当前env · 黄=回退default · 红=回退stage方法论 · 灰=miss跳过</div>
        <el-collapse v-model="activeStages">
          <el-collapse-item v-for="(defs, stage) in snapshot.activityDefsByStage" :key="stage" :name="stage">
            <template #title>
              <span class="stage-title">{{ stageLabel(String(stage)) }}</span>
              <el-tag size="small" class="ml-8px">{{ defs.length }} 项</el-tag>
            </template>
            <el-table :data="defs" border stripe size="small" class="spec-table">
              <el-table-column label="Activity" width="150">
                <template #default="{ row }">
                  <div class="act-id">{{ row.activityId }}</div>
                  <div class="act-name">{{ row.name }}</div>
                </template>
              </el-table-column>
              <el-table-column label="产物类型" width="150">
                <template #default="{ row }">
                  <el-select v-model="row.outputArtifactType" size="small" clearable style="width: 100%" @change="onSaveRow(row, 'outputArtifactType')">
                    <el-option v-for="k in artifactTypeOptions" :key="k" :label="artifactTypeLabel(k)" :value="k" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="是否验证" width="80" align="center">
                <template #default="{ row }">
                  <el-switch v-model="row.useIndependentVerifier" :active-value="1" :inactive-value="0" @change="onSaveRow(row, 'useIndependentVerifier')" />
                </template>
              </el-table-column>
              <el-table-column label="验证器" width="120">
                <template #default="{ row }">
                  <el-select v-model="row.verifierType" size="small" clearable style="width: 100%" @change="onSaveRow(row, 'verifierType')">
                    <el-option v-for="k in verifierTypeOptions" :key="k" :label="verifierTypeLabel(k)" :value="k" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="执行位置" width="130">
                <template #default="{ row }">
                  <el-select v-model="row.executionLocation" size="small" clearable style="width: 100%" @change="onSaveRow(row, 'executionLocation')">
                    <el-option v-for="k in execLocOptions" :key="k" :label="executionLocationLabel(k)" :value="k" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="绑定 skill" width="200">
                <template #default="{ row }">
                  <el-select v-model="row.skills" size="small" clearable filterable style="width: 100%"
                    :placeholder="`选 skill（${skillConfigForm.defaultEnv}）`" @change="onSaveRow(row, 'skills')">
                    <el-option v-for="s in currentEnvSkills" :key="s.dir" :label="s.description ? `${s.dir} — ${s.description}` : s.dir" :value="s.dir" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="命中状态" width="130">
                <template #default="{ row }">
                  <el-tag :type="hitTagType(resolveHitStatus(row).level)" size="small">{{ resolveHitStatus(row).label }}</el-tag>
                  <div class="hit-skill">{{ resolveHitStatus(row).skill || '—' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="环境要求（高级覆盖）" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.envRequirements" size="small"
                    placeholder='{"skillEnv":"...","runtime":"..."}' @blur="onSaveRow(row, 'envRequirements')" />
                  <div class="hint">skillEnv 覆盖全局默认 env（派发层 ① 级）</div>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- 卡片 4：Skill 全局配置（skillsRoot + 全局默认 env，保存调 PUT /flow-config/skill-config） -->
      <div class="config-card">
        <div class="config-card-title"><span class="dot dot-purple"></span>Skill 全局配置</div>
        <el-form label-width="108px" size="small">
          <el-form-item label="skill 根路径">
            <el-input v-model="skillConfigForm.skillsRoot" placeholder="skillsRoot" />
            <div class="hint">env/skillName/SKILL.md 三段式解析的根</div>
          </el-form-item>
          <el-form-item label="全局默认 env">
            <el-select v-model="skillConfigForm.defaultEnv" style="width: 100%">
              <el-option v-for="e in ENV_OPTIONS" :key="e.value" :label="e.label" :value="e.value" />
            </el-select>
            <el-tag size="small" class="mt-4px">该 env 下 {{ currentEnvSkills.length }} 个 skill</el-tag>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingSkill" @click="onSaveSkillConfig">
              <Icon class="mr-4px" icon="ep:check" />保存配置
            </el-button>
          </el-form-item>
        </el-form>
        <div class="hint">各 env 扫描结果：
          <el-tag v-for="e in envCountList" :key="e.env" size="small" class="skill-chip" :type="e.count === 0 ? 'info' : 'success'">
            {{ e.env }}: {{ e.count }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getFlowConfigSnapshot, updateActivityDefBindings, updateSkillConfig, getProfile, updateProfile } from '@/api/spk/ipd/governance'
import {
  stageMap, flowTypeMap, artifactTypeMap, verifierTypeMap,
  executionLocationMap, statusMap, labelText
} from '@/views/spk/ipd/home/components/status'

defineOptions({ name: 'SpkIpdGovernanceFlowConfig' })

const stageLabel = (s?: string) => labelText(stageMap, s)
const flowTypeLabel = (s?: string) => labelText(flowTypeMap, s)
const artifactTypeLabel = (s?: string) => labelText(artifactTypeMap, s)
const verifierTypeLabel = (s?: string) => labelText(verifierTypeMap, s)
const executionLocationLabel = (s?: string) => labelText(executionLocationMap, s)
const profileStatusLabel = (s?: string) => labelText(statusMap, s)

// select options：从 map keys 动态生成（无硬编码，map 扩即自动多选项）
const artifactTypeOptions = computed(() => Object.keys(artifactTypeMap))
const verifierTypeOptions = computed(() => Object.keys(verifierTypeMap))
const execLocOptions = computed(() => Object.keys(executionLocationMap))

const flowType = ref('FULL_RELEASE')
const activeStages = ref<string[]>([])
const loading = ref(false)
const savingDir = ref(false)
const savingSkill = ref(false)
const snapshot = reactive<any>({
  profile: null,
  dirTemplate: '',
  defaultProjectRootPattern: '',
  envProfile: '',
  defaultSkillBindings: '',
  activityDefsByStage: {},
  skillCatalog: [] as string[],
  skillsRoot: '',
  defaultSkillEnv: 'default',
  envCatalog: {} as Record<string, any[]>
})

// 卡片2 交付目录表单（同步 Profile 四治理字段）
const dirForm = reactive({
  deliveryDirTemplate: '',
  defaultProjectRootPattern: '',
  envProfile: '',
  defaultSkillBindings: ''
})
// 卡片4 skill 全局配置表单
const skillConfigForm = reactive({ skillsRoot: '', defaultEnv: 'default' })

const ENV_OPTIONS = [
  { label: 'default（开发/测试默认）', value: 'default' },
  { label: 'test（轻量化测试）', value: 'test' },
  { label: 'commercial-release（商业交付·编号编排器）', value: 'commercial-release' },
  { label: 'prototype-release（原型）', value: 'prototype-release' }
]
const STAGE_SKILL_FALLBACK: Record<string, string> = {
  concept: 'spk-ipd-concept', plan: 'spk-ipd-plan', develop: 'spk-ipd-develop',
  qualify: 'spk-ipd-verify', launch: 'spk-ipd-launch', lifecycle: 'spk-ipd-tr-gate'
}
const COMMERCIAL_STAGE_SKILLS: Record<string, string> = {
  concept: 'spk-ipd-01-concept', plan: 'spk-ipd-02-plan', develop: 'spk-ipd-03-develop',
  qualify: 'spk-ipd-04-verify', launch: 'spk-ipd-05-launch', lifecycle: 'spk-ipd-06-lifecycle'
}

// 目录结构模板 JSON → 表格行（解析失败兜底默认四目录）
const dirTree = computed(() => {
  try {
    const arr = JSON.parse(snapshot.dirTemplate || '[]')
    if (Array.isArray(arr) && arr.length) return arr
  } catch { /* 兜底 */ }
  return [
    { name: '.flow', desc: '全流程状态文件，可还原任意时刻流程状态' },
    { name: 'asset', desc: '各阶段交付产物（按 stage/版本隔离）' },
    { name: 'src', desc: '项目代码与构建产物' },
    { name: 'docs', desc: '长文档（Gitea 镜像 + 设计文档）' }
  ]
})

const loadSnapshot = async () => {
  loading.value = true
  try {
    const data = await getFlowConfigSnapshot(flowType.value)
    Object.assign(snapshot, data)
    // 同步卡片2/4 表单为 DB 当前值
    dirForm.deliveryDirTemplate = snapshot.dirTemplate || ''
    dirForm.defaultProjectRootPattern = snapshot.defaultProjectRootPattern || ''
    dirForm.envProfile = snapshot.envProfile || 'native-ai'
    dirForm.defaultSkillBindings = snapshot.defaultSkillBindings || ''
    skillConfigForm.skillsRoot = snapshot.skillsRoot || ''
    skillConfigForm.defaultEnv = snapshot.defaultSkillEnv || 'default'
    // 阶段规范表：normalize skills 为首元素单字符串供 select 绑定（派发层取首元素）
    Object.values(snapshot.activityDefsByStage || {}).forEach((defs: any) => {
      defs.forEach((d: any) => { d.skills = parseFirstSkill(d.skills) })
    })
    activeStages.value = Object.keys(snapshot.activityDefsByStage || {})
  } catch (e) {
    ElMessage.error('流程配置快照加载失败')
  } finally {
    loading.value = false
  }
}

// 保存卡片2 交付目录配置：先 getProfile 拿完整 VO 再 merge 四字段（避免覆盖其他字段为空）
const onSaveDir = async () => {
  if (!snapshot.profile?.id) return
  savingDir.value = true
  try {
    const full = await getProfile(snapshot.profile.id)
    await updateProfile({
      ...full,
      deliveryDirTemplate: dirForm.deliveryDirTemplate,
      defaultProjectRootPattern: dirForm.defaultProjectRootPattern,
      envProfile: dirForm.envProfile,
      defaultSkillBindings: dirForm.defaultSkillBindings
    })
    ElMessage.success('交付目录配置已保存（即时生效）')
    await loadSnapshot()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    savingDir.value = false
  }
}

// 保存卡片4 skill 全局配置
const onSaveSkillConfig = async () => {
  savingSkill.value = true
  try {
    await updateSkillConfig({ skillsRoot: skillConfigForm.skillsRoot, defaultEnv: skillConfigForm.defaultEnv })
    ElMessage.success('skill 环境配置已保存（即时生效）')
    await loadSnapshot()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    savingSkill.value = false
  }
}

const currentEnvSkills = computed(() => snapshot.envCatalog?.[skillConfigForm.defaultEnv] || [])
const envCountList = computed(() => {
  const cat = snapshot.envCatalog || {}
  return Object.keys(cat).map((env) => ({ env, count: (cat[env] || []).length }))
})

const parseFirstSkill = (skillsJson: string | null | undefined): string | null => {
  if (!skillsJson) return null
  try {
    const arr = JSON.parse(skillsJson)
    if (Array.isArray(arr) && arr.length) return String(arr[0])
  } catch { /* 兜底 */ }
  return null
}

// 复刻派发层 resolveSkillNameForEnv + resolveSkillPath 三级回退命中判定
const resolveHitStatus = (row: any): { level: string; label: string; skill: string } => {
  const env = skillConfigForm.defaultEnv || 'default'
  const stage = row.stage
  let skillName = parseFirstSkill(row.skills) || (stage ? STAGE_SKILL_FALLBACK[stage] : null)
  if (!skillName) return { level: 'miss', label: '无 skill（跳过注入）', skill: '' }
  if (env === 'commercial-release') {
    const envSkills = snapshot.envCatalog?.['commercial-release'] || []
    if (!envSkills.some((s: any) => s.dir === skillName) && stage && COMMERCIAL_STAGE_SKILLS[stage]) {
      skillName = COMMERCIAL_STAGE_SKILLS[stage]
    }
  }
  const envSkills = snapshot.envCatalog?.[env] || []
  const defaultSkills = snapshot.envCatalog?.['default'] || []
  if (envSkills.some((s: any) => s.dir === skillName)) return { level: 'primary', label: `命中 ${env}`, skill: skillName }
  if (env !== 'default' && defaultSkills.some((s: any) => s.dir === skillName)) return { level: 'fallbackDefault', label: '回退 default', skill: skillName }
  if (stage && STAGE_SKILL_FALLBACK[stage] && defaultSkills.some((s: any) => s.dir === STAGE_SKILL_FALLBACK[stage]))
    return { level: 'stageFallback', label: '回退 stage 方法论', skill: STAGE_SKILL_FALLBACK[stage] }
  return { level: 'miss', label: '全 miss（跳过注入）', skill: skillName }
}

const hitTagType = (level: string) => ({ primary: 'success', fallbackDefault: 'warning', stageFallback: 'danger', miss: 'info' } as any)[level] || ''

// 行内保存单字段：normalize 后只传变更字段（避免覆盖其他字段空值）
const onSaveRow = async (row: any, field: string) => {
  if (!row.id) return
  const body: Record<string, string> = {}
  if (field === 'skills') {
    // skills 归一化为 JSON 数组字符串（派发层 parseFirstSkill 取首元素）
    let v = row.skills || ''
    if (v && !v.trim().startsWith('[')) v = JSON.stringify([v])
    row.skills = parseFirstSkill(v)
    body.skills = v
  } else if (field === 'useIndependentVerifier') {
    body[field] = String(row[field] ?? 0)
  } else {
    body[field] = row[field] == null ? '' : String(row[field])
  }
  try {
    await updateActivityDefBindings(row.id, body)
    ElMessage.success(`${row.activityId} · ${field} 已保存`)
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const statusTagType = (s: string) => ({ PUBLISHED: 'success', DRAFT: 'info', SUPERSEDED: 'warning', DEPRECATED: 'danger' } as any)[s] || ''

onMounted(loadSnapshot)
</script>
<style scoped>
.spk-flow-config { padding: 12px; }
.toolbar { margin-bottom: 12px; }
.config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}
.config-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}
.config-card--wide {
  grid-column: 1 / -1;
}
.config-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}
.dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.dot-blue { background: #409eff; }
.dot-green { background: #67c23a; }
.dot-orange { background: #e6a23c; }
.dot-purple { background: #8b5cf6; }
.stage-title { font-size: 14px; font-weight: 600; color: var(--el-color-primary); }
.hint { color: var(--el-text-color-secondary); font-size: 12px; margin-top: 4px; }
.ml-8px { margin-left: 8px; }
.mt-4px { margin-top: 4px; }
.mb-8px { margin-bottom: 8px; }
.mb-12px { margin-bottom: 12px; }
.dir-tree { display: flex; flex-direction: column; gap: 4px; margin-top: 4px; }
.tree-node { font-size: 12px; display: flex; gap: 8px; }
.tree-folder { color: var(--el-color-primary); font-weight: 600; }
.tree-desc { color: var(--el-text-color-secondary); }
.spec-table :deep(.el-table__cell) { padding: 4px 0; }
.act-id { font-size: 12px; color: var(--el-text-color-secondary); }
.act-name { font-size: 13px; font-weight: 500; }
.hit-skill { font-size: 11px; color: var(--el-text-color-secondary); margin-top: 2px; }
.skill-chip { margin: 0 4px 4px 0; }
@media (max-width: 1100px) {
  .config-grid { grid-template-columns: 1fr; }
}
</style>
