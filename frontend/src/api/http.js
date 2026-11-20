import axios from 'axios'
import { message } from 'ant-design-vue'

const TOKEN_KEY = 'blog-token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers['blog-token'] = token
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.code === 'ERR_CANCELED' || error.config?.silent) return Promise.reject(error)
    const status = error.response?.status
    const body = error.response?.data
    const text = body?.message || error.message || '请求失败'

    if (status === 401) {
      setToken('')
      const { pathname, search } = window.location
      if (!pathname.startsWith('/admin/login')) {
        message.warning('登录状态已失效，请重新登录')
        // 仅清 token 不够：路由守卫只在导航时生效，
        // 若停留当前页，后续每个请求依旧 401，页面会停在不可用的半残状态。
        const redirect = encodeURIComponent(pathname + search)
        window.location.replace(`/admin/login?redirect=${redirect}`)
      }
    } else if (status === 404) {
      message.error(body?.message || '资源不存在')
    } else if (status === 409) {
      message.warning(text)
    } else if (status >= 500) {
      message.error('服务器异常，请稍后重试')
    } else {
      message.error(text)
    }
    return Promise.reject(error)
  }
)

export default http
