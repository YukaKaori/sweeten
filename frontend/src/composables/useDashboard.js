import { ref, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { queryStatsApi, queryRevenueChartApi, queryRankingApi } from '@/api/modules/dashboard'

/**
 * 仪表盘数据与图表管理 composable
 * — 封装所有 API 调用、ECharts 生命周期、降级数据
 */
export function useDashboard() {
  // ---------- 加载状态 ----------
  const loading = ref(false)

  // ---------- 统计数据 ----------
  const statsCards = ref([])
  const rankList = ref([])
  const stockAlerts = ref([])
  const reviews = ref([])
  const campaigns = ref([])

  // ---------- 快捷操作（静态配置） ----------
  const quickActions = [
    { title: '发布当季限定单品', desc: '联动BOM计算物料损耗与价格区间', route: '/price', icon: 'Plus', iconBg: 'rgba(232, 99, 122, 0.06)', iconColor: '#e8637a' },
    { title: '冷链配送派送控制台', desc: '实时查看冷藏车在途轨迹与温控曲线', route: '/order', icon: 'Van', iconBg: 'rgba(107, 140, 206, 0.06)', iconColor: '#6b8cce' },
    { title: '导出精细化财务报表', desc: '生成包含原材料波动的毛利润桑基图', route: '/report', icon: 'DataAnalysis', iconBg: 'rgba(92, 184, 138, 0.06)', iconColor: '#5cb88a' },
    { title: '智能配方与物料调控', desc: '查看烘焙配方变动触发的成本变差', route: '/report', icon: 'Document', iconBg: 'rgba(240, 163, 92, 0.06)', iconColor: '#f0a35c' },
  ]

  // ---------- 营收图表 ----------
  const chartRef = ref(null)
  const chartRange = ref('week')
  let chartInstance = null
  let resizeObserver = null

  /** 初始化 ECharts 实例 */
  function initChart() {
    if (!chartRef.value) return
    chartInstance = echarts.init(chartRef.value)
    resizeObserver = new ResizeObserver(() => chartInstance?.resize())
    resizeObserver.observe(chartRef.value)
  }

  /** 销毁 ECharts 实例 */
  function disposeChart() {
    resizeObserver?.disconnect()
    chartInstance?.dispose()
  }

  /** 从 API 加载全部仪表盘数据 */
  async function loadData() {
    loading.value = true
    try {
      const [statsRes, revenueRes, rankingRes] = await Promise.allSettled([
        queryStatsApi(),
        queryRevenueChartApi(chartRange.value),
        queryRankingApi(),
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
        chartInstance.setOption({
          xAxis: { data: d.labels },
          series: [{ data: d.revenue }, { data: d.orders }],
        })
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

  return {
    loading,
    statsCards,
    rankList,
    stockAlerts,
    reviews,
    campaigns,
    quickActions,
    chartRef,
    chartRange,
    initChart,
    disposeChart,
    loadData,
    setChartRange,
    handleReorder,
    sendCompensation,
  }
}
