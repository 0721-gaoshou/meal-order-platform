// 我的订单列表
const api = require('../../utils/api')
const { fenToYuan } = require('../../utils/money')

const STATUS_TEXT = {
  PENDING_PAY: '待支付', PAID: '已支付', MAKING: '制作中', READY: '待取餐',
  COMPLETED: '已完成', CLOSED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款',
}

Page({
  data: {
    tab: 'ALL',
    list: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    token: '',
    loggingIn: false,
  },

  onShow() {
    const token = wx.getStorageSync('token') || ''
    this.setData({ token })
    if (!token) {
      this.setData({ list: [] })
      return
    }
    this.loadPage(true)
  },

  loadPage(reset) {
    if (this.data.loading || !this.data.token) return
    const pageNum = reset ? 1 : this.data.pageNum + 1
    // wx.request 会把 undefined 序列化成字面量 "undefined"，筛选参数按需拼接
    const params = { pageNum, pageSize: 10 }
    if (this.data.tab !== 'ALL') params.status = this.data.tab
    this.setData({ loading: true })
    api
      .ordersPage(params)
      .then((page) => {
        const rows = (page.records || []).map((o) => ({
          id: o.id,
          orderNo: o.orderNo,
          statusText: STATUS_TEXT[o.status] || o.status,
          pending: o.status === 'PENDING_PAY',
          payText: fenToYuan(o.payAmount),
          createdText: (o.createdAt || '').slice(5, 16).replace('T', ' '),
          summary: o.items.length
            ? (o.items[0].spuName + ' × ' + o.items[0].quantity
              + (o.items.length > 1 ? ' 等 ' + o.items.length + ' 件' : ''))
            : '—',
        }))
        this.setData({
          list: reset ? rows : this.data.list.concat(rows),
          pageNum,
          hasMore: page.current < page.pages,
          loading: false,
        })
      })
      .catch(() => this.setData({ loading: false }))
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    if (tab === this.data.tab) return
    this.setData({ tab, list: [] })
    this.loadPage(true)
  },

  onTapOrder(e) {
    wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + e.currentTarget.dataset.id })
  },

  onTapLogin() {
    if (this.data.loggingIn) return
    this.setData({ loggingIn: true })
    api
      .login({ username: 'admin', password: 'admin123' })
      .then((data) => {
        wx.setStorageSync('token', data.accessToken)
        wx.setStorageSync('username', data.username)
        wx.setStorageSync('role', data.role)
        this.setData({ loggingIn: false, token: data.accessToken })
        this.loadPage(true)
      })
      .catch(() => this.setData({ loggingIn: false }))
  },

  onReachBottom() {
    if (this.data.hasMore) this.loadPage(false)
  },
})