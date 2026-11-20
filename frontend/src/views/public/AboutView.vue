<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { publicApi } from '@/api'
import { formatNumber } from '@/utils/format'
import AboutMarkdown from '@/components/public/AboutMarkdown.vue'

const router = useRouter()
const site = ref(null)
const markdown = ref('')
const aboutFailed = ref(false)

onMounted(() => {
  publicApi.site().then((info) => { site.value = info }).catch(() => null)
  publicApi.about()
    .then((about) => { markdown.value = about.markdown })
    .catch(() => { aboutFailed.value = true })
})
</script>

<template>
  <div class="page-shell about-page">
    <section class="glass hero-card rise-in">
    <div class="hero-profile">
      <div class="hero-left">
        <a-avatar :size="76" :src="site?.avatar" class="avatar">
          {{ (site?.name || '云').slice(0, 1) }}
        </a-avatar>
        <div>
          <h1>{{ site?.name || '云间小筑' }}</h1>
          <p class="role">{{ site?.title || '个人博客' }}</p>
        </div>
      </div>
      <p class="intro">{{ site?.description || '记录工程实践与生活碎片。' }}</p>
    </div>
    <div class="hero-overview">
      <div class="metrics">
        <div><strong>{{ formatNumber(site?.articleCount ?? 0) }}</strong><span>文章</span></div>
        <div><strong>{{ formatNumber(site?.viewCount ?? 0) }}</strong><span>阅读</span></div>
        <div><strong>{{ site?.categoryCount ?? 0 }}</strong><span>分类</span></div>
        <div><strong>{{ site?.tagCount ?? 0 }}</strong><span>标签</span></div>
      </div>
      <div class="actions">
        <a-button type="primary" shape="round" @click="router.push('/articles')">浏览文章</a-button>
        <a-button v-magnetic shape="round" :href="`mailto:${site?.email || 'hello@example.com'}`">
          联系我
        </a-button>
      </div>
    </div>
    </section>
    <section v-if="markdown.trim()" class="glass about-content rise-in" aria-label="关于页正文">
      <AboutMarkdown :markdown="markdown" />
    </section>
    <a-alert v-else-if="aboutFailed" class="about-content-error" type="error" message="关于页正文加载失败" show-icon />
  </div>
</template>

<style scoped>
.about-page {
  padding-top: 38px;
}

.hero-card,
.about-content {
  padding: 34px clamp(18px, 4vw, 36px) 30px;
}

.about-content,
.about-content-error {
  margin-top: 18px;
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, auto);
  align-items: center;
  gap: clamp(18px, 4vw, 56px);
  overflow-wrap: anywhere;
}
.hero-card .avatar { flex: none; }
.hero-overview { border-inline-start: 1px solid var(--surface-border); padding-inline-start: clamp(18px, 4vw, 46px); }
.intro { white-space: pre-line; }
@media (max-width: 768px) {
  .hero-card { grid-template-columns: minmax(0, 1fr); }
  .hero-overview { border-inline-start: 0; padding-inline-start: 0; border-top: 1px solid var(--surface-border); }
}

.hero-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar {
  box-shadow: 0 14px 32px -16px rgba(99, 102, 241, 0.6);
  animation: floaty 6s ease-in-out infinite;
}

.hero-card h1 {
  margin: 0 0 4px;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.role {
  margin: 0;
  font-size: 13.5px;
  color: var(--ink-3);
}

.intro {
  max-width: 62ch;
  margin: 20px 0 0;
  font-size: 15px;
  line-height: 1.8;
  color: var(--ink-2);
}

.metrics {
  display: flex;
  gap: 32px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px dashed var(--surface-border);
  flex-wrap: wrap;
}

.metrics div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.metrics strong {
  font-size: 21px;
  font-weight: 750;
  background: linear-gradient(120deg, var(--brand-1), var(--brand-2));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.metrics span {
  font-size: 12px;
  color: var(--ink-3);
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 22px;
}

</style>
