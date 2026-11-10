// 登录页（开发占位：预填 admin；正式微信登录 M4 接 wx.login + code2session）
const api = require('../../utils/api')

Page({
  data: {
    username: 'admin',
    password: 'admin123',
    submitting: false,
  },

  onLoad() {
    if (wx.getStorageSync('token')) {
      wx.navigateBack()
    }
  },

  onInputUsername(e) {
    this.setData({ username: e.detail.value })
  },

  onInputPassword(e) {
    this.setData({ password: e.detail.value })
  },

  onTapLogin() {
    if (this.data.submitting) return
    if (!this.data.username || !this.data.password) {
      wx.showToast({ title: '请输入账号密码', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    api
      .login({ username: this.data.username, password: this.data.password })
      .then((data) => {
        wx.setStorageSync('token', data.accessToken)
        wx.setStorageSync('username', data.username)
        wx.setStorageSync('role', data.role)
        this.setData({ submitting: false })
        wx.showToast({ title: '登录成功', icon: 'success', duration: 800 })
        setTimeout(() => wx.navigateBack(), 800)
      })
      .catch(() => this.setData({ submitting: false }))
  },
})