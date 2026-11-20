<script setup>
import { ref, watch } from 'vue'
import { adminApi } from '@/api'
import { attachmentKind } from '@/utils/attachments'

const props = defineProps({ url: String, name: { type: String, default: '附件预览' } })
const src = ref('')
const failed = ref(false)
watch(() => props.url, async (url, _, cleanup) => {
  src.value = ''
  failed.value = false
  if (!url) return
  if (!attachmentKind(url)) { src.value = url; return }
  const request = new AbortController()
  let objectUrl
  cleanup(() => {
    request.abort()
    if (objectUrl) URL.revokeObjectURL(objectUrl)
  })
  try {
    const blob = await adminApi.attachmentContent(url, request.signal)
    if (!request.signal.aborted) src.value = objectUrl = URL.createObjectURL(blob)
  } catch {
    if (!request.signal.aborted) failed.value = true
  }
}, { immediate: true })
</script>

<template>
  <div v-if="url">
    <p v-if="failed" role="alert">附件预览加载失败</p>
    <a-spin v-else-if="!src" />
    <audio v-else-if="attachmentKind(url) === 'audio'" class="attachment-media" :src="src" controls :aria-label="name" />
    <video v-else-if="attachmentKind(url) === 'video'" class="attachment-media" :src="src" controls :aria-label="name" />
    <img v-else class="attachment-media attachment-image" :src="src" :alt="name" @error="failed = true" />
  </div>
</template>
