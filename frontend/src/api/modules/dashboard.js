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

// -------------------- 报表 API --------------------

/** 获取报表统计卡片 */
export const queryReportStatsApi = (begin = '', end = '') =>
  http.get(`/dashboard/report-stats?begin=${begin}&end=${end}`)

/** 获取报表营收趋势 */
export const queryReportRevenueApi = (begin = '', end = '') =>
  http.get(`/dashboard/report-revenue?begin=${begin}&end=${end}`)

/** 获取报表分类销售占比 */
export const queryReportCategoryApi = (begin = '', end = '') =>
  http.get(`/dashboard/report-category?begin=${begin}&end=${end}`)

/** 获取报表工作日订单分布 */
export const queryReportWeekdayApi = (begin = '', end = '') =>
  http.get(`/dashboard/report-weekday?begin=${begin}&end=${end}`)

/** 获取报表销售排行 */
export const queryReportRankingApi = (begin = '', end = '', limit = 7) =>
  http.get(`/dashboard/report-ranking?begin=${begin}&end=${end}&limit=${limit}`)
