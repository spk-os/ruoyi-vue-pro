// 静态办公室布局：分区 / 工位锚点
// 移植自 Paddock src/lib/office-layout.ts，泛型化以承接 OfficeAgent
import type { OfficeAgent, OfficeZoneDefinition, WorkstationAnchor, ZonedAgent, OfficeZoneLayout } from './types'

export type OfficeZoneType = 'engineering' | 'operations' | 'research' | 'product' | 'quality' | 'general'

export const OFFICE_ZONES: OfficeZoneDefinition[] = [
  {
    id: 'engineering',
    label: '工程舱',
    icon: '🧑‍💻',
    accentClass: 'border-cyan-400/30 bg-cyan-400/10',
    roleKeywords: ['engineer', 'dev', 'frontend', 'backend', 'fullstack', 'software', '工程', '研发', '前端', '后端']
  },
  {
    id: 'operations',
    label: '运维舱',
    icon: '🛠️',
    accentClass: 'border-amber-400/30 bg-amber-400/10',
    roleKeywords: ['ops', 'sre', 'infra', 'platform', 'reliability', '运维', '基础设施', '平台']
  },
  {
    id: 'research',
    label: '研究角',
    icon: '🔬',
    accentClass: 'border-violet-400/30 bg-violet-400/10',
    roleKeywords: ['research', 'science', 'analyst', 'ai', '研究', '分析', '智能']
  },
  {
    id: 'product',
    label: '产品舱',
    icon: '📐',
    accentClass: 'border-emerald-400/30 bg-emerald-400/10',
    roleKeywords: ['product', 'pm', 'design', 'ux', 'ui', '产品', '设计']
  },
  {
    id: 'quality',
    label: '质量舱',
    icon: '🧪',
    accentClass: 'border-rose-400/30 bg-rose-400/10',
    roleKeywords: ['qa', 'test', 'quality', '测试', '质量']
  },
  {
    id: 'general',
    label: '通用工位',
    icon: '🏢',
    accentClass: 'border-slate-400/30 bg-slate-400/10',
    roleKeywords: []
  }
]

function normalizeRole(role: string | undefined): string {
  return String(role || '').toLowerCase()
}

export function getZoneByRole(role: string | undefined): OfficeZoneDefinition {
  const normalized = normalizeRole(role)
  for (const zone of OFFICE_ZONES) {
    if (zone.id === 'general') continue
    if (zone.roleKeywords.some((keyword) => normalized.includes(keyword))) {
      return zone
    }
  }
  return OFFICE_ZONES.find((zone) => zone.id === 'general')!
}

function buildAnchor(index: number, columnCount: number): WorkstationAnchor {
  const row = Math.floor(index / columnCount)
  const col = index % columnCount
  const rowLabel = String.fromCharCode(65 + row)
  const seatLabel = `${rowLabel}${col + 1}`
  return {
    deskId: `desk-${seatLabel.toLowerCase()}`,
    seatLabel,
    row,
    col,
    // 供未来绝对定位/碰撞机制使用
    x: col * 220 + 110,
    y: row * 160 + 80
  }
}

/** 按角色把 agents 分到各分区，并赋予工位锚点；分区按人数降序排列 */
export function buildOfficeLayout(agents: OfficeAgent[]): OfficeZoneLayout[] {
  const zoneMap = new Map<string, OfficeAgent[]>()
  for (const zone of OFFICE_ZONES) zoneMap.set(zone.id, [])

  for (const agent of agents) {
    const zone = getZoneByRole(agent.role)
    zoneMap.get(zone.id)!.push(agent)
  }

  const result: OfficeZoneLayout[] = []
  for (const zone of OFFICE_ZONES) {
    const workers = zoneMap.get(zone.id) || []
    if (workers.length === 0) continue

    const columns = workers.length >= 8 ? 4 : workers.length >= 4 ? 3 : 2
    const zoned: ZonedAgent[] = workers.map((agent, i) => ({
      agent,
      anchor: buildAnchor(i, columns)
    }))

    result.push({ zone, workers: zoned })
  }

  return result.sort((a, b) => b.workers.length - a.workers.length)
}
