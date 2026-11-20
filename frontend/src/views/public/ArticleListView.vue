<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { publicApi } from '@/api'
import ArticleCard from '@/components/public/ArticleCard.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const listBody = ref(null)
const page = ref(null)
const categories = ref([])
const tags = ref([])

const keyword = ref(route.query.keyword || '')
const categoryId = ref(route.query.categoryId ? Number(route.query.categoryId) : undefined)
const tagId = ref(route.query.tagId ? Number(route.query.tagId) : undefined)
const sort = ref(route.query.sort || 'latest')
const current = ref(Number(route.query.page || 1))

const sortOptions = [
  { value: 'latest', label: '最新发布' },
  { value: 'oldest', label: '最早发布' },
  { value: 'views', label: '最多阅读' },
  { value: 'title', label: '标题排序' }
]

const activeCategory = computed(() =>
  categories.value.find((item) => item.id === categoryId.value)
)
const activeTag = computed(() => tags.value.find((item) => item.id === tagId.value))

async function load() {
  loading.value = true
  try {
    page.value = await publicApi.articles({
      keyword: keyword.value || undefined,
      categoryId: categoryId.value,
      tagId: tagId.value,
      sort: sort.value,
      page: current.value,
      size: 9
    })
    if (listBody.value) listBody.value.scrollTop = 0
  } finally {
    loading.value = false
  }
}

function syncQuery() {
  router.replace({
    query: {
      ...(keyword.value ? { keyword: keyword.value } : {}),
      ...(categoryId.value ? { categoryId: categoryId.value } : {}),
      ...(tagId.value ? { tagId: tagId.value } : {}),
      ...(sort.value !== 'latest' ? { sort: sort.value } : {}),
      ...(current.value > 1 ? { page: current.value } : {})
    }
  })
}

function pickCategory(id) {
  categoryId.value = categoryId.value === id ? undefined : id
}

function clearFilters() {
  const filtersChanged = categoryId.value !== undefined || tagId.value !== undefined || sort.value !== 'latest'
  keyword.value = ''
  categoryId.value = undefined
  tagId.value = undefined
  sort.value = 'latest'
  if (filtersChanged) {
    // 筛选变化会触发 watch，由它统一回到第 1 页并重载
    if (current.value !== 1) current.value = 1
    return
  }
  current.value = 1
  refresh()
}

function refresh() {
  syncQuery()
  load()
}

// 筛选条件变化时回到第 1 页。页码真的变了就交给页码 watcher，
// 否则（本来就在第 1 页）直接重载——避免两个 watcher 各发一次相同请求。
watch([categoryId, tagId, sort], () => {
  if (current.value !== 1) {
    current.value = 1
    return
  }
  refresh()
})

watch(current, refresh)

// 关键词只在提交搜索时生效：若进入 watch，每敲一个字都会发一次请求。
function onSearch() {
  if (current.value !== 1) {
    current.value = 1
    return
  }
  refresh()
}

onMounted(async () => {
  const [categoryList, tagList] = await Promise.all([
    publicApi.categories().catch(() => []),
    publicApi.tags().catch(() => [])
  ])
  categories.value = categoryList
  tags.value = tagList
  await load()
})
</script>

<template>
  <div class="page-shell list-page">
    <header class="list-head rise-in">
      <div>
        <h1>文章库</h1>
        <p class="muted">
          <template v-if="activeCategory">分类「{{ activeCategory.name }}」</template>
          <template v-else-if="activeTag">标签「#{{ activeTag.name }}」</template>
          <template v-else>共 {{ page?.total ?? 0 }} 篇文章，按主题归档整理</template>
        </p>
      </div>
      <div class="search-row">
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索标题或摘要…"
          allow-clear
          style="width: 260px"
          @search="onSearch"
        />
        <a-select v-model:value="sort" style="width: 132px" :options="sortOptions" />
      </div>
    </header>

    <div class="filter-bar glass">
      <div class="filter-group">
        <span class="filter-label">分类</span>
        <a-tag
          class="filter-chip"
          :class="{ 'is-active': !categoryId }"
          @click="categoryId = undefined"
        >
          全部
        </a-tag>
        <a-tag
          v-for="category in categories"
          :key="category.id"
          class="filter-chip"
          :class="{ 'is-active': categoryId === category.id }"
          @click="pickCategory(category.id)"
        >
          {{ category.name }} <em>{{ category.articleCount }}</em>
        </a-tag>
      </div>
      <div class="filter-group" v-if="tags.length">
        <span class="filter-label">标签</span>
        <a-tag
          v-for="tag in tags"
          :key="tag.id"
          :color="tagId === tag.id ? tag.color : undefined"
          class="filter-chip"
          :class="{ 'is-active': tagId === tag.id }"
          @click="tagId = tagId === tag.id ? undefined : tag.id"
        >
          # {{ tag.name }}
        </a-tag>
      </div>
      <a-button
        v-if="categoryId || tagId || keyword || sort !== 'latest'"
        type="link"
        size="small"
        @click="clearFilters"
      >
        清空筛选
      </a-button>
    </div>

    <div ref="listBody" class="library-results" tabindex="0" aria-label="文章列表" :aria-busy="loading">
      <a-skeleton v-if="loading" active :paragraph="{ rows: 10 }" />
      <div v-else-if="page?.items?.length" class="grid stagger">
        <ArticleCard v-for="item in page.items" :key="item.id" :article="item" />
      </div>
      <a-empty v-else description="没有找到匹配的文章" style="margin-top: 64px" />
    </div>

    <div class="pager" aria-label="文章分页">
      <a-pagination
        v-model:current="current"
        :total="page?.total ?? 0"
        :page-size="9"
        :disabled="loading"
        :show-size-changer="false"
        :show-less-items="true"
        :show-total="(total) => `共 ${total} 篇`"
      />
    </div>
  </div>
</template>

<style scoped>
.list-page {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding-top: 26px;
}

.library-results {
  flex: 1 1 auto;
  min-height: 80px;
  overflow: auto;
  overscroll-behavior: contain;
  padding: 16px 3px 8px;
}

.list-head, .filter-bar, .pager { flex: none; }

.list-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.list-head h1 {
  margin: 0 0 6px;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.list-head p {
  margin: 0;
  font-size: 13.5px;
}

.search-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  max-width: 100%;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 18px;
  padding: 14px 18px;
  margin-top: 22px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink-3);
}

.filter-chip {
  cursor: pointer;
  transition: transform var(--dur-fast) var(--ease-out), background var(--dur-fast) linear;
  background: rgba(148, 163, 184, 0.12);
  border-color: transparent;
}

.filter-chip:hover {
  transform: translateY(-2px);
}

.filter-chip.is-active {
  color: #fff;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-2));
  border-color: transparent;
}

.filter-chip em {
  font-style: normal;
  opacity: 0.65;
  margin-left: 3px;
  font-variant-numeric: tabular-nums;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(300px, 100%), 1fr));
  gap: 20px;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 10px 0;
  border-top: 1px solid var(--surface-border);
}

@media (max-height: 600px), (max-width: 650px) {
  .list-page { overflow-y: auto; padding-top: 12px; }
  .library-results { flex-basis: 140px; }
  .filter-bar { max-height: 18dvh; overflow: auto; }
  .pager { position: sticky; bottom: 0; z-index: 2; background: #f8fafc; }
}
</style>
