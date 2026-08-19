<!--
  SPK-OS IPD「我的审批」（UCD v4 §4.7：审批中心 + 流程中心合并为单页五 Tab）
  ① 待我审批 ② 已办/抄送 ③ 我的流程(FlowRunList) ④ 发起流程(StartFlowWizard 三步引导) ⑤ 流程模板配置(链接原生 BPM)
  审批项底层复用 BPM 待办/已办分页；点「决策」打开右侧抽屉加载 DecisionPackage 六区（§7.9）。
  全真实后端聚合，无任何写死数据。支持 ?tab=&type= 路由参数（供 workflow 重定向与其他页跳转）。
-->
<template>
  <div class="spk-ipd-approval">
    <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">
      <!-- ① 待我审批 -->
      <el-tab-pane name="todo">
        <template #label><Icon class="mr-4px" icon="ep:bell" />待我审批 {{ todoCountBadge }}</template>
        <ApprovalList :type="'todo'" ref="todoListRef" @update:total="(n:number) => todoCount = n" @open-decision="openDecision" />
      </el-tab-pane>

      <!-- ② 已办/抄送 -->
      <el-tab-pane name="done" lazy>
        <template #label><Icon class="mr-4px" icon="ep:finished" />已办 / 抄送</template>
        <ApprovalList :type="'done'" @open-decision="openDecision" />
      </el-tab-pane>

      <!-- ③ 我的流程 -->
      <el-tab-pane name="runs" lazy>
        <template #label><Icon class="mr-4px" icon="ep:list" />我的流程</template>
        <FlowRunList />
      </el-tab-pane>

      <!-- ④ 发起流程 -->
      <el-tab-pane name="start" lazy>
        <template #label><Icon class="mr-4px" icon="ep:plus" />发起流程</template>
        <StartFlowWizard />
      </el-tab-pane>

      <!-- ⑤ 流程模板配置（嵌入 governance/flow-config 四 Tab：工作流模板/交付目录/阶段产物/skill 与环境；v3 能力全保留） -->
      <el-tab-pane name="template" lazy>
        <template #label><Icon class="mr-4px" icon="ep:set-up" />流程模板</template>
        <FlowConfig />
        <div class="template-links mt-12px">
          <el-card shadow="never">
            <div class="text-sm text-gray-500 mb-8px">BPM 引擎高级管理（流程模型编辑/运行实例/业务表单/用户角色映射）—— 复用 RuoYi 原生 BPM，不复制引擎能力。</div>
            <div class="link-grid">
              <el-button type="primary" @click="goNative('/bpm/model')"><Icon icon="ep:set-up" class="mr-4px" />流程模型管理</el-button>
              <el-button @click="goNative('/bpm/instance')"><Icon icon="ep:document" class="mr-4px" />运行实例</el-button>
              <el-button @click="goNative('/bpm/form')"><Icon icon="ep:form" class="mr-4px" />业务表单</el-button>
              <el-button @click="goNative('/bpm/user-group')"><Icon icon="ep:user" class="mr-4px" />用户/角色映射</el-button>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" direction="rtl" size="80%" :destroy-on-close="true">
      <DecisionPackage v-if="drawerVisible" :task-id="currentTaskId" @decided="onDecided" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ApprovalList from './ApprovalList.vue'
import DecisionPackage from './DecisionPackage.vue'
import FlowRunList from '../workflow/FlowRunList.vue'
import StartFlowWizard from './StartFlowWizard.vue'
import FlowConfig from '../governance/flow-config/index.vue'
import type { SpkIpdApprovalTaskRespVO } from '@/api/spk/ipd/approval'

defineOptions({ name: 'SpkIpdApproval' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

// Tab 由路由 ?tab= 驱动（默认 todo）；支持 ?type=done 跳已办（workflow 重定向兼容）
const activeTab = ref<string>((route.query.tab as string) || (route.query.type as string) || 'todo')
const todoListRef = ref<InstanceType<typeof ApprovalList> | null>(null)
const todoCount = ref(0)
const todoCountBadge = computed(() => (todoCount.value ? `(${todoCount.value})` : ''))

const onTabChange = (name: string | number) => {
  router.replace({ path: route.path, query: { ...route.query, tab: String(name) } })
}

// 决策包抽屉
const drawerVisible = ref(false)
const drawerTitle = ref('')
const currentTaskId = ref('')
const openDecision = (row: SpkIpdApprovalTaskRespVO) => {
  currentTaskId.value = row.taskId
  drawerTitle.value = `决策包 - ${row.name || ''}（${row.projectName || '-'} / ${row.versionLabel || '-'}）`
  drawerVisible.value = true
}
const onDecided = () => {
  message.success('决策已提交')
  drawerVisible.value = false
  // 决策后刷新当前 Tab 列表（待办项可能消失/已办项新增）
  if (activeTab.value === 'todo') todoListRef.value?.refresh?.()
}

const goNative = (path: string) => router.push(path)

onMounted(() => {})
</script>

<style lang="scss" scoped>
.spk-ipd-approval {
  :deep(.el-tabs__content) { padding-top: 0; }
}
.template-links .link-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
