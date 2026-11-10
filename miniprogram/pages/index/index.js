// 首页（点餐）：搜索 + 分类横滑 + 在售商品流；免登录可浏览，加购需登录
const api = require('../../utils/api')
const { fenToYuan } = require('../../utils/money')

Page({
  data: {
    categories: [],
    activeCategoryId: '',
    keyword: '',
    list: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    token: '',
    cartCount: 0,
    loggingIn: false,
  },

  onLoad() {
    this.loadCategories()
    this.loadPage(true)
  },

  onShow() {
    const token = wx.getStorageSync('token') || ''
    this.setData({ token })
    if (token) this.refreshCartCount()
  },

  refreshCartCount() {
    api
      .cartList()
      .then((d) => this.setData({ cartCount: (d.items || []).reduce((s, i) => s + i.quantity, 0) }))
      .catch(() => {})
  },

  loadCategories() {
    api
      .publicCategories()
      .then((list) => this.setData({ categories: list || [] }))
      .catch(() => {})
  },

  loadPage(reset) {
    if (this.data.loading) return
    const pageNum = reset ? 1 : this.data.pageNum + 1
    // wx.request 会把 undefined 序列化成字面量 "undefined"，条件参数必须按需拼接
    const params = { pageNum, pageSize: 10 }
    if (this.data.keyword) params.keyword = this.data.keyword
    if (this.data.activeCategoryId) params.categoryId = this.data.activeCategoryId
    this.setData({ loading: true })
    api
      .publicSpus(params)
      .then((page) => {
        const rows = (page.records || []).map((spu) => {
          const skus = spu.skus || []
          const minPrice = skus.length ? Math.min(...skus.map((s) => s.price)) : 0
          return {
            spuId: spu.id,
            name: spu.name,
            image: spu.mainImage,
            description: spu.description,
            tagList: (spu.tags || '').split(',').filter(Boolean).slice(0, 3),
            skuId: skus[0]?.id,
            stock: skus.reduce((s, k) => s + (k.stock || 0), 0),
            specCount: skus.length,
            priceText: fenToYuan(minPrice),
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

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearchConfirm() {
    this.loadPage(true)
  },

  onTapCategory(e) {
    const id = e.currentTarget.dataset.id || ''
    if (id === this.data.activeCategoryId) return
    this.setData({ activeCategoryId: id })
    this.loadPage(true)
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
        this.refreshCartCount()
      })
      .catch(() => this.setData({ loggingIn: false }))
  },

  onTapAdd(e) {
    if (!this.data.token) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    const { skuid, stock } = e.currentTarget.dataset
    if (!skuid) return
    if (Number(stock) <= 0) {
      wx.showToast({ title: '已售罄', icon: 'none' })
      return
    }
    api
      .cartAdd(skuid, 1)
      .then(() => {
        this.setData({ cartCount: this.data.cartCount + 1 })
        wx.showToast({ title: '已加入购物车', icon: 'none', duration: 700 })
      })
      .catch(() => {})
  },

  onTapGoods(e) {
    const id = e.currentTarget.dataset.id
    if (id) wx.navigateTo({ url: '/pages/goods-detail/goods-detail?id=' + id })
  },

  onTapCart() {
    if (!this.data.token) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    wx.navigateTo({ url: '/pages/cart/cart' })
  },

  onReachBottom() {
    if (this.data.hasMore) this.loadPage(false)
  },
})