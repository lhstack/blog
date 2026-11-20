<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import { adminApi } from '@/api'
import { formatDate, formatNumber } from '@/utils/format'
import AdminArticlePreview from '@/components/admin/AdminArticlePreview.vue'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const listBody = ref(null)
let listRequest = 0
const keyword = ref('')
const status = ref('ALL')
const categories = ref([])
const categoryId = ref(undefined)
const tags = ref([])
const tagId = ref(undefined)
const dates = ref(null)
const previewId = ref(null)
const tagOptions = computed(() => tags.value.map(item => ({ label: item.name, value: item.id })))

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: '分类', dataIndex: 'categoryName', key: 'categoryName', width: 110 },
  { title: '标签', key: 'tags', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '阅读', dataIndex: 'viewCount', key: 'viewCount', width: 84, align: 'right' },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 160 },
  { title: '操作', key: 'action', width: 320, align: 'right' }
]

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '草稿', value: 'DRAFT' }
]

const categoryOptions = computed(() => [
  { label: '全部分类', value: null },
  ...categories.value.map((item) => ({ label: item.name, value: item.id }))
])

async function load() {
  const request = ++listRequest
  loading.value = true
  try {
    const result = await adminApi.articles({
      keyword: keyword.value || undefined,
      status: status.value,
      categoryId: categoryId.value ?? undefined,
      tagId: tagId.value,
      from: dates.value?.[0],
      to: dates.value?.[1],
      page: page.value,
      size: pageSize
    })
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

async function onPublish(record) {
  await adminApi.publishArticle(record.id, false)
  message.success('已发布')
  load()
}

async function onWithdraw(record) {
  await adminApi.withdrawArticle(record.id)
  message.success('已撤回为草稿')
  load()
}

async function onTogglePin(record) {
  await adminApi.pinArticle(record.id, !record.pinned)
  message.success(record.pinned ? '已取消置顶' : '已置顶')
  load()
}

function onDelete(record) {
  Modal.confirm({
    title: '确认删除这篇文章？',
    content: `「${record.title}」删除后不可恢复，其评论也会一并移除。`,
    okText: '删除',
    okButtonProps: { danger: true },
    cancelText: '取消',
    async onOk() {
      await adminApi.deleteArticle(record.id)
      message.success('已删除')
      if (rows.value.length === 1 && page.value > 1) {
        page.value -= 1
      }
      load()
    }
  })
}

onMounted(async () => {
  const [categoryList, tagList] = await Promise.all([adminApi.categories().catch(() => []), adminApi.tags().catch(() => [])])
  categories.value = categoryList
  tags.value = tagList
  load()
})
</script>

<template>
  <div class="manage-page admin-list-page">
    <div class="glass toolbar">
      <div class="toolbar-left">
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索标题或摘要"
          allow-clear
          style="width: 240px"
          @search="page = 1; load()"
        />
        <a-select
          v-model:value="status"
          :options="statusOptions"
          style="width: 116px"
          @change="page = 1; load()"
        />
        <a-select
          v-model:value="categoryId"
          :options="categoryOptions"
          style="width: 136px"
          placeholder="全部分类"
          allow-clear
          @change="page = 1; load()"
        />
        <a-select v-model:value="tagId" :options="tagOptions" placeholder="全部标签" aria-label="按标签筛选"
          option-filter-prop="label" show-search allow-clear style="width: 136px" @change="page = 1; load()" />
        <a-range-picker v-model:value="dates" value-format="YYYY-MM-DD" :placeholder="['更新开始日期', '更新结束日期']"
          @change="page = 1; load()" />
      </div>
      <a-button v-magnetic type="primary" shape="round" @click="router.push('/admin/articles/new')">
        + 写文章
      </a-button>
    </div>

    <div ref="listBody" class="glass table-card admin-list-scroll">
      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        row-key="id"
        :pagination="false"
        :scroll="{ x: 1160 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <div class="title-cell">
              <span v-if="record.pinned" class="pin-tag">置顶</span>
              <a class="title-text" @click="router.push(`/admin/articles/${record.id}/edit`)">
                {{ record.title }}
              </a>
            </div>
          </template>

          <template v-else-if="column.key === 'tags'">
            <div v-if="record.tags?.length" class="tags-cell" tabindex="0" :aria-label="`标签：${record.title}`">
              <a-tag v-for="tag in record.tags" :key="tag.id" :color="tag.color" class="mini-tag">
                {{ tag.name }}
              </a-tag>
            </div>
            <span v-else class="muted">—</span>
          </template>

          <template v-else-if="column.key === 'categoryName'">
            <span class="muted">{{ record.categoryName || '未分类' }}</span>
          </template>

          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'PUBLISHED' ? 'green' : 'gold'">
              {{ record.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'viewCount'">
            <span class="mono">{{ formatNumber(record.viewCount) }}</span>
          </template>

          <template v-else-if="column.key === 'updatedAt'">
            <span class="muted mono">{{ formatDate(record.updatedAt, 'YYYY-MM-DD HH:mm') }}</span>
          </template>

          <template v-else-if="column.key === 'action'">
            <div class="row-actions">
              <a-button type="link" size="small" @click="router.push(`/admin/articles/${record.id}/edit`)">编辑</a-button>
              <a-button type="link" size="small" @click="previewId = record.id">
                预览
              </a-button>
              <a-button
                v-if="record.status === 'DRAFT'"
                type="link"
                size="small"
                @click="onPublish(record)"
              >
                发布
              </a-button>
              <a-button v-else type="link" size="small" @click="onWithdraw(record)">撤回</a-button>
              <a-button
                v-if="record.status === 'PUBLISHED'"
                type="link"
                size="small"
                @click="onTogglePin(record)"
              >
                {{ record.pinned ? '取消置顶' : '置顶' }}
              </a-button>
              <a-button type="link" size="small" danger @click="onDelete(record)">删除</a-button>
            </div>
          </template>
        </template>
      </a-table>
    </div>

    <div class="glass admin-list-pager" aria-label="文章分页">
      <span class="muted">共 {{ total }} 篇 · 每页 {{ pageSize }} 篇</span>
      <a-pagination v-model:current="page" :total="total" :page-size="pageSize"
        :show-size-changer="false" :show-less-items="true" @change="load" />
    </div>

    <AdminArticlePreview :article-id="previewId" @close="previewId = null" />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.table-card {
  padding: 8px;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 7px;
}

.pin-tag {
  flex-shrink: 0;
  padding: 1px 7px;
  border-radius: 5px;
  font-size: 10.5px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-3));
}

.title-text {
  color: var(--ink-1);
  font-weight: 550;
  transition: color var(--dur-fast) linear;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.title-text:hover {
  color: var(--brand-1);
}

.tags-cell {
  display: flex;
  gap: 4px;
  max-width: 152px;
  overflow-x: auto;
  padding-bottom: 3px;
}

.mini-tag {
  flex: none;
  margin-inline-end: 0;
  font-size: 10.5px !important;
  line-height: 16px !important;
  height: 18px !important;
}

.row-actions {
  display: flex;
  justify-content: flex-end;
  gap: 2px;
  flex-wrap: wrap;
}

.mono {
  font-variant-numeric: tabular-nums;
}
</style>
