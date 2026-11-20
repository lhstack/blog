<script setup>
import { computed, ref, watch } from 'vue'
import { adminApi } from '@/api'
import { downloadPrivate } from '@/utils/privateAttachments'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({ content: String })
const root = ref(null)
const html = computed(() => renderMarkdown(props.content, true))

watch([html, root], ([, container], _, cleanup) => {
  if (!container) return
  const request = new AbortController()
  const urls = new Set()
  async function resolveMedia(node) {
    try {
      const blob = await adminApi.attachmentContent(node.dataset.attachmentSrc, request.signal)
      if (request.signal.aborted) return
      const url = URL.createObjectURL(blob)
      urls.add(url)
      node.src = url
    } catch {
      if (!request.signal.aborted) node.insertAdjacentText('afterend', '附件预览加载失败')
    }
  }
  const observer = new IntersectionObserver((entries) => entries.forEach(({ target, isIntersecting }) => {
    if (!isIntersecting) return
    observer.unobserve(target)
    resolveMedia(target)
  }))
  container.querySelectorAll('[data-attachment-src]').forEach(node => observer.observe(node))
  cleanup(() => {
    request.abort()
    observer.disconnect()
    urls.forEach(url => URL.revokeObjectURL(url))
  })
}, { flush: 'post' })

async function download(event) {
  const link = event.target.closest('[data-attachment-link]')
  if (!link) return
  event.preventDefault()
  try { await downloadPrivate(link.dataset.attachmentLink, link.textContent) } catch { /* API already reports failures */ }
}
</script>

<template>
  <div ref="root" class="markdown-body" v-html="html" @click="download" />
</template>
