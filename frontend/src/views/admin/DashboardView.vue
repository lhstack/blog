<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '@/api'
import { formatNumber, fromNow } from '@/utils/format'

const router = useRouter()
const data = ref(null)
const loading = ref(true)

const cards = computed(() => {
  if (!data.value) return []
  return [
    { label: '已发布文章', value: data.value.publishedArticles, suffix: '篇', tone: 'indigo' },
    { label: '草稿', value: data.value.draftArticles, suffix: '篇', tone: 'amber' },
    { label: '累计阅读', value: data.value.totalViews, suffix: '次', tone: 'cyan' },
    { label: '待审评论', value: data.value.pendingComments, suffix: '条', tone: 'rose' }
  ]
})

const archiveBars = computed(() => {
  const list = data.value?.archives || []
  const max = Math.max(1, ...list.map((item) => item.count))
  return list.slice(0, 8).map((item) => ({
    label: `${item.year}/${item.month}`,
    count: item.count,
    height: Math.round((item.count / max) * 100)
  }))
})

const archiveHint = computed(() => {
  const count = archiveBars.value.length
  return count ? `近 ${count} 个月` : '暂无发布记录'
})

onMounted(async () => {
  try {
    data.value = await adminApi.dashboard()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="dashboard">
    <a-skeleton v-if="loading" active :paragraph="{ rows: 8 }" />

    <template v-else-if="data">
      <div class="stat-grid">
        <div v-for="card in cards" :key="card.label" class="glass stat-card" :class="`tone-${card.tone}`">
          <span class="stat-label">{{ card.label }}</span>
          <div class="stat-value">
            <strong>{{ formatNumber(card.value) }}</strong>
            <em>{{ card.suffix }}</em>
          </div>
          <span class="stat-glow" />
        </div>
      </div>

      <div class="chart-row">
        <div class="glass panel">
          <div class="panel-head">
            <h3>发布节奏</h3>
            <span class="muted">{{ archiveHint }}</span>
          </div>
          <div class="bars">
            <div v-for="bar in archiveBars" :key="bar.label" class="bar-item">
              <div class="bar-track">
                <div class="bar-fill" :style="{ height: `${bar.height}%` }" />
              </div>
              <span class="bar-count">{{ bar.count }}</span>
              <span class="bar-label">{{ bar.label }}</span>
            </div>
            <a-empty v-if="!archiveBars.length" description="暂无数据" style="width: 100%" />
          </div>
        </div>

        <div class="glass panel">
          <div class="panel-head">
            <h3>内容概况</h3>
            <a-button type="link" size="small" @click="router.push('/admin/articles/new')">写文章</a-button>
          </div>
          <ul class="overview">
            <li>
              <span>分类</span>
              <strong>{{ data.categoryCount }}</strong>
            </li>
            <li>
              <span>标签</span>
              <strong>{{ data.tagCount }}</strong>
            </li>
            <li>
              <span>已通过评论</span>
              <strong>{{ data.approvedComments }}</strong>
            </li>
            <li>
              <span>待审评论</span>
              <strong class="warn">{{ data.pendingComments }}</strong>
            </li>
          </ul>
          <a-button
            v-if="data.pendingComments > 0"
            type="primary"
            ghost
            block
            style="margin-top: 14px"
            @click="router.push('/admin/comments')"
          >
            去审核 {{ data.pendingComments }} 条评论
          </a-button>
        </div>
      </div>

      <div class="glass panel">
        <div class="panel-head">
          <h3>最近更新</h3>
          <a-button type="link" size="small" @click="router.push('/admin/articles')">全部文章 →</a-button>
        </div>
        <ul class="recent-list">
          <li v-for="article in data.recentArticles" :key="article.id">
            <span class="status-dot" :class="article.status === 'PUBLISHED' ? 'is-live' : 'is-draft'" />
            <RouterLink :to="`/admin/articles/${article.id}/edit`" class="recent-title">
              {{ article.title }}
            </RouterLink>
            <a-tag :color="article.status === 'PUBLISHED' ? 'green' : 'gold'" style="margin-inline-end: 0">
              {{ article.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </a-tag>
            <span class="recent-time">{{ fromNow(article.updatedAt) }}</span>
            <span class="recent-views">{{ formatNumber(article.viewCount) }} 阅读</span>
          </li>
          <li v-if="!data.recentArticles?.length" class="muted">还没有文章</li>
        </ul>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 14px;
}

.stat-card {
  position: relative;
  padding: 18px 20px;
  overflow: hidden;
}

.stat-label {
  font-size: 12.5px;
  color: var(--ink-3);
}

.stat-value {
  display: flex;
  align-items: baseline;
  gap: 5px;
  margin-top: 8px;
}

.stat-value strong {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.stat-value em {
  font-style: normal;
  font-size: 12px;
  color: var(--ink-3);
}

.stat-glow {
  position: absolute;
  right: -30px;
  bottom: -40px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  opacity: 0.5;
  filter: blur(18px);
  pointer-events: none;
}

.tone-indigo .stat-value strong { color: #4f46e5; }
.tone-indigo .stat-glow { background: radial-gradient(circle, rgba(99, 102, 241, 0.5), transparent 70%); }
.tone-amber .stat-value strong { color: #b45309; }
.tone-amber .stat-glow { background: radial-gradient(circle, rgba(251, 191, 36, 0.5), transparent 70%); }
.tone-cyan .stat-value strong { color: #0e7490; }
.tone-cyan .stat-glow { background: radial-gradient(circle, rgba(34, 211, 238, 0.5), transparent 70%); }
.tone-rose .stat-value strong { color: #be123c; }
.tone-rose .stat-glow { background: radial-gradient(circle, rgba(244, 114, 182, 0.45), transparent 70%); }

.chart-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 0.6fr);
  gap: 14px;
  align-items: stretch;
}

.panel {
  padding: 18px 20px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 0;
  font-size: 14.5px;
  font-weight: 700;
}

.panel-head .muted {
  font-size: 12px;
}

.bars {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 168px;
  padding-top: 8px;
}

.bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  height: 100%;
}

.bar-track {
  position: relative;
  width: 100%;
  flex: 1;
  display: flex;
  align-items: flex-end;
  border-radius: 8px 8px 4px 4px;
  background: rgba(99, 102, 241, 0.07);
  overflow: hidden;
}

.bar-fill {
  width: 100%;
  border-radius: 8px 8px 4px 4px;
  background: linear-gradient(180deg, var(--brand-2), var(--brand-1));
  transform-origin: bottom;
  animation: growBar 700ms var(--ease-out) both;
}

@keyframes growBar {
  from {
    transform: scaleY(0);
  }
  to {
    transform: scaleY(1);
  }
}

.bar-count {
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-1);
  font-variant-numeric: tabular-nums;
}

.bar-label {
  font-size: 10.5px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

.overview {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.overview li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 2px;
  font-size: 13px;
  color: var(--ink-2);
  border-bottom: 1px dashed var(--surface-border);
}

.overview li:last-child {
  border-bottom: none;
}

.overview strong {
  font-variant-numeric: tabular-nums;
}

.warn {
  color: #b45309;
}

.recent-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
}

.recent-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 2px;
  border-bottom: 1px dashed var(--surface-border);
  font-size: 13.5px;
}

.recent-list li:last-child {
  border-bottom: none;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.is-live {
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.16);
}

.status-dot.is-draft {
  background: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.16);
}

.recent-title {
  flex: 1;
  min-width: 0;
  color: var(--ink-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color var(--dur-fast) linear;
}

.recent-title:hover {
  color: var(--brand-1);
}

.recent-time,
.recent-views {
  font-size: 12px;
  color: var(--ink-3);
  flex-shrink: 0;
}

@media (max-width: 900px) {
  .chart-row {
    grid-template-columns: 1fr;
  }
  .recent-views {
    display: none;
  }
}
</style>
