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
function checkoutPreview(skuIds) {
  return request({ url: '/checkout/preview', method: 'POST', data: { skuIds } })
}

function orderCreate(data) {
  return request({ url: '/orders', method: 'POST', data })
}

module.exports = {
  health,
  login,
  spuPage,
  cartList,
  cartAdd,
  cartUpdateQuantity,
  cartRemove,
  cartClear,
  checkoutPreview,
  orderCreate,
}