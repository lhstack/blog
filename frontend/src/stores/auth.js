import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { getToken, setToken } from '@/api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    profile: null,
    loading: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async login(payload) {
      this.loading = true
      try {
        const result = await authApi.login(payload)
        this.token = result.token
        this.profile = result.profile
        setToken(result.token)
        return result
      } finally {
        this.loading = false
      }
    },
    async loadProfile() {
      if (!this.token) {
        return null
      }
      this.profile = await authApi.profile()
      return this.profile
    },
    async logout() {
      try {
        if (this.token) {
          await authApi.logout()
        }
      } finally {
        this.token = ''
        this.profile = null
        setToken('')
      }
    }
  }
})
