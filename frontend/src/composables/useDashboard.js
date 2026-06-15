import { ref, computed, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { queryStatsApi, queryRevenueChartApi, queryRankingApi } from '@/api/modules/dashboard'

// ---------- 降级数据（API 不可用时使用） ----------
const FALLBACK = {
  statsCards: [
    { title: '今日营收', desc: '截至当前时刻', value: 12860, prefix: '¥', suffix: '', trend: '+14.2%', trendType: 'up', trendIcon: 'Top', icon: 'Wallet', iconBg: 'rgba(232, 99, 122, 0.08)', iconColor: '#e8637a', progress: 82, decimals: 0 },
    { title: '总订单数', desc: '今日已完成', value: 147, prefix: '', suffix: '单', trend: '+8.5%', trendType: 'up', trendIcon: 'Top', icon: 'ShoppingBag', iconBg: 'rgba(107, 140, 206, 0.08)', iconColor: '#6b8cce', progress: 74, decimals: 0 },
    { title: '活跃会员', desc: '近30日有消费记录', value: 892, prefix: '', suffix: '人', trend: '+3.2%', trendType: 'up', trendIcon: 'Top', icon: 'UserFilled', iconBg: 'rgba(92, 184, 138, 0.08)', iconColor: '#5cb88a', progress: 91, decimals: 0 },
    { title: '热门甜品', desc: '今日销量最高', value: 328, prefix: '', suffix: '份', trend: '蓝莓芝士蛋糕', trendType: 'neutral', trendIcon: 'Dessert', icon: 'Dessert', iconBg: 'rgba(240, 163, 92, 0.08)', iconColor: '#f0a35c', progress: 65, decimals: 0 },
  ],
  rankList: [
    { name: '蓝莓芝士蛋糕', sales: 1024, category: '蛋糕' },
    { name: '焦糖布丁', sales: 892, category: '甜点' },
    { name: '舒芙蕾', sales: 618, category: '甜点' },
    { name: '法式可颂', sales: 543, category: '面包' },
    { name: '提拉米苏', sales: 432, category: '蛋糕' },
  ],
  stockAlerts: [
    { name: '马达加斯加香草荚', stock: 4, threshold: 20, unit: '支' },
    { name: '法国发酵黄油', stock: 2, threshold: 8, unit: '块' },
    { name: '蓝莓果茸', stock: 3, threshold: 10, unit: 'kg' },
  ],
  campaigns: [
    { name: '「初夏覆盆子狂欢节」特别企划', subtitle: '精选法式甜品覆盆子慕斯买一赠一', status: '进行中', statusType: 'danger', time: '05.20 - 06.05', metrics: { sales: '¥45,280', usage: '78.5%', roi: '3.6x' }, progress: 82 },
    { name: '「芒里偷闲」夏日限定芒果班戟首发', subtitle: '联合高端冷链骑手精准投送', status: '筹备中', statusType: 'warning', time: '06.10 - 06.25', metrics: { sales: '150k+', usage: '¥8,000', roi: '12.8%' }, progress: 0 },
  ],
  reviews: [
    { user: '沈小姐', avatarBg: 'linear-gradient(135deg, #e8637a, #f08c9e)', rating: 5, text: '覆盆子慕斯口感细腻，酸甜平衡得恰到好处！' },
    { user: '王先生', avatarBg: 'linear-gradient(135deg, #6b8cce, #8ba3d8)', rating: 5, text: '冷链配送非常专业，千层蛋糕到货完好无损。' },
    { user: '李女士', avatarBg: 'linear-gradient(135deg, #5cb88a, #7dc9a3)', rating: 4, text: '提拉米苏味道很好，分量也足够。' },
    { user: '张小姐', avatarBg: 'linear-gradient(135deg, #f0a35c, #f5c18a)', rating: 5, text: '巧克力熔岩蛋糕太惊艳了，朋友们都赞不绝口！' },
  ],
  salesDataMap: {
    realtime: { xAxis: ['10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00'], revenue: [2100, 4800, 3600, 7200, 9800, 11200, 12860], margins: [62, 60, 65, 68, 64, 63, 64.8] },
    week: { xAxis: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'], revenue: [8200, 9600, 11200, 8800, 13400, 15800, 12860], margins: [58, 60, 62, 61, 65, 66, 64.8] },
    month: { xAxis: ['05-05', '05-10', '05-15', '05-20', '05-25', '05-30', '05-31'], revenue: [92000, 105000, 98000, 124000, 132000, 145000, 156000], margins: [61, 62, 60, 63, 64, 65, 64.8] },
  },
  microKpis: [
    { label: '今日边际毛利额', value: '12,860.00', currency: '¥', subtext: '较昨日 +14.2%', subtextClass: 'up' },
    { label: '冷链配送准时率', value: '99.4', unit: '%', subtext: '极佳运转状态', subtextClass: 'stable' },
    { label: '社媒情感正向率', value: '98.2', unit: '%', subtext: '覆盆子甜度超标', subtextClass: 'highlight' },
  ],
}

/** 构建完整的 ECharts option（双轴：营收 + 利润率） */
function buildChartOption(data) {
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ede6e1',
      borderWidth: 1,
      textStyle: { color: '#2d2327', fontSize: 12 },
      axisPointer: { type: 'line', lineStyle: { color: 'rgba(232, 99, 122, 0.4)', width: 1.5, type: 'dashed' } },
    },
    grid: { top: 30, right: 48, bottom: 24, left: 52 },
    xAxis: {
      type: 'category', data: data.xAxis,
      axisLine: { lineStyle: { color: '#ede6e1' } },
      axisTick: { show: false },
      axisLabel: { color: '#a3949b', fontSize: 11 },
    },
    yAxis: [
      {
        type: 'value', name: '营业额',
        axisLine: { show: false }, axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f5f0ec', type: 'dashed' } },
        axisLabel: { color: '#a3949b', fontSize: 11, formatter: (v) => v >= 1000 ? (v / 1000) + 'k' : v },
      },
      {
        type: 'value', name: '利润率', min: 40, max: 80,
        axisLine: { show: false }, axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { color: '#a3949b', fontSize: 11, formatter: '{value}%' },
      },
    ],
    series: [
      {
        name: '营业额', type: 'line', smooth: true,
        symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 3, color: '#e8637a' },
        itemStyle: { color: '#fff', borderWidth: 2.5, borderColor: '#e8637a' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(232, 99, 122, 0.12)' }, { offset: 1, color: 'rgba(232, 99, 122, 0)' }]) },
        data: data.revenue,
      },
      {
        name: '边际利润率', type: 'line', yAxisIndex: 1, smooth: true,
        symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 2, color: '#5cb88a' },
        itemStyle: { color: '#fff', borderWidth: 2, borderColor: '#5cb88a' },
        data: data.margins,
      },
    ],
  }
}

/**
 * 仪表盘数据与图表管理 composable
 * — 单一数据源：API 优先 → 降级数据 fallback
 * — 视图层只需调用 useDashboard() 并解构使用
 */
export function useDashboard() {
  const loading = ref(false)
  const statsCards = ref([])
  const rankList = ref([])
  const stockAlerts = ref([])
  const reviews = ref([])
  const campaigns = ref([])
  const microKpis = ref(FALLBACK.microKpis)

  // 图表
  const chartRange = ref('week')
  const chartRef = ref(null)
  let chartInstance = null
  let resizeObserver = null

  // 快捷操作（静态配置）
  const quickActions = [
    { title: '发布当季限定单品', desc: '联动BOM计算物料损耗与价格区间', route: '/price', icon: 'Plus', iconBg: 'rgba(232, 99, 122, 0.06)', iconColor: '#e8637a' },
    { title: '冷链配送派送控制台', desc: '实时查看冷藏车在途轨迹与温控曲线', route: '/order', icon: 'Van', iconBg: 'rgba(107, 140, 206, 0.06)', iconColor: '#6b8cce' },
    { title: '导出精细化财务报表', desc: '生成包含原材料波动的毛利润桑基图', route: '/report', icon: 'DataAnalysis', iconBg: 'rgba(92, 184, 138, 0.06)', iconColor: '#5cb88a' },
    { title: '智能配方与物料调控', desc: '查看烘焙配方变动触发的成本变差', route: '/report', icon: 'Document', iconBg: 'rgba(240, 163, 92, 0.06)', iconColor: '#f0a35c' },
  ]

  // 图表底部图例（响应式计算）
  const chartLegend = computed(() => {
    const data = FALLBACK.salesDataMap[chartRange.value]
    const lastRev = data.revenue[data.revenue.length - 1]
    const lastMargin = data.margins[data.margins.length - 1]
    return {
      revenue: `¥${lastRev.toLocaleString()}`,
      margin: `${lastMargin}%`,
      avgOrder: '¥69.20', // 静态参考值
    }
  })

  /** 初始化 ECharts 实例 */
  function initChart() {
    if (!chartRef.value) return
    chartInstance = echarts.init(chartRef.value)
    renderChart()
    resizeObserver = new ResizeObserver(() => chartInstance?.resize())
    resizeObserver.observe(chartRef.value)
  }

  /** 渲染/更新图表 */
  function renderChart() {
    if (!chartInstance) return
    const data = FALLBACK.salesDataMap[chartRange.value]
    chartInstance.setOption(buildChartOption(data), true)
  }

  /** 销毁图表 */
  function disposeChart() {
    resizeObserver?.disconnect()
    chartInstance?.dispose()
    chartInstance = null
  }

  /** 从 API 加载仪表盘数据 */
  async function loadData() {
    loading.value = true
    try {
      const [statsRes, revenueRes, rankingRes] = await Promise.allSettled([
        queryStatsApi(), queryRevenueChartApi(chartRange.value), queryRankingApi(),
      ])
      if (statsRes.status === 'fulfilled' && statsRes.value?.code) {
        const d = statsRes.value.data
        if (d.statsCards?.length) statsCards.value = d.statsCards
        if (d.stockAlerts?.length) stockAlerts.value = d.stockAlerts
        if (d.campaigns?.length) campaigns.value = d.campaigns
        if (d.reviews?.length) reviews.value = d.reviews
      }
      if (revenueRes.status === 'fulfilled' && revenueRes.value?.code && chartInstance) {
        const d = revenueRes.value.data
        chartInstance.setOption({ xAxis: { data: d.labels }, series: [{ data: d.revenue }, { data: d.orders }] })
      }
      if (rankingRes.status === 'fulfilled' && rankingRes.value?.code) {
        rankList.value = rankingRes.value.data
      }
    } finally {
      loading.value = false
    }
  }

  /** 切换图表时间范围 */
  function setChartRange(range) {
    chartRange.value = range
    renderChart()
    loadData()
  }

  /** 补货操作 */
  function handleReorder(itemName) {
    ElMessage.success(`已生成 [${itemName}] 的智能合并补货单`)
  }

  /** 发送补偿券 */
  function sendCompensation(username) {
    ElMessage.success(`已向 [${username}] 派送尝鲜新品免单致谢券`)
  }

  onUnmounted(() => {
    disposeChart()
  })

  return {
    loading,
    statsCards,
    rankList,
    stockAlerts,
    reviews,
    campaigns,
    microKpis,
    quickActions,
    chartRef,
    chartRange,
    chartLegend,
    initChart,
    disposeChart,
    renderChart,
    loadData,
    setChartRange,
    handleReorder,
    sendCompensation,
  }
}
