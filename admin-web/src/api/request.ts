import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { clearCredentials, getAccessToken, getRefreshToken, saveCredentials } from '@/utils/credentials'

/** 与后端统一响应体对应：{ code, message, data }，约定见 docs/api-convention.md */
export interface ApiResult<T = unknown> {
  code: string
  message: string
  data: T
}

export const SUCCESS_CODE = '00000'
/** 未登录/token 过期业务码 */
export const UNAUTHORIZED_CODE = 'B0001'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

/** 后端 LoginResponse 结构（登录/刷新共用） */
interface LoginPayload {
  accessToken: string
  refreshToken: string
  username: string
  role: string
}

/** 单飞刷新：并发多个 B0001 只发一次 /auth/refresh，其余请求排队等结果后重放 */
let refreshing: Promise<boolean> | null = null

async function refreshTokens(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false
  try {
    // 刷新请求走裸 axios，不进本实例拦截器，避免递归
    const resp = await axios.post<ApiResult<LoginPayload>>(
      '/api/auth/refresh',
      { refreshToken },
      { timeout: 10000 },
    )
    const result = resp.data
    if (result.code === SUCCESS_CODE && result.data?.accessToken) {
      saveCredentials(result.data)
      return true
    }
    return false
  } catch {
    return false
  }
}

function forceLogout(): void {
  clearCredentials()
  ElMessage.error('登录已过期，请重新登录')
  window.location.href = '/login'
}

// 请求拦截：自动携带 accessToken
request.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一处理业务错误码与网络异常
request.interceptors.response.use(
  async (resp) => {
    const result = resp.data as ApiResult
    if (result.code === SUCCESS_CODE) {
      // 成功时直接返回业务体，调用方 `result.data` 即为载荷，
      // 与各 api 文件 `request.post<never, ApiResult<T>>` 的类型约定一致。
      // （M0 版返回整个 AxiosResponse，token 实际存成了 undefined，被 REQUIRE_AUTH=false 掩盖）
      return result as unknown as typeof resp
    }
    // 未登录/过期：单飞刷新后重放一次，仍失败则回登录页
    if (result.code === UNAUTHORIZED_CODE) {
      const original = resp.config as InternalAxiosRequestConfig & { _retried?: boolean }
      if (original._retried) {
        forceLogout()
        return Promise.reject(result)
      }
      refreshing ??= refreshTokens().finally(() => {
        refreshing = null
      })
      const ok = await refreshing
      if (!ok) {
        forceLogout()
        return Promise.reject(result)
      }
      original._retried = true
      return request.request(original)
    }
    ElMessage.error(result.message || '请求失败')
    return Promise.reject(result)
  },
  (error) => {
    const msg = error.message?.includes('timeout') ? '请求超时' : '网络异常或服务不可用'
    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export default request
