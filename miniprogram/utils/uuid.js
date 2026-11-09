// 下单幂等键：requestId 在同一结算页内复用，重试命中后端幂等回放而不是重复下单
function uuid() {
  const s = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'
  return s.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

module.exports = { uuid }