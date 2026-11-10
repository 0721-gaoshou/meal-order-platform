// 券中心 + 我的券（M3，功能联调版；视觉细化待审核后调整）
const api = require('../../utils/api')
const { fenToYuan } = require('../../utils/money')

const typeText = (c) => {
  if (c.type === 'DISCOUNT') {
    const off = (c.discountRate / 10).toFixed(c.discountRate % 10 === 0 ? 0 : 1)
    return (c.thresholdAmount > 0 ? '满' + fenToYuan(c.thresholdAmount) + '元' : '无门槛') + off + '折'
  }
  return (c.thresholdAmount > 0 ? '满' + fenToYuan(c.thresholdAmount) + '减' : '无门槛减') + fenToYuan(c.discountAmount) + '元'
}
const timeText = (s, e) => (s || '').slice(0, 10) + ' ~ ' + (e || '').slice(0, 10)
const statusText = { UNUSED: '未使用', USED: '已使用', EXPIRED: '已过期' }

Page({
  data: {
    tab: 'center',
    center: [],
    mine: [],
    loading: false,
    receivingId: '',
  },

  onShow() {
    this.loadAll()
  },

  loadAll() {
    this.setData({ loading: true })
    Promise.all([api.couponCenter(), api.myCoupons()])
      .then(([center, mine]) => {
        this.setData({
          center: (center || []).map((c) => ({
            ...c,
            typeText: typeText(c),
            timeText: timeText(c.startTime, c.endTime),
            remainText: c.remain <= 0 ? '已抢光' : '剩 ' + c.remain + '/' + c.totalCount,
          })),
          mine: (mine || []).map((c) => ({
            ...c,
            typeText: typeText(c),
            timeText: timeText(c.couponStartTime, c.couponEndTime),
            statusText: statusText[c.status] || c.status,
          })),
          loading: false,
        })
      })
      .catch(() => this.setData({ loading: false }))
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
  },

  onTapReceive(e) {
    const { id, received } = e.currentTarget.dataset
    if (received || this.data.receivingId) return
    this.setData({ receivingId: id })
    api
      .couponReceive(id)
      .then(() => {
        wx.showToast({ title: '领取成功', icon: 'success' })
        this.setData({ receivingId: '' })
        this.loadAll()
      })
      .catch(() => this.setData({ receivingId: '' }))
  },
})