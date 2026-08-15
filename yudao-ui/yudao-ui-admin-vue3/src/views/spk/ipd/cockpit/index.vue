<!--
  SPK-OS IPD Cockpit 监控台（Cortext-IPD §9 / §11）
  主视图：流程图。点 Activity 卡片 → 原地弹右侧大抽屉，把该节点全部信息
  （contract/三件套/验证/证据链/Plane 需求/Omnigent 会话）聚合铺开，不再切 tab。
  Agent 负载单独 tab。
-->
<template>
  <div class="spk-ipd-cockpit">
    <!-- C-14：未携带 flowRunId/processInstanceId 进入时显式提示，不静默自动载入全局最新 -->
    <el-alert
      v-if="!externalPid"
      type="info"
      :closable="false"
      show-icon
      title="未指定 FlowRun"
      description="请从「项目监控」选择一行运行后进入；或在下方流程图输入 processInstanceId / 点「载入最新」。"
      class="mb-10px"
    />
    <el-tabs v-model="activeTab" type="card" class="mb-10px">
      <el-tab-pane label="流程图" name="swimlane">
        <Swimlane v-if="activeTab === 'swimlane'" :external-pid="externalPid" @show-detail="onShowDetail" />
      </el-tab-pane>
      <el-tab-pane label="Agent 负载" name="agentLoad">
        <AgentLoad v-if="activeTab === 'agentLoad'" />
      </el-tab-pane>
    </el-tabs>

    <!-- 节点聚合详情抽屉：点卡片即弹，所有信息原地铺开 -->
    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      direction="rtl"
      size="72%"
      :destroy-on-close="true"
    >
      <ActivityDetail v-if="drawerVisible" :activity-run-id="detailRunId" embedded />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import Swimlane from './Swimlane.vue'
import ActivityDetail from './ActivityDetail.vue'
import AgentLoad from './AgentLoad.vue'

defineOptions({ name: 'SpkIpdCockpit' })

const route = useRoute()
// 监控行带入的流程实例编号（C-14：不得丢 ID）。优先 processInstanceId，回退 flowRunId。
const externalPid = computed(() => {
  const pid = route.query.processInstanceId as string | undefined
  if (pid) return pid
  const fr = route.query.flowRunId as string | undefined
  return fr || ''
})

const activeTab = ref('swimlane')
const drawerVisible = ref(false)
const detailRunId = ref<string>('')

const drawerTitle = computed(() =>
  detailRunId.value ? `节点详情 · ${detailRunId.value}` : '节点详情'
)

const onShowDetail = (activityRunId: string) => {
  detailRunId.value = activityRunId
  drawerVisible.value = true
}
</script>

<style scoped>
.spk-ipd-cockpit {
  padding: 12px;
}
.mb-10px {
  margin-bottom: 10px;
}
</style>
