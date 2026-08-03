<template>
  <div class="office-floor flex gap-3">
    <!-- 编队侧栏 -->
    <div
      v-if="engine.showSidebar.value"
      class="w-64 shrink-0 rounded-lg border border-cyan-400/15 bg-black/30 backdrop-blur-sm p-2.5 space-y-2"
    >
      <div class="flex items-center justify-between">
        <div class="text-[10px] text-cyan-300/70 font-mono uppercase tracking-wider">{{ TXT.crewHeader }}</div>
        <div class="text-[10px] text-slate-400 font-mono">
          {{ formatText(TXT.onlineCount, { count: engine.counts.value.busy + engine.counts.value.idle }) }}
        </div>
      </div>
      <div class="flex flex-wrap gap-1">
        <button
          v-for="f in filters"
          :key="f.key"
          type="button"
          class="flex-1 h-auto px-2 py-1 text-[10px] font-mono border rounded"
          :class="engine.sidebarFilter.value === f.key ? 'bg-cyan-400/15 border-cyan-400/30 text-cyan-300' : 'bg-white/5 border-white/10 text-slate-400 hover:bg-white/10'"
          @click="engine.sidebarFilter.value = f.key"
        >{{ f.label }}</button>
      </div>
      <div class="space-y-2 max-h-[560px] overflow-y-auto pr-1">
        <button
          v-for="row in engine.filteredRosterRows.value"
          :key="row.agent.id"
          type="button"
          class="w-full flex items-center gap-2 rounded-lg p-2 text-left h-auto"
          :class="row.needsAttention ? 'bg-amber-500/12 border border-amber-400/60 hover:bg-amber-500/20' : 'bg-black/20 border border-white/5 hover:bg-black/35'"
          @click="selectRoster(row.agent)"
        >
          <span class="w-6 h-6 rounded flex items-center justify-center text-[10px] font-bold text-white" :class="hashColor(row.agent.name)">{{ getInitials(row.agent.name) }}</span>
          <span class="min-w-0 flex-1">
            <span class="block text-xs font-medium truncate text-slate-100">{{ row.agent.name }}</span>
            <span class="block text-[10px] text-slate-300 truncate">{{ row.agent.role }}</span>
            <span class="block text-[9px] text-slate-400 truncate">{{ row.agent.lastActivity || TXT.noRecentActivity }}</span>
          </span>
          <span class="flex flex-col items-end gap-1">
            <span class="rounded-full border px-1.5 py-0.5 text-[9px] leading-none" :class="workCue(row.agent).toneClass">{{ workCue(row.agent).label }}</span>
            <span class="text-[9px]" :class="row.needsAttention ? 'text-amber-300 font-semibold' : 'text-slate-400'">
              {{ row.agent.status === 'busy' ? TXT.activeStatus : formatText(TXT.idleMinutes, { minutes: row.minutesIdle }) }}
            </span>
          </span>
        </button>
        <div v-if="engine.filteredRosterRows.value.length === 0" class="text-[11px] text-slate-400 px-1 py-2">{{ TXT.noWorkersInFilter }}</div>
      </div>
    </div>
    <!-- 2D 平面图视口 -->
    <div
      :ref="setViewportRef"
      class="relative flex-1 rounded-lg border border-white/10 overflow-hidden min-h-[560px] cursor-grab active:cursor-grabbing shadow-[0_20px_60px_rgba(0,0,0,0.55)]"
      :style="viewportStyle"
      @wheel.prevent="onWheel"
      @mousedown="onMouseDown"
      @mousemove="onMouseMove"
      @mouseup="engine.endMapDrag"
      @mouseleave="engine.endMapDrag"
    >
      <div class="absolute inset-0 pointer-events-none z-0" :style="{ backgroundImage: palette.haze }"></div>
      <div class="absolute inset-0 pointer-events-none z-0" :style="{ backgroundImage: palette.glow }"></div>
      <div class="absolute inset-0 pointer-events-none z-0" :style="{ backgroundImage: palette.atmosphere, mixBlendMode: 'screen', opacity: 0.9 }"></div>
      <div class="absolute inset-0 pointer-events-none z-0" :style="{ backgroundImage: palette.shadowVeil }"></div>
      <!-- 主题动画层 -->
      <div v-if="theme.timeTheme.value === 'dawn'" class="absolute inset-0 pointer-events-none z-[2]" :style="dawnStyle"></div>
      <template v-if="theme.timeTheme.value === 'day'">
        <div class="absolute inset-0 pointer-events-none z-[2]" :style="dayStyle1"></div>
        <div class="absolute inset-0 pointer-events-none z-[2]" :style="dayStyle2"></div>
      </template>
      <div v-if="theme.timeTheme.value === 'dusk'" class="absolute inset-0 pointer-events-none z-[2]" :style="duskStyle"></div>
      <template v-if="theme.timeTheme.value === 'night'">
        <div class="absolute inset-0 pointer-events-none z-[2]" :style="nightStyle"></div>
        <div
          v-for="spark in theme.nightSparkles.value"
          :key="`spark-${spark.id}`"
          class="absolute pointer-events-none z-[2] rounded-full bg-white/80"
          :style="{ left: `${spark.x}%`, top: `${spark.y}%`, width: `${spark.size}px`, height: `${spark.size}px`, boxShadow: '0 0 8px rgba(180,210,255,0.9)', animation: `mcTwinkle 2.6s ease-in-out ${spark.delay}s infinite` }"
        ></div>
      </template>

      <div class="absolute left-[8%] top-[8%] rounded-md bg-black/50 backdrop-blur-sm border border-cyan-400/20 text-cyan-300 text-xs px-2 py-1 font-mono z-30">{{ TXT.mainDeck }}</div>
      <!-- 缩放控制 -->
      <div class="absolute right-3 top-3 z-30 flex items-center gap-1 rounded-md bg-black/50 backdrop-blur-sm border border-white/10 text-slate-100 px-2 py-1">
        <button type="button" class="h-auto px-1.5 py-0.5 text-xs hover:bg-cyan-400/10" @click="engine.mapZoom.value = Math.max(0.8, Number((engine.mapZoom.value - 0.1).toFixed(2)))">-</button>
        <span class="text-[11px] font-mono w-10 text-center">{{ Math.round(engine.mapZoom.value * 100) }}%</span>
        <button type="button" class="h-auto px-1.5 py-0.5 text-xs hover:bg-cyan-400/10" @click="engine.mapZoom.value = Math.min(2.2, Number((engine.mapZoom.value + 0.1).toFixed(2)))">+</button>
        <button type="button" class="h-auto px-1.5 py-0.5 text-[11px] hover:bg-cyan-400/10" @click="engine.resetMapView()">{{ TXT.resetView }}</button>
      </div>
      <!-- 主题切换 -->
      <div class="absolute right-3 top-12 z-30 flex items-center gap-1 rounded-md bg-black/50 backdrop-blur-sm border border-white/10 text-slate-100 px-2 py-1">
        <button
          v-for="item in themes"
          :key="item"
          type="button"
          class="h-auto px-1.5 py-0.5 text-[10px] font-mono uppercase"
          :class="theme.timeTheme.value === item ? 'bg-cyan-400/20 text-cyan-300' : 'hover:bg-cyan-400/10 text-slate-400'"
          @click="theme.setTimeTheme(item)"
        >{{ item }}</button>
      </div>
      <!-- 工具栏 -->
      <div class="absolute left-3 top-3 z-30 flex items-center gap-1 rounded-md bg-black/50 backdrop-blur-sm border border-white/10 text-slate-100 px-2 py-1">
        <button type="button" class="h-auto px-1.5 py-0.5 text-[10px] font-mono hover:bg-cyan-400/10" @click="engine.showSidebar.value = !engine.showSidebar.value">{{ engine.showSidebar.value ? TXT.hideCrewButton : TXT.showCrewButton }}</button>
        <button type="button" class="h-auto px-1.5 py-0.5 text-[10px] font-mono hover:bg-cyan-400/10" @click="engine.showMinimap.value = !engine.showMinimap.value">{{ engine.showMinimap.value ? TXT.hideRadarButton : TXT.showRadarButton }}</button>
        <button type="button" class="h-auto px-1.5 py-0.5 text-[10px] font-mono hover:bg-cyan-400/10" @click="engine.showEvents.value = !engine.showEvents.value">{{ engine.showEvents.value ? TXT.hideLogButton : TXT.showLogButton }}</button>
        <button type="button" class="h-auto px-1.5 py-0.5 text-[10px] font-mono hover:bg-cyan-400/10" @click="engine.resetOfficeLayout()">{{ TXT.resetLayout }}</button>
      </div>
      <!-- 缩放/平移变换层 -->
      <div class="absolute inset-0 origin-top-left" :style="{ transform: `translate(${engine.mapPan.value.x}px, ${engine.mapPan.value.y}px) scale(${engine.mapZoom.value})` }">
        <!-- 地板 -->
        <div class="absolute inset-0 z-0">
          <div
            v-for="tile in engine.floorTiles.value"
            :key="tile.id"
            class="absolute border border-cyan-400/[0.06]"
            :style="{ left: `${tile.x}%`, top: `${tile.y}%`, width: `${tile.w}%`, height: `${tile.h}%`, backgroundImage: `url('/office-sprites/kenney/floorFull.png')`, backgroundSize: '100% 100%', opacity: tile.sprite ? palette.floorOpacityA : palette.floorOpacityB, filter: palette.floorFilter }"
          ></div>
        </div>
        <!-- 走廊 -->
        <div class="absolute left-[14%] top-[45%] w-[72%] h-[6%] border-y border-cyan-400/15 shadow-[0_0_30px_rgba(34,211,238,0.1)]" :style="{ backgroundColor: palette.corridor }"></div>
        <div class="absolute left-[14%] top-[47.6%] w-[72%] h-[0.7%]" :style="{ backgroundColor: palette.corridorStripe }"></div>
        <!-- 热力图 -->
        <div class="absolute inset-0 pointer-events-none z-[1]">
          <div
            v-for="point in engine.heatmapPoints.value"
            :key="`heat-${point.id}`"
            class="absolute -translate-x-1/2 -translate-y-1/2 rounded-full blur-xl"
            :style="{ left: `${point.x}%`, top: `${point.y}%`, width: `${point.radius * 2}px`, height: `${point.radius * 2}px`, background: `radial-gradient(circle, ${point.color} 0%, rgba(0,0,0,0) 72%)` }"
          ></div>
        </div>
        <!-- 投递数据包 SVG -->
        <svg class="absolute inset-0 w-full h-full pointer-events-none z-[12]" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
          <defs>
            <filter id="officePacketGlow" x="-120%" y="-120%" width="340%" height="340%">
              <feGaussianBlur stdDeviation="0.75" result="blur" />
              <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
            </filter>
          </defs>
          <g v-for="packet in engine.deliveryPackets.value" :key="packet.id">
            <path :d="`M ${packet.fromX} ${packet.fromY} Q ${packet.midX} ${packet.midY} ${packet.toX} ${packet.toY}`" fill="none" :stroke="packet.color" strokeOpacity="0.18" strokeWidth="0.18" strokeDasharray="0.8 1.1" />
            <circle :r="0.75" :fill="packet.color" filter="url(#officePacketGlow)">
              <animate attributeName="cx" :values="`${packet.fromX};${packet.midX};${packet.toX}`" :dur="`${packet.duration}s`" :begin="`${packet.delay}s`" repeatCount="indefinite" calcMode="spline" keyTimes="0;0.48;1" keySplines="0.35 0 0.65 1;0.35 0 0.65 1" />
              <animate attributeName="cy" :values="`${packet.fromY};${packet.midY};${packet.toY}`" :dur="`${packet.duration}s`" :begin="`${packet.delay}s`" repeatCount="indefinite" calcMode="spline" keyTimes="0;0.48;1" keySplines="0.35 0 0.65 1;0.35 0 0.65 1" />
              <animate attributeName="opacity" values="0;1;1;0" :dur="`${packet.duration}s`" :begin="`${packet.delay}s`" repeatCount="indefinite" keyTimes="0;0.12;0.82;1" />
            </circle>
          </g>
        </svg>
        <!-- 投递终端 -->
        <div
          v-for="terminal in DELIVERY_TERMINALS"
          :key="terminal.id"
          class="absolute z-[13] -translate-x-1/2 -translate-y-1/2 rounded border bg-black/45 px-2 py-1 shadow-[0_0_18px_rgba(34,211,238,0.16)] backdrop-blur-sm pointer-events-none"
          :class="terminal.border"
          :style="{ left: `${terminal.x}%`, top: `${terminal.y}%` }"
        >
          <div class="text-[8px] font-mono leading-none tracking-wider text-slate-200/90">{{ terminal.label }}</div>
          <div class="mt-1 h-1 w-full overflow-hidden rounded-full bg-white/10">
            <div class="h-full rounded-full" :style="{ width: terminal.id === 'owner' ? '72%' : terminal.id === 'review' ? '58%' : '84%', backgroundColor: terminal.color, animation: `mcScreenScan ${terminal.id === 'owner' ? 2.8 : terminal.id === 'review' ? 2.3 : 2.1}s ease-in-out infinite` }"></div>
          </div>
        </div>
        <!-- 分区房间 -->
        <div
          v-for="room in engine.roomLayoutState.value"
          :key="room.id"
          class="absolute border border-cyan-400/15 shadow-[inset_0_0_0_1px_rgba(34,211,238,0.04),0_8px_24px_rgba(0,0,0,0.3)] cursor-pointer"
          :class="room.style"
          :style="{ left: `${room.x}%`, top: `${room.y}%`, width: `${room.w}%`, height: `${room.h}%`, backgroundImage: `linear-gradient(to bottom right, rgba(255,255,255,0.04), rgba(0,0,0,0.1)), url('/office-sprites/kenney/floorFull.png')`, backgroundSize: 'auto, 22% 22%', filter: palette.floorFilter }"
          @click.stop="selectRoom(room)"
        >
          <div class="absolute inset-0 pointer-events-none" :style="{ backgroundImage: `${palette.roomTone}, linear-gradient(to bottom right, rgba(255,255,255,0.08), transparent 45%)` }"></div>
          <div v-if="roomActivity(room.id).total > 0" class="absolute inset-0 pointer-events-none" :style="{ background: `radial-gradient(circle at 50% 48%, ${roomActivity(room.id).alerts > 0 ? 'rgba(244,63,94,0.22)' : roomActivity(room.id).busy > 0 ? 'rgba(251,191,36,0.18)' : 'rgba(52,211,153,0.12)'}, transparent 62%)`, animation: `mcRoomPulse ${roomActivity(room.id).busy > 0 ? 3.2 : 4.8}s ease-in-out infinite` }"></div>
          <div class="absolute left-2 top-1 rounded bg-black/50 backdrop-blur-sm border border-cyan-400/15 text-cyan-300/80 text-[9px] px-1.5 py-0.5 font-mono uppercase tracking-wide">{{ room.label }}</div>
          <div class="absolute right-2 top-1 flex items-center gap-1 rounded bg-black/55 border border-white/10 px-1.5 py-0.5 text-[8px] font-mono text-slate-200">
            <span class="h-1.5 w-1.5 rounded-full" :class="roomActivity(room.id).alerts > 0 ? 'bg-rose-500' : roomActivity(room.id).busy > 0 ? 'bg-amber-400' : 'bg-emerald-400'"></span>
            {{ roomActivity(room.id).total }}
          </div>
          <div class="absolute bottom-2 left-2 right-2 h-1 overflow-hidden rounded-full bg-black/35">
            <div class="h-full rounded-full" :style="{ width: `${activityLevel(room.id)}%`, background: roomActivity(room.id).alerts > 0 ? 'linear-gradient(90deg, rgba(244,63,94,0.2), rgba(244,63,94,0.9))' : roomActivity(room.id).busy > 0 ? 'linear-gradient(90deg, rgba(251,191,36,0.2), rgba(251,191,36,0.9))' : 'linear-gradient(90deg, rgba(52,211,153,0.16), rgba(52,211,153,0.65))' }"></div>
          </div>
        </div>
        <!-- 家具/道具 -->
        <div
          v-for="prop in engine.mapPropsState.value"
          :key="prop.id"
          class="absolute relative border shadow-[0_0_12px_rgba(108,164,255,0.18)] overflow-hidden cursor-pointer"
          :class="[prop.style, prop.border]"
          :style="{ left: `${prop.x}%`, top: `${prop.y}%`, width: `${prop.w}%`, height: `${prop.h}%` }"
          @click.stop="selectProp(prop)"
        >
          <img :src="getPropSprite(prop.id)" alt="" aria-hidden="true" draggable="false" class="absolute inset-0 w-full h-full object-contain opacity-95" :style="{ imageRendering: 'pixelated', filter: palette.spriteFilter }" />
        </div>
        <!-- 寻路连线 -->
        <svg class="absolute inset-0 w-full h-full pointer-events-none" aria-hidden="true">
          <line v-for="(edge, idx) in engine.pathEdges.value" :key="`edge-${idx}`" :x1="`${edge.x1}%`" :y1="`${edge.y1}%`" :x2="`${edge.x2}%`" :y2="`${edge.y2}%`" stroke="rgba(170, 203, 255, 0.42)" strokeWidth="2" strokeDasharray="4 6" />
        </svg>
        <!-- worker 精灵 -->
        <WorkerSprite
          v-for="w in engine.renderedWorkers.value"
          :key="w.agent.id"
          :worker="w"
          :sprite-filter="palette.spriteFilter"
          :sprite-frame="engine.spriteFrame.value"
          :transitioning="engine.transitioningAgentIds.value.has(w.agent.id)"
          :action="engine.agentActionOverrides.value.get(w.agent.id)"
          @select="onSelectAgent"
        />
      </div>
      <!-- 雷达小地图 -->
      <div
        v-if="engine.showMinimap.value"
        class="absolute right-3 bottom-3 z-30 w-44 h-28 rounded-md border border-cyan-400/15 bg-black/70 backdrop-blur-sm p-1.5"
        @mousedown.stop
        @click.stop="onMinimapClick"
      >
        <div class="text-[9px] text-cyan-300/60 font-mono uppercase tracking-wider mb-1">{{ TXT.radarLabel }}</div>
        <div class="relative w-full h-[calc(100%-16px)] rounded-sm overflow-hidden border border-cyan-400/10 bg-[#0a0f18]">
          <div v-for="room in engine.roomLayoutState.value" :key="`mini-${room.id}`" class="absolute border border-cyan-400/15 bg-cyan-400/5" :style="{ left: `${room.x}%`, top: `${room.y}%`, width: `${room.w}%`, height: `${room.h}%` }"></div>
          <div class="absolute left-[14%] top-[47%] w-[72%] h-[4%] bg-cyan-400/20"></div>
          <button
            v-for="worker in engine.renderedWorkers.value"
            :key="`mini-worker-${worker.agent.id}`"
            type="button"
            class="absolute w-2.5 h-2.5 rounded-full -translate-x-1/2 -translate-y-1/2 border border-black/40 h-auto p-0 min-w-0 hover:bg-transparent"
            :class="hashColor(worker.agent.name)"
            :style="{ left: `${worker.x}%`, top: `${worker.y}%` }"
            :title="worker.agent.name"
            @click.stop="onMinimapWorker(worker)"
          ></button>
        </div>
      </div>
      <!-- 事件流 + 热点编辑 -->
      <div v-if="engine.showEvents.value" class="absolute left-3 bottom-3 z-30 w-72 rounded-md border border-cyan-400/15 bg-black/80 backdrop-blur-sm p-2.5 space-y-2" @wheel.stop>
        <div class="text-[10px] text-cyan-300/60 font-mono uppercase tracking-wider">{{ TXT.deckLog }}</div>
        <div class="flex items-center gap-2 text-[10px] text-slate-400">
          <span class="inline-flex items-center gap-1"><span class="w-2 h-2 rounded-full bg-amber-400"></span>{{ TXT.legendActive }}</span>
          <span class="inline-flex items-center gap-1"><span class="w-2 h-2 rounded-full bg-emerald-400"></span>{{ TXT.legendStandby }}</span>
          <span class="inline-flex items-center gap-1"><span class="w-2 h-2 rounded-full bg-cyan-400"></span>{{ TXT.legendOther }}</span>
        </div>
        <div class="space-y-1.5 max-h-36 overflow-y-auto pr-1" @wheel.stop>
          <div v-if="engine.officeEvents.value.length === 0" class="text-[11px] text-slate-400">{{ TXT.noEventsYet }}</div>
          <div v-for="event in engine.officeEvents.value" :key="event.id" class="text-[11px] rounded px-2 py-1 bg-white/5 border border-white/10">
            <div class="flex items-center justify-between gap-2">
              <span class="uppercase font-mono text-[9px]" :class="event.severity === 'good' ? 'text-emerald-400' : event.severity === 'warn' ? 'text-amber-400' : 'text-cyan-300'">{{ event.kind }}</span>
              <span class="text-slate-400 text-[9px]">{{ formatLastSeen(Math.floor(event.at / 1000)) }}</span>
            </div>
            <div class="text-slate-200/80">{{ event.message }}</div>
          </div>
        </div>
        <div v-if="engine.selectedHotspot.value" class="rounded border border-cyan-400/15 bg-white/5 p-2">
          <div class="flex items-center justify-between">
            <div class="text-[11px] font-semibold text-slate-100">{{ engine.selectedHotspot.value.label }}</div>
            <div class="text-[9px] font-mono uppercase text-cyan-300/60">{{ engine.selectedHotspot.value.kind }}</div>
          </div>
          <div class="mt-1.5 space-y-1">
            <div v-for="line in engine.selectedHotspot.value.stats" :key="line" class="text-[10px] text-slate-300">{{ line }}</div>
          </div>
          <div class="mt-2 grid grid-cols-3 gap-1">
            <button type="button" class="h-auto py-1 text-[10px] border border-white/10 hover:bg-white/10 rounded" @click="engine.nudgeSelectedHotspot(0, -1)">{{ TXT.hotspotUp }}</button>
            <button type="button" class="h-auto py-1 text-[10px] border border-white/10 hover:bg-white/10 rounded" @click="engine.nudgeSelectedHotspot(-1, 0)">{{ TXT.hotspotLeft }}</button>
            <button type="button" class="h-auto py-1 text-[10px] border border-white/10 hover:bg-white/10 rounded" @click="engine.nudgeSelectedHotspot(1, 0)">{{ TXT.hotspotRight }}</button>
            <button type="button" class="h-auto py-1 text-[10px] border border-white/10 hover:bg-white/10 rounded" @click="engine.nudgeSelectedHotspot(0, 1)">{{ TXT.hotspotDown }}</button>
            <button type="button" class="h-auto py-1 text-[10px] border border-white/10 hover:bg-white/10 rounded" @click="engine.nudgeSelectedHotspot(-0.5, 0)">{{ TXT.hotspotFineMinusX }}</button>
            <button type="button" class="h-auto py-1 text-[10px] border border-white/10 hover:bg-white/10 rounded" @click="engine.nudgeSelectedHotspot(0.5, 0)">{{ TXT.hotspotFinePlusX }}</button>
          </div>
          <div v-if="engine.selectedHotspot.value.kind === 'room'" class="mt-1.5 grid grid-cols-2 gap-1">
            <button type="button" class="h-auto py-1 text-[10px] border border-white/10 hover:bg-white/10 rounded" @click="engine.resizeSelectedRoom(1, 0)">{{ TXT.hotspotWider }}</button>
            <button type="button" class="h-auto py-1 text-[10px] border border-white/10 hover:bg-white/10 rounded" @click="engine.resizeSelectedRoom(-1, 0)">{{ TXT.hotspotNarrower }}</button>
            <button type="button" class="h-auto py-1 text-[10px] border border-white/10 hover:bg-white/10 rounded" @click="engine.resizeSelectedRoom(0, 1)">{{ TXT.hotspotTaller }}</button>
            <button type="button" class="h-auto py-1 text-[10px] border border-white/10 hover:bg-white/10 rounded" @click="engine.resizeSelectedRoom(0, -1)">{{ TXT.hotspotShorter }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { CSSProperties } from 'vue'
import type { MapProp, MapRoom, OfficeAgent, OfficeHotspot, RenderedWorker, TimeTheme } from './types'
import {
  DELIVERY_TERMINALS, TXT, clamp, formatLastSeen, formatText,
  getAgentWorkCue, getInitials, getPropSprite, hashColor
} from './office-config'
import WorkerSprite from './WorkerSprite.vue'
import type { useOfficeEngine } from './useOfficeEngine'
import type { useTimeTheme } from './useTimeTheme'

const props = defineProps<{
  engine: ReturnType<typeof useOfficeEngine>
  theme: ReturnType<typeof useTimeTheme>
}>()

const emit = defineEmits<{ (e: 'select', agent: OfficeAgent): void }>()

const filters = [
  { key: 'all', label: TXT.filterAll },
  { key: 'working', label: TXT.filterWorking },
  { key: 'idle', label: TXT.filterIdle },
  { key: 'attention', label: TXT.filterNeedsAttention }
] as const

const themes: TimeTheme[] = ['dawn', 'day', 'dusk', 'night']

const palette = computed(() => props.theme.themePalette.value)

const viewportStyle = computed<CSSProperties>(() => ({
  backgroundColor: '#0a0f18',
  backgroundImage: `${palette.value.shell}, linear-gradient(90deg, ${palette.value.gridLine} 1px, transparent 1px), linear-gradient(${palette.value.gridLine} 1px, transparent 1px)`,
  backgroundSize: 'auto, 64px 64px, 64px 64px'
}))

const dawnStyle = computed<CSSProperties>(() => ({
  background: `linear-gradient(115deg, transparent 8%, ${palette.value.accentGlow} 24%, transparent 42%)`,
  mixBlendMode: 'screen',
  animation: 'mcSunSweep 17s ease-in-out infinite'
}))
const dayStyle1 = computed<CSSProperties>(() => ({
  background: `linear-gradient(112deg, transparent 10%, ${palette.value.accentGlow} 24%, transparent 44%)`,
  mixBlendMode: 'screen',
  animation: 'mcSunSweep 16s ease-in-out infinite'
}))
const dayStyle2 = computed<CSSProperties>(() => ({
  background: 'linear-gradient(96deg, transparent 24%, rgba(255,255,255,0.15) 38%, transparent 58%)',
  mixBlendMode: 'screen',
  animation: 'mcSunSweepReverse 20s ease-in-out infinite'
}))
const duskStyle = computed<CSSProperties>(() => ({
  background: `radial-gradient(circle at 50% 22%, ${palette.value.accentGlow} 0, transparent 56%)`,
  mixBlendMode: 'screen',
  animation: 'mcDuskPulse 7.5s ease-in-out infinite'
}))
const nightStyle = computed<CSSProperties>(() => ({
  background: `radial-gradient(circle at 18% 12%, ${palette.value.accentGlow} 0, transparent 44%), radial-gradient(circle at 82% 16%, rgba(138,178,255,0.2) 0, transparent 42%)`,
  mixBlendMode: 'screen',
  animation: 'mcNightBloom 8.5s ease-in-out infinite'
}))

const setViewportRef = (el: Element | null) => {
  ;(props.engine as any).mapViewportRef.value = el as HTMLDivElement | null
}

const workCue = (agent: OfficeAgent) => getAgentWorkCue(agent, props.engine.agentActionOverrides.value.get(agent.id))
const roomActivity = (id: string) => props.engine.roomActivityById.value.get(id) || { total: 0, busy: 0, idle: 0, alerts: 0, taskLoad: 0 }
const activityLevel = (id: string) => {
  const a = roomActivity(id)
  return a.total > 0 ? Math.min(100, 28 + a.busy * 18 + a.taskLoad * 8 + a.alerts * 16) : 12
}

const onWheel = (e: WheelEvent) => props.engine.onMapWheel(e)
const onMouseDown = (e: MouseEvent) => props.engine.onMapMouseDown(e)
const onMouseMove = (e: MouseEvent) => props.engine.onMapMouseMove(e)
const onSelectAgent = (agent: OfficeAgent) => emit('select', agent)

const selectRoster = (agent: OfficeAgent) => {
  props.engine.selectedAgent.value = agent
  const worker = props.engine.renderedWorkers.value.find((w) => w.agent.id === agent.id)
  if (worker) props.engine.focusMapPoint(worker.x, worker.y)
}

const selectRoom = (room: MapRoom) => {
  const a = roomActivity(room.id)
  const hotspot: OfficeHotspot = {
    kind: 'room', id: room.id, label: room.label,
    x: room.x + room.w / 2, y: room.y + room.h / 2,
    stats: [`${a.total} 名成员`, `${a.busy} 活动 / ${a.idle} 待命 / ${a.alerts} 告警`, `${a.taskLoad} 可见任务交接`]
  }
  props.engine.selectedHotspot.value = hotspot
  props.engine.pushOfficeEvent({
    kind: 'room',
    severity: a.alerts > 0 ? 'warn' : a.busy > 0 ? 'good' : 'info',
    message: `${room.label} 检视（${a.total} 名成员，${a.taskLoad} 交接）。`
  })
}

const selectProp = (prop: MapProp) => {
  const nearest = props.engine.renderedWorkers.value.slice()
    .sort((a, b) => Math.hypot(a.x - prop.x, a.y - prop.y) - Math.hypot(b.x - prop.x, b.y - prop.y))[0]
  const label = prop.id.replace(/^desk-/, '工位 ').replace(/^plant-/, '植物 ').replace(/^kitchen$/, '休息区地毯')
  props.engine.selectedHotspot.value = {
    kind: 'desk', id: prop.id, label,
    x: prop.x + prop.w / 2, y: prop.y + prop.h / 2,
    stats: [nearest ? `最近成员：${nearest.agent.name}` : '附近无成员', `占地 ${prop.w.toFixed(1)}x${prop.h.toFixed(1)}`, '在成员弹窗中使用动作按钮']
  }
  props.engine.pushOfficeEvent({ kind: 'desk', severity: 'info', message: `${prop.id} 检视${nearest ? `，邻近 ${nearest.agent.name}` : ''}。` })
}

const onMinimapClick = (event: MouseEvent) => {
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const x = clamp(((event.clientX - rect.left) / rect.width) * 100, 0, 100)
  const y = clamp(((event.clientY - rect.top) / rect.height) * 100, 0, 100)
  props.engine.focusMapPoint(x, y)
}
const onMinimapWorker = (worker: RenderedWorker) => {
  props.engine.selectedAgent.value = worker.agent
  props.engine.focusMapPoint(worker.x, worker.y)
}
</script>

<style>
@keyframes mcSunSweep {
  0% { transform: translateX(-10%) translateY(-2%); opacity: 0.34; }
  50% { transform: translateX(8%) translateY(2%); opacity: 0.56; }
  100% { transform: translateX(-10%) translateY(-2%); opacity: 0.34; }
}
@keyframes mcSunSweepReverse {
  0% { transform: translateX(8%) translateY(2%); opacity: 0.18; }
  50% { transform: translateX(-8%) translateY(-2%); opacity: 0.32; }
  100% { transform: translateX(8%) translateY(2%); opacity: 0.18; }
}
@keyframes mcDuskPulse {
  0% { opacity: 0.28; transform: scale(1); }
  50% { opacity: 0.52; transform: scale(1.03); }
  100% { opacity: 0.28; transform: scale(1); }
}
@keyframes mcNightBloom {
  0% { opacity: 0.25; }
  50% { opacity: 0.5; }
  100% { opacity: 0.25; }
}
@keyframes mcTwinkle {
  0% { opacity: 0.25; transform: scale(0.9); }
  50% { opacity: 1; transform: scale(1.15); }
  100% { opacity: 0.25; transform: scale(0.9); }
}
@keyframes mcRoomPulse {
  0% { opacity: 0.35; transform: scale(0.98); }
  50% { opacity: 0.8; transform: scale(1.02); }
  100% { opacity: 0.35; transform: scale(0.98); }
}
@keyframes mcScreenScan {
  0% { transform: translateX(-140%); opacity: 0; }
  35% { opacity: 0.85; }
  70% { opacity: 0.5; }
  100% { transform: translateX(260%); opacity: 0; }
}
@keyframes mcTypingLine {
  0% { transform: scaleX(0.35); opacity: 0.38; }
  50% { transform: scaleX(1); opacity: 1; }
  100% { transform: scaleX(0.55); opacity: 0.52; }
}
</style>

