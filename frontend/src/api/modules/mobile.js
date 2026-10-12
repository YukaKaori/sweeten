import http from '@/utils/http'

const BASE = '/mobile'

/**
 * 获取移动端商品列表
 * @param {string} category - all | cake | bread | drink | dessert | icecream | snack | gift
 */
export const getMobileProducts = (category = 'all') =>
  http.get(`${BASE}/products`, { params: { category } })

/**
 * 获取单个在售商品详情（已下架/不存在 → 404）
 */
export const getMobileProduct = (id) =>
  http.get(`${BASE}/products/${id}`, { silent: true })

/**
 * 获取商品分类列表（含数量）
 */
export const getMobileCategories = () =>
  http.get(`${BASE}/categories`)

/**
 * 移动端提交订单
 * @param {{ customerName: string, phone: string, address: string, items: Array, totalAmount: number }} data
 */
export const createMobileOrder = (data) =>
  http.post(`${BASE}/orders`, data)

/**
 * 顾客查单（订单号 + 下单手机号都匹配才返回）
 */
export const getMobileOrder = (orderNo, phone) =>
  http.get(`${BASE}/orders/${encodeURIComponent(orderNo)}`, { params: { phone }, silent: true })
