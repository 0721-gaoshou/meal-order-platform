// index.js
const api = require('../../utils/api')

Page({
  data: {
    checking: false,
    healthText: '尚未检测',
    token: '',
  },

  onShow() {
    this.setData({ token: wx.getStorageSync('token') || '' })
  },

  // 开发者占位登录：M4 换 wx.login + code2session
  onTapLogin() {
    api
      .login({ username: 'admin', password: 'admin123' })
      .then((data) => {
        wx.setStorageSync('token', data.accessToken)
        this.setData({ token: data.accessToken })
        wx.showToast({ title: '登录成功', icon: 'success' })
      })
      .catch(() => {})
  },

  onTapGoods() {
    wx.navigateTo({ url: '/pages/goods/goods' })
  },

  onTapCart() {
    wx.navigateTo({ url: '/pages/cart/cart' })
  },

  onTapCoupons() {
    wx.navigateTo({ url: '/pages/coupons/coupons' })
  },

  // 一键验证：小程序 → 后端 → MySQL 链路是否贯通
  onTapHealth() {
    if (this.data.checking) return
    this.setData({ checking: true, healthText: '检测中…' })
    api
      .health()
      .then((data) => {
        this.setData({
          checking: false,
          healthText: '后端连通：' + data.service + ' · ' + data.time,
        })
      })
      .catch(() => {
        this.setData({
          checking: false,
          healthText: '后端不可达，请先启动 backend 服务',
        })
      })
  },
})