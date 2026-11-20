<script setup>
import { computed, onMounted, ref } from 'vue'
import { publicApi } from '@/api'
import { formatMonth } from '@/utils/format'

const archives = ref([])
const articles = ref([])
const loading = ref(true)
const activeMonth = ref('')

const grouped = computed(() => {
  const map = new Map()
  for (const article of articles.value) {
    const date = article.publishedAt || article.createdAt
    const key = String(date).slice(0, 7)
    if (!map.has(key)) {
      map.set(key, { key, year: key.slice(0, 4), month: key.slice(5, 7), items: [] })
    }
    map.get(key).items.push(article)
  }
  return Array.from(map.values())
})

async function load() {
  loading.value = true
  try {
    const pageSize = 100
    const [archiveList, firstPage] = await Promise.all([
      publicApi.archives(),
      publicApi.articles({ page: 1, size: pageSize, sort: 'latest' })
    ])
    // 归档页声称“全部文章”，后端单页上限 100，因此需要翻页取全。
    const all = [...(firstPage.items || [])]
    const totalPages = Math.ceil((firstPage.total || 0) / pageSize)
    for (let page = 2; page <= totalPages; page++) {
      const next = await publicApi.articles({ page, size: pageSize, sort: 'latest' })
      all.push(...(next.items || []))
    }
    archives.value = archiveList
    articles.value = all
  } finally {
    loading.value = false
  }
}

function scrollTo(key) {
  activeMonth.value = key
  const el = document.getElementById(`archive-${key}`)
  if (el) {
    window.scrollTo({ top: el.getBoundingClientRect().top + window.scrollY - 96, behavior: 'smooth' })
  }
}

onMounted(load)
</script>

<template>
  <div class="page-shell archive-page">
    <header class="rise-in">
      <h1>归档</h1>
      <p class="muted">按时间顺序排列的全部已发布文章，共 {{ articles.length }} 篇</p>
    </header>

    <a-skeleton v-if="loading" active :paragraph="{ rows: 10 }" style="margin-top: 28px" />

    <div v-else class="archive-layout">
      <aside class="glass year-nav">
        <h3>年月</h3>
        <ul v-if="archives.length">
          <li
            v-for="item in archives"
            :key="`${item.year}-${item.month}`"
            :class="{ 'is-active': activeMonth === `${item.year}-${item.month}` }"
            @click="scrollTo(`${item.year}-${item.month}`)"
          >
            <span>{{ formatMonth(item.year, item.month) }}</span>
            <em>{{ item.count }}</em>
          </li>
        </ul>
        <p v-else class="year-empty">暂无归档</p>
      </aside>

      <div class="timeline">
        <section
          v-for="group in grouped"
          :id="`archive-${group.key}`"
          :key="group.key"
          class="glass month-block"
        >
          <h2 class="month-title">
            <span class="dot" />
            {{ formatMonth(group.year, group.month) }}
            <a-tag color="blue">{{ group.items.length }} 篇</a-tag>
          </h2>
          <ul>
            <li v-for="article in group.items" :key="article.id">
              <span class="date">{{ String(article.publishedAt || article.createdAt).slice(8, 10) }} 日</span>
              <RouterLink :to="`/articles/${article.id}`" class="title-link">{{ article.title }}</RouterLink>
              <span v-if="article.categoryName" class="cat">{{ article.categoryName }}</span>
            </li>
          </ul>
        </section>
        <a-empty v-if="!grouped.length" description="还没有已发布的文章" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.archive-page {
  padding-top: 38px;
}

.archive-page h1 {
  margin: 0 0 6px;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.archive-page p {
  margin: 0;
  font-size: 13.5px;
}

.archive-layout {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
  margin-top: 24px;
}

.year-nav {
  padding: 16px 18px;
  position: sticky;
  top: 88px;
}

.year-nav h3 {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--ink-3);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.year-nav ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.year-nav li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 10px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--ink-2);
  cursor: pointer;
  transition: background var(--dur-fast) linear, color var(--dur-fast) linear;
}

.year-nav li:hover {
  background: rgba(99, 102, 241, 0.08);
  color: var(--brand-1);
}

.year-nav li.is-active {
  background: rgba(99, 102, 241, 0.12);
  color: var(--brand-1);
  font-weight: 600;
}

.year-nav em {
  font-style: normal;
  font-size: 11.5px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

.year-empty {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-3);
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.month-block {
  padding: 20px 24px;
  scroll-margin-top: 96px;
}

.month-title {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0 0 14px;
  font-size: 16px;
  font-weight: 700;
}

.month-title .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-2));
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.14);
}

.month-block ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
}

.month-block li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 2px;
  border-bottom: 1px dashed var(--surface-border);
  font-size: 13.5px;
}

.month-block li:last-child {
  border-bottom: none;
}

.date {
  flex-shrink: 0;
  width: 42px;
  font-size: 12px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

.title-link {
  flex: 1;
  min-width: 0;
  color: var(--ink-1);
  transition: color var(--dur-fast) linear, transform var(--dur-fast) var(--ease-out);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.title-link:hover {
  color: var(--brand-1);
  transform: translateX(3px);
}

.cat {
  flex-shrink: 0;
  min-width: 0;
  max-width: 30%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11.5px;
  padding: 1px 9px;
  border-radius: 999px;
  color: var(--brand-1);
  background: rgba(99, 102, 241, 0.09);
}

@media (max-width: 900px) {
  .archive-layout {
    grid-template-columns: minmax(0, 1fr);
  }
  .year-nav {
    position: static;
  }
  .year-nav ul {
    flex-direction: row;
    flex-wrap: wrap;
  }
}

@media (max-width: 600px) {
  .archive-layout {
    gap: 16px;
  }

  .month-block {
    padding: 16px;
  }

  .month-block li {
    align-items: baseline;
    gap: 8px;
  }

  .date {
    width: auto;
    white-space: nowrap;
  }

  .title-link {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    white-space: normal;
    overflow-wrap: anywhere;
  }
}
</style>
