/**
 * Mock 数据降级策略
 * — 当后端 API 不可达时自动使用本地 mock 数据
 *
 * 使用方式：在 main.js 中调用 setupMockFallback()
 */

import {
  mockDashboardStats, mockRevenueChart, mockRanking,
  mockDepts, mockEmps, mockOrders, mockDesserts, mockLogs,
  mockReportStats, mockReportRevenue, mockReportCategory,
  mockReportWeekday, mockReportRanking,
} from './data'

/**
 * 包装为统一响应格式 { code: 1, msg: 'success', data: ... }
 */
const ok = (data) => ({ code: 1, msg: 'success', data })

/**
 * 根据 URL 路径返回对应的 mock 数据
 * @param {string} url
 * @returns {{ code: number, msg: string, data: any } | null}
 */
export function resolveMockData(url) {
  // ---------- 仪表盘 ----------
  if (url.includes('/dashboard/stats'))         return ok(mockDashboardStats)
  if (url.includes('/dashboard/revenue'))       return ok(mockRevenueChart)
  if (url.includes('/dashboard/ranking'))       return ok(mockRanking)
  if (url.includes('/dashboard/stock-alert'))   return ok(mockDashboardStats.stockAlerts)
  if (url.includes('/dashboard/reviews'))       return ok(mockDashboardStats.reviews)
  if (url.includes('/dashboard/campaigns'))     return ok(mockDashboardStats.campaigns)

  // ---------- 报表 ----------
  if (url.includes('/dashboard/report-stats'))    return ok(mockReportStats)
  if (url.includes('/dashboard/report-revenue'))  return ok(mockReportRevenue)
  if (url.includes('/dashboard/report-category')) return ok(mockReportCategory)
  if (url.includes('/dashboard/report-weekday'))  return ok(mockReportWeekday)
  if (url.includes('/dashboard/report-ranking'))  return ok(mockReportRanking)

  // ---------- 部门 ----------
  if (url.includes('/depts') && url.includes('id')) {
    // 按 ID 查询单个部门
    const idMatch = url.match(/\/depts\/(\d+)/)
    if (idMatch) {
      const dept = mockDepts.find(d => d.id === Number(idMatch[1]))
      return dept ? ok(dept) : { code: 0, msg: '部门不存在', data: null }
    }
  }
  if (url.includes('/depts') && !url.includes('id')) return ok(mockDepts)
  // POST/PUT/DELETE depts operations
  if (url.includes('/depts'))                         return ok(null)

  // ---------- 员工 ----------
  if (url.includes('/emps') && url.includes('page')) return ok(mockEmps)
  if (url.includes('/emps') && /\d+/.test(url)) {
    const idMatch = url.match(/\/emps\/(\d+)/)
    if (idMatch) {
      const emp = mockEmps.rows.find(e => e.id === Number(idMatch[1]))
      return emp ? ok(emp) : { code: 0, msg: '员工不存在', data: null }
    }
  }
  if (url.includes('/emps')) return ok(mockEmps)

  // ---------- 订单 ----------
  if (url.includes('/orders') && url.includes('page')) return ok(mockOrders)
  if (url.includes('/orders') && /\d+/.test(url)) {
    const idMatch = url.match(/\/orders\/(\d+)/)
    if (idMatch) {
      const order = mockOrders.rows.find(o => o.id === Number(idMatch[1]))
      return order ? ok(order) : { code: 0, msg: '订单不存在', data: null }
    }
  }
  if (url.includes('/orders')) return ok(mockOrders)

  // ---------- 甜品 ----------
  if (url.includes('/desserts/category-count')) {
    const counts = Object.entries(mockDesserts).map(([cat, items]) => ({
      category: cat, count: items.length,
    }))
    return ok(counts)
  }
  if (url.includes('/desserts/category')) {
    for (const [cat, items] of Object.entries(mockDesserts)) {
      if (url.includes(cat)) return ok(items)
    }
  }
  if (url.includes('/desserts/status')) return ok(null)
  if (url.includes('/desserts') && /\d+/.test(url)) {
    // 查找任何分类中的甜品
    for (const items of Object.values(mockDesserts)) {
      const idMatch = url.match(/\/desserts\/(\d+)/)
      if (idMatch) {
        const dessert = items.find(d => d.id === Number(idMatch[1]))
        if (dessert) return ok(dessert)
      }
    }
  }
  if (url.includes('/desserts')) return ok(mockDesserts.cake)

  // ---------- 日志 ----------
  if (url.includes('/log/operate') && /\d+/.test(url)) {
    const idMatch = url.match(/\/(\d+)/)
    if (idMatch) {
      const log = mockLogs.rows.find(l => l.id === Number(idMatch[1]))
      return log ? ok(log) : { code: 0, msg: '日志不存在', data: null }
    }
  }
  if (url.includes('/log/operate')) return ok(mockLogs)

  // ---------- 登录 ----------
  if (url.includes('/login')) {
    return ok({ token: 'mock-token-123456', username: 'admin', name: '系统管理员' })
  }

  return null
}
