import { ref, computed } from 'vue'
import { queryStatsApi, queryRevenueChartApi, queryRankingApi } from '@/api/modules/dashboard'
import { queryPageApi as queryOrdersApi } from '@/api/modules/order'
import { useUserStore } from '@/stores/modules/user'

/**
 * 经营概览数据 —— 全部来自后端真实接口，不使用演示数据
 *
 *   GET /dashboard/stats          本月营收 / 本月订单 / 客单价 / 完成率 + 低库存甜品
 *   GET /dashboard/revenue?range= 每日营收与订单（仅统计「配送中 + 已完成」，与后端 sumRevenue 口径一致）
 *   GET /dashboard/ranking        累计销量排行
 *   GET /orders?status=1|2|3      订单处理队列（需要 order:order:list 权限）
 *
 * 说明：后端 statsCards 中的 trend / progress 字段为固定文案，这里不展示；
 * 环比由近 30 日真实日数据在前端计算。
 */
export function useDashboard() {
  const userStore = useUserStore()
  const canSeeOrders = computed(() => userStore.can('order:order:list'))

  const loading = ref(true)
  const statsError = ref('')
  const month = ref(null) // { revenue, orders, avgOrder, completionRate }
  const stockAlerts = ref([])
  const ranking = ref([])
  const trend = ref({ labels: [], revenue: [], orders: [] }) // 近 30 日
  const trendError = ref('')
  const pipeline = ref({ pending: null, making: null, delivering: null, latest: [] })
  const pipelineError = ref('')

  const cardValue = (cards, title) => cards.find((c) => c.title === title)?.value ?? 0

  const sum = (arr) => arr.reduce((s, v) => s + (Number(v) || 0), 0)

  /** 今日、近 7 日与前 7 日对比（来自近 30 日每日数据） */
  const recent = computed(() => {
    const { revenue, orders } = trend.value
    const n = revenue.length
    if (!n) return null
    const last7 = sum(revenue.slice(-7))
    const prev7 = sum(revenue.slice(-14, -7))
    return {
      todayRevenue: Number(revenue[n - 1]) || 0,
      todayOrders: Number(orders[n - 1]) || 0,
      weekRevenue: last7,
      weekOrders: sum(orders.slice(-7)),
      weekChange: prev7 > 0 ? (last7 - prev7) / prev7 : null,
      prevWeekRevenue: prev7,
    }
  })

  async function loadStats() {
    statsError.value = ''
    try {
      const res = await queryStatsApi()
      if (res?.code && res.data) {
        const cards = res.data.statsCards || []
        month.value = {
          revenue: cardValue(cards, '本月营收'),
          orders: cardValue(cards, '本月订单'),
          avgOrder: cardValue(cards, '客单价'),
          completionRate: cardValue(cards, '完成率'),
        }
        stockAlerts.value = Array.isArray(res.data.stockAlerts) ? res.data.stockAlerts : []
      } else {
        statsError.value = res?.msg || '经营数据加载失败'
      }
    } catch (e) {
      statsError.value = e?.displayMessage || '经营数据加载失败'
    }
  }

  async function loadTrend() {
    trendError.value = ''
    try {
      const res = await queryRevenueChartApi('month')
      if (res?.code && res.data) {
        trend.value = {
          labels: res.data.labels || [],
          revenue: res.data.revenue || [],
          orders: res.data.orders || [],
        }
      } else {
        trendError.value = res?.msg || '营收趋势加载失败'
      }
    } catch (e) {
      trendError.value = e?.displayMessage || '营收趋势加载失败'
    }
  }

  async function loadRanking() {
    try {
      const res = await queryRankingApi(5)
      ranking.value = res?.code && Array.isArray(res.data) ? res.data : []
    } catch {
      ranking.value = []
    }
  }

  async function loadPipeline() {
    if (!canSeeOrders.value) return
    pipelineError.value = ''
    try {
      const silent = { silent: true }
      const [pending, making, delivering] = await Promise.all([
        queryOrdersApi('', '', 1, '', '', 1, 5, silent),
        queryOrdersApi('', '', 2, '', '', 1, 1, silent),
        queryOrdersApi('', '', 3, '', '', 1, 1, silent),
      ])
      pipeline.value = {
        pending: pending?.data?.total ?? 0,
        making: making?.data?.total ?? 0,
        delivering: delivering?.data?.total ?? 0,
        latest: pending?.data?.rows || [],
      }
    } catch (e) {
      pipelineError.value = e?.displayMessage || '订单队列加载失败'
    }
  }

  async function loadAll() {
    loading.value = true
    await Promise.allSettled([loadStats(), loadTrend(), loadRanking(), loadPipeline()])
    loading.value = false
  }

  return {
    loading,
    month,
    statsError,
    stockAlerts,
    ranking,
    trend,
    trendError,
    recent,
    pipeline,
    pipelineError,
    canSeeOrders,
    loadAll,
    loadPipeline,
  }
}
