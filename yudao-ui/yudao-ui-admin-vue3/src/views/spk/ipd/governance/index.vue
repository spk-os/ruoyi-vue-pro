<!--
  SPK-OS Cortext-IPD 流程治理（§7.2 第 4 顶层表面）
  4 Tab：流程模板与 Profile / 角色映射与裁剪规则 / 引擎实例档案与失败作业 / 治理审计。
  数据铁律：运行统计无数据如实空，绝不造假；Profile 模板为管理员配置产物可建真实种子。
-->
<template>
  <div class="spk-ipd-governance">
    <el-alert type="info" :closable="false" show-icon title="流程治理（ProcessProfile）"
      description="流程模板/版本/裁剪规则为管理员配置产物；引擎档案/失败作业/审计为运行时聚合，无数据时如实标注，不造假。"
      class="mb-10px" />
    <el-tabs v-model="activeTab" type="card">
      <!-- Tab 1: Profile 模板与版本 -->
      <el-tab-pane label="流程模板与 Profile" name="profiles">
        <div class="tab-toolbar">
          <el-form :inline="true" @submit.prevent>
            <el-form-item label="流程类型">
              <el-select v-model="query.flowType" clearable placeholder="全部" style="width: 160px">
                <el-option label="完整发布 FULL_RELEASE" value="FULL_RELEASE" />
                <el-option label="增量发布 INCREMENT_RELEASE" value="INCREMENT_RELEASE" />
                <el-option label="问题解决 ISSUE_RESOLUTION" value="ISSUE_RESOLUTION" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
                <el-option label="草稿 DRAFT" value="DRAFT" />
                <el-option label="已发布 PUBLISHED" value="PUBLISHED" />
                <el-option label="已弃用 DEPRECATED" value="DEPRECATED" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键字">
              <el-input v-model="query.keyword" placeholder="编码/名称" style="width: 180px" @keyup.enter="onPageChange(1)" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onPageChange(1)">查询</el-button>
              <el-button @click="onResetQuery">重置</el-button>
              <el-button type="success" @click="onOpenProfileForm(null)">新建模板</el-button>
            </el-form-item>
          </el-form>
        </div>
        <el-table v-loading="loading.profiles" :data="profileList" border stripe size="small">
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="编码" prop="profileCode" width="180" />
          <el-table-column label="名称" prop="name" min-width="140" />
          <el-table-column label="流程类型" prop="flowType" width="170" />
          <el-table-column label="状态" prop="status" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="当前版本" prop="currentVersion" width="90" />
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="onOpenProfileForm(row)">编辑</el-button>
              <el-button link type="primary" @click="onOpenVersions(row)">版本</el-button>
              <el-button link type="danger" @click="onDeleteProfile(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination class="mt-10px" v-model:current-page="query.pageNo" v-model:page-size="query.pageSize"
          :total="profileTotal" layout="total, prev, pager, next" @current-change="loadProfiles" />
        <!-- Profile 新建/编辑弹窗 -->
        <el-dialog v-model="profileFormVisible" :title="profileForm.id ? '编辑流程模板' : '新建流程模板'" width="560px">
          <el-form ref="profileFormRef" :model="profileForm" label-width="90px">
            <el-form-item label="编码" v-if="!profileForm.id">
              <el-input v-model="profileForm.profileCode" placeholder="FULL_RELEASE_V1" />
            </el-form-item>
            <el-form-item label="编码" v-else>
              <el-input v-model="profileForm.profileCode" disabled />
            </el-form-item>
            <el-form-item label="名称"><el-input v-model="profileForm.name" /></el-form-item>
            <el-form-item label="流程类型">
              <el-select v-model="profileForm.flowType" :disabled="!!profileForm.id" style="width: 100%">
                <el-option label="完整发布 FULL_RELEASE" value="FULL_RELEASE" />
                <el-option label="增量发布 INCREMENT_RELEASE" value="INCREMENT_RELEASE" />
                <el-option label="问题解决 ISSUE_RESOLUTION" value="ISSUE_RESOLUTION" />
              </el-select>
            </el-form-item>
            <el-form-item label="描述"><el-input v-model="profileForm.description" type="textarea" :rows="2" /></el-form-item>
            <el-form-item label="状态">
              <el-select v-model="profileForm.status" style="width: 100%">
                <el-option label="草稿 DRAFT" value="DRAFT" />
                <el-option label="已发布 PUBLISHED" value="PUBLISHED" />
                <el-option label="已弃用 DEPRECATED" value="DEPRECATED" />
              </el-select>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="profileFormVisible = false">取消</el-button>
            <el-button type="primary" :loading="loading.save" @click="onSaveProfile">保存</el-button>
          </template>
        </el-dialog>
        <!-- 版本管理弹窗 -->
        <el-drawer v-model="versionDrawerVisible" :title="`版本管理 - ${currentProfile?.profileCode ?? ''}`" direction="rtl" size="60%">
          <div v-if="versionDrawerVisible">
            <el-button type="primary" size="small" class="mb-10px" @click="onOpenVersionForm">新建草稿版本</el-button>
            <el-table :data="versionList" border stripe size="small">
              <el-table-column label="ID" prop="id" width="70" />
              <el-table-column label="版本号" prop="version" width="80" />
              <el-table-column label="状态" prop="status" width="110">
                <template #default="{ row }"><el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag></template>
              </el-table-column>
              <el-table-column label="兼容哈希" prop="compatibilityHash" width="160" show-overflow-tooltip />
              <el-table-column label="发布人" prop="publishedBy" width="90" />
              <el-table-column label="发布时间" prop="publishedAt" width="160" />
              <el-table-column label="操作" width="170" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="onPublishVersion(row)">发布</el-button>
                  <el-button v-if="row.status !== 'DRAFT'" link type="warning" @click="onRollbackVersion(row)">回滚到此</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-dialog v-model="versionFormVisible" title="新建草稿版本" width="560px" append-to-body>
              <el-form :model="versionForm" label-width="100px">
                <el-form-item label="档案快照 JSON">
                  <el-input v-model="versionForm.snapshotJson" type="textarea" :rows="8" placeholder='{"stages":[...],"gates":[...]}' />
                </el-form-item>
                <el-form-item label="兼容哈希"><el-input v-model="versionForm.compatibilityHash" placeholder="可选" /></el-form-item>
              </el-form>
              <template #footer>
                <el-button @click="versionFormVisible = false">取消</el-button>
                <el-button type="primary" :loading="loading.version" @click="onCreateVersion">创建</el-button>
              </template>
            </el-dialog>
          </div>
        </el-drawer>
      </el-tab-pane>
      <!-- Tab 2: 角色映射与裁剪规则 -->
      <el-tab-pane label="角色映射与裁剪规则" name="trim">
        <div class="tab-toolbar">
          <el-form :inline="true" @submit.prevent>
            <el-form-item label="Profile 版本 ID">
              <el-input-number v-model="trimQuery.profileVersionId" :min="1" controls-position="right" style="width: 160px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadTrimRules">查询</el-button>
              <el-button type="success" @click="onOpenTrimForm(null)">新建裁剪规则</el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="!trimQuery.profileVersionId" type="warning" :closable="false" show-icon
            title="请先选择 Profile 版本" description="裁剪规则挂在 Profile 版本下；请到「流程模板与 Profile」打开版本管理获取版本 ID。" class="mt-10px" />
        </div>
        <el-table v-loading="loading.trim" :data="trimList" border stripe size="small">
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="阶段" prop="stage" width="120" />
          <el-table-column label="Activity 定义" prop="activityDefId" width="140" show-overflow-tooltip />
          <el-table-column label="裁剪条件" prop="trimCondition" min-width="160" show-overflow-tooltip />
          <el-table-column label="动作" prop="action" width="100">
            <template #default="{ row }"><el-tag :type="actionTagType(row.action)">{{ row.action }}</el-tag></template>
          </el-table-column>
          <el-table-column label="理由" prop="reason" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="onOpenTrimForm(row)">编辑</el-button>
              <el-button link type="danger" @click="onDeleteTrim(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-dialog v-model="trimFormVisible" :title="trimForm.id ? '编辑裁剪规则' : '新建裁剪规则'" width="560px">
          <el-form :model="trimForm" label-width="120px">
            <el-form-item label="Profile 版本 ID">
              <el-input-number v-model="trimForm.profileVersionId" :min="1" :disabled="!!trimForm.id" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="阶段"><el-input v-model="trimForm.stage" placeholder="如 SYSTEM_DESIGN；与 Activity 二选一" /></el-form-item>
            <el-form-item label="Activity 定义 ID"><el-input v-model="trimForm.activityDefId" placeholder="留空表示整阶段裁剪" /></el-form-item>
            <el-form-item label="裁剪条件"><el-input v-model="trimForm.trimCondition" placeholder="flowType=INCREMENT_RELEASE" /></el-form-item>
            <el-form-item label="动作">
              <el-select v-model="trimForm.action" style="width: 100%">
                <el-option label="跳过 SKIP" value="SKIP" />
                <el-option label="可选 OPTIONAL" value="OPTIONAL" />
                <el-option label="简化 SIMPLIFY" value="SIMPLIFY" />
              </el-select>
            </el-form-item>
            <el-form-item label="理由"><el-input v-model="trimForm.reason" type="textarea" :rows="2" /></el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="trimFormVisible = false">取消</el-button>
            <el-button type="primary" :loading="loading.trimSave" @click="onSaveTrim">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
      <!-- Tab 3: 引擎实例档案 / 失败作业 -->
      <el-tab-pane label="引擎档案与失败作业" name="runtime">
        <el-alert type="info" :closable="false" show-icon
          title="运行时聚合视图" description="引擎实例档案 / 失败作业由运行时写入；当前若无数据，属正常（未接入或样本不足），不造假。" class="mb-10px" />
        <h4 class="block-title">失败作业</h4>
        <el-table v-loading="loading.jobs" :data="failedJobList" border stripe size="small">
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="作业类型" prop="jobType" width="140" />
          <el-table-column label="引用 ID" prop="refId" width="100" />
          <el-table-column label="失败原因" prop="reason" min-width="200" show-overflow-tooltip />
          <el-table-column label="重试次数" prop="retryCount" width="90" />
          <el-table-column label="状态" prop="status" width="100" />
          <el-table-column label="下次重试" prop="nextRetryAt" width="160" />
        </el-table>
        <el-empty v-if="!loading.jobs && failedJobList.length === 0" description="暂无失败作业（样本不足/未接入）" />
      </el-tab-pane>
      <!-- Tab 4: 治理审计 -->
      <el-tab-pane label="治理审计" name="audit">
        <div class="tab-toolbar">
          <el-form :inline="true" @submit.prevent>
            <el-form-item label="动作类型">
              <el-input v-model="auditQuery.actionType" placeholder="PROFILE_CREATE / VERSION_PUBLISH ..." style="width: 220px" clearable />
            </el-form-item>
            <el-form-item label="引用 ID">
              <el-input-number v-model="auditQuery.refId" :min="0" controls-position="right" style="width: 140px" />
            </el-form-item>
            <el-form-item><el-button type="primary" @click="loadAudit">查询</el-button></el-form-item>
          </el-form>
        </div>
        <el-table v-loading="loading.audit" :data="auditList" border stripe size="small">
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="动作" prop="actionType" width="160" />
          <el-table-column label="引用 ID" prop="refId" width="90" />
          <el-table-column label="操作人" prop="operatorId" width="90" />
          <el-table-column label="变更前" prop="beforeJson" min-width="200" show-overflow-tooltip />
          <el-table-column label="变更后" prop="afterJson" min-width="200" show-overflow-tooltip />
          <el-table-column label="时间" prop="createTime" width="160" />
        </el-table>
        <el-empty v-if="!loading.audit && auditList.length === 0" description="暂无审计记录（样本不足/未接入）" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as Gov from '@/api/spk/ipd/governance'

defineOptions({ name: 'SpkIpdGovernance' })

const activeTab = ref('profiles')

// ---------- Profile 列表 ----------
const query = reactive({ pageNo: 1, pageSize: 10, flowType: '', status: '', keyword: '' })
const profileList = ref<any[]>([])
const profileTotal = ref(0)
const loading = reactive({ profiles: false, save: false, version: false, trim: false, trimSave: false, jobs: false, audit: false })

const loadProfiles = async () => {
  loading.profiles = true
  try {
    const res = await Gov.pageProfiles({ ...query })
    profileList.value = res.list || []
    profileTotal.value = res.total || 0
  } catch (e) {
    profileList.value = []
    profileTotal.value = 0
  } finally {
    loading.profiles = false
  }
}
const onPageChange = (n: number) => { query.pageNo = n; loadProfiles() }
const onResetQuery = () => { query.flowType = ''; query.status = ''; query.keyword = ''; query.pageNo = 1; loadProfiles() }

// ---------- Profile 表单 ----------
const profileFormVisible = ref(false)
const profileForm = reactive<any>({ id: null, profileCode: '', name: '', flowType: 'FULL_RELEASE', description: '', status: 'DRAFT' })
const onOpenProfileForm = (row: any) => {
  if (row) {
    Object.assign(profileForm, { id: row.id, profileCode: row.profileCode, name: row.name, flowType: row.flowType, description: row.description, status: row.status })
  } else {
    Object.assign(profileForm, { id: null, profileCode: '', name: '', flowType: 'FULL_RELEASE', description: '', status: 'DRAFT' })
  }
  profileFormVisible.value = true
}
const onSaveProfile = async () => {
  if (!profileForm.profileCode || !profileForm.name || !profileForm.flowType) {
    ElMessage.warning('编码/名称/流程类型必填'); return
  }
  loading.save = true
  try {
    if (profileForm.id) { await Gov.updateProfile({ ...profileForm }); ElMessage.success('已更新') }
    else { await Gov.createProfile({ ...profileForm }); ElMessage.success('已创建') }
    profileFormVisible.value = false; loadProfiles()
  } finally { loading.save = false }
}
const onDeleteProfile = (row: any) => {
  ElMessageBox.confirm(`确认删除模板「${row.profileCode}」？`, '提示', { type: 'warning' })
    .then(async () => { await Gov.deleteProfile(row.id); ElMessage.success('已删除'); loadProfiles() })
    .catch(() => {})
}

// ---------- 版本管理 ----------
const versionDrawerVisible = ref(false)
const currentProfile = ref<any>(null)
const versionList = ref<any[]>([])
const versionFormVisible = ref(false)
const versionForm = reactive({ snapshotJson: '', compatibilityHash: '' })
const onOpenVersions = async (row: any) => {
  currentProfile.value = row; versionDrawerVisible.value = true; await loadVersions(row.id)
}
const loadVersions = async (profileId: number) => {
  try { versionList.value = (await Gov.listVersions(profileId)) || [] } catch { versionList.value = [] }
}
const onOpenVersionForm = () => { Object.assign(versionForm, { snapshotJson: '', compatibilityHash: '' }); versionFormVisible.value = true }
const onCreateVersion = async () => {
  if (!versionForm.snapshotJson) { ElMessage.warning('档案快照 JSON 必填'); return }
  loading.version = true
  try {
    await Gov.createVersion(currentProfile.value.id, { ...versionForm })
    ElMessage.success('草稿版本已创建'); versionFormVisible.value = false; loadVersions(currentProfile.value.id)
  } finally { loading.version = false }
}
const onPublishVersion = (row: any) => {
  ElMessageBox.confirm(`确认发布版本 v${row.version}？旧已发布版本将被置为 SUPERSEDED。`, '发布', { type: 'warning' })
    .then(async () => { await Gov.publishVersion(row.id); ElMessage.success('已发布'); loadVersions(currentProfile.value.id) })
    .catch(() => {})
}
const onRollbackVersion = (row: any) => {
  ElMessageBox.confirm(`确认回滚到版本 v${row.version}？`, '回滚', { type: 'warning' })
    .then(async () => { await Gov.rollbackVersion(row.id); ElMessage.success('已回滚'); loadVersions(currentProfile.value.id) })
    .catch(() => {})
}

// ---------- 裁剪规则 ----------
const trimQuery = reactive({ profileVersionId: undefined as number | undefined })
const trimList = ref<any[]>([])
const trimFormVisible = ref(false)
const trimForm = reactive<any>({ id: null, profileVersionId: undefined, stage: '', activityDefId: '', trimCondition: '', action: 'SKIP', reason: '' })
const loadTrimRules = async () => {
  if (!trimQuery.profileVersionId) return
  loading.trim = true
  try { trimList.value = (await Gov.listTrimRules(trimQuery.profileVersionId)) || [] } catch { trimList.value = [] } finally { loading.trim = false }
}
const onOpenTrimForm = (row: any) => {
  if (row) { Object.assign(trimForm, { ...row }) }
  else { Object.assign(trimForm, { id: null, profileVersionId: trimQuery.profileVersionId, stage: '', activityDefId: '', trimCondition: '', action: 'SKIP', reason: '' }) }
  trimFormVisible.value = true
}
const onSaveTrim = async () => {
  if (!trimForm.profileVersionId || !trimForm.action) { ElMessage.warning('Profile 版本与动作必填'); return }
  loading.trimSave = true
  try { await Gov.saveTrimRule({ ...trimForm }); ElMessage.success('已保存'); trimFormVisible.value = false; loadTrimRules() } finally { loading.trimSave = false }
}
const onDeleteTrim = (row: any) => {
  ElMessageBox.confirm('确认删除该裁剪规则？', '提示', { type: 'warning' })
    .then(async () => { await Gov.deleteTrimRule(row.id); ElMessage.success('已删除'); loadTrimRules() }).catch(() => {})
}

// ---------- 运行时聚合 ----------
const failedJobList = ref<any[]>([])
const loadFailedJobs = async () => {
  loading.jobs = true
  try { failedJobList.value = (await Gov.listFailedJobs('PENDING')) || [] } catch { failedJobList.value = [] } finally { loading.jobs = false }
}
// ---------- 审计 ----------
const auditQuery = reactive({ actionType: '', refId: undefined as number | undefined })
const auditList = ref<any[]>([])
const loadAudit = async () => {
  loading.audit = true
  try {
    auditList.value = (await Gov.listAudit({ actionType: auditQuery.actionType || undefined, refId: auditQuery.refId || undefined })) || []
  } catch { auditList.value = [] } finally { loading.audit = false }
}

// ---------- 工具 ----------
const statusTagType = (s: string) => ({ PUBLISHED: 'success', DRAFT: 'info', SUPERSEDED: 'warning', DEPRECATED: 'danger' } as any)[s] || ''
const actionTagType = (a: string) => ({ SKIP: 'danger', OPTIONAL: 'warning', SIMPLIFY: 'info' } as any)[a] || ''

onMounted(() => { loadProfiles(); loadFailedJobs(); loadAudit() })
</script>

<style scoped>
.spk-ipd-governance { padding: 12px; }
.tab-toolbar { margin-bottom: 12px; }
.block-title { font-size: 14px; font-weight: 600; margin: 12px 0 8px; color: var(--el-text-color-primary); }
</style>
