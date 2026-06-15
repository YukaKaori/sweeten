import http from '@/utils/http'

const BASE = '/mobile'

/**
 * 获取移动端商品列表
 * @param {string} category - all | cake | bread | drink | icecream | snack | gift
 */
export const getMobileProducts = (category = 'all') =>
  http.get(`${BASE}/products`, { params: { category } })

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
