import request from './request'
import type { ApiResult } from './request'

export interface LoginResult {
  accessToken: string
  refreshToken: string
  username: string
  role: string
}

export function login(data: { username: string; password: string }) {
  return request.post<never, ApiResult<LoginResult>>('/auth/login', data)
}

export function logout() {
  return request.post<never, ApiResult<void>>('/auth/logout')
}
