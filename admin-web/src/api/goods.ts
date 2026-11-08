import request from './request'
import type { ApiResult } from './request'

/**
 * 注意：后端雪花 ID 以字符串形式序列化（@JsonSerialize(ToStringSerializer)），
 * 超过 JS Number 安全整数，前端一律当 string 处理
 */
export interface Category {
  id: string
  name: string
  icon: string
  sort: number
  status: number
  createdAt: string
}

export interface SkuVO {
  id: string
  spec: string
  price: number
  stock: number
}

export interface SpuVO {
  id: string
  name: string
  categoryId: string
  categoryName: string
  mainImage: string
  tags: string
  description: string
  status: 'ON_SALE' | 'OFF_SALE'
  createdAt: string
  skus: SkuVO[]
}

export interface PageData<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export interface CategoryPayload {
  name: string
  icon?: string
  sort?: number
  status?: number
}

export interface SkuPayload {
  spec: string
  price: number
  stock: number
}

export interface SpuPayload {
  name: string
  categoryId: string
  mainImage?: string
  tags?: string
  description?: string
  skus: SkuPayload[]
}

export interface SpuQueryParams {
  keyword?: string
  categoryId?: string
  status?: string
  pageNum?: number
  pageSize?: number
}

// ---------------- 分类 ----------------

export function listCategories() {
  return request.get<never, ApiResult<Category[]>>('/admin/categories')
}

export function createCategory(data: CategoryPayload) {
  return request.post<never, ApiResult<void>>('/admin/categories', data)
}

export function updateCategory(id: string, data: CategoryPayload) {
  return request.put<never, ApiResult<void>>(`/admin/categories/${id}`, data)
}

export function deleteCategory(id: string) {
  return request.delete<never, ApiResult<void>>(`/admin/categories/${id}`)
}

/** 拖拽排序：ids 按新顺序整体提交 */
export function sortCategories(ids: string[]) {
  return request.put<never, ApiResult<void>>('/admin/categories/sort', { ids })
}

// ---------------- 商品 ----------------

export function pageSpus(params: SpuQueryParams) {
  return request.get<never, ApiResult<PageData<SpuVO>>>('/admin/spus/page', { params })
}

export function createSpu(data: SpuPayload) {
  return request.post<never, ApiResult<string>>('/admin/spus', data)
}

export function updateSpu(id: string, data: SpuPayload) {
  return request.put<never, ApiResult<void>>(`/admin/spus/${id}`, data)
}

export function batchChangeStatus(ids: string[], onSale: boolean) {
  return request.put<never, ApiResult<void>>('/admin/spus/status', { ids, onSale })
}

export function deleteSpu(id: string) {
  return request.delete<never, ApiResult<void>>(`/admin/spus/${id}`)
}
