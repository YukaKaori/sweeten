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

// -------------------- 工作台 · 会员管理（需登录，见后端 MemberController） --------------------

/** 分页查询会员（member:member:list） */
export const queryMembersApi = ({ name = '', phone = '', level = '', page = 1, pageSize = 10 } = {}) =>
  http.get('/members', {
    params: {
      name: name || undefined,
      phone: phone || undefined,
      level: level === '' || level === null ? undefined : level,
      page,
      pageSize,
    },
  })

/** 会员积分流水（member:points:list） */
export const queryMemberPointsLogApi = (id) => http.get(`/members/${id}/points-log`)
