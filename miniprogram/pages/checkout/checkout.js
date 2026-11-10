// 结算页：预览试算（含券）+ 选券 + 下单
// 防重复提交：按钮 loading 守卫；requestId 页面内复用；选券只重发预览，不换 requestId
const api = require('../../utils/api')
const { fenToYuan } = require('../../utils/money')
const { uuid } = require('../../utils/uuid')

const typeText = (c) => {
  if (c.type === 'DISCOUNT') {
    const off = (c.discountRate / 10).toFixed(c.discountRate % 10 === 0 ? 0 : 1)
    return (c.thresholdAmount > 0 ? '满' + fenToYuan(c.thresholdAmount) + '元' : '无门槛') + off + '折'
  }
  return (c.thresholdAmount > 0 ? '满' + fenToYuan(c.thresholdAmount) + '元减' : '无门槛减')
    + fenToYuan(c.discountAmount) + '元'
}

Page({
  data: {
    items: [],
    totalText: '0.00',
    discountText: '0.00',
    payText: '0.00',
    couponName: '',
    userCouponId: '',
    usable: [],
    usableCount: 0,
    sheet: false,
    remark: '',
    token: '',
    ready: false,
    submitting: false,
  },

  onLoad(query) {
    this.skuIds = (query.skuIds || '').split(',').filter(Boolean)
    this.requestId = uuid()
    if (!this.skuIds.length) {
      wx.showToast({ title: '未选择商品', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 600)
      return
    }
    this.refreshPreview('')
  },

  // userCouponId 为空串表示不用券；金额与分摊以服务端返回为准
  refreshPreview(userCouponId) {
    return api
      .checkoutPreview(this.skuIds, userCouponId || undefined)
      .then((data) => {
        this.setData({
          items: (data.items || []).map((i) => ({
            ...i,
            priceText: fenToYuan(i.unitPrice),
            subtotalText: fenToYuan(i.subtotal),
            shareText: i.couponShareAmount > 0 ? '-¥' + fenToYuan(i.couponShareAmount) : '',
          })),
          totalText: fenToYuan(data.totalAmount),
          discountText: fenToYuan(data.discountAmount),
          payText: fenToYuan(data.payAmount),
          couponName: data.couponName || '',
          userCouponId: userCouponId || '',
          token: data.token,
          ready: true,
        })
        this.loadUsable(data.totalAmount)
      })
      .catch(() => this.setData({ ready: false }))
  },

  loadUsable(amount) {
    api
      .usableCoupons(amount)
      .then((list) =>
        this.setData({
          usable: (list || []).map((c) => ({
            ...c,
            typeText: typeText(c),
            estimatedText: c.estimatedDiscount == null ? '' : fenToYuan(c.estimatedDiscount),
          })),
          usableCount: (list || []).filter((c) => c.usable).length,
        })
      )
      .catch(() => this.setData({ usable: [], usableCount: 0 }))
  },

  openSheet() {
    if (!this.data.usable.length) {
      wx.showToast({ title: '暂无可用券，去券中心看看', icon: 'none' })
      return
    }
    this.setData({ sheet: true })
  },

  closeSheet() {
    this.setData({ sheet: false })
  },

  onPickCoupon(e) {
    const { id, usable } = e.currentTarget.dataset
    if (id && !usable) return
    this.setData({ sheet: false })
    // 同一张券重复点选不再重发预览
    if ((id || '') === this.data.userCouponId) return
    this.refreshPreview(id || '')
  },

  onInputRemark(e) {
    this.setData({ remark: e.detail.value })
  },

  onTapRefresh() {
    this.refreshPreview(this.data.userCouponId)
  },

  onTapSubmit() {
    if (this.data.submitting || !this.data.token) return
    this.setData({ submitting: true })
    api
      .orderCreate({
        requestId: this.requestId,
        token: this.data.token,
        skuIds: this.skuIds,
        remark: this.data.remark,
        userCouponId: this.data.userCouponId || undefined,
      })
      .then((order) => {
        this.setData({ submitting: false })
        wx.showModal({
          title: '下单成功',
          content: '单号 ' + order.orderNo + '\n实付 ¥' + fenToYuan(order.payAmount)
            + (order.discountAmount > 0 ? '（已优惠 ¥' + fenToYuan(order.discountAmount) + '）' : ''),
          showCancel: false,
          confirmText: '回首页',
          confirmColor: '#ff6b35',
          success: () => wx.reLaunch({ url: '/pages/index/index' }),
        })
      })
      .catch((err) => {
        this.setData({ submitting: false })
        const msg = (err && err.message) || ''
        // 令牌过期或券被抢用：重发预览（券状态与金额会刷新），用户可直接再点提交
        if (msg.indexOf('令牌') >= 0 || msg.indexOf('优惠券') >= 0) {
          this.refreshPreview(this.data.userCouponId)
        }
      })
  },
})