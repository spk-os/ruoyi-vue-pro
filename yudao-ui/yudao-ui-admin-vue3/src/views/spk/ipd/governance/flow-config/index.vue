<!--
  SPK-OS IPD 流程配置聚合页（菜单 6987，§B）
  四 Tab：
    1. 工作流模板：当前 flowType 的已发布 Profile（BPM key + 版本 + 发布状态）。
    2. 交付目录结构模板：.flow/asset/src/docs 树预览（来自 Profile.deliveryDirTemplate）+ 默认根路径。
    3. 阶段产物与校验：activity_def 按 stage 分组，列 outputArtifactType/useIndependentVerifier/verifierType/executionLocation。
    4. 节点 skill 与环境：activity_def.skills / envRequirements，下拉选 spk-* skill（来自 skillCatalog）。
  数据铁律：Profile 未接入治理时如实标"未接入"，绝不造假；skill 目录扫描失败用设计文档 §7 兜底。
-->
<template>
  <div class="spk-flow-config">
    <el-alert type="info" :closable="false" show-icon title="流程配置"
      description="一站式配置工作流模板、交付目录结构、各阶段产物与结果验证、节点环境要求与智能体执行技能。启动项目时严格按此规则执行。"
      class="mb-12px" />

    <div class="toolbar mb-12px">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="流程类型">
          <el-select v-model="flowType" style="width: 220px" @change="loadSnapshot">
            <el-option label="完整发布 FULL_RELEASE" value="FULL_RELEASE" />
            <el-option label="增量发布 INCREMENT_RELEASE" value="INCREMENT_RELEASE" />
            <el-option label="问题解决 ISSUE_RESOLUTION" value="ISSUE_RESOLUTION" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadSnapshot">刷新</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-tabs v-model="activeTab" type="card">
      <!-- Tab 1: 工作流模板 -->
      <el-tab-pane label="工作流模板" name="profile">
        <el-alert v-if="!snapshot.profile" type="warning" :closable="false" show-icon
          title="未接入治理" description="该 flowType 尚未创建已发布 Profile，项目启动将走默认 BPM key + 默认目录模板。请到「流程管理」创建并发布 Profile。" class="mb-12px" />
        <el-descriptions v-else :column="2" border size="small" title="已发布 Profile">
          <el-descriptions-item label="编码">{{ snapshot.profile.profileCode }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ snapshot.profile.name }}</el-descriptions-item>
          <el-descriptions-item label="流程类型">{{ flowTypeLabel(snapshot.profile.flowType) }}（{{ snapshot.profile.flowType }}）</el-descriptions-item>
          <el-descriptions-item label="BPM Key">{{ snapshot.profile.processDefinitionKey }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(snapshot.profile.status)">{{ profileStatusLabel(snapshot.profile.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前版本">v{{ snapshot.profile.currentVersion }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ snapshot.profile.description || '—' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- Tab 2: 交付目录结构模板 -->
      <el-tab-pane label="交付目录结构" name="dir">
        <el-descriptions :column="1" border size="small" class="mb-12px" title="默认配置">
          <el-descriptions-item label="默认项目根路径">
            <code>{{ snapshot.defaultProjectRootPattern }}</code>
            <div class="hint">启动项目时按此模板渲染 {businessKey}；用户可在发起向导中修改。</div>
          </el-descriptions-item>
          <el-descriptions-item label="节点环境默认">
            <el-tag size="small">{{ snapshot.envProfile }}</el-tag>
            <span class="hint ml-8px">activity_def.envRequirements 为空时继承</span>
          </el-descriptions-item>
          <el-descriptions-item label="默认 skill 映射">
            <code class="block-code">{{ snapshot.defaultSkillBindings }}</code>
          </el-descriptions-item>
        </el-descriptions>
        <h4 class="block-title">目录结构模板</h4>
        <el-table :data="dirTree" border stripe size="small">
          <el-table-column label="目录" prop="name" width="120" />
          <el-table-column label="类型" prop="kind" width="100">
            <template #default="{ row }"><el-tag size="small">{{ row.kind }}</el-tag></template>
          </el-table-column>
          <el-table-column label="说明" prop="desc" min-width="280" />
        </el-table>
        <div class="hint mt-12px">项目启动后真实创建以上四子目录，并写 project.yaml + .flow/manifest.json；每步状态追加写 .flow/ 可还原全流程。</div>
      </el-tab-pane>

      <!-- Tab 3: 阶段产物与校验 -->
      <el-tab-pane label="阶段产物与校验" name="artifact">
        <el-collapse v-model="activeStages">
          <el-collapse-item v-for="(defs, stage) in snapshot.activityDefsByStage" :key="stage" :name="stage">
            <template #title>
              <span class="stage-title">{{ stageLabel(String(stage)) }}</span>
              <el-tag size="small" class="ml-8px">{{ defs.length }} 项</el-tag>
            </template>
            <el-table :data="defs" border stripe size="small">
              <el-table-column label="Activity ID" prop="activityId" width="150" />
              <el-table-column label="名称" prop="name" min-width="140" />
              <el-table-column label="输出产物类型" width="180">
                <template #default="{ row }">
                  <span :title="row.outputArtifactType">{{ artifactTypeLabel(row.outputArtifactType) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="独立验证" prop="useIndependentVerifier" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.useIndependentVerifier ? 'success' : 'info'" size="small">
                    {{ row.useIndependentVerifier ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="验证器类型" width="110">
                <template #default="{ row }">{{ verifierTypeLabel(row.verifierType) }}</template>
              </el-table-column>
              <el-table-column label="执行位置" width="170" show-overflow-tooltip>
                <template #default="{ row }">
                  <span :title="row.executionLocation">{{ executionLocationLabel(row.executionLocation) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </el-tab-pane>

      <!-- Tab 4: 节点 skill 与环境 -->
      <el-tab-pane label="节点 skill 与环境" name="skill">
        <div class="mb-12px">
          <span class="block-title inline">可选 skill 目录</span>
          <el-tag v-for="s in snapshot.skillCatalog" :key="s" class="skill-chip" size="small">{{ s }}</el-tag>
        </div>
        <el-collapse v-model="activeSkillStages">
          <el-collapse-item v-for="(defs, stage) in snapshot.activityDefsByStage" :key="stage" :name="stage">
            <template #title>
              <span class="stage-title">{{ stageLabel(String(stage)) }}</span>
              <el-tag size="small" class="ml-8px">{{ defs.length }} 项</el-tag>
            </template>
            <el-table :data="defs" border stripe size="small">
              <el-table-column label="Activity ID" prop="activityId" width="150" />
              <el-table-column label="名称" prop="name" min-width="120" />
              <el-table-column label="绑定 skill" width="200">
                <template #default="{ row }">
                  <el-select v-model="row.skills" placeholder="选 skill" filterable clearable size="small" style="width: 100%"
                    @change="onSaveBindings(row)">
                    <el-option v-for="s in snapshot.skillCatalog" :key="s" :label="s" :value="s" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="环境要求" min-width="240">
                <template #default="{ row }">
                  <el-input v-model="row.envRequirements" placeholder='例如：{"runtime":"native-ai","timeout":"120s"}'
                    size="small" @blur="onSaveBindings(row)" />
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getFlowConfigSnapshot, updateActivityDefBindings } from '@/api/spk/ipd/governance'
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

const flowType = ref('FULL_RELEASE')
const activeTab = ref('profile')
const activeStages = ref<string[]>([])
const activeSkillStages = ref<string[]>([])
const snapshot = reactive<any>({
  profile: null,
  dirTemplate: '',
  defaultProjectRootPattern: '',
  envProfile: '',
  defaultSkillBindings: '',
  activityDefsByStage: {},
  skillCatalog: [] as string[]
})
const loading = ref(false)

// 目录结构模板 JSON → 表格行（解析失败兜底默认四目录）
const dirTree = computed(() => {
  try {
    const arr = JSON.parse(snapshot.dirTemplate || '[]')
    if (Array.isArray(arr) && arr.length) return arr
  } catch { /* 兜底 */ }
  return [
    { name: '.flow', kind: 'flow', desc: '全流程状态文件，可还原任意时刻流程状态' },
    { name: 'asset', kind: 'asset', desc: '各阶段交付产物（按 stage/版本隔离）' },
    { name: 'src', kind: 'src', desc: '项目代码与构建产物' },
    { name: 'docs', kind: 'docs', desc: '长文档（Gitea 镜像 + 设计文档）' }
  ]
})

const loadSnapshot = async () => {
  loading.value = true
  try {
    const data = await getFlowConfigSnapshot(flowType.value)
    Object.assign(snapshot, data)
    // 默认展开所有 stage
    activeStages.value = Object.keys(snapshot.activityDefsByStage || {})
    activeSkillStages.value = [...activeStages.value]
  } catch (e) {
    ElMessage.error('流程配置快照加载失败')
  } finally {
    loading.value = false
  }
}

// 行内保存 skill/envRequirements（失焦/选择变更触发）
const saveQueue = ref<Set<number>>(new Set())
const onSaveBindings = async (row: any) => {
  if (!row.id) return
  try {
    await updateActivityDefBindings(row.id, { skills: row.skills || '', envRequirements: row.envRequirements || '' })
    ElMessage.success(`${row.activityId} 已保存`)
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
.block-title { font-size: 14px; font-weight: 600; color: var(--el-text-color-primary); }
.block-title.inline { display: inline-block; margin-right: 8px; }
.stage-title { font-size: 14px; font-weight: 600; color: var(--el-color-primary); }
.hint { color: var(--el-text-color-secondary); font-size: 12px; margin-top: 4px; }
.ml-8px { margin-left: 8px; }
.mt-12px { margin-top: 12px; }
.mb-12px { margin-bottom: 12px; }
code { background: var(--el-fill-color-light); padding: 2px 6px; border-radius: 4px; font-size: 12px; }
.block-code { display: block; white-space: pre-wrap; word-break: break-all; }
.skill-chip { margin: 0 4px 4px 0; }
</style>
