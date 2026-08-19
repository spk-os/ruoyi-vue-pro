<template>
  <el-tabs v-model="activeTab" class="agent-tabs">
    <!-- Tab1 智能体定义（抽到 AgentDefPanel，团队页 Agent Tab 复用同一组件，避免两处维护） -->
    <el-tab-pane label="智能体定义" name="def">
      <AgentDefPanel />
    </el-tab-pane>

    <!-- Tab2 智能体小队（原编队页降为组件，设计 §8 line549） -->
    <el-tab-pane label="智能体小队" name="squad" lazy>
      <component :is="SquadPage" />
    </el-tab-pane>

    <!-- Tab3 运行负载（§7.1 第 3 视图，真实聚合不造假） -->
    <el-tab-pane label="运行负载" name="load" lazy>
      <AgentLoadPanel />
    </el-tab-pane>
  </el-tabs>
</template>

<script lang="ts" setup>
import { useRoute } from 'vue-router'
import AgentDefPanel from './AgentDefPanel.vue'
import AgentLoadPanel from './AgentLoadPanel.vue'
import { defineAsyncComponent } from 'vue'

defineOptions({ name: 'SpkAgent' })

// 异步加载编队页（降为 Tab2 组件，避免拆两处维护）
const SquadPage = defineAsyncComponent(() => import('../squad/index.vue'))

const route = useRoute()

const activeTab = ref<'def' | 'squad' | 'load'>('def')
// 从拓扑大屏下钻带 tab=load 进入，自动切到运行负载 tab
if (route.query.tab === 'load') {
  activeTab.value = 'load'
}
</script>

<style lang="scss" scoped>
.agent-tabs {
  :deep(.el-tabs__content) {
    padding-top: 0;
  }
}
</style>
