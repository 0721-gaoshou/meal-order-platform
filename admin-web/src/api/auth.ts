import request from './request'
import type { ApiResult } from './request'

export interface LoginResult {
  token: string
}

export function login(data: { username: string; password: string }) {
  // 后端登录接口在 M1 实现
  return request.post<never, ApiResult<LoginResult>>('/auth/login', data)
}
