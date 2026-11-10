const { request } = require('./request')

// 健康检查：验证 小程序 → 后端 → MySQL 整条链路（M0 验收项）
function health() {
  return request({ url: '/health' })
}

// 登录（开发者占位：正式微信登录 M4 接 wx.login + code2session）
function login(data) {
  return request({ url: '/auth/login', method: 'POST', data })
}

// ------- 商品（占位：复用管理端分页接口，C 端公开商品接口 M4 补） -------
function spuPage(params) {
  return request({ url: '/admin/spus/page', data: params })
}

// ------- 购物车 -------
function cartList() {
  return request({ url: '/cart' })
}

function cartAdd(skuId, quantity) {
  return request({ url: '/cart/items', method: 'POST', data: { skuId, quantity } })
}

function cartUpdateQuantity(skuId, quantity) {
  return request({ url: '/cart/items/' + skuId, method: 'PUT', data: { quantity } })
}

function cartRemove(skuId) {
  return request({ url: '/cart/items/' + skuId, method: 'DELETE' })
}

function cartClear() {
  return request({ url: '/cart', method: 'DELETE' })
}

// ------- 结算与下单 -------
function checkoutPreview(skuIds, userCouponId) {
  return request({ url: '/checkout/preview', method: 'POST', data: { skuIds, userCouponId } })
}

function orderCreate(data) {
  return request({ url: '/orders', method: 'POST', data })
}

// ------- C 端公开商品（免登录） -------
function publicCategories() {
  return request({ url: '/public/goods/categories' })
}

function publicSpus(params) {
  return request({ url: '/public/goods/spus', data: params })
}

function publicSpuDetail(id) {
  return request({ url: '/public/goods/spus/' + id })
}

// ------- 订单 -------
function ordersPage(params) {
  return request({ url: '/orders/page', data: params })
}

function orderDetail(id) {
  return request({ url: '/orders/' + id })
}

// ------- 支付（Mock 网关）与取消 -------
function orderPay(orderId) {
  return request({ url: '/pay/orders/' + orderId, method: 'POST' })
}

function payMockCallback(data) {
  return request({ url: '/pay/mock-callback', method: 'POST', data })
}

function orderCancel(orderId) {
  return request({ url: '/orders/' + orderId + '/cancel', method: 'POST' })
}

// ------- 优惠券 -------
function couponCenter() {
  return request({ url: '/coupons' })
}

function couponReceive(couponId) {
  return request({ url: '/coupons/' + couponId + '/receive', method: 'POST' })
}

function myCoupons() {
  return request({ url: '/coupons/mine' })
}

// 结算页选券：按订单金额逐张给出可用性与预计优惠（amount 单位·分）
function usableCoupons(amount) {
  return request({ url: '/coupons/usable?amount=' + amount })
}

module.exports = {
  health,
  login,
  spuPage,
  publicCategories,
  publicSpus,
  publicSpuDetail,
  ordersPage,
  orderDetail,
  orderPay,
  payMockCallback,
  orderCancel,
  cartList,
  cartAdd,
  cartUpdateQuantity,
  cartRemove,
  cartClear,
  checkoutPreview,
  orderCreate,
  couponCenter,
  couponReceive,
  myCoupons,
  usableCoupons,
}