import request from './request'
import type { ApiResult } from './request'

export interface HealthInfo {
  service: string
  time: string
}

export function health() {
  return request.get<never, ApiResult<HealthInfo>>('/health')
}
