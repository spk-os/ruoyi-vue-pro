<template>
  <div>
    <!-- 椅子（座位下方） -->
    <div
      class="absolute -translate-x-1/2 pointer-events-none"
      :style="{ left: xPercent, top: `calc(${yPercent} - 14px)` }"
    >
      <img
        src="/office-sprites/kenney/chairDesk.png"
        alt=""
        aria-hidden="true"
        draggable="false"
        class="w-6 h-6 object-contain opacity-90"
        :style="{ imageRendering: 'pixelated' }"
      />
    </div>

    <!-- 桌子 + 屏幕（座位上方） -->
    <div
      class="absolute -translate-x-1/2 pointer-events-none"
      :style="{ left: xPercent, top: `calc(${yPercent} - 56px)` }"
    >
      <div class="relative w-16 h-9">
        <img
          src="/office-sprites/kenney/desk.png"
          alt=""
          aria-hidden="true"
          draggable="false"
          class="w-16 h-9 object-contain opacity-95"
          :style="{ imageRendering: 'pixelated', filter: spriteFilter }"
        />
        <img
          src="/office-sprites/kenney/computerScreen.png"
          alt=""
          aria-hidden="true"
          draggable="false"
          class="absolute left-1/2 -translate-x-1/2 top-[6px] w-7 h-2 object-contain opacity-95"
          :style="{ imageRendering: 'pixelated', filter: spriteFilter }"
        />
        <div
          class="absolute left-1/2 top-[4px] h-3 w-8 -translate-x-1/2 overflow-hidden rounded-sm border"
          :class="hasLiveWork ? 'border-amber-200/30 bg-cyan-950/80' : 'border-slate-400/10 bg-slate-950/70'"
        >
          <span
            v-for="line in [0, 1, 2]"
            :key="`${agent.id}-screen-${line}`"
            class="absolute left-1 h-px rounded-full"
            :class="hasLiveWork ? 'bg-cyan-200/80' : 'bg-slate-500/35'"
            :style="{
              top: `${3 + line * 3}px`,
              width: `${11 + ((hashNumber(agent.name) + line * 5) % 14)}px`,
              animation: hasLiveWork ? `mcTypingLine ${1.15 + line * 0.2}s ease-in-out ${line * 0.18}s infinite` : undefined
            }"
          />
          <span
            v-if="hasLiveWork"
            class="absolute inset-y-0 w-1 bg-white/25"
            style="animation: mcScreenScan 1.8s ease-in-out infinite"
          />
        </div>
      </div>
    </div>

    <!-- 点击主体（精灵 + 名称 + 浮标） -->
    <button
      type="button"
      class="absolute -translate-x-1/2 -translate-y-1/2 transition-all duration-500 hover:scale-110 h-auto p-0 rounded-none hover:bg-transparent cursor-pointer"
      :style="{ left: xPercent, top: yPercent }"
      @click="emit('select', agent)"
    >
      <!-- 名称 -->
      <div
        class="absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full bg-black/70 border border-white/10 text-white text-[11px] px-2 py-0.5 shadow-[0_0_12px_rgba(0,0,0,0.4)]"
      >
        <span class="inline-block w-2 h-2 rounded-full mr-1" :class="statusDot[agent.status]" />
        {{ agent.name }}
      </div>
      <!-- 工作浮标 -->
      <div
        class="absolute -top-14 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border px-2 py-0.5 text-[9px] font-mono uppercase tracking-wide"
        :class="cue.toneClass"
        :style="{ boxShadow: `0 0 16px ${cue.glowColor}` }"
      >
        <span class="mr-1" :class="agent.status === 'busy' ? 'animate-bounce' : 'animate-pulse'">{{ emote }}</span>
        {{ cue.label }}
      </div>
      <!-- 精灵 -->
      <div class="relative w-8 h-12 mx-auto">
        <div
          class="absolute inset-0"
          :class="(transitioning || isMoving) ? 'animate-pulse' : ''"
          :style="spriteStyle"
        />
        <div class="absolute left-[8px] top-[14px] w-4 h-3 border border-black/60" :class="hashColor(agent.name)" />
      </div>
      <div v-if="!isMoving" class="text-[9px] text-slate-300 font-mono mt-0.5">#{{ seatLabel }}</div>
    </button>

    <!-- 工作明细 -->
    <div
      v-if="hasLiveWork"
      class="absolute -translate-x-1/2 rounded border px-1.5 py-0.5 text-[9px] leading-none"
      :class="cue.toneClass"
      :style="{ left: xPercent, top: `calc(${yPercent} - 24px)`, boxShadow: `0 0 12px ${cue.glowColor}` }"
    >
      {{ cue.detail }}
    </div>

    <!-- 移动中标记 -->
    <div
      v-if="transitioning || isMoving"
      class="absolute -translate-x-1/2 text-[9px] text-slate-200/85 font-medium px-1.5 py-0.5 rounded bg-black/45 border border-white/10"
      :style="{ left: xPercent, top: `calc(${yPercent} + 22px)` }"
    >
      {{ TXT.moving }}
    </div>

    <!-- 分区标签 -->
    <div
      class="absolute text-[9px] text-slate-500/70 font-mono pointer-events-none"
      :style="{ left: xPercent, top: `calc(${yPercent} + 38px)` }"
    >
      {{ zoneLabel }}
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { CSSProperties } from 'vue'
import type { OfficeAction, OfficeAgent, RenderedWorker, WorkCue } from './types'
import {
  HERO_SHEET_COLS,
  HERO_SHEET_ROWS,
  TXT,
  getAgentWorkCue,
  getStatusEmote,
  getWorkerHeroFrame,
  hashColor,
  hashNumber,
  statusDot
} from './office-config'

const props = defineProps<{
  worker: RenderedWorker
  spriteFilter: string
  spriteFrame: number
  transitioning: boolean
  action?: OfficeAction
}>()

const emit = defineEmits<{ (e: 'select', agent: OfficeAgent): void }>()

const agent = computed(() => props.worker.agent)
const isMoving = computed(() => props.worker.isMoving)
const xPercent = computed(() => `${props.worker.x}%`)
const yPercent = computed(() => `${props.worker.y}%`)
const seatLabel = computed(() => props.worker.seatLabel)
const zoneLabel = computed(() => props.worker.zoneLabel)

const cue = computed<WorkCue>(() => getAgentWorkCue(agent.value, props.action))
const emote = computed(() => getStatusEmote(agent.value.status))
const hasLiveWork = computed(
  () => agent.value.status === 'busy' || props.action != null
)

const spriteStyle = computed<CSSProperties>(() => {
  const frame = getWorkerHeroFrame(agent.value.status, isMoving.value, props.spriteFrame)
  const xPct = (frame.col / (HERO_SHEET_COLS - 1)) * 100
  const yPct = (frame.row / (HERO_SHEET_ROWS - 1)) * 100
  const flip =
    isMoving.value &&
    Math.abs(props.worker.direction.dx) > Math.abs(props.worker.direction.dy) &&
    props.worker.direction.dx < 0
  return {
    backgroundImage: `url('/office-sprites/cc0-hero/player_full_animation.png')`,
    backgroundRepeat: 'no-repeat',
    backgroundSize: `${HERO_SHEET_COLS * 100}% ${HERO_SHEET_ROWS * 100}%`,
    backgroundPosition: `${xPct}% ${yPct}%`,
    imageRendering: 'pixelated',
    filter: props.spriteFilter,
    transform: flip ? 'scaleX(-1)' : undefined,
    transformOrigin: 'center'
  }
})
</script>
