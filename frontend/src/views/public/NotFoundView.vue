<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="page-shell notfound">
    <div class="glass panel rise-in">
      <div class="code">404</div>
      <h1>这里什么也没有</h1>
      <p class="muted">页面可能已被移动或删除，也可能只是一个打错的地址。</p>
      <div class="actions">
        <a-button type="primary" shape="round" @click="router.push('/')">回到首页</a-button>
        <a-button v-magnetic shape="round" @click="router.push('/articles')">看看文章</a-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notfound {
  display: grid;
  place-items: center;
  min-height: 68vh;
  padding-top: 40px;
}

.panel {
  padding: 48px 54px;
  text-align: center;
}

.code {
  font-size: 76px;
  font-weight: 850;
  line-height: 1;
  letter-spacing: -0.04em;
  background: linear-gradient(120deg, var(--brand-1), var(--brand-3), var(--brand-2));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.panel h1 {
  margin: 14px 0 8px;
  font-size: 22px;
  font-weight: 700;
}

.panel p {
  margin: 0 0 24px;
  font-size: 13.5px;
}

.actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}
</style>
