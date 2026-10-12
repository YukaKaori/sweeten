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
export const getMemberCoupons = (memberId, phone) =>
  http.get(`${BASE}/coupons`, { params: { memberId, phone } })

/**
 * 获取积分日志
 */
export const getPointsLog = (memberId, phone) =>
  http.get(`${BASE}/points-log`, { params: { memberId, phone } })

/**
 * 积分兑换优惠券
 */
export const redeemCoupon = (memberId, phone, templateId) =>
  http.post(`${BASE}/redeem`, { memberId, phone, templateId })

/**
 * 获取可兑换优惠券列表
 */
export const getExchangeList = () =>
  http.get(`${BASE}/exchange-list`)
