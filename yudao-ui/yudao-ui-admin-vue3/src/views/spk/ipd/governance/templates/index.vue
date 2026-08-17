<!--
  SPK-OS IPD 流程治理 - BPMN 模板库（对齐原型 templates.html）
  展示 3 种 flowType 模板卡片 + 节点链路预览（阶段→门→DCP→TR）。
  "使用模板" → 跳转治理规则配置页新建对应 flowType 的 Profile。
  D1：三种 flowType 各对应一套真实 BPM 流程（spkIpdFlowFull/Increment/Issue）。
-->
<template>
  <div class="spk-ipd-gov-templates">
    <el-alert type="info" :closable="false" show-icon title="BPMN 模板库"
      description="三种 flowType 各对应一套真实 BPM 流程：FULL_RELEASE 全量六阶段 / INCREMENT_RELEASE 增量轻量 / ISSUE_RESOLUTION 问题处置四段。"
      class="mb-12px" />

    <div class="stat-row mb-12px">
      <el-card v-for="s in stats" :key="s.label" shadow="hover" class="stat-card">
        <div class="stat-label">{{ s.label }}</div>
        <div class="stat-value">{{ s.value }}</div>
      </el-card>
    </div>

    <div class="templates-grid">
      <el-card v-for="t in templates" :key="t.flowType" class="template-card"
        :class="t.official ? 'official' : 'custom'" shadow="hover">
        <div class="template-header">
          <div>
            <h3 class="template-title">{{ t.title }}</h3>
            <div class="template-version">{{ t.flowKey }} · v1.0.0</div>
          </div>
          <el-tag :type="t.official ? 'success' : 'warning'" size="small">
            {{ t.official ? '官方' : '自定义' }}
          </el-tag>
        </div>

        <div class="template-preview">
          <div v-if="schemaMap[t.flowType]" class="chain">
            <template v-for="(st, i) in schemaMap[t.flowType].stages" :key="st.stage">
              <div class="chain-node">
                <div class="chain-stage">{{ st.stage }}</div>
                <div class="chain-meta">
                  <span v-for="g in st.gates" :key="g" class="chip gate">{{ g }}</span>
                  <span v-for="d in st.dcps" :key="d" class="chip dcp">{{ d }}</span>
                  <span v-for="tr in st.trs" :key="tr" class="chip tr">{{ tr }}</span>
                </div>
              </div>
              <el-icon v-if="i < schemaMap[t.flowType].stages.length - 1" class="chain-arrow">
                <ArrowRight />
              </el-icon>
            </template>
          </div>
          <div v-else class="preview-placeholder">加载预览中…</div>
        </div>

        <div class="template-footer">
          <el-button type="primary" size="small" @click="useTemplate(t)">使用模板</el-button>
          <el-button size="small" @click="goProfiles">查看治理配置</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { getSnapshotSchema } from '@/api/spk/ipd/governance'

defineOptions({ name: 'SpkIpdGovernanceTemplates' })

const router = useRouter()

const templates = [
  { flowType: 'FULL_RELEASE', flowKey: 'spkIpdFlowFull', title: 'IPD 全量发布流程', official: true },
  { flowType: 'INCREMENT_RELEASE', flowKey: 'spkIpdFlowIncrement', title: 'IPD 增量发布流程', official: true },
  { flowType: 'ISSUE_RESOLUTION', flowKey: 'spkIpdFlowIssue', title: 'IPD 问题处置流程', official: true }
]

const stats = [
  { label: '总模板数', value: 3 },
  { label: '官方模板', value: 3 },
  { label: '活跃使用', value: 0 },
  { label: '正在运行', value: 0 }
]

const schemaMap = reactive<Record<string, any>>({})

const loadSchemas = async () => {
  await Promise.all(templates.map(async (t) => {
    try {
      schemaMap[t.flowType] = await getSnapshotSchema(t.flowType)
    } catch (e) {
      // 治理层未发布时不阻断渲染
    }
  }))
}

const useTemplate = (t: any) => {
  router.push({ path: '/spk/ipd-governance/profiles', query: { flowType: t.flowType, flowKey: t.flowKey } })
}
const goProfiles = () => router.push('/spk/ipd-governance/profiles')

onMounted(loadSchemas)
</script>

<style scoped>
.stat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.stat-card { text-align: center; }
.stat-label { color: var(--el-text-color-secondary); font-size: 13px; }
.stat-value { font-size: 22px; font-weight: 600; margin-top: 4px; }
.templates-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 16px; }
.template-card.official { border-left: 4px solid var(--el-color-success); }
.template-card.custom { border-left: 4px solid var(--el-color-warning); }
.template-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.template-title { font-size: 16px; font-weight: 600; margin: 0; }
.template-version { font-size: 12px; color: var(--el-text-color-secondary); font-family: monospace; margin-top: 4px; }
.template-preview { min-height: 140px; background: var(--el-fill-color-light); border-radius: 6px; padding: 12px; margin-bottom: 12px; overflow-x: auto; }
.chain { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.chain-node { display: flex; flex-direction: column; gap: 4px; min-width: 90px; }
.chain-stage { font-size: 12px; font-weight: 600; color: var(--el-color-primary); }
.chain-meta { display: flex; flex-wrap: wrap; gap: 4px; }
.chip { font-size: 11px; padding: 1px 6px; border-radius: 4px; background: var(--el-fill-color); }
.chip.dcp { color: var(--el-color-success); }
.chip.tr { color: var(--el-color-warning); }
.chain-arrow { color: var(--el-text-color-placeholder); }
.template-footer { display: flex; gap: 8px; }
.preview-placeholder { color: var(--el-text-color-placeholder); text-align: center; line-height: 116px; }
</style>
