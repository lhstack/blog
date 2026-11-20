<script setup>
import { ref, watch } from 'vue'
import { adminApi } from '@/api'
import AdminAttachmentPreview from './AdminAttachmentPreview.vue'
import AdminAttachmentMarkdown from './AdminAttachmentMarkdown.vue'

const props = defineProps({ articleId: Number })
const emit = defineEmits(['close'])
const article = ref(null)
const loading = ref(false)

watch(() => props.articleId, async (id, _, onCleanup) => {
  article.value = null
  loading.value = id != null
  if (id == null) return
  const request = new AbortController()
  onCleanup(() => request.abort())
  try {
    const data = await adminApi.article(id, { signal: request.signal })
    if (!request.signal.aborted) article.value = data
  } catch { /* HTTP 层已提示 */ } finally {
    if (!request.signal.aborted) loading.value = false
  }
}, { immediate: true })
</script>

<template>
  <a-modal :open="articleId != null" title="文章预览" :footer="null" :width="860" @cancel="emit('close')">
    <div class="preview-body">
      <a-skeleton v-if="loading" active />
      <article v-else-if="article">
        <a-tag :color="article.status === 'PUBLISHED' ? 'green' : 'gold'">{{ article.status === 'PUBLISHED' ? '已发布' : '草稿' }}</a-tag>
        <h1>{{ article.title }}</h1>
        <p v-if="article.summary" class="muted">{{ article.summary }}</p>
        <AdminAttachmentPreview v-if="article.cover" :url="article.cover" :name="article.title" class="preview-cover" />
        <AdminAttachmentMarkdown :content="article.content" />
      </article>
      <a-empty v-else description="预览加载失败，请关闭后重试" />
    </div>
  </a-modal>
</template>

<style scoped>
.preview-body { max-height: 65vh; overflow: auto; overflow-wrap: anywhere; }
.preview-cover { display: block; max-width: 100%; max-height: 340px; object-fit: cover; margin-bottom: 16px; border-radius: 12px; }
</style>
