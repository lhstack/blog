<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useFinePointer, useReducedMotion } from '@/composables/useMotionPrefs'

/**
 * 鼠标跟随：光晕做惯性插值（lerp），光标点做快速跟随。
 * 仅在精确指针设备与未开启减少动效时启用；触摸设备完全不渲染。
 */
const glowRef = ref(null)
const dotRef = ref(null)
const visible = ref(false)
const active = ref(false)
const reduced = useReducedMotion()
const finePointer = useFinePointer()

const target = { x: window.innerWidth / 2, y: window.innerHeight / 2 }
const glow = { x: target.x, y: target.y }
const dot = { x: target.x, y: target.y }

const GLOW_EASE = 0.12
const DOT_EASE = 0.42

let rafId = 0
let running = false

function tick() {
  glow.x += (target.x - glow.x) * GLOW_EASE
  glow.y += (target.y - glow.y) * GLOW_EASE
  dot.x += (target.x - dot.x) * DOT_EASE
  dot.y += (target.y - dot.y) * DOT_EASE

  if (glowRef.value) {
    glowRef.value.style.transform = `translate3d(${glow.x}px, ${glow.y}px, 0)`
  }
  if (dotRef.value) {
    dotRef.value.style.transform = `translate3d(${dot.x}px, ${dot.y}px, 0)`
  }
  rafId = requestAnimationFrame(tick)
}

function start() {
  if (running) return
  running = true
  rafId = requestAnimationFrame(tick)
}

function stop() {
  running = false
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = 0
  }
}

function onMove(event) {
  target.x = event.clientX
  target.y = event.clientY
  if (!visible.value) {
    visible.value = true
    // 首次出现时避免从左上角飞过来
    glow.x = dot.x = target.x
    glow.y = dot.y = target.y
  }
}

function onOver(event) {
  const target_ = event.target
  const interactive =
    target_ instanceof Element &&
    target_.closest('a, button, .ant-btn, .ant-card, .hover-lift, input, textarea, [role="button"]')
  active.value = Boolean(interactive)
}

function onLeaveWindow() {
  visible.value = false
}

function onDown() {
  active.value = true
}

function onUp(event) {
  onOver(event)
}

onMounted(() => {
  if (reduced.value || !finePointer.value) {
    return
  }
  window.addEventListener('pointermove', onMove, { passive: true })
  window.addEventListener('pointerover', onOver, { passive: true })
  window.addEventListener('pointerdown', onDown, { passive: true })
  window.addEventListener('pointerup', onUp, { passive: true })
  document.addEventListener('mouseleave', onLeaveWindow)
  start()
})

onBeforeUnmount(() => {
  stop()
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerover', onOver)
  window.removeEventListener('pointerdown', onDown)
  window.removeEventListener('pointerup', onUp)
  document.removeEventListener('mouseleave', onLeaveWindow)
})
</script>

<template>
  <div v-if="!reduced && finePointer" aria-hidden="true">
    <div ref="glowRef" class="cursor-glow" :class="{ 'is-visible': visible }" />
    <div
      ref="dotRef"
      class="cursor-dot"
      :class="{ 'is-visible': visible, 'is-active': active }"
    />
  </div>
</template>
