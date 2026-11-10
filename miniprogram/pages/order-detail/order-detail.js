// 订单详情
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
}

Page({
  data: {
    order: null,
    loading: true,
  },

  onLoad(query) {
    if (!query.id) {
      wx.showToast({ title: '缺少订单号', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 600)
      return
    }
    api
      .orderDetail(query.id)
      .then((o) => {
        this.setData({
          loading: false,
          order: {
            ...o,
            statusText: STATUS_TEXT[o.status] || o.status,
            statusTip: STATUS_TIP[o.status] || '',
            pending: o.status === 'PENDING_PAY',
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
      })
      .catch(() => this.setData({ loading: false }))
  },
})