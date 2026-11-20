<script setup>
import { onMounted, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { adminApi } from '@/api'
import { attachmentMarkdown, copyText, fileSize } from '@/utils/attachments'
import { downloadPrivate } from '@/utils/privateAttachments'
import AdminFilePicker from './AdminFilePicker.vue'
import AdminAttachmentPreview from './AdminAttachmentPreview.vue'

const props = defineProps({ picker: Boolean })
const emit = defineEmits(['select'])
const items = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const failed = ref(false)
const uploading = ref(false)
const editing = ref(null)
const name = ref('')
const renaming = ref(false)
const preview = ref(null)
const dates = ref(null)
const kind = ref(undefined)
const keyword = ref('')
const appliedKeyword = ref('')
const kinds = [
  { label: '图片', value: 'image' }, { label: '音频', value: 'audio' },
  { label: '视频', value: 'video' }, { label: '其他文件', value: 'download' }
]
let listRequest = 0

async function load(next = page.value) {
  const request = ++listRequest
  loading.value = true
  failed.value = false
  try {
    const result = await adminApi.attachments({ page: next, kind: kind.value, keyword: appliedKeyword.value || undefined,
      from: dates.value?.[0], to: dates.value?.[1] })
    if (request !== listRequest) return
    const last = Math.max(1, Math.ceil(result.total / 20))
    if (next > last) return await load(last)
    page.value = next
    total.value = result.total
    items.value = result.items
  } catch {
    if (request === listRequest) failed.value = true
  } finally {
    if (request === listRequest) loading.value = false
  }
}

async function upload([file]) {
  if (!file) return
  uploading.value = true
  try {
    const entry = await adminApi.uploadAttachment(file)
    if (props.picker && entry.kind === 'image') emit('select', entry)
    else if (props.picker) message.warning('文件已上传，但只有图片可以作为封面')
    else message.success('附件已上传')
    await load(1)
  } catch { /* API 已提示 */ } finally { uploading.value = false }
}

function rename(file) {
  editing.value = file
  name.value = file.name
}

async function saveName() {
  if (!name.value.trim()) return
  renaming.value = true
  try {
    await adminApi.renameAttachment(editing.value.id, name.value.trim())
    editing.value = null
    message.success('名称已更新')
    await load()
  } catch { /* 保留表单以便重试 */ } finally { renaming.value = false }
}

function remove(file) {
  Modal.confirm({
    title: '删除附件？', content: file.name, okText: '删除', cancelText: '取消', okType: 'danger',
    async onOk() {
      try {
        await adminApi.deleteAttachment(file.id)
        message.success('附件已删除')
        await load(items.value.length === 1 && page.value > 1 ? page.value - 1 : page.value)
      } catch { /* API 已提示 */ }
    }
  })
}

async function copy(file, markdown) {
  try {
    await copyText(markdown ? attachmentMarkdown(file) : new URL(file.url, window.location.origin).href)
    message.success(markdown ? '已复制 Markdown' : '已复制原始链接')
  } catch { message.error('复制失败，请检查剪贴板权限') }
}

async function download(file) {
  try { await downloadPrivate(file.url, file.name) } catch { /* API 已提示 */ }
}

onMounted(() => load())
</script>

<template>
  <section class="attachment-library" aria-label="附件库">
    <div class="library-header">
      <p class="muted">{{ picker ? '选择图片作为文章封面。' : '附件在被已发布文章引用后才对读者开放。' }}</p>
      <AdminFilePicker :label="picker ? '选择图片' : '上传附件'" hint="单个文件不超过 20 MB"
        :accept="picker ? 'image/*' : undefined" :busy="uploading" @select="upload" />
    </div>
    <div v-if="!picker" class="library-filters">
      <a-input-search v-model:value="keyword" placeholder="搜索文件名" aria-label="搜索文件名" allow-clear
        style="width: 220px" @search="value => { appliedKeyword = value.trim(); load(1) }" />
      <a-select v-model:value="kind" :options="kinds" placeholder="全部文件类型" aria-label="按附件类型筛选"
        allow-clear style="width: 148px" @change="load(1)" />
      <a-range-picker v-model:value="dates" value-format="YYYY-MM-DD" :placeholder="['上传开始日期', '上传结束日期']"
        @change="load(1)" />
    </div>
    <a-alert v-if="failed" type="error" message="附件列表加载失败" show-icon>
      <template #action><a-button size="small" @click="load()">重试</a-button></template>
    </a-alert>
    <a-spin :spinning="loading">
      <a-empty v-if="!items.length" description="没有符合条件的附件" />
      <ul v-else class="attachment-rows">
        <li v-for="file in items" :key="file.id">
          <span class="file-info"><strong>{{ file.name }}</strong> · {{ fileSize(file.size) }}</span>
          <div class="file-actions">
            <a-button v-if="file.kind !== 'download'" type="link" size="small" @click="preview = file">预览</a-button>
            <a-button v-else-if="!picker" type="link" size="small" @click="download(file)">下载</a-button>
            <a-button v-if="picker" size="small" type="link" :disabled="file.kind !== 'image'" @click="emit('select', file)">用作封面</a-button>
            <template v-else>
              <a-button type="link" size="small" @click="copy(file, false)">复制链接</a-button>
              <a-button type="link" size="small" @click="copy(file, true)">复制 Markdown</a-button>
              <a-button type="link" size="small" @click="rename(file)">改名</a-button>
              <a-button type="link" size="small" danger @click="remove(file)">删除</a-button>
            </template>
          </div>
        </li>
      </ul>
    </a-spin>
    <a-pagination v-if="total > 20" :current="page" :total="total" :page-size="20" :disabled="loading" :show-size-changer="false" @change="load" />
    <a-modal :open="!!editing" title="修改附件名称" :confirm-loading="renaming" ok-text="保存" cancel-text="取消" @ok="saveName" @cancel="editing = null">
      <a-input v-model:value="name" aria-label="附件名称" :maxlength="255" />
    </a-modal>
    <a-modal :open="!!preview" :title="preview?.name" :footer="null" @cancel="preview = null">
      <AdminAttachmentPreview v-if="preview" :url="preview.url" :name="preview.name" />
    </a-modal>
  </section>
</template>

<style scoped>
.attachment-library { display: grid; gap: 16px; min-width: 0; }
.library-header, .attachment-rows li { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; }
.library-filters { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.library-header p { margin: 0; }
.attachment-rows { margin: 0; padding: 0; list-style: none; }
.attachment-rows li { padding: 12px 0; border-bottom: 1px solid var(--surface-border); }
.file-info { overflow-wrap: anywhere; }
.file-actions { display: flex; flex-wrap: wrap; gap: 6px; }
</style>
