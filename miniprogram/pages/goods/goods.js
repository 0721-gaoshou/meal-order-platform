// 商品列表页（占位：复用管理端分页接口，多规格弹层与 C 端接口 M4 补）
const api = require('../../utils/api')
const { fenToYuan } = require('../../utils/money')

Page({
  data: {
    list: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    addedSkuId: '',
  },

  onLoad() {
    if (!wx.getStorageSync('token')) {
      wx.showToast({ title: '请先在首页登录', icon: 'none' })
    }
    this.loadPage(true)
  },

  loadPage(reset) {
    if (this.data.loading) return
    const pageNum = reset ? 1 : this.data.pageNum + 1
    this.setData({ loading: true })
    api
      .spuPage({ status: 'ON_SALE', pageNum, pageSize: 20 })
      .then((page) => {
        const rows = (page.records || []).map((spu) => {
          const sku = (spu.skus || [])[0] || {}
          return {
            spuId: spu.id,
            name: spu.name,
            image: spu.mainImage,
            skuId: sku.id,
            spec: sku.spec,
            priceText: fenToYuan(sku.price),
            stock: sku.stock,
            moreSpec: (spu.skus || []).length > 1 ? '等' + spu.skus.length + '种规格' : '',
          }
        })
        this.setData({
          list: reset ? rows : this.data.list.concat(rows),
          pageNum,
          hasMore: page.current < page.pages,
          loading: false,
        })
      })
      .catch(() => this.setData({ loading: false }))
  },

  onTapAdd(e) {
    const { skuid, stock } = e.currentTarget.dataset
    if (!skuid) return
    if (Number(stock) <= 0) {
      wx.showToast({ title: '库存不足', icon: 'none' })
      return
    }
    api
      .cartAdd(skuid, 1)
      .then(() => {
        // 加购成功才给按钮一次弹性反馈
        this.setData({ addedSkuId: skuid })
        setTimeout(() => this.setData({ addedSkuId: '' }), 260)
        wx.showToast({ title: '已加入购物车', icon: 'none', duration: 800 })
      })
      .catch(() => {})
  },

  onTapGoCart() {
    wx.navigateTo({ url: '/pages/cart/cart' })
  },

  onReachBottom() {
    if (this.data.hasMore) this.loadPage(false)
  },
})