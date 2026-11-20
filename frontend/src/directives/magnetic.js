/**
 * v-magnetic：鼠标靠近时元素轻微吸附位移。
 * 用法：<a-button v-magnetic>…</a-button>
 */
const STRENGTH = 0.18
const MAX_OFFSET = 8

export default {
  mounted(el) {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    if (!window.matchMedia('(pointer: fine)').matches) return

    el.classList.add('magnetic')

    const onMove = (event) => {
      const rect = el.getBoundingClientRect()
      const relX = event.clientX - (rect.left + rect.width / 2)
      const relY = event.clientY - (rect.top + rect.height / 2)
      const dx = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, relX * STRENGTH))
      const dy = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, relY * STRENGTH))
      el.style.setProperty('--mx', `${dx}px`)
      el.style.setProperty('--my', `${dy}px`)
    }

    const onLeave = () => {
      el.style.setProperty('--mx', '0px')
      el.style.setProperty('--my', '0px')
    }

    el.addEventListener('pointermove', onMove)
    el.addEventListener('pointerleave', onLeave)
    el.__magneticCleanup = () => {
      el.removeEventListener('pointermove', onMove)
      el.removeEventListener('pointerleave', onLeave)
    }
  },
  unmounted(el) {
    el.__magneticCleanup?.()
  }
}
