// 购物车页
// 交互：全选错峰弹性打勾（状态即时、视觉逐行 70ms 跟进）/ 左滑删除（曲线两阶段飞出后才移除数据）/ 数量步进弹性
// 原则：所有数量变更以服务端返回为准，失败即重拉列表，本地不造数
const api = require('../../utils/api')
const { fenToYuan } = require('../../utils/money')

const SWIPE_OPEN = -70
const SWIPE_THRESHOLD = -60
const STAGGER_MS = 70

Page({
  data: {
    items: [],
    allSelected: false,
    selCount: 0,
    selAmountText: '0.00',
    loading: true,
  },

  onShow() {
    this.load()
  },

  load() {
    this.setData({ loading: true })
    api
      .cartList()
      .then((data) => {
        const items = (data.items || []).map((i) => ({
          skuId: i.skuId,
          spuId: i.spuId,
          spuName: i.spuName,
          spec: i.spec,
          mainImage: i.mainImage,
          price: i.price,
          priceText: fenToYuan(i.price),
          quantity: i.quantity,
          stock: i.stock,
          valid: !!i.valid,
          selected: !!i.valid,
          vChecked: !!i.valid,
          tx: 0,
          open: false,
          snap: false,
          flyPhase: '',
          pop: false,
        }))
        this.setData({ items, loading: false })
        this.recompute()
      })
      .catch(() => this.setData({ loading: false }))
  },

  recompute() {
    const items = this.data.items
    const valid = items.filter((i) => i.valid)
    const selected = valid.filter((i) => i.selected)
    this.setData({
      selCount: selected.length,
      selAmountText: fenToYuan(selected.reduce((s, i) => s + i.price * i.quantity, 0)),
      allSelected: valid.length > 0 && selected.length === valid.length,
    })
  },

  // ------- 批量勾选：主交互（状态即时更新，视觉错峰） -------
  onTapAll() {
    const target = !this.data.allSelected
    const items = this.data.items
    // 真实选中状态一批 setData，合计与按钮立即生效
    const patch = {}
    items.forEach((i, idx) => {
      if (i.valid) patch['items[' + idx + '].selected'] = target
    })
    this.setData(patch)
    this.recompute()
    // 视觉勾选从第一行开始逐个弹
    let k = 0
    items.forEach((i, idx) => {
      if (!i.valid) return
      setTimeout(() => {
        this.setData({ ['items[' + idx + '].vChecked']: target })
      }, STAGGER_MS * k)
      k += 1
    })
  },

  onTapCheck(e) {
    const idx = e.currentTarget.dataset.index
    const item = this.data.items[idx]
    if (!item.valid) {
      wx.showToast({ title: '失效商品不能结算，请删除', icon: 'none' })
      return
    }
    // 单选：状态与视觉同步（勾选标记自身带 overshoot 弹性）
    this.setData(
      { ['items[' + idx + '].selected']: !item.selected, ['items[' + idx + '].vChecked']: !item.selected },
      () => this.recompute()
    )
  },

  // ------- 数量步进：成功才改本地，数字轻弹一次 -------
  onTapPlus(e) {
    const idx = e.currentTarget.dataset.index
    const item = this.data.items[idx]
    if (item.quantity >= item.stock) {
      wx.showToast({ title: '库存不足，仅剩 ' + item.stock + ' 件', icon: 'none' })
      return
    }
    this.changeQuantity(idx, item.quantity + 1)
  },

  onTapMinus(e) {
    const idx = e.currentTarget.dataset.index
    const item = this.data.items[idx]
    if (item.quantity <= 1) {
      this.removeAt(idx, false)
      return
    }
    this.changeQuantity(idx, item.quantity - 1)
  },

  changeQuantity(idx, quantity) {
    const item = this.data.items[idx]
    api
      .cartUpdateQuantity(item.skuId, quantity)
      .then(() => {
        this.setData({ ['items[' + idx + '].quantity']: quantity, ['items[' + idx + '].pop']: true })
        setTimeout(() => this.setData({ ['items[' + idx + '].pop']: false }), 280)
        this.recompute()
      })
      .catch(() => this.load())
  },

  // ------- 滑动删除：辅助交互（位移跟手 → 松手吸附 → 两阶段曲线飞出 → 才删数据） -------
  onTouchStart(e) {
    const idx = e.currentTarget.dataset.index
    this._touch = {
      idx,
      x0: e.touches[0].clientX,
      y0: e.touches[0].clientY,
      base: this.data.items[idx].open ? SWIPE_OPEN : 0,
      axis: '',
    }
    // 跟手阶段关掉过渡，避免位移被动画拖慢
    this.setData({ ['items[' + idx + '].snap']: false })
  },

  onTouchMove(e) {
    const t = this._touch
    if (!t) return
    const dx = e.touches[0].clientX - t.x0
    const dy = e.touches[0].clientY - t.y0
    if (!t.axis) {
      if (Math.abs(dx) > 8 && Math.abs(dx) > Math.abs(dy)) t.axis = 'x'
      else if (Math.abs(dy) > 8) t.axis = 'y'
    }
    if (t.axis !== 'x') return
    const tx = Math.max(SWIPE_OPEN - 20, Math.min(0, t.base + dx))
    this.setData({ ['items[' + t.idx + '].tx']: tx })
  },

  onTouchEnd() {
    const t = this._touch
    if (!t) return
    const open = this.data.items[t.idx].tx < SWIPE_THRESHOLD
    const patch = {
      ['items[' + t.idx + '].open']: open,
      ['items[' + t.idx + '].tx']: open ? SWIPE_OPEN : 0,
      ['items[' + t.idx + '].snap']: true,
    }
    // 同时只允许一行展开
    this.data.items.forEach((i, idx) => {
      if (idx !== t.idx && i.open) {
        patch['items[' + idx + '].open'] = false
        patch['items[' + idx + '].tx'] = 0
      }
    })
    this.setData(patch)
    this._touch = null
  },

  onTapDelete(e) {
    const idx = e.currentTarget.dataset.index
    this.removeAt(idx, true)
  },

  removeAt(idx, withFly) {
    const item = this.data.items[idx]
    const done = () => {
      const items = this.data.items.slice()
      items.splice(idx, 1)
      this.setData({ items })
      this.recompute()
    }
    api
      .cartRemove(item.skuId)
      .then(() => {
        if (!withFly) {
          done()
          return
        }
        // 两阶段：先上扬缩小，再向右下旋转淡出，形成折线轨迹
        this.setData({ ['items[' + idx + '].flyPhase']: 'a' })
        this.setData({ ['items[' + idx + '].tx']: 0, ['items[' + idx + '].open']: false })
        setTimeout(() => this.setData({ ['items[' + idx + '].flyPhase']: 'b' }), 180)
        setTimeout(done, 430)
      })
      .catch(() => this.load())
  },

  onTapClear() {
    wx.showModal({
      title: '清空购物车',
      content: '确定要清空全部条目吗？',
      confirmColor: '#ff6b35',
      success: (res) => {
        if (!res.confirm) return
        api.cartClear().then(() => this.load()).catch(() => {})
      },
    })
  },

  onTapCheckout() {
    const ids = this.data.items.filter((i) => i.valid && i.selected).map((i) => i.skuId)
    if (!ids.length) {
      wx.showToast({ title: '请先勾选商品', icon: 'none' })
      return
    }
    wx.navigateTo({ url: '/pages/checkout/checkout?skuIds=' + ids.join(',') })
  },
})