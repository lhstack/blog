<script setup>
import { computed } from 'vue'
import { formatDate, formatNumber } from '@/utils/format'

const props = defineProps({
  article: { type: Object, required: true },
  featured: { type: Boolean, default: false }
})

const tagColor = (tag) => tag.color || 'blue'
const readingTime = computed(() => {
  const words = (props.article.summary || '').length
  return Math.max(1, Math.round(words / 120))
})
</script>

<template>
  <RouterLink :to="`/articles/${article.id}`" class="card-link">
    <article class="article-card glass hover-lift" :class="{ 'is-featured': featured }">
      <div v-if="article.cover" class="cover">
        <img :src="article.cover" alt="" loading="lazy" />
        <span v-if="article.pinned && !featured" class="pin-badge">置顶</span>
      </div>

      <div class="body">
        <div class="meta-top">
          <span v-if="article.categoryName" class="category">{{ article.categoryName }}</span>
          <span class="date">{{ formatDate(article.publishedAt || article.createdAt, 'YYYY-MM-DD') }}</span>
        </div>

        <h3 class="title">{{ article.title }}</h3>
        <p class="summary">{{ article.summary || '—' }}</p>

        <div class="tags" v-if="article.tags?.length">
          <a-tag v-for="tag in article.tags.slice(0, 3)" :key="tag.id" :color="tagColor(tag)">
            {{ tag.name }}
          </a-tag>
        </div>

        <div class="footer">
          <span class="metric">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z" />
              <circle cx="12" cy="12" r="2.6" />
            </svg>
            {{ formatNumber(article.viewCount) }}
          </span>
          <span class="metric">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 12a8 8 0 0 1-8 8H8l-5 3 1.5-5A8 8 0 1 1 21 12Z" />
            </svg>
            {{ article.commentCount }}
          </span>
          <span class="metric">{{ readingTime }} 分钟</span>
          <span v-if="article.status === 'DRAFT'" class="draft-flag">草稿</span>
        </div>
      </div>
    </article>
  </RouterLink>
</template>

<style scoped>
.card-link {
  display: block;
  min-width: 0;
  height: 100%;
}

.article-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  cursor: pointer;
}

.article-card.is-featured {
  border-color: rgba(99, 102, 241, 0.28);
}

.cover {
  position: relative;
  aspect-ratio: 16 / 8.4;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.16), rgba(34, 211, 238, 0.16));
}

.article-card.is-featured .cover {
  flex: none;
  height: clamp(110px, 10vw, 160px);
  aspect-ratio: auto;
}

.cover img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 520ms var(--ease-out), filter 320ms linear;
}

.article-card:hover .cover img {
  transform: scale(1.06);
  filter: saturate(1.1);
}

.pin-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-3));
  box-shadow: 0 6px 16px -8px rgba(99, 102, 241, 0.9);
}

.body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 16px 18px 14px;
  gap: 8px;
}

.meta-top {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 10px;
  font-size: 11.5px;
}

.category {
  overflow-wrap: anywhere;
  padding: 2px 9px;
  border-radius: 999px;
  color: var(--brand-1);
  background: rgba(99, 102, 241, 0.1);
  font-weight: 600;
}

.date {
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

.title {
  margin: 0;
  font-size: 16.5px;
  line-height: 1.45;
  font-weight: 700;
  letter-spacing: -0.01em;
  overflow-wrap: anywhere;
  color: var(--ink-1);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color var(--dur-fast) linear;
}

.article-card:hover .title {
  color: var(--brand-1);
}

.summary {
  margin: 0;
  font-size: 13px;
  line-height: 1.68;
  overflow-wrap: anywhere;
  color: var(--ink-3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tags :deep(.ant-tag) {
  max-width: 100%;
  margin-inline-end: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 14px;
  padding-top: 10px;
  border-top: 1px dashed var(--surface-border);
  font-size: 11.5px;
  color: var(--ink-3);
}

.metric {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-variant-numeric: tabular-nums;
}

.draft-flag {
  margin-left: auto;
  padding: 1px 8px;
  border-radius: 999px;
  color: #b45309;
  background: rgba(251, 191, 36, 0.16);
}
</style>
