/**
 * API 通用类型定义
 */

/** 后端统一响应结构 */
export interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

/** 分页查询参数 */
export interface PageParams {
  page: number
  pageSize: number
}

/** 分页查询结果 */
export interface PageResult<T = any> {
  rows: T[]
  total: number
}

/** 仪表盘统计卡片 */
export interface StatsCard {
  title: string
  desc: string
  value: number
  prefix: string
  suffix: string
  trend: string
  trendType: 'up' | 'down' | 'neutral'
  trendIcon: string
  icon: string
  iconBg: string
  iconColor: string
  progress: number
  decimals: number
}

/** 库存预警项 */
export interface StockAlert {
  name: string
  stock: number
  threshold: number
  unit: string
}

/** 营销活动 */
export interface Campaign {
  name: string
  subtitle: string
  status: string
  statusType: 'danger' | 'warning' | 'success' | 'info'
  time: string
  metrics: {
    sales: string
    usage: string
    roi: string
  }
  progress: number
}

/** 客户评价 */
export interface Review {
  user: string
  avatarBg: string
  rating: number
  text: string
}

/** 仪表盘统计 */
export interface DashboardStats {
  statsCards: StatsCard[]
  stockAlerts: StockAlert[]
  campaigns: Campaign[]
  reviews: Review[]
}

/** 营收图表数据 */
export interface RevenueChartData {
  labels: string[]
  revenue: number[]
  orders: number[]
}

/** 排行项 */
export interface RankingItem {
  name: string
  sales: number
  category?: string
}

/** 订单状态枚举 */
export type OrderStatus = 0 | 1 | 2 | 3 | 4 | 5

/** 订单 */
export interface Order {
  id: number
  orderNo: string
  customerName: string
  phone: string
  address: string
  items: string
  amount: number
  status: OrderStatus
  createTime: string
}

/** 员工 */
export interface Employee {
  id?: number
  username: string
  name: string
  gender: number | ''
  phone: string
  job: number | ''
  salary: string
  deptId: number | ''
  deptName?: string
  entryDate: string
  image: string
  updateTime?: string
  exprList: WorkExperience[]
}

/** 工作经历 */
export interface WorkExperience {
  company: string
  job: string
  begin: string
  end: string
  exprDate?: [string, string]
}

/** 部门 */
export interface Department {
  id?: number
  name: string
  updateTime?: string
}

/** 操作日志 */
export interface OperateLog {
  id: number
  operateEmpId: number
  className: string
  methodName: string
  methodParams: string
  returnValue: string
  costTime: number
  operateTime: string
}

/** 甜品 */
export interface Dessert {
  id: number
  name: string
  category: string
  price: number
  originalPrice: number
  sales: number
  status: number
  image: string
}

/** 报表统计 */
export interface ReportStat {
  label: string
  value: string
  change: string
  trend: 'up' | 'down'
  color: string
}

/** 报表营收数据 */
export interface ReportRevenue {
  xAxis: string[]
  revenue: number[]
  orders: number[]
}

/** 报表分类数据 */
export interface ReportCategoryItem {
  category: string
  count?: number
  value?: number
  name?: string
  itemStyle?: { color: string }
}
