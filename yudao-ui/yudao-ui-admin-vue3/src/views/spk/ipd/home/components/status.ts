/**
 * SPK-OS 研发驾驶舱 — 状态色映射字典
 * 来源：原型 shared/scripts.js 的 healthBadge/statusBadge/typeBadge/severityLabel 4 函数。
 * 设计文档 §4 / D-04：统一 7 色语义，Element 内置 type 无法直接覆盖，故自研 SpkBadge 查表。
 */

// 7 色语义键 → Element tag type 兼容映射（仅用于必须用 el-tag 的场景）
export type SpkColor =
  | 'green' | 'yellow' | 'orange' | 'red' | 'blue' | 'purple' | 'gray'

export interface BadgeMeta {
  color: SpkColor
  text: string
}

// 项目健康度
export const healthMap: Record<string, BadgeMeta> = {
  'on-track': { color: 'green', text: '正常' },
  'at-risk': { color: 'yellow', text: '有风险' },
  blocked: { color: 'red', text: '阻塞' },
  waiting: { color: 'yellow', text: '待处理' },
  delivered: { color: 'green', text: '已交付' },
  completed: { color: 'green', text: '已完成' },
  GOOD: { color: 'green', text: '正常' },
  WARN: { color: 'yellow', text: '有风险' },
  CRITICAL: { color: 'red', text: '阻塞' }
}

// 运行/流程/活动状态
export const statusMap: Record<string, BadgeMeta> = {
  RUNNING: { color: 'blue', text: '运行中' },
  COMPLETED: { color: 'green', text: '已完成' },
  BLOCKED: { color: 'red', text: '阻塞' },
  FAILED: { color: 'red', text: '失败' },
  WAITING_APPROVAL: { color: 'yellow', text: '待审批' },
  DRAFT: { color: 'gray', text: '草稿' },
  QUEUED: { color: 'gray', text: '排队中' },
  IDLE: { color: 'gray', text: '空闲' },
  ACTIVE: { color: 'green', text: '活跃' },
  ERROR: { color: 'red', text: '错误' },
  AWAITING_DECISION: { color: 'yellow', text: '待决策' },
  DELIVERED: { color: 'green', text: '已交付' },
  IN_PROGRESS: { color: 'blue', text: '进行中' },
  PENDING: { color: 'gray', text: '待处理' },
  PLANNING: { color: 'purple', text: '规划中' },
  ARCHIVED: { color: 'gray', text: '已归档' },
  CANCELLED: { color: 'gray', text: '已取消' },
  RELEASED: { color: 'green', text: '已发布' },
  VERIFYING: { color: 'yellow', text: '验证中' },
  // Activity 任务态（小写枚举：spk_task_contract.status，与上面流程态大写键区分）
  queued: { color: 'gray', text: '排队中' },
  running: { color: 'blue', text: '执行中' },
  done: { color: 'green', text: '完成' },
  failed: { color: 'red', text: '失败' },
  timeout: { color: 'orange', text: '超时' },
  cancelled: { color: 'gray', text: '已取消' },
  // Agent 运行态（小写：spk_agent_def.status）
  idle: { color: 'gray', text: '空闲' },
  busy: { color: 'yellow', text: '繁忙' }
}

// 版本/流程类型
export const typeMap: Record<string, BadgeMeta> = {
  FULL: { color: 'blue', text: '全量' },
  INCREMENT: { color: 'purple', text: '增量' },
  ISSUE: { color: 'orange', text: '问题' },
  HOTFIX: { color: 'orange', text: '热修复' },
  BASELINE: { color: 'green', text: '基线' }
}

// 严重度
export const severityMap: Record<string, BadgeMeta> = {
  critical: { color: 'red', text: 'P0 紧急' },
  P0: { color: 'red', text: 'P0 紧急' },
  CRITICAL: { color: 'red', text: 'P0 紧急' },
  high: { color: 'orange', text: 'P1 高' },
  P1: { color: 'orange', text: 'P1 高' },
  WARN: { color: 'orange', text: 'P1 高' },
  medium: { color: 'yellow', text: 'P2 中' },
  P2: { color: 'yellow', text: 'P2 中' },
  low: { color: 'green', text: 'P3 低' },
  P3: { color: 'green', text: 'P3 低' }
}

// 集成健康
export const integrationMap: Record<string, BadgeMeta> = {
  HEALTHY: { color: 'green', text: '正常' },
  DEGRADED: { color: 'yellow', text: '降级' },
  DOWN: { color: 'red', text: '离线' }
}

// 风险等级
export const riskMap: Record<string, BadgeMeta> = {
  L0: { color: 'gray', text: 'L0 只读' },
  L1: { color: 'blue', text: 'L1 可逆' },
  L2: { color: 'orange', text: 'L2 运行变更' },
  L3: { color: 'red', text: 'L3 治理决策' }
}

// IPD 阶段（spk_activity_stage 枚举，替代 concept/plan 等英文裸露）
export const stageMap: Record<string, BadgeMeta> = {
  concept: { color: 'blue', text: '概念' },
  plan: { color: 'purple', text: '计划' },
  develop: { color: 'blue', text: '开发' },
  qualify: { color: 'yellow', text: '验证' },
  launch: { color: 'green', text: '发布' },
  lifecycle: { color: 'gray', text: '生命周期' },
  support: { color: 'gray', text: '支撑' },
  unknown: { color: 'gray', text: '未归类' }
}

// 流程类型（flowType，替代 FULL_RELEASE 等英文裸露；含 typeMap 的短键兼容）
export const flowTypeMap: Record<string, BadgeMeta> = {
  FULL_RELEASE: { color: 'blue', text: '完整发布' },
  INCREMENT_RELEASE: { color: 'purple', text: '增量发布' },
  ISSUE_RESOLUTION: { color: 'orange', text: '问题解决' },
  INTEGRATED_CROSS_VALIDATION: { color: 'yellow', text: '集成交叉验证' },
  FULL: { color: 'blue', text: '全量' },
  INCREMENT: { color: 'purple', text: '增量' },
  ISSUE: { color: 'orange', text: '问题' },
  HOTFIX: { color: 'orange', text: '热修复' },
  BASELINE: { color: 'green', text: '基线' }
}

// 产物类型（artifactType，落盘文件名一部分——主显中文，原文需可追溯见 tooltip）
// 覆盖 spk_artifact_manifest / spk_ipd_activity_def.output_artifact_type 全量枚举
export const artifactTypeMap: Record<string, BadgeMeta> = {
  'req-insight-report': { color: 'blue', text: '需求洞察报告' },
  'concept-options-set': { color: 'blue', text: '概念选项集' },
  'feasibility-report': { color: 'blue', text: '可行性分析报告' },
  'business-case': { color: 'blue', text: '商业案例' },
  'concept-decision-brief': { color: 'blue', text: '概念决策简报' },
  'prs-baseline': { color: 'green', text: '需求基线' },
  'arch-baseline': { color: 'green', text: '架构基线' },
  'test-strategy': { color: 'yellow', text: '测试策略' },
  'project-plan': { color: 'purple', text: '项目计划' },
  'schedule-plan': { color: 'purple', text: '进度计划' },
  'architecture-design-doc': { color: 'blue', text: '架构设计文档' },
  'detail-design-doc': { color: 'blue', text: '详细设计文档' },
  'dev-doc': { color: 'blue', text: '开发文档' },
  'risk-assessment': { color: 'orange', text: '风险评估' },
  'ip-strategy': { color: 'gray', text: '知识产权策略' },
  'code-package': { color: 'blue', text: '代码包' },
  'code-review-report': { color: 'yellow', text: '代码评审报告' },
  'unit-test-report': { color: 'yellow', text: '单元测试报告' },
  'integration-build': { color: 'blue', text: '集成构建' },
  'integration-test-report': { color: 'yellow', text: '集成测试报告' },
  'system-test-report': { color: 'yellow', text: '系统测试报告' },
  'perf-test-report': { color: 'yellow', text: '性能测试报告' },
  'security-test-report': { color: 'orange', text: '安全测试报告' },
  'beta-test-report': { color: 'yellow', text: 'Beta 测试报告' },
  'launch-plan': { color: 'green', text: '发布计划' },
  'release-package': { color: 'green', text: '发布包' },
  'deployment-record': { color: 'green', text: '部署记录' },
  'ga-signoff': { color: 'green', text: 'GA 签收' },
  'ops-feedback-report': { color: 'gray', text: '运营反馈报告' },
  'ops-monitoring-report': { color: 'gray', text: '运营监控报告' },
  'improvement-plan': { color: 'gray', text: '改进计划' }
}

// 证据类型
export const evidenceTypeMap: Record<string, BadgeMeta> = {
  context_manifest: { color: 'gray', text: '上下文清单' },
  artifact_signed: { color: 'gray', text: '产物签署' },
  run_receipt: { color: 'gray', text: '执行回执' },
  verification: { color: 'gray', text: '验证回执' },
  gate_decision: { color: 'gray', text: '门禁决策' }
}

// 验证器类型（verifierType，未知回落原文）
export const verifierTypeMap: Record<string, BadgeMeta> = {
  TR: { color: 'blue', text: '技术评审' },
  TR1: { color: 'blue', text: '技术评审 TR1' },
  TR2: { color: 'blue', text: '技术评审 TR2' },
  TR3: { color: 'blue', text: '技术评审 TR3' },
  TR4: { color: 'blue', text: '技术评审 TR4' },
  TR5: { color: 'blue', text: '技术评审 TR5' },
  TR6: { color: 'blue', text: '技术评审 TR6' },
  RE: { color: 'blue', text: '需求评审' },
  SEC: { color: 'orange', text: '安全评审' }
}

// 执行位置 / 执行模式（executionLocation，连字符与下划线两种键均覆盖）
export const executionLocationMap: Record<string, BadgeMeta> = {
  task_system: { color: 'gray', text: '任务系统派发' },
  'task-system': { color: 'gray', text: '任务系统派发' },
  lead_internal: { color: 'blue', text: 'Lead 内部执行' },
  'lead-internal': { color: 'blue', text: 'Lead 内部执行' },
  omnigent: { color: 'purple', text: 'Omnigent 执行' },
  'omnigent-sandbox': { color: 'purple', text: 'Omnigent 沙箱执行' },
  native_ai: { color: 'blue', text: '本地 AI 执行' },
  'native-ai': { color: 'blue', text: '本地 AI 运行时' }
}

// 智能体类别（agentKind）
export const agentKindMap: Record<string, BadgeMeta> = {
  lead: { color: 'purple', text: '主控' },
  LEAD: { color: 'purple', text: '主控' },
  verifier: { color: 'blue', text: '验证器' },
  VERIFIER: { color: 'blue', text: '验证器' }
}

// 产物签署状态（spk_artifact_manifest.status）
export const artifactStatusMap: Record<string, BadgeMeta> = {
  signed: { color: 'green', text: '已签署' },
  quarantined: { color: 'red', text: '已隔离' },
  pending: { color: 'gray', text: '待签署' },
  draft: { color: 'gray', text: '草稿' }
}

// 纯文本标签辅助：取 map 中文文案，未知回落原 key（用于非徽章场景：折叠标题/表格列/副文本）
export const labelText = (
  map: Record<string, BadgeMeta>,
  key?: string,
  fallback?: string
): string => {
  if (key && map[key]) return map[key].text
  return fallback || key || '—'
}

// color → CSS 变量（前景/背景/边框），与原型 styles.css 状态色一致
export const colorVars: Record<SpkColor, { fg: string; bg: string; border: string }> = {
  green: { fg: '#116329', bg: '#dafbe1', border: '#4ac26b' },
  yellow: { fg: '#6e5b02', bg: '#fff8c5', border: '#d4a72c' },
  orange: { fg: '#953800', bg: '#fff1e5', border: '#fb8f44' },
  red: { fg: '#a40e26', bg: '#ffebe9', border: '#ff8182' },
  blue: { fg: '#0550ae', bg: '#ddf4ff', border: '#54aeff' },
  purple: { fg: '#6639ba', bg: '#fbefff', border: '#c297fc' },
  gray: { fg: '#656d76', bg: '#f6f8fa', border: '#d0d7de' }
}

export const resolveBadge = (
  map: Record<string, BadgeMeta>,
  key?: string,
  fallbackText?: string
): BadgeMeta => {
  if (key && map[key]) return map[key]
  return { color: 'gray', text: fallbackText || key || '—' }
}
