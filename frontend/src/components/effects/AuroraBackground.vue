<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useReducedMotion } from '@/composables/useMotionPrefs'

/**
 * 背景动画：Canvas 粒子网络 + 鼠标斥力，配合 CSS 极光光斑与网格。
 * 性能约束：
 *  - 粒子数按视口面积推导并设上限；
 *  - 页面隐藏时停止 rAF；
 *  - prefers-reduced-motion 时只画静态粒子，不做动画。
 */
const canvasRef = ref(null)
const reduced = useReducedMotion()

let ctx = null
let rafId = 0
let particles = []
let width = 0
let height = 0
let dpr = 1
const pointer = { x: -9999, y: -9999, active: false }

const LINK_DIST = 132
const POINTER_DIST = 168

const colors = [
  [99, 102, 241],
  [34, 211, 238],
  [168, 85, 247]
]

function targetCount() {
  const area = width * height
  const count = Math.round(area / 19000)
  return Math.min(Math.max(count, 26), 96)
}

function createParticle() {
  const palette = colors[Math.floor(Math.random() * colors.length)]
  return {
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * 0.34,
    vy: (Math.random() - 0.5) * 0.34,
    r: Math.random() * 1.7 + 0.7,
    color: palette
  }
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas) {
    return
  }
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  width = window.innerWidth
  height = window.innerHeight
  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

  const count = targetCount()
  if (particles.length > count) {
    particles = particles.slice(0, count)
  } else {
    while (particles.length < count) {
      particles.push(createParticle())
    }
  }
  particles.forEach((p) => {
    p.x = Math.min(p.x, width)
    p.y = Math.min(p.y, height)
  })
}

/** 网格分桶降低连线计算量：只比较相邻桶内粒子。 */
function draw() {
  ctx.clearRect(0, 0, width, height)

  const bucketSize = LINK_DIST
  const cols = Math.ceil(width / bucketSize) + 1
  const rows = Math.ceil(height / bucketSize) + 1
  const buckets = new Map()

  for (const p of particles) {
    p.x += p.vx
    p.y += p.vy

    if (p.x < -20) p.x = width + 20
    if (p.x > width + 20) p.x = -20
    if (p.y < -20) p.y = height + 20
    if (p.y > height + 20) p.y = -20

    if (pointer.active) {
      const dx = p.x - pointer.x
      const dy = p.y - pointer.y
      const dist = Math.hypot(dx, dy)
      if (dist < POINTER_DIST && dist > 0.01) {
        const force = (1 - dist / POINTER_DIST) * 0.9
        p.x += (dx / dist) * force
        p.y += (dy / dist) * force
      }
    }

    const key = `${Math.floor(p.x / bucketSize)}:${Math.floor(p.y / bucketSize)}`
    if (!buckets.has(key)) {
      buckets.set(key, [])
    }
    buckets.get(key).push(p)
  }

  // 连线
  for (let ci = 0; ci < cols; ci++) {
    for (let ri = 0; ri < rows; ri++) {
      const cell = buckets.get(`${ci}:${ri}`)
      if (!cell) continue
      for (let di = 0; di <= 1; di++) {
        for (let dj = -1; dj <= 1; dj++) {
          if (di === 0 && dj < 0) continue
          const neighbor = buckets.get(`${ci + di}:${ri + dj}`)
          if (!neighbor) continue
          const same = di === 0 && dj === 0
          for (let i = 0; i < cell.length; i++) {
            const start = same ? i + 1 : 0
            for (let j = start; j < neighbor.length; j++) {
              const a = cell[i]
              const b = neighbor[j]
              const dx = a.x - b.x
              const dy = a.y - b.y
              const dist = Math.hypot(dx, dy)
              if (dist > LINK_DIST) continue
              const alpha = (1 - dist / LINK_DIST) * 0.24
              ctx.strokeStyle = `rgba(99, 102, 241, ${alpha})`
              ctx.lineWidth = 0.75
              ctx.beginPath()
              ctx.moveTo(a.x, a.y)
              ctx.lineTo(b.x, b.y)
              ctx.stroke()
            }
          }
        }
      }
    }
  }

  // 粒子
  for (const p of particles) {
    const [r, g, b] = p.color
    ctx.beginPath()
    ctx.fillStyle = `rgba(${r}, ${g}, ${b}, 0.75)`
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fill()
  }

  rafId = requestAnimationFrame(draw)
}

function start() {
  if (rafId) return
  rafId = requestAnimationFrame(draw)
}

function stop() {
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = 0
  }
}

function onPointerMove(event) {
  pointer.x = event.clientX
  pointer.y = event.clientY
  pointer.active = true
}

function onPointerLeave() {
  pointer.active = false
  pointer.x = -9999
  pointer.y = -9999
}

function onVisibility() {
  if (document.hidden) {
    stop()
  } else if (!reduced.value) {
    start()
  }
}

let resizeTimer = 0
function onResize() {
  clearTimeout(resizeTimer)
  resizeTimer = window.setTimeout(resize, 180)
}

onMounted(() => {
  resize()
  if (reduced.value) {
    drawStatic()
    return
  }
  window.addEventListener('resize', onResize)
  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener('pointerleave', onPointerLeave)
  document.addEventListener('visibilitychange', onVisibility)
  start()
})

/** 静态降级：画一次带连线的粒子图，不做任何运动。 */
function drawStatic() {
  ctx.clearRect(0, 0, width, height)
  for (const p of particles) {
    const [r, g, b] = p.color
    ctx.beginPath()
    ctx.fillStyle = `rgba(${r}, ${g}, ${b}, 0.5)`
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fill()
  }
}

onBeforeUnmount(() => {
  stop()
  clearTimeout(resizeTimer)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerleave', onPointerLeave)
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<template>
  <div class="aurora-stage">
    <div class="aurora-blob b1" />
    <div class="aurora-blob b2" />
    <div class="aurora-blob b3" />
    <div class="aurora-grid" />
    <canvas ref="canvasRef" class="particle-canvas" aria-hidden="true" />
  </div>
</template>
