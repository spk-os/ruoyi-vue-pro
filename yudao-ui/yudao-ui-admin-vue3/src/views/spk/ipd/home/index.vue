<!--
  SPK-OS 研发总览 Shell（UCD v4 §4.1）
  原型「研发驾驶舱」，菜单「研发总览」。v4.0：4 视图→3 视图，运行监控并入「监控与证据」页。
  路由契约 /spk/ipd-home?view= —— query 实现，规避 yudao 嵌套菜单 404 坑，URL 可独立恢复。
  view ∈ overview(全景总览)/actions(行动中心)/analytics(交付分析)。
  页头含全局上下文选择器（项目级，研发总览允许"全部项目"）+ 行动引导条（§3.2，真实 overview 计数）。
-->
<template>
  <div class="spk-home">
    <!-- 全局上下文选择器（§3.1，项目级，S1 落地） -->
    <IpdContextPicker v-model="projectId" :allow-all="true" @refresh="onCtxRefresh" />

    <!-- 行动引导条（§3.2，真实计数；无待办/阻断时隐藏） -->
    <div v-if="guideVisible" class="spk-home__guide" :class="guideTone" @click="goApprovals">
      <span class="spk-home__guide-dot" />
      <span>{{ guideText }}</span>
      <span class="spk-home__guide-link">去处理 →</span>
    </div>

    <!-- 页头 -->
    <div class="spk-home__header">
      <div class="spk-home__titlewrap">
        <div class="spk-home__title">研发总览</div>
        <div class="spk-home__subtitle">{{ activeMeta.subtitle }}</div>
      </div>
      <div class="spk-home__header-actions">
        <SpkFreshness :minutes="2" />
      </div>
    </div>

    <div class="spk-home__body">
      <!-- 侧栏 -->
      <aside class="spk-home__sidebar">
        <div class="spk-side-group">
          <div class="spk-side-group__title">研发总览</div>
          <router-link
            v-for="v in VIEWS"
            :key="v.key"
            :to="{ name: 'SpkIpdHome', query: { view: v.key } }"
            class="spk-side-link"
            :class="{ 'spk-side-link--active': activeView === v.key }"
          >
            <Icon :icon="v.icon" class="spk-side-link__icon" />
            <span class="spk-side-link__text">{{ v.label }}</span>
            <el-badge v-if="v.badge" :value="v.badge" type="danger" class="spk-side-link__badge" />
          </router-link>
        </div>

        <div class="spk-side-group">
          <div class="spk-side-group__title">快捷入口</div>
          <router-link :to="{ name: 'SpkIpdApproval' }" class="spk-side-link">
            <Icon icon="ep:document-checked" class="spk-side-link__icon" />
            <span class="spk-side-link__text">我的审批</span>
          </router-link>
          <router-link :to="{ name: 'SpkIpdProjects' }" class="spk-side-link">
            <Icon icon="ep:folder-opened" class="spk-side-link__icon" />
            <span class="spk-side-link__text">项目空间</span>
          </router-link>
          <router-link :to="{ name: 'SpkIpdMonitor' }" class="spk-side-link">
            <Icon icon="ep:monitor" class="spk-side-link__icon" />
            <span class="spk-side-link__text">监控与证据</span>
          </router-link>
          <router-link :to="{ name: 'SpkIpdTeam' }" class="spk-side-link">
            <Icon icon="ep:user-filled" class="spk-side-link__icon" />
            <span class="spk-side-link__text">团队与智能体</span>
          </router-link>
        </div>
      </aside>

      <!-- 视图内容 -->
      <main class="spk-home__content">
        <component :is="activeComp" v-model:fresh-minutes="freshMinutes" :project-id="projectId" />
      </main>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTitle } from '@vueuse/core'
import IpdContextPicker from '../components/IpdContextPicker.vue'
import SpkFreshness from './components/SpkFreshness.vue'
import OverviewView from './overview.vue'
import ActionsView from './actions.vue'
import AnalyticsView from './analytics.vue'
import { useOverviewData } from './composables/useOverviewData'

defineOptions({ name: 'SpkIpdHome' })

const route = useRoute()
const router = useRouter()

// v4.0：3 视图（运行监控并入「监控与证据」页）
const VIEWS = [
  { key: 'overview', label: '全景总览', icon: 'ep:data-board', badge: 0 },
  { key: 'actions', label: '行动中心', icon: 'ep:promotion', badge: 0 },
  { key: 'analytics', label: '交付分析', icon: 'ep:data-analysis', badge: 0 }
] as const

const VIEW_META: Record<string, { subtitle: string; comp: any }> = {
  overview: { subtitle: '所有项目的综合健康度、路线图与交付动态', comp: OverviewView },
  actions: { subtitle: '您的下一步行动、团队协作与受控命令执行', comp: ActionsView },
  analytics: { subtitle: '组合范围内的流程效率、质量指标及智能体效能', comp: AnalyticsView }
}

const activeView = computed<string>(() => {
  const v = route.query.view as string
  return VIEW_META[v] ? v : 'overview'
})

const activeMeta = computed(() => VIEW_META[activeView.value])
const activeComp = computed(() => VIEW_META[activeView.value].comp)

// 各视图上报的数据新鲜度（分钟），驱动页头 freshness
const freshMinutes = ref<number>(2)
watch(activeView, () => { freshMinutes.value = 2 })

// 标题随视图切换
useTitle(computed(() => `研发总览 · ${VIEWS.find((v) => v.key === activeView.value)?.label || ''}`))

// 进入时若无 view 参数，补默认 query（不触发历史污染）
if (!route.query.view) {
  router.replace({ name: 'SpkIpdHome', query: { view: 'overview' } })
}

// 全局上下文（项目级）：从 URL query 恢复
const projectId = computed<number | undefined>({
  get: () => (route.query.projectId ? Number(route.query.projectId) : undefined),
  set: (val) => {
    router.replace({ query: { ...route.query, projectId: val ? String(val) : '', view: route.query.view } })
  }
})
const onCtxRefresh = () => {
  freshMinutes.value = 0
}

// 行动引导条（§3.2）：真实 overview 计数驱动
const { summary, load: loadOverview } = useOverviewData()
const guideText = computed(() => {
  const a = summary.value.attentionCount
  const b = summary.value.blockedCount
  if (a && b) return `${a} 项待审批 · ${b} 项阻断`
  if (a) return `${a} 项待审批`
  if (b) return `${b} 项阻断`
  return ''
})
const guideVisible = computed(() => summary.value.attentionCount > 0 || summary.value.blockedCount > 0)
const guideTone = computed(() => (summary.value.blockedCount > 0 ? 'spk-home__guide--alert' : 'spk-home__guide--warn'))
const goApprovals = () => router.push({ name: 'SpkIpdApproval' })

onMounted(() => {
  loadOverview()
})
</script>

<style scoped>
.spk-home {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f6f8fa;
}
.spk-home__guide {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 24px;
  font-size: 13px;
  cursor: pointer;
}
.spk-home__guide--warn {
  background: #fdf6ec;
  border-bottom: 1px solid #faecd8;
  color: #e6a23c;
}
.spk-home__guide--alert {
  background: #fef0f0;
  border-bottom: 1px solid #fde2e2;
  color: #f56c6c;
}
.spk-home__guide-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}
.spk-home__guide-link {
  margin-left: auto;
  font-weight: 500;
}
.spk-home__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding: 16px 20px 12px;
  background: #fff;
  border-bottom: 1px solid #d0d7de;
}
.spk-home__title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2328;
  line-height: 1.2;
}
.spk-home__subtitle {
  font-size: 13px;
  color: #656d76;
  margin-top: 4px;
}
.spk-home__body {
  flex: 1;
  display: flex;
  min-height: 0;
}
.spk-home__sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #d0d7de;
  padding: 12px 8px;
  overflow-y: auto;
}
.spk-home__content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}
.spk-side-group {
  margin-bottom: 16px;
}
.spk-side-group__title {
  font-size: 11px;
  font-weight: 700;
  color: #8c959f;
  letter-spacing: 0.04em;
  padding: 4px 8px;
  text-transform: uppercase;
}
.spk-side-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border-radius: 6px;
  font-size: 13px;
  color: #1f2328;
  text-decoration: none;
  transition: background 0.15s;
}
.spk-side-link:hover {
  background: #f6f8fa;
  text-decoration: none;
}
.spk-side-link--active {
  background: #ddf4ff;
  color: #0550ae;
  font-weight: 600;
}
.spk-side-link__icon {
  font-size: 14px;
  flex-shrink: 0;
}
.spk-side-link__text {
  flex: 1;
}
.spk-side-link__badge {
  flex-shrink: 0;
}
</style>
