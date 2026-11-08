const { BASE_URL } = require('../config/index')

/**
 * 统一请求封装：Promise 化、自动携带 token、统一错误提示
 * 响应体约定与后端一致：{ code, message, data }（见 docs/api-convention.md）
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token')
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: Object.assign(
        { 'Content-Type': 'application/json' },
        options.header || {},
        // M1 起自动携带 JWT
        token ? { Authorization: 'Bearer ' + token } : {},
      ),
      success(res) {
        const body = res.data || {}
        if (body.code === '00000') {
          resolve(body.data)
          return
        }
        if (body.code === 'B0001') {
          // 未登录/过期：清凭证；M1 小程序登录页就绪后跳转登录
          wx.removeStorageSync('token')
          wx.showToast({ title: body.message || '登录已过期', icon: 'none' })
          reject(body)
          return
        }
        wx.showToast({ title: body.message || '请求失败', icon: 'none' })
        reject(body)
      },
      fail(err) {
        wx.showToast({ title: '网络异常，请检查后端服务是否启动', icon: 'none' })
        reject(err)
      },
    })
  })
}

module.exports = { request }
