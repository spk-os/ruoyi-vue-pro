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
  VERIFYING: { color: 'yellow', text: '验证中' }
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
