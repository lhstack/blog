<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { publicApi } from '@/api'
import { renderMarkdown, extractHeadings } from '@/utils/markdown'
import { formatDate, formatNumber, fromNow } from '@/utils/format'

const route = useRoute()
const article = ref(null)
const comments = ref([])
const commentPage = ref(1)
const commentPageSize = 5
const commentList = ref(null)
const visibleComments = computed(() => {
  const start = (commentPage.value - 1) * commentPageSize
  return comments.value.slice(start, start + commentPageSize)
})
watch(commentPage, () => {
  if (commentList.value) commentList.value.scrollTop = 0
})
const loading = ref(true)
const notFound = ref(false)
const progress = ref(0)

const submitting = ref(false)
const form = ref({ author: '', email: '', website: '', content: '' })

const html = computed(() => renderMarkdown(article.value?.content))
const headings = computed(() => extractHeadings(article.value?.content))
const activeHeading = ref('')

async function load(id) {
  loading.value = true
  notFound.value = false
  try {
    article.value = await publicApi.article(id)
    comments.value = (await publicApi.comments(id)).items || []
    commentPage.value = 1
  } catch (error) {
    if (error.response?.status === 404) {
      notFound.value = true
      article.value = null
    } else {
      // 非 404（网络 / 500）由 axios 拦截器提示，这里保留空态而不是伪装成 404
      article.value = null
    }
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, (id) => {
  if (id) load(id)
})

/* 阅读进度与目录高亮：只在文章页生效 */
function onScroll() {
  const doc = document.documentElement
  const total = doc.scrollHeight - doc.clientHeight
  progress.value = total > 0 ? Math.min(100, (doc.scrollTop / total) * 100) : 0

  if (!headings.value.length) return
  let currentId = ''
  for (const heading of headings.value) {
    const el = document.getElementById(heading.id)
    if (el && el.getBoundingClientRect().top <= 120) {
      currentId = heading.id
    }
  }
  activeHeading.value = currentId
}

function scrollTo(id) {
  const el = document.getElementById(id)
  if (el) {
    const top = el.getBoundingClientRect().top + window.scrollY - 96
    window.scrollTo({ top, behavior: 'smooth' })
  }
}

async function submitComment() {
  if (!form.value.author?.trim() || !form.value.content?.trim()) {
    return
  }
  submitting.value = true
  try {
    await publicApi.submitComment(article.value.id, {
      author: form.value.author.trim(),
      email: form.value.email || null,
      website: form.value.website || null,
      content: form.value.content.trim()
    })
    form.value.content = ''
    message.success('已提交，审核通过后公开')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  load(route.params.id)
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<template>
  <div class="detail-page">
    <div class="read-progress" :style="{ width: `${progress}%` }" />

    <div v-if="loading" class="page-shell" style="padding-top: 40px">
      <a-skeleton active :paragraph="{ rows: 14 }" />
    </div>

    <div v-else-if="notFound" class="page-shell">
      <a-result status="404" title="文章不存在" sub-title="它可能已被删除，或还未发布。">
        <template #extra>
          <a-button type="primary" @click="$router.push('/articles')">返回文章列表</a-button>
        </template>
      </a-result>
    </div>

    <template v-else-if="article">
      <div class="page-shell detail-grid">
        <article class="glass main-card rise-in">
          <header class="article-head">
            <div class="crumbs">
              <RouterLink to="/articles">文章</RouterLink>
              <span>/</span>
              <span>{{ article.categoryName || '未分类' }}</span>
            </div>
            <h1>{{ article.title }}</h1>
            <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
            <div class="article-meta">
              <span>{{ formatDate(article.publishedAt || article.createdAt) }}</span>
              <span class="sep">·</span>
              <span>{{ formatNumber(article.viewCount) }} 次阅读</span>
              <span class="sep">·</span>
              <span>{{ article.wordCount }} 字 / 约 {{ article.readingMinutes }} 分钟</span>
              <span v-if="article.updatedAt !== article.publishedAt" class="sep">·</span>
              <span v-if="article.updatedAt !== article.publishedAt">
                更新于 {{ fromNow(article.updatedAt) }}
              </span>
            </div>
            <div class="tag-row">
              <a-tag v-for="tag in article.tags" :key="tag.id" :color="tag.color"># {{ tag.name }}</a-tag>
            </div>
          </header>

          <img v-if="article.cover" :src="article.cover" :alt="article.title" class="hero-cover" />

          <div class="markdown-body" v-html="html" />

          <footer class="article-foot">
            <div class="nav-cards">
              <div class="nav-card" :class="{ 'is-empty': !article.previous }">
                <span class="nav-label">上一篇</span>
                <RouterLink v-if="article.previous" :to="`/articles/${article.previous.id}`">
                  {{ article.previous.title }}
                </RouterLink>
                <span v-else class="muted">已经是第一篇</span>
              </div>
              <div class="nav-card is-next" :class="{ 'is-empty': !article.next }">
                <span class="nav-label">下一篇</span>
                <RouterLink v-if="article.next" :to="`/articles/${article.next.id}`">
                  {{ article.next.title }}
                </RouterLink>
                <span v-else class="muted">已经是最新一篇</span>
              </div>
            </div>
          </footer>
        </article>

        <aside class="side">
          <div v-if="headings.length" class="glass toc-card">
            <h3>目录</h3>
            <ul>
              <li
                v-for="heading in headings"
                :key="heading.id"
                :class="{ 'is-sub': heading.level === 3, 'is-active': activeHeading === heading.id }"
                @click="scrollTo(heading.id)"
              >
                {{ heading.text }}
              </li>
            </ul>
          </div>

          <div v-if="article.related?.length" class="glass side-card">
            <h3>相关文章</h3>
            <ul class="related-list">
              <li v-for="item in article.related" :key="item.id">
                <RouterLink :to="`/articles/${item.id}`">{{ item.title }}</RouterLink>
              </li>
            </ul>
          </div>
        </aside>
      </div>

      <section class="page-shell comment-section">
        <h2 class="section-title">留言（{{ comments.length }}）</h2>

        <div class="comment-body">
          <div class="glass comment-list-card">
            <ul ref="commentList" class="comment-thread" tabindex="0" aria-label="公开留言">
            <li v-for="comment in visibleComments" :key="comment.id" class="comment-item">
              <div class="avatar">{{ comment.author.slice(0, 1) }}</div>
              <div class="comment-main">
                <div class="comment-head">
                  <strong>
                    <a v-if="comment.website" :href="comment.website" target="_blank" rel="noopener noreferrer">
                      {{ comment.author }}
                    </a>
                    <template v-else>{{ comment.author }}</template>
                  </strong>
                  <span class="muted">{{ fromNow(comment.createdAt) }}</span>
                </div>
                <p>{{ comment.content }}</p>
              </div>
            </li>
            <li v-if="!comments.length" class="muted empty-tip">还没有留言，来说点什么吧。</li>
            </ul>
            <div class="comment-pager" aria-label="留言分页">
              <a-pagination v-model:current="commentPage" :total="comments.length" :page-size="commentPageSize"
                :show-size-changer="false" size="small" :simple="true" />
            </div>
          </div>

          <div class="glass comment-form">
            <h3>留下你的想法</h3>
            <a-form layout="vertical" :model="form" @finish="submitComment">
              <div class="form-row">
                <a-form-item label="昵称" required>
                  <a-input v-model:value="form.author" placeholder="怎么称呼你" :maxlength="40" />
                </a-form-item>
                <a-form-item label="邮箱（可选，不公开）">
                  <a-input v-model:value="form.email" placeholder="you@example.com" :maxlength="120" />
                </a-form-item>
              </div>
              <a-form-item label="网站（可选）">
                <a-input v-model:value="form.website" placeholder="https://" :maxlength="200" />
              </a-form-item>
              <a-form-item label="内容" required>
                <a-textarea
                  v-model:value="form.content"
                  :rows="4"
                  :maxlength="2000"
                  show-count
                  placeholder="友善交流，不必客气…"
                />
              </a-form-item>
              <a-button
                v-magnetic
                type="primary"
                shape="round"
                html-type="submit"
                :loading="submitting"
                :disabled="!form.author?.trim() || !form.content?.trim()"
              >
                提交留言
              </a-button>
            </a-form>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail-page {
  position: relative;
  padding-top: 26px;
}

.read-progress {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  z-index: 30;
  background: linear-gradient(90deg, var(--brand-1), var(--brand-2), var(--brand-3));
  transition: width 80ms linear;
  border-radius: 0 999px 999px 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 26px;
  align-items: start;
}

.main-card {
  padding: 34px 38px 30px;
  overflow: hidden;
}

.article-head .crumbs {
  display: flex;
  gap: 8px;
  font-size: 12.5px;
  color: var(--ink-3);
  margin-bottom: 12px;
}

.article-head .crumbs a:hover {
  color: var(--brand-1);
}

.article-head h1 {
  margin: 0 0 12px;
  font-size: clamp(26px, 3.6vw, 36px);
  line-height: 1.28;
  font-weight: 800;
  letter-spacing: -0.025em;
}

.article-summary {
  margin: 0 0 16px;
  font-size: 14.5px;
  line-height: 1.75;
  color: var(--ink-3);
  padding-left: 14px;
  border-left: 3px solid rgba(99, 102, 241, 0.35);
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

.sep {
  opacity: 0.5;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
}

.hero-cover {
  width: 100%;
  margin: 24px 0 8px;
  border-radius: 14px;
  aspect-ratio: 16 / 7.6;
  object-fit: cover;
}

.article-foot {
  margin-top: 34px;
  padding-top: 22px;
  border-top: 1px dashed var(--surface-border);
}

.nav-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.nav-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: rgba(99, 102, 241, 0.06);
  border: 1px solid rgba(99, 102, 241, 0.12);
  transition: transform var(--dur-fast) var(--ease-out), background var(--dur-fast) linear;
}

.nav-card:hover {
  transform: translateY(-3px);
  background: rgba(99, 102, 241, 0.1);
}

.nav-card.is-next {
  text-align: right;
}

.nav-label {
  font-size: 11.5px;
  color: var(--ink-3);
}

.nav-card a {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink-1);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.side {
  position: sticky;
  top: 88px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toc-card,
.side-card {
  padding: 16px 18px;
}

.toc-card h3,
.side-card h3 {
  margin: 0 0 12px;
  font-size: 13.5px;
  font-weight: 700;
}

.toc-card ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 52vh;
  overflow-y: auto;
}

.toc-card li {
  padding: 5px 10px;
  border-left: 2px solid transparent;
  font-size: 12.5px;
  color: var(--ink-3);
  cursor: pointer;
  line-height: 1.5;
  transition: color var(--dur-fast) linear, border-color var(--dur-fast) linear;
}

.toc-card li:hover {
  color: var(--brand-1);
}

.toc-card li.is-sub {
  padding-left: 20px;
  font-size: 12px;
}

.toc-card li.is-active {
  color: var(--brand-1);
  border-left-color: var(--brand-1);
  font-weight: 600;
}

.related-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.related-list a {
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color var(--dur-fast) linear;
}

.related-list a:hover {
  color: var(--brand-1);
}

.comment-section {
  margin-top: 40px;
}

.comment-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 22px;
  margin-top: 18px;
}

.comment-list-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 360px;
}

.comment-thread {
  flex: 1;
  min-height: 0;
  contain: size;
  overflow: auto;
  overscroll-behavior: contain;
  margin: 0;
  padding: 0;
  list-style: none;
}

.comment-pager {
  flex: none;
  display: flex;
  justify-content: center;
  padding: 12px;
  border-top: 1px solid var(--surface-border);
}

.comment-item + .comment-item { border-top: 1px solid var(--surface-border); }

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 18px;
}

.avatar {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-2));
  font-size: 14px;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
  margin-bottom: 5px;
}

.comment-head a:hover {
  color: var(--brand-1);
}

.comment-head .muted {
  font-size: 11.5px;
}

.comment-main p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.72;
  color: var(--ink-2);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.empty-tip {
  padding: 18px;
  font-size: 13px;
}

.comment-form {
  padding: 20px 22px;
}

.comment-form h3 {
  margin: 0 0 16px;
  font-size: 14.5px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

@media (max-width: 1024px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
  .side {
    position: static;
    order: -1;
  }
  .comment-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .main-card {
    padding: 24px 20px;
  }
  .nav-cards {
    grid-template-columns: 1fr;
  }
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
