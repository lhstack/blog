<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { publicApi } from '@/api'
import { formatDate, formatNumber } from '@/utils/format'
import ArticleCard from '@/components/public/ArticleCard.vue'
import PinnedCarousel from '@/components/public/PinnedCarousel.vue'
import TypewriterText from '@/components/public/TypewriterText.vue'

const router = useRouter()
const home = ref(null)
const loading = ref(true)
const liveStats = ref(null)
const typewriterLines = ref([])

const site = computed(() => home.value?.site)
const heroChars = computed(() => Array.from(site.value?.name || '云间小筑'))
const heroTitleRef = ref(null)

/**
 * 逐字渐显动画要求每个字符 span 各自裁切整行渐变，
 * 因此把整行总宽度与每个字符的横向偏移以 CSS 变量下发。
 */
function syncCharOffsets() {
  const title = heroTitleRef.value
  if (!title) return
  const spans = title.querySelectorAll('span')
  if (!spans.length) return
  const titleBox = title.getBoundingClientRect()
  title.style.setProperty('--title-w', `${titleBox.width}px`)
  spans.forEach((span) => {
    span.style.setProperty('--cx', `${span.getBoundingClientRect().left - titleBox.left}px`)
  })
}

const yearSpan = computed(() => {
  const start = site.value?.startYear
  const now = new Date().getFullYear()
  return start ? `${start} — ${now}` : String(now)
})

let eventSource = null
let typewriterTimer
let typewriterRequest
const request = new AbortController()

// 从服务器获取当前生效的文案，不依赖访客设备的时区。
async function refreshTypewriter() {
  clearTimeout(typewriterTimer)
  typewriterRequest?.abort()
  if (document.hidden || request.signal.aborted) return
  const task = new AbortController()
  typewriterRequest = task
  let delay = 60_000
  try {
    const { texts, refreshAfterMs } = await publicApi.typewriter({ signal: task.signal, silent: true })
    if (task.signal.aborted) return
    if (texts.length !== typewriterLines.value.length || texts.some((line, i) => line !== typewriterLines.value[i])) {
      typewriterLines.value = texts
    }
    delay = Math.max(250, refreshAfterMs)
  } catch {
    if (!task.signal.aborted) typewriterLines.value = []
  } finally {
    if (!task.signal.aborted) typewriterTimer = setTimeout(refreshTypewriter, delay)
  }
}

onMounted(async () => {
  window.addEventListener('resize', syncCharOffsets, { passive: true })
  document.addEventListener('visibilitychange', syncStream)
  document.addEventListener('visibilitychange', refreshTypewriter)
  syncStream()
  refreshTypewriter()
  syncCharOffsets()
  try {
    home.value = await publicApi.home({ signal: request.signal })
  } catch {
    // 请求错误由 API 拦截器提示。
  } finally {
    if (!request.signal.aborted) loading.value = false
  }
  await nextTick()
  if (!request.signal.aborted) syncCharOffsets()
})

function syncStream() {
  if (document.hidden) disconnectStream()
  else connectStream()
}

/**
 * SSE 实时统计：浏览器原生 EventSource 自带断线重连。
 * 组件卸载时必须关闭，否则会留下悬挂连接。
 */
function connectStream() {
  if (eventSource || typeof EventSource === 'undefined') return
  eventSource = new EventSource('/api/public/site/stats/stream')
  eventSource.onmessage = (event) => {
    try {
      liveStats.value = JSON.parse(event.data)
    } catch {
      // 单条消息解析失败不影响后续推送
    }
  }
  eventSource.onerror = () => {
    // 交给浏览器自动重连，这里只清理状态展示
    liveStats.value = null
  }
}

function disconnectStream() {
  eventSource?.close()
  eventSource = null
}

onBeforeUnmount(() => {
  request.abort()
  clearTimeout(typewriterTimer)
  typewriterRequest?.abort()
  disconnectStream()
  document.removeEventListener('visibilitychange', syncStream)
  document.removeEventListener('visibilitychange', refreshTypewriter)
  window.removeEventListener('resize', syncCharOffsets)
})
</script>

<template>
  <div class="home">
    <!-- Hero -->
    <section class="hero page-shell">
      <div class="hero-inner glass rise-in">
        <div class="hero-copy">
          <span class="hero-badge">
            <span class="pulse" />
            {{ yearSpan }} · 持续写作中
          </span>
          <h1 ref="heroTitleRef" class="hero-title char-reveal">
            <span v-for="(char, index) in heroChars" :key="index" :style="{ '--i': index }">{{ char }}</span>
          </h1>
          <p class="hero-sub">
            <TypewriterText v-if="typewriterLines.length" :texts="typewriterLines" />
            <template v-else>{{ site?.description || '记录工程实践与生活碎片。' }}</template>
          </p>
          <div class="hero-actions">
            <a-button type="primary" size="large" shape="round" @click="router.push('/articles')">
              开始阅读
            </a-button>
            <a-button v-magnetic size="large" shape="round" @click="router.push('/about')">
              关于我
            </a-button>
          </div>
          <div class="hero-stats">
            <div class="stat">
              <strong>{{ formatNumber(site?.articleCount ?? 0) }}</strong>
              <span>篇文章</span>
            </div>
            <div class="divider" />
            <div class="stat">
              <strong>{{ formatNumber(site?.viewCount ?? 0) }}</strong>
              <span>次阅读</span>
            </div>
            <div class="divider" />
            <div class="stat">
              <strong>{{ formatNumber(site?.commentCount ?? 0) }}</strong>
              <span>条留言</span>
            </div>
            <div class="divider" />
            <div class="stat">
              <strong>{{ site?.tagCount ?? 0 }}</strong>
              <span>个标签</span>
            </div>
          </div>
        </div>

        <div class="hero-side">
          <div class="live-panel">
            <div class="live-head">
              <span class="live-dot" :class="{ 'is-live': Boolean(liveStats) }" />
              <span>{{ liveStats ? '实时统计已连接' : '正在连接实时统计…' }}</span>
            </div>
            <dl class="live-grid">
              <div>
                <dt>文章</dt>
                <dd>{{ liveStats?.articles ?? site?.articleCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>阅读</dt>
                <dd>{{ formatNumber(liveStats?.views ?? site?.viewCount ?? 0) }}</dd>
              </div>
              <div>
                <dt>留言</dt>
                <dd>{{ liveStats?.comments ?? site?.commentCount ?? 0 }}</dd>
              </div>
            </dl>
          </div>
        </div>
      </div>
    </section>

    <a-skeleton v-if="loading" active class="page-shell" :paragraph="{ rows: 8 }" />

    <template v-else-if="home">
      <!-- 置顶 -->
      <section v-if="home.pinned?.length" class="page-shell block">
        <PinnedCarousel :articles="home.pinned">
          <template #heading><h2 class="section-title">置顶推荐</h2></template>
        </PinnedCarousel>
      </section>

      <div class="page-shell content-split">
        <!-- 最新文章 -->
        <section class="block latest-block">
          <h2 class="section-title">最新文章</h2>
          <div v-if="home.latest?.length" class="article-grid stagger">
            <ArticleCard v-for="item in home.latest" :key="item.id" :article="item" />
          </div>
          <a-empty v-else class="empty-block" description="还没有发布任何文章">
            <template #image>
              <span class="empty-mark">✍️</span>
            </template>
            <a-button type="primary" shape="round" @click="router.push('/admin/articles/new')">
              写第一篇文章
            </a-button>
          </a-empty>
          <div v-if="home.latest?.length" class="more-row">
            <a-button type="link" @click="router.push('/articles')">查看全部文章 →</a-button>
          </div>
        </section>

        <!-- 侧栏 -->
        <aside class="side-col">
          <div class="glass side-card">
            <h3>热门阅读</h3>
            <ol v-if="home.popular?.length" class="popular-list">
              <li v-for="(item, index) in home.popular" :key="item.id">
                <span class="rank" :class="{ 'is-top': index < 3 }">{{ index + 1 }}</span>
                <RouterLink :to="`/articles/${item.id}`" class="popular-title">{{ item.title }}</RouterLink>
                <span class="popular-views">{{ formatNumber(item.viewCount) }}</span>
              </li>
            </ol>
            <p v-else class="side-empty">暂无阅读记录</p>
          </div>

          <div class="glass side-card">
            <h3>分类</h3>
            <div v-if="home.categories?.length" class="chip-wrap">
              <a-tag
                v-for="category in home.categories"
                :key="category.id"
                class="clickable"
                @click="router.push({ path: '/articles', query: { categoryId: category.id } })"
              >
                {{ category.name }}
                <span class="chip-count">{{ category.articleCount }}</span>
              </a-tag>
            </div>
            <p v-else class="side-empty">还没有分类</p>
          </div>

          <div class="glass side-card">
            <h3>标签云</h3>
            <div v-if="home.tags?.length" class="chip-wrap">
              <a-tag
                v-for="tag in home.tags"
                :key="tag.id"
                :color="tag.color"
                class="clickable"
                @click="router.push({ path: '/articles', query: { tagId: tag.id } })"
              >
                # {{ tag.name }}
              </a-tag>
            </div>
            <p v-else class="side-empty">还没有标签</p>
          </div>

          <div class="glass side-card">
            <h3>最新留言</h3>
            <ul class="comment-list">
              <li v-for="comment in home.recentComments" :key="comment.id">
                <div class="comment-meta">
                  <strong>{{ comment.author }}</strong>
                  <span>{{ formatDate(comment.createdAt, 'MM-DD') }}</span>
                </div>
                <p>{{ comment.content }}</p>
              </li>
              <li v-if="!home.recentComments?.length" class="muted">还没有留言</li>
            </ul>
          </div>
        </aside>
      </div>

      <!-- 归档时间轴 -->
      <section v-if="home.archives?.length" class="page-shell block">
        <h2 class="section-title">时间轴</h2>
        <div class="glass archive-strip">
          <div v-for="item in home.archives.slice(0, 8)" :key="`${item.year}-${item.month}`" class="archive-pill">
            <strong>{{ item.year }}</strong>
            <span>/ {{ item.month }}</span>
            <em>{{ item.count }} 篇</em>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.home {
  position: relative;
}

.hero {
  padding-top: 42px;
  padding-bottom: 10px;
}

.hero-inner {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 0.65fr);
  gap: 28px;
  padding: 40px 40px 34px;
  overflow: hidden;
  position: relative;
}

.hero-inner::before {
  content: '';
  position: absolute;
  right: -80px;
  top: -120px;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.22), transparent 68%);
  pointer-events: none;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 13px;
  border-radius: 999px;
  font-size: 12.5px;
  color: var(--brand-1);
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.2);
}

.pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand-1);
  box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.55);
  animation: pulseRing 2s ease-out infinite;
}

@keyframes pulseRing {
  70% {
    box-shadow: 0 0 0 8px rgba(99, 102, 241, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(99, 102, 241, 0);
  }
}

.hero-title {
  margin: 18px 0 14px;
  overflow-wrap: anywhere;
  font-size: clamp(34px, 5vw, 54px);
  line-height: 1.12;
  font-weight: 800;
  letter-spacing: -0.03em;
  /* 渐变以 CSS 变量下发，由每个字符 span 各自裁剪，避免子元素继承透明填充色却无背景 */
  --title-grad: linear-gradient(118deg, #1e293b 12%, var(--brand-1) 52%, var(--brand-2));
  color: var(--ink-1);
}

.hero-sub {
  max-width: 46ch;
  overflow-wrap: anywhere;
  margin: 0 0 26px;
  font-size: 15.5px;
  line-height: 1.75;
  color: var(--ink-3);
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 34px;
  padding-top: 22px;
  border-top: 1px dashed var(--surface-border);
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat strong {
  font-size: 22px;
  font-weight: 750;
  letter-spacing: -0.02em;
  background: linear-gradient(120deg, var(--brand-1), var(--brand-2));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stat span {
  font-size: 12px;
  color: var(--ink-3);
}

.divider {
  width: 1px;
  height: 28px;
  background: var(--surface-border);
}

.hero-side {
  display: flex;
  align-items: center;
}

.live-panel {
  width: 100%;
  padding: 20px;
  border-radius: var(--radius-md);
  background: linear-gradient(160deg, rgba(15, 23, 42, 0.92), rgba(30, 41, 59, 0.88));
  color: #e2e8f0;
  box-shadow: 0 20px 46px -26px rgba(15, 23, 42, 0.7);
  animation: floaty 6s ease-in-out infinite;
}

.live-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: #cbd5e1;
}

.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #64748b;
  transition: background var(--dur-mid) linear;
}

.live-dot.is-live {
  background: #34d399;
  box-shadow: 0 0 0 4px rgba(52, 211, 153, 0.18);
}

.live-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin: 16px 0 12px;
}

.live-grid div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.live-grid dt {
  font-size: 11.5px;
  color: #94a3b8;
}

.live-grid dd {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: #fff;
}

.block {
  margin-top: 36px;
}

.content-split {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 26px;
  margin-top: 32px;
}

.latest-block {
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin-top: 0;
}

.article-grid {
  --article-gap: 18px;
  --article-width: calc((100% - 2 * var(--article-gap)) / 3);
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: var(--article-gap);
  margin-top: 18px;
}

.article-grid > * {
  flex: 1 1 var(--article-width);
  height: auto;
}

.more-row {
  margin-top: 14px;
  text-align: center;
}

.empty-block {
  margin: 34px 0 8px;
  padding: 28px 20px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.5);
  border: 1px dashed var(--surface-border);
}

.empty-mark {
  font-size: 36px;
  line-height: 1;
}

.side-empty {
  margin: 0;
  font-size: 12.5px;
  color: var(--ink-3);
}

.side-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-self: start;
  min-width: 0;
  position: sticky;
  top: 88px;
}

.side-card {
  padding: 18px 20px;
}

.side-card h3 {
  margin: 0 0 14px;
  font-size: 14.5px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.popular-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.popular-list li {
  display: flex;
  align-items: baseline;
  gap: 9px;
  font-size: 13.5px;
}

.rank {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  color: var(--ink-3);
  background: rgba(148, 163, 184, 0.16);
}

.rank.is-top {
  color: #fff;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-3));
}

.popular-title {
  flex: 1;
  min-width: 0;
  color: var(--ink-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color var(--dur-fast) linear;
}

.popular-title:hover {
  color: var(--brand-1);
}

.popular-views {
  font-size: 11.5px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

.chip-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip-wrap :deep(.ant-tag) {
  max-width: 100%;
  margin-inline-end: 0;
  white-space: normal;
  overflow-wrap: anywhere;
}

.clickable {
  cursor: pointer;
  transition: transform var(--dur-fast) var(--ease-out);
}

.clickable:hover {
  transform: translateY(-2px);
}

.chip-count {
  margin-left: 4px;
  opacity: 0.6;
  font-variant-numeric: tabular-nums;
}

.comment-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comment-meta {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 4px 8px;
  overflow-wrap: anywhere;
  font-size: 12.5px;
  margin-bottom: 4px;
}

.comment-meta span {
  color: var(--ink-3);
}

.comment-list p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
  color: var(--ink-2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.archive-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 20px 22px;
  margin-top: 18px;
}

.archive-pill {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.07);
  border: 1px solid rgba(99, 102, 241, 0.14);
  font-size: 13px;
  transition: transform var(--dur-fast) var(--ease-out), background var(--dur-fast) linear;
}

.archive-pill:hover {
  transform: translateY(-2px);
  background: rgba(99, 102, 241, 0.12);
}

.archive-pill strong {
  font-weight: 700;
  color: var(--ink-1);
}

.archive-pill span {
  color: var(--ink-3);
  font-size: 12px;
}

.archive-pill em {
  font-style: normal;
  font-size: 11.5px;
  color: var(--brand-1);
}

@media (max-width: 1280px) {
  .article-grid { --article-width: calc((100% - var(--article-gap)) / 2); }
}

@media (max-width: 1024px) {
  .hero-inner {
    grid-template-columns: minmax(0, 1fr);
    padding: 32px 26px 28px;
  }
  .content-split {
    grid-template-columns: minmax(0, 1fr);
  }
  .side-col {
    position: static;
  }
}

@media (max-width: 640px) {
  .hero { padding-block: 16px 0; }
  .hero-inner { gap: 16px; padding: 18px 16px; }
  .hero-title { margin: 12px 0 8px; }
  .hero-sub { margin-bottom: 14px; }
  .hero-actions { gap: 8px; }
  .hero-actions :deep(.ant-btn) { min-height: 40px; padding-inline: 16px; font-size: 14px; }
  .hero-stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 6px;
    margin-top: 14px;
    padding-top: 10px;
    text-align: center;
  }
  .stat { overflow-wrap: anywhere; }
  .stat strong { font-size: 16px; }
  .stat span { font-size: 11px; }
  .divider { display: none; }
  .live-panel { padding: 12px 14px; animation: none; }
  .live-grid { margin: 8px 0 0; grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .live-grid dd { font-size: 16px; overflow-wrap: anywhere; }
  .block, .content-split { margin-top: 22px; }
  .content-split { gap: 18px; }
  .article-grid { --article-width: 100%; --article-gap: 12px; margin-top: 12px; }
  .side-card, .archive-strip { padding: 14px 16px; }
}
</style>
