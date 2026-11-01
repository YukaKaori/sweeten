import http from '@/utils/http'

// 工作台 · 会员管理（需员工登录，见后端 MemberController）。
// 顾客自己的会员中心接口（/mobile/member/**）在 api/modules/storefront.js。

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
