// index.js
const api = require('../../utils/api')

Page({
  data: {
    checking: false,
    healthText: '尚未检测',
  },

  // 一键验证：小程序 → 后端 → MySQL 链路是否贯通
  onTapHealth() {
    if (this.data.checking) return
    this.setData({ checking: true, healthText: '检测中…' })
    api
      .health()
      .then((data) => {
        this.setData({
          checking: false,
          healthText: '后端连通：' + data.service + ' · ' + data.time,
        })
      })
      .catch(() => {
        this.setData({
          checking: false,
          healthText: '后端不可达，请先启动 backend 服务',
        })
      })
  },
})
