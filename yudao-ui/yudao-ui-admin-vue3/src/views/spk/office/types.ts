// 指挥台（Office）可视化类型定义
// 由 spk_agent_def 派生，替代 Paddock 的 React Agent 类型

/** 智能体状态：offline 离线 / idle 空闲 / busy 忙碌 / error 异常 */
export type AgentStatus = 'offline' | 'idle' | 'busy' | 'error'

/** Office 视图模式：office 平面图 / org-chart 组织架构 */
export type ViewMode = 'office' | 'org-chart'

/** 组织架构分段：category 类别 / role 角色 / status 状态 */
export type OrgSegmentMode = 'category' | 'role' | 'status'

/** 侧栏过滤：all 全部 / working 工作中 / idle 空闲 / attention 需关注 */
export type SidebarFilter = 'all' | 'working' | 'idle' | 'attention'

/** 时间主题：dawn 黎明 / day 白昼 / dusk 黄昏 / night 夜晚 */
export type TimeTheme = 'dawn' | 'day' | 'dusk' | 'night'

/** 智能体动作：focus 专注 / pair 结对 / break 休息 */
export type OfficeAction = 'focus' | 'pair' | 'break'

/** 热点类型：room 房间 / desk 工位 */
export type HotspotKind = 'room' | 'desk'

/** Office 智能体（由 AgentDefVO 映射派生） */
export interface OfficeAgent {
  id: number
  name: string
  role: string
  status: AgentStatus
  /** 最近活动时间（秒级时间戳，0 表示未知） */
  lastSeen: number
  /** 最近活动描述 */
  lastActivity: string
  /** 会话/编码标识 */
  sessionKey: string
  model?: string
  runtimeType?: string
}

/** 工位锚点 */
export interface WorkstationAnchor {
  deskId: string
  seatLabel: string
  row: number
  col: number
  x: number
  y: number
}

/** 分区后的智能体 */
export interface ZonedAgent {
  agent: OfficeAgent
  anchor: WorkstationAnchor
}

/** 分区布局 */
export interface OfficeZoneLayout {
  zone: OfficeZoneDefinition
  workers: ZonedAgent[]
}

export interface OfficeZoneDefinition {
  id: string
  label: string
  icon: string
  accentClass: string
  roleKeywords: string[]
}

/** 座位坐标 */
export interface SeatPosition {
  seatKey: string
  x: number
  y: number
}

/** 移动中的 worker */
export interface MovingWorker {
  id: string
  agentId: number
  initials: string
  colorClass: string
  startX: number
  startY: number
  endX: number
  endY: number
  startedAt: number
  durationMs: number
  progress: number
  path: Array<{ x: number; y: number }>
  pathLengths: number[]
  totalLength: number
  destinationTile: string
}

/** 渲染 worker */
export interface RenderedWorker {
  agent: OfficeAgent
  x: number
  y: number
  zoneLabel: string
  seatLabel: string
  isMoving: boolean
  direction: { dx: number; dy: number }
  variant: WorkerVariant
}

/** worker 视觉变体 */
export interface WorkerVariant {
  id: string
  filter: string
  accent: string
}

/** 地图房间 */
export interface MapRoom {
  id: string
  label: string
  x: number
  y: number
  w: number
  h: number
  style: string
}

/** 地图道具/家具 */
export interface MapProp {
  id: string
  x: number
  y: number
  w: number
  h: number
  style: string
  border: string
}

/** 热点（点击房间/工位后选中） */
export interface OfficeHotspot {
  kind: HotspotKind
  id: string
  label: string
  x: number
  y: number
  stats: string[]
}

/** 事件流条目 */
export interface OfficeEvent {
  id: string
  kind: 'action' | 'room' | 'desk'
  message: string
  at: number
  severity: 'info' | 'warn' | 'good'
}

/** 工作提示浮标 */
export interface WorkCue {
  label: string
  detail: string
  toneClass: string
  glowColor: string
}

/** 投递数据包（SVG 动画） */
export interface DeliveryPacket {
  id: string
  fromX: number
  fromY: number
  midX: number
  midY: number
  toX: number
  toY: number
  delay: number
  duration: number
  color: string
  label: string
}

/** 房间活动统计 */
export interface RoomActivity {
  total: number
  busy: number
  idle: number
  alerts: number
  taskLoad: number
}

/** 主题调色板 */
export interface ThemePalette {
  shell: string
  gridLine: string
  haze: string
  glow: string
  corridor: string
  corridorStripe: string
  atmosphere: string
  shadowVeil: string
  floorFilter: string
  spriteFilter: string
  roomTone: string
  floorOpacityA: number
  floorOpacityB: number
  accentGlow: string
}

/** 持久化偏好（localStorage） */
export interface PersistedOfficePrefs {
  version: 1
  viewMode: ViewMode
  sidebarFilter: SidebarFilter
  mapZoom: number
  mapPan: { x: number; y: number }
  timeTheme: TimeTheme
  showSidebar: boolean
  showMinimap: boolean
  showEvents: boolean
  roomLayout: MapRoom[]
  mapProps: MapProp[]
}

/** Toast 提示 */
export interface LaunchToast {
  kind: 'success' | 'info' | 'error'
  title: string
  detail: string
}
