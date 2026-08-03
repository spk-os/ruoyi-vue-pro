<!--
  SPK-OS IPD Cockpit 监控台（Cortext-IPD §9 / §11）
  三 Tab：泳道图 / Activity 详情 / Agent 负载。
  泳道图按流程实例聚合阶段→Activity 卡片，状态色编码；
  Activity 详情展示三件套（ContextManifest/ArtifactManifest/RunReceipt）+ VerificationReceipt + 证据链哈希；
  Agent 负载看 12 Lead + 3 Verifier 的 running 合同数与死信。
-->
<template>
  <div class="spk-ipd-cockpit">
    <el-tabs v-model="activeTab" type="card" class="mb-10px">
      <el-tab-pane label="泳道图" name="swimlane">
        <Swimlane v-if="activeTab === 'swimlane'" @show-detail="onShowDetail" />
      </el-tab-pane>
      <el-tab-pane label="Activity 详情" name="detail">
        <ActivityDetail v-if="activeTab === 'detail'" :activity-run-id="detailRunId" />
      </el-tab-pane>
      <el-tab-pane label="Agent 负载" name="agentLoad">
        <AgentLoad v-if="activeTab === 'agentLoad'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Swimlane from './Swimlane.vue'
import ActivityDetail from './ActivityDetail.vue'
import AgentLoad from './AgentLoad.vue'

defineOptions({ name: 'SpkIpdCockpit' })

const activeTab = ref('swimlane')
const detailRunId = ref<string>('')

const onShowDetail = (activityRunId: string) => {
  detailRunId.value = activityRunId
  activeTab.value = 'detail'
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
