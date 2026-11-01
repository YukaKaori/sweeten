/**
 * 本地存储键名迁移。
 *
 * 界面偏好类的键统一使用 sw- 前缀；更名前（DessertShop 时期）的旧键在首次读取前迁移过来，
 * 用户已有的设置（外观、⌘K 最近访问、记住的用户名）不会丢失，之后删除旧键。
 *
 * 业务数据的键名属于兼容标识、保持不变：顾客购物车 cart_items、我的订单 my_orders、
 * 结算联系人 checkout_contact、last_order_phone，以及员工登录态 loginUser。
 *
 * @param {string} key 新键名
 * @param {...string} legacyKeys 旧键名
 * @returns {string} 新键名（在模块顶层调用：const KEY = migratedKey('sw-x', 'old-x')）
 */
export function migratedKey(key, ...legacyKeys) {
  try {
    if (localStorage.getItem(key) === null) {
      const legacy = legacyKeys.find((k) => localStorage.getItem(k) !== null)
      if (legacy) localStorage.setItem(key, localStorage.getItem(legacy))
    }
    legacyKeys.forEach((k) => localStorage.removeItem(k))
  } catch {
    // 隐私模式 / 存储不可用时忽略，调用方各自已有降级
  }
  return key
}
