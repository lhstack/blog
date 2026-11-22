<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const submitting = ref(false)
const loginError = ref('')

async function onSubmit() {
  if (submitting.value) return

  submitting.value = true
  loginError.value = ''
  try {
    await auth.login({ username: form.username, password: form.password })
  } catch (error) {
    loginError.value = getLoginError(error)
    return
  } finally {
    submitting.value = false
  }

  message.success('登录成功')
  const redirect = route.query.redirect
  router.replace(typeof redirect === 'string' ? redirect : '/admin/dashboard')
}

function getLoginError(error) {
  const response = error.response
  if (response?.status >= 500) return '服务器异常，请稍后重试'
  if (error.code === 'ERR_NETWORK') return '无法连接到服务器，请检查网络后重试'
  if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') return '登录请求超时，请重试'

  const detail = response?.data?.message
  if (typeof detail === 'string' && detail.trim()) return detail
  if (response?.status === 401) return '用户名或密码不正确'
  return '登录失败，请稍后重试'
}
</script>

<template>
  <div class="login-page">
    <div class="login-card glass rise-in">
      <div class="login-brand">
        <span class="mark">B</span>
        <div>
          <h1>管理后台</h1>
          <p class="muted">登录后可发布文章、审核评论</p>
        </div>
      </div>

      <a-form layout="vertical" :model="form" @finish="onSubmit">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="form.username" size="large" placeholder="请输入用户名" autocomplete="username" />
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password
            v-model:value="form.password"
            size="large"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </a-form-item>
        <a-form-item v-if="loginError">
          <a-alert :message="loginError" type="error" show-icon role="alert" />
        </a-form-item>
        <a-button
          v-magnetic
          type="primary"
          size="large"
          html-type="submit"
          block
          shape="round"
          :loading="submitting"
        >
          登录
        </a-button>
      </a-form>

      <div class="login-foot">
        <RouterLink to="/">← 返回站点首页</RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 388px;
  padding: 32px 32px 24px;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 26px;
}

.mark {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  font-weight: 800;
  font-size: 17px;
  color: #fff;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-2));
  box-shadow: 0 12px 26px -14px rgba(99, 102, 241, 0.9);
}

.login-brand h1 {
  margin: 0 0 2px;
  font-size: 19px;
  font-weight: 750;
}

.login-brand p {
  margin: 0;
  font-size: 12.5px;
}

.login-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed var(--surface-border);
  font-size: 12.5px;
  color: var(--ink-3);
  flex-wrap: wrap;
}

.login-foot a:hover {
  color: var(--brand-1);
}
</style>
