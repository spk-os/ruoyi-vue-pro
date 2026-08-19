<!--
  研发驾驶舱 · 智能协同（设计文档 §4.2 / 原型 actions.html）
  回答"我现在该做什么"。四区：AI 交付简报 + 命令栏(解析→预览→确认) + 行动收件箱 + 团队行动板。
  数据：workbench API（inbox/board/snapshot + commands parse/execute）。动作风险分级 L0-L3（§4.2）。
-->
<template>
  <div v-loading="loading" class="spk-actions">
    <!-- AI 交付简报 -->
    <div class="spk-briefing">
      <div class="spk-briefing__head">
        <span class="spk-briefing__icon">AI</span>
        <div>
          <div class="spk-briefing__title">交付简报</div>
          <div class="spk-briefing__sub">基于当前权限与上下文的今日变化、阻断原因、即将到期 Gate</div>
        </div>
        <div class="spk-briefing__fresh"><SpkFreshness :minutes="2" /></div>
      </div>
      <div class="spk-briefing__body">
        <div v-if="!briefing.length" class="spk-empty-inline">AI 简报未接入或样本不足</div>
        <div v-for="(b, i) in briefing" :key="i" class="spk-briefing__item">
          <b>{{ b.focus }}</b>
          <span class="spk-briefing__text">{{ b.text }}</span>
          <router-link v-if="b.to" :to="b.to" class="spk-briefing__src">[打开]</router-link>
        </div>
      </div>
    </div>

    <!-- 命令栏 + 预览 -->
    <div class="spk-cmd">
      <div class="spk-cmd__bar">
        <span class="spk-cmd__prefix">&gt;_</span>
        <el-input
          v-model="cmdText"
          placeholder="输入命令：重试 AR-004, 同步 Plane PRJ-001, 检查 FR-003 健康度..."
          @keyup.enter="onParse"
        />
        <el-button type="primary" :loading="parsing" @click="onParse">预览</el-button>
      </div>
    </div>

    <el-collapse-transition>
      <div v-if="preview" class="spk-cmd-preview">
        <div class="spk-card__header">
          <div class="spk-card__title">命令预览 — "{{ cmdText }}"</div>
          <el-button link @click="preview = null">取消</el-button>
        </div>
        <div class="spk-cmd-preview__body">
          <div class="spk-cmd-preview__row">
            <span class="spk-cmd-preview__label">命令</span>
            <code>{{ preview.intent }}</code>
          </div>
          <div v-if="preview.impact" class="spk-cmd-preview__row">
            <span class="spk-cmd-preview__label">影响</span>
            <span>{{ preview.impact }}</span>
          </div>
          <div class="spk-cmd-preview__row">
            <span class="spk-cmd-preview__label">风险等级</span>
            <SpkBadge :map="riskMap" :value="riskLevel" />
          </div>
          <div v-if="preview.idempotencyKey" class="spk-cmd-preview__row">
            <span class="spk-cmd-preview__label">幂等键</span>
            <code>{{ preview.idempotencyKey }}</code>
          </div>
          <div v-if="preview.executable === false" class="spk-cmd-preview__row spk-cmd-preview__row--warn">
            <span class="spk-cmd-preview__label">不可执行</span>
            <span>{{ (preview.ambiguities || []).join('；') || '参数不明确或权限不足' }}</span>
          </div>
          <div class="spk-cmd-preview__row">
            <span class="spk-cmd-preview__label">原因</span>
            <el-input v-model="reason" placeholder="必填：为什么现在执行？（L2 及以上强制）" />
          </div>
        </div>
        <div class="spk-cmd-preview__foot">
          <el-button :loading="executing" :disabled="!canExec" type="primary" size="small" @click="onExecute">
            确认并执行
          </el-button>
          <el-button size="small" @click="preview = null">取消</el-button>
        </div>
      </div>
    </el-collapse-transition>

    <!-- 待办收件箱 + 团队看板 -->
    <div class="grid grid-cols-1 lg:grid-cols-[420px_1fr] gap-16px mt-16px">
      <div class="spk-card">
        <div class="spk-card__header">
          <div class="spk-card__title">
            待办收件箱
            <SpkBadge v-if="inbox.length" :meta="{ color: 'red', text: inbox.length + ' 项' }" />
          </div>
          <el-radio-group v-model="inboxScope" size="small">
            <el-radio-button label="mine">我的 ({{ mineCount }})</el-radio-button>
            <el-radio-button label="team">团队 ({{ teamCount }})</el-radio-button>
          </el-radio-group>
        </div>
        <div class="spk-card__body">
          <el-empty v-if="!inboxFiltered.length" description="收件箱清空" :image-size="50" />
          <div v-else class="spk-att-list">
            <SpkActionItem v-for="(it, i) in inboxFiltered" :key="i" :item="it" @act="onInboxAct" />
          </div>
        </div>
      </div>

      <div class="spk-card">
        <div class="spk-card__header">
          <div class="spk-card__title">团队协作看板</div>
          <span class="spk-card__sub">统一 Human/BPM/Agent/Plane 工作项投影</span>
        </div>
        <div class="spk-card__body spk-card__body--flush">
          <el-empty v-if="!board.length" description="暂无看板数据" :image-size="50" />
          <div v-else class="spk-board">
            <div v-for="col in board" :key="col.stage" class="spk-board__col">
              <div class="spk-board__col-head">{{ colLabel(col.stage) }} <span>{{ col.items.length }}</span></div>
              <div v-for="it in col.items" :key="it.id" class="spk-board__card" :class="boardCardClass(it)">
                <div class="spk-board__card-head">
                  <span class="spk-board__dot" :class="boardDotClass(it)" />
                  <span class="spk-board__name">{{ it.name }}</span>
                </div>
                <div class="spk-board__meta">
                  <el-tag size="small" effect="plain">{{ it.itemType || it.ownerType || '—' }}</el-tag>
                  <span v-if="it.flowRunId">FR-{{ it.flowRunId }}</span>
                  <span v-if="it.dueAt">{{ it.dueAt }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as Wb from '@/api/spk/ipd/workbench'
import SpkBadge from './components/SpkBadge.vue'
import SpkActionItem from './components/SpkActionItem.vue'
import SpkFreshness from './components/SpkFreshness.vue'
import { riskMap } from './components/status'
import type { CommandParseResp, WorkbenchInboxItem, WorkbenchBoardColumn } from '@/api/spk/ipd/workbench'

defineOptions({ name: 'SpkIpdHomeActions' })

const message = useMessage()
const { push } = useRouter()
const loading = ref(true)
const inbox = ref<WorkbenchInboxItem[]>([])
const board = ref<WorkbenchBoardColumn[]>([])
const context = ref<Record<string, any>>({})

const inboxScope = ref<'mine' | 'team'>('mine')
const mineCount = computed(() => inbox.value.length)
const teamCount = computed(() => inbox.value.length)
const inboxFiltered = computed(() => inbox.value)

// —— AI 简报（从 inbox + context 派生；后端无独立简报接口时前端聚合）——
const briefing = computed(() => {
  const blocked = inbox.value.filter((i) => i.type === 'BLOCKED_FLOW' || i.type === 'AGENT_FAILURE')
  const approvals = inbox.value.filter((i) => i.type === 'MY_TODO' || i.type === 'PENDING_DECISION' || i.action === 'APPROVE')
  const out: { focus: string; text: string; to?: any }[] = []
  if (approvals.length) {
    out.push({ focus: '今日焦点', text: `有 ${approvals.length} 项审批/决策待你处理，最紧迫：${approvals[0].title}`, to: { name: 'SpkIpdApproval' } })
  }
  if (blocked.length) {
    out.push({ focus: '风险', text: `${blocked.length} 项阻断/失败需介入：${blocked[0].title}`, to: { name: 'SpkIpdMonitor' } })
  }
  if (context.value?.nextGate) {
    out.push({ focus: '即将到期', text: `下一门禁 ${context.value.nextGate} 即将到期`, to: { name: 'SpkIpdMonitor' } })
  }
  return out
})

// —— 命令栏 ——
const cmdText = ref('')
const parsing = ref(false)
const executing = ref(false)
const preview = ref<CommandParseResp | null>(null)
const reason = ref('')
const riskLevel = computed(() => {
  const intent = preview.value?.intent || ''
  if (/retry|unblock|cancel|reassign/i.test(intent)) return 'L2'
  if (/go|no-go|redirect|baseline|release/i.test(intent)) return 'L3'
  if (/sync|claim|watch/i.test(intent)) return 'L1'
  return 'L0'
})
const canExec = computed(() => {
  if (!preview.value) return false
  if (preview.value.executable === false) return false
  if (riskLevel.value === 'L2' || riskLevel.value === 'L3') return !!reason.value.trim()
  return true
})

const onParse = async () => {
  if (!cmdText.value.trim()) return
  parsing.value = true
  preview.value = null
  try {
    preview.value = await Wb.parseWorkbenchCommand({ text: cmdText.value })
  } catch (e: any) {
    message.error(e?.message || '命令解析失败')
  } finally {
    parsing.value = false
  }
}

const onExecute = async () => {
  if (!preview.value?.idempotencyKey) return
  executing.value = true
  try {
    const resp = await Wb.executeWorkbenchCommand({
      idempotencyKey: preview.value.idempotencyKey,
      intent: preview.value.intent
    })
    if (resp.status === 'SUCCESS' || resp.status === 'IDEMPOTENT') {
      message.success(`已执行 · ${resp.intent}（${resp.status}）`)
      preview.value = null
      cmdText.value = ''
      reason.value = ''
      load()
    } else {
      message.warning(`未执行 · ${resp.status}：${resp.message || ''}`)
    }
  } catch (e: any) {
    message.error(e?.message || '命令执行失败')
  } finally {
    executing.value = false
  }
}

// —— 收件箱动作 ——
const onInboxAct = (item: WorkbenchInboxItem) => {
  if (item.action === 'APPROVE' || item.type === 'MY_TODO') {
    push({ name: 'SpkIpdApproval' })
  } else if (item.flowRunId) {
    push({ name: 'SpkIpdMonitor' })
  }
}

// —— 看板 ——
const colLabel = (s: string) => ({ PENDING: '待处理', RUNNING: '进行中', BLOCKED: '失败/阻塞', DONE: '今日完成' } as any)[s] || s
const boardCardClass = (it: any) => {
  if (it.status === 'FAILED') return 'spk-board__card--red'
  if (it.status === 'BLOCKED') return 'spk-board__card--orange'
  if (it.status === 'DONE' || it.status === 'COMPLETED') return 'spk-board__card--green'
  if (it.status === 'RUNNING') return 'spk-board__card--blue'
  return ''
}
const boardDotClass = (it: any) => {
  if (it.status === 'FAILED') return 'spk-board__dot--red'
  if (it.status === 'BLOCKED') return 'spk-board__dot--orange'
  if (it.status === 'DONE' || it.status === 'COMPLETED') return 'spk-board__dot--green'
  if (it.status === 'RUNNING') return 'spk-board__dot--blue'
  return 'spk-board__dot--gray'
}

const load = async () => {
  loading.value = true
  try {
    const snap = await Wb.getWorkbenchSnapshot().catch(() => ({} as any))
    inbox.value = snap?.inbox || []
    board.value = snap?.board || []
    context.value = snap?.context || {}
  } catch (e: any) {
    if (e?.message) message.error(e.message)
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.spk-actions { }
.spk-briefing {
  background: linear-gradient(135deg, #ddf4ff 0%, #fbefff 100%);
  border: 1px solid #54aeff;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 16px;
}
.spk-briefing__head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.spk-briefing__icon {
  width: 32px; height: 32px; border-radius: 6px; background: #0969da; color: #fff;
  display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px;
}
.spk-briefing__title { font-size: 15px; font-weight: 700; color: #1f2328; }
.spk-briefing__sub { font-size: 12px; color: #656d76; }
.spk-briefing__fresh { margin-left: auto; }
.spk-briefing__body { display: flex; flex-direction: column; gap: 6px; }
.spk-briefing__item { font-size: 13px; color: #1f2328; line-height: 1.6; }
.spk-briefing__text { color: #656d76; }
.spk-briefing__src { color: #0969da; text-decoration: none; margin-left: 4px; }
.spk-briefing__src:hover { text-decoration: underline; }
.spk-cmd { margin-bottom: 12px; }
.spk-cmd__bar { display: flex; align-items: center; gap: 8px; }
.spk-cmd__prefix { font-family: ui-monospace, monospace; color: #4ac26b; font-weight: 700; }
.spk-cmd-preview {
  background: #fff; border: 1px solid #d0d7de; border-radius: 8px; margin-bottom: 12px;
  border-left: 4px solid #fb8f44;
}
.spk-cmd-preview__body { padding: 10px 16px; }
.spk-cmd-preview__row {
  display: grid; grid-template-columns: 100px 1fr; gap: 8px; padding: 6px 0;
  align-items: center; font-size: 13px;
}
.spk-cmd-preview__row--warn { color: #a40e26; }
.spk-cmd-preview__label { color: #656d76; font-weight: 600; }
.spk-cmd-preview__foot { padding: 8px 16px; border-top: 1px solid #eaeef2; display: flex; gap: 8px; }
.spk-card { background: #fff; border: 1px solid #d0d7de; border-radius: 8px; display: flex; flex-direction: column; }
.spk-card__header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #eaeef2; }
.spk-card__title { font-size: 14px; font-weight: 600; color: #1f2328; display: flex; align-items: center; gap: 8px; }
.spk-card__sub { font-size: 12px; color: #8c959f; }
.spk-card__body { padding: 12px 16px; }
.spk-card__body--flush { padding: 0; }
.spk-att-list { display: flex; flex-direction: column; gap: 8px; }
.spk-empty-inline { font-size: 13px; color: #8c959f; text-align: center; padding: 16px; }
.spk-board { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1px; background: #eaeef2; }
.spk-board__col { background: #fff; padding: 8px; min-height: 120px; }
.spk-board__col-head { font-size: 12px; font-weight: 600; color: #656d76; margin-bottom: 8px; display: flex; justify-content: space-between; }
.spk-board__card { background: #f6f8fa; border: 1px solid #eaeef2; border-radius: 6px; padding: 8px; margin-bottom: 6px; }
.spk-board__card--red { background: #ffebe9; border-color: #ff8182; }
.spk-board__card--orange { background: #fff1e5; border-color: #fb8f44; }
.spk-board__card--green { background: #dafbe1; border-color: #4ac26b; }
.spk-board__card--blue { background: #ddf4ff; border-color: #54aeff; }
.spk-board__card-head { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.spk-board__name { font-size: 13px; font-weight: 500; color: #1f2328; }
.spk-board__dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.spk-board__dot--red { background: #ff8182; }
.spk-board__dot--orange { background: #fb8f44; }
.spk-board__dot--green { background: #4ac26b; }
.spk-board__dot--blue { background: #54aeff; }
.spk-board__dot--gray { background: #d0d7de; }
.spk-board__meta { display: flex; align-items: center; gap: 6px; font-size: 11px; color: #656d76; }
</style>
