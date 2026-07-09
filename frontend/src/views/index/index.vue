<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  Wallet, ShoppingBag, UserFilled, Dessert,
  Top, Van, Position, ArrowRight, Plus, Promotion, DataAnalysis
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/modules/user'
import { useDashboard } from '@/composables/useDashboard'
import { useGlassSpotlight, useMagneticTilt, useGlassRipple } from '@/composables/useLiquidGlass'
import { useScrollReveal } from '@/composables/useScrollReveal'
import { useMotionPref } from '@/composables/useMotionPref'
import CountUp from '@/components/CountUp.vue'
import FloatingParticles from '@/components/FloatingParticles.vue'

const router = useRouter()
const userStore = useUserStore()
const userName = ref(userStore.username || '主厨')

// 动效降级信号：static 档（reduced-motion / 低端）→ 入场瞬时到位
const { allowMotion } = useMotionPref()

/**
 * KPI 卡 v-motion 变体：弹簧 + 逐级 stagger 升起。
 * static 档返回"瞬时到位"变体，尊重 prefers-reduced-motion。
 */
const kpiMotion = (index) => {
  if (!allowMotion.value) {
    return { initial: { opacity: 1, y: 0 }, enter: { opacity: 1, y: 0 } }
  }
  return {
    initial: { opacity: 0, y: 26, scale: 0.98 },
    enter: {
      opacity: 1,
      y: 0,
      scale: 1,
      transition: { type: 'spring', stiffness: 240, damping: 20, delay: 120 + index * 90 },
    },
  }
}

const {
  loading: dashboardLoading,
  statsCards, rankList, stockAlerts, reviews, campaigns,
  microKpis, quickActions, chartRange, chartRef, chartLegend,
  roseChartRef, heatmapChartRef, orderFlow, orderFlowVisible,
  initChart, initRoseChart, initHeatmapChart, loadData, setChartRange,
  handleReorder, sendCompensation: sendCompensationCoupon,
} = useDashboard()

// 图表：IntersectionObserver 触发入场，每个只初始化一次（滚入视口才播放"生长"动画）
let chartObserver = null
onMounted(() => {
  nextTick(() => {
    loadData()
    chartObserver = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return
        const el = entry.target
        if (el === chartRef.value) initChart()
        else if (el === roseChartRef.value) initRoseChart()
        else if (el === heatmapChartRef.value) initHeatmapChart()
        chartObserver.unobserve(el)
      })
    }, { threshold: 0.15 })
    ;[chartRef.value, roseChartRef.value, heatmapChartRef.value]
      .forEach((el) => el && chartObserver.observe(el))
  })
})

onUnmounted(() => {
  chartObserver?.disconnect()
})

/* ==================== 智能问候引擎 ==================== */
const getGreeting = () => {
  const hour = new Date().getHours()
  if (hour < 5) return '夜深了'
  if (hour < 11) return '清晨好'
  if (hour < 13) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}

const navigateTo = (path) => {
  if (router) router.push(path)
}

// ========== Liquid Glass Interactive Effects ==========
const bannerRef = ref(null)
const dashboardRef = ref(null)

// Cursor-follow spotlight on the aurora banner
useGlassSpotlight(bannerRef, { selector: ':self' })

// Magnetic tilt on metric cards
useMagneticTilt(dashboardRef, { selector: '.metric-stripe-card', maxTilt: 5, scale: 1.015 })

// Glass ripple on quick action cards
useGlassRipple(dashboardRef, { selector: '.action-dock-card', color: 'rgba(232, 99, 122, 0.12)', maxSize: 400 })

// Scroll reveal for bento cards
useScrollReveal('.bento-card', { stagger: 0.08, threshold: 0.1 })

// Parallax aurora orbs — follow cursor with subtle offset
const orbStyle = ref({})
let bannerRafId = null
let orbTargetX = 0, orbTargetY = 0, orbCurrentX = 0, orbCurrentY = 0

const onBannerMove = (e) => {
  if (!bannerRef.value) return
  const rect = bannerRef.value.getBoundingClientRect()
  orbTargetX = (e.clientX - rect.left) / rect.width - 0.5
  orbTargetY = (e.clientY - rect.top) / rect.height - 0.5
  if (!bannerRafId) {
    bannerRafId = requestAnimationFrame(animateOrbs)
  }
}

const onBannerLeave = () => {
  orbTargetX = 0
  orbTargetY = 0
}

const animateOrbs = () => {
  orbCurrentX += (orbTargetX - orbCurrentX) * 0.05
  orbCurrentY += (orbTargetY - orbCurrentY) * 0.05

  if (Math.abs(orbTargetX - orbCurrentX) < 0.0005 && Math.abs(orbTargetY - orbCurrentY) < 0.0005 && orbTargetX === 0 && orbTargetY === 0) {
    bannerRafId = null
    return
  }

  orbStyle.value = {
    '--orb-x': orbCurrentX * 30 + 'px',
    '--orb-y': orbCurrentY * 20 + 'px',
  }
  bannerRafId = requestAnimationFrame(animateOrbs)
}

onMounted(() => {
  if (bannerRef.value) {
    bannerRef.value.addEventListener('mousemove', onBannerMove, { passive: true })
    bannerRef.value.addEventListener('mouseleave', onBannerLeave)
  }
})

onUnmounted(() => {
  if (bannerRafId) cancelAnimationFrame(bannerRafId)
  if (bannerRef.value) {
    bannerRef.value.removeEventListener('mousemove', onBannerMove)
    bannerRef.value.removeEventListener('mouseleave', onBannerLeave)
  }
})
</script>

<template>
  <div ref="dashboardRef" class="dolce-dashboard-wrapper">
    <FloatingParticles :count="12" color="primary" :speed="0.8" />

    <!-- ==================== 1. Apple-style Liquid Glass 欢迎 Banner ==================== -->
    <header ref="bannerRef" class="aurora-banner glass-panel glass-spotlight">
      <div class="aurora-banner__blur-canvas">
        <div class="aurora-banner__orb aurora-banner__orb--raspberry" :style="orbStyle"></div>
        <div class="aurora-banner__orb aurora-banner__orb--amber" :style="orbStyle"></div>
        <div class="aurora-banner__orb aurora-banner__orb--cream" :style="orbStyle"></div>
      </div>

      <div class="aurora-banner__content-row">
        <div class="aurora-banner__brand-profile">
          <span class="aurora-banner__greeting-tag">
            <span class="aurora-banner__greeting-dot"></span>
            {{ getGreeting() }}，{{ userName }}
          </span>
          <h1 class="aurora-banner__title">DessertShop 运营决策大盘</h1>
          <p class="aurora-banner__description">
            基于实时经营数据，智能监控营收趋势、冷链物流与客户情感，驱动数据化精细运营。
          </p>
        </div>

        <!-- Stripe-style 高频业务快照 -->
        <div class="aurora-banner__micro-kpis">
          <div
            v-for="(kpi, i) in microKpis"
            :key="kpi.label"
            class="micro-kpi-card"
          >
            <span class="micro-kpi-card__label">{{ kpi.label }}</span>
            <div class="micro-kpi-card__value-group">
              <span v-if="kpi.currency" class="micro-kpi-card__currency">{{ kpi.currency }}</span>
              <span class="micro-kpi-card__value">{{ kpi.value }}</span>
              <span v-if="kpi.unit" class="micro-kpi-card__unit">{{ kpi.unit }}</span>
            </div>
            <span class="micro-kpi-card__subtext" :class="kpi.subtextClass">{{ kpi.subtext }}</span>
            <div v-if="i < microKpis.length - 1" class="micro-kpi-card__divider"></div>
          </div>
        </div>
      </div>
    </header>

    <!-- ==================== 2. 今日经营数据指标带 ==================== -->
    <section class="metrics-stripe-grid">
      <div
        v-for="(card, index) in statsCards"
        :key="card.title"
        v-motion="kpiMotion(index)"
        class="metric-stripe-motion"
      >
       <div class="metric-stripe-card glass-panel magnetic-hover">
        <div class="metric-stripe-card__top">
          <div class="metric-stripe-card__meta">
            <span class="metric-stripe-card__title">{{ card.title }}</span>
            <span class="metric-stripe-card__desc">{{ card.desc }}</span>
          </div>
          <div class="metric-stripe-card__badge" :class="card.trendType">
            <el-icon :size="12"><Top /></el-icon>
            <span>{{ card.trend }}</span>
          </div>
        </div>

        <div class="metric-stripe-card__middle">
          <span class="metric-stripe-card__value">
            <CountUp odometer :value="card.value" :prefix="card.prefix" :suffix="card.suffix" :decimals="card.decimals || 0" />
          </span>
          <div class="metric-stripe-card__icon-wrapper" :style="{ background: card.iconBg }">
            <el-icon :size="20" :style="{ color: card.iconColor }">
              <component :is="card.icon" />
            </el-icon>
          </div>
        </div>

        <div class="metric-stripe-card__footer">
          <div class="metric-stripe-card__progress-container">
            <div
              class="metric-stripe-card__progress-bar"
              :style="{ width: card.progress + '%', background: card.iconColor }"
            ></div>
          </div>
          <span class="metric-stripe-card__target-label">达成率 {{ card.progress }}%</span>
        </div>
       </div>
      </div>
    </section>

    <!-- ==================== 3. 黄金分割 Bento 主体网格 ==================== -->
    <main class="bento-layout-matrix">

      <!-- LEFT CONTAINER (70%) -->
      <div class="bento-layout-matrix__main">

        <!-- 3.1 销售趋势 & 边际利润双轴面板 -->
        <div class="glass-panel glass-spotlight bento-card bento-card--chart">
          <div class="bento-card__header">
            <div class="bento-card__title-group">
              <span class="bento-card__subtitle">SALES & MARGIN ENGINE</span>
              <h2 class="bento-card__title">销售增长与边际利润曲线</h2>
            </div>
            <div class="bento-card__action-controls">
              <el-radio-group v-model="chartRange" size="small" class="linear-segmented-control" @change="setChartRange">
                <el-radio-button label="realtime">实时流</el-radio-button>
                <el-radio-button label="week">近7日</el-radio-button>
                <el-radio-button label="month">近30日</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <div class="chart-canvas-container">
            <div class="chart-canvas-glow"></div>
            <div ref="chartRef" class="chart-canvas-dom"></div>
          </div>

          <div class="chart-legend-stripe">
            <div class="chart-legend-item">
              <span class="legend-color-dot legend-color-dot--raspberry"></span>
              <span class="legend-label">营业净营收</span>
              <span class="legend-value">{{ chartLegend.revenue }}</span>
            </div>
            <div class="chart-legend-item">
              <span class="legend-color-dot legend-color-dot--mint"></span>
              <span class="legend-label">平均销售利润率</span>
              <span class="legend-value">{{ chartLegend.margin }}</span>
            </div>
            <div class="chart-legend-item">
              <span class="legend-color-dot legend-color-dot--amber"></span>
              <span class="legend-label">客单均价</span>
              <span class="legend-value">{{ chartLegend.avgOrder }}</span>
            </div>
          </div>
        </div>

        <!-- 3.2 活动运营与营销大盘 -->
        <div class="glass-panel glass-spotlight bento-card bento-card--campaign">
          <div class="bento-card__header">
            <div class="bento-card__title-group">
              <span class="bento-card__subtitle">CAMPAIGN & PROMOTION</span>
              <h2 class="bento-card__title">活动运营与转化效能看板</h2>
            </div>
            <el-button type="primary" size="small" class="shining-action-btn" @click="navigateTo('/report')">
              <el-icon class="btn-icon-spin"><Promotion /></el-icon>
              发布新季活动
            </el-button>
          </div>

          <div class="campaign-grid">
            <div
              v-for="campaign in campaigns"
              :key="campaign.name"
              class="campaign-item-card glass-panel"
            >
              <div class="campaign-item-card__header">
                <el-tag :type="campaign.statusType" size="small" effect="plain" class="custom-badge-tag">
                  {{ campaign.status }}
                </el-tag>
                <span class="campaign-item-card__time">{{ campaign.time }}</span>
              </div>
              <h3 class="campaign-item-card__name">{{ campaign.name }}</h3>
              <p class="campaign-item-card__subtitle">{{ campaign.subtitle }}</p>

              <div class="campaign-metrics-row">
                <div class="campaign-micro-metric">
                  <span class="campaign-micro-metric__label">活动销售额</span>
                  <span class="campaign-micro-metric__value">{{ campaign.metrics.sales }}</span>
                </div>
                <div class="campaign-micro-metric">
                  <span class="campaign-micro-metric__label">优惠券使用率</span>
                  <span class="campaign-micro-metric__value">{{ campaign.metrics.usage }}</span>
                </div>
                <div class="campaign-micro-metric">
                  <span class="campaign-micro-metric__label">投资回报 ROI</span>
                  <span class="campaign-micro-metric__value highlight-mint">{{ campaign.metrics.roi }}</span>
                </div>
              </div>

              <div class="campaign-item-card__footer-progress">
                <div class="campaign-progress-track">
                  <div class="campaign-progress-fill" :class="{ ready: campaign.progress === 0 }" :style="{ width: campaign.progress + '%' }"></div>
                </div>
                <span class="campaign-progress-label">{{ campaign.progress > 0 ? `目标达成率 ${campaign.progress}%` : '锁定合作博主 15 位' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 3.3 Linear-style 快捷操作悬浮舱 -->
        <section class="quick-action-dock-panel">
          <div class="quick-action-dock-panel__title-bar">
            <span class="quick-action-dock-panel__hint">QUICK SHORTCUTS</span>
            <span class="quick-action-dock-panel__desc">主厨快捷指令盘 · 支持一键快速导航</span>
          </div>

          <div class="action-dock-grid">
            <div
              v-for="action in quickActions"
              :key="action.title"
              class="action-dock-card glass-panel glass-ripple"
              @click="navigateTo(action.route)"
            >
              <div class="action-dock-card__icon-halo" :style="{ background: action.iconBg }">
                <el-icon :size="24" :style="{ color: action.iconColor }">
                  <component :is="action.icon" />
                </el-icon>
              </div>
              <div class="action-dock-card__content">
                <h4 class="action-dock-card__title">{{ action.title }}</h4>
                <p class="action-dock-card__desc">{{ action.desc }}</p>
              </div>
              <el-icon class="action-dock-card__arrow-link"><ArrowRight /></el-icon>
            </div>
          </div>
        </section>

      </div>

      <!-- RIGHT CONTAINER (30%) -->
      <div class="bento-layout-matrix__side">

        <!-- 3.4 品类销售玫瑰图 -->
        <div class="glass-panel glass-spotlight bento-card bento-card--rose">
          <div class="bento-card__header">
            <div class="bento-card__title-group">
              <span class="bento-card__subtitle">CATEGORY DISTRIBUTION</span>
              <h2 class="bento-card__title">品类销售占比</h2>
            </div>
          </div>
          <div ref="roseChartRef" class="rose-chart-canvas"></div>
        </div>

        <!-- 3.5 智能库存效期与冷链预警 -->
        <div class="glass-panel glass-spotlight bento-card bento-card--alert">
          <div class="bento-card__header">
            <div class="bento-card__title-group">
              <span class="bento-card__subtitle">SHELF-LIFE & INVENTORY</span>
              <h2 class="bento-card__title">效期追踪与库存预警</h2>
            </div>
            <el-tag type="warning" size="small" effect="dark" class="warning-pill-count">
              {{ stockAlerts.filter(a => a.stock < a.threshold).length }} 项待补货
            </el-tag>
          </div>

          <div class="bento-alerts-flow">
            <div
              v-for="item in stockAlerts"
              :key="item.name"
              class="bento-alert-pill glass-panel"
            >
              <div class="bento-alert-pill__body">
                <div class="bento-alert-pill__left">
                  <span class="bento-alert-pill__name">{{ item.name }}</span>
                  <div class="bento-alert-pill__stats-bar">
                    <span class="bento-alert-pill__amount font-mono">
                      余 {{ item.stock }} / {{ item.threshold }} {{ item.unit }}
                    </span>
                    <span
                      class="bento-alert-pill__status-dot"
                      :class="item.stock < item.threshold * 0.5 ? 'danger' : 'warning'"
                    ></span>
                  </div>
                </div>
                <div class="bento-alert-pill__right">
                  <el-button type="info" size="small" class="reorder-action-btn" @click="handleReorder(item.name)">
                    快捷采购
                  </el-button>
                </div>
              </div>
              <div class="bento-alert-pill__countdown-line">
                <div
                  class="bento-alert-pill__countdown-fill"
                  :class="item.stock < item.threshold * 0.5 ? 'danger' : 'warning'"
                  :style="{ width: (item.stock / item.threshold * 100) + '%' }"
                ></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 3.6 顾客情感分析 -->
        <div class="glass-panel glass-spotlight bento-card bento-card--reviews">
          <div class="bento-card__header">
            <div class="bento-card__title-group">
              <span class="bento-card__subtitle">CUSTOMER SENTIMENT FEED</span>
              <h2 class="bento-card__title">顾客口碑与情感倾向</h2>
            </div>
            <span class="bento-card__link-text" @click="navigateTo('/log')">
              分析全部 <el-icon><ArrowRight /></el-icon>
            </span>
          </div>

          <div class="bento-reviews-flow">
            <div
              v-for="review in reviews"
              :key="review.user"
              class="bento-review-bubble glass-panel"
            >
              <div class="bento-review-bubble__avatar-halo" :style="{ background: review.avatarBg }">
                {{ review.user.charAt(0) }}
              </div>
              <div class="bento-review-bubble__text-area">
                <div class="bento-review-bubble__meta-row">
                  <span class="bento-review-bubble__username">{{ review.user }}</span>
                  <div class="bento-review-bubble__stars">
                    <el-icon
                      v-for="s in 5"
                      :key="s"
                      :size="10"
                      :style="{ color: s <= review.rating ? '#f0a35c' : 'rgba(45,35,39,0.1)' }"
                    >
                      <Dessert />
                    </el-icon>
                  </div>
                </div>
                <p class="bento-review-bubble__content">{{ review.text }}</p>
                <div class="bento-review-bubble__actions-row">
                  <span class="sentiment-pill positive">
                    <span class="sentiment-pill__dot"></span>
                    情感极佳
                  </span>
                  <button class="sentiment-action-btn-link" @click="sendCompensationCoupon(review.user)">
                    一键赠券
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </main>

    <!-- ==================== 4. 第二行面板：热力图 + 热销排行 + 实时订单流 ==================== -->
    <section class="dashboard-row-2">
      <!-- 4.1 销售热度日历 -->
      <div class="glass-panel glass-spotlight bento-card bento-card--heatmap">
        <div class="bento-card__header">
          <div class="bento-card__title-group">
            <span class="bento-card__subtitle">SALES HEATMAP · MAY 2026</span>
            <h2 class="bento-card__title">月度销售热度日历</h2>
          </div>
          <span class="heatmap-highlight-tag">🔥 峰值 ¥10,600</span>
        </div>
        <div ref="heatmapChartRef" class="heatmap-chart-canvas"></div>
      </div>

      <!-- 4.2 热销排行 -->
      <div class="glass-panel glass-spotlight bento-card bento-card--rank">
        <div class="bento-card__header">
          <div class="bento-card__title-group">
            <span class="bento-card__subtitle">HOT SELLING TOP 5</span>
            <h2 class="bento-card__title">热销排行</h2>
          </div>
          <span class="bento-card__link-text" @click="navigateTo('/price')">
            全部 <el-icon :size="12"><ArrowRight /></el-icon>
          </span>
        </div>

        <div class="bento-rank-flow">
          <div
            v-for="(item, index) in rankList"
            :key="item.name"
            class="bento-rank-item"
          >
            <div class="bento-rank-item__position" :class="{ 'bento-rank-item__position--top': index < 3 }">
              {{ index + 1 }}
            </div>
            <div class="bento-rank-item__main-body">
              <div class="bento-rank-item__title-row">
                <span class="bento-rank-item__name">{{ item.name }}</span>
                <span class="bento-rank-item__category">{{ item.category }}</span>
              </div>
              <div class="bento-rank-item__stat-row">
                <span class="bento-rank-item__sales-count">已售 {{ item.sales }} 份</span>
                <div class="bento-rank-item__progress-rail">
                  <div
                    class="bento-rank-item__progress-fill"
                    :style="{ width: (item.sales / (rankList[0]?.sales || 1) * 100) + '%' }"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 4.3 实时订单流 -->
      <div class="glass-panel glass-spotlight bento-card bento-card--orderflow">
        <div class="bento-card__header">
          <div class="bento-card__title-group">
            <span class="bento-card__subtitle">LIVE ORDER STREAM</span>
            <h2 class="bento-card__title">实时订单流</h2>
          </div>
          <div class="orderflow-header-right">
            <span class="orderflow-live-dot"></span>
            <span class="orderflow-live-text">LIVE</span>
          </div>
        </div>

        <div class="orderflow-list">
          <div
            v-for="order in orderFlow"
            :key="order.id"
            class="orderflow-item"
          >
            <div class="orderflow-item__time">{{ order.time }}</div>
            <div class="orderflow-item__body">
              <div class="orderflow-item__info">
                <span class="orderflow-item__id">#{{ order.id.slice(-4) }}</span>
                <span class="orderflow-item__customer">{{ order.customer }}</span>
              </div>
              <div class="orderflow-item__items">{{ order.items }}</div>
            </div>
            <div class="orderflow-item__amount">¥{{ order.amount }}</div>
            <el-tag :type="order.statusColor" size="small" effect="plain" round>
              {{ order.status }}
            </el-tag>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style lang="scss" scoped>
/* ==========================================================================
   DessertShop High-End Design System - Apple Liquid Glass & visionOS Style
   All data sourced from useDashboard composable — no hardcoded values.
   ========================================================================== */

.dolce-dashboard-wrapper {
  --color-text-chocolate: var(--color-text-primary);
  --color-primary-raspberry: var(--color-primary);
  --color-primary-raspberry-glow: rgba(var(--rose-rgb), 0.18);
  --color-accent-amber: var(--color-accent);
  --color-accent-amber-glow: rgba(var(--amber-rgb), 0.15);
  --color-accent-mint: var(--matcha);

  width: 100%;
  max-width: 1360px;
  margin: 0 auto;
  padding: 24px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 28px;
  font-family: var(--font-body);
  color: var(--color-text-primary);
  background: radial-gradient(circle at 0% 0%, var(--color-bg-tertiary) 0%, var(--color-bg-primary) 70%);
  position: relative;
  min-height: 100vh;
  animation: pageEntrance 0.8s cubic-bezier(0.16, 1, 0.3, 1) both;

  &::before {
    content: '';
    position: absolute;
    top: 25%;
    right: 5%;
    width: 450px;
    height: 450px;
    background: radial-gradient(circle, rgba(240, 163, 92, 0.04) 0%, transparent 70%);
    z-index: 0;
    pointer-events: none;
  }
}

/* ==========================================================================
   1. 顶部全局欢迎看板
   ========================================================================== */
.aurora-banner {
  padding: 36px 40px;
  z-index: 2;
  overflow: hidden;

  &__blur-canvas {
    position: absolute;
    inset: 0;
    z-index: 0;
    overflow: hidden;
    opacity: 0.85;
    pointer-events: none;
  }

  &__orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    mix-blend-mode: multiply;
    animation: orbFloating 18s ease-in-out infinite;
    /* Parallax offset driven by --orb-x / --orb-y CSS vars from JS */
    transform: translate(var(--orb-x, 0px), var(--orb-y, 0px));
    transition: transform 0.3s ease-out;

    &--raspberry {
      width: 280px; height: 280px;
      right: -20px; top: -60px;
      background: rgba(232, 99, 122, 0.18);
    }
    &--amber {
      width: 220px; height: 220px;
      right: 200px; bottom: -60px;
      background: rgba(240, 163, 92, 0.12);
      animation-delay: -4s; animation-duration: 22s;
    }
    &--cream {
      width: 140px; height: 140px;
      left: 10%; top: -20px;
      background: rgba(253, 232, 236, 0.25);
      animation-duration: 15s;
    }
  }

  &__content-row {
    position: relative; z-index: 2;
    display: flex; justify-content: space-between; align-items: center;
    gap: 32px; flex-wrap: wrap;
  }

  &__brand-profile { flex: 1; min-width: 320px; }

  &__greeting-tag {
    display: inline-flex; align-items: center; gap: 8px;
    padding: 6px 12px;
    background: rgba(232, 99, 122, 0.06);
    border: 1px solid rgba(232, 99, 122, 0.15);
    border-radius: 99px;
    font-size: 13px; font-weight: 600;
    color: var(--color-primary-raspberry);
    margin-bottom: 14px; letter-spacing: 0.02em;
  }

  &__greeting-dot {
    width: 6px; height: 6px; border-radius: 50%;
    background: var(--color-primary-raspberry);
    animation: beaconPulse 2s infinite;
  }

  &__title {
    font-size: 26px; font-weight: 800;
    letter-spacing: -0.02em;
    color: var(--color-text-chocolate);
    margin-bottom: 6px;
  }

  &__description {
    font-size: 13.5px;
    color: var(--color-text-secondary);
    line-height: 1.5;
  }

  &__micro-kpis {
    display: flex; align-items: center; gap: 24px;
    background: rgba(255, 255, 255, 0.35);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.4);
    border-radius: 16px;
    padding: 16px 28px;
    box-shadow: var(--glass-specular);
  }
}

.micro-kpi-card {
  display: flex; flex-direction: column; gap: 4px;

  &__label {
    font-size: 11px; font-weight: 600;
    text-transform: uppercase; letter-spacing: 0.08em;
    color: var(--color-text-muted);
  }
  &__value-group { display: flex; align-items: baseline; color: var(--color-text-chocolate); }
  &__currency { font-size: 14px; font-weight: 700; margin-right: 2px; }
  &__value {
    font-size: 20px; font-weight: 750;
    font-variant-numeric: tabular-nums;
    letter-spacing: -0.01em;
  }
  &__unit { font-size: 12px; font-weight: 700; margin-left: 1px; }
  &__subtext { font-size: 11px; font-weight: 600; letter-spacing: 0.02em;
    &.up { color: var(--color-primary-raspberry); }
    &.stable { color: var(--color-text-secondary); }
    &.highlight { color: var(--color-accent-amber); }
  }
  &__divider { width: 1px; height: 36px; background: rgba(45, 35, 39, 0.08); }
}

/* ==========================================================================
   2. 指标带
   ========================================================================== */
.metrics-stripe-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; z-index: 2;
  @media (max-width: 1024px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 640px) { grid-template-columns: 1fr; }
}

/* v-motion 包裹层：作为 grid 单元，卡片填满；入场由 v-motion 弹簧驱动 */
.metric-stripe-motion { display: grid; }

.metric-stripe-card {
  padding: 24px; display: flex; flex-direction: column;
  transform-style: preserve-3d;
  perspective: 800px;
  will-change: transform;

  &:hover {
    transform: translateY(-4px) scale(1.02);
    box-shadow: var(--glass-specular), var(--glass-shadow-hover);
    border-color: rgba(232, 99, 122, 0.25);
    &::after { left: 100%; }
  }

  &::after {
    content: ''; position: absolute; top: 0; left: -100%;
    width: 50%; height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.35), transparent);
    transform: skewX(-25deg);
    transition: left 0.75s ease;
    z-index: 2; pointer-events: none;
  }

  &__top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
  &__meta { display: flex; flex-direction: column; gap: 2px; }
  &__title { font-size: 13px; font-weight: 700; color: var(--color-text-chocolate); }
  &__desc { font-size: 11px; color: var(--color-text-muted); }
  &__badge {
    display: inline-flex; align-items: center; gap: 3px;
    padding: 3px 8px; border-radius: 99px; font-size: 11px; font-weight: 600;
    &.up { color: var(--color-accent-mint); background: rgba(92, 184, 138, 0.08); }
    &.down { color: var(--color-text-secondary); background: rgba(45, 35, 39, 0.05); }
    &.neutral { color: var(--color-accent-amber); background: rgba(240, 163, 92, 0.08); }
  }
  &__middle { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
  &__value { font-size: 26px; font-weight: 850; font-variant-numeric: tabular-nums; letter-spacing: -0.03em; color: var(--color-text-chocolate); }
  &__icon-wrapper { width: 42px; height: 42px; border-radius: 12px; display: flex; align-items: center; justify-content: center; border: 1px solid rgba(255, 255, 255, 0.5); box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02); }
  &__footer { display: flex; flex-direction: column; gap: 6px; margin-top: auto; }
  &__progress-container { width: 100%; height: 4px; background: rgba(45, 35, 39, 0.05); border-radius: 99px; overflow: hidden; }
  &__progress-bar { height: 100%; border-radius: 99px; transition: width 1s cubic-bezier(0.16, 1, 0.3, 1); }
  &__target-label { font-size: 11px; font-weight: 500; color: var(--color-text-secondary); text-align: right; }
}

/* ==========================================================================
   3. Bento Matrix
   ========================================================================== */
.bento-layout-matrix {
  display: grid; grid-template-columns: 7fr 3fr; gap: 24px; z-index: 2;
  @media (max-width: 1140px) { grid-template-columns: 1fr; }
  &__main { display: flex; flex-direction: column; gap: 24px; }
  &__side { display: flex; flex-direction: column; gap: 24px; }
}

.bento-card {
  padding: 28px; display: flex; flex-direction: column;
  animation: cardEntrance 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
  &:hover { box-shadow: var(--glass-specular), var(--glass-shadow-hover); }

  &__header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; gap: 16px; }
  &__title-group { display: flex; flex-direction: column; gap: 4px; }
  &__subtitle { font-size: 10.5px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: var(--color-primary-raspberry); }
  &__title { font-size: 18px; font-weight: 800; letter-spacing: -0.02em; color: var(--color-text-chocolate); }
  &__link-text {
    font-size: 12.5px; font-weight: 600; color: var(--color-primary-raspberry);
    cursor: pointer; display: inline-flex; align-items: center; gap: 4px;
    transition: var(--transition-fast);
    &:hover { opacity: 0.8; transform: translateX(2px); }
  }
}

/* Chart */
.bento-card--chart .bento-card__action-controls { display: flex; align-items: center; gap: 12px; }

.linear-segmented-control {
  background: rgba(45, 35, 39, 0.05) !important; border-radius: 8px !important; padding: 2px !important; border: none !important;
  :deep(.el-radio-button__inner) {
    background: transparent !important; border: none !important; border-radius: 6px !important;
    color: var(--color-text-secondary) !important; font-weight: 600 !important;
    font-size: 12px !important; padding: 6px 14px !important; box-shadow: none !important;
    transition: var(--transition-fast) !important;
    &:hover { color: var(--color-text-chocolate) !important; }
  }
  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: rgba(255, 255, 255, 0.95) !important;
    color: var(--color-text-chocolate) !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05) !important;
  }
}

.chart-canvas-container { width: 100%; height: 320px; position: relative; border-radius: 12px; background: rgba(255, 255, 255, 0.2); border: 1px solid rgba(255, 255, 255, 0.2); margin-bottom: 20px; overflow: hidden; }
.chart-canvas-glow { position: absolute; bottom: 0; left: 10%; right: 10%; height: 120px; background: radial-gradient(ellipse at bottom, rgba(232, 99, 122, 0.05) 0%, transparent 70%); pointer-events: none; }
.chart-canvas-dom { width: 100%; height: 100%; z-index: 2; position: relative; }

.chart-legend-stripe { display: flex; align-items: center; gap: 32px; flex-wrap: wrap; padding-top: 16px; border-top: 1px solid rgba(45, 35, 39, 0.06); }
.chart-legend-item { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.legend-color-dot { width: 8px; height: 8px; border-radius: 50%;
  &--raspberry { background: var(--color-primary-raspberry); }
  &--mint { background: var(--color-accent-mint); }
  &--amber { background: var(--color-accent-amber); }
}
.legend-label { color: var(--color-text-secondary); font-weight: 500; }
.legend-value { color: var(--color-text-chocolate); font-weight: 750; font-variant-numeric: tabular-nums; }

/* Campaigns */
.shining-action-btn {
  background: linear-gradient(135deg, var(--color-primary-raspberry) 0%, hsl(340, 70%, 55%) 100%) !important;
  border: none !important; color: #fff !important; font-weight: 600 !important;
  border-radius: 10px !important; padding: 8px 18px !important;
  box-shadow: 0 4px 14px var(--color-primary-raspberry-glow) !important;
  transition: var(--transition-fast) !important;
  &:hover { transform: translateY(-1.5px); box-shadow: 0 6px 20px var(--color-primary-raspberry-glow) !important; filter: brightness(1.05); }
  &:active { transform: scale(0.96); }
  .btn-icon-spin { transition: transform 0.5s ease; }
  &:hover .btn-icon-spin { transform: rotate(18deg) scale(1.1); }
}

.campaign-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px;
  @media (max-width: 768px) { grid-template-columns: 1fr; }
}

.campaign-item-card {
  background: rgba(255, 255, 255, 0.4); padding: 22px; display: flex; flex-direction: column; gap: 12px;
  &:hover { transform: translateY(-2px); border-color: rgba(232, 99, 122, 0.2); }
  &__header { display: flex; justify-content: space-between; align-items: center; }
  .custom-badge-tag {
    border-radius: 6px !important; font-weight: 700 !important; padding: 2px 8px !important;
    letter-spacing: 0.02em !important; border: none !important;
    &.el-tag--danger { background: rgba(232, 99, 122, 0.1) !important; color: var(--color-primary-raspberry) !important; }
    &.el-tag--warning { background: rgba(240, 163, 92, 0.1) !important; color: var(--color-accent-amber) !important; }
  }
  &__time { font-size: 11px; font-weight: 600; color: var(--color-text-muted); letter-spacing: 0.04em; }
  &__name { font-size: 15px; font-weight: 800; color: var(--color-text-chocolate); line-height: 1.3; }
  &__subtitle { font-size: 12px; color: var(--color-text-secondary); line-height: 1.4; }
  .campaign-metrics-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; background: rgba(255, 255, 255, 0.3); border-radius: 12px; padding: 10px 14px; margin-top: 4px; }
  .campaign-micro-metric { display: flex; flex-direction: column; gap: 2px;
    &__label { font-size: 10px; font-weight: 600; color: var(--color-text-muted); }
    &__value { font-size: 13.5px; font-weight: 750; font-variant-numeric: tabular-nums; color: var(--color-text-chocolate);
      &.highlight-mint { color: var(--color-accent-mint); }
    }
  }
  &__footer-progress { display: flex; flex-direction: column; gap: 6px; margin-top: auto; padding-top: 10px; }
  .campaign-progress-track { width: 100%; height: 4px; background: rgba(45, 35, 39, 0.05); border-radius: 99px; overflow: hidden; }
  .campaign-progress-fill { height: 100%; background: linear-gradient(90deg, var(--color-primary-raspberry), var(--color-accent-amber)); border-radius: 99px;
    &.ready { background: var(--color-text-muted); }
  }
  .campaign-progress-label { font-size: 10.5px; font-weight: 600; color: var(--color-text-secondary); text-align: right; }
}

/* Quick Actions Dock */
.quick-action-dock-panel { margin-top: 4px;
  &__title-bar { display: flex; align-items: baseline; gap: 8px; margin-bottom: 16px; }
  &__hint { font-size: 10px; font-weight: 750; letter-spacing: 0.06em; color: var(--color-primary-raspberry); }
  &__desc { font-size: 12px; color: var(--color-text-secondary); }
}

.action-dock-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;
  @media (max-width: 640px) { grid-template-columns: 1fr; }
}
.action-dock-card {
  background: rgba(255, 255, 255, 0.48); padding: 16px 20px;
  display: flex; align-items: center; gap: 16px; cursor: pointer;
  &:hover {
    transform: translateY(-2px) scale(1.015);
    border-color: var(--color-primary-raspberry); background: rgba(255, 255, 255, 0.8);
    box-shadow: var(--glass-specular), 0 6px 20px rgba(232, 99, 122, 0.06);
    .action-dock-card__arrow-link { transform: translateX(3px) scale(1.1); color: var(--color-primary-raspberry); }
  }
  &:active { transform: scale(0.97) translateY(1px); }
  &__icon-halo { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; border: 1px solid rgba(255, 255, 255, 0.6); }
  &__content { flex: 1; display: flex; flex-direction: column; gap: 2px; }
  &__title { font-size: 13.5px; font-weight: 750; color: var(--color-text-chocolate); }
  &__desc { font-size: 11px; color: var(--color-text-muted); }
  &__arrow-link { font-size: 14px; color: var(--color-text-muted); transition: var(--transition-fast); }
}

/* Rank */
.bento-rank-flow { display: flex; flex-direction: column; gap: 14px; }
.bento-rank-item {
  display: flex; align-items: center; gap: 16px; padding: 8px 0; border-bottom: 1px dashed rgba(45, 35, 39, 0.05);
  &:last-child { border: none; padding-bottom: 0; }
  &__position { width: 32px; height: 32px; border-radius: 9px; background: rgba(45, 35, 39, 0.04); display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 800; color: var(--color-text-secondary); transition: var(--transition-fast);
    &--top { background: rgba(232, 99, 122, 0.08); color: var(--color-primary-raspberry); box-shadow: 0 2px 8px rgba(232, 99, 122, 0.05); }
  }
  &__main-body { flex: 1; display: flex; flex-direction: column; gap: 6px; }
  &__title-row { display: flex; justify-content: space-between; align-items: baseline; }
  &__name { font-size: 13.5px; font-weight: 750; color: var(--color-text-chocolate); }
  &__category { font-size: 10px; font-weight: 600; color: var(--color-text-muted); }
  &__stat-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
  &__sales-count { font-size: 11px; font-weight: 600; color: var(--color-text-secondary); font-variant-numeric: tabular-nums; }
  &__progress-rail { flex: 1; height: 4px; background: rgba(45, 35, 39, 0.04); border-radius: 99px; overflow: hidden; }
  &__progress-fill { height: 100%; background: linear-gradient(90deg, var(--color-primary-raspberry) 0%, var(--color-accent-amber) 100%); border-radius: 99px; transition: width 1s cubic-bezier(0.16, 1, 0.3, 1); }
  &:hover .bento-rank-item__position { transform: scale(1.08) rotate(-4deg); }
}

/* Inventory Alerts */
.warning-pill-count { background: rgba(240, 163, 92, 0.15) !important; border-radius: 6px !important; color: var(--color-accent-amber) !important; font-weight: 700 !important; border: none !important; }

.bento-alerts-flow { display: flex; flex-direction: column; gap: 14px; }
.bento-alert-pill {
  background: rgba(255, 255, 255, 0.35); padding: 14px 18px; border-radius: 14px; display: flex; flex-direction: column; gap: 8px;
  &__body { display: flex; justify-content: space-between; align-items: center; }
  &__left { display: flex; flex-direction: column; gap: 3px; }
  &__name { font-size: 13px; font-weight: 750; color: var(--color-text-chocolate); }
  &__stats-bar { display: flex; align-items: center; gap: 8px; }
  &__amount { font-size: 11px; color: var(--color-text-secondary); font-variant-numeric: tabular-nums; }
  &__status-dot { width: 5px; height: 5px; border-radius: 50%; display: inline-block;
    &.danger { background: var(--color-primary-raspberry); animation: beaconPulse 1.5s infinite; }
    &.warning { background: var(--color-accent-amber); }
  }
  .reorder-action-btn {
    background: rgba(45, 35, 39, 0.05) !important; border: none !important; color: var(--color-text-secondary) !important;
    font-weight: 700 !important; font-size: 11px !important; padding: 6px 12px !important; border-radius: 8px !important;
    transition: var(--transition-fast) !important;
    &:hover { background: var(--color-primary-raspberry) !important; color: #fff !important; transform: scale(1.03); }
  }
  &__countdown-line { width: 100%; height: 3px; background: rgba(45, 35, 39, 0.03); border-radius: 99px; overflow: hidden; }
  &__countdown-fill { height: 100%; border-radius: 99px; transition: width 1s ease;
    &.danger { background: var(--color-primary-raspberry); }
    &.warning { background: var(--color-accent-amber); }
  }
  &:hover { transform: translateX(2px); border-color: rgba(240, 163, 92, 0.25); }
}

/* Reviews */
.bento-reviews-flow { display: flex; flex-direction: column; gap: 14px; }
.bento-review-bubble {
  background: rgba(255, 255, 255, 0.28); padding: 16px; border-radius: 16px; display: flex; gap: 12px;
  &__avatar-halo { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 800; color: #fff; box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05); flex-shrink: 0; }
  &__text-area { flex: 1; display: flex; flex-direction: column; gap: 4px; }
  &__meta-row { display: flex; justify-content: space-between; align-items: center; }
  &__username { font-size: 12.5px; font-weight: 750; color: var(--color-text-chocolate); }
  &__stars { display: flex; gap: 1px; }
  &__content { font-size: 11.5px; color: var(--color-text-secondary); line-height: 1.4; }
  &__actions-row { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; padding-top: 6px; border-top: 1px solid rgba(45, 35, 39, 0.03); }
  .sentiment-pill { display: inline-flex; align-items: center; gap: 4px; font-size: 10px; font-weight: 700;
    &.positive { color: var(--color-accent-mint);
      .sentiment-pill__dot { width: 4px; height: 4px; border-radius: 50%; background: var(--color-accent-mint); display: inline-block; }
    }
  }
  .sentiment-action-btn-link { background: transparent; border: none; font-size: 10.5px; font-weight: 700; color: var(--color-primary-raspberry); cursor: pointer; transition: var(--transition-fast); padding: 0;
    &:hover { opacity: 0.8; text-decoration: underline; }
  }
  &:hover { transform: translateY(-2px); background: rgba(255, 255, 255, 0.5); }
}

/* ==========================================================================
   4. Row 2: Heatmap + Rank + Order Flow
   ========================================================================== */
.dashboard-row-2 {
  display: grid;
  grid-template-columns: 1fr 0.55fr 0.7fr;
  gap: 24px;
  z-index: 2;
  @media (max-width: 1140px) { grid-template-columns: 1fr 1fr; }
  @media (max-width: 768px) { grid-template-columns: 1fr; }
}

/* ---- Rose Chart ---- */
.rose-chart-canvas {
  width: 100%;
  height: 240px;
  margin-top: 8px;
}

/* ---- Heatmap ---- */
.heatmap-highlight-tag {
  font-size: 11px;
  font-weight: 700;
  color: var(--color-accent);
  background: rgba(240, 163, 92, 0.1);
  padding: 2px 10px;
  border-radius: 99px;
  letter-spacing: 0.02em;
}

.heatmap-chart-canvas {
  width: 100%;
  height: 260px;
  margin-top: 8px;
}

/* ---- Order Flow ---- */
.orderflow-header-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.orderflow-live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ef4444;
  animation: beaconPulse 1.5s infinite;
}

.orderflow-live-text {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: #ef4444;
  text-transform: uppercase;
}

.orderflow-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.orderflow-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: all var(--transition-fast);
  animation: flowItemSlideIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;

  &:hover {
    background: rgba(255, 255, 255, 0.55);
    transform: translateX(4px);
    border-color: rgba(232, 99, 122, 0.15);
  }

  &:first-child {
    border-left: 3px solid var(--color-primary);
    background: rgba(232, 99, 122, 0.04);
    animation-delay: 0s;
  }

  &:nth-child(2) { animation-delay: 0.05s; }
  &:nth-child(3) { animation-delay: 0.1s; }
  &:nth-child(4) { animation-delay: 0.15s; }
  &:nth-child(5) { animation-delay: 0.2s; }
}

.orderflow-item__time {
  font-size: 10px;
  font-weight: 600;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
  min-width: 36px;
  flex-shrink: 0;
}

.orderflow-item__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.orderflow-item__info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.orderflow-item__id {
  font-size: 11px;
  font-weight: 700;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}

.orderflow-item__customer {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.orderflow-item__items {
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.orderflow-item__amount {
  font-size: 13px;
  font-weight: 750;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

@keyframes flowItemSlideIn {
  from { opacity: 0; transform: translateX(-12px); }
  to { opacity: 1; transform: translateX(0); }
}

/* ---- Rank card (in row 2) ---- */
.bento-card--rank {
  .bento-rank-item {
    padding: 6px 0;
  }
}

/* ---- Responsive overrides for row 2 ---- */
@media (max-width: 1140px) {
  .dashboard-row-2 {
    grid-template-columns: 1fr 1fr;
    .bento-card--orderflow {
      grid-column: 1 / -1;
    }
  }
}

@media (max-width: 768px) {
  .dashboard-row-2 {
    grid-template-columns: 1fr;
    .bento-card--orderflow {
      grid-column: auto;
    }
  }
  .orderflow-item {
    flex-wrap: wrap;
    gap: 6px;
  }
  .rose-chart-canvas {
    height: 200px;
  }
  .heatmap-chart-canvas {
    height: 220px;
  }
}

/* ==========================================================================
   Keyframes
   ========================================================================== */
@keyframes pageEntrance { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
@keyframes cardEntrance { from { opacity: 0; transform: translateY(12px) scale(0.99); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes orbFloating {
  0% { transform: translate(0px, 0px) rotate(0deg); }
  33% { transform: translate(30px, -40px) rotate(120deg); }
  66% { transform: translate(-20px, 20px) rotate(240deg); }
  100% { transform: translate(0px, 0px) rotate(360deg); }
}
@keyframes beaconPulse {
  0% { transform: scale(0.9); opacity: 0.5; box-shadow: 0 0 0 0 rgba(232, 99, 122, 0.4); }
  70% { transform: scale(1.1); opacity: 1; box-shadow: 0 0 0 6px rgba(232, 99, 122, 0); }
  100% { transform: scale(0.9); opacity: 0.5; box-shadow: 0 0 0 0 rgba(232, 99, 122, 0); }
}

/* ==========================================================================
   Mobile Responsive
   ========================================================================== */
@media (max-width: 768px) {
  .dolce-dashboard-wrapper { padding: 12px; gap: 16px; }
  .aurora-banner { padding: 20px;
    &__content-row { flex-direction: column; gap: 16px; }
    &__brand-profile { min-width: 0; }
    &__title { font-size: 20px; }
    &__description { font-size: 12px; }
    &__micro-kpis { width: 100%; padding: 12px 16px; gap: 12px; flex-wrap: wrap; overflow-x: auto; }
  }
  .micro-kpi-card {
    &__value { font-size: 16px; }
    &__currency { font-size: 12px; }
    &__divider { height: 28px; }
  }
  .bento-card { padding: 18px;
    &__header { flex-direction: column; gap: 10px; }
    &__title { font-size: 16px; }
  }
  .chart-canvas-container { height: 240px; }
  .chart-legend-stripe { gap: 16px; flex-wrap: wrap; }
  .campaign-item-card .campaign-metrics-row { grid-template-columns: repeat(3, 1fr); gap: 4px; padding: 8px 10px; }
  .bento-layout-matrix { gap: 16px; &__main, &__side { gap: 16px; } }
  .action-dock-card { padding: 14px; gap: 10px;
    &__title { font-size: 12px; }
    &__desc { font-size: 10px; }
    &__icon-halo { width: 36px; height: 36px; border-radius: 10px; }
  }
}

@media (max-width: 480px) {
  .aurora-banner__micro-kpis { flex-direction: column; align-items: stretch; gap: 8px; }
  .micro-kpi-card__divider { width: 100%; height: 1px; }
  .campaign-item-card .campaign-metrics-row { grid-template-columns: 1fr 1fr 1fr; }
  .metric-stripe-card { padding: 16px; &__value { font-size: 22px; } }
}

/* ==========================================================================
   深色模式表面修复
   浅色版把大量子卡片硬编码成 rgba(255,255,255,α)「牛奶白」、分隔线用深墨
   rgba(45,35,39,α)。深色下前者刺眼、后者隐形 → 这里统一改暖玻璃 + 反相描边。
   ========================================================================== */
:global([data-theme='dark']) {
  /* 玻璃子卡片：牛奶白 → 暖玻璃 */
  .aurora-banner__micro-kpis,
  .chart-canvas-container,
  .campaign-item-card,
  .campaign-item-card .campaign-metrics-row,
  .action-dock-card,
  .bento-alert-pill,
  .bento-review-bubble,
  .orderflow-item {
    background: rgba(255, 255, 255, 0.045);
    border-color: rgba(255, 255, 255, 0.08);
  }

  /* hover 抬升时略提亮 */
  .action-dock-card:hover { background: rgba(255, 255, 255, 0.09); }
  .bento-review-bubble:hover { background: rgba(255, 255, 255, 0.08); }
  .orderflow-item:hover { background: rgba(255, 255, 255, 0.08); }

  /* 分隔线 / 进度轨道 / 虚线：深墨在暗底不可见 → 反相为浅 */
  .micro-kpi-card__divider,
  .metric-stripe-card__progress-container,
  .campaign-progress-track,
  .bento-rank-item__progress-rail,
  .bento-alert-pill__countdown-line {
    background: rgba(255, 255, 255, 0.10);
  }
  .bento-rank-item { border-bottom-color: rgba(255, 255, 255, 0.07); }
  .bento-review-bubble__actions-row { border-top-color: rgba(255, 255, 255, 0.07); }
  .chart-legend-stripe { border-top-color: rgba(255, 255, 255, 0.08); }

  /* 浅墨底控件 → 浅玻璃底 */
  .linear-segmented-control { background: rgba(255, 255, 255, 0.06) !important; }
  .bento-rank-item__position { background: rgba(255, 255, 255, 0.06); }
  .reorder-action-btn { background: rgba(255, 255, 255, 0.08) !important; }
  .linear-segmented-control .el-radio-button__original-radio:checked + .el-radio-button__inner {
    background: rgba(255, 255, 255, 0.14) !important;
    color: var(--color-text-primary) !important;
  }

  /* 实时订单流首行高亮：浅色 → 玫瑰微光 */
  .orderflow-item:first-child {
    background: rgba(var(--rose-rgb), 0.14);
    border-left-color: var(--color-primary);
  }

  /* metric 卡的扫光高光在暗底过白 → 收敛 */
  .metric-stripe-card::after {
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.08), transparent);
  }
}
</style>
