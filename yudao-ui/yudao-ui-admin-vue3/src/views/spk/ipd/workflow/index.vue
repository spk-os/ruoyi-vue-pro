<!--
  SPK-OS IPD 流程中心 → 「我的审批」重定向占位（UCD v4 §4.7：审批中心+流程中心合并为单页）
  原 workflow 五 Tab 能力已全部并入 approval 五 Tab，零能力丢失：
    mine(我的流程)→runs / pending(待我审批)→todo / done→done / start(发起流程)→start / admin(流程模板)→template
  保留本路由文件使直接访问 /spk/ipd-workflow 不 404，自动 replace 到合并页并映射 tab。
-->
<template>
  <div class="workflow-redirect">
    <el-empty description="流程中心已合并至「我的审批」">
      <template #description>
        <p>流程中心已合并至「我的审批」</p>
        <p class="text-xs text-gray-400">正在跳转…如未自动跳转请点击下方按钮</p>
      </template>
      <el-button type="primary" @click="redirect">前往我的审批</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineOptions({ name: 'SpkIpdWorkflow' })

const route = useRoute()
const router = useRouter()

// workflow tab 名 → approval tab 名映射（§4.7 合并零能力丢失）
const TAB_MAP: Record<string, string> = {
  mine: 'runs',
  pending: 'todo',
  done: 'done',
  start: 'start',
  admin: 'template'
}

const redirect = () => {
  const srcTab = (route.query.tab as string) || 'mine'
  const targetTab = TAB_MAP[srcTab] || 'todo'
  const { tab: _omit, ...rest } = route.query
  router.replace({ path: '/spk/ipd-approval', query: { ...rest, tab: targetTab } })
}

let timer: any
onMounted(() => { timer = setTimeout(redirect, 200) })
onUnmounted(() => timer && clearTimeout(timer))
</script>

<style scoped lang="scss">
.workflow-redirect { padding: 48px 0; }
</style>
