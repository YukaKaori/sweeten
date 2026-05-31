import request from '@/utils/request'

// 获取仪表盘统计数据
export const queryStatsApi = () => request.get('/dashboard/stats')

// 获取营收趋势图表数据
export const queryRevenueChartApi = (range = 'month') =>
  request.get(`/dashboard/revenue?range=${range}`)

// 获取甜品销量排行
export const queryRankingApi = (limit = 5) =>
  request.get(`/dashboard/ranking?limit=${limit}`)

// 获取库存预警
export const queryStockAlertApi = () => request.get('/dashboard/stock-alert')

// 获取客户评价
export const queryReviewsApi = (limit = 4) =>
  request.get(`/dashboard/reviews?limit=${limit}`)

// 获取营销活动
export const queryCampaignsApi = () => request.get('/dashboard/campaigns')
