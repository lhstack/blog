<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { adminApi } from '@/api'
import { attachmentMarkdown } from '@/utils/attachments'
import AdminFilePicker from '@/components/admin/AdminFilePicker.vue'
import AttachmentLibrary from '@/components/admin/AttachmentLibrary.vue'
import AdminAttachmentPreview from '@/components/admin/AdminAttachmentPreview.vue'
import AdminAttachmentMarkdown from '@/components/admin/AdminAttachmentMarkdown.vue'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => Boolean(route.params.id))
const loading = ref(false)
const saving = ref(false)
const previewMode = ref('edit')
const recentUploads = ref([])
const attachmentPreview = ref(null)
const loadFailed = ref(false)
const coverPicker = ref(false)
const uploadingAttachments = ref(false)
const editorArea = ref(null)
const categories = ref([])
const tags = ref([])

const form = reactive({
  title: '',
  summary: '',
  content: '',
  cover: '',
  categoryId: undefined,
  tagIds: [],
  pinned: false
})

const snapshot = ref('')
const dirty = computed(() => JSON.stringify(form) !== snapshot.value)

async function insertAttachments(files, textarea = editorArea.value?.querySelector('textarea')) {
  if (!files.length || !textarea || uploadingAttachments.value || saving.value) return
  const original = form.content
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const references = []
  uploadingAttachments.value = true
  try {
    for (const file of files) {
      try {
        const attachment = await adminApi.uploadAttachment(file)
        recentUploads.value.push(attachment)
        references.push(attachmentMarkdown(attachment))
      }
      catch { /* 失败的文件不插入；接口已提示错误。 */ }
    }
    if (!references.length || !textarea.isConnected || form.content !== original) return
    const before = original.slice(0, start)
    const after = original.slice(end)
    const insertion = `\n\n${references.join('\n\n')}\n\n`
    form.content = before + insertion + after
    await nextTick()
    textarea.setSelectionRange(start + insertion.length, start + insertion.length)
  } finally {
    uploadingAttachments.value = false
    if (textarea.isConnected) nextTick(() => textarea.focus())
  }
}

function pasteAttachments(event) {
  const items = Array.from(event.clipboardData?.items || []).filter(item => item.kind === 'file')
  const files = items.length ? items.map(item => item.getAsFile()).filter(Boolean) : Array.from(event.clipboardData?.files || [])
  if (!files.length) return
  event.preventDefault()
  insertAttachments(files, event.target)
}

function uploadBodyFiles(files) {
  insertAttachments(files)
}

function selectCover(file) {
  form.cover = file.url
  coverPicker.value = false
}

const categoryOptions = computed(() =>
  categories.value.map((item) => ({ label: item.name, value: item.id }))
)

const tagOptions = computed(() =>
  tags.value.map((item) => ({ label: `# ${item.name}`, value: item.id }))
)

function takeSnapshot() {
  snapshot.value = JSON.stringify(form)
}

async function load() {
  loading.value = true
  loadFailed.value = false
  previewMode.value = 'edit'
  recentUploads.value = []
  try {
    const [categoryList, tagList] = await Promise.all([adminApi.categories(), adminApi.tags()])
    categories.value = categoryList
    tags.value = tagList

    if (isEdit.value) {
      const detail = await adminApi.article(route.params.id)
      Object.assign(form, {
        title: detail.title,
        summary: detail.summary || '',
        content: detail.content,
        cover: detail.cover || '',
        categoryId: detail.categoryId ?? undefined,
        tagIds: (detail.tags || []).map((tag) => tag.id),
        pinned: detail.pinned
      })
    } else {
      Object.assign(form, { title: '', summary: '', content: '', cover: '', categoryId: undefined, tagIds: [], pinned: false })
    }
    takeSnapshot()
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

async function save(publishAfter = false) {
  if (uploadingAttachments.value || saving.value) return
  if (!form.title?.trim()) {
    message.warning('标题不能为空')
    return
  }
  if (!form.content?.trim()) {
    message.warning('正文不能为空')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      summary: form.summary?.trim() || null,
      content: form.content,
      cover: form.cover?.trim() || null,
      categoryId: form.categoryId ?? null,
      tagIds: form.tagIds,
      pinned: form.pinned
    }
    let saved
    if (isEdit.value) {
      saved = await adminApi.updateArticle(route.params.id, payload)
    } else {
      saved = await adminApi.createArticle(payload)
    }

    if (publishAfter) {
      await adminApi.publishArticle(saved.id, form.pinned)
      message.success('已保存并发布')
    } else {
      message.success('已保存')
    }
    takeSnapshot()
    router.push('/admin/articles')
  } finally {
    saving.value = false
  }
}

onBeforeRouteLeave(() => {
  if (uploadingAttachments.value) return false
  if (!dirty.value) return true
  const leave = window.confirm('当前内容尚未保存，确定离开吗？')
  return leave
})

watch(() => route.params.id, () => {
  load()
})

onMounted(load)
</script>

<template>
  <div class="edit-page">
    <a-skeleton v-if="loading" active :paragraph="{ rows: 12 }" />

    <a-result v-else-if="loadFailed" status="error" title="文章编辑区加载失败">
      <template #extra><a-button @click="load">重试</a-button></template>
    </a-result>
    <template v-else>
      <div class="edit-grid">
        <div class="glass editor-card">
          <div class="editor-head">
            <a-input
              v-model:value="form.title"
              class="title-input"
              placeholder="输入文章标题…"
              :maxlength="120"
              size="large"
              bordered
            />
            <div class="head-actions">
              <a-segmented
                v-model:value="previewMode"
                :options="[{ label: '编辑', value: 'edit' }, { label: '预览', value: 'preview' }]"
                :disabled="uploadingAttachments"
                size="small"
              />
            </div>
          </div>

          <a-textarea
            v-model:value="form.summary"
            placeholder="一句话摘要（可选，展示在列表卡片）"
            :rows="2"
            :maxlength="500"
            show-count
            class="summary-input"
          />

          <div v-if="previewMode === 'edit'" ref="editorArea" class="editor-area">
            <AdminFilePicker class="body-file-picker" label="选择文件" hint="也可直接粘贴文件" multiple
              :busy="uploadingAttachments" :disabled="saving" @select="uploadBodyFiles" />
            <ul v-if="recentUploads.length" class="recent-uploads" aria-label="本次上传的附件">
              <li v-for="file in recentUploads" :key="file.id">
                <span>{{ file.name }}</span>
                <a-button v-if="file.kind !== 'download'" type="link" size="small" @click="attachmentPreview = file">预览附件</a-button>
              </li>
            </ul>
            <a-textarea
              v-model:value="form.content"
              :readonly="uploadingAttachments"
              placeholder="支持 Markdown：## 标题、**加粗**、```代码块```、> 引用…"
              :rows="22"
              class="content-input"
              @paste="pasteAttachments"
            />
          </div>
          <AdminAttachmentMarkdown v-else class="preview-area" :content="form.content" />

          <div class="editor-foot">
            <span class="muted">{{ form.content.length }} 字符</span>
            <div class="foot-actions">
              <a-button @click="router.push('/admin/articles')">取消</a-button>
              <a-button v-magnetic :disabled="uploadingAttachments" :loading="saving" @click="save(false)">保存</a-button>
              <a-button v-magnetic type="primary" :disabled="uploadingAttachments" :loading="saving" @click="save(true)">
                保存并发布
              </a-button>
            </div>
          </div>
        </div>

        <aside class="glass side-card">
          <h3>文章设置</h3>

          <a-form layout="vertical">
            <a-form-item label="分类">
              <a-select
                v-model:value="form.categoryId"
                :options="categoryOptions"
                placeholder="选择分类"
                allow-clear
              />
            </a-form-item>

            <a-form-item label="标签">
              <a-select
                v-model:value="form.tagIds"
                mode="multiple"
                :options="tagOptions"
                placeholder="选择或搜索标签"
                :filter-option="(input, option) => option.label.toLowerCase().includes(input.toLowerCase())"
              />
            </a-form-item>

            <a-form-item label="封面图">
              <a-space direction="vertical" style="width: 100%">
                <a-button @click="coverPicker = true">上传或选择附件</a-button>
                <a-input v-model:value="form.cover" placeholder="也可保留已有封面 URL" allow-clear />
              </a-space>
            </a-form-item>

            <a-form-item>
              <a-checkbox v-model:checked="form.pinned">置顶这篇文章（发布后生效）</a-checkbox>
            </a-form-item>
          </a-form>

          <div v-if="form.cover" class="cover-preview">
            <AdminAttachmentPreview :url="form.cover" name="封面预览" />
          </div>

          <div class="tips">
            <strong>写作提示</strong>
            <ul>
              <li>标题不超过 120 字符</li>
              <li>正文支持 Markdown 与代码高亮</li>
              <li>草稿不会出现在前台</li>
            </ul>
          </div>
        </aside>
      </div>
    </template>
    <a-modal :open="!!attachmentPreview" :title="attachmentPreview?.name" :footer="null" @cancel="attachmentPreview = null">
      <AdminAttachmentPreview v-if="attachmentPreview" :url="attachmentPreview.url" :name="attachmentPreview.name" />
    </a-modal>
    <a-modal v-model:open="coverPicker" title="选择封面图片" :width="760" :footer="null">
      <AttachmentLibrary v-if="coverPicker" picker @select="selectCover" />
    </a-modal>
  </div>
</template>

<style scoped>
.body-file-picker { margin-bottom: 10px; }
.recent-uploads { padding-inline-start: 20px; overflow-wrap: anywhere; }
.edit-page {
  min-height: 60vh;
}

.edit-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 14px;
  align-items: start;
}

.editor-card {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.editor-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-input {
  font-size: 17px;
  font-weight: 650;
}

.head-actions {
  flex-shrink: 0;
}

.summary-input {
  font-size: 13px;
}

.editor-area .content-input {
  font-family: 'JetBrains Mono', Consolas, Monaco, monospace;
  font-size: 13.5px;
  line-height: 1.8;
  border-radius: 12px;
}

.preview-area {
  min-height: 520px;
  max-height: 70vh;
  overflow-y: auto;
  padding: 20px 22px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid var(--surface-border);
}

.editor-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--surface-border);
  font-size: 12.5px;
}

.foot-actions {
  display: flex;
  gap: 8px;
}

.side-card {
  padding: 18px 20px;
  position: sticky;
  top: 92px;
}

.side-card h3 {
  margin: 0 0 16px;
  font-size: 14.5px;
  font-weight: 700;
}

.cover-preview {
  margin-top: 4px;
  border-radius: 10px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  background: rgba(148, 163, 184, 0.12);
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.tips {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed var(--surface-border);
  font-size: 12.5px;
  color: var(--ink-3);
}

.tips strong {
  display: block;
  margin-bottom: 8px;
  color: var(--ink-2);
}

.tips ul {
  margin: 0;
  padding-left: 1.1em;
  display: flex;
  flex-direction: column;
  gap: 4px;
  line-height: 1.6;
}

@media (max-width: 1000px) {
  .edit-grid {
    grid-template-columns: 1fr;
  }
  .side-card {
    position: static;
  }
}
</style>
