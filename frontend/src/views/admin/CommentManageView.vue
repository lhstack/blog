<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { adminApi } from '@/api'
import { formatDate } from '@/utils/format'
import AdminArticlePreview from '@/components/admin/AdminArticlePreview.vue'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const listBody = ref(null)
let listRequest = 0
const status = ref('ALL')
const selectedComment = ref(null)
const previewId = ref(null)
const articleId = ref(undefined)
const dates = ref(null)
const articleOptions = ref([])
const optionsLoading = ref(false)
let articleSearch
let searchTimer

function searchArticles(keyword = '') {
  clearTimeout(searchTimer)
  articleSearch?.abort()
  optionsLoading.value = true
  searchTimer = setTimeout(async () => {
    const task = new AbortController()
    articleSearch = task
    try {
      const result = await adminApi.articles({ keyword: keyword.trim() || undefined, size: 30 }, { signal: task.signal })
      if (task.signal.aborted) return
      const options = result.items.map(article => ({ value: article.id, label: article.title }))
      const selected = articleOptions.value.find(option => option.value === articleId.value)
      if (selected && !options.some(option => option.value === selected.value)) options.unshift(selected)
      articleOptions.value = options
    } catch { /* HTTP 层已提示 */ } finally {
      if (!task.signal.aborted) optionsLoading.value = false
    }
  }, 200)
}

onBeforeUnmount(() => { clearTimeout(searchTimer); articleSearch?.abort() })
const columns = [
  { title: '作者', key: 'author', dataIndex: 'author', width: 120, ellipsis: true },
  { title: '内容', key: 'content' },
  { title: '所属文章', key: 'article', width: 232 },
  { title: '状态', key: 'status', width: 94 },
  { title: '时间', key: 'time', width: 156 },
  { title: '操作', key: 'actions', width: 252 }
]

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' }
]

const statusMeta = {
  PENDING: { color: 'gold', label: '待审核' },
  APPROVED: { color: 'green', label: '已通过' },
  REJECTED: { color: 'red', label: '已拒绝' }
}

async function load() {
  const request = ++listRequest
  loading.value = true
  try {
    const result = await adminApi.comments({ status: status.value, articleId: articleId.value,
      from: dates.value?.[0], to: dates.value?.[1], page: page.value, size: pageSize })
    if (request !== listRequest) return
    const lastPage = Math.max(1, Math.ceil(result.total / pageSize))
    if (page.value > lastPage) {
      page.value = lastPage
      await load()
      return
    }
    rows.value = result.items
    total.value = result.total
    if (listBody.value) listBody.value.scrollTop = 0
  } finally {
    if (request === listRequest) loading.value = false
  }
}

async function moderate(record, action) {
  await adminApi.moderateComment(record.id, action)
  message.success(action === 'approve' ? '已通过' : '已拒绝')
  load()
}

function remove(record) {
  Modal.confirm({
    title: '确认删除这条评论？',
    content: record.content.slice(0, 80),
    okText: '删除',
    okButtonProps: { danger: true },
    cancelText: '取消',
    async onOk() {
      await adminApi.deleteComment(record.id)
      message.success('已删除')
      if (rows.value.length === 1 && page.value > 1) page.value -= 1
      load()
    }
  })
}

onMounted(load)
</script>

<template>
  <div class="comment-page admin-list-page">
    <div class="glass toolbar">
      <a-select
        v-model:value="status"
        :options="statusOptions"
        style="width: 130px"
        @change="page = 1; load()"
      />
      <a-select v-model:value="articleId" :options="articleOptions" :loading="optionsLoading" :filter-option="false"
        placeholder="搜索并选择文章" aria-label="按所属文章筛选" show-search allow-clear style="width: 215px"
        @dropdown-visible-change="open => { if (open) searchArticles() }" @search="searchArticles" @change="page = 1; load()" />
      <a-range-picker v-model:value="dates" value-format="YYYY-MM-DD" :placeholder="['评论开始日期', '评论结束日期']"
        @change="page = 1; load()" />
      <span class="muted">共 {{ total }} 条评论</span>
    </div>

    <div ref="listBody" class="glass comment-content admin-list-scroll">
      <a-table :columns="columns" :data-source="rows" :loading="loading" :pagination="false"
        row-key="id" size="middle" :scroll="{ x: 1080 }" :locale="{ emptyText: '没有符合条件的评论' }">
        <template #bodyCell="{ column, record }">
          <p v-if="column.key === 'content'" class="comment-excerpt">{{ record.content }}</p>
          <div v-else-if="column.key === 'article'" class="article-cell">
            <span :title="record.articleTitle">{{ record.articleTitle || '文章不可用' }}</span>
            <a-button v-if="record.articleTitle" type="link" size="small" @click="previewId = record.articleId">预览</a-button>
          </div>
          <a-tag v-else-if="column.key === 'status'" :color="statusMeta[record.status]?.color">
            {{ statusMeta[record.status]?.label || record.status }}
          </a-tag>
          <time v-else-if="column.key === 'time'" class="muted" :datetime="record.createdAt">
            {{ formatDate(record.createdAt, 'YYYY-MM-DD HH:mm') }}
          </time>
          <div v-else-if="column.key === 'actions'" class="actions">
            <a-button type="link" size="small" @click="selectedComment = record">查看详情</a-button>
            <a-button v-if="record.status !== 'APPROVED'" size="small" type="link" @click="moderate(record, 'approve')">通过</a-button>
            <a-button v-if="record.status !== 'REJECTED'" size="small" type="link" @click="moderate(record, 'reject')">拒绝</a-button>
            <a-button size="small" type="link" danger @click="remove(record)">删除</a-button>
          </div>
        </template>
      </a-table>
    </div>

    <div class="glass admin-list-pager" aria-label="评论分页">
      <span class="muted">共 {{ total }} 条 · 每页 {{ pageSize }} 条</span>
      <a-pagination
        v-model:current="page"
        :total="total"
        :page-size="pageSize"
        :show-size-changer="false"
        :show-less-items="true"
        @change="load"
      />
    </div>

    <a-modal :open="Boolean(selectedComment)" title="评论详情" :footer="null" @cancel="selectedComment = null">
      <div v-if="selectedComment">
        <div class="actions">
          <strong>{{ selectedComment.author }}</strong>
          <a-tag :color="statusMeta[selectedComment.status]?.color">{{ statusMeta[selectedComment.status]?.label }}</a-tag>
          <time class="muted" :datetime="selectedComment.createdAt">{{ formatDate(selectedComment.createdAt, 'YYYY-MM-DD HH:mm') }}</time>
        </div>
        <p class="comment-detail-text">{{ selectedComment.content }}</p>
        <a-button v-if="selectedComment.articleTitle" type="link" size="small" @click="previewId = selectedComment.articleId; selectedComment = null">
          预览文章：{{ selectedComment.articleTitle }}
        </a-button>
      </div>
    </a-modal>
    <AdminArticlePreview :article-id="previewId" @close="previewId = null" />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  padding: 12px 18px;
  font-size: 13px;
}

.article-cell { display: flex; align-items: center; gap: 4px; }
.article-cell span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.article-cell button { flex: none; }

.comment-content { padding: 8px; }

.comment-excerpt {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  margin: 0;
}

.comment-detail-text {
  max-height: 55vh;
  overflow: auto;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  padding: 12px 0;
}

.actions {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
}
</style>
