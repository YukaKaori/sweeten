<script setup>
/**
 * 数据报表 —— 只展示后端真实数据；接口失败时显示失败状态，不用演示数据填充。
 *
 * 口径（与后端 DashboardServiceImpl / OrderMapper 一致）：
 *   营收、每日订单    只统计「配送中 + 已完成」
 *   订单总数 / 完成率 期间内全部下单
 *   星期分布          期间内全部下单，按下单星期统计
 *   商品分类构成      按商品款数统计（不是销售额），不随日期变化
 *   销量排行          在售商品累计销量，不随日期变化
 */
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Download, Refresh } from '@element-plus/icons-vue'
import { exportCSV } from '@/composables/useExport'
import {
  queryReportStatsApi,
  queryReportRevenueApi,
  queryReportCategoryApi,
  queryReportWeekdayApi,
  queryReportRankingApi,
} from '@/api/modules/dashboard'
import { brand } from '@/utils/brandTokens'
import { registerDessertThemes, currentChartTheme, isDarkTheme, glassTooltip, sliceBorderColor } from '@/utils/echartsTheme'
import { useMotionPref } from '@/composables/useMotionPref'
import { formatMoney, formatCount } from '@/utils/format'
import { DESSERT_CATEGORIES } from '@/utils/constants'
import PageHeader from '@/components/PageHeader.vue'

registerDessertThemes(echarts)
const { allowMotion } = useMotionPref()

// ---------------- 日期范围 ----------------
const pad = (n) => String(n).padStart(2, '0')
const ymd = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

const quick = ref('month')
const customRange = ref([])

const range = computed(() => {
  if (customRange.value?.length === 2) return { begin: customRange.value[0], end: customRange.value[1] }
  const now = new Date()
  const start = new Date(now)
  if (quick.value === 'week') start.setDate(now.getDate() - ((now.getDay() + 6) % 7))
  else if (quick.value === 'year') start.setMonth(0, 1)
  else start.setDate(1)
  return { begin: ymd(start), end: ymd(now) }
})

const rangeLabel = computed(() => `${range.value.begin} 至 ${range.value.end}`)

// ---------------- 数据 ----------------
const loading = ref(false)
const errors = ref({})
const stats = ref([])
const revenue = ref({ xAxis: [], revenue: [], orders: [] })
const categories = ref([])
const weekday = ref({ xAxis: [], orders: [] })
const ranking = ref([])

const STAT_NOTES = {
  营收总额: '配送中 + 已完成',
  订单总数: '期间内全部下单',
  客单价: '营收 ÷ 订单总数',
  完成率: '已完成 ÷ 订单总数',
}

const categoryLabel = (v) => DESSERT_CATEGORIES.find((c) => c.value === v)?.label || v || '未分类'

/** 数字 + 单位：「23 单」「85.5%」 */
function formatStatNumber(card) {
  const num = Number(card.value || 0).toLocaleString('zh-CN', { maximumFractionDigits: card.decimals ?? 0 })
  const unit = (card.suffix || '').trim()
  return unit === '%' || !unit ? `${num}${unit}` : `${num} ${unit}`
}

/** 后端把结束日期按「次日 0 点」处理，日序列会多出结束日之后的一天，这里去掉 */
function trimAfterEnd(data, end) {
  const d = new Date(`${end}T00:00:00`)
  d.setDate(d.getDate() + 1)
  const extra = `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  const xs = data.xAxis || []
  if (xs.length && xs[xs.length - 1] === extra) {
    return { xAxis: xs.slice(0, -1), revenue: (data.revenue || []).slice(0, -1), orders: (data.orders || []).slice(0, -1) }
  }
  return data
}

async function loadReportData() {
  loading.value = true
  const { begin, end } = range.value
  const [s, r, c, w, k] = await Promise.allSettled([
    queryReportStatsApi(begin, end),
    queryReportRevenueApi(begin, end),
    queryReportCategoryApi(begin, end),
    queryReportWeekdayApi(begin, end),
    queryReportRankingApi(begin, end, 7),
  ])
  const ok = (res) => res.status === 'fulfilled' && res.value?.code === 1
  const nextErrors = {}

  if (ok(s)) {
    stats.value = (s.value.data || []).map((card) => ({
      label: card.title,
      value: card.prefix === '¥'
        ? formatMoney(card.value)
        : formatStatNumber(card),
      note: STAT_NOTES[card.title] || '',
    }))
  } else nextErrors.stats = true

  if (ok(r)) revenue.value = trimAfterEnd(r.value.data || { xAxis: [], revenue: [], orders: [] }, end)
  else nextErrors.revenue = true

  if (ok(c)) {
    categories.value = (c.value.data || []).map((item) => ({ name: categoryLabel(item.category), value: Number(item.count) || 0 }))
  } else nextErrors.category = true

  if (ok(w)) weekday.value = w.value.data || { xAxis: [], orders: [] }
  else nextErrors.weekday = true

  if (ok(k)) ranking.value = k.value.data || []
  else nextErrors.ranking = true

  errors.value = nextErrors
  loading.value = false
  await nextTick()
  renderCharts()
}

function pickQuick(v) {
  quick.value = v
  customRange.value = []
  loadReportData()
}

function onCustomRange(v) {
  if (v?.length === 2) quick.value = ''
  else if (!quick.value) quick.value = 'month'
  loadReportData()
}

const revenueTotal = computed(() => revenue.value.revenue.reduce((s, v) => s + (Number(v) || 0), 0))
const hasRevenue = computed(() => revenueTotal.value > 0)
const weekdayTotal = computed(() => weekday.value.orders.reduce((s, v) => s + (Number(v) || 0), 0))

// ---------------- 图表 ----------------
const revenueEl = ref(null)
const categoryEl = ref(null)
const weekdayEl = ref(null)
const rankingEl = ref(null)
const charts = []

function make(el, option) {
  if (!el) return
  const chart = echarts.init(el, currentChartTheme())
  chart.setOption({ animation: allowMotion.value, ...option })
  charts.push(chart)
}

function disposeCharts() {
  charts.splice(0).forEach((c) => c && !c.isDisposed() && c.dispose())
}

function renderCharts() {
  disposeCharts()
  const dark = isDarkTheme()
  const tip = glassTooltip(dark)

  make(revenueEl.value, {
    tooltip: {
      trigger: 'axis',
      ...tip,
      formatter: (ps) => `<div style="font-weight:600;margin-bottom:4px">${ps[0]?.axisValue ?? ''}</div>`
        + ps.map((p) => `${p.marker}${p.seriesName} ${p.seriesName === '营收' ? formatMoney(p.value) : `${p.value} 单`}`).join('<br/>'),
    },
    legend: { top: 0, right: 0, data: ['营收', '已配送 / 完成订单'] },
    grid: { top: 36, right: 8, bottom: 8, left: 8, containLabel: true },
    xAxis: { type: 'category', data: revenue.value.xAxis, axisLabel: { hideOverlap: true } },
    yAxis: [
      { type: 'value', axisLabel: { formatter: (v) => (v >= 1000 ? `${v / 1000}k` : v) } },
      { type: 'value', minInterval: 1, splitLine: { show: false } },
    ],
    series: [
      {
        name: '营收',
        type: 'line',
        showSymbol: revenue.value.xAxis.length <= 31,
        symbolSize: 5,
        lineStyle: { width: 2, color: brand('strawberry') },
        itemStyle: { color: brand('strawberry') },
        areaStyle: { color: brand('strawberry', dark ? 0.16 : 0.12) },
        data: revenue.value.revenue,
      },
      {
        name: '已配送 / 完成订单',
        type: 'bar',
        yAxisIndex: 1,
        barMaxWidth: 14,
        itemStyle: { color: brand('cocoa', dark ? 0.5 : 0.28), borderRadius: [3, 3, 0, 0] },
        data: revenue.value.orders,
      },
    ],
  })

  make(categoryEl.value, {
    tooltip: { trigger: 'item', ...tip, formatter: (p) => `${p.name}：${p.value} 款（${p.percent}%）` },
    legend: { orient: 'vertical', right: 8, top: 'center', itemWidth: 10, itemHeight: 10 },
    series: [{
      type: 'pie',
      radius: ['48%', '74%'],
      center: ['36%', '50%'],
      itemStyle: { borderColor: sliceBorderColor(dark), borderWidth: 2, borderRadius: 4 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 13, fontWeight: 600 } },
      data: categories.value,
    }],
  })

  make(weekdayEl.value, {
    tooltip: { trigger: 'axis', ...tip, formatter: (ps) => `${ps[0].axisValue}：${ps[0].value} 单` },
    grid: { top: 16, right: 8, bottom: 8, left: 8, containLabel: true },
    xAxis: { type: 'category', data: weekday.value.xAxis },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      barMaxWidth: 28,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: (p) => (p.dataIndex >= 5 ? brand('strawberry') : brand('milktea')),
      },
      data: weekday.value.orders,
    }],
  })

  make(rankingEl.value, {
    tooltip: { trigger: 'axis', ...tip, formatter: (ps) => `${ps[0].name}：累计 ${formatCount(ps[0].value)} 份` },
    grid: { top: 8, right: 40, bottom: 8, left: 8, containLabel: true },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', inverse: true, data: ranking.value.map((r) => r.name) },
    series: [{
      type: 'bar',
      barMaxWidth: 16,
      label: { show: true, position: 'right', color: dark ? '#cfc4b8' : '#6b5d55', fontSize: 11 },
      itemStyle: { borderRadius: [0, 4, 4, 0], color: (p) => (p.dataIndex < 3 ? brand('strawberry') : brand('milktea')) },
      data: ranking.value.map((r) => Number(r.sales) || 0),
    }],
  })
}

const resize = () => charts.forEach((c) => c.resize())

// 深浅外观切换：主题在 init 时烘焙，需要重建
let themeObserver = null

onMounted(() => {
  loadReportData()
  window.addEventListener('resize', resize)
  themeObserver = new MutationObserver(() => charts.length && renderCharts())
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  themeObserver?.disconnect()
  disposeCharts()
})

function handleExport() {
  const rows = revenue.value.xAxis.map((d, i) => ({ date: d, revenue: revenue.value.revenue[i], orders: revenue.value.orders[i] }))
  exportCSV(
    [...stats.value.map((s) => ({ date: s.label, revenue: s.value, orders: s.note })), ...rows],
    [
      { key: 'date', label: `日期 / 指标（${rangeLabel.value}）` },
      { key: 'revenue', label: '营收 / 数值' },
      { key: 'orders', label: '已配送或完成订单 / 口径' },
    ],
    '喜甜数据报表'
  )
}
</script>

<template>
  <div class="page-container">
    <PageHeader
      title="数据报表"
      :description="`统计区间：${rangeLabel}`"
    >
      <template #actions>
        <el-radio-group
          :model-value="quick"
          size="default"
          aria-label="快捷区间"
          @change="pickQuick"
        >
          <el-radio-button value="week">本周</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
          <el-radio-button value="year">本年</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-model="customRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 250px"
          @change="onCustomRange"
        />
        <el-button
          :loading="loading"
          @click="loadReportData"
        >
          <el-icon v-if="!loading"><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button
          :disabled="!stats.length"
          @click="handleExport"
        >
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </template>
    </PageHeader>

    <!-- 指标 -->
    <section class="stats">
      <p
        v-if="errors.stats"
        class="state surface"
      >指标加载失败，请刷新重试</p>
      <template v-else>
        <article
          v-for="s in stats"
          :key="s.label"
          class="stat surface"
        >
          <p class="stat__label">{{ s.label }}</p>
          <p class="stat__value sw-num">{{ s.value }}</p>
          <p class="stat__note">{{ s.note }}</p>
        </article>
        <template v-if="loading && !stats.length">
          <article
            v-for="i in 4"
            :key="i"
            class="stat surface"
          ><span class="skeleton-shimmer sk"></span></article>
        </template>
      </template>
    </section>

    <div class="charts">
      <section class="card surface full">
        <header class="card__head">
          <div>
            <h2 class="card__title">营收趋势</h2>
            <p class="card__desc">区间营收 <strong class="sw-num">{{ formatMoney(revenueTotal) }}</strong> · 只统计配送中与已完成的订单</p>
          </div>
        </header>
        <p
          v-if="errors.revenue"
          class="state"
        >营收趋势加载失败</p>
        <div
          v-else
          class="chart-wrap"
        >
          <div
            ref="revenueEl"
            class="chart"
            style="height: 340px"
            role="img"
            aria-label="营收与订单趋势图"
          ></div>
          <p
            v-if="!loading && !hasRevenue"
            class="chart-empty"
          >这段时间还没有已配送或已完成的订单</p>
        </div>
      </section>

      <section class="card surface">
        <header class="card__head">
          <div>
            <h2 class="card__title">商品分类构成</h2>
            <p class="card__desc">按商品款数统计，含已下架商品，不随日期变化</p>
          </div>
        </header>
        <p
          v-if="errors.category"
          class="state"
        >分类数据加载失败</p>
        <div
          v-else
          ref="categoryEl"
          class="chart"
          style="height: 280px"
          role="img"
          aria-label="商品分类构成环形图"
        ></div>
      </section>

      <section class="card surface">
        <header class="card__head">
          <div>
            <h2 class="card__title">下单时间分布</h2>
            <p class="card__desc">区间内全部下单 <strong class="sw-num">{{ weekdayTotal }}</strong> 笔，按星期统计（周末以草莓色标出）</p>
          </div>
        </header>
        <p
          v-if="errors.weekday"
          class="state"
        >分布数据加载失败</p>
        <div
          v-else
          ref="weekdayEl"
          class="chart"
          style="height: 280px"
          role="img"
          aria-label="按星期的下单分布柱状图"
        ></div>
      </section>

      <section class="card surface full">
        <header class="card__head">
          <div>
            <h2 class="card__title">销量排行</h2>
            <p class="card__desc">在售甜品累计销量 Top 7，不随日期变化</p>
          </div>
        </header>
        <p
          v-if="errors.ranking"
          class="state"
        >排行数据加载失败</p>
        <div
          v-else
          ref="rankingEl"
          class="chart"
          style="height: 300px"
          role="img"
          aria-label="甜品累计销量排行条形图"
        ></div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat {
  padding: 18px 20px;
  min-height: 112px;
}

.stat__label {
  font-size: 13px;
  color: var(--sw-ink-3);
}

.stat__value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
}

.stat__note {
  margin-top: 6px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.sk {
  display: block;
  height: 72px;
}

.charts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.card {
  padding: 20px;
  min-width: 0;
}

.card.full {
  grid-column: 1 / -1;
}

.card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.card__title {
  font-size: 16px;
  font-weight: 600;
}

.card__desc {
  margin-top: 2px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.card__desc strong {
  color: var(--sw-ink);
}

.chart-wrap {
  position: relative;
}

.chart-empty {
  position: absolute;
  left: 50%;
  top: 45%;
  transform: translate(-50%, -50%);
  padding: 8px 14px;
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-pill);
  background: var(--sw-surface);
  font-size: 13px;
  color: var(--sw-ink-3);
  white-space: nowrap;
}

.state {
  grid-column: 1 / -1;
  padding: 40px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--sw-ink-3);
}

@media (max-width: 1100px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .charts {
    grid-template-columns: 1fr;
  }
}
</style>
