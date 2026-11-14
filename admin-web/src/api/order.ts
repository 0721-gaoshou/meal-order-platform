import request from './request'
import type { ApiResult } from './request'
import type { PageData } from './goods'

/** 后端 OrderDtos.OrderVO 对应；雪花 ID 一律 string，金额单位分 */
export interface OrderItemVO {
  skuId: string
  spuId: string
  spuName: string
  skuSpec: string
  skuImage: string | null
  quantity: number
  unitPrice: number
  couponShareAmount: number
  refundStatus: string
}

export interface OrderVO {
  id: string
  orderNo: string
  status: string
  totalAmount: number
  discountAmount: number
  payAmount: number
  couponId: string | null
  remark: string
  expireTime: string | null
  payTime: string | null
  createdAt: string
  items: OrderItemVO[]
}

export interface OrderQueryParams {
  status?: string
  pageNum?: number
  pageSize?: number
}

export function pageOrders(params: OrderQueryParams) {
  return request.get<never, ApiResult<PageData<OrderVO>>>('/admin/orders/page', { params })
}

/** 只允许 PAID→MAKING→READY→COMPLETED 逐步推进，跳态后端拒绝 */
export function transitionOrderStatus(id: string, status: string) {
  return request.put<never, ApiResult<void>>(`/admin/orders/${id}/status`, { status })
}