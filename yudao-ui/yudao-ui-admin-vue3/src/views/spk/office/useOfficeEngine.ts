// 指挥台引擎 composable：座位分配 + 移动动画(rAF) + roam 循环 + 状态动画 + 事件流 + 热点编辑
// 移植自 Paddock office-panel.tsx 状态机段，React hooks → Vue Composition API
import { computed, onMounted, onUnmounted, ref, watch, type Ref } from 'vue'
import type {
  DeliveryPacket,
  LaunchToast,
  MapProp,
  MapRoom,
  MovingWorker,
  OfficeAction,
  OfficeAgent,
  OfficeEvent,
  OfficeHotspot,
  RenderedWorker,
  RoomActivity,
  SeatPosition,
  SidebarFilter,
  ViewMode
} from './types'
import { buildOfficeLayout } from './office-layout'
import {
  clamp,
  easeInOut,
  getAgentTaskLoad,
  getInitials,
  getWorkerVariant,
  hashColor,
  hashNumber,
  MAP_COLS,
  MAP_ROWS,
  MAP_PROPS,
  ROOM_LAYOUT,
  LOUNGE_WAYPOINTS,
  DELIVERY_TERMINALS,
  pointAlongPath,
  buildPath,
  toTile,
  tileKey,
  statusLabel
} from './office-config'

export function useOfficeEngine(displayAgents: Ref<OfficeAgent[]>) {
  // ---- 视图/交互状态 ----
  const viewMode = ref<ViewMode>('office')
  const sidebarFilter = ref<SidebarFilter>('all')
  const showSidebar = ref(true)
  const showMinimap = ref(true)
  const showEvents = ref(true)
  const mapZoom = ref(1)
  const mapPan = ref({ x: 0, y: 0 })
  const selectedAgent = ref<OfficeAgent | null>(null)
  const selectedHotspot = ref<OfficeHotspot | null>(null)
  const launchToast = ref<LaunchToast | null>(null)

  // ---- 引擎状态 ----
  const agentActionOverrides = ref<Map<number, OfficeAction>>(new Map())
  const officeEvents = ref<OfficeEvent[]>([])
  const roomLayoutState = ref<MapRoom[]>(ROOM_LAYOUT.map((room) => ({ ...room })))
  const mapPropsState = ref<MapProp[]>(MAP_PROPS.map((prop) => ({ ...prop })))
  const movingWorkers = ref<MovingWorker[]>([])
  const transitioningAgentIds = ref<Set<number>>(new Set())
  const spriteFrame = ref(0)
  const mapViewportRef = ref<HTMLDivElement | null>(null)

  // ---- refs（不触发渲染，供回调读取最新值） ----
  const prevStatusRef = ref<Map<number, string>>(new Map())
  const transitionTimersRef = new Map<number, ReturnType<typeof setTimeout>>()
  const launchToastTimerRef = ref<ReturnType<typeof setTimeout> | null>(null)
  const roamReturnTimersRef = new Map<number, ReturnType<typeof setTimeout>>()
  const movingWorkersRef = ref<MovingWorker[]>([])
  const renderedWorkersRef = ref<RenderedWorker[]>([])
  const previousSeatMapRef = ref<Map<number, SeatPosition>>(new Map())
  let rafId: number | null = null
  let roamTimer: ReturnType<typeof setInterval> | null = null
  let eventTimer: ReturnType<typeof setInterval> | null = null
  let spriteTimer: ReturnType<typeof setInterval> | null = null

  // ---- 计数/分组 ----
  const counts = computed(() => {
    const c = { idle: 0, busy: 0, error: 0, offline: 0 }
    for (const a of displayAgents.value) {
      c[a.status] = (c[a.status] || 0) + 1
    }
    return c
  })

  const roleGroups = computed(() => {
    const groups = new Map<string, OfficeAgent[]>()
    for (const a of displayAgents.value) {
      const role = a.role || '未分配'
      if (!groups.has(role)) groups.set(role, [])
      groups.get(role)!.push(a)
    }
    return groups
  })

  const officeLayout = computed(() => buildOfficeLayout(displayAgents.value))

  // ---- 座位分配（按分区模板，溢出回退到 general） ----
  const currentSeatMap = computed<Map<number, SeatPosition>>(() => {
    const seatMap = new Map<number, SeatPosition>()
    const zoneSeatTemplates: Record<string, Array<{ x: number; y: number }>> = {
      engineering: [{ x: 24, y: 36 }, { x: 32, y: 36 }, { x: 24, y: 42 }, { x: 32, y: 42 }],
      product: [{ x: 54, y: 36 }, { x: 62, y: 36 }, { x: 54, y: 42 }, { x: 62, y: 42 }],
      operations: [{ x: 24, y: 64 }, { x: 32, y: 64 }, { x: 24, y: 70 }, { x: 32, y: 70 }],
      research: [{ x: 50, y: 64 }, { x: 58, y: 64 }, { x: 50, y: 70 }, { x: 58, y: 70 }],
      quality: [{ x: 58, y: 64 }, { x: 66, y: 64 }, { x: 58, y: 70 }, { x: 66, y: 70 }],
      general: [{ x: 38, y: 45 }, { x: 46, y: 39 }, { x: 54, y: 45 }, { x: 62, y: 39 }, { x: 42, y: 52 }, { x: 58, y: 52 }]
    }
    const fallbackByZone: Record<string, string[]> = {
      engineering: ['operations', 'general'],
      product: ['research', 'general'],
      operations: ['engineering', 'general'],
      research: ['product', 'general'],
      quality: ['research', 'general'],
      general: ['general']
    }
    const usageByZone = new Map<string, number>()
    const pullSeat = (zoneId: string) => {
      const templates = zoneSeatTemplates[zoneId] || zoneSeatTemplates.general
      const used = usageByZone.get(zoneId) || 0
      const chosen = templates[used % templates.length] || { x: 38, y: 47 }
      const overflowBand = Math.floor(used / templates.length)
      usageByZone.set(zoneId, used + 1)
      return { x: chosen.x, y: chosen.y + overflowBand * 3.5 }
    }
    for (const zone of officeLayout.value) {
      const sortedWorkers = [...zone.workers].sort((a, b) =>
        a.agent.name.localeCompare(b.agent.name)
      )
      for (const worker of sortedWorkers) {
        const primaryTemplates = zoneSeatTemplates[zone.zone.id] || zoneSeatTemplates.general
        const primaryUsed = usageByZone.get(zone.zone.id) || 0
        const inPrimaryCapacity = primaryUsed < primaryTemplates.length * 2
        const targetZone = inPrimaryCapacity ? zone.zone.id : (fallbackByZone[zone.zone.id] || ['general'])[0]
        const seat = pullSeat(targetZone)
        const x = clamp(seat.x, 8, 92)
        const y = clamp(seat.y, 12, 92)
        seatMap.set(worker.agent.id, {
          seatKey: `${targetZone}:${worker.anchor.seatLabel}`,
          x,
          y
        })
      }
    }
    return seatMap
  })

  const gameWorkers = computed(() => {
    const workers: Array<{ agent: OfficeAgent; x: number; y: number; zoneLabel: string; seatLabel: string }> = []
    for (const zone of officeLayout.value) {
      for (const worker of zone.workers) {
        const seat = currentSeatMap.value.get(worker.agent.id)
        if (!seat) continue
        workers.push({
          agent: worker.agent,
          x: seat.x,
          y: seat.y,
          zoneLabel: zone.zone.label,
          seatLabel: worker.anchor.seatLabel
        })
      }
    }
    return workers
  })

  const floorTiles = computed(() => {
    const tiles: Array<{ id: string; x: number; y: number; w: number; h: number; sprite: boolean }> = []
    const tileW = 100 / MAP_COLS
    const tileH = 100 / MAP_ROWS
    for (let row = 0; row < MAP_ROWS; row += 1) {
      for (let col = 0; col < MAP_COLS; col += 1) {
        tiles.push({
          id: `tile-${row}-${col}`,
          x: col * tileW,
          y: row * tileH,
          w: tileW,
          h: tileH,
          sprite: (row + col) % 2 === 0
        })
      }
    }
    return tiles
  })

  const movingPositionByAgent = computed(() => {
    const positions = new Map<number, { x: number; y: number }>()
    for (const worker of movingWorkers.value) {
      const eased = easeInOut(worker.progress)
      positions.set(
        worker.agentId,
        pointAlongPath(worker.path, worker.pathLengths, worker.totalLength, eased)
      )
    }
    return positions
  })

  const movingDirectionByAgent = computed(() => {
    const directions = new Map<number, { dx: number; dy: number }>()
    for (const worker of movingWorkers.value) {
      directions.set(worker.agentId, {
        dx: worker.endX - worker.startX,
        dy: worker.endY - worker.startY
      })
    }
    return directions
  })

  const renderedWorkers = computed<RenderedWorker[]>(() => {
    return gameWorkers.value.map((worker) => {
      const movingPosition = movingPositionByAgent.value.get(worker.agent.id)
      return {
        ...worker,
        x: movingPosition?.x ?? worker.x,
        y: movingPosition?.y ?? worker.y,
        isMoving: Boolean(movingPosition),
        direction: movingDirectionByAgent.value.get(worker.agent.id) || { dx: 0, dy: 0 },
        variant: getWorkerVariant(worker.agent.name)
      }
    })
  })

  const heatmapPoints = computed(() => {
    return renderedWorkers.value.map((worker) => {
      const action = agentActionOverrides.value.get(worker.agent.id)
      let intensity = worker.agent.status === 'busy' ? 0.95 : worker.agent.status === 'idle' ? 0.45 : 0.7
      if (action === 'focus') intensity += 0.25
      if (action === 'pair') intensity += 0.15
      if (worker.isMoving) intensity += 0.2
      const radius = worker.agent.status === 'busy' ? 14 : 10
      const hue =
        worker.agent.status === 'busy'
          ? 'rgba(255,191,84,'
          : worker.agent.status === 'idle'
            ? 'rgba(88,220,139,'
            : 'rgba(120,189,255,'
      return {
        id: worker.agent.id,
        x: worker.x,
        y: worker.y,
        radius,
        color: `${hue}${Math.min(0.85, Math.max(0.2, intensity)).toFixed(2)})`
      }
    })
  })

  const roomActivityById = computed<Map<string, RoomActivity>>(() => {
    const activity = new Map<string, RoomActivity>()
    for (const room of roomLayoutState.value) {
      activity.set(
        room.id,
        ((): RoomActivity => {
          const occupants = renderedWorkers.value.filter((w) => {
            return (
              w.x >= room.x &&
              w.x <= room.x + room.w &&
              w.y >= room.y &&
              w.y <= room.y + room.h
            )
          })
          return occupants.reduce<RoomActivity>(
            (acc, w) => {
              acc.total += 1
              if (w.agent.status === 'busy') acc.busy += 1
              else if (w.agent.status === 'idle') acc.idle += 1
              else if (w.agent.status === 'error') acc.alerts += 1
              acc.taskLoad += getAgentTaskLoad(w.agent)
              return acc
            },
            { total: 0, busy: 0, idle: 0, alerts: 0, taskLoad: 0 }
          )
        })()
      )
    }
    return activity
  })

  const deliveryPackets = computed<DeliveryPacket[]>(() => {
    if (renderedWorkers.value.length === 0) return []
    const candidates = renderedWorkers.value
      .filter(
        (w) =>
          w.agent.status === 'busy' ||
          getAgentTaskLoad(w.agent) > 0 ||
          agentActionOverrides.value.has(w.agent.id)
      )
      .sort((a, b) => {
        const aLoad = getAgentTaskLoad(a.agent) + (a.agent.status === 'busy' ? 2 : 0)
        const bLoad = getAgentTaskLoad(b.agent) + (b.agent.status === 'busy' ? 2 : 0)
        return bLoad - aLoad || a.agent.name.localeCompare(b.agent.name)
      })
      .slice(0, 8)
    return candidates.map((worker, index) => {
      const target = DELIVERY_TERMINALS[index % 2]
      const partner =
        renderedWorkers.value[
          (renderedWorkers.value.findIndex((item) => item.agent.id === worker.agent.id) + 1) %
            renderedWorkers.value.length
        ]
      const toX = agentActionOverrides.value.get(worker.agent.id) === 'pair' && partner ? partner.x : target.x
      const toY = agentActionOverrides.value.get(worker.agent.id) === 'pair' && partner ? partner.y - 5 : target.y
      const color =
        worker.agent.status === 'busy' ? 'rgba(251,191,36,0.95)' : DELIVERY_TERMINALS[0].color
      return {
        id: `${worker.agent.id}-${index}`,
        fromX: worker.x,
        fromY: worker.y - 7,
        midX: clamp((worker.x + toX) / 2, 8, 92),
        midY: clamp(Math.min(worker.y, toY) - 8 - (index % 3) * 2, 10, 82),
        toX,
        toY,
        delay: (index % 4) * 0.75,
        duration: 5.2 + (index % 3) * 0.7,
        color,
        label: target.label
      }
    })
  })

  const rosterRows = computed(() => {
    return gameWorkers.value.map(({ agent }) => {
      const minutesIdle = agent.lastSeen
        ? Math.floor((Date.now() / 1000 - agent.lastSeen) / 60)
        : Number.POSITIVE_INFINITY
      const needsAttention = agent.status === 'idle' && minutesIdle >= 15
      return { agent, minutesIdle, needsAttention }
    })
  })

  const filteredRosterRows = computed(() => {
    if (sidebarFilter.value === 'all') return rosterRows.value
    if (sidebarFilter.value === 'working') return rosterRows.value.filter((r) => r.agent.status === 'busy')
    if (sidebarFilter.value === 'idle') return rosterRows.value.filter((r) => r.agent.status === 'idle')
    return rosterRows.value.filter((r) => r.needsAttention)
  })

  const pathEdges = computed(() => {
    const edges: Array<{ x1: number; y1: number; x2: number; y2: number }> = []
    const zoneGroups = new Map<string, Array<{ x: number; y: number }>>()
    for (const worker of gameWorkers.value) {
      if (!zoneGroups.has(worker.zoneLabel)) zoneGroups.set(worker.zoneLabel, [])
      zoneGroups.get(worker.zoneLabel)!.push({ x: worker.x, y: worker.y })
    }
    for (const points of zoneGroups.values()) {
      const sorted = [...points].sort((a, b) => a.x - b.x || a.y - b.y)
      for (let i = 0; i < sorted.length - 1; i += 1) {
        edges.push({
          x1: sorted[i].x,
          y1: sorted[i].y + 2,
          x2: sorted[i + 1].x,
          y2: sorted[i + 1].y + 2
        })
      }
    }
    edges.push({ x1: 16, y1: 47, x2: 84, y2: 47 })
    edges.push({ x1: 30, y1: 33, x2: 30, y2: 47 })
    edges.push({ x1: 60, y1: 33, x2: 60, y2: 47 })
    edges.push({ x1: 28, y1: 47, x2: 28, y2: 68 })
    edges.push({ x1: 54, y1: 47, x2: 54, y2: 68 })
    return edges
  })

  const categoryGroups = computed(() => {
    const groups = new Map<string, OfficeAgent[]>()
    const getCategory = (agent: OfficeAgent): string => {
      const name = (agent.name || '').toLowerCase()
      if (name.startsWith('habi-')) return 'Habi 通道'
      if (name.startsWith('ops-')) return '运维自动化'
      if (name.includes('canary')) return '金丝雀'
      if (name.startsWith('main')) return '核心'
      if (name.startsWith('remote-')) return '远程'
      return '其他'
    }
    for (const a of displayAgents.value) {
      const category = getCategory(a)
      if (!groups.has(category)) groups.set(category, [])
      groups.get(category)!.push(a)
    }
    const order = ['Habi 通道', '运维自动化', '核心', '金丝雀', '远程', '其他']
    return new Map(
      [...groups.entries()].sort(([a], [b]) => {
        const ai = order.indexOf(a)
        const bi = order.indexOf(b)
        const av = ai === -1 ? Number.MAX_SAFE_INTEGER : ai
        const bv = bi === -1 ? Number.MAX_SAFE_INTEGER : bi
        if (av !== bv) return av - bv
        return a.localeCompare(b)
      })
    )
  })

  const statusGroups = computed(() => {
    const groups = new Map<string, OfficeAgent[]>()
    for (const a of displayAgents.value) {
      const key = statusLabel[a.status] || a.status
      if (!groups.has(key)) groups.set(key, [])
      groups.get(key)!.push(a)
    }
    const order = ['活动中', '待命', '告警', '离线']
    return new Map(
      [...groups.entries()].sort(([a], [b]) => {
        const ai = order.indexOf(a)
        const bi = order.indexOf(b)
        const av = ai === -1 ? Number.MAX_SAFE_INTEGER : ai
        const bv = bi === -1 ? Number.MAX_SAFE_INTEGER : bi
        if (av !== bv) return av - bv
        return a.localeCompare(b)
      })
    )
  })

  // ---- 事件流 ----
  const pushOfficeEvent = (event: Omit<OfficeEvent, 'id' | 'at'>) => {
    const next: OfficeEvent = {
      ...event,
      id: `${Date.now()}-${Math.floor(Math.random() * 1000)}`,
      at: Date.now()
    }
    officeEvents.value = [next, ...officeEvents.value].slice(0, 12)
  }

  const showLaunchToast = (toast: LaunchToast) => {
    launchToast.value = toast
    if (launchToastTimerRef.value) clearTimeout(launchToastTimerRef.value)
    launchToastTimerRef.value = setTimeout(() => {
      launchToast.value = null
      launchToastTimerRef.value = null
    }, 5000)
  }

  // ---- 移动入队（A* 寻路） ----
  const enqueueMovement = (
    agent: OfficeAgent,
    startX: number,
    startY: number,
    endX: number,
    endY: number,
    durationMs = 2200
  ) => {
    const blockedTiles = new Set<string>()
    for (const worker of renderedWorkersRef.value) {
      if (worker.agent.id === agent.id) continue
      const tile = toTile(worker.x, worker.y)
      blockedTiles.add(tileKey(tile.col, tile.row))
    }
    for (const moving of movingWorkersRef.value) {
      if (moving.agentId === agent.id) continue
      blockedTiles.add(moving.destinationTile)
    }
    const destination = toTile(endX, endY)
    const movement: MovingWorker = {
      id: `${agent.id}-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
      agentId: agent.id,
      initials: getInitials(agent.name),
      colorClass: hashColor(agent.name),
      startX,
      startY,
      endX,
      endY,
      startedAt: Date.now(),
      durationMs,
      progress: 0,
      ...buildPath(startX, startY, endX, endY, blockedTiles),
      destinationTile: tileKey(destination.col, destination.row)
    }
    movingWorkers.value = movingWorkers.value.some((i) => i.agentId === agent.id)
      ? movingWorkers.value
      : [...movingWorkers.value, movement]
  }

  // ---- 动作执行 ----
  const executeAgentAction = (agent: OfficeAgent, action: OfficeAction) => {
    const next = new Map(agentActionOverrides.value)
    next.set(agent.id, action)
    agentActionOverrides.value = next

    if (action === 'focus') {
      pushOfficeEvent({ kind: 'action', severity: 'good', message: `${agent.name} 进入深度专注模式。` })
      return
    }
    if (action === 'pair') {
      const partner = renderedWorkersRef.value.find((w) => w.agent.id !== agent.id)?.agent
      pushOfficeEvent({
        kind: 'action',
        severity: 'info',
        message: partner
          ? `${agent.name} 与 ${partner.name} 开始结对会话。`
          : `${agent.name} 开始单人结对准备。`
      })
      return
    }
    const worker = renderedWorkersRef.value.find((i) => i.agent.id === agent.id)
    const waypoint = LOUNGE_WAYPOINTS[hashNumber(agent.name) % LOUNGE_WAYPOINTS.length]
    if (worker) {
      enqueueMovement(agent, worker.x, worker.y, waypoint.x, waypoint.y, 2200)
      pushOfficeEvent({ kind: 'action', severity: 'warn', message: `${agent.name} 去休息区短暂休息。` })
      return
    }
    pushOfficeEvent({ kind: 'action', severity: 'warn', message: `${agent.name} 申请休息。` })
  }

  // ---- 地图控制 ----
  const resetMapView = () => {
    mapZoom.value = 1
    mapPan.value = { x: 0, y: 0 }
  }

  const onMapWheel = (event: WheelEvent) => {
    event.preventDefault()
    const delta = event.deltaY > 0 ? -0.08 : 0.08
    mapZoom.value = Math.min(2.2, Math.max(0.8, Number((mapZoom.value + delta).toFixed(2))))
  }

  const mapDragActiveRef = ref(false)
  const mapDragOriginRef = ref({ x: 0, y: 0 })
  const mapPanStartRef = ref({ x: 0, y: 0 })
  const onMapMouseDown = (event: MouseEvent) => {
    mapDragActiveRef.value = true
    mapDragOriginRef.value = { x: event.clientX, y: event.clientY }
    mapPanStartRef.value = { ...mapPan.value }
  }
  const onMapMouseMove = (event: MouseEvent) => {
    if (!mapDragActiveRef.value) return
    const dx = event.clientX - mapDragOriginRef.value.x
    const dy = event.clientY - mapDragOriginRef.value.y
    mapPan.value = {
      x: mapPanStartRef.value.x + dx,
      y: mapPanStartRef.value.y + dy
    }
  }
  const endMapDrag = () => {
    mapDragActiveRef.value = false
  }

  const focusMapPoint = (xPercent: number, yPercent: number) => {
    const viewport = mapViewportRef.value
    if (!viewport) return
    const rect = viewport.getBoundingClientRect()
    const nextPanX = rect.width / 2 - (xPercent / 100) * rect.width * mapZoom.value
    const nextPanY = rect.height / 2 - (yPercent / 100) * rect.height * mapZoom.value
    mapPan.value = { x: nextPanX, y: nextPanY }
  }

  // ---- 热点编辑 ----
  const nudgeSelectedHotspot = (dx: number, dy: number) => {
    if (!selectedHotspot.value) return
    if (selectedHotspot.value.kind === 'room') {
      roomLayoutState.value = roomLayoutState.value.map((room) => {
        if (room.id !== selectedHotspot.value!.id) return room
        return {
          ...room,
          x: clamp(room.x + dx, 2, 94 - room.w),
          y: clamp(room.y + dy, 8, 94 - room.h)
        }
      })
      const cur = selectedHotspot.value
      selectedHotspot.value = {
        ...cur,
        x: clamp(cur.x + dx, 2, 98),
        y: clamp(cur.y + dy, 8, 98)
      }
      return
    }
    mapPropsState.value = mapPropsState.value.map((prop) => {
      if (prop.id !== selectedHotspot.value!.id) return prop
      return {
        ...prop,
        x: clamp(prop.x + dx, 2, 98 - prop.w),
        y: clamp(prop.y + dy, 8, 98 - prop.h)
      }
    })
    const cur = selectedHotspot.value
    selectedHotspot.value = {
      ...cur,
      x: clamp(cur.x + dx, 2, 98),
      y: clamp(cur.y + dy, 8, 98)
    }
  }

  const resizeSelectedRoom = (dw: number, dh: number) => {
    if (!selectedHotspot.value || selectedHotspot.value.kind !== 'room') return
    roomLayoutState.value = roomLayoutState.value.map((room) => {
      if (room.id !== selectedHotspot.value!.id) return room
      const nextW = clamp(room.w + dw, 10, 40)
      const nextH = clamp(room.h + dh, 10, 36)
      return {
        ...room,
        w: nextW,
        h: nextH,
        x: clamp(room.x, 2, 98 - nextW),
        y: clamp(room.y, 8, 98 - nextH)
      }
    })
  }

  const resetOfficeLayout = () => {
    roomLayoutState.value = ROOM_LAYOUT.map((room) => ({ ...room }))
    mapPropsState.value = MAP_PROPS.map((prop) => ({ ...prop }))
    mapZoom.value = 1
    mapPan.value = { x: 0, y: 0 }
    showSidebar.value = true
    showMinimap.value = true
    showEvents.value = true
    selectedHotspot.value = null
    pushOfficeEvent({ kind: 'room', severity: 'info', message: '办公室布局已重置为默认。' })
  }

  // ---- rAF 移动动画驱动 ----
  const startRaf = () => {
    if (rafId != null) return
    const step = () => {
      const now = Date.now()
      const current = movingWorkers.value
      if (current.length > 0) {
        const updated = current
          .map((w) => ({ ...w, progress: Math.max(0, Math.min(1, (now - w.startedAt) / w.durationMs)) }))
          .filter((w) => w.progress < 1)
        movingWorkers.value = updated
      }
      rafId = window.requestAnimationFrame(step)
    }
    rafId = window.requestAnimationFrame(step)
  }
  const stopRaf = () => {
    if (rafId != null) {
      window.cancelAnimationFrame(rafId)
      rafId = null
    }
  }

  // ---- refs 同步（供回调读取最新渲染状态） ----
  watch(movingWorkers, (v) => { movingWorkersRef.value = v }, { deep: true })
  watch(renderedWorkers, (v) => { renderedWorkersRef.value = v })

  // ---- rAF 随有移动 worker 启停 ----
  watch(() => movingWorkers.value.length, (len) => {
    if (len > 0) startRaf()
    else stopRaf()
  }, { immediate: true })

  // ---- 状态变化 → 转场动画标记 ----
  watch(displayAgents, (agents) => {
    const prev = prevStatusRef.value
    const next = new Map<number, string>()
    const toAnimate: number[] = []
    for (const agent of agents) {
      next.set(agent.id, agent.status)
      const prevStatus = prev.get(agent.id)
      if (prevStatus && prevStatus !== agent.status) toAnimate.push(agent.id)
    }
    prevStatusRef.value = next
    if (toAnimate.length === 0) return
    const updated = new Set(transitioningAgentIds.value)
    for (const id of toAnimate) updated.add(id)
    transitioningAgentIds.value = updated
    for (const id of toAnimate) {
      const existing = transitionTimersRef.get(id)
      if (existing) clearTimeout(existing)
      const timer = setTimeout(() => {
        const cur = new Set(transitioningAgentIds.value)
        cur.delete(id)
        transitioningAgentIds.value = cur
        transitionTimersRef.delete(id)
      }, 2200)
      transitionTimersRef.set(id, timer)
    }
  }, { deep: true })

  // ---- 座位变化 → 触发移动动画 ----
  watch(currentSeatMap, (seatMap) => {
    const previous = previousSeatMapRef.value
    for (const agent of displayAgents.value) {
      const cur = seatMap.get(agent.id)
      const prev = previous.get(agent.id)
      if (!cur || !prev) continue
      if (cur.seatKey === prev.seatKey) continue
      enqueueMovement(agent, prev.x, prev.y, cur.x, cur.y, 1800)
    }
    previousSeatMapRef.value = seatMap
  })

  // ---- 精灵帧切换 ----
  onMounted(() => {
    spriteTimer = setInterval(() => {
      spriteFrame.value = (spriteFrame.value + 1) % 2
    }, 380)
  })

  // ---- roam 循环：idle worker 定期走向休息区再返回 ----
  onMounted(() => {
    roamTimer = setInterval(() => {
      const activeMovingIds = new Set(movingWorkersRef.value.map((w) => w.agentId))
      const idleCandidates = renderedWorkersRef.value
        .filter((w) => w.agent.status === 'idle' && !w.isMoving && !activeMovingIds.has(w.agent.id))
        .sort((a, b) => a.agent.name.localeCompare(b.agent.name))
        .slice(0, 2)
      if (idleCandidates.length === 0) return
      const cycle = Math.floor(Date.now() / 14_000)
      for (const worker of idleCandidates) {
        const waypoint = LOUNGE_WAYPOINTS[(hashNumber(worker.agent.name) + cycle) % LOUNGE_WAYPOINTS.length]
        enqueueMovement(worker.agent, worker.x, worker.y, waypoint.x, waypoint.y, 2200)
        const existing = roamReturnTimersRef.get(worker.agent.id)
        if (existing) clearTimeout(existing)
        const returnTimer = setTimeout(() => {
          const seat = currentSeatMap.value.get(worker.agent.id)
          if (seat) enqueueMovement(worker.agent, waypoint.x, waypoint.y, seat.x, seat.y, 2200)
          roamReturnTimersRef.delete(worker.agent.id)
        }, 2700)
        roamReturnTimersRef.set(worker.agent.id, returnTimer)
      }
    }, 14_000)
  })

  // ---- 事件采样：每 22s 上报一个 worker 状态 ----
  onMounted(() => {
    eventTimer = setInterval(() => {
      const workers = renderedWorkersRef.value
      if (workers.length === 0) return
      const sample = workers[Math.floor(Math.random() * workers.length)]
      const mood: OfficeEvent['severity'] =
        sample.agent.status === 'busy' ? 'good' : sample.agent.status === 'idle' ? 'warn' : 'info'
      pushOfficeEvent({
        kind: 'room',
        severity: mood,
        message: `${sample.zoneLabel}：${sample.agent.name} 状态为 ${statusLabel[sample.agent.status]}。`
      })
    }, 22000)
  })

  // ---- 清理 ----
  onUnmounted(() => {
    stopRaf()
    if (roamTimer) clearInterval(roamTimer)
    if (eventTimer) clearInterval(eventTimer)
    if (spriteTimer) clearInterval(spriteTimer)
    for (const t of transitionTimersRef.values()) clearTimeout(t)
    transitionTimersRef.clear()
    for (const t of roamReturnTimersRef.values()) clearTimeout(t)
    roamReturnTimersRef.clear()
    if (launchToastTimerRef.value) clearTimeout(launchToastTimerRef.value)
  })

  return {
    // 状态
    viewMode, sidebarFilter, showSidebar, showMinimap, showEvents,
    mapZoom, mapPan, selectedAgent, selectedHotspot, launchToast,
    agentActionOverrides, officeEvents, roomLayoutState, mapPropsState,
    movingWorkers, transitioningAgentIds, spriteFrame, mapViewportRef,
    // 计算属性
    counts, roleGroups, officeLayout, currentSeatMap, gameWorkers, floorTiles,
    renderedWorkers, heatmapPoints, roomActivityById, deliveryPackets,
    rosterRows, filteredRosterRows, pathEdges, categoryGroups, statusGroups,
    // 方法
    pushOfficeEvent, showLaunchToast, enqueueMovement, executeAgentAction,
    resetMapView, onMapWheel, onMapMouseDown, onMapMouseMove, endMapDrag,
    focusMapPoint, nudgeSelectedHotspot, resizeSelectedRoom, resetOfficeLayout
  }
}