<script setup>
/**
 * 经营概览（工作台首页）
 * 只展示后端真实数据；各区块按当前员工权限显示（订单队列需 order:order:list）。
 */
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { useUserStore } from '@/stores/modules/user'
import { useDashboard } from '@/composables/useDashboard'
import { useMotionPref } from '@/composables/useMotionPref'
import { registerDessertThemes, currentChartTheme, glassTooltip, isDarkTheme } from '@/utils/echartsTheme'
import { brand } from '@/utils/brandTokens'
import { formatMoney, formatCount, formatRelative, greeting } from '@/utils/format'
import { formatOrderItems } from '@/utils/constants'
import SwIcon from '@/components/brand/SwIcon.vue'
import StatusBadge from '@/components/StatusBadge.vue'

registerDessertThemes(echarts)

const router = useRouter()
const userStore = useUserStore()
const { allowMotion } = useMotionPref()
const {
  loading, month, statsError, stockAlerts, ranking, trend, trendError, recent,
  pipeline, pipelineError, canSeeOrders, loadAll, loadPipeline,
} = useDashboard()

const can = (p) => userStore.can(p)
const today = new Date()
const dateText = `${today.getFullYear()}年${today.getMonth() + 1}月${today.getDate()}日 · 星期${'日一二三四五六'[today.getDay()]}`
const hello = `${greeting(today)}，${userStore.displayName || '同事'}`

const pct = (v) => (v == null ? '' : `${v >= 0 ? '+' : ''}${(v * 100).toFixed(1)}%`)

const kpis = computed(() => {
  const r = recent.value
  const m = month.value
  return [
    {
      key: 'today',
      label: '今日营收',
      value: r ? formatMoney(r.todayRevenue) : '—',
      sub: r ? `今日已配送 / 完成 ${r.todayOrders} 单` : '',
    },
    {
      key: 'week',
      label: '近 7 日营收',
      value: r ? formatMoney(r.weekRevenue) : '—',
      sub: r ? (r.weekChange == null ? '前 7 日无营收，暂无对比' : `较前 7 日 ${pct(r.weekChange)}`) : '',
      tone: r?.weekChange == null ? '' : r.weekChange >= 0 ? 'up' : 'down',
    },
    {
      key: 'month',
      label: '本月营收',
      value: m ? formatMoney(m.revenue) : '—',
      sub: m ? `本月下单 ${formatCount(m.orders)} 笔 · 完成率 ${Number(m.completionRate || 0).toFixed(1)}%` : '',
    },
    {
      key: 'avg',
      label: '本月客单价',
      value: m ? formatMoney(m.avgOrder) : '—',
      sub: '本月营收 ÷ 本月下单笔数',
    },
  ]
})

const maxSales = computed(() => Math.max(...ranking.value.map((r) => Number(r.sales) || 0), 1))

const actions = computed(() =>
  [
    { label: '处理订单', desc: '查看待接单与制作中订单', icon: 'receipt', to: { path: '/order', query: { status: 1 } }, permission: 'order:order:list' },
    { label: '上新甜品', desc: '新增商品、上传图片', icon: 'cake', to: { path: '/dessert', query: { action: 'create' } }, permission: 'dessert:dessert:create' },
    { label: '调整价格', desc: '调价与上下架', icon: 'tag', to: '/price', permission: 'dessert:dessert:update' },
    { label: '原料库存', desc: '入库、出库与预警', icon: 'box', to: '/inventory', permission: 'inventory:inventory:list' },
    { label: '数据报表', desc: '营收趋势与销售结构', icon: 'chart', to: '/report', permission: 'report:report:read' },
  ].filter((a) => can(a.permission))
)

// ---------------- 营收趋势图 ----------------
const range = ref('week')
const chartEl = ref(null)
let chart = null
let resizeObserver = null
let themeObserver = null

const chartData = computed(() => {
  const n = range.value === 'week' ? 7 : 30
  const { labels, revenue, orders } = trend.value
  return { labels: labels.slice(-n), revenue: revenue.slice(-n), orders: orders.slice(-n) }
})

const rangeTotal = computed(() => chartData.value.revenue.reduce((s, v) => s + (Number(v) || 0), 0))
const rangeOrders = computed(() => chartData.value.orders.reduce((s, v) => s + (Number(v) || 0), 0))

function renderChart() {
  if (!chartEl.value) return
  if (!chart) {
    chart = echarts.init(chartEl.value, currentChartTheme())
  }
  const dark = isDarkTheme()
  const d = chartData.value
  chart.setOption(
    {
      animation: allowMotion.value,
      animationDuration: 500,
      grid: { top: 28, right: 44, bottom: 8, left: 8, containLabel: true },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow', shadowStyle: { color: dark ? 'rgba(242,236,228,0.05)' : 'rgba(73,59,53,0.05)' } },
        ...glassTooltip(dark),
        formatter: (params) => {
          const rev = params.find((p) => p.seriesName === '营收')
          const ord = params.find((p) => p.seriesName === '订单')
          return `<div style="font-weight:600;margin-bottom:4px">${params[0]?.axisValue ?? ''}</div>`
            + `营收 ${formatMoney(rev?.value ?? 0)}<br/>订单 ${ord?.value ?? 0} 单`
        },
      },
      xAxis: { type: 'category', data: d.labels, axisLabel: { hideOverlap: true } },
      yAxis: [
        { type: 'value', axisLabel: { formatter: (v) => (v >= 1000 ? `${v / 1000}k` : v) } },
        { type: 'value', minInterval: 1, splitLine: { show: false }, axisLabel: { formatter: '{value} 单' } },
      ],
      series: [
        {
          name: '营收',
          type: 'bar',
          barMaxWidth: range.value === 'week' ? 28 : 12,
          itemStyle: { color: brand('strawberry', dark ? 0.85 : 0.75), borderRadius: [5, 5, 0, 0] },
          emphasis: { itemStyle: { color: brand('strawberry') } },
          data: d.revenue,
        },
        {
          name: '订单',
          type: 'line',
          yAxisIndex: 1,
          smooth: false,
          symbolSize: 6,
          lineStyle: { width: 2, color: brand('cocoa') },
          itemStyle: { color: brand('cocoa') },
          data: d.orders,
        },
      ],
    },
    true
  )
}

function disposeChart() {
  chart?.dispose()
  chart = null
}

watch([range, trend], () => nextTick(renderChart), { deep: true })

onMounted(async () => {
  await loadAll()
  await nextTick()
  renderChart()
  if (chartEl.value) {
    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(chartEl.value)
  }
  // 深浅外观切换时按新主题重建图表
  themeObserver = new MutationObserver(() => {
    disposeChart()
    nextTick(renderChart)
  })
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  themeObserver?.disconnect()
  disposeChart()
})

const openOrder = () => router.push({ path: '/order', query: { status: 1 } })
const openDessert = (name) => router.push({ path: '/dessert', query: { name } })
</script>

<template>
  <div class="dash">
    <!-- 问候 -->
    <header class="dash__head">
      <div>
        <p class="dash__date">{{ dateText }}</p>
        <h1 class="dash__hello">{{ hello }}</h1>
        <p class="dash__sub">这是喜甜今天的经营情况，数据实时取自门店系统。</p>
      </div>
      <div class="dash__head-actions">
        <el-button @click="loadAll">
          <SwIcon
            name="refresh"
            :size="16"
          />
          <span style="margin-left: 6px">刷新</span>
        </el-button>
        <el-button
          v-if="can('order:order:list')"
          type="primary"
          @click="openOrder"
        >
          处理订单
        </el-button>
      </div>
    </header>

    <!-- KPI -->
    <p
      v-if="statsError && trendError"
      class="dash__error"
      role="alert"
    >
      <SwIcon
        name="alert"
        :size="16"
      />{{ statsError }}
      <button
        type="button"
        class="link-btn"
        @click="loadAll"
      >重试</button>
    </p>
    <section
      class="kpis"
      aria-label="关键指标"
    >
      <article
        v-for="k in kpis"
        :key="k.key"
        class="kpi surface"
      >
        <p class="kpi__label">{{ k.label }}</p>
        <p
          class="kpi__value sw-num"
          :class="{ 'is-loading': loading }"
        >{{ loading ? ' ' : k.value }}</p>
        <p
          class="kpi__sub"
          :class="k.tone ? `is-${k.tone}` : ''"
        >{{ loading ? '' : k.sub }}</p>
      </article>
    </section>

    <div class="dash__grid">
      <!-- 营收趋势 -->
      <section
        class="panel surface panel--chart"
        aria-labelledby="trend-title"
      >
        <header class="panel__head">
          <div>
            <h2
              id="trend-title"
              class="panel__title"
            >营收趋势</h2>
            <p class="panel__desc">
              {{ range === 'week' ? '近 7 日' : '近 30 日' }}营收 <strong class="sw-num">{{ formatMoney(rangeTotal) }}</strong>
              · 已配送 / 完成 <strong class="sw-num">{{ rangeOrders }}</strong> 单
            </p>
          </div>
          <el-radio-group
            v-model="range"
            size="small"
            aria-label="时间范围"
          >
            <el-radio-button value="week">近 7 日</el-radio-button>
            <el-radio-button value="month">近 30 日</el-radio-button>
          </el-radio-group>
        </header>
        <div
          v-if="trendError"
          class="panel__state"
        >{{ trendError }}</div>
        <div
          v-else
          class="chart-wrap"
        >
          <div
            ref="chartEl"
            class="chart"
            role="img"
            :aria-label="`${range === 'week' ? '近 7 日' : '近 30 日'}营收与订单柱线图`"
          ></div>
          <p
            v-if="!loading && rangeTotal === 0 && rangeOrders === 0"
            class="chart-empty"
          >
            {{ range === 'week' ? '近 7 日' : '近 30 日' }}还没有已配送或已完成的订单
          </p>
        </div>
        <p class="panel__foot">营收与订单只统计「配送中」与「已完成」的订单，与报表口径一致。</p>
      </section>

      <!-- 订单队列 -->
      <section
        v-if="canSeeOrders"
        class="panel surface"
        aria-labelledby="queue-title"
      >
        <header class="panel__head">
          <h2
            id="queue-title"
            class="panel__title"
          >订单队列</h2>
          <router-link
            to="/order"
            class="panel__link"
          >全部订单 <SwIcon
            name="chevron-right"
            :size="14"
          /></router-link>
        </header>
        <div
          v-if="pipelineError"
          class="panel__state"
        >
          {{ pipelineError }} <button
            type="button"
            class="link-btn"
            @click="loadPipeline"
          >重试</button>
        </div>
        <template v-else>
          <div class="queue">
            <router-link
              :to="{ path: '/order', query: { status: 1 } }"
              class="queue__cell is-pending"
            >
              <span class="queue__num sw-num">{{ pipeline.pending ?? '—' }}</span>
              <span class="queue__label">待接单</span>
            </router-link>
            <router-link
              :to="{ path: '/order', query: { status: 2 } }"
              class="queue__cell"
            >
              <span class="queue__num sw-num">{{ pipeline.making ?? '—' }}</span>
              <span class="queue__label">制作中</span>
            </router-link>
            <router-link
              :to="{ path: '/order', query: { status: 3 } }"
              class="queue__cell"
            >
              <span class="queue__num sw-num">{{ pipeline.delivering ?? '—' }}</span>
              <span class="queue__label">配送中</span>
            </router-link>
          </div>
          <ul
            v-if="pipeline.latest.length"
            class="queue__list"
          >
            <li
              v-for="o in pipeline.latest"
              :key="o.id"
            >
              <router-link
                :to="{ path: '/order', query: { status: 1, orderNo: o.orderNo } }"
                class="queue__order"
              >
                <span class="queue__order-main">
                  <span class="queue__order-name">{{ o.customerName }}</span>
                  <span class="queue__order-items">{{ formatOrderItems(o.items) }}</span>
                </span>
                <span class="queue__order-side">
                  <span class="sw-num">{{ formatMoney(o.amount) }}</span>
                  <span class="queue__order-time">{{ formatRelative(o.createTime) }}</span>
                </span>
              </router-link>
            </li>
          </ul>
          <p
            v-else-if="!loading"
            class="panel__empty"
          >
            <StatusBadge
              tone="success"
              label="已清空"
              size="sm"
            />
            暂无待接单的订单
          </p>
        </template>
      </section>

      <!-- 热销排行 -->
      <section
        class="panel surface"
        aria-labelledby="rank-title"
      >
        <header class="panel__head">
          <h2
            id="rank-title"
            class="panel__title"
          >热销排行</h2>
          <span class="panel__desc">在售甜品 · 累计销量</span>
        </header>
        <ol
          v-if="ranking.length"
          class="rank"
        >
          <li
            v-for="(item, i) in ranking"
            :key="item.name"
            class="rank__item"
          >
            <span
              class="rank__pos sw-num"
              :class="{ 'is-top': i < 3 }"
            >{{ i + 1 }}</span>
            <span class="rank__main">
              <span class="rank__row">
                <span class="rank__name">{{ item.name }}</span>
                <span class="rank__sales sw-num">{{ formatCount(item.sales) }} 份</span>
              </span>
              <span class="rank__bar"><span :style="{ width: `${(Number(item.sales) || 0) / maxSales * 100}%` }"></span></span>
            </span>
          </li>
        </ol>
        <p
          v-else-if="!loading"
          class="panel__empty"
        >暂无销量数据</p>
      </section>

      <!-- 库存提醒 -->
      <section
        class="panel surface"
        aria-labelledby="stock-title"
      >
        <header class="panel__head">
          <h2
            id="stock-title"
            class="panel__title"
          >库存提醒</h2>
          <span class="panel__desc">在售且库存低于 20 件</span>
        </header>
        <ul
          v-if="stockAlerts.length"
          class="stock"
        >
          <li
            v-for="item in stockAlerts"
            :key="item.name"
          >
            <button
              type="button"
              class="stock__item"
              :disabled="!can('dessert:dessert:list')"
              @click="openDessert(item.name)"
            >
              <span class="stock__name">{{ item.name }}</span>
              <StatusBadge
                :tone="item.stock <= 0 ? 'muted' : item.stock < 10 ? 'danger' : 'warning'"
                :label="item.stock <= 0 ? '售罄' : `剩 ${item.stock} 件`"
                size="sm"
              />
            </button>
          </li>
        </ul>
        <p
          v-else-if="!loading"
          class="panel__empty"
        >
          <StatusBadge
            tone="success"
            label="充足"
            size="sm"
          />
          所有在售甜品库存充足
        </p>
      </section>
    </div>

    <!-- 快捷入口 -->
    <section
      v-if="actions.length"
      class="shortcuts"
      aria-label="快捷入口"
    >
      <router-link
        v-for="a in actions"
        :key="a.label"
        :to="a.to"
        class="shortcut surface"
      >
        <span class="shortcut__icon"><SwIcon
          :name="a.icon"
          :size="20"
        /></span>
        <span class="shortcut__text">
          <span class="shortcut__label">{{ a.label }}</span>
          <span class="shortcut__desc">{{ a.desc }}</span>
        </span>
        <SwIcon
          name="chevron-right"
          :size="16"
          class="shortcut__go"
        />
      </router-link>
      <a
        href="/m"
        target="_blank"
        rel="noopener"
        class="shortcut surface"
      >
        <span class="shortcut__icon"><SwIcon
          name="bag"
          :size="20"
        /></span>
        <span class="shortcut__text">
          <span class="shortcut__label">顾客商城</span>
          <span class="shortcut__desc">以顾客视角查看在售甜品</span>
        </span>
        <SwIcon
          name="external"
          :size="16"
          class="shortcut__go"
        />
      </a>
    </section>
  </div>
</template>

<style scoped>
.dash__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.dash__date {
  font-size: 13px;
  color: var(--sw-ink-3);
}

.dash__hello {
  margin-top: 4px;
  font-size: 26px;
  font-weight: 600;
}

.dash__sub {
  margin-top: 4px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.dash__head-actions {
  display: flex;
  gap: 10px;
}

.dash__error {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px 14px;
  border-radius: var(--sw-r-md);
  background: var(--sw-danger-bg);
  color: var(--sw-danger);
  font-size: 13px;
}

/* ---- KPI ---- */
.kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.kpi {
  padding: 18px 20px;
}

.kpi__label {
  font-size: 13px;
  color: var(--sw-ink-3);
}

.kpi__value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kpi__value.is-loading {
  width: 60%;
  height: 31px;
  border-radius: var(--sw-r-sm);
  background: var(--sw-fill);
}

.kpi__sub {
  min-height: 18px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.kpi__sub.is-up {
  color: var(--sw-success);
}

.kpi__sub.is-down {
  color: var(--sw-danger);
}

/* ---- 面板网格 ---- */
.dash__grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.panel {
  display: flex;
  flex-direction: column;
  padding: 20px;
  min-width: 0;
}

.panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel__title {
  font-size: 16px;
  font-weight: 600;
}

.panel__desc {
  margin-top: 2px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.panel__desc strong {
  color: var(--sw-ink);
}

.panel__link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.panel__link:hover {
  color: var(--sw-accent);
}

.panel__state,
.panel__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex: 1;
  min-height: 120px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.panel__foot {
  margin-top: 8px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.chart-wrap {
  position: relative;
}

.chart {
  height: 300px;
}

.chart-empty {
  position: absolute;
  left: 50%;
  top: 42%;
  transform: translate(-50%, -50%);
  padding: 8px 14px;
  border-radius: var(--sw-r-pill);
  background: var(--sw-surface);
  border: 1px solid var(--sw-border-soft);
  font-size: 13px;
  color: var(--sw-ink-3);
  white-space: nowrap;
}

/* ---- 订单队列 ---- */
.queue {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.queue__cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px;
  border-radius: var(--sw-r-md);
  background: var(--sw-fill-soft);
  color: var(--sw-ink);
  transition: background-color var(--sw-dur-1) var(--sw-ease);
}

.queue__cell:hover {
  background: var(--sw-fill);
}

.queue__cell.is-pending {
  background: var(--sw-warning-bg);
  color: var(--sw-warning);
}

.queue__num {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.queue__label {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.queue__cell.is-pending .queue__label {
  color: inherit;
}

.queue__list {
  list-style: none;
  margin-top: 12px;
}

.queue__order {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 4px;
  border-top: 1px solid var(--sw-border-soft);
  color: var(--sw-ink);
}

.queue__order:hover .queue__order-name {
  color: var(--sw-accent);
}

.queue__order-main,
.queue__order-side {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.queue__order-side {
  align-items: flex-end;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
}

.queue__order-name {
  font-size: 13px;
  font-weight: 600;
}

.queue__order-items,
.queue__order-time {
  font-size: 12px;
  font-weight: 400;
  color: var(--sw-ink-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---- 热销排行 ---- */
.rank {
  list-style: none;
}

.rank__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 0;
}

.rank__pos {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--sw-fill-soft);
  color: var(--sw-ink-3);
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.rank__pos.is-top {
  background: var(--sw-accent-soft);
  color: var(--sw-accent);
}

.rank__main {
  flex: 1;
  min-width: 0;
}

.rank__row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
}

.rank__name {
  font-weight: 600;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.rank__sales {
  color: var(--sw-ink-3);
  flex-shrink: 0;
}

.rank__bar {
  display: block;
  height: 4px;
  margin-top: 6px;
  border-radius: 2px;
  background: var(--sw-fill-soft);
  overflow: hidden;
}

.rank__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--sw-blush);
}

/* ---- 库存提醒 ---- */
.stock {
  list-style: none;
  max-height: 260px;
  overflow-y: auto;
}

.stock__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 10px 4px;
  border: 0;
  border-top: 1px solid var(--sw-border-soft);
  background: transparent;
  color: var(--sw-ink);
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.stock li:first-child .stock__item {
  border-top: 0;
}

.stock__item:hover:not(:disabled) .stock__name {
  color: var(--sw-accent);
}

.stock__item:disabled {
  cursor: default;
}

/* ---- 快捷入口 ---- */
.shortcuts {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.shortcut {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  color: var(--sw-ink);
  transition: border-color var(--sw-dur-1) var(--sw-ease), box-shadow var(--sw-dur-1) var(--sw-ease);
}

.shortcut:hover {
  border-color: var(--sw-border);
  box-shadow: var(--sw-shadow-sm);
}

.shortcut__icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: var(--sw-r-md);
  background: var(--sw-accent-soft);
  color: var(--sw-ink);
  flex-shrink: 0;
}

.shortcut__text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.shortcut__label {
  font-size: 14px;
  font-weight: 600;
}

.shortcut__desc {
  font-size: 12px;
  color: var(--sw-ink-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.shortcut__go {
  color: var(--sw-ink-4);
}

@media (max-width: 1200px) {
  .kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .dash__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .kpis {
    gap: 10px;
  }

  .kpi {
    padding: 14px;
  }

  .kpi__value {
    font-size: 20px;
  }

  .chart {
    height: 240px;
  }

  .dash__hello {
    font-size: 22px;
  }
}
</style>
