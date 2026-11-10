// 我的订单列表（产品化精修：状态配色/待支付倒计时/骨架屏/下拉刷新）
const api = require('../../utils/api')
const { fenToYuan } = require('../../utils/money')

const STATUS_TEXT = {
  PENDING_PAY: '待支付', PAID: '已支付', MAKING: '制作中', READY: '待取餐',
  COMPLETED: '已完成', CLOSED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款',
}
const STATUS_CLASS = {
  PENDING_PAY: '', PAID: 'tag-green', MAKING: 'tag-green', READY: 'tag-green',
  COMPLETED: 'tag-green', CLOSED: 'tag-grey', REFUNDING: 'tag-grey', REFUNDED: 'tag-grey',
}

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}

function countdownText(expireTime) {
  if (!expireTime) return ''
  const left = new Date(expireTime).getTime() - Date.now()
  if (left <= 0) return '已超时'
  const m = Math.floor(left / 60000)
  const s = Math.floor((left % 60000) / 1000)
  return '剩余 ' + pad(m) + ':' + pad(s)
}

Page({
  data: {
    tab: 'ALL',
    list: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    token: '',
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

  onHide() {
    this.stopTick()
  },

  onUnload() {
    this.stopTick()
  },

  loadPage(reset) {
    if (this.data.loading || !this.data.token) return
    const pageNum = reset ? 1 : this.data.pageNum + 1
    const params = { pageNum, pageSize: 10 }
    if (this.data.tab !== 'ALL') params.status = this.data.tab
    this.setData({ loading: true })
    api
      .ordersPage(params)
      .then((page) => {
        const rows = (page.records || []).map((o) => ({
          id: o.id,
          orderNo: o.orderNo,
          status: o.status,
          statusText: STATUS_TEXT[o.status] || o.status,
          statusClass: STATUS_CLASS[o.status] || 'tag-grey',
          pending: o.status === 'PENDING_PAY',
          expireTime: o.expireTime,
          countdownText: o.status === 'PENDING_PAY' ? countdownText(o.expireTime) : '',
          payText: fenToYuan(o.payAmount),
          discountText: o.discountAmount > 0 ? '已优惠 ¥' + fenToYuan(o.discountAmount) : '',
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
        this.startTick()
      })
      .catch(() => this.setData({ loading: false }))
  },

  startTick() {
    this.stopTick()
    if (!this.data.list.some((o) => o.pending)) return
    this.timer = setInterval(() => {
      const patch = {}
      this.data.list.forEach((o, i) => {
        if (o.pending) patch['list[' + i + '].countdownText'] = countdownText(o.expireTime)
      })
      if (Object.keys(patch).length) this.setData(patch)
    }, 1000)
  },

  stopTick() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    if (tab === this.data.tab) return
    this.setData({ tab, list: [], pageNum: 1, hasMore: true })
    this.loadPage(true)
  },

  onTapOrder(e) {
    wx.navigateTo({ url: '/pages/order-detail/order-detail?id=' + e.currentTarget.dataset.id })
  },

  onTapLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },

  onPullDownRefresh() {
    this.loadPage(true)
    setTimeout(() => wx.stopPullDownRefresh(), 800)
  },

  onReachBottom() {
    if (this.data.hasMore) this.loadPage(false)
  },
})