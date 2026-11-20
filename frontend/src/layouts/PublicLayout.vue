<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { publicApi } from '@/api'
import { formatNumber } from '@/utils/format'
import BackgroundMusicPlayer from '@/components/public/BackgroundMusicPlayer.vue'

const route = useRoute()
const router = useRouter()
const site = ref(null)
const scrolled = ref(false)

const navItems = [
  { name: 'home', label: '首页', path: '/' },
  { name: 'articles', label: '文章', path: '/articles' },
  { name: 'archives', label: '归档', path: '/archives' },
  { name: 'about', label: '关于', path: '/about' }
]

const activeKey = computed(() => {
  if (route.name === 'article') return 'articles'
  return route.name
})

const year = new Date().getFullYear()

/** ICP 备案号的官方查询入口固定，公安备案用后台可配的链接。 */
const ICP_QUERY_URL = 'https://beian.miit.gov.cn'

const hasFiling = computed(() => !!(site.value?.icpNumber || site.value?.policeNumber))

function onScroll() {
  scrolled.value = window.scrollY > 12
}

onMounted(async () => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
  try {
    site.value = await publicApi.site()
  } catch {
    // 站点信息失败不阻塞页面渲染，保持骨架
  }
})

onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))

function go(item) {
  router.push(item.path)
}
</script>

<template>
  <div class="public-layout" :class="{ 'is-article-library': route.name === 'articles', 'has-player': site?.presentation?.tracks?.length }">
    <header class="site-header" :class="{ 'is-scrolled': scrolled }">
      <div class="page-shell header-inner">
        <RouterLink to="/" class="brand">
          <span class="brand-mark">
            <svg viewBox="0 0 32 32" width="26" height="26" aria-hidden="true">
              <defs>
                <linearGradient id="brandGrad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#6366f1" />
                  <stop offset="100%" stop-color="#22d3ee" />
                </linearGradient>
              </defs>
              <rect width="32" height="32" rx="9" fill="url(#brandGrad)" />
              <path
                d="M9 22V10h4.2c2.6 0 4.2 1.4 4.2 3.6 0 1.5-.8 2.6-2 3.1 1.5.4 2.5 1.6 2.5 3.3 0 2.4-1.8 3.9-4.6 3.9H9Zm3-7.2h1.1c1 0 1.6-.5 1.6-1.3s-.6-1.3-1.6-1.3H12v2.6Zm0 5h1.4c1.1 0 1.8-.5 1.8-1.5s-.7-1.5-1.8-1.5H12v3Z"
                fill="#fff"
              />
            </svg>
          </span>
          <span class="brand-text">
            <strong>{{ site?.name || '云间小筑' }}</strong>
            <small>{{ site?.title || '个人博客' }}</small>
          </span>
        </RouterLink>

        <nav class="nav-links">
          <button
            v-for="item in navItems"
            :key="item.name"
            class="nav-link"
            :class="{ 'is-active': activeKey === item.name }"
            type="button"
            @click="go(item)"
          >
            {{ item.label }}
          </button>
        </nav>

        <div class="header-right">
          <a-tooltip title="文章总数">
            <span class="stat-chip">
              <span class="dot" />
              {{ formatNumber(site?.articleCount ?? 0) }} 篇
            </span>
          </a-tooltip>
        </div>
      </div>
    </header>

    <main class="site-main">
      <RouterView v-slot="{ Component }">
        <Transition name="route-fade" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>

    <footer class="site-footer">
      <div class="page-shell footer-inner">
        <div class="footer-left">
          <strong>{{ site?.name || '云间小筑' }}</strong>
          <span>© {{ site?.startYear || year }}–{{ year }}</span>
          <!-- 备案信息：与版权行同处一行；未在后台填写时不渲染，不做占位符 -->
          <template v-if="hasFiling">
            <a
              v-if="site?.icpNumber"
              class="filing-link"
              :href="ICP_QUERY_URL"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ site.icpNumber }}
            </a>
            <a
              v-if="site?.policeNumber"
              class="filing-link"
              :href="site.policeUrl || ICP_QUERY_URL"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ site.policeNumber }}
            </a>
          </template>
        </div>
        <div class="footer-links">
          <a href="/rss.xml" type="application/rss+xml">RSS 订阅</a>
          <a :href="site?.github || '#'" target="_blank" rel="noopener noreferrer">GitHub</a>
          <a :href="`mailto:${site?.email || 'hello@example.com'}`">邮件</a>
        </div>
      </div>
    </footer>

    <Teleport to="body">
      <BackgroundMusicPlayer v-if="site?.presentation?.tracks?.length" :tracks="site.presentation.tracks" />
    </Teleport>
  </div>
</template>

<style scoped>
.public-layout {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  transition: background var(--dur-mid) var(--ease-out), box-shadow var(--dur-mid) var(--ease-out),
    border-color var(--dur-mid) var(--ease-out);
  border-bottom: 1px solid transparent;
}

.site-header.is-scrolled {
  background: rgba(255, 255, 255, 0.78);
  border-bottom-color: var(--surface-border);
  box-shadow: 0 10px 30px -22px rgba(15, 23, 42, 0.35);
  backdrop-filter: blur(16px) saturate(160%);
  -webkit-backdrop-filter: blur(16px) saturate(160%);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 20px;
  height: 68px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.brand-mark {
  flex: none;
  display: grid;
  place-items: center;
  filter: drop-shadow(0 6px 12px rgba(99, 102, 241, 0.32));
}

.brand-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.2;
}

.brand-text > * {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-text strong {
  font-size: 15.5px;
  letter-spacing: -0.01em;
}

.brand-text small {
  font-size: 11.5px;
  color: var(--ink-3);
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 12px;
}

.nav-link {
  position: relative;
  padding: 8px 14px;
  border: none;
  background: transparent;
  border-radius: 999px;
  font-size: 14px;
  color: var(--ink-2);
  cursor: pointer;
  transition: color var(--dur-fast) linear, background var(--dur-fast) linear;
}

.nav-link:hover {
  color: var(--brand-1);
  background: rgba(99, 102, 241, 0.08);
}

.nav-link.is-active {
  color: var(--brand-1);
  font-weight: 600;
}

.nav-link.is-active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 2px;
  width: 16px;
  height: 2px;
  border-radius: 999px;
  transform: translateX(-50%);
  background: linear-gradient(90deg, var(--brand-1), var(--brand-2));
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12.5px;
  color: var(--ink-2);
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--surface-border);
}

.stat-chip .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.18);
  animation: floaty 2.6s ease-in-out infinite;
}

.site-main {
  position: relative;
  z-index: 1;
  flex: 1;
  padding-bottom: 56px;
}

.is-article-library { height: 100dvh; min-height: 0; }
.is-article-library .site-main { display: grid; min-height: 0; padding-bottom: 0; }
.is-article-library .site-header, .is-article-library .site-footer { flex: none; }

.site-footer {
  position: relative;
  z-index: 1;
  border-top: 1px solid var(--surface-border);
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 22px;
  padding-bottom: 22px;
  font-size: 13px;
  color: var(--ink-3);
  flex-wrap: wrap;
}

.footer-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
}

.footer-left strong {
  color: var(--ink-1);
  font-size: 14px;
}

.footer-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.footer-left { overflow-wrap: anywhere; }

.footer-links a {
  color: var(--ink-2);
  transition: color var(--dur-fast) linear;
}

.footer-links a:hover {
  color: var(--brand-1);
}

/* 备案链接：与所在行的正文同基线，字号略小以区分合规信息与品牌信息 */
.filing-link {
  font-size: 12px;
  color: var(--ink-3);
  transition: color var(--dur-fast) linear;
}

.filing-link:hover {
  color: var(--brand-1);
}

@media (max-width: 860px) {
  .header-inner { height: auto; padding-block: 10px 7px; gap: 6px; flex-wrap: wrap; }
  .nav-links { order: 2; flex: 0 0 100%; justify-content: space-between; gap: 4px; margin-left: 0; }
  .nav-link { padding-inline: 8px; min-height: 40px; flex: 1; min-width: 0; }
  .site-header { background: rgba(255, 255, 255, 0.88); }
  .header-right { display: none; }
}
@media (max-width: 768px) {
  .footer-inner { padding-block: 12px; gap: 8px; }
  .has-player .footer-inner { padding-inline-end: calc(64px + env(safe-area-inset-right, 0px)); }
}
</style>
