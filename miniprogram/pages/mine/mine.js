// 我的：登录态/用户名、券与订单入口、退出
Page({
  data: {
    token: '',
    username: '',
    avatarText: '未',
    roleText: '',
  },

  onShow() {
    const token = wx.getStorageSync('token') || ''
    const role = wx.getStorageSync('role') || ''
    // 早期登录只存了 token，用户名缺失时用兜底文案，避免空白卡片
    const username = wx.getStorageSync('username') || (token ? '点餐用户' : '')
    this.setData({
      token,
      username,
      avatarText: (username || '未')[0],
      roleText: role === 'ADMIN' ? '管理员' : role === 'MERCHANT' ? '商家' : '',
    })
  },

  onTapLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },

  onTapLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定退出当前账号吗？',
      confirmColor: '#ff6b35',
      success: (res) => {
        if (!res.confirm) return
        wx.removeStorageSync('token')
        wx.removeStorageSync('username')
        wx.removeStorageSync('role')
        this.onShow()
      },
    })
  },

  onTapCoupons() {
    wx.navigateTo({ url: '/pages/coupons/coupons' })
  },

  onTapOrders() {
    wx.switchTab({ url: '/pages/orders/orders' })
  },

  onTapCart() {
    wx.navigateTo({ url: '/pages/cart/cart' })
  },
})