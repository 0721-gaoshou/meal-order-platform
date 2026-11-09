// 结算页：预览试算 + 下单
// 防重复提交第一层：按钮 loading + submitting 守卫；requestId 页面内复用，重试命中后端幂等回放
const api = require('../../utils/api')
const { fenToYuan } = require('../../utils/money')
const { uuid } = require('../../utils/uuid')

Page({
  data: {
    items: [],
    totalText: '0.00',
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
    this.refreshPreview()
  },

  refreshPreview() {
    return api
      .checkoutPreview(this.skuIds)
      .then((data) => {
        this.setData({
          items: (data.items || []).map((i) => ({
            ...i,
            priceText: fenToYuan(i.unitPrice),
            subtotalText: fenToYuan(i.subtotal),
          })),
          totalText: fenToYuan(data.totalAmount),
          token: data.token,
          ready: true,
        })
      })
      .catch(() => this.setData({ ready: false }))
  },

  onInputRemark(e) {
    this.setData({ remark: e.detail.value })
  },

  onTapRefresh() {
    this.refreshPreview()
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
      })
      .then((order) => {
        this.setData({ submitting: false })
        wx.showModal({
          title: '下单成功',
          content: '单号 ' + order.orderNo + '\n实付 ¥' + fenToYuan(order.payAmount),
          showCancel: false,
          confirmText: '回首页',
          confirmColor: '#ff6b35',
          success: () => wx.reLaunch({ url: '/pages/index/index' }),
        })
      })
      .catch((err) => {
        this.setData({ submitting: false })
        // 令牌被占用/过期：自动重发令牌，用户可直接再点一次（requestId 不变，已成功的重试走幂等回放）
        if (err && err.message && err.message.indexOf('令牌') >= 0) {
          this.refreshPreview()
        }
      })
  },
})