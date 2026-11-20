<script setup>
import { computed, onMounted, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message } from 'ant-design-vue'
import { adminApi } from '@/api'
import AboutMarkdown from '@/components/public/AboutMarkdown.vue'

const ABOUT_LIMIT = 50_000
const markdown = ref('')
const savedMarkdown = ref('')
const loading = ref(true)
const loadFailed = ref(false)
const saving = ref(false)
const previewOpen = ref(false)
const dirty = computed(() => markdown.value !== savedMarkdown.value)

async function load() {
  loading.value = true
  loadFailed.value = false
  try {
    const about = await adminApi.about()
    savedMarkdown.value = about.markdown
    markdown.value = about.markdown
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!dirty.value || saving.value) return
  saving.value = true
  try {
    const about = await adminApi.updateAbout(markdown.value)
    savedMarkdown.value = about.markdown
    markdown.value = about.markdown
    message.success('已保存，前台关于页已更新')
  } catch {
    // 请求拦截器已提示失败；保留未保存内容以便重试。
  } finally {
    saving.value = false
  }
}

onBeforeRouteLeave(() => !dirty.value || window.confirm('关于页正文尚未保存，确定离开吗？'))
onMounted(load)
</script>

<template>
  <section class="glass about-editor">
    <a-skeleton v-if="loading" active :paragraph="{ rows: 8 }" />
    <a-result v-else-if="loadFailed" status="error" title="关于页正文加载失败">
      <template #extra>
        <a-button @click="load">重试</a-button>
      </template>
    </a-result>
    <template v-else>
      <div class="editor-head">
        <div>
          <label class="editor-label" for="about-markdown">Markdown 正文</label>
          <p id="about-markdown-help" class="editor-hint">正文显示在前台关于页的站点名片下方；留空则只显示名片。</p>
        </div>
        <div class="editor-actions">
          <a-button @click="previewOpen = true">预览</a-button>
          <a-button type="primary" :disabled="!dirty" :loading="saving" @click="save">保存</a-button>
        </div>
      </div>
      <a-textarea
        id="about-markdown"
        v-model:value="markdown"
        aria-describedby="about-markdown-help"
        :maxlength="ABOUT_LIMIT"
        :disabled="saving"
        show-count
        placeholder="在这里输入 Markdown…"
      />
    </template>

    <a-modal v-model:open="previewOpen" title="关于页正文预览" :width="1000" :footer="null">
      <div v-if="previewOpen" class="preview-scroll">
        <AboutMarkdown v-if="markdown.trim()" :markdown="markdown" />
        <a-empty v-else description="正文为空，前台只会显示站点名片" />
      </div>
    </a-modal>
  </section>
</template>

<style scoped>
.about-editor {
  padding: clamp(16px, 3vw, 30px);
}

.editor-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.editor-label {
  font-size: 17px;
  font-weight: 700;
}

.editor-hint {
  margin: 4px 0 0;
  color: var(--ink-3);
}

.editor-actions {
  display: flex;
  gap: 8px;
}

.about-editor :deep(textarea) {
  min-height: clamp(330px, 60vh, 960px);
  resize: vertical;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 14px;
  line-height: 1.7;
}

.preview-scroll {
  max-height: 68vh;
  padding: 12px 0;
  overflow: auto;
}
</style>
