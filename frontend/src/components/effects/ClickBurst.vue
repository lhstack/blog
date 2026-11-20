<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { useReducedMotion } from '@/composables/useMotionPrefs'

/**
 * 点击特效：主涟漪 + 放射粒子。
 * 元素在动画结束后立即移除，DOM 不累积；仅在未开启减少动效时工作。
 */
const reduced = useReducedMotion()
const SPARK_COUNT = 8
const PALETTE = ['#6366f1', '#22d3ee', '#a855f7', '#f472b6']

const activeNodes = new Map()
const NODE_LIMIT = 90
let lastSlide = 0

function addEffect(node, lifetime) {
  if (activeNodes.size >= NODE_LIMIT) removeEffect(activeNodes.keys().next().value)
  document.body.appendChild(node)
  activeNodes.set(node, window.setTimeout(() => removeEffect(node), lifetime))
}

function removeEffect(node) {
  window.clearTimeout(activeNodes.get(node))
  activeNodes.delete(node)
  node.remove()
}

function spawn(x, y) {
  const ripple = document.createElement('span')
  ripple.className = 'click-ripple'
  ripple.style.left = `${x}px`
  ripple.style.top = `${y}px`
  ripple.style.borderColor = 'rgba(99, 102, 241, 0.55)'
  addEffect(ripple, 660)

  for (let i = 0; i < SPARK_COUNT; i++) {
    const spark = document.createElement('span')
    spark.className = 'click-spark'
    const angle = (Math.PI * 2 * i) / SPARK_COUNT + Math.random() * 0.4
    const distance = 34 + Math.random() * 40
    spark.style.left = `${x}px`
    spark.style.top = `${y}px`
    spark.style.background = PALETTE[i % PALETTE.length]
    spark.style.boxShadow = `0 0 10px ${PALETTE[i % PALETTE.length]}`
    spark.style.setProperty('--dx', `${Math.cos(angle) * distance}px`)
    spark.style.setProperty('--dy', `${Math.sin(angle) * distance}px`)
    addEffect(spark, 680)
  }
}

function onClick(event) {
  // 忽略拖拽与多指操作，只响应主键单击
  if (reduced.value || !event.isPrimary || event.button !== 0) return
  spawn(event.clientX, event.clientY)
}

function onSlide(event) {
  if (reduced.value || !event.isPrimary || event.pointerType === 'touch' || event.timeStamp - lastSlide < 65) return
  lastSlide = event.timeStamp
  const dot = document.createElement('span')
  dot.className = 'slide-ripple'
  dot.style.left = `${event.clientX}px`
  dot.style.top = `${event.clientY}px`
  addEffect(dot, 380)
}

onMounted(() => {
  window.addEventListener('pointerdown', onClick, { passive: true })
  window.addEventListener('pointermove', onSlide, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('pointerdown', onClick)
  window.removeEventListener('pointermove', onSlide)
  for (const node of activeNodes.keys()) removeEffect(node)
})
</script>

<template>
  <span />
</template>
