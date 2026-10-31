/**
 * 顾客本机下单记录（H5 无登录体系，订单号 + 手机号保存在本机，用于"我的订单"查询实时状态）
 */
const KEY = 'my_orders'
const MAX = 30

export function loadMyOrders() {
  try {
    const list = JSON.parse(localStorage.getItem(KEY) || '[]')
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

export function saveMyOrder(order) {
  const list = [order, ...loadMyOrders().filter((o) => o.orderNo !== order.orderNo)].slice(0, MAX)
  try {
    localStorage.setItem(KEY, JSON.stringify(list))
    // 会员中心按最近一次下单手机号自动查询
    localStorage.setItem('last_order_phone', order.phone)
  } catch { /* ignore */ }
}
