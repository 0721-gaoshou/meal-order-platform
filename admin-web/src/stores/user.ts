import { defineStore } from 'pinia'
import { login, logout } from '@/api/auth'
import {
  clearCredentials,
  getAccessToken,
  getRefreshToken,
  getRole,
  saveCredentials,
} from '@/utils/credentials'

export const useUserStore = defineStore('user', {
  state: () => ({
    accessToken: getAccessToken(),
    refreshToken: getRefreshToken(),
    username: localStorage.getItem('username') ?? '',
    role: getRole(),
  }),
  getters: {
    isLoggedIn: (state) => state.accessToken !== '',
    isAdmin: (state) => state.role === 'ADMIN',
  },
  actions: {
    hasRole(...roles: string[]): boolean {
      return roles.length === 0 || roles.includes(this.role)
    },
    async login(username: string, password: string) {
      const result = await login({ username, password })
      saveCredentials(result.data)
      this.accessToken = result.data.accessToken
      this.refreshToken = result.data.refreshToken
      this.username = result.data.username
      this.role = result.data.role
    },
    async logout() {
      // 后端为无状态 JWT：接口只是语义占位，失败不阻塞本地清理
      try {
        await logout()
      } catch {
        // 拦截器已统一提示
      } finally {
        clearCredentials()
        this.accessToken = ''
        this.refreshToken = ''
        this.username = ''
        this.role = ''
      }
    },
  },
})
