import axios from 'axios'
import type { AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'

/** 与后端统一响应体对应：{ code, message, data }，约定见 docs/api-convention.md */
export interface ApiResult<T = unknown> {
  code: string
  message: string
  data: T
}

export const SUCCESS_CODE = '00000'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截：自动携带 JWT（M1 起生效）
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一处理业务错误码与网络异常
request.interceptors.response.use(
  (resp) => {
    const result = resp.data as ApiResult
    if (result.code === SUCCESS_CODE) {
      return resp
    }
    // 未登录/过期：清凭证并回登录页
    if (result.code === 'B0001') {
      localStorage.removeItem('token')
      ElMessage.error(result.message || '登录已过期')
      window.location.href = '/login'
      return Promise.reject(result)
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
