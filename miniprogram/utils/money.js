// 金额工具：后端金额单位为分，前端展示统一走这里
function fenToYuan(fen) {
  const n = Number(fen || 0)
  return (n / 100).toFixed(2)
}

module.exports = { fenToYuan }