// 指挥台常量 + 纯函数 helpers（移植自 Paddock office-panel.tsx，去 React 依赖）
import type {
  AgentStatus,
  MapProp,
  MapRoom,
  OfficeAgent,
  OfficeAction,
  RenderedWorker,
  RoomActivity,
  WorkerVariant,
  WorkCue
} from './types'

export const MAP_COLS = 24
export const MAP_ROWS = 16
export const HERO_SHEET_COLS = 6
export const HERO_SHEET_ROWS = 7

// 中文字符串（硬编码，与既有 spk agent 页一致）
export const TXT = {
  title: '智能体拓扑大屏',
  subtitle: '只读视图 · 展示智能体编队实时状态，不承担命令（点节点查看运行负载）',
  mainDeck: '主甲板',
  resetView: '重置视图',
  resetLayout: '恢复布局',
  showCrewButton: '显示编队',
  hideCrewButton: '隐藏编队',
  showRadarButton: '显示雷达',
  hideRadarButton: '隐藏雷达',
  showLogButton: '显示日志',
  hideLogButton: '隐藏日志',
  crewHeader: 'CREW',
  onlineCount: '在线 {count}',
  filterAll: '全部',
  filterWorking: '工作中',
  filterIdle: '空闲',
  filterNeedsAttention: '需关注',
  filterRunning: '运行中',
  filterNotRunning: '未运行',
  noRecentActivity: '无近期活动',
  activeStatus: '活动中',
  idleMinutes: '{minutes}m 空闲',
  noWorkersInFilter: '该过滤条件下无成员',
  radarLabel: 'RADAR',
  deckLog: 'DECK LOG',
  legendActive: '活动',
  legendStandby: '待命',
  legendOther: '其他',
  noEventsYet: '暂无事件',
  moving: '移动中',
  hotspotUp: '上',
  hotspotDown: '下',
  hotspotLeft: '左',
  hotspotRight: '右',
  hotspotFineMinusX: '微-',
  hotspotFinePlusX: '微+',
  hotspotWider: '加宽',
  hotspotNarrower: '变窄',
  hotspotTaller: '加高',
  hotspotShorter: '变矮',
  segmentedBy: '分段方式',
  segmentCategory: '类别',
  segmentRole: '角色',
  segmentStatus: '状态',
  emptyDeck: '当前没有可显示的智能体',
  emptyDeckSubtitle: '请在「SPK 研发」→「智能体」中新增智能体后再回到指挥台',
  loadingOffice: '正在加载指挥台…',
  buttonDeck: '平面图',
  buttonCrewChart: '组织架构',
  refresh: '刷新',
  activeCount: '活动 {count}',
  standbyCount: '待命 {count}',
  alertCount: '告警 {count}',
  offlineCount: '离线 {count}',
  currentActivity: '当前活动',
  sessionLabel: '会话/编码:',
  quickActions: '快速动作',
  actionFocus: '专注',
  actionPair: '结对',
  actionBreak: '休息',
  neverSeen: '从未活动',
  justNow: '刚刚',
  minutesAgo: '{minutes}分钟前',
  hoursAgo: '{hours}小时前',
  daysAgo: '{days}天前'
} as const

export function formatText(tpl: string, values: Record<string, string | number>): string {
  return tpl.replace(/\{(\w+)\}/g, (_m, k) => String(values[k] ?? ''))
}

// ---- 状态色板（用标准 tailwind 色，替代 Paddock 的 void-* 自定义色） ----
export const statusGlow: Record<string, string> = {
  idle: 'shadow-[0_0_12px_rgba(52,211,153,0.3)] border-emerald-400/60',
  busy: 'shadow-[0_0_12px_rgba(251,191,36,0.3)] border-amber-400/60',
  error: 'shadow-[0_0_12px_rgba(244,63,94,0.3)] border-rose-500/60',
  offline: 'shadow-[0_0_8px_rgba(148,163,184,0.2)] border-slate-500/40'
}

export const statusDot: Record<string, string> = {
  idle: 'bg-emerald-400',
  busy: 'bg-amber-400',
  error: 'bg-rose-500',
  offline: 'bg-slate-400/60'
}

export const statusLabel: Record<string, string> = {
  idle: '待命',
  busy: '活动中',
  error: '告警',
  offline: '离线'
}

export function getInitials(name: string): string {
  return name
    .split(/[\s_-]+/)
    .filter(Boolean)
    .map((w) => w[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

const HASH_COLORS = [
  'bg-blue-600', 'bg-emerald-600', 'bg-violet-600', 'bg-amber-600',
  'bg-rose-600', 'bg-cyan-600', 'bg-indigo-600', 'bg-teal-600',
  'bg-orange-600', 'bg-pink-600', 'bg-lime-600', 'bg-fuchsia-600'
]

export function hashColor(name: string): string {
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return HASH_COLORS[Math.abs(hash) % HASH_COLORS.length]
}

export function hashNumber(value: string): number {
  let hash = 0
  for (let i = 0; i < value.length; i += 1) {
    hash = value.charCodeAt(i) + ((hash << 5) - hash)
  }
  return Math.abs(hash)
}

export function formatLastSeen(ts?: number): string {
  if (!ts) return TXT.neverSeen
  const diff = Date.now() - ts * 1000
  const m = Math.floor(diff / 60000)
  if (m < 1) return TXT.justNow
  if (m < 60) return formatText(TXT.minutesAgo, { minutes: m })
  const h = Math.floor(m / 60)
  if (h < 24) return formatText(TXT.hoursAgo, { hours: h })
  return formatText(TXT.daysAgo, { days: Math.floor(h / 24) })
}

export function easeInOut(progress: number): number {
  if (progress <= 0) return 0
  if (progress >= 1) return 1
  return progress < 0.5
    ? 2 * progress * progress
    : 1 - Math.pow(-2 * progress + 2, 2) / 2
}

export function getStatusEmote(status: AgentStatus): string {
  if (status === 'busy') return '●' // filled circle
  if (status === 'idle') return '○' // open circle
  if (status === 'error') return '▲' // triangle
  return '–' // dash
}

// OfficeAgent 无 taskStats，任务负载恒为 0；保留函数签名以便 delivery/heatmap 逻辑复用
export function getAgentTaskLoad(_agent: OfficeAgent): number {
  return 0
}

// ---- 地图常量 ----
export const ROOM_LAYOUT: MapRoom[] = [
  { id: 'eng', label: '工程舱', x: 16, y: 22, w: 28, h: 22, style: 'bg-[#0c1628]' },
  { id: 'product', label: '产品舱', x: 48, y: 22, w: 24, h: 22, style: 'bg-[#0a1a2a]' },
  { id: 'ops', label: '运维舱', x: 16, y: 49, w: 24, h: 24, style: 'bg-[#10132a]' },
  { id: 'research', label: '实验室', x: 44, y: 49, w: 22, h: 24, style: 'bg-[#0d1526]' },
  { id: 'lounge', label: '船员舱', x: 70, y: 49, w: 16, h: 24, style: 'bg-[#0c1a1a]' }
]

export const MAP_PROPS: MapProp[] = [
  { id: 'desk-a', x: 22, y: 30, w: 8, h: 2.8, style: 'bg-[#0f1c30]', border: 'border-cyan-400/25' },
  { id: 'desk-b', x: 33, y: 30, w: 8, h: 2.8, style: 'bg-[#0f1c30]', border: 'border-cyan-400/25' },
  { id: 'desk-c', x: 52, y: 30, w: 8, h: 2.8, style: 'bg-[#0f1c30]', border: 'border-cyan-400/25' },
  { id: 'desk-d', x: 61, y: 30, w: 8, h: 2.8, style: 'bg-[#0f1c30]', border: 'border-cyan-400/25' },
  { id: 'desk-e', x: 22, y: 58, w: 8, h: 2.8, style: 'bg-[#0f1c30]', border: 'border-cyan-400/25' },
  { id: 'desk-f', x: 31, y: 58, w: 8, h: 2.8, style: 'bg-[#0f1c30]', border: 'border-cyan-400/25' },
  { id: 'desk-g', x: 48, y: 58, w: 8, h: 2.8, style: 'bg-[#0f1c30]', border: 'border-cyan-400/25' },
  { id: 'desk-h', x: 57, y: 58, w: 8, h: 2.8, style: 'bg-[#0f1c30]', border: 'border-cyan-400/25' },
  { id: 'plant-l', x: 14, y: 47, w: 3, h: 5, style: 'bg-emerald-400/30', border: 'border-emerald-400/20' },
  { id: 'plant-r', x: 84, y: 47, w: 3, h: 5, style: 'bg-emerald-400/30', border: 'border-emerald-400/20' },
  { id: 'kitchen', x: 72, y: 57, w: 12, h: 10, style: 'bg-[#0c1a1a]', border: 'border-emerald-400/20' }
]

export const LOUNGE_WAYPOINTS = [
  { x: 74, y: 60 },
  { x: 79, y: 60 },
  { x: 82, y: 66 },
  { x: 76, y: 68 }
]

export const DELIVERY_TERMINALS = [
  { id: 'queue', label: '任务队列', x: 42, y: 47.8, color: 'rgba(34,211,238,0.9)', border: 'border-cyan-300/30' },
  { id: 'review', label: '审核', x: 58, y: 47.8, color: 'rgba(167,139,250,0.9)', border: 'border-violet-300/30' },
  { id: 'owner', label: '负责', x: 78, y: 35, color: 'rgba(52,211,153,0.9)', border: 'border-emerald-300/30' }
] as const

export function getAgentWorkCue(agent: OfficeAgent, action?: OfficeAction): WorkCue {
  const activeTasks = getAgentTaskLoad(agent)
  if (action === 'focus') {
    return { label: '专注', detail: '深度工作', toneClass: 'text-cyan-100 border-cyan-300/35 bg-cyan-400/15', glowColor: 'rgba(34,211,238,0.6)' }
  }
  if (action === 'pair') {
    return { label: '结对', detail: '同步中', toneClass: 'text-violet-100 border-violet-300/35 bg-violet-400/15', glowColor: 'rgba(167,139,250,0.6)' }
  }
  if (action === 'break') {
    return { label: '休息', detail: '离开', toneClass: 'text-emerald-100 border-emerald-300/35 bg-emerald-400/15', glowColor: 'rgba(52,211,153,0.5)' }
  }
  if (agent.status === 'error') {
    return { label: '阻塞', detail: '需协助', toneClass: 'text-rose-100 border-rose-300/35 bg-rose-400/15', glowColor: 'rgba(244,63,94,0.65)' }
  }
  if (agent.status === 'busy') {
    return { label: '录入', detail: activeTasks > 0 ? `${activeTasks} 任务` : '工作中', toneClass: 'text-amber-100 border-amber-300/35 bg-amber-400/15', glowColor: 'rgba(251,191,36,0.65)' }
  }
  if (agent.status === 'offline') {
    return { label: '离开', detail: '离线', toneClass: 'text-slate-200 border-slate-400/20 bg-slate-500/15', glowColor: 'rgba(148,163,184,0.35)' }
  }
  return { label: '待命', detail: activeTasks > 0 ? `${activeTasks} 排队` : '就绪', toneClass: 'text-emerald-100 border-emerald-300/30 bg-emerald-400/10', glowColor: 'rgba(52,211,153,0.45)' }
}

// ---- 房间活动统计 ----
export function workerIsInsideRoom(room: MapRoom, worker: Pick<RenderedWorker, 'x' | 'y'>): boolean {
  return (
    worker.x >= room.x &&
    worker.x <= room.x + room.w &&
    worker.y >= room.y &&
    worker.y <= room.y + room.h
  )
}

export function getRoomActivity(room: MapRoom, workers: RenderedWorker[]): RoomActivity {
  const occupants = workers.filter((worker) => workerIsInsideRoom(room, worker))
  return occupants.reduce<RoomActivity>(
    (acc, worker) => {
      acc.total += 1
      if (worker.agent.status === 'busy') acc.busy += 1
      else if (worker.agent.status === 'idle') acc.idle += 1
      else if (worker.agent.status === 'error') acc.alerts += 1
      acc.taskLoad += getAgentTaskLoad(worker.agent)
      return acc
    },
    { total: 0, busy: 0, idle: 0, alerts: 0, taskLoad: 0 }
  )
}

export function getPropSprite(propId: string): string {
  if (propId === 'desk-a' || propId === 'desk-b' || propId === 'desk-e' || propId === 'desk-f') return '/office-sprites/kenney/desk.png'
  if (propId.startsWith('desk-')) return '/office-sprites/kenney/tableCross.png'
  if (propId === 'plant-l') return '/office-sprites/kenney/plantSmall1.png'
  if (propId === 'plant-r') return '/office-sprites/kenney/plantSmall2.png'
  if (propId === 'kitchen') return '/office-sprites/kenney/rugRectangle.png'
  return ''
}

export function getWorkerHeroFrame(status: AgentStatus, isMoving: boolean, frame: number) {
  const phase = frame % 2
  const walkCol = phase === 0 ? 1 : 3
  if (isMoving) return { col: walkCol, row: 3 } // 侧步行
  if (status === 'busy') return { col: walkCol, row: 0 } // 前向循环作为录入代理
  if (status === 'error') return { col: 5, row: 6 }
  return { col: phase === 0 ? 0 : 5, row: 0 } // 空闲脉动
}

const WORKER_VARIANTS: WorkerVariant[] = [
  { id: 'default', filter: 'none', accent: 'border-cyan-300/60' },
  { id: 'warm', filter: 'hue-rotate(18deg) saturate(1.08)', accent: 'border-amber-300/60' },
  { id: 'cool', filter: 'hue-rotate(-20deg) saturate(1.1)', accent: 'border-sky-300/60' },
  { id: 'mint', filter: 'hue-rotate(42deg) saturate(1.08)', accent: 'border-emerald-300/60' },
  { id: 'violet', filter: 'hue-rotate(64deg) saturate(1.12)', accent: 'border-violet-300/60' }
]

export function getWorkerVariant(name: string): WorkerVariant {
  return WORKER_VARIANTS[hashNumber(name) % WORKER_VARIANTS.length]
}

// ---- 网格/寻路纯函数 ----
export function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value))
}

export function toTile(xPercent: number, yPercent: number) {
  const col = clamp(Math.round((xPercent / 100) * (MAP_COLS - 1)), 0, MAP_COLS - 1)
  const row = clamp(Math.round((yPercent / 100) * (MAP_ROWS - 1)), 0, MAP_ROWS - 1)
  return { col, row }
}

export function tileToPercent(col: number, row: number) {
  const x = (col / (MAP_COLS - 1)) * 100
  const y = (row / (MAP_ROWS - 1)) * 100
  return { x, y }
}

export function tileKey(col: number, row: number): string {
  return `${col},${row}`
}

export function buildWalkabilityGrid() {
  const walkable: boolean[][] = Array.from({ length: MAP_ROWS }, () =>
    Array.from({ length: MAP_COLS }, () => true)
  )
  // 边界墙
  for (let r = 0; r < MAP_ROWS; r += 1) {
    walkable[r][0] = false
    walkable[r][MAP_COLS - 1] = false
  }
  for (let c = 0; c < MAP_COLS; c += 1) {
    walkable[0][c] = false
    walkable[MAP_ROWS - 1][c] = false
  }
  // 家具/障碍格阻挡，让路径优先走走廊
  const obstacleRects = [
    { c1: 5, c2: 8, r1: 4, r2: 5 },
    { c1: 9, c2: 12, r1: 4, r2: 5 },
    { c1: 13, c2: 16, r1: 4, r2: 5 },
    { c1: 17, c2: 20, r1: 4, r2: 5 },
    { c1: 5, c2: 8, r1: 9, r2: 10 },
    { c1: 9, c2: 12, r1: 9, r2: 10 },
    { c1: 13, c2: 16, r1: 9, r2: 10 },
    { c1: 17, c2: 20, r1: 10, r2: 13 }
  ]
  for (const rect of obstacleRects) {
    for (let r = rect.r1; r <= rect.r2; r += 1) {
      for (let c = rect.c1; c <= rect.c2; c += 1) {
        if (r >= 0 && r < MAP_ROWS && c >= 0 && c < MAP_COLS) walkable[r][c] = false
      }
    }
  }
  // 保持中央水平走廊畅通
  const corridorRow = 7
  for (let c = 1; c < MAP_COLS - 1; c += 1) walkable[corridorRow][c] = true
  return walkable
}

export function findGridPath(
  start: { col: number; row: number },
  end: { col: number; row: number },
  walkable: boolean[][]
) {
  const inBounds = (col: number, row: number) =>
    row >= 0 && row < MAP_ROWS && col >= 0 && col < MAP_COLS
  const key = (col: number, row: number) => `${col},${row}`
  const parse = (k: string) => {
    const [c, r] = k.split(',').map(Number)
    return { col: c, row: r }
  }

  const open = new Set<string>([key(start.col, start.row)])
  const cameFrom = new Map<string, string>()
  const gScore = new Map<string, number>([[key(start.col, start.row), 0]])
  const fScore = new Map<string, number>([
    [key(start.col, start.row), Math.abs(start.col - end.col) + Math.abs(start.row - end.row)]
  ])

  while (open.size > 0) {
    let currentKey = ''
    let lowest = Number.POSITIVE_INFINITY
    for (const k of open) {
      const f = fScore.get(k) ?? Number.POSITIVE_INFINITY
      if (f < lowest) {
        lowest = f
        currentKey = k
      }
    }
    if (!currentKey) break

    const current = parse(currentKey)
    if (current.col === end.col && current.row === end.row) {
      const path = [current]
      let ck = currentKey
      while (cameFrom.has(ck)) {
        ck = cameFrom.get(ck)!
        path.push(parse(ck))
      }
      path.reverse()
      return path
    }

    open.delete(currentKey)
    const neighbors = [
      { col: current.col + 1, row: current.row },
      { col: current.col - 1, row: current.row },
      { col: current.col, row: current.row + 1 },
      { col: current.col, row: current.row - 1 }
    ]

    for (const n of neighbors) {
      if (!inBounds(n.col, n.row)) continue
      if (!walkable[n.row][n.col]) continue
      const nk = key(n.col, n.row)
      const tentative = (gScore.get(currentKey) ?? Number.POSITIVE_INFINITY) + 1
      if (tentative >= (gScore.get(nk) ?? Number.POSITIVE_INFINITY)) continue
      cameFrom.set(nk, currentKey)
      gScore.set(nk, tentative)
      fScore.set(nk, tentative + Math.abs(n.col - end.col) + Math.abs(n.row - end.row))
      open.add(nk)
    }
  }

  return [start, end]
}

export function buildPath(
  startX: number,
  startY: number,
  endX: number,
  endY: number,
  blockedTiles: Set<string> = new Set()
) {
  const walkable = buildWalkabilityGrid()
  const startTile = toTile(startX, startY)
  const endTile = toTile(endX, endY)
  for (const tile of blockedTiles) {
    const [col, row] = tile.split(',').map(Number)
    if (!Number.isFinite(col) || !Number.isFinite(row)) continue
    if (row < 0 || row >= MAP_ROWS || col < 0 || col >= MAP_COLS) continue
    walkable[row][col] = false
  }
  // 起止格始终可通行
  walkable[startTile.row][startTile.col] = true
  walkable[endTile.row][endTile.col] = true
  const tilePath = findGridPath(startTile, endTile, walkable)
  const path = tilePath.map((tile) => tileToPercent(tile.col, tile.row))
  const pathLengths: number[] = [0]
  let totalLength = 0
  for (let i = 1; i < path.length; i += 1) {
    const dx = path[i].x - path[i - 1].x
    const dy = path[i].y - path[i - 1].y
    totalLength += Math.hypot(dx, dy)
    pathLengths.push(totalLength)
  }
  return { path, pathLengths, totalLength }
}

export function pointAlongPath(
  path: Array<{ x: number; y: number }>,
  pathLengths: number[],
  totalLength: number,
  progress: number
) {
  if (path.length === 0) return { x: 0, y: 0 }
  if (path.length === 1 || totalLength <= 0) return path[path.length - 1]
  const target = totalLength * clamp(progress, 0, 1)
  let idx = 1
  while (idx < pathLengths.length && pathLengths[idx] < target) idx += 1
  const prevIdx = Math.max(0, idx - 1)
  const prevLen = pathLengths[prevIdx] ?? 0
  const nextLen = pathLengths[Math.min(idx, pathLengths.length - 1)] ?? totalLength
  const local = nextLen > prevLen ? (target - prevLen) / (nextLen - prevLen) : 0
  const a = path[prevIdx]
  const b = path[Math.min(idx, path.length - 1)]
  return {
    x: a.x + (b.x - a.x) * local,
    y: a.y + (b.y - a.y) * local
  }
}
