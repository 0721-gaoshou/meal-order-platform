const { request } = require('./request')

// 健康检查：验证 小程序 → 后端 → MySQL 整条链路（M0 验收项）
function health() {
  return request({ url: '/health' })
}

// 登录（后端接口 M1 实现）
function login(data) {
  return request({ url: '/auth/login', method: 'POST', data })
}

module.exports = { health, login }
