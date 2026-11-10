// 商品详情：主图/信息/规格弹层（加购与立即购买共用）
const api = require('../../utils/api')
const { fenToYuan } = require('../../utils/money')

Page({
  data: {
    spu: null,
    skuList: [],
    selectedSkuId: '',
    loading: true,
    sheet: false,
    buyMode: 'cart', // cart=加入购物车 buy=立即购买
    qty: 1,
    totalText: '0.00',
    selectedPriceText: '0.00',
    selectedStock: 0,
    submitting: false,
  },

  onLoad(query) {
    if (!query.id) {
      wx.showToast({ title: '缺少商品ID', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 600)
      return
    }
    this.spuId = query.id
    this.load()
  },

  load() {
    this.setData({ loading: true })
    api
      .publicSpuDetail(this.spuId)
      .then((spu) => {
        const skuList = (spu.skus || []).map((s) => ({
          ...s,
          priceText: fenToYuan(s.price),
          soldOut: s.stock <= 0,
        }))
        const first = skuList.find((s) => !s.soldOut) || skuList[0]
        this.setData({
          spu: {
            ...spu,
            tagList: (spu.tags || '').split(',').filter(Boolean),
            minPriceText: skuList.length ? fenToYuan(Math.min(...skuList.map((s) => s.price))) : '0.00',
            totalStock: skuList.reduce((sum, s) => sum + (s.stock || 0), 0),
          },
          skuList,
          selectedSkuId: first ? first.id : '',
          loading: false,
        })
        this.recompute()
      })
      .catch(() => this.setData({ loading: false }))
  },

  // 无登录态 → 引导去登录页（登录后返回再点一次）
  ensureLogin() {
    if (wx.getStorageSync('token')) return Promise.resolve(true)
    wx.navigateTo({ url: '/pages/login/login' })
    return Promise.resolve(false)
  },

  selectedSku() {
    return this.data.skuList.find((s) => s.id === this.data.selectedSkuId)
  },

  recompute() {
    const sku = this.selectedSku()
    if (!sku) return
    this.setData({
      selectedPriceText: sku.priceText,
      selectedStock: sku.stock,
      totalText: fenToYuan(sku.price * this.data.qty),
    })
  },

  openSheet(e) {
    const mode = e.currentTarget.dataset.mode || 'cart'
    if (!this.data.skuList.length) {
      wx.showToast({ title: '暂无可售规格', icon: 'none' })
      return
    }
    const selected = this.selectedSku()
    this.setData({
      sheet: true,
      buyMode: mode,
      qty: selected && !selected.soldOut ? Math.min(1, selected.stock) : 1,
    })
    this.recompute()
  },

  closeSheet() {
    this.setData({ sheet: false })
  },

  onPickSku(e) {
    const { id, soldout } = e.currentTarget.dataset
    if (soldout) return
    this.setData({ selectedSkuId: id })
    const sku = this.selectedSku()
    if (sku && this.data.qty > sku.stock) this.setData({ qty: Math.max(1, sku.stock) })
    this.recompute()
  },

  onQtyMinus() {
    if (this.data.qty <= 1) return
    this.setData({ qty: this.data.qty - 1 })
    this.recompute()
  },

  onQtyPlus() {
    const sku = this.selectedSku()
    if (!sku) return
    if (this.data.qty >= sku.stock) {
      wx.showToast({ title: '仅剩 ' + sku.stock + ' 件', icon: 'none' })
      return
    }
    this.setData({ qty: this.data.qty + 1 })
    this.recompute()
  },

  onConfirm() {
    const sku = this.selectedSku()
    if (!sku || sku.soldOut || this.data.submitting) return
    this.setData({ submitting: true })
    this.ensureLogin().then((ok) => {
      if (!ok) {
        this.setData({ submitting: false })
        return
      }
      api
        .cartAdd(sku.id, this.data.qty)
        .then(() => {
          this.setData({ submitting: false, sheet: false })
          if (this.data.buyMode === 'buy') {
            wx.navigateTo({ url: '/pages/checkout/checkout?skuIds=' + sku.id })
            return
          }
          wx.showToast({ title: '已加入购物车', icon: 'none' })
        })
        .catch(() => this.setData({ submitting: false }))
    })
  },

  onTapCart() {
    wx.navigateTo({ url: '/pages/cart/cart' })
  },
})