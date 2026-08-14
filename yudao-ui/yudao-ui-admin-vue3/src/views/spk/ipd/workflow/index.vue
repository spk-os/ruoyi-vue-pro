<!--
  SPK-OS IPD 流程中心（Cortext-IPD §7.8）
  业务用户 Tab：发起流程 / 我的流程 / 待我审批 / 已办抄送。
  管理员 Tab：流程模板 / 运行实例（复用 RuoYi 原生 BPM 组件，不复制引擎能力）。
  我的流程 = FlowRun 列表（项目/版本/流程类型/当前阶段/门禁/健康/等待时长）。
  待我审批/已办 = 跳转决策包审批页（§7.9）。
-->
<template>
  <div class="spk-ipd-workflow" data-test="workflow-center-page">
    <el-tabs v-model="activeTab" type="card" class="mb-10px">
      <el-tab-pane label="我的流程" name="mine">
        <FlowRunList v-if="activeTab === 'mine'" />
      </el-tab-pane>
      <el-tab-pane label="待我审批" name="pending" data-test="workflow-tab-pending">
        <div class="empty-tip">
          <el-button type="primary" @click="goApproval('todo')">
            <Icon icon="ep:check" class="mr-4px" />前往审批决策包
          </el-button>
          <span class="text-gray-400 ml-12px">审批与决策包在「审批决策」页面处理（§7.9）</span>
        </div>
      </el-tab-pane>
      <el-tab-pane label="已办 / 抄送" name="done">
        <div class="empty-tip">
          <el-button @click="goApproval('done')">
            <Icon icon="ep:finished" class="mr-4px" />查看已办
          </el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="发起流程" name="start">
        <div class="empty-tip">
          <el-button type="primary" @click="goProjects">
            <Icon icon="ep:plus" class="mr-4px" />从项目发起
          </el-button>
          <span class="text-gray-400 ml-12px">在项目详情选择版本 → 创建 FlowRun（§7.4 向导）</span>
        </div>
      </el-tab-pane>
      <el-tab-pane label="流程模板（管理员）" name="admin">
        <div class="empty-tip">
          <el-button type="primary" @click="goNative('/bpm/model')">
            <Icon icon="ep:set-up" class="mr-4px" />流程模型管理
          </el-button>
          <el-button @click="goNative('/bpm/instance')">
            <Icon icon="ep:document" class="mr-4px" />运行实例
          </el-button>
          <el-button @click="goNative('/bpm/form')">
            <Icon icon="ep:form" class="mr-4px" />业务表单
          </el-button>
          <el-button @click="goNative('/bpm/user-group')">
            <Icon icon="ep:user" class="mr-4px" />用户/角色映射
          </el-button>
          <p class="text-gray-400 text-12px mt-12px">
            流程模型编辑器与实例管理复用 RuoYi 原生 BPM 组件；SPK 流程档案校验通过后才出现在创建向导。
          </p>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import FlowRunList from './FlowRunList.vue'

defineOptions({ name: 'SpkIpdWorkflow' })

const router = useRouter()
const activeTab = ref('mine')

const goApproval = (type: string) =>
  router.push({ path: '/spk/ipd-approval', query: { type } })
const goProjects = () => router.push('/spk/ipd/projects')
const goNative = (path: string) => router.push(path)
</script>

<style scoped lang="scss">
.empty-tip {
  padding: 32px;
  text-align: center;
  color: var(--el-text-color-secondary);
}
</style>
