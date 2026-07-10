<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { Download, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { exportCSV } from '@/composables/useExport'
import {
  queryReportStatsApi,
  queryReportRevenueApi,
  queryReportCategoryApi,
  queryReportWeekdayApi,
  queryReportRankingApi,
} from '@/api/modules/dashboard'
import PageHeader from '@/components/PageHeader.vue'
import { brand } from '@/utils/brandTokens'
import {
  registerDessertThemes,
  currentChartTheme,
  isDarkTheme,
  glassTooltip,
  revenueLineGradient,
  sliceBorderColor,
} from '@/utils/echartsTheme'
import { useMotionPref } from '@/composables/useMotionPref'

registerDessertThemes(echarts)

// static 档（reduced-motion / 低端）：图表「生长」动画关闭，数据瞬时到位
const { allowMotion } = useMotionPref()

// 日期范围
const dateRange = ref([])
const quickDate = ref('month')
const loadingStats = ref(false)

// 日期参数
const dateParams = computed(() => {
  if (dateRange.value?.length === 2) {
    return { begin: dateRange.value[0], end: dateRange.value[1] }
  }
  return { begin: '', end: '' }
})

// 统计卡片 (API 数据 + 降级)。color 用 CSS var —— DOM 元素直接吃 var()，
// 主题切换时圆点颜色自动跟随，无需 JS 重算
const stats = ref([
  { label: '本月营收', value: '¥128,640', change: '+12.5%', trend: 'up', color: 'var(--rose)' },
  { label: '订单总数', value: '2,846', change: '+8.3%', trend: 'up', color: 'var(--blueberry)' },
  { label: '客单价', value: '¥45.2', change: '-2.1%', trend: 'down', color: 'var(--matcha)' },
  { label: '复购率', value: '34.6%', change: '+5.2%', trend: 'up', color: 'var(--amber)' },
])

// 图表数据 (API 数据 + 降级)
const revenueData = ref({
  xAxis: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
  revenue: [86000, 72000, 95000, 108000, 128000, 115000, 132000, 148000, 125000, 138000, 142000, 0],
  orders: [1800, 1520, 2100, 2350, 2846, 2500, 2800, 3100, 2700, 2950, 3050, 0],
})

// 不再逐扇烘焙 itemStyle.color —— 交给 dessert / dessert-dark 主题调色板，
// 主题切换重建图表时扇区颜色自动换成对应色系（浅玫瑰系 / 暗金系）
const categoryData = ref([
  { value: 38, name: '蛋糕' },
  { value: 25, name: '面包' },
  { value: 18, name: '饮品' },
  { value: 12, name: '甜点' },
  { value: 7, name: '冰淇淋' },
])

const weekdayData = ref({
  xAxis: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  orders: [320, 280, 350, 410, 520, 680, 590],
})

const rankingData = ref({
  names: ['蓝莓芝士蛋糕', '焦糖布丁', '舒芙蕾', '法式可颂', '提拉米苏', '抹茶拿铁', '草莓慕斯蛋糕'],
  sales: [326, 389, 432, 543, 618, 892, 1024],
})

// 图表引用
const revenueChartRef = ref(null)
const categoryChartRef = ref(null)
const weekdayChartRef = ref(null)
const rankingChartRef = ref(null)
const chartInstances = []

const initRevenueChart = () => {
  const chart = echarts.init(revenueChartRef.value, currentChartTheme())
  const dark = isDarkTheme()
  const g = revenueLineGradient(dark)
  const option = {
    animation: allowMotion.value,
    tooltip: { trigger: 'axis', ...glassTooltip(dark, g.dot) },
    grid: { top: 40, right: 20, bottom: 30, left: 60 },
    xAxis: { type: 'category', data: revenueData.value.xAxis },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: (v) => `¥${(v / 1000).toFixed(0)}k` }
    },
    series: [
      {
        name: '营收',
        type: 'line',
        lineStyle: {
          width: 3,
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: g.from },
            { offset: 1, color: g.to }
          ])
        },
        itemStyle: { color: g.dot, borderWidth: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: brand(g.areaKey, 0.25) },
            { offset: 1, color: brand(g.areaKey, 0.02) }
          ])
        },
        data: revenueData.value.revenue
      },
      {
        name: '订单数',
        type: 'bar',
        barWidth: 24,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: brand('blueberry', 0.6) },
            { offset: 1, color: brand('blueberry', 0.1) }
          ])
        },
        data: revenueData.value.orders,
        yAxisIndex: 0
      }
    ]
  }
  chart.setOption(option)
  return chart
}

const initCategoryChart = () => {
  const chart = echarts.init(categoryChartRef.value, currentChartTheme())
  const dark = isDarkTheme()
  const option = {
    animation: allowMotion.value,
    tooltip: { trigger: 'item', ...glassTooltip(dark) },
    legend: { orient: 'vertical', right: 20, top: 'center' },
    series: [{
      name: '甜品分类',
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: sliceBorderColor(dark), borderWidth: 3 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      data: categoryData.value
    }]
  }
  chart.setOption(option)
  return chart
}

const initWeekdayChart = () => {
  const chart = echarts.init(weekdayChartRef.value, currentChartTheme())
  const dark = isDarkTheme()
  // 工作日蓝莓 / 周末强调色（浅玫瑰、暗金）——高峰日一眼可辨
  const weekday = brand('blueberry')
  const weekend = dark ? brand('gold') : brand('rose')
  const option = {
    animation: allowMotion.value,
    tooltip: { trigger: 'axis', ...glassTooltip(dark, weekend) },
    grid: { top: 20, right: 20, bottom: 30, left: 50 },
    xAxis: { type: 'category', data: weekdayData.value.xAxis },
    yAxis: { type: 'value' },
    series: [{
      name: '订单量',
      type: 'bar',
      barWidth: 32,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: (params) => (params.dataIndex >= 4 ? weekend : weekday)
      },
      data: weekdayData.value.orders
    }]
  }
  chart.setOption(option)
  return chart
}

const initRankingChart = () => {
  const chart = echarts.init(rankingChartRef.value, currentChartTheme())
  const dark = isDarkTheme()
  const accentKey = dark ? 'gold' : 'rose'
  const option = {
    animation: allowMotion.value,
    tooltip: { trigger: 'axis', ...glassTooltip(dark) },
    grid: { top: 10, right: 30, bottom: 10, left: 120 },
    xAxis: { type: 'value' },
    yAxis: {
      type: 'category',
      data: rankingData.value.names,
      inverse: true
    },
    series: [{
      name: '销量',
      type: 'bar',
      barWidth: 18,
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: brand(accentKey, 0.3) },
          { offset: 1, color: brand(accentKey) }
        ])
      },
      data: rankingData.value.sales
    }]
  }
  chart.setOption(option)
  return chart
}

// 更新所有图表
const refreshCharts = () => {
  chartInstances.forEach((c) => {
    if (c && !c.isDisposed()) c.dispose()
  })
  chartInstances.length = 0
  chartInstances.push(
    initRevenueChart(),
    initCategoryChart(),
    initWeekdayChart(),
    initRankingChart(),
  )
}

// 分类英文→中文映射
const categoryNameMap = { cake: '蛋糕', bread: '面包', drink: '饮品', dessert: '甜点', icecream: '冰淇淋' }
const cardColors = ['var(--rose)', 'var(--blueberry)', 'var(--matcha)', 'var(--amber)']

// 加载报表数据
const loadReportData = async () => {
  loadingStats.value = true
  try {
    const { begin, end } = dateParams.value
    const [statsRes, revenueRes, categoryRes, weekdayRes, rankingRes] =
      await Promise.allSettled([
        queryReportStatsApi(begin, end),
        queryReportRevenueApi(begin, end),
        queryReportCategoryApi(begin, end),
        queryReportWeekdayApi(begin, end),
        queryReportRankingApi(begin, end, 7),
      ])

    // 统计卡片：后端 {title,value,prefix,suffix,trend,trendType,...} → 前端 {label,value,change,trend,color}
    if (statsRes.status === 'fulfilled' && statsRes.value?.code === 1) {
      const cards = statsRes.value.data
      if (Array.isArray(cards) && cards.length) {
        stats.value = cards.map((c, i) => ({
          label: c.title || '',
          value: `${c.prefix || ''}${Number(c.value || 0).toLocaleString()}${c.suffix || ''}`,
          change: c.trend || '',
          trend: c.trendType || 'up',
          color: cardColors[i] || 'var(--rose)',
        }))
      }
    }

    // 营收趋势：后端 {xAxis,revenue,orders} ✓ 字段一致
    if (revenueRes.status === 'fulfilled' && revenueRes.value?.code === 1) {
      revenueData.value = revenueRes.value.data
    }

    // 分类占比：后端 {category(英文),count,itemStyle} → 前端 {value,name(中文),itemStyle}
    if (categoryRes.status === 'fulfilled' && categoryRes.value?.code === 1) {
      const list = categoryRes.value.data
      if (Array.isArray(list) && list.length) {
        // 丢弃后端下发的 itemStyle —— 扇区颜色统一交给 dessert 主题调色板
        categoryData.value = list.map(item => ({
          value: item.count || item.value || 0,
          name: categoryNameMap[item.category] || item.name || item.category || '未知',
        }))
      }
    }

    // 工作日：后端 {xAxis,orders} ✓ 字段一致
    if (weekdayRes.status === 'fulfilled' && weekdayRes.value?.code === 1) {
      weekdayData.value = weekdayRes.value.data
    }

    // 排行：后端 [{name,sales},...] → 前端 {names:[],sales:[]}
    if (rankingRes.status === 'fulfilled' && rankingRes.value?.code === 1) {
      const list = rankingRes.value.data
      if (Array.isArray(list) && list.length) {
        rankingData.value = {
          names: list.map(item => item.name),
          sales: list.map(item => item.sales),
        }
      }
    }

    refreshCharts()
  } catch {
    // API fail — keep hardcoded fallback data
  } finally {
    loadingStats.value = false
  }
}

const setQuickDate = (type) => {
  quickDate.value = type
  dateRange.value = [] // 清除自定义日期
  // quickDate changes handled via watch in template
  loadReportData()
}

const handleExport = () => {
  const columns = [
    { key: 'label', label: '指标' },
    { key: 'value', label: '当前值' },
    { key: 'change', label: '变化幅度' },
    { key: 'trend', label: '趋势' },
  ]
  exportCSV(stats.value, columns, '数据报表')
}

const handleRefresh = () => {
  ElMessage.success('数据已刷新')
  loadReportData()
}

// 主题切换（<html data-theme>）时销毁重建全部图表 —— init(el, theme) 的主题
// 在创建时烘焙，setOption 换不掉，必须 dispose + 重新 init 才能刷新配色
let themeObserver = null
if (typeof MutationObserver !== 'undefined' && typeof document !== 'undefined') {
  themeObserver = new MutationObserver(() => {
    if (chartInstances.length) refreshCharts()
  })
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme'],
  })
}

onMounted(async () => {
  await nextTick()
  refreshCharts()
  loadReportData()
})

onBeforeUnmount(() => {
  themeObserver?.disconnect()
  chartInstances.forEach(chart => {
    if (chart && !chart.isDisposed()) {
      chart.dispose()
    }
  })
  chartInstances.length = 0
})
</script>

<template>
  <div class="page-container">
    <PageHeader title="数据报表" description="查看营业数据统计与分析">
      <template #actions>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="loadReportData"
        />
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出报表
        </el-button>
      </template>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="stats-grid animate-fade-in-up delay-1">
      <div
        v-for="(stat, index) in stats"
        :key="stat.label"
        class="stat-card glass-panel--hover"
        :style="{ animationDelay: `${(index + 1) * 0.1}s` }"
      >
        <div class="stat-dot" :style="{ background: stat.color }"></div>
        <div class="stat-content">
          <span class="stat-label">{{ stat.label }}</span>
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-change" :class="{ up: stat.trend === 'up', down: stat.trend === 'down' }">
            {{ stat.trend === 'up' ? '↑' : '↓' }} {{ stat.change }}
          </span>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 营收趋势 -->
      <div class="chart-card full glass-panel animate-fade-in-up delay-2">
        <div class="chart-header">
          <h3>营收趋势</h3>
          <div class="chart-actions">
            <el-radio-group v-model="quickDate" size="small" @change="setQuickDate">
              <el-radio-button value="week">本周</el-radio-button>
              <el-radio-button value="month">本月</el-radio-button>
              <el-radio-button value="year">本年</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div ref="revenueChartRef" class="chart-body" style="height: 360px"></div>
      </div>

      <!-- 分类占比 -->
      <div class="chart-card glass-panel animate-fade-in-up delay-3">
        <div class="chart-header">
          <h3>甜品分类占比</h3>
        </div>
        <div ref="categoryChartRef" class="chart-body" style="height: 300px"></div>
      </div>

      <!-- 每周订单 -->
      <div class="chart-card glass-panel animate-fade-in-up delay-3">
        <div class="chart-header">
          <h3>每周订单分布</h3>
        </div>
        <div ref="weekdayChartRef" class="chart-body" style="height: 300px"></div>
      </div>

      <!-- 销量排行 -->
      <div class="chart-card full glass-panel animate-fade-in-up delay-4">
        <div class="chart-header">
          <h3>甜品销量排行</h3>
        </div>
        <div ref="rankingChartRef" class="chart-body" style="height: 320px"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  box-shadow: var(--glass-specular), var(--glass-shadow);
  transition: all var(--transition-spring);
  animation: fadeInUp 0.5s ease forwards;
  opacity: 0;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15) 0%, transparent 60%);
  pointer-events: none;
  z-index: 1;
  border-radius: inherit;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--glass-specular), var(--glass-shadow-hover);
  border-color: rgba(var(--rose-rgb), 0.2);
}

.stat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 8px;
  flex-shrink: 0;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 6px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
  margin-bottom: 6px;
}

.stat-change {
  font-size: 12px;
  font-weight: 500;
}

.stat-change.up {
  color: var(--matcha);
}

.stat-change.down {
  color: #ef6b6b;
}

/* 图表网格 */
.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card {
  padding: 24px;
}

.chart-card.full {
  grid-column: 1 / -1;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.chart-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.chart-body {
  width: 100%;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
