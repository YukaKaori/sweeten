<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Download, Refresh } from '@element-plus/icons-vue'
import { exportCSV } from '@/composables/useExport'
import PageHeader from '@/components/PageHeader.vue'

// 日期范围
const dateRange = ref([])
const quickDate = ref('month')

// 统计卡片
const stats = ref([
  { label: '本月营收', value: '¥128,640', change: '+12.5%', trend: 'up', color: '#e8637a' },
  { label: '订单总数', value: '2,846', change: '+8.3%', trend: 'up', color: '#6b8cce' },
  { label: '客单价', value: '¥45.2', change: '-2.1%', trend: 'down', color: '#5cb88a' },
  { label: '复购率', value: '34.6%', change: '+5.2%', trend: 'up', color: '#f0a35c' }
])

// 图表引用
const revenueChartRef = ref(null)
const categoryChartRef = ref(null)
const weekdayChartRef = ref(null)
const rankingChartRef = ref(null)
const chartInstances = []

const initRevenueChart = () => {
  const chart = echarts.init(revenueChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ede6e1',
      textStyle: { color: '#2d2327', fontSize: 13 }
    },
    grid: { top: 40, right: 20, bottom: 30, left: 60 },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
      axisLine: { lineStyle: { color: '#ede6e1' } },
      axisLabel: { color: '#a3949b', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f5f0ec', type: 'dashed' } },
      axisLabel: {
        color: '#a3949b',
        fontSize: 12,
        formatter: (v) => `¥${(v / 1000).toFixed(0)}k`
      }
    },
    series: [
      {
        name: '营收',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#e8637a' },
        itemStyle: { color: '#e8637a', borderWidth: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(232, 99, 122, 0.25)' },
            { offset: 1, color: 'rgba(232, 99, 122, 0.02)' }
          ])
        },
        data: [86000, 72000, 95000, 108000, 128000, 115000, 132000, 148000, 125000, 138000, 142000, 0]
      },
      {
        name: '订单数',
        type: 'bar',
        barWidth: 24,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(107, 140, 206, 0.6)' },
            { offset: 1, color: 'rgba(107, 140, 206, 0.1)' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        data: [1800, 1520, 2100, 2350, 2846, 2500, 2800, 3100, 2700, 2950, 3050, 0],
        yAxisIndex: 0
      }
    ]
  }
  chart.setOption(option)
  return chart
}

const initCategoryChart = () => {
  const chart = echarts.init(categoryChartRef.value)
  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ede6e1',
      textStyle: { color: '#2d2327', fontSize: 13 }
    },
    legend: {
      orient: 'vertical',
      right: 20,
      top: 'center',
      textStyle: { color: '#6b5b63', fontSize: 13 }
    },
    series: [{
      name: '甜品分类',
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#2d2327' }
      },
      data: [
        { value: 38, name: '蛋糕', itemStyle: { color: '#e8637a' } },
        { value: 25, name: '面包', itemStyle: { color: '#f0a35c' } },
        { value: 18, name: '饮品', itemStyle: { color: '#6b8cce' } },
        { value: 12, name: '甜点', itemStyle: { color: '#5cb88a' } },
        { value: 7, name: '冰淇淋', itemStyle: { color: '#a78bfa' } }
      ]
    }]
  }
  chart.setOption(option)
  return chart
}

const initWeekdayChart = () => {
  const chart = echarts.init(weekdayChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ede6e1',
      textStyle: { color: '#2d2327', fontSize: 13 }
    },
    grid: { top: 20, right: 20, bottom: 30, left: 50 },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLine: { lineStyle: { color: '#ede6e1' } },
      axisLabel: { color: '#a3949b', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f5f0ec', type: 'dashed' } },
      axisLabel: { color: '#a3949b', fontSize: 12 }
    },
    series: [{
      name: '订单量',
      type: 'bar',
      barWidth: 32,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: (params) => {
          const colors = ['#6b8cce', '#6b8cce', '#6b8cce', '#6b8cce', '#e8637a', '#e8637a', '#e8637a']
          return colors[params.dataIndex]
        }
      },
      data: [320, 280, 350, 410, 520, 680, 590]
    }]
  }
  chart.setOption(option)
  return chart
}

const initRankingChart = () => {
  const chart = echarts.init(rankingChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ede6e1',
      textStyle: { color: '#2d2327', fontSize: 13 }
    },
    grid: { top: 10, right: 30, bottom: 10, left: 120 },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f5f0ec', type: 'dashed' } },
      axisLabel: { color: '#a3949b', fontSize: 12 }
    },
    yAxis: {
      type: 'category',
      data: ['蓝莓芝士蛋糕', '焦糖布丁', '舒芙蕾', '法式可颂', '提拉米苏', '抹茶拿铁', '草莓慕斯蛋糕'],
      axisLine: { lineStyle: { color: '#ede6e1' } },
      axisLabel: { color: '#6b5b63', fontSize: 12 },
      inverse: true
    },
    series: [{
      name: '销量',
      type: 'bar',
      barWidth: 18,
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: 'rgba(232, 99, 122, 0.3)' },
          { offset: 1, color: '#e8637a' }
        ])
      },
      data: [326, 389, 432, 543, 618, 892, 1024]
    }]
  }
  chart.setOption(option)
  return chart
}

const setQuickDate = (type) => {
  quickDate.value = type
}

const handleExport = () => {
  const columns = [
    { key: 'label', label: '指标' },
    { key: 'value', label: '当前值' },
    { key: 'change', label: '变化幅度' },
    { key: 'trend', label: '趋势' }
  ]
  exportCSV(stats.value, columns, '数据报表')
}

onMounted(async () => {
  await nextTick()
  chartInstances.push(
    initRevenueChart(),
    initCategoryChart(),
    initWeekdayChart(),
    initRankingChart()
  )
})

onBeforeUnmount(() => {
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
        />
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
  border-color: rgba(232, 99, 122, 0.2);
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
  color: #5cb88a;
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
