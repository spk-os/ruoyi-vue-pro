<!--
  SPK-OS IPD 流程治理 - 模板版本管理（对齐原型 versions.html）
  左：Profile 选择 + 版本时间线；右：版本详情 + 发布/回滚 + 兼容性信息。
  D1：发布时固化 processDefinitionKey 进版本，回滚可定位历史流程定义。
-->
<template>
  <div class="spk-ipd-gov-versions">
    <el-alert type="info" :closable="false" show-icon title="模板版本管理"
      description="跟踪 Profile 发布历史、流程定义绑定与兼容性。发布时固化 BPM key 进版本；回滚重新置为当前已发布。"
      class="mb-12px" />

    <div class="versions-layout">
      <!-- 左：Profile + 版本时间线 -->
      <div class="version-list-panel">
        <div class="version-list-header">
          <span>Profile 列表</span>
        </div>
        <el-select v-model="selectedProfileId" placeholder="选择 Profile" size="small" class="mb-12px"
          filterable @change="onProfileChange">
          <el-option v-for="p in profiles" :key="p.id" :label="`${p.name} (v${p.currentVersion})`"
            :value="p.id" />
        </el-select>
        <div class="version-timeline">
          <div v-for="v in versions" :key="v.id" class="version-item"
            :class="{ active: selectedVersionId === v.id }" @click="selectVersion(v)">
            <div class="version-item-name">
              <span>v{{ v.version }}</span>
              <el-tag size="small" :type="statusType(v.status)">{{ v.status }}</el-tag>
            </div>
            <div class="version-item-meta">{{ formatTime(v.publishedAt) || '未发布' }}</div>
            <div class="version-item-meta">key: {{ v.processDefinitionKey || '—' }}</div>
          </div>
          <el-empty v-if="!versions.length" description="暂无版本" />
        </div>
      </div>

      <!-- 右：版本详情 -->
      <div class="version-detail-panel">
        <template v-if="selectedVersion">
          <div class="detail-title">
            <span>v{{ selectedVersion.version }} 详情</span>
            <div>
              <el-button size="small" type="success" :disabled="selectedVersion.status === 'PUBLISHED'"
                @click="onPublish">发布</el-button>
              <el-button size="small" :disabled="!['PUBLISHED', 'SUPERSEDED'].includes(selectedVersion.status)"
                @click="onRollback">回滚到此</el-button>
            </div>
          </div>

          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="版本号">v{{ selectedVersion.version }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag size="small" :type="statusType(selectedVersion.status)">{{ selectedVersion.status }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="绑定 BPM key">{{ selectedVersion.processDefinitionKey || '—' }}</el-descriptions-item>
            <el-descriptions-item label="兼容性哈希">{{ selectedVersion.compatibilityHash || '—' }}</el-descriptions-item>
            <el-descriptions-item label="发布人">{{ selectedVersion.publishedBy || '—' }}</el-descriptions-item>
            <el-descriptions-item label="发布时间">{{ formatTime(selectedVersion.publishedAt) || '—' }}</el-descriptions-item>
            <el-descriptions-item label="前序版本">{{ selectedVersion.supersedesVersionId || '—' }}</el-descriptions-item>
          </el-descriptions>

          <div class="detail-section-title">Snapshot（治理快照）</div>
          <el-input v-model="snapshotView" type="textarea" :rows="10" readonly />
        </template>
        <el-empty v-else description="请从左侧选择版本" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  pageProfiles, listVersions, publishVersion, rollbackVersion
} from '@/api/spk/ipd/governance'

defineOptions({ name: 'SpkIpdGovernanceVersions' })

const profiles = ref<any[]>([])
const selectedProfileId = ref<number | null>(null)
const versions = ref<any[]>([])
const selectedVersionId = ref<number | null>(null)

const selectedVersion = computed(() => versions.value.find((v: any) => v.id === selectedVersionId.value))
const snapshotView = computed(() => {
  const s = selectedVersion.value?.snapshotJson
  try { return s ? JSON.stringify(JSON.parse(s), null, 2) : '—' } catch { return s || '—' }
})

const statusType = (s: string) => ({ PUBLISHED: 'success', DRAFT: 'info', SUPERSEDED: 'warning', DEPRECATED: 'danger' } as any)[s] || 'info'
const formatTime = (t: string) => t ? t.replace('T', ' ').substring(0, 19) : ''

const loadProfiles = async () => {
  const res = await pageProfiles({ pageNo: 1, pageSize: 50 })
  profiles.value = res.list || []
  if (profiles.value.length) {
    selectedProfileId.value = profiles.value[0].id
    await onProfileChange(profiles.value[0].id)
  }
}

const onProfileChange = async (profileId: number) => {
  versions.value = await listVersions(profileId)
  selectedVersionId.value = versions.value[0]?.id || null
}

const selectVersion = (v: any) => { selectedVersionId.value = v.id }

const onPublish = async () => {
  await publishVersion(selectedVersionId.value!)
  ElMessage.success('版本已发布')
  await onProfileChange(selectedProfileId.value!)
}

const onRollback = async () => {
  await rollbackVersion(selectedVersionId.value!)
  ElMessage.success('版本已回滚')
  await onProfileChange(selectedProfileId.value!)
}

onMounted(loadProfiles)
</script>

<style scoped>
.versions-layout { display: grid; grid-template-columns: 320px 1fr; gap: 16px; }
.version-list-panel { display: flex; flex-direction: column; }
.version-list-header { font-weight: 600; margin-bottom: 8px; }
.version-timeline { display: flex; flex-direction: column; gap: 8px; }
.version-item { padding: 10px; border: 1px solid var(--el-border-color); border-radius: 6px; cursor: pointer; }
.version-item.active { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.version-item-name { display: flex; justify-content: space-between; align-items: center; font-weight: 600; }
.version-item-meta { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
.version-detail-panel { min-height: 400px; }
.detail-title { display: flex; justify-content: space-between; align-items: center; font-weight: 600; margin-bottom: 12px; }
.detail-section-title { font-weight: 600; margin: 16px 0 8px; }
</style>
