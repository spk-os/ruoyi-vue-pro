<!--
  SPK-OS IPD 指挥工作台 v4.0（Cortex-IPD UCD §4.4 三视图整体重设计）
  三视图：
    · 任务——全状态扁平任务表（按流程分组）+ 统计 + 详情抽屉（Omnigent iframe/SSE/停止·重跑·再分配）+ 行动队列侧栏
    · 指挥——KPI + 全局参数 + 事件流 + 批量再分配 + 阶段看板 + 自然语言命令条
    · 团队——人/Agent/编队负载网格 + 成员任务抽屉
  数据全真实后端聚合（/workbench/inbox|snapshot 返回 tasks/taskStats/kpis/globalParams/eventStream），
  任务控制走真实端点（agent-task/intervene、flow-runs/cancel|retry、assignments/reassign），
  Omnigent 会话复用既有 proxy（iframe /c/{sid} + SSE），不重造、不造假。
  v3 能力保留：行动队列并入「任务」侧栏，阶段看板+NL 命令条并入「指挥」。
-->
<template>
  <div class="spk-ipd-workbench" data-test="workbench-page">
    <!-- 上下文头 + 三视图切换 -->
    <el-card class="mb-10px" shadow="never">
      <div class="flex flex-wrap items-center gap-12px">
        <span class="font-600 text-16px">指挥工作台</span>
        <el-select v-model="projectId" placeholder="全部项目" clearable filterable class="!w-200px"
          @change="reload">
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-tag v-if="nextGate" type="warning" effect="plain">下一门禁：{{ nextGate }}</el-tag>
        <el-radio-group v-model="view" size="small" data-test="view-switcher">
          <el-radio-button value="tasks" data-test="view-tasks">任务</el-radio-button>
          <el-radio-button value="command" data-test="view-command">指挥</el-radio-button>
          <el-radio-button value="team" data-test="view-team">团队</el-radio-button>
        </el-radio-group>
        <el-button :loading="loading" @click="reload" type="primary" plain size="small">
          <Icon icon="ep:refresh" class="mr-4px" />刷新
        </el-button>
        <span class="text-gray-400 text-12px ml-auto">更新于 {{ refreshedAt || '—' }}</span>
      </div>
    </el-card>

    <!-- ============ 视图一 · 任务 ============ -->
    <div v-show="view === 'tasks'">
      <el-row :gutter="10">
        <el-col :span="18">
          <!-- 统计条 -->
          <div class="stat-row mb-10px">
            <div v-for="s in statCards" :key="s.key" class="stat-card"
              :class="{ 'is-clickable': s.key === 'failed' && s.value > 0 }"
              @click="s.key === 'failed' && s.value > 0 ? (failFilter = !failFilter) : null">
              <div class="stat-card__value" :style="{ color: s.color }">{{ s.value }}</div>
              <div class="stat-card__label">{{ s.label }}</div>
            </div>
          </div>

          <!-- 过滤条 -->
          <el-card shadow="never" class="mb-10px">
            <div class="flex flex-wrap items-center gap-10px">
              <el-input v-model="taskSearch" placeholder="搜索任务名/流程编号/执行者" clearable
                class="!w-260px" :prefix-icon="'ep:search'" />
              <el-select v-model="taskStatusFilter" placeholder="状态" clearable class="!w-140px">
                <el-option v-for="s in statusOptions" :key="s" :label="statusLabel(s)" :value="s" />
              </el-select>
              <el-select v-model="taskOwnerFilter" placeholder="执行者类型" clearable class="!w-140px">
                <el-option label="人员" value="HUMAN" />
                <el-option label="Agent" value="AGENT" />
                <el-option label="编队" value="SQUAD" />
                <el-option label="系统" value="SYSTEM" />
              </el-select>
              <el-checkbox v-model="failFilter">仅看失败</el-checkbox>
              <el-button text @click="failFilter = false; taskStatusFilter = undefined; taskOwnerFilter = undefined; taskSearch = ''">重置</el-button>
            </div>
          </el-card>

          <!-- 任务表（按流程分组） -->
          <el-card shadow="never" data-test="task-table-card">
            <template #header>
              <div class="flex items-center justify-between">
                <span><Icon icon="ep:list" class="mr-4px" />任务清单</span>
                <span class="text-gray-400 text-12px">{{ filteredTasks.length }} 项 / {{ groupedTasks.length }} 流程</span>
              </div>
            </template>
            <el-empty v-if="!groupedTasks.length" description="无活跃任务" :image-size="60" />
            <div v-for="g in groupedTasks" :key="g.flowRunKey" class="task-group">
              <div class="task-group__head">
                <Icon icon="ep:files" class="mr-4px" />
                <span class="font-600">{{ g.flowRunNo || g.projectName || '未分组' }}</span>
                <el-tag size="small" effect="plain" class="ml-8px">{{ g.items.length }}</el-tag>
                <span v-if="g.projectName" class="text-gray-400 text-12px ml-8px">{{ g.projectName }}</span>
              </div>
              <el-table :data="g.items" size="small" @row-click="openDetail" row-class-name="task-row">
                <el-table-column prop="name" label="任务" min-width="180" show-overflow-tooltip />
                <el-table-column prop="stage" label="阶段" width="110" />
                <el-table-column label="执行者" width="120">
                  <template #default="{ row }">
                    <el-tag :type="ownerTypeColor(row.ownerType)" size="small" effect="plain">
                      {{ ownerTypeLabel(row.ownerType) }}
                    </el-tag>
                    <span class="ml-4px text-12px">{{ row.ownerName || '—' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="statusType(row.status)" size="small" effect="dark">
                      {{ statusLabel(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="进度" width="120">
                  <template #default="{ row }">
                    <el-progress :percentage="row.progress || 0" :stroke-width="6" :show-text="false"
                      :status="progressStatus(row.status)" />
                  </template>
                </el-table-column>
                <el-table-column label="时长" width="90">
                  <template #default="{ row }">{{ formatDuration(row.durationSec) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="220" fixed="right">
                  <template #default="{ row }">
                    <el-button size="small" text type="primary" @click.stop="openDetail(row)">详情</el-button>
                    <el-button v-if="isRunningLike(row.status)" size="small" text type="danger"
                      @click.stop="doAbort(row)" data-test="task-abort">停止</el-button>
                    <el-button v-if="isRecoverable(row.status)" size="small" text type="warning"
                      @click.stop="doRerun(row)" data-test="task-rerun">重跑</el-button>
                    <el-button size="small" text type="info" @click.stop="openReassign(row)">再分配</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-card>
        </el-col>

        <!-- 行动队列侧栏（v3 保留） -->
        <el-col :span="6">
          <el-card shadow="never" class="inbox-card" data-test="action-inbox">
            <template #header>
              <div class="flex items-center justify-between">
                <span><Icon icon="ep:bell" class="mr-4px" />行动队列</span>
                <el-tag size="small" type="danger">{{ inbox.length }}</el-tag>
              </div>
            </template>
            <el-empty v-if="!inbox.length" description="无待办行动" :image-size="50" />
            <div v-for="(it, i) in inbox" :key="i" class="inbox-item">
              <div class="flex items-center gap-6px mb-4px">
                <el-tag :type="severityType(it.severity)" size="small" effect="dark">{{ it.severity }}</el-tag>
                <span class="text-12px font-600 flex-1 ellipsis">{{ it.title }}</span>
              </div>
              <div class="text-gray-500 text-11px mb-6px ellipsis2">{{ it.detail }}</div>
              <div class="flex gap-4px flex-wrap">
                <el-button v-if="it.action === 'APPROVE'" size="small" type="primary" @click="goApprove(it)">去审批</el-button>
                <el-button v-if="it.action === 'RETRY'" size="small" type="warning" @click="quickCommand('INTERVENE', it, 'rerun')">重试</el-button>
                <el-button v-if="it.action === 'UNBLOCK'" size="small" type="danger" @click="quickCommand('UNBLOCK', it, 'rerun')">解除阻断</el-button>
                <el-button size="small" text @click="copyRef(it)">复制引用</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 任务详情抽屉 -->
    <el-drawer v-model="detailVisible" size="60%" :title="detailTitle" data-test="task-detail-drawer">
      <div v-if="selectedTask" class="task-detail">
        <el-descriptions :column="2" border size="small" class="mb-12px">
          <el-descriptions-item label="任务名">{{ selectedTask.name }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(selectedTask.status)" size="small" effect="dark">{{ statusLabel(selectedTask.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="所属项目">{{ selectedTask.projectName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="流程编号">{{ selectedTask.flowRunNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="阶段">{{ selectedTask.stage || '—' }}</el-descriptions-item>
          <el-descriptions-item label="执行者">{{ ownerTypeLabel(selectedTask.ownerType) }} {{ selectedTask.ownerName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="进度">
            <el-progress :percentage="selectedTask.progress || 0" :status="progressStatus(selectedTask.status)" />
          </el-descriptions-item>
          <el-descriptions-item label="已运行时长">{{ formatDuration(selectedTask.durationSec) }}</el-descriptions-item>
          <el-descriptions-item label="到期">{{ selectedTask.dueAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="Activity Run ID">
            <el-link type="primary" @click="copyText(selectedTask.activityRunId)">{{ selectedTask.activityRunId }}</el-link>
          </el-descriptions-item>
          <el-descriptions-item label="Omnigent 会话">{{ selectedTask.sessionId || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 控制按钮 -->
        <div class="flex gap-8px mb-12px">
          <el-button v-if="isRunningLike(selectedTask.status)" type="danger" plain :loading="ctrlLoading" @click="doAbort(selectedTask)">
            <Icon icon="ep:circle-close" class="mr-4px" />停止运行
          </el-button>
          <el-button v-if="isRecoverable(selectedTask.status)" type="warning" plain :loading="ctrlLoading" @click="doRerun(selectedTask)">
            <Icon icon="ep:refresh-right" class="mr-4px" />重新运行
          </el-button>
          <el-button type="info" plain @click="openReassign(selectedTask)">
            <Icon icon="ep:sort" class="mr-4px" />重新分配
          </el-button>
        </div>

        <!-- Omnigent 会话嵌入（复用既有 proxy） -->
        <el-card v-if="selectedTask.sessionId" shadow="never" class="mb-12px">
          <template #header>
            <div class="flex items-center justify-between">
              <span><Icon icon="ep:monitor" class="mr-4px" />Omnigent 会话</span>
              <div class="flex gap-6px">
                <el-tag size="small" :type="sseConnected ? 'success' : 'info'">
                  {{ sseConnected ? '实时流已连接' : '实时流未连接' }}
                </el-tag>
                <el-button size="small" text @click="toggleSse">{{ sseConnected ? '断开' : '连接' }}实时流</el-button>
              </div>
            </div>
          </template>
          <iframe :src="omniFrameUrl" class="omni-frame" frameborder="0"
            @load="onFrameLoad" data-test="omnigent-iframe"></iframe>
          <div v-if="sseLogs.length" class="sse-log" data-test="sse-log">
            <div v-for="(l, i) in sseLogs" :key="i" class="sse-log__line">{{ l }}</div>
          </div>
        </el-card>
        <el-empty v-else description="该任务无 Omnigent 会话（非 Agent 执行或会话未落库）" :image-size="50" />
      </div>
    </el-drawer>

    <!-- 再分配弹窗 -->
    <el-dialog v-model="reassignVisible" title="重新分配执行者" width="520px">
      <el-form v-if="reassignForm" label-width="100px">
        <el-form-item label="任务">
          <span class="text-13px">{{ reassignForm.taskName }}</span>
        </el-form-item>
        <el-form-item label="候选执行者">
          <el-select v-model="reassignForm.candidate" filterable placeholder="选择执行者（真实 candidates 接口）"
            :loading="candidateLoading" class="!w-full">
            <el-option v-for="c in candidates" :key="`${c.actorType}:${c.actorId}`"
              :label="`${c.name}（${ownerTypeLabel(c.actorType)}${c.businessRole ? ' · ' + c.businessRole : ''}）`"
              :value="`${c.actorType}:${c.actorId}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="reassignForm.reason" type="textarea" :rows="2" placeholder="再分配原因（审计追溯）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reassignVisible = false">取消</el-button>
        <el-button type="primary" :loading="reassignLoading" @click="doReassign" data-test="reassign-confirm">确认再分配</el-button>
      </template>
    </el-dialog>

    <!-- ============ 视图二 · 指挥 ============ -->
    <div v-show="view === 'command'">
      <!-- KPI 卡片 -->
      <div class="stat-row mb-10px">
        <div v-for="k in kpiCards" :key="k.key" class="kpi-card">
          <div class="kpi-card__value" :style="{ color: k.color }">{{ k.value }}</div>
          <div class="kpi-card__label">{{ k.label }}</div>
        </div>
      </div>

      <el-row :gutter="10">
        <el-col :span="14">
          <!-- 阶段看板（v3 保留） -->
          <el-card shadow="never" class="board-card" data-test="stage-task-board">
            <template #header>
              <div class="flex items-center justify-between">
                <span><Icon icon="ep:grid" class="mr-4px" />阶段任务看板</span>
                <span class="text-gray-400 text-12px">{{ boardCount }} 项</span>
              </div>
            </template>
            <el-empty v-if="!board.length" description="无活跃流程" :image-size="50" />
            <div class="board-cols">
              <div v-for="col in board" :key="col.stage" class="board-col">
                <div class="board-col__head">
                  {{ col.stage }}
                  <el-tag size="small" round>{{ col.items.length }}</el-tag>
                </div>
                <div v-for="bi in col.items" :key="bi.id" class="board-item" :class="statusClass(bi.status)">
                  <div class="text-13px font-600 mb-2px ellipsis">{{ bi.name }}</div>
                  <div class="flex items-center justify-between text-11px text-gray-500">
                    <span>{{ bi.ownerName || '未分派' }}</span>
                    <el-tag :type="statusType(bi.status)" size="small" effect="plain">{{ statusLabel(bi.status) }}</el-tag>
                  </div>
                </div>
              </div>
            </div>
          </el-card>

          <!-- 批量操作 -->
          <el-card shadow="never" class="mt-10px" data-test="batch-ops">
            <template #header>
              <span><Icon icon="ep:operation" class="mr-4px" />批量操作（勾选任务后执行）</span>
            </template>
            <div class="flex items-center gap-8px">
              <el-select v-model="batchTaskSel" multiple collapse-tags placeholder="选择任务（按任务名）" class="flex-1">
                <el-option v-for="t in tasks" :key="t.activityRunId"
                  :label="`${t.name} · ${t.flowRunNo || ''}`" :value="t.activityRunId" />
              </el-select>
              <el-button type="primary" plain :disabled="!batchTaskSel.length" @click="openBatchReassign">批量再分配</el-button>
            </div>
            <div class="text-gray-400 text-12px mt-6px">
              说明：批量优先级调整能力尚未对齐后端端点（设计稿标注的"批量调整优先级"在当前接口暂无落点），已实现的批量再分配走真实 assignments/reassign，不做假按钮。
            </div>
          </el-card>
        </el-col>

        <el-col :span="10">
          <!-- 全局执行参数 -->
          <el-card shadow="never" class="mb-10px">
            <template #header><span><Icon icon="ep:setting" class="mr-4px" />全局执行参数（只读快照）</span></template>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item v-for="(v, k) in globalParams" :key="k" :label="paramLabel(String(k))">
                {{ v === null || v === undefined ? '—' : String(v) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 事件流 -->
          <el-card shadow="never" data-test="event-stream">
            <template #header><span><Icon icon="ep:clock" class="mr-4px" />最近事件流</span></template>
            <el-empty v-if="!eventStream.length" description="暂无事件" :image-size="50" />
            <el-timeline>
              <el-timeline-item v-for="(e, i) in eventStream" :key="i" :timestamp="e.at" placement="top"
                :type="eventTagType(e.status)">
                <div class="text-13px font-600">{{ e.flowRunNo || e.activity || '—' }}</div>
                <div class="text-12px text-gray-500">
                  <el-tag size="small" effect="plain">{{ e.status }}</el-tag>
                  <span v-if="e.agent" class="ml-6px">{{ e.agent }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </el-col>
      </el-row>

      <!-- 自然语言命令条（v3 保留） -->
      <el-card class="mt-10px" shadow="never">
        <template #header>
          <span><Icon icon="ep:magic-stick" class="mr-4px" />自然语言指挥</span>
          <span class="text-gray-400 text-12px ml-8px">关键词：重试/介入/解除阻断/审批/CCB/分派/启动 — 解析预览后确认执行（幂等白名单）</span>
        </template>
        <div class="flex gap-8px">
          <el-input v-model="cmdText" placeholder="例如：对 activity a1b2 重试" clearable class="flex-1" @keyup.enter="doParse" />
          <el-button type="primary" :loading="parsing" @click="doParse" data-test="command-parse-btn">解析</el-button>
        </div>
        <div v-if="preview" class="mt-12px command-preview" data-test="command-preview">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="意图">
              <el-tag :type="intentType(preview.intent)">{{ preview.intent }}</el-tag>
              <el-tag v-if="preview.executable" type="success" size="small" class="ml-8px">可执行</el-tag>
              <el-tag v-else type="info" size="small" class="ml-8px">不可执行</el-tag>
            </el-descriptions-item>
            <el-descriptions-item v-if="preview.targets && preview.targets.length" label="对象">
              <el-tag v-for="(t, i) in preview.targets" :key="i" size="small" class="mr-4px">{{ JSON.stringify(t) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item v-if="preview.actions && preview.actions.length" label="动作">
              <div v-for="(a, i) in preview.actions" :key="i" class="text-13px">{{ i + 1 }}. {{ a.action }} - {{ a.description }}</div>
            </el-descriptions-item>
            <el-descriptions-item v-if="preview.ambiguities && preview.ambiguities.length" label="歧义">
              <div v-for="(a, i) in preview.ambiguities" :key="i" class="text-orange-500 text-12px">· {{ a }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="影响">{{ preview.impact }}</el-descriptions-item>
          </el-descriptions>
          <div class="mt-8px flex gap-8px">
            <el-input v-if="needsRunId(preview.intent)" v-model="preview.activityRunId" placeholder="补全 activityRunId（必填）" class="!w-320px" />
            <el-button type="success" :loading="executing" :disabled="!preview.executable" @click="doExecute" data-test="command-execute-btn">确认执行</el-button>
            <el-button @click="preview = null">取消</el-button>
          </div>
        </div>
        <el-alert v-if="execResult" class="mt-10px" :title="execResult.message" :type="resultType(execResult.status)" :closable="false" show-icon />
      </el-card>
    </div>

    <!-- ============ 视图三 · 团队 ============ -->
    <div v-show="view === 'team'">
      <el-card shadow="never" class="mb-10px">
        <div class="flex flex-wrap items-center gap-10px">
          <el-radio-group v-model="teamFilter" size="small">
            <el-radio-button value="ALL">全部</el-radio-button>
            <el-radio-button value="AGENT">仅 Agent</el-radio-button>
            <el-radio-button value="HUMAN">仅人员</el-radio-button>
            <el-radio-button value="SQUAD">仅编队</el-radio-button>
          </el-radio-group>
          <el-radio-group v-model="teamSort" size="small">
            <el-radio-button value="load">按负载</el-radio-button>
            <el-radio-button value="name">按名称</el-radio-button>
          </el-radio-group>
          <span v-if="teamSummary" class="text-gray-400 text-12px ml-auto">
            人 {{ teamSummary.people || 0 }} · Agent {{ teamSummary.agents || 0 }} · 编队 {{ teamSummary.squads || 0 }}
          </span>
        </div>
      </el-card>

      <el-empty v-if="!teamMembers.length" description="无团队成员" :image-size="60" />
      <div class="member-grid">
        <div v-for="m in teamMembers" :key="`${m.actorType}:${m.actorId}`" class="member-card"
          @click="openMember(m)" data-test="member-card">
          <div class="flex items-center gap-8px mb-8px">
            <el-tag :type="ownerTypeColor(m.actorType)" size="small" effect="dark">{{ ownerTypeLabel(m.actorType) }}</el-tag>
            <span class="text-14px font-600 ellipsis flex-1">{{ m.name }}</span>
          </div>
          <div v-if="m.businessRole || m.subtitle" class="text-12px text-gray-500 mb-6px ellipsis">
            {{ m.businessRole || m.subtitle }}
          </div>
          <div v-if="m.load" class="mb-4px">
            <div class="flex justify-between text-11px text-gray-500 mb-2px">
              <span>负载 {{ m.load.total }} 项</span>
              <span>{{ Math.round((m.load.running + m.load.blocked) / Math.max(m.load.total, 1) * 100) }}%</span>
            </div>
            <el-progress :percentage="Math.round((m.load.running + m.load.blocked) / Math.max(m.load.total, 1) * 100)"
              :stroke-width="8" :show-text="false" status="warning" />
            <div class="text-11px text-gray-400 mt-2px">
              运行 {{ m.load.running }} · 阻塞 {{ m.load.blocked }} · 完成 {{ m.load.done }}
            </div>
          </div>
          <div v-else class="text-12px text-gray-400">暂无负载数据</div>
        </div>
      </div>

      <!-- 成员任务抽屉 -->
      <el-drawer v-model="memberVisible" size="55%" :title="`成员任务：${memberTitle}`">
        <div v-if="memberTasks.length" class="mb-12px">
          <el-table :data="memberTasks" size="small">
            <el-table-column prop="name" label="任务" min-width="160" show-overflow-tooltip />
            <el-table-column prop="flowRunNo" label="流程" width="120" />
            <el-table-column prop="stage" label="阶段" width="100" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small" effect="dark">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时长" width="80">
              <template #default="{ row }">{{ formatDuration(row.durationSec) }}</template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else description="该成员当前无任务" :image-size="60" />
      </el-drawer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as WorkbenchApi from '@/api/spk/ipd/workbench'
import { getPage as getPageProjects, getTeam, candidates as loadCandidates,
  reassign as reassignAssignment, pageAssignments } from '@/api/spk/ipd/business'
import type {
  WorkbenchRespVO, WorkbenchInboxItem, WorkbenchBoardColumn,
  WorkbenchTaskRow, CommandParseResp, CommandExecuteResp
} from '@/api/spk/ipd/workbench'
import { statusMap, labelText } from '@/views/spk/ipd/home/components/status'

defineOptions({ name: 'SpkIpdWorkbench' })

const router = useRouter()
const loading = ref(false)
const view = ref<'tasks' | 'command' | 'team'>('tasks')
const projectId = ref<number | undefined>(undefined)
const projects = ref<any[]>([])
const context = ref<Record<string, any> | null>(null)
const nextGate = ref<string | undefined>(undefined)
const refreshedAt = ref<string | undefined>(undefined)
const inbox = ref<WorkbenchInboxItem[]>([])
const board = ref<WorkbenchBoardColumn[]>([])
const tasks = ref<WorkbenchTaskRow[]>([])
const taskStats = ref<Record<string, number>>({})
const kpis = ref<Record<string, any>>({})
const globalParams = ref<Record<string, any>>({})
const eventStream = ref<Array<Record<string, any>>>([])

// ===== 数据加载 =====
const reload = async () => {
  loading.value = true
  try {
    const data = await WorkbenchApi.getWorkbenchSnapshot(projectId.value, undefined)
    context.value = data.context || null
    nextGate.value = data.nextGate
    refreshedAt.value = data.refreshedAt
    inbox.value = data.inbox || []
    board.value = data.board || []
    tasks.value = data.tasks || []
    taskStats.value = data.taskStats || {}
    kpis.value = data.kpis || {}
    globalParams.value = data.globalParams || {}
    eventStream.value = data.eventStream || []
  } catch (e: any) {
    ElMessage.error('工作台数据加载失败：' + (e?.message || e))
  } finally {
    loading.value = false
  }
}

const loadProjects = async () => {
  try {
    const res = await getPageProjects({ pageNo: 1, pageSize: 100 } as any)
    projects.value = (res?.list || []).map((p: any) => ({ id: p.id, name: p.name || p.projectCode }))
  } catch {
    projects.value = []
  }
}

onMounted(async () => {
  await loadProjects()
  await reload()
})
onBeforeUnmount(() => disconnectSse())

// ===== 任务视图 =====
const taskSearch = ref('')
const taskStatusFilter = ref<string | undefined>(undefined)
const taskOwnerFilter = ref<string | undefined>(undefined)
const failFilter = ref(false)

const statusOptions = ['queued', 'running', 'done', 'failed', 'timeout', 'cancelled']

const filteredTasks = computed(() => {
  let list = tasks.value
  if (failFilter.value) list = list.filter(t => ['failed', 'timeout', 'cancelled'].includes(t.status || ''))
  if (taskStatusFilter.value) list = list.filter(t => t.status === taskStatusFilter.value)
  if (taskOwnerFilter.value) list = list.filter(t => t.ownerType === taskOwnerFilter.value)
  const kw = taskSearch.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(t =>
      (t.name || '').toLowerCase().includes(kw) ||
      (t.flowRunNo || '').toLowerCase().includes(kw) ||
      (t.ownerName || '').toLowerCase().includes(kw))
  }
  return list
})

// 按流程分组（flowRunId 为键）
const groupedTasks = computed(() => {
  const map = new Map<string, { flowRunKey: string; flowRunNo: string; projectName: string; items: WorkbenchTaskRow[] }>()
  for (const t of filteredTasks.value) {
    const key = String(t.flowRunId || t.activityRunId)
    if (!map.has(key)) {
      map.set(key, {
        flowRunKey: key,
        flowRunNo: t.flowRunNo || '',
        projectName: t.projectName || '',
        items: []
      })
    }
    map.get(key)!.items.push(t)
  }
  return Array.from(map.values())
})

const statCards = computed(() => {
  const s = taskStats.value
  const card = (key: string, label: string, color: string) => ({
    key, label, color, value: Number(s[key] || 0)
  })
  return [
    card('queued', '待执行', '#909399'),
    card('running', '执行中', '#e6a23c'),
    card('failed', '失败', '#f56c6c'),
    card('timeout', '超时', '#f56c6c'),
    card('cancelled', '已取消', '#909399'),
    card('done', '已完成', '#67c23a')
  ]
})

// ===== 任务详情抽屉 =====
const detailVisible = ref(false)
const selectedTask = ref<WorkbenchTaskRow | null>(null)
const ctrlLoading = ref(false)
const detailTitle = computed(() => selectedTask.value ? `任务详情：${selectedTask.value.name}` : '任务详情')

const omniFrameUrl = computed(() =>
  selectedTask.value?.sessionId ? WorkbenchApi.omnigentSessionFrameUrl(selectedTask.value.sessionId) : ''
)

const openDetail = (row: WorkbenchTaskRow) => {
  selectedTask.value = row
  detailVisible.value = true
  // 有会话则自动连接 SSE
  if (row.sessionId) {
    nextTickConnect(row.sessionId)
  }
}

const doAbort = async (t: WorkbenchTaskRow) => {
  try {
    await ElMessageBox.confirm(`确认停止任务「${t.name}」（${t.activityRunId}）？将标记失败并推进流程。`, '停止运行', { type: 'warning' })
  } catch { return }
  ctrlLoading.value = true
  try {
    await WorkbenchApi.interveneActivity(t.activityRunId, 'abort', 'manual-abort')
    ElMessage.success('已停止任务')
    await reload()
  } catch (e: any) {
    ElMessage.error('停止失败：' + (e?.message || e))
  } finally {
    ctrlLoading.value = false
  }
}

const doRerun = async (t: WorkbenchTaskRow) => {
  try {
    await ElMessageBox.confirm(`确认重新运行任务「${t.name}」（${t.activityRunId}）？`, '重新运行', { type: 'warning' })
  } catch { return }
  ctrlLoading.value = true
  try {
    await WorkbenchApi.interveneActivity(t.activityRunId, 'rerun', 'manual-rerun')
    ElMessage.success('已发起重跑')
    await reload()
  } catch (e: any) {
    ElMessage.error('重跑失败：' + (e?.message || e))
  } finally {
    ctrlLoading.value = false
  }
}

// ===== 再分配 =====
const reassignVisible = ref(false)
const reassignForm = ref<any>(null)
const candidates = ref<any[]>([])
const candidateLoading = ref(false)
const reassignLoading = ref(false)

const openReassign = async (t: WorkbenchTaskRow) => {
  selectedTask.value = t
  reassignForm.value = { taskName: t.name, activityRunId: t.activityRunId, flowRunId: t.flowRunId, candidate: '', reason: '' }
  reassignVisible.value = true
  candidateLoading.value = true
  try {
    candidates.value = await loadCandidates(undefined, undefined, undefined) || []
  } catch (e: any) {
    candidates.value = []
    ElMessage.warning('候选执行者加载失败：' + (e?.message || e))
  } finally {
    candidateLoading.value = false
  }
}

const doReassign = async () => {
  if (!reassignForm.value?.candidate) {
    ElMessage.warning('请选择执行者')
    return
  }
  // 先查当前 assignmentId（按 activityRunId 取该任务分派记录），真实接口
  reassignLoading.value = true
  try {
    const [actorType, actorIdStr] = reassignForm.value.candidate.split(':')
    const actorId = Number(actorIdStr)
    // 按 activityRunId 查 assignment
    const page: any = await pageAssignments({
      activityRunId: reassignForm.value.activityRunId, pageNo: 1, pageSize: 10
    })
    const assignmentId = page?.list?.[0]?.id
    if (!assignmentId) {
      ElMessage.warning('未找到该任务的分派记录，无法再分配（可能尚未分派）')
      return
    }
    await reassignAssignment(assignmentId, {
      actorType, actorId,
      reason: reassignForm.value.reason || 'workbench-reassign'
    })
    ElMessage.success('已再分配')
    reassignVisible.value = false
    await reload()
  } catch (e: any) {
    ElMessage.error('再分配失败：' + (e?.message || e))
  } finally {
    reassignLoading.value = false
  }
}

// ===== 批量再分配 =====
const batchTaskSel = ref<string[]>([])
const openBatchReassign = () => {
  if (!batchTaskSel.value.length) return
  ElMessageBox.prompt(`确认为选中的 ${batchTaskSel.value.length} 个任务批量再分配，输入执行者 actorType:actorId：`, '批量再分配', {
    inputPlaceholder: '例如 AGENT:101'
  }).then(async ({ value }: any) => {
    const [actorType, actorIdStr] = String(value).split(':')
    if (!actorType || !actorIdStr) { ElMessage.warning('格式应为 ACTOR_TYPE:ID'); return }
    let ok = 0, fail = 0
    for (const aid of batchTaskSel.value) {
      try {
        const page: any = await pageAssignments({ activityRunId: aid, pageNo: 1, pageSize: 10 })
        const assignmentId = page?.list?.[0]?.id
        if (assignmentId) {
          await reassignAssignment(assignmentId, { actorType, actorId: Number(actorIdStr), reason: 'batch-reassign' })
          ok++
        } else { fail++ }
      } catch { fail++ }
    }
    ElMessage.success(`批量再分配完成：成功 ${ok}，跳过 ${fail}`)
    batchTaskSel.value = []
    await reload()
  }).catch(() => {})
}

// ===== Omnigent SSE 实时流（复用既有 proxy） =====
let sseRef: EventSource | null = null
const sseConnected = ref(false)
const sseLogs = ref<string[]>([])
const MAX_SSE_LOGS = 200

const nextTickConnect = (sid: string) => {
  disconnectSse()
  try {
    const url = WorkbenchApi.omnigentStreamUrl(sid)
    const es = new EventSource(url)
    sseRef = es
    es.onopen = () => { sseConnected.value = true; pushLog('[stream] 已连接') }
    es.onmessage = (ev) => pushLog(ev.data)
    es.onerror = () => { sseConnected.value = false; pushLog('[stream] 连接断开/超时') }
  } catch (e: any) {
    pushLog('[stream] 初始化失败：' + (e?.message || e))
  }
}
const disconnectSse = () => {
  if (sseRef) { sseRef.close(); sseRef = null }
  sseConnected.value = false
}
const toggleSse = () => {
  if (sseConnected.value) { disconnectSse() }
  else if (selectedTask.value?.sessionId) { nextTickConnect(selectedTask.value.sessionId) }
}
const pushLog = (line: string) => {
  const ts = new Date().toLocaleTimeString()
  sseLogs.value.push(`${ts} ${line}`)
  if (sseLogs.value.length > MAX_SSE_LOGS) sseLogs.value = sseLogs.value.slice(-MAX_SSE_LOGS)
}
const onFrameLoad = () => { pushLog('[iframe] Omnigent 会话页已加载') }

// ===== 团队视图 =====
const teamData = ref<any>(null)
const teamFilter = ref<'ALL' | 'AGENT' | 'HUMAN' | 'SQUAD'>('ALL')
const teamSort = ref<'load' | 'name'>('load')
const memberVisible = ref(false)
const memberTitle = ref('')
const memberTasks = ref<WorkbenchTaskRow[]>([])

const teamSummary = computed(() => teamData.value?.summary || null)

const teamMembers = computed(() => {
  const d = teamData.value
  if (!d) return []
  let list: any[] = []
  if (teamFilter.value === 'AGENT' || teamFilter.value === 'ALL') list = list.concat((d.agents || []).map((a: any) => ({ ...a, actorType: a.actorType || 'AGENT' })))
  if (teamFilter.value === 'HUMAN' || teamFilter.value === 'ALL') list = list.concat((d.people || []).map((a: any) => ({ ...a, actorType: a.actorType || 'HUMAN' })))
  if (teamFilter.value === 'SQUAD' || teamFilter.value === 'ALL') list = list.concat((d.squads || []).map((a: any) => ({ ...a, actorType: a.actorType || 'SQUAD' })))
  // 关联负载
  const loadMap = new Map((d.load || []).map((l: any) => [`${l.actorType}:${l.actorId}`, l]))
  list = list.map(m => ({ ...m, load: loadMap.get(`${m.actorType}:${m.actorId}`) }))
  if (teamSort.value === 'name') {
    list.sort((a, b) => String(a.name).localeCompare(String(b.name)))
  } else {
    list.sort((a, b) => (b.load?.total || 0) - (a.load?.total || 0))
  }
  return list
})

const loadTeam = async () => {
  try {
    teamData.value = await getTeam(projectId.value, undefined)
  } catch (e: any) {
    teamData.value = null
    ElMessage.warning('团队数据加载失败：' + (e?.message || e))
  }
}
watch(() => view.value, (v) => { if (v === 'team' && !teamData.value) loadTeam() })

const openMember = (m: any) => {
  memberTitle.value = `${ownerTypeLabel(m.actorType)} ${m.name}`
  // 按执行者名过滤当前任务列表
  memberTasks.value = tasks.value.filter(t => t.ownerName === m.name)
  memberVisible.value = true
}

// ===== 行动队列动作（v3 保留） =====
const goApprove = (it: WorkbenchInboxItem) => {
  router.push({ path: '/spk/ipd-approval', query: { taskId: it.refId, flowRunId: it.flowRunId } })
}
const copyRef = (it: WorkbenchInboxItem) => { navigator.clipboard?.writeText(`${it.refType}:${it.refId}`); ElMessage.success('已复制引用') }

// ===== 自然语言命令（v3 保留） =====
const cmdText = ref('')
const parsing = ref(false)
const executing = ref(false)
const preview = ref<(CommandParseResp & { activityRunId?: string }) | null>(null)
const execResult = ref<CommandExecuteResp | null>(null)

const doParse = async () => {
  if (!cmdText.value.trim()) { ElMessage.warning('请输入命令'); return }
  parsing.value = true
  execResult.value = null
  try {
    const resp = await WorkbenchApi.parseWorkbenchCommand({ text: cmdText.value, projectId: projectId.value })
    preview.value = { ...resp, activityRunId: extractRunId(cmdText.value) }
  } catch (e: any) {
    ElMessage.error('解析失败：' + (e?.message || e))
  } finally {
    parsing.value = false
  }
}
const needsRunId = (intent?: string) => ['RETRY', 'INTERVENE', 'UNBLOCK'].includes(intent || '')
const doExecute = async () => {
  if (!preview.value) return
  const intent = preview.value.intent
  let params: Record<string, any> = {}
  if (needsRunId(intent)) {
    const runId = (preview.value as any).activityRunId
    if (!runId) { ElMessage.warning('该意图需要 activityRunId'); return }
    params.activityRunId = runId
    params.action = 'rerun'
  }
  try {
    await ElMessageBox.confirm(`确认执行意图 ${intent}？该操作幂等，可重复提交。`, '命令确认', { type: 'warning' })
  } catch { return }
  executing.value = true
  try {
    const resp = await WorkbenchApi.executeWorkbenchCommand({
      idempotencyKey: preview.value.idempotencyKey!, intent, params
    })
    execResult.value = resp
    if (resp.status === 'SUCCESS') { ElMessage.success(resp.message); reload() }
  } finally {
    executing.value = false
  }
}
const extractRunId = (text: string): string | undefined => {
  const m = text.match(/([a-zA-Z0-9_-]{6,})/)
  return m ? m[1] : undefined
}
const quickCommand = async (intent: string, it: WorkbenchInboxItem, action: string) => {
  try {
    await ElMessageBox.confirm(`确认对 ${it.refId} 执行 ${action}？`, '快捷操作', { type: 'warning' })
  } catch { return }
  try {
    const resp = await WorkbenchApi.executeWorkbenchCommand({
      idempotencyKey: `quick-${intent}-${it.refId}-${action}`, intent,
      params: { activityRunId: it.refId, action, targetType: it.refType }
    })
    if (resp.status === 'SUCCESS') { ElMessage.success(resp.message); reload() }
    else { ElMessage.warning(resp.message) }
  } catch (e: any) {
    ElMessage.error('执行失败：' + (e?.message || e))
  }
}

// ===== 展示辅助 =====
const statusLabel = (s?: string) => labelText(statusMap, s)
const statusType = (s?: string) => {
  if (!s) return 'info'
  const u = s.toLowerCase()
  if (u === 'failed' || u === 'timeout') return 'danger'
  if (u === 'running') return 'warning'
  if (u === 'queued') return 'info'
  return 'success'
}
const statusClass = (s?: string) => {
  if (!s) return ''
  const u = s.toLowerCase()
  if (u === 'failed' || u === 'timeout') return 'is-failed'
  if (u === 'running') return 'is-running'
  if (u === 'queued') return 'is-queued'
  return ''
}
const isRunningLike = (s?: string) => ['running', 'queued'].includes((s || '').toLowerCase())
const isRecoverable = (s?: string) => ['failed', 'timeout'].includes((s || '').toLowerCase())
const progressStatus = (s?: string) => {
  const u = (s || '').toLowerCase()
  if (u === 'failed' || u === 'timeout' || u === 'cancelled') return 'exception'
  if (u === 'done') return 'success'
  return undefined
}
const ownerTypeLabel = (t?: string) => {
  const m: Record<string, string> = { HUMAN: '人员', AGENT: 'Agent', SQUAD: '编队', SYSTEM: '系统' }
  return m[t || ''] || t || '—'
}
const ownerTypeColor = (t?: string) => {
  const m: Record<string, string> = { HUMAN: 'success', AGENT: 'primary', SQUAD: 'warning', SYSTEM: 'info' }
  return m[t || ''] || 'info'
}
const formatDuration = (sec?: number) => {
  if (sec == null) return '—'
  if (sec < 60) return `${sec}s`
  if (sec < 3600) return `${Math.floor(sec / 60)}m${sec % 60}s`
  return `${Math.floor(sec / 3600)}h${Math.floor((sec % 3600) / 60)}m`
}
const paramLabel = (k: string) => {
  const m: Record<string, string> = {
    adapter: '适配器', fastMode: '快速模式', defaultMode: '默认模式',
    omnigentTimeoutMs: 'Omnigent 超时(ms)', pollIntervalMs: '轮询间隔(ms)'
  }
  return m[k] || k
}
const eventTagType = (s?: string) => {
  if (!s) return 'info'
  const u = s.toLowerCase()
  if (u.includes('fail') || u.includes('error') || u === 'failed') return 'danger'
  if (u.includes('run') || u === 'running') return 'warning'
  if (u === 'done' || u === 'completed') return 'success'
  return 'primary'
}
const kpiCards = computed(() => {
  const k = kpis.value
  const card = (key: string, label: string, color: string, fmt?: (v: any) => string) => ({
    key, label, color, value: fmt ? fmt(k[key]) : (k[key] ?? 0)
  })
  return [
    card('running', '执行中', '#e6a23c'),
    card('queued', '待执行', '#909399'),
    card('todayDone', '今日完成', '#67c23a'),
    card('failureRate', '失败率', '#f56c6c', (v) => v == null ? '—' : `${v}`),
    card('avgDurationSec', '平均时长', '#409eff', (v) => formatDuration(typeof v === 'number' ? v : Number(v))),
    card('agentUtilization', 'Agent 利用率', '#9c27b0', (v) => v == null ? '—' : `${v}`)
  ]
})
const boardCount = computed(() => board.value.reduce((s, c) => s + (c.items?.length || 0), 0))
const typeLabel = (t: string) => {
  const m: Record<string, string> = {
    MY_TODO: '待办', AGENT_FAILURE: 'Agent失败', DCP_TR: 'DCP/TR',
    EVIDENCE_GAP: '证据缺口', SYNC_FAILURE: '同步失败', BLOCKED_FLOW: '阻断流程'
  }
  return m[t] || t
}
const severityType = (s?: string) => {
  if (!s) return 'info'
  const u = s.toUpperCase()
  if (['P0', 'CRITICAL'].includes(u)) return 'danger'
  if (['P1', 'WARN'].includes(u)) return 'warning'
  return 'info'
}
const intentType = (i?: string) => (i === 'UNKNOWN' ? 'info' : 'warning')
const resultType = (s?: string) => {
  if (s === 'SUCCESS' || s === 'IDEMPOTENT') return 'success'
  if (s === 'NOT_SUPPORTED') return 'info'
  return 'error'
}
const copyText = (t?: string) => { if (t) { navigator.clipboard?.writeText(t); ElMessage.success('已复制') } }
</script>

<style scoped lang="scss">
.spk-ipd-workbench {
  padding: 0 0 10px;
}
.stat-row, .member-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}
.member-grid {
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
}
.stat-card, .kpi-card, .member-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  &__value { font-size: 24px; font-weight: 700; line-height: 1.2; }
  &__label { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
  &.is-clickable { cursor: pointer; &:hover { border-color: var(--el-color-danger); } }
}
.member-card {
  text-align: left;
  cursor: pointer;
  &:hover { box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08); }
}
.task-group {
  margin-bottom: 12px;
  &__head {
    font-size: 13px;
    padding: 6px 8px;
    background: var(--el-fill-color-light);
    border-radius: 4px;
    margin-bottom: 6px;
    display: flex;
    align-items: center;
  }
}
:deep(.task-row) { cursor: pointer; }
.inbox-card {
  :deep(.el-card__body) { max-height: 600px; overflow-y: auto; }
}
.inbox-item {
  padding: 10px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
  &:last-child { border-bottom: none; }
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ellipsis2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.board-cols {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 6px;
}
.board-col {
  flex: 0 0 220px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
  padding: 8px;
  &__head {
    font-weight: 600; font-size: 13px; margin-bottom: 8px;
    display: flex; align-items: center; justify-content: space-between;
  }
}
.board-item {
  background: var(--el-bg-color);
  border-radius: 6px;
  padding: 8px;
  margin-bottom: 8px;
  border-left: 3px solid var(--el-color-success);
  &.is-failed { border-left-color: var(--el-color-danger); }
  &.is-running { border-left-color: var(--el-color-warning); }
  &.is-queued { border-left-color: var(--el-color-info); }
}
.command-preview {
  background: var(--el-fill-color-light);
  border-radius: 6px;
  padding: 10px;
}
.task-detail {
  padding: 0 4px;
}
.omni-frame {
  width: 100%;
  height: 520px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
}
.sse-log {
  margin-top: 8px;
  max-height: 160px;
  overflow-y: auto;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 4px;
  padding: 8px;
  font-family: 'Consolas', monospace;
  font-size: 12px;
  &__line { line-height: 1.6; word-break: break-all; }
}
</style>
