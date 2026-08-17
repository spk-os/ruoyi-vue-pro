<!--
  SPK-OS IPD 流程治理 - 治理规则配置（对齐原型 profiles.html）
  Profile 列表（左）+ 版本 + 裁剪规则编辑卡片（rule_name/enabled/condition/action）。
  D2/D3：Profile 按 flowType 绑定 BPM key（D1），TrimRule 运行时由评估器消费生效（D3）。
-->
<template>
  <div class="spk-ipd-gov-profiles">
    <el-alert type="info" :closable="false" show-icon title="治理规则配置"
      description="定义和管理流程执行的约束、阈值和裁剪规则。Profile 按 flowType 自动绑定对应 BPM 流程；裁剪规则 SKIP/OPTIONAL/SIMPLIFY 在触发器入口运行时生效。"
      class="mb-12px" />

    <div class="profiles-layout">
      <!-- 左：Profile 列表 -->
      <div class="profile-list-panel">
        <div class="panel-header">
          <span>Profile 列表</span>
          <el-button type="primary" size="small" @click="openCreate">新建</el-button>
        </div>
        <el-input v-model="keyword" placeholder="搜索 profileCode/名称" size="small" clearable class="mb-8px"
          @input="loadProfiles" />
        <div class="profile-cards">
          <el-card v-for="p in profiles" :key="p.id" class="profile-card"
            :class="{ active: selectedId === p.id }" shadow="hover" @click="selectProfile(p)">
            <div class="profile-card-header">
              <span class="profile-name">{{ p.name }}</span>
              <el-tag size="small" :type="statusType(p.status)">{{ statusLabel(p.status) }}</el-tag>
            </div>
            <div class="profile-meta">{{ p.profileCode }} · {{ flowTypeLabel(p.flowType) }}</div>
            <div class="profile-meta">v{{ p.currentVersion }} · key={{ p.processDefinitionKey }}</div>
          </el-card>
          <el-empty v-if="!profiles.length" description="暂无 Profile，点击新建" />
        </div>
      </div>

      <!-- 右：版本 + 规则 -->
      <div class="rule-section">
        <template v-if="selected">
          <div class="rule-section-title">
            <span>{{ selected.name }} - 版本与规则</span>
            <el-button size="small" @click="loadVersions(selected.id!)">刷新</el-button>
          </div>

          <el-select v-model="selectedVersionId" placeholder="选择版本" size="small" class="mb-12px"
            @change="loadRules">
            <el-option v-for="v in versions" :key="v.id" :label="`v${v.version} (${v.status})`"
              :value="v.id" />
          </el-select>

          <div class="version-actions mb-12px">
            <el-button size="small" type="success" :disabled="!selectedVersionId"
              @click="onPublish">发布</el-button>
            <el-button size="small" :disabled="!selectedVersionId" @click="onRollback">回滚</el-button>
            <el-button size="small" type="primary" :disabled="!selectedVersionId"
              @click="openRuleDialog()">新增规则</el-button>
          </div>

          <div class="rule-cards">
            <el-card v-for="r in rules" :key="r.id" class="rule-card" shadow="never">
              <div class="rule-header">
                <span class="rule-name">{{ r.reason || r.activityDefId || r.stage }}</span>
                <div>
                  <el-tag size="small" :type="actionType(r.action)">{{ r.action }}</el-tag>
                  <el-button link size="small" @click="openRuleDialog(r)">编辑</el-button>
                  <el-button link size="small" type="danger" @click="onDelete(r)">删除</el-button>
                </div>
              </div>
              <div class="rule-condition">
                <span class="label">阶段：</span>{{ r.stage || '—' }}
                <span class="label">活动：</span>{{ r.activityDefId || '整阶段' }}
                <span class="label">条件：</span>{{ r.trimCondition || '—' }}
              </div>
            </el-card>
            <el-empty v-if="!rules.length" description="该版本暂无裁剪规则" />
          </div>
        </template>
        <el-empty v-else description="请从左侧选择 Profile" />
      </div>
    </div>

    <!-- 新建 Profile Dialog -->
    <el-dialog v-model="createVisible" title="新建 Profile" width="480px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="profileCode"><el-input v-model="createForm.profileCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="createForm.name" /></el-form-item>
        <el-form-item label="流程类型">
          <el-select v-model="createForm.flowType" style="width: 100%">
            <el-option label="全量发布" value="FULL_RELEASE" />
            <el-option label="增量发布" value="INCREMENT_RELEASE" />
            <el-option label="问题处置" value="ISSUE_RESOLUTION" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="createForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="onCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 规则编辑 Dialog -->
    <el-dialog v-model="ruleDialogVisible" :title="ruleForm.id ? '编辑裁剪规则' : '新增裁剪规则'" width="520px">
      <el-form :model="ruleForm" label-width="90px">
        <el-form-item label="阶段">
          <el-input v-model="ruleForm.stage" placeholder="如 concept（留空+活动匹配则活动级）" />
        </el-form-item>
        <el-form-item label="活动/节点">
          <el-input v-model="ruleForm.activityDefId" placeholder="BPM nodeKey 或 activityId（留空=整阶段）" />
        </el-form-item>
        <el-form-item label="裁剪条件">
          <el-input v-model="ruleForm.trimCondition" placeholder="如 mode==INCREMENT_RELEASE && stage==concept" />
        </el-form-item>
        <el-form-item label="动作">
          <el-select v-model="ruleForm.action" style="width: 100%">
            <el-option label="SKIP 跳过" value="SKIP" />
            <el-option label="OPTIONAL 可选" value="OPTIONAL" />
            <el-option label="SIMPLIFY 简化" value="SIMPLIFY" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="ruleForm.reason" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSaveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageProfiles, createProfile, listVersions, publishVersion, rollbackVersion,
  listTrimRules, saveTrimRule, deleteTrimRule
} from '@/api/spk/ipd/governance'
import { statusMap, flowTypeMap, labelText } from '@/views/spk/ipd/home/components/status'

defineOptions({ name: 'SpkIpdGovernanceProfiles' })

const route = useRoute()
const keyword = ref('')
const profiles = ref<any[]>([])
const selectedId = ref<number | null>(null)
const selected = ref<any>(null)
const versions = ref<any[]>([])
const selectedVersionId = ref<number | null>(null)
const rules = ref<any[]>([])

const createVisible = ref(false)
const createForm = reactive({ profileCode: '', name: '', flowType: 'FULL_RELEASE', description: '' })

const ruleDialogVisible = ref(false)
const ruleForm = reactive<any>({ id: null, profileVersionId: null, stage: '', activityDefId: '', trimCondition: '', action: 'SKIP', reason: '' })

const statusType = (s: string) => ({ PUBLISHED: 'success', DRAFT: 'info', DEPRECATED: 'warning' } as any)[s] || 'info'
const actionType = (a: string) => ({ SKIP: 'danger', OPTIONAL: 'warning', SIMPLIFY: 'primary' } as any)[a] || 'info'
const statusLabel = (s: string) => labelText(statusMap, s)
const flowTypeLabel = (s?: string) => labelText(flowTypeMap, s)

const loadProfiles = async () => {
  const res = await pageProfiles({ pageNo: 1, pageSize: 50, keyword: keyword.value })
  profiles.value = res.list || []
  // 从 templates 页带 flowType 来 → 自动选中或新建
  if (route.query.flowType && !profiles.value.length) {
    createForm.flowType = route.query.flowType as string
    createVisible.value = true
  }
}

const selectProfile = async (p: any) => {
  selected.value = p
  selectedId.value = p.id
  await loadVersions(p.id)
}

const loadVersions = async (profileId: number) => {
  versions.value = await listVersions(profileId)
  const cur = versions.value.find((v: any) => v.status === 'PUBLISHED') || versions.value[0]
  if (cur) {
    selectedVersionId.value = cur.id
    await loadRules()
  } else {
    rules.value = []
  }
}

const loadRules = async () => {
  if (!selectedVersionId.value) { rules.value = []; return }
  rules.value = await listTrimRules(selectedVersionId.value)
}

const openCreate = () => {
  Object.assign(createForm, { profileCode: '', name: '', flowType: 'FULL_RELEASE', description: '' })
  createVisible.value = true
}

const onCreate = async () => {
  await createProfile({ ...createForm, status: 'DRAFT' })
  ElMessage.success('Profile 已创建')
  createVisible.value = false
  await loadProfiles()
}

const onPublish = async () => {
  await publishVersion(selectedVersionId.value!)
  ElMessage.success('版本已发布')
  await loadVersions(selected.value.id!)
}

const onRollback = async () => {
  await rollbackVersion(selectedVersionId.value!)
  ElMessage.success('版本已回滚')
  await loadVersions(selected.value.id!)
}

const openRuleDialog = (r?: any) => {
  if (r) {
    Object.assign(ruleForm, { ...r })
  } else {
    Object.assign(ruleForm, { id: null, profileVersionId: selectedVersionId.value, stage: '', activityDefId: '', trimCondition: '', action: 'SKIP', reason: '' })
  }
  ruleDialogVisible.value = true
}

const onSaveRule = async () => {
  await saveTrimRule({ ...ruleForm, profileVersionId: selectedVersionId.value })
  ElMessage.success('规则已保存')
  ruleDialogVisible.value = false
  await loadRules()
}

const onDelete = async (r: any) => {
  await ElMessageBox.confirm('确认删除该裁剪规则？', '提示', { type: 'warning' })
  await deleteTrimRule(r.id)
  ElMessage.success('已删除')
  await loadRules()
}

onMounted(loadProfiles)
</script>

<style scoped>
.profiles-layout { display: grid; grid-template-columns: 320px 1fr; gap: 16px; }
.profile-list-panel { display: flex; flex-direction: column; }
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-weight: 600; }
.profile-cards { display: flex; flex-direction: column; gap: 8px; }
.profile-card { cursor: pointer; }
.profile-card.active { border-color: var(--el-color-primary); }
.profile-card-header { display: flex; justify-content: space-between; align-items: center; }
.profile-name { font-weight: 600; }
.profile-meta { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
.rule-section { min-height: 400px; }
.rule-section-title { display: flex; justify-content: space-between; align-items: center; font-weight: 600; margin-bottom: 8px; }
.version-actions { display: flex; gap: 8px; }
.rule-cards { display: flex; flex-direction: column; gap: 8px; }
.rule-card { }
.rule-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.rule-name { font-weight: 600; }
.rule-condition { font-size: 12px; color: var(--el-text-color-secondary); }
.rule-condition .label { color: var(--el-text-color-regular); margin-left: 8px; margin-right: 2px; }
.rule-condition .label:first-child { margin-left: 0; }
</style>
