<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import AdminFilePicker from './AdminFilePicker.vue'
import { adminApi } from '@/api'

const saved = ref(null)
const url = ref('')
const file = ref(null)
const objectUrl = ref('')
const loading = ref(true)
const saving = ref(false)
const previewFailed = ref(false)
const preview = computed(() => objectUrl.value || validWebUrl(url.value) || saved.value?.avatar)

function validWebUrl(value) {
  try {
    const parsed = new URL(value.trim())
    if (['https:', 'http:'].includes(parsed.protocol) && !parsed.username && !parsed.password) return parsed.href
  } catch {
    // 文案仍可继续编辑；保存时再提示格式错误。
  }
  return ''
}

function clearFile() {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
  objectUrl.value = ''
  file.value = null
}

function apply(result) {
  clearFile()
  saved.value = result
  url.value = validWebUrl(result.location) ? result.location : ''
  previewFailed.value = false
}

async function load() {
  loading.value = true
  try {
    apply(await adminApi.siteAvatar())
  } catch {
    // 错误由 HTTP 层提示，可重新载入。
  } finally {
    loading.value = false
  }
}

function selectFile([next]) {
  if (!next) return
  if (!next.size || next.size > 5 * 1024 * 1024 || !['image/jpeg', 'image/png', 'image/webp'].includes(next.type)) {
    message.warning('请选择不超过 5 MB 的 JPEG、PNG 或 WebP 图片')
    return
  }
  clearFile()
  file.value = next
  objectUrl.value = URL.createObjectURL(next)
  previewFailed.value = false
}

async function save(restore = false) {
  if (saving.value) return
  if (!restore && !file.value && (!validWebUrl(url.value) || previewFailed.value)) {
    message.warning('请输入可预览的 HTTP(S) 图片 URL，或选择图片文件')
    return
  }
  if (!restore && file.value && previewFailed.value) {
    message.warning('图片无法预览，请重新选择')
    return
  }
  saving.value = true
  try {
    const result = !restore && file.value
      ? await adminApi.uploadSiteAvatar(file.value)
      : await adminApi.updateSiteAvatar(restore ? '' : url.value.trim())
    apply(result)
    message.success(restore ? '已恢复默认头像' : '站点头像已保存')
  } catch {
    // 保持上次保存的头像，保留当前选择供重试。
  } finally {
    saving.value = false
  }
}

onMounted(load)
onBeforeUnmount(clearFile)
</script>

<template>
  <section class="glass settings-panel">
    <h3>站点头像</h3>
    <p class="settings-hint">用于前台「关于」页，不影响管理员账号头像。</p>
    <a-spin :spinning="loading">
      <template v-if="saved">
        <div class="edit-avatar">
          <a-avatar :size="64" :src="previewFailed ? undefined : preview" alt="头像预览"
            :load-error="() => { previewFailed = true; return true }">云</a-avatar>
          <div class="fields">
            <a-input v-model:value="url" aria-label="站点头像 URL" placeholder="https://…" :maxlength="2048"
              :disabled="saving" @change="clearFile(); previewFailed = false" />
            <AdminFilePicker label="选择图片" accept="image/jpeg,image/png,image/webp" :disabled="saving"
              :hint="file?.name || 'JPEG、PNG、WebP，最多 5 MB'" @select="selectFile" />
            <p v-if="previewFailed" class="settings-hint" role="status">此图片暂时无法预览。</p>
          </div>
        </div>
        <div class="settings-actions">
          <a-button type="primary" shape="round" :loading="saving" @click="save()">保存头像</a-button>
          <a-button :disabled="saving" @click="save(true)">恢复默认头像</a-button>
        </div>
      </template>
      <div v-else-if="!loading" class="settings-actions">
        <a-button @click="load">重新载入</a-button>
      </div>
    </a-spin>
  </section>
</template>

<style scoped>
.edit-avatar { display: flex; gap: 14px; align-items: flex-start; }
.edit-avatar > :first-child { flex: none; }
.fields { display: grid; gap: 8px; flex: 1; min-width: 0; }
</style>
