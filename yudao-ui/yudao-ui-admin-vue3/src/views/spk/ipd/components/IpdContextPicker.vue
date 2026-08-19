<!--
  IpdContextPicker — SPK-OS Cortext-IPD 全局上下文选择器（设计文档 §3.1）
  位置：所有 IPD 页面页头面包屑下方固定。
  S1 落地：项目级一级选择器贯通 7 个主菜单页；版本/流程二级选择器后续切片接入。
  规则：项目必选（研发总览允许"全部项目"），上下文写 URL query(projectId) 支持刷新/收藏/分享；
  最近 5 个上下文存 localStorage，默认恢复最近有效上下文；切换项目清空不属于它的版本/流程（二级接入后生效）。
-->
<template>
  <div class="ipd-ctx">
    <div class="ipd-ctx__select">
      <label class="ipd-ctx__label">项目</label>
      <el-select
        v-model="projectId"
        :loading="projectLoading"
        :placeholder="allowAll ? '全部项目（全局）' : '请选择项目'"
        filterable
        class="ipd-ctx__proj"
        @change="onProjectChange"
      >
        <el-option v-if="allowAll" :value="undefined" label="全部项目（全局）" />
        <el-option v-for="p in projects" :key="p.id" :value="p.id" :label="p.name" />
      </el-select>
    </div>

    <!-- 版本 / 流程二级选择器：后续切片接入，此处保持占位不阻断布局 -->
    <div v-if="showLevel2" class="ipd-ctx__select ipd-ctx__select--disabled">
      <label class="ipd-ctx__label">版本</label>
      <el-select v-model="versionId" placeholder="全版本范围" disabled class="ipd-ctx__ver">
        <el-option :value="undefined" label="全版本范围" />
      </el-select>
    </div>
    <div v-if="showLevel2" class="ipd-ctx__select ipd-ctx__select--disabled">
      <label class="ipd-ctx__label">流程</label>
      <el-select v-model="flowRunId" placeholder="全 IPD 流程" disabled class="ipd-ctx__flow">
        <el-option :value="undefined" label="全 IPD 流程" />
      </el-select>
    </div>

    <div class="ipd-ctx__refresh">
      <span class="ipd-ctx__fresh">更新于 {{ freshLabel }}</span>
      <el-button :icon="Refresh" size="small" @click="onRefresh">刷新</el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import * as IpdBusinessApi from '@/api/spk/ipd/business'

defineOptions({ name: 'IpdContextPicker' })

const props = withDefaults(
  defineProps<{
    modelValue?: number | undefined // projectId（v-model）
    allowAll?: boolean // 是否允许"全部项目"（研发总览 true，其余页可 false）
    showLevel2?: boolean // 是否显示版本/流程二级占位（后续切片）
    loading?: boolean
  }>(),
  { modelValue: undefined, allowAll: true, showLevel2: false, loading: false }
)

const emit = defineEmits<{
  (e: 'update:modelValue', projectId: number | undefined): void
  (e: 'refresh'): void
}>()

const route = useRoute()
const router = useRouter()

const projects = ref<Array<{ id: number; name: string }>>([])
const projectLoading = ref(false)

const projectId = ref<number | undefined>(props.modelValue)
const versionId = ref<number | undefined>(undefined)
const flowRunId = ref<number | undefined>(undefined)

const RECENT_KEY = 'spk-ipd-recent-contexts'
const freshMinutes = ref<number>(2)
const freshLabel = computed(() => {
  if (freshMinutes.value < 1) return '刚刚'
  if (freshMinutes.value < 60) return `${freshMinutes.value} 分钟前`
  const h = Math.floor(freshMinutes.value / 60)
  return `${h} 小时前`
})

/** 载入项目列表（真实接口 /spk/ipd/projects） */
const loadProjects = async () => {
  projectLoading.value = true
  try {
    const res = await IpdBusinessApi.getPage({ pageNo: 1, pageSize: 200 })
    projects.value = (res.list || []).map((p: any) => ({ id: p.id, name: p.name }))
  } finally {
    projectLoading.value = false
  }
}

/** 切换项目：清空不属于它的版本/流程（二级接入后），写 URL query，存最近上下文 */
const onProjectChange = (val: number | undefined) => {
  versionId.value = undefined
  flowRunId.value = undefined
  emit('update:modelValue', val)
  freshMinutes.value = 0
  // 写 URL query，支持刷新/分享
  router.replace({ query: { ...route.query, projectId: val ?? '', view: route.query.view } })
  pushRecent(val)
}

const pushRecent = (pid: number | undefined) => {
  try {
    const raw = localStorage.getItem(RECENT_KEY)
    const arr: Array<number | ''> = raw ? JSON.parse(raw) : []
    const key = pid ?? ''
    const next = [key, ...arr.filter((x) => x !== key)].slice(0, 5)
    localStorage.setItem(RECENT_KEY, JSON.stringify(next))
  } catch {
    /* localStorage 不可用时静默降级 */
  }
}

/** 恢复最近有效上下文（URL query 优先，其次 localStorage） */
const restoreContext = () => {
  const q = route.query.projectId
  if (q !== undefined && q !== '' && q !== null) {
    const pid = Number(q)
    if (!Number.isNaN(pid)) {
      projectId.value = pid
      emit('update:modelValue', pid)
      return
    }
  }
  // 研发总览允许"全部"，不强行回填；其余页无选中时尝试最近上下文
  if (!props.allowAll) {
    try {
      const raw = localStorage.getItem(RECENT_KEY)
      const arr: Array<number | ''> = raw ? JSON.parse(raw) : []
      const recent = arr.find((x) => x !== '' && typeof x === 'number') as number | undefined
      if (recent && projects.value.some((p) => p.id === recent)) {
        projectId.value = recent
        emit('update:modelValue', recent)
        router.replace({ query: { ...route.query, projectId: String(recent) } })
      }
    } catch {
      /* 静默 */
    }
  }
}

const onRefresh = () => {
  freshMinutes.value = 0
  emit('refresh')
}

// 每分钟自增新鲜度计数（仅展示，不触发数据请求）
let timer: ReturnType<typeof setInterval> | undefined
onMounted(async () => {
  await loadProjects()
  restoreContext()
  timer = setInterval(() => {
    freshMinutes.value += 1
  }, 60_000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
// modelValue 外部变更同步回内部（如父页直接置空）
watch(
  () => props.modelValue,
  (val) => {
    if (val !== projectId.value) projectId.value = val
  }
)
</script>

<style scoped>
.ipd-ctx {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 10px 24px;
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}
.ipd-ctx__select {
  display: flex;
  align-items: center;
  gap: 6px;
}
.ipd-ctx__select--disabled {
  opacity: 0.6;
}
.ipd-ctx__label {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
}
.ipd-ctx__proj {
  width: 220px;
}
.ipd-ctx__ver,
.ipd-ctx__flow {
  width: 160px;
}
.ipd-ctx__refresh {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #909399;
  font-size: 12px;
}
.ipd-ctx__fresh {
  white-space: nowrap;
}
</style>
