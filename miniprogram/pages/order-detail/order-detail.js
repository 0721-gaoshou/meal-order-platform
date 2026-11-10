// 订单详情：状态/倒计时 + 待支付操作（Mock 支付、取消）
const api = require('../../utils/api')
const { fenToYuan } = require('../../utils/money')

const STATUS_TEXT = {
  PENDING_PAY: '待支付', PAID: '已支付', MAKING: '制作中', READY: '待取餐',
  COMPLETED: '已完成', CLOSED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款',
}
const STATUS_TIP = {
  PENDING_PAY: '请尽快完成支付，超时订单将自动取消',
  PAID: '商家已接单，正在为你准备',
  MAKING: '正在制作中，请稍候',
  READY: '餐品已就绪，请到店取餐',
  COMPLETED: '订单已完成，感谢光临',
  CLOSED: '订单已取消',
  REFUNDING: '退款申请已提交，等待渠道处理',
  REFUNDED: '退款已完成，款项将原路退回',
}
const REFUNDABLE = ['PAID', 'MAKING', 'READY']
const STATUS_CARD_CLASS = {
  PAID: 'status-green', MAKING: 'status-green', READY: 'status-green', COMPLETED: 'status-green',
  CLOSED: 'status-grey', REFUNDING: 'status-grey', REFUNDED: 'status-grey',
}

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}
function countdownText(expireTime) {
  if (!expireTime) return ''
  const left = new Date(expireTime).getTime() - Date.now()
  if (left <= 0) return '已超时，订单即将自动取消'
  const m = Math.floor(left / 60000)
  const s = Math.floor((left % 60000) / 1000)
  return '剩余 ' + pad(m) + ':' + pad(s)
}

Page({
  data: {
    order: null,
    loading: true,
    paying: false,
    refunding: false,
  },

  onLoad(query) {
    if (!query.id) {
      wx.showToast({ title: '缺少订单号', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 600)
      return
    }
    this.orderId = query.id
    this.load()
  },

  load() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
    api
      .orderDetail(this.orderId)
      .then((o) => {
        this.setData({
          loading: false,
          order: {
            ...o,
            statusText: STATUS_TEXT[o.status] || o.status,
            statusTip: STATUS_TIP[o.status] || '',
            statusCardClass: STATUS_CARD_CLASS[o.status] || '',
            pending: o.status === 'PENDING_PAY',
            refundable: REFUNDABLE.indexOf(o.status) >= 0,
            countdownText: o.status === 'PENDING_PAY' ? countdownText(o.expireTime) : '',
            createdText: (o.createdAt || '').replace('T', ' ').slice(0, 16),
            expireText: (o.expireTime || '').replace('T', ' ').slice(0, 16),
            payTimeText: o.payTime ? o.payTime.replace('T', ' ').slice(0, 16) : '',
            totalText: fenToYuan(o.totalAmount),
            discountText: fenToYuan(o.discountAmount),
            payText: fenToYuan(o.payAmount),
            items: (o.items || []).map((i) => ({
              ...i,
              unitPriceText: fenToYuan(i.unitPrice),
              subtotalText: fenToYuan(i.unitPrice * i.quantity),
              shareText: i.couponShareAmount > 0 ? '-¥' + fenToYuan(i.couponShareAmount) : '',
            })),
          },
        })
        if (o.status === 'PENDING_PAY') {
          this.timer = setInterval(() => {
            this.setData({ ['order.countdownText']: countdownText(o.expireTime) })
          }, 1000)
        }
      })
      .catch(() => this.setData({ loading: false }))
  },

  // Mock 支付：发起拿商户单号 → 模拟渠道回调 → 刷新状态（正式微信支付 M9 替换回调来源）
  onTapPay() {
    if (this.data.paying) return
    this.setData({ paying: true })
    api
      .orderPay(this.orderId)
      .then((pay) =>
        api.payMockCallback({ outTradeNo: pay.outTradeNo, tradeNo: 'MOCK' + Date.now() })
      )
      .then(() => {
        this.setData({ paying: false })
        wx.showToast({ title: '支付成功', icon: 'success' })
        setTimeout(() => this.load(), 600)
      })
      .catch(() => this.setData({ paying: false }))
  },

  onTapCancel() {
    wx.showModal({
      title: '取消订单',
      content: '取消后库存与优惠券将退回，确定取消吗？',
      confirmColor: '#ff6b35',
      success: (res) => {
        if (!res.confirm) return
        api
          .orderCancel(this.orderId)
          .then(() => {
            wx.showToast({ title: '订单已取消', icon: 'none' })
            setTimeout(() => this.load(), 600)
          })
          .catch(() => {})
      },
    })
  },

  // Mock 退款：申请 → 模拟渠道退款回调 → 刷新为已退款（正式微信退款 M9 替换回调来源）
  onTapRefund() {
    if (this.data.refunding) return
    this.setData({ refunding: true })
    api
      .orderRefund(this.orderId, { reason: '用户申请退款（Mock）' })
      .then((refund) => api.refundMockCallback({ refundNo: refund.refundNo }))
      .then(() => {
        this.setData({ refunding: false })
        wx.showToast({ title: '退款成功', icon: 'success' })
        setTimeout(() => this.load(), 600)
      })
      .catch(() => this.setData({ refunding: false }))
  },

  onUnload() {
    if (this.timer) clearInterval(this.timer)
  },
})