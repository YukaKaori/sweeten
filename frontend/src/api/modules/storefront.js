import http from '@/utils/http'

/**
 * 顾客商城公开接口（免登录）。
 * 后端路径前缀 /mobile 是早期移动端 H5 留下的对外契约，现服务于全部顾客端（桌面 / 平板 / 手机），保持不变。
 */
const BASE = '/mobile'

// ———————————————————————————————— 菜单与商品

/**
 * 获取在售商品列表（后端已过滤下架商品，"all" 按销量降序）
 * @param {string} category - all | cake | bread | drink | dessert | icecream | snack | gift
 * @param {object} [config] axios 配置，如 { silent: true } 不弹全局错误提示
 */
export const getStorefrontProducts = (category = 'all', config = {}) =>
  http.get(`${BASE}/products`, { params: { category }, ...config })

/**
 * 获取单个在售商品详情（已下架/不存在 → 404）
 */
export const getStorefrontProduct = (id) =>
  http.get(`${BASE}/products/${id}`, { silent: true })

/**
 * 获取商品分类列表（含数量）
 */
export const getStorefrontCategories = () =>
  http.get(`${BASE}/categories`)

// ———————————————————————————————— 结算与查单

/**
 * 提交订单（金额以服务端按商品库价格重算为准，不一致会拒单）
 * @param {{ customerName: string, phone: string, address: string, items: Array, totalAmount: number }} data
 */
export const placeStorefrontOrder = (data) =>
  http.post(`${BASE}/orders`, data)

/**
 * 顾客查单（订单号 + 下单手机号都匹配才返回）
 */
export const lookupStorefrontOrder = (orderNo, phone) =>
  http.get(`${BASE}/orders/${encodeURIComponent(orderNo)}`, { params: { phone }, silent: true })

// ———————————————————————————————— 会员中心（目前凭手机号识别，见后端 StorefrontMemberController）

/**
 * 查询会员完整信息（含积分、等级、优惠券）
 */
export const getMemberProfile = (phone) =>
  http.get(`${BASE}/member/profile`, { params: { phone } })

/**
 * 获取会员可用优惠券列表
 */
export const getMemberCoupons = (memberId, phone) =>
  http.get(`${BASE}/member/coupons`, { params: { memberId, phone } })

/**
 * 获取积分日志
 */
export const getPointsLog = (memberId, phone) =>
  http.get(`${BASE}/member/points-log`, { params: { memberId, phone } })

/**
 * 积分兑换优惠券
 */
export const redeemCoupon = (memberId, phone, templateId) =>
  http.post(`${BASE}/member/redeem`, { memberId, phone, templateId })

/**
 * 获取可兑换优惠券列表
 */
export const getExchangeList = () =>
  http.get(`${BASE}/member/exchange-list`)
