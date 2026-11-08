import request from './request'
import type { ApiResult } from './request'

/** 上传图片到 RustFS（经后端 S3 SDK 转发），返回可直接访问的 URL */
export function uploadImage(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<never, ApiResult<{ url: string }>>('/admin/upload/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000,
  })
}
