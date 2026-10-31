/**
 * Mock 数据 — 所有 Entity 的模拟数据
 * 用于后端不可用时的降级渲染
 */

// ---------- 仪表盘 ----------
export const mockDashboardStats = {
  statsCards: [
    {
      title: '今日营收',
      desc: '截至当前时刻',
      value: 12860,
      prefix: '¥',
      suffix: '',
      trend: '+14.2%',
      trendType: 'up',
      trendIcon: 'Top',
      icon: 'Wallet',
      iconBg: 'rgba(232, 99, 122, 0.08)',
      iconColor: '#e8637a',
      progress: 82,
      decimals: 0,
    },
    {
      title: '总订单数',
      desc: '今日已完成',
      value: 147,
      prefix: '',
      suffix: '单',
      trend: '+8.5%',
      trendType: 'up',
      trendIcon: 'Top',
      icon: 'ShoppingBag',
      iconBg: 'rgba(107, 140, 206, 0.08)',
      iconColor: '#6b8cce',
      progress: 74,
      decimals: 0,
    },
    {
      title: '活跃会员',
      desc: '近30日有消费记录',
      value: 892,
      prefix: '',
      suffix: '人',
      trend: '+3.2%',
      trendType: 'up',
      trendIcon: 'Top',
      icon: 'UserFilled',
      iconBg: 'rgba(92, 184, 138, 0.08)',
      iconColor: '#5cb88a',
      progress: 91,
      decimals: 0,
    },
    {
      title: '热门甜品',
      desc: '今日销量最高',
      value: 328,
      prefix: '',
      suffix: '份',
      trend: '蓝莓芝士蛋糕',
      trendType: 'neutral',
      trendIcon: 'Dessert',
      icon: 'Dessert',
      iconBg: 'rgba(240, 163, 92, 0.08)',
      iconColor: '#f0a35c',
      progress: 65,
      decimals: 0,
    },
  ],
  stockAlerts: [
    { name: '马达加斯加香草荚', stock: 4, threshold: 20, unit: '支' },
    { name: '法国发酵黄油', stock: 2, threshold: 8, unit: '块' },
    { name: '蓝莓果茸', stock: 3, threshold: 10, unit: 'kg' },
  ],
  campaigns: [
    {
      name: '「初夏覆盆子狂欢节」特别企划',
      subtitle: '精选法式甜品覆盆子慕斯买一赠一，主推外卖高端单人餐',
      status: '进行中',
      statusType: 'danger',
      time: '05.20 - 06.05',
      metrics: { sales: '¥45,280', usage: '78.5%', roi: '3.6x' },
      progress: 82,
    },
    {
      name: '「芒里偷闲」夏日限定芒果班戟首发',
      subtitle: '联合高端冷链骑手，实施特定高价值白领社区的精准投送',
      status: '筹备中',
      statusType: 'warning',
      time: '06.10 - 06.25',
      metrics: { sales: '150k+', usage: '¥8,000', roi: '12.8%' },
      progress: 0,
    },
  ],
  reviews: [
    {
      user: '沈小姐',
      avatarBg: 'linear-gradient(135deg, #e8637a, #f08c9e)',
      rating: 5,
      text: '覆盆子慕斯口感细腻，酸甜平衡得恰到好处，是我在巴黎吃过的味道！',
    },
    {
      user: '王先生',
      avatarBg: 'linear-gradient(135deg, #6b8cce, #8ba3d8)',
      rating: 5,
      text: '冷链配送非常专业，千层蛋糕到货完好无损。包装也很有仪式感。',
    },
    {
      user: '李女士',
      avatarBg: 'linear-gradient(135deg, #5cb88a, #7dc9a3)',
      rating: 4,
      text: '提拉米苏味道很好，分量也足够。如果再能加一张手写卡就更好了。',
    },
    {
      user: '张小姐',
      avatarBg: 'linear-gradient(135deg, #f0a35c, #f5c18a)',
      rating: 5,
      text: '生日当天收到的巧克力熔岩蛋糕太惊艳了，朋友们都赞不绝口！',
    },
  ],
  quickActions: [
    { title: '发布当季限定单品', desc: '联动BOM计算物料损耗与价格区间', route: '/price', icon: 'Plus', iconBg: 'rgba(232, 99, 122, 0.06)', iconColor: '#e8637a' },
    { title: '冷链配送派送控制台', desc: '实时查看冷藏车在途轨迹与温控曲线', route: '/order', icon: 'Van', iconBg: 'rgba(107, 140, 206, 0.06)', iconColor: '#6b8cce' },
    { title: '导出精细化财务报表', desc: '生成包含原材料波动的毛利润桑基图', route: '/report', icon: 'DataAnalysis', iconBg: 'rgba(92, 184, 138, 0.06)', iconColor: '#5cb88a' },
    { title: '智能配方与物料调控', desc: '查看烘焙配方变动触发的成本变差', route: '/report', icon: 'Document', iconBg: 'rgba(240, 163, 92, 0.06)', iconColor: '#f0a35c' },
  ],
}

export const mockRevenueChart = {
  labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  revenue: [8200, 9600, 11200, 8800, 13400, 15800, 12860],
  orders: [68, 82, 95, 72, 108, 131, 106],
}

export const mockRanking = [
  { name: '蓝莓芝士蛋糕', sales: 1024, category: '蛋糕' },
  { name: '焦糖布丁', sales: 892, category: '甜点' },
  { name: '舒芙蕾', sales: 618, category: '甜点' },
  { name: '法式可颂', sales: 543, category: '面包' },
  { name: '提拉米苏', sales: 432, category: '蛋糕' },
]

// ---------- 部门 ----------
export const mockDepts = [
  { id: 1, name: '运营部', updateTime: '2026-06-14 10:30:00' },
  { id: 2, name: '市场部', updateTime: '2026-06-13 15:20:00' },
  { id: 3, name: '研发部', updateTime: '2026-06-12 09:00:00' },
  { id: 4, name: '财务部', updateTime: '2026-06-11 14:45:00' },
  { id: 5, name: '人事部', updateTime: '2026-06-10 11:10:00' },
]

// ---------- 员工 ----------
export const mockEmps = {
  rows: [
    { id: 1, name: '张三', username: 'zhangsan', gender: 1, phone: '13800138001', job: 1, salary: '15000', deptId: 1, deptName: '运营部', entryDate: '2025-01-15', image: '', updateTime: '2026-06-14 09:00:00' },
    { id: 2, name: '李四', username: 'lisi', gender: 2, phone: '13800138002', job: 2, salary: '12000', deptId: 2, deptName: '市场部', entryDate: '2025-03-20', image: '', updateTime: '2026-06-13 16:30:00' },
    { id: 3, name: '王五', username: 'wangwu', gender: 1, phone: '13800138003', job: 3, salary: '5000', deptId: 3, deptName: '研发部', entryDate: '2025-06-01', image: '', updateTime: '2026-06-12 11:00:00' },
    { id: 4, name: '赵六', username: 'zhaoliu', gender: 2, phone: '13800138004', job: 4, salary: '8000', deptId: 1, deptName: '运营部', entryDate: '2025-02-10', image: '', updateTime: '2026-06-11 14:00:00' },
    { id: 5, name: '孙七', username: 'sunqi', gender: 1, phone: '13800138005', job: 5, salary: '10000', deptId: 4, deptName: '财务部', entryDate: '2025-04-05', image: '', updateTime: '2026-06-10 10:00:00' },
  ],
  total: 5,
}

// ---------- 订单 ----------
export const mockOrders = {
  rows: [
    { id: 1, orderNo: 'DS20260614001', customerName: '沈小姐', phone: '13900139001', address: '上海市静安区南京西路1888号', items: '蓝莓芝士蛋糕 ×1, 焦糖布丁 ×2', amount: 178.00, status: 4, createTime: '2026-06-14 10:30:00' },
    { id: 2, orderNo: 'DS20260614002', customerName: '王先生', phone: '13900139002', address: '上海市浦东新区陆家嘴环路1000号', items: '抹茶拿铁 ×2, 舒芙蕾 ×1', amount: 96.00, status: 3, createTime: '2026-06-14 11:15:00' },
    { id: 3, orderNo: 'DS20260614003', customerName: '李女士', phone: '13900139003', address: '上海市徐汇区衡山路88号', items: '提拉米苏 ×1', amount: 68.00, status: 2, createTime: '2026-06-14 12:00:00' },
    { id: 4, orderNo: 'DS20260614004', customerName: '张小姐', phone: '13900139004', address: '上海市长宁区延安西路500号', items: '法式可颂 ×3, 咖啡 ×2', amount: 144.00, status: 1, createTime: '2026-06-14 12:30:00' },
    { id: 5, orderNo: 'DS20260614005', customerName: '刘先生', phone: '13900139005', address: '上海市黄浦区淮海中路300号', items: '巧克力熔岩蛋糕 ×1, 草莓慕斯蛋糕 ×1', amount: 256.00, status: 0, createTime: '2026-06-14 13:00:00' },
    { id: 6, orderNo: 'DS20260614006', customerName: '陈女士', phone: '13900139006', address: '上海市虹口区四川北路1500号', items: '芝士蛋糕 ×2', amount: 136.00, status: 5, createTime: '2026-06-13 18:00:00' },
  ],
  total: 6,
}

// ---------- 甜品 / 价格 ----------
export const mockDesserts = {
  cake: [
    { id: 1, name: '蓝莓芝士蛋糕', category: 'cake', price: 68.00, originalPrice: 88.00, sales: 1024, status: 1, image: '' },
    { id: 2, name: '巧克力熔岩蛋糕', category: 'cake', price: 78.00, originalPrice: 98.00, sales: 756, status: 1, image: '' },
    { id: 3, name: '草莓慕斯蛋糕', category: 'cake', price: 58.00, originalPrice: 72.00, sales: 512, status: 1, image: '' },
    { id: 4, name: '提拉米苏', category: 'cake', price: 48.00, originalPrice: 48.00, sales: 432, status: 1, image: '' },
    { id: 5, name: '红丝绒蛋糕', category: 'cake', price: 72.00, originalPrice: 88.00, sales: 289, status: 0, image: '' },
  ],
  bread: [
    { id: 11, name: '法式可颂', category: 'bread', price: 28.00, originalPrice: 35.00, sales: 543, status: 1, image: '' },
    { id: 12, name: '全麦面包', category: 'bread', price: 22.00, originalPrice: 22.00, sales: 320, status: 1, image: '' },
    { id: 13, name: '肉桂卷', category: 'bread', price: 25.00, originalPrice: 30.00, sales: 198, status: 1, image: '' },
  ],
  drink: [
    { id: 21, name: '抹茶拿铁', category: 'drink', price: 32.00, originalPrice: 38.00, sales: 820, status: 1, image: '' },
    { id: 22, name: '焦糖玛奇朵', category: 'drink', price: 30.00, originalPrice: 30.00, sales: 645, status: 1, image: '' },
  ],
  dessert: [
    { id: 31, name: '焦糖布丁', category: 'dessert', price: 22.00, originalPrice: 28.00, sales: 892, status: 1, image: '' },
    { id: 32, name: '舒芙蕾', category: 'dessert', price: 38.00, originalPrice: 45.00, sales: 618, status: 1, image: '' },
  ],
  icecream: [
    { id: 41, name: '香草冰淇淋', category: 'icecream', price: 18.00, originalPrice: 22.00, sales: 410, status: 1, image: '' },
    { id: 42, name: '草莓冰淇淋', category: 'icecream', price: 20.00, originalPrice: 25.00, sales: 355, status: 1, image: '' },
  ],
}

// ---------- 日志 ----------
export const mockLogs = {
  rows: [
    { id: 1, operateEmpId: 1, className: 'com.itheima.controller.DeptController', methodName: 'delete', methodParams: '{"id": 10}', returnValue: '{"code": 1, "msg": "success"}', costTime: 45, operateTime: '2026-06-14 14:30:00' },
    { id: 2, operateEmpId: 1, className: 'com.itheima.controller.EmpController', methodName: 'insert', methodParams: '{"name": "新员工", "job": 3}', returnValue: '{"code": 1, "msg": "success"}', costTime: 120, operateTime: '2026-06-14 14:00:00' },
    { id: 3, operateEmpId: 2, className: 'com.itheima.controller.DeptController', methodName: 'update', methodParams: '{"id": 3, "name": "技术部"}', returnValue: '{"code": 1, "msg": "success"}', costTime: 38, operateTime: '2026-06-14 13:15:00' },
    { id: 4, operateEmpId: 3, className: 'com.itheima.controller.OrderController', methodName: 'update', methodParams: '{"id": 3, "status": 3}', returnValue: '{"code": 1, "msg": "success"}', costTime: 56, operateTime: '2026-06-14 12:05:00' },
    { id: 5, operateEmpId: 1, className: 'com.itheima.controller.DessertController', methodName: 'update', methodParams: '{"id": 1, "price": 68}', returnValue: '{"code": 1, "msg": "success"}', costTime: 1500, operateTime: '2026-06-14 11:30:00' },
    { id: 6, operateEmpId: 2, className: 'com.itheima.controller.AuthController', methodName: 'login', methodParams: '{"username": "admin"}', returnValue: '{"code": 1}', costTime: 210, operateTime: '2026-06-14 09:00:00' },
  ],
  total: 6,
}

// ---------- 报表 ----------
export const mockReportStats = [
  { title: '本月营收', value: 128640, prefix: '¥', suffix: '', trend: '+12.5%', trendType: 'up' },
  { title: '订单总数', value: 2846, prefix: '', suffix: '', trend: '+8.3%', trendType: 'up' },
  { title: '客单价', value: 45.2, prefix: '¥', suffix: '', trend: '-2.1%', trendType: 'down' },
  { title: '复购率', value: 34.6, prefix: '', suffix: '%', trend: '+5.2%', trendType: 'up' },
]

export const mockReportRevenue = {
  xAxis: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
  revenue: [86000, 72000, 95000, 108000, 128000, 115000, 132000, 148000, 125000, 138000, 142000, 0],
  orders: [1800, 1520, 2100, 2350, 2846, 2500, 2800, 3100, 2700, 2950, 3050, 0],
}

export const mockReportCategory = [
  { category: 'cake', count: 38, itemStyle: { color: '#e8637a' } },
  { category: 'bread', count: 25, itemStyle: { color: '#f0a35c' } },
  { category: 'drink', count: 18, itemStyle: { color: '#6b8cce' } },
  { category: 'dessert', count: 12, itemStyle: { color: '#5cb88a' } },
  { category: 'icecream', count: 7, itemStyle: { color: '#a78bfa' } },
]

export const mockReportWeekday = {
  xAxis: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  orders: [320, 280, 350, 410, 520, 680, 590],
}

export const mockReportRanking = [
  { name: '蓝莓芝士蛋糕', sales: 1024 },
  { name: '焦糖布丁', sales: 892 },
  { name: '抹茶拿铁', sales: 820 },
  { name: '巧克力熔岩蛋糕', sales: 756 },
  { name: '焦糖玛奇朵', sales: 645 },
  { name: '舒芙蕾', sales: 618 },
  { name: '法式可颂', sales: 543 },
]
