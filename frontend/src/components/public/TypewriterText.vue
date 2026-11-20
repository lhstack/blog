<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({ texts: { type: Array, required: true } })
const current = ref(0)
const visible = ref(props.texts[0] || '')
const reducedMotion = ref(true)
const segmenter = typeof Intl.Segmenter === 'function' ? new Intl.Segmenter('zh', { granularity: 'grapheme' }) : null
let timer
let motionPreference

function syncMotion() {
  reducedMotion.value = motionPreference.matches
}

onMounted(() => {
  motionPreference = window.matchMedia('(prefers-reduced-motion: reduce)')
  motionPreference.addEventListener('change', syncMotion)
  syncMotion()
})

watch([() => props.texts, reducedMotion], ([texts, reduced]) => {
  clearTimeout(timer)
  current.value = 0
  visible.value = texts[0] || ''
  if (reduced || !texts.length) return

  function startLine() {
    const text = texts[current.value]
    const chars = segmenter ? Array.from(segmenter.segment(text), ({ segment }) => segment) : Array.from(text)
    let length = 0
    visible.value = ''

    function type() {
      visible.value = chars.slice(0, ++length).join('')
      timer = length === chars.length ? setTimeout(erase, 3000) : setTimeout(type, 85)
    }

    function erase() {
      visible.value = chars.slice(0, --length).join('')
      if (length > 0) timer = setTimeout(erase, 55)
      else timer = setTimeout(() => {
        current.value = (current.value + 1) % texts.length
        startLine()
      }, 350)
    }

    timer = setTimeout(type, 85)
  }

  startLine()
})

onBeforeUnmount(() => {
  clearTimeout(timer)
  motionPreference?.removeEventListener('change', syncMotion)
})
</script>

<template>
  <span class="typewriter">
    <span v-for="(line, index) in texts" :key="index" class="full-text" :aria-hidden="index !== current">{{ line }}</span>
    <span class="animated-text" aria-hidden="true">{{ visible }}<span v-if="!reducedMotion" class="caret" /></span>
  </span>
</template>

<style scoped>
.typewriter {
  position: relative;
  display: inline-grid;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.full-text,
.animated-text {
  grid-area: 1 / 1;
}

/* 保留完整文案所占空间，也让辅助技术只读一遍完整文案。 */
.full-text {
  opacity: 0;
  user-select: none;
}

.caret {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-inline-start: 2px;
  vertical-align: -0.12em;
  background: var(--brand-1);
}
</style>
