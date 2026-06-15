import http from '@/utils/http'

const BASE = '/mobile/member'

/**
 * 查询会员完整信息（含积分、等级、优惠券）
 */
export const getMemberProfile = (phone) =>
  http.get(`${BASE}/profile`, { params: { phone } })

/**
 * 获取会员可用优惠券列表
 */
export const getMemberCoupons = (memberId) =>
  http.get(`${BASE}/coupons`, { params: { memberId } })

/**
 * 获取积分日志
 */
export const getPointsLog = (memberId) =>
  http.get(`${BASE}/points-log`, { params: { memberId } })

/**
 * 积分兑换优惠券
 */
export const redeemCoupon = (memberId, templateId) =>
  http.post(`${BASE}/redeem`, { memberId, templateId })

/**
 * 获取可兑换优惠券列表
 */
export const getExchangeList = () =>
  http.get(`${BASE}/exchange-list`)
