import { defineStore } from 'pinia'
import { login } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') ?? '',
    username: localStorage.getItem('username') ?? '',
  }),
  actions: {
    async login(username: string, password: string) {
      const result = await login({ username, password })
      this.token = result.data.token
      this.username = username
      localStorage.setItem('token', this.token)
      localStorage.setItem('username', username)
    },
    logout() {
      this.token = ''
      this.username = ''
      localStorage.removeItem('token')
      localStorage.removeItem('username')
    },
  },
})
