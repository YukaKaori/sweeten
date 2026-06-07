import http from '@/utils/http'

/** 获取仪表盘统计数据 */
export const queryStatsApi = () => http.get('/dashboard/stats')

/** 获取营收趋势图表数据 */
export const queryRevenueChartApi = (range = 'month') =>
  http.get(`/dashboard/revenue?range=${range}`)

/** 获取甜品销量排行 */
export const queryRankingApi = (limit = 5) =>
  http.get(`/dashboard/ranking?limit=${limit}`)

/** 获取库存预警 */
export const queryStockAlertApi = () => http.get('/dashboard/stock-alert')

/** 获取客户评价 */
export const queryReviewsApi = (limit = 4) =>
  http.get(`/dashboard/reviews?limit=${limit}`)

/** 获取营销活动 */
export const queryCampaignsApi = () => http.get('/dashboard/campaigns')
