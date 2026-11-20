import { ref, onMounted, onBeforeUnmount } from 'vue'

/**
 * 统一的「减少动效」探测。
 * 用户系统开启 prefers-reduced-motion 时，所有特效组件应直接降级为静态。
 */
export function useReducedMotion() {
  const reduced = ref(false)
  let mql = null
  const onChange = (event) => {
    reduced.value = event.matches
  }

  onMounted(() => {
    mql = window.matchMedia('(prefers-reduced-motion: reduce)')
    reduced.value = mql.matches
    mql.addEventListener('change', onChange)
  })

  onBeforeUnmount(() => {
    if (mql) {
      mql.removeEventListener('change', onChange)
    }
  })

  return reduced
}

/** 是否为精确指针（桌面鼠标），触摸设备不启用鼠标跟随类特效。 */
export function useFinePointer() {
  const fine = ref(false)
  let mql = null
  const onChange = (event) => {
    fine.value = event.matches
  }

  onMounted(() => {
    mql = window.matchMedia('(pointer: fine)')
    fine.value = mql.matches
    mql.addEventListener('change', onChange)
  })

  onBeforeUnmount(() => {
    if (mql) {
      mql.removeEventListener('change', onChange)
    }
  })

  return fine
}
