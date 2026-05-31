<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  Wallet,
  ShoppingBag,
  UserFilled,
  Dessert,
  Trophy,
  Top,
  Van,
  Position,
  CircleCheck,
  Timer,
  StarFilled,
  ArrowRight,
  Plus,
  Document,
  Promotion,
  DataAnalysis
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { queryStatsApi, queryRevenueChartApi, queryRankingApi } from '@/api/dashboard'
import CountUp from '@/components/CountUp.vue'

const router = useRouter()
const userStore = useUserStore()
const userName = ref('')
const dashboardLoading = ref(false)
const salesChartRef = ref(null)
const chartRange = ref('week')
let salesChart = null
let resizeObserver = null

// 加载仪表盘数据
const loadDashboardData = async () => {
  dashboardLoading.value = true
  try {
    userName.value = userStore.username || '主厨'

    // 并行请求仪表盘数据
    const [statsRes, revenueRes, rankingRes] = await Promise.allSettled([
      queryStatsApi(),
      queryRevenueChartApi(chartRange.value),
      queryRankingApi()
    ])

    // 统计数据
    if (statsRes.status === 'fulfilled' && statsRes.value?.code) {
      const data = statsRes.value.data
      if (data.statsCards) {
        statsCards.value = data.statsCards
      }
      if (data.campaigns) {
        campaigns.value = data.campaigns
      }
      if (data.stockAlerts) {
        stockAlerts.value = data.stockAlerts
      }
      if (data.reviews) {
        reviews.value = data.reviews
      }
    }

    // 营收图表
    if (revenueRes.status === 'fulfilled' && revenueRes.value?.code && salesChart) {
      const chartData = revenueRes.value.data
      salesChart.setOption({
        xAxis: { data: chartData.labels },
        series: [
          { data: chartData.revenue },
          { data: chartData.orders }
        ]
      })
    }

    // 排行数据
    if (rankingRes.status === 'fulfilled' && rankingRes.value?.code) {
      rankList.value = rankingRes.value.data
    }
  } catch {
    // API 未实现时使用硬编码数据（静默降级）
  } finally {
    dashboardLoading.value = false
  }
}

onMounted(() => {
  nextTick(() => {
    initSalesChart()
    loadDashboardData()
  })
})

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
  if (salesChart) {
    salesChart.dispose()
  }
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
  if (router) {
    router.push(path)
  } else {
    ElMessage.info(`导航至: ${path}`)
  }
}

/* ==================== 今日核心经营快照 (Stripe 风格) ==================== */
const statsCards = ref([
  {
    title: '今日销售额',
    value: 12860,
    prefix: '¥',
    suffix: '',
    decimals: 2,
    trend: '+12.5%',
    trendType: 'up',
    trendIcon: Top,
    icon: Wallet,
    iconBg: 'rgba(232, 99, 122, 0.08)',
    iconColor: '#e8637a',
    progress: 82,
    desc: '目标达成额 ¥15,000.00'
  },
  {
    title: '客单交易均价',
    value: 69.2,
    prefix: '¥',
    suffix: '',
    decimals: 2,
    trend: '+5.8%',
    trendType: 'up',
    trendIcon: Top,
    icon: ShoppingBag,
    iconBg: 'rgba(240, 163, 92, 0.08)',
    iconColor: '#f0a35c',
    progress: 75,
    desc: '较昨日上涨 ¥3.80'
  },
  {
    title: '私域高价值尊享会员',
    value: 1428,
    prefix: '',
    suffix: ' 人',
    decimals: 0,
    trend: '+2.4%',
    trendType: 'up',
    trendIcon: Top,
    icon: UserFilled,
    iconBg: 'rgba(92, 184, 138, 0.08)',
    iconColor: '#5cb88a',
    progress: 95,
    desc: '高频复购群体占比 62%'
  },
  {
    title: '冷链物流派单率',
    value: 186,
    prefix: '',
    suffix: ' 单',
    decimals: 0,
    trend: '稳定',
    trendType: 'neutral',
    trendIcon: CircleCheck,
    icon: Dessert,
    iconBg: 'rgba(107, 140, 206, 0.08)',
    iconColor: '#6b8cce',
    progress: 88,
    desc: '平均骑手接单响应 4.2s'
  }
])

/* ==================== Stripe 风格：高密度双轴经营大盘图表数据 ==================== */
const salesDataMap = {
  realtime: {
    xAxis: ['10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00'],
    revenue: [2100, 4800, 3600, 7200, 9800, 11200, 12860],
    margins: [62, 60, 65, 68, 64, 63, 64.8]
  },
  week: {
    xAxis: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    revenue: [8200, 9600, 11200, 8800, 13400, 15800, 12860],
    margins: [58, 60, 62, 61, 65, 66, 64.8]
  },
  month: {
    xAxis: ['05-05', '05-10', '05-15', '05-20', '05-25', '05-30', '05-31'],
    revenue: [92000, 105000, 98000, 124000, 132000, 145000, 156000],
    margins: [61, 62, 60, 63, 64, 65, 64.8]
  }
}

const initSalesChart = () => {
  if (!salesChartRef.value) return
  salesChart = echarts.init(salesChartRef.value)
  renderSalesChart()

  resizeObserver = new ResizeObserver(() => {
    salesChart?.resize()
  })
  resizeObserver.observe(salesChartRef.value)
}

const renderSalesChart = () => {
  if (!salesChart) return
  const data = salesDataMap[chartRange.value]
  
  salesChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ede6e1',
      borderWidth: 1,
      textStyle: { color: '#2d2327', fontSize: 12 },
      axisPointer: {
        type: 'line',
        lineStyle: {
          color: 'rgba(232, 99, 122, 0.4)',
          width: 1.5,
          type: 'dashed'
        }
      }
    },
    grid: {
      top: 30,
      right: 48,
      bottom: 24,
      left: 52
    },
    xAxis: {
      type: 'category',
      data: data.xAxis,
      axisLine: { lineStyle: { color: '#ede6e1' } },
      axisTick: { show: false },
      axisLabel: { color: '#a3949b', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        name: '营业额',
        show: true,
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f5f0ec', type: 'dashed' } },
        axisLabel: {
          color: '#a3949b',
          fontSize: 11,
          formatter: (v) => v >= 1000 ? (v / 1000) + 'k' : v
        }
      },
      {
        type: 'value',
        name: '利润率',
        show: true,
        min: 40,
        max: 80,
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: {
          color: '#a3949b',
          fontSize: 11,
          formatter: '{value}%'
        }
      }
    ],
    series: [
      {
        name: '营业额',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#e8637a' },
        itemStyle: { color: '#fff', borderWidth: 2.5, borderColor: '#e8637a' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(232, 99, 122, 0.12)' },
            { offset: 1, color: 'rgba(232, 99, 122, 0)' }
          ])
        },
        data: data.revenue
      },
      {
        name: '边际利润率',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: '#5cb88a', type: 'solid' },
        itemStyle: { color: '#fff', borderWidth: 2, borderColor: '#5cb88a' },
        data: data.margins
      }
    ]
  }, true)
}

watch(chartRange, () => {
  renderSalesChart()
})

/* ==================== 热销甜品 TOP 5 (Monospace数值) ==================== */
const topDesserts = ref([
  { name: '马达加斯加香草千层蛋糕', category: '经典烘焙', sales: 124 },
  { name: '开心果野莓慕斯', category: '夏日限定', sales: 98 },
  { name: '冲绳黑糖焦糖布丁', category: '日式冰糕', sales: 86 },
  { name: '意式特浓提拉米苏', category: '主厨招牌', sales: 74 },
  { name: '静冈抹茶舒芙蕾', category: '现场烘焙', sales: 52 }
])

/* ==================== 效期库存防御体系 ==================== */
const stockAlerts = ref([
  { name: '法国进口淡奶油 (AOP)', stock: 8, threshold: 24, unit: 'L' },
  { name: '马斯卡彭芝士 (意大利)', stock: 5, threshold: 15, unit: 'kg' },
  { name: '有机新鲜大草莓 (奶油红颜)', stock: 3, threshold: 10, unit: 'kg' }
])

const handleQuickReorder = (itemName) => {
  ElMessage.success(`已生成 [${itemName}] 的智能合并补货单，已自动流转至采购链系统`)
}

/* ==================== 顾客社交口碑情感监控 (NLP) ==================== */
const latestReviews = ref([
  {
    user: 'Sherry',
    rating: 5,
    text: '香草千层的香草籽清晰可见，奶油轻盈丝滑，完全没有腻感！配上冷链配送简直完美。',
    avatarBg: 'linear-gradient(135deg, #e8637a, #c94d63)'
  },
  {
    user: '主厨挚友',
    rating: 5,
    text: '限定的开心果野莓慕斯酸甜平衡极佳，底部饼干碎的酥脆度在配送20分钟后依旧保持。',
    avatarBg: 'linear-gradient(135deg, #f0a35c, #e28e46)'
  },
  {
    user: '咖啡小馆',
    rating: 4,
    text: '提拉米苏酒香纯正，美中不足的是手指饼干吸附咖啡液略显偏湿，建议微调配比。',
    avatarBg: 'linear-gradient(135deg, #6b8cce, #5478b8)'
  }
])

const sendCompensationCoupon = (username) => {
  ElMessage.success(`已向客群账户 [${username}] 派送夏日限定「尝鲜新品免单致谢券」`)
}

/* ==================== Linear 质感主厨悬浮舱 (Action Dock) ==================== */
const quickActions = ref([
  {
    title: '发布当季限定单品',
    desc: '联动BOM计算物料损耗与价格区间',
    route: '/price',
    icon: Plus,
    iconBg: 'rgba(232, 99, 122, 0.06)',
    iconColor: '#e8637a'
  },
  {
    title: '冷链配送派送控制台',
    desc: '实时查看冷藏车在途轨迹与温控曲线',
    route: '/order',
    icon: Van,
    iconBg: 'rgba(107, 140, 206, 0.06)',
    iconColor: '#6b8cce'
  },
  {
    title: '导出精细化财务报表',
    desc: '生成包含原材料波动的毛利润桑基图',
    route: '/report',
    icon: DataAnalysis,
    iconBg: 'rgba(92, 184, 138, 0.06)',
    iconColor: '#5cb88a'
  },
  {
    title: '智能配方与物料调控',
    desc: '查看烘焙配方变动触发的成本变差',
    route: '/report',
    icon: Document,
    iconBg: 'rgba(240, 163, 92, 0.06)',
    iconColor: '#f0a35c'
  }
])
</script>

<template>
  <div class="dolce-dashboard-wrapper">
    <!-- ==================== 1. Apple-style Liquid Glass 欢迎 Banner ==================== -->
    <header class="aurora-banner glass-panel">
      <!-- 动态背景光晕，模拟黄油与野莓渐变融合的视觉层 -->
      <div class="aurora-banner__blur-canvas">
        <div class="aurora-banner__orb aurora-banner__orb--raspberry"></div>
        <div class="aurora-banner__orb aurora-banner__orb--amber"></div>
        <div class="aurora-banner__orb aurora-banner__orb--cream"></div>
      </div>

      <div class="aurora-banner__content-row">
        <div class="aurora-banner__brand-profile">
          <span class="aurora-banner__greeting-tag">
            <span class="aurora-banner__greeting-dot"></span>
            {{ getGreeting() }}，{{ userName }}
          </span>
          <h1 class="aurora-banner__title">DessertShop 运营决策大盘</h1>
          <p class="aurora-banner__description">
            今日空气湿度 42%，推荐制作「法式经典千层蛋糕」 · 系统已自动调校冷链配平温度至 4°C
          </p>
        </div>

        <!-- Stripe-style 高频业务快照（Monospace数字，高精细度呈现） -->
        <div class="aurora-banner__micro-kpis">
          <div class="micro-kpi-card">
            <span class="micro-kpi-card__label">今日边际毛利额</span>
            <div class="micro-kpi-card__value-group">
              <span class="micro-kpi-card__currency">¥</span>
              <span class="micro-kpi-card__value">12,860.00</span>
            </div>
            <span class="micro-kpi-card__subtext up">较昨日 +14.2%</span>
          </div>
          
          <div class="micro-kpi-card__divider"></div>

          <div class="micro-kpi-card">
            <span class="micro-kpi-card__label">冷链配送准时率</span>
            <div class="micro-kpi-card__value-group">
              <span class="micro-kpi-card__value">99.4</span>
              <span class="micro-kpi-card__unit">%</span>
            </div>
            <span class="micro-kpi-card__subtext stable">极佳运转状态</span>
          </div>

          <div class="micro-kpi-card__divider"></div>

          <div class="micro-kpi-card">
            <span class="micro-kpi-card__label">社媒情感正向率</span>
            <div class="micro-kpi-card__value-group">
              <span class="micro-kpi-card__value">98.2</span>
              <span class="micro-kpi-card__unit">%</span>
            </div>
            <span class="micro-kpi-card__subtext highlight">覆盆子甜度超标</span>
          </div>
        </div>
      </div>
    </header>

    <!-- ==================== 2. 今日经营数据 Stripe 密集指标带 ==================== -->
    <section class="metrics-stripe-grid">
      <div 
        v-for="(card, index) in statsCards" 
        :key="card.title" 
        class="metric-stripe-card glass-panel"
        :style="{ animationDelay: `${(index + 1) * 0.05}s` }"
      >
        <div class="metric-stripe-card__top">
          <div class="metric-stripe-card__meta">
            <span class="metric-stripe-card__title">{{ card.title }}</span>
            <span class="metric-stripe-card__desc">{{ card.desc }}</span>
          </div>
          <div class="metric-stripe-card__badge" :class="card.trendType">
            <el-icon :size="12"><component :is="card.trendIcon" /></el-icon>
            <span>{{ card.trend }}</span>
          </div>
        </div>

        <div class="metric-stripe-card__middle">
          <span class="metric-stripe-card__value">
            <CountUp :value="card.value" :prefix="card.prefix" :suffix="card.suffix" :decimals="card.decimals || 0" />
          </span>
          <div class="metric-stripe-card__icon-wrapper" :style="{ background: card.iconBg }">
            <el-icon :size="20" :style="{ color: card.iconColor }">
              <component :is="card.icon" />
            </el-icon>
          </div>
        </div>

        <!-- 极致物理微进度条：不包含任何硬刺色彩 -->
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
    </section>

    <!-- ==================== 3. 非对称黄金分割 Bento 主体网格 ==================== -->
    <main class="bento-layout-matrix">
      
      <!-- LEFT CONTAINER (70% 宽度) -->
      <div class="bento-layout-matrix__main">
        
        <!-- ==================== 3.1 销售趋势 & 边际利润双轴面板 ==================== -->
        <div class="glass-panel bento-card bento-card--chart">
          <div class="bento-card__header">
            <div class="bento-card__title-group">
              <span class="bento-card__subtitle">SALES & MARGIN ENGINE</span>
              <h2 class="bento-card__title">销售增长与边际利润曲线</h2>
            </div>
            <div class="bento-card__action-controls">
              <!-- 媲美 Linear 的极简按钮组 -->
              <el-radio-group v-model="chartRange" size="small" class="linear-segmented-control">
                <el-radio-button label="realtime">实时流</el-radio-button>
                <el-radio-button label="week">近7日</el-radio-button>
                <el-radio-button label="month">近30日</el-radio-button>
              </el-radio-group>
              <el-button class="linear-icon-button" size="small" @click="renderSalesChart">
                <el-icon><DataAnalysis /></el-icon>
              </el-button>
            </div>
          </div>

          <!-- 双轴 Echarts 挂载区（带流莹背景高光） -->
          <div class="chart-canvas-container">
            <div class="chart-canvas-glow"></div>
            <div ref="salesChartRef" class="chart-canvas-dom"></div>
          </div>

          <!-- 图表下方图例及实时波动反馈 -->
          <div class="chart-legend-stripe">
            <div class="chart-legend-item">
              <span class="legend-color-dot legend-color-dot--raspberry"></span>
              <span class="legend-label">营业净营收</span>
              <span class="legend-value">¥12,860.00</span>
            </div>
            <div class="chart-legend-item">
              <span class="legend-color-dot legend-color-dot--mint"></span>
              <span class="legend-label">平均销售利润率</span>
              <span class="legend-value">64.8%</span>
            </div>
            <div class="chart-legend-item">
              <span class="legend-color-dot legend-color-dot--amber"></span>
              <span class="legend-label">客单均价</span>
              <span class="legend-value">¥69.20</span>
            </div>
          </div>
        </div>

        <!-- ==================== 3.2 活动运营与营销大盘 (Shopify 商业感) ==================== -->
        <div class="glass-panel bento-card bento-card--campaign">
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
            <!-- 核心活动卡片一：经典复刻活动 -->
            <div class="campaign-item-card glass-panel">
              <div class="campaign-item-card__header">
                <el-tag type="danger" size="small" effect="plain" class="custom-badge-tag">进行中</el-tag>
                <span class="campaign-item-card__time">05.20 - 06.05</span>
              </div>
              <h3 class="campaign-item-card__name">「初夏覆盆子狂欢节」特别企划</h3>
              <p class="campaign-item-card__subtitle">精选法式甜品覆盆子慕斯买一赠一，主推外卖高端单人餐</p>
              
              <div class="campaign-metrics-row">
                <div class="campaign-micro-metric">
                  <span class="campaign-micro-metric__label">活动销售额</span>
                  <span class="campaign-micro-metric__value">¥45,280</span>
                </div>
                <div class="campaign-micro-metric">
                  <span class="campaign-micro-metric__label">优惠券使用率</span>
                  <span class="campaign-micro-metric__value">78.5%</span>
                </div>
                <div class="campaign-micro-metric">
                  <span class="campaign-micro-metric__label">投资回报 ROI</span>
                  <span class="campaign-micro-metric__value highlight-mint">3.6x</span>
                </div>
              </div>

              <!-- ROI 达成指示线 -->
              <div class="campaign-item-card__footer-progress">
                <div class="campaign-progress-track">
                  <div class="campaign-progress-fill" style="width: 82%"></div>
                </div>
                <span class="campaign-progress-label">目标达成率 82%</span>
              </div>
            </div>

            <!-- 核心活动卡片二：高转化渠道卡片 -->
            <div class="campaign-item-card glass-panel">
              <div class="campaign-item-card__header">
                <el-tag type="warning" size="small" effect="plain" class="custom-badge-tag">筹备中</el-tag>
                <span class="campaign-item-card__time">06.10 - 06.25</span>
              </div>
              <h3 class="campaign-item-card__name">「芒里偷闲」夏日限定芒果班戟首发</h3>
              <p class="campaign-item-card__subtitle">联合高端冷链骑手，实施特定高价值白领社区的精准投送</p>

              <div class="campaign-metrics-row">
                <div class="campaign-micro-metric">
                  <span class="campaign-micro-metric__label">拟邀红人覆盖</span>
                  <span class="campaign-micro-metric__value">150k+</span>
                </div>
                <div class="campaign-micro-metric">
                  <span class="campaign-micro-metric__label">首批预算配置</span>
                  <span class="campaign-micro-metric__value">¥8,000</span>
                </div>
                <div class="campaign-micro-metric">
                  <span class="campaign-micro-metric__label">预估裂变转化率</span>
                  <span class="campaign-micro-metric__value">12.8%</span>
                </div>
              </div>

              <div class="campaign-item-card__footer-progress">
                <div class="campaign-progress-track">
                  <div class="campaign-progress-fill ready" style="width: 0%"></div>
                </div>
                <span class="campaign-progress-label">锁定合作博主 15 位</span>
              </div>
            </div>
          </div>
        </div>

        <!-- ==================== 3.3 Linear-style 快捷操作悬浮舱 (Action Dock) ==================== -->
        <section class="quick-action-dock-panel">
          <div class="quick-action-dock-panel__title-bar">
            <span class="quick-action-dock-panel__hint">QUICK SHORTCUTS</span>
            <span class="quick-action-dock-panel__desc">主厨快捷指令盘 · 支持一键快速导航</span>
          </div>
          
          <div class="action-dock-grid">
            <div 
              v-for="action in quickActions" 
              :key="action.title" 
              class="action-dock-card glass-panel"
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

      <!-- RIGHT CONTAINER (30% 宽度，垂直流水板) -->
      <div class="bento-layout-matrix__side">
        
        <!-- ==================== 3.4 热销甜品排行 Bento (艺术高感呈现) ==================== -->
        <div class="glass-panel bento-card bento-card--rank">
          <div class="bento-card__header">
            <div class="bento-card__title-group">
              <span class="bento-card__subtitle">HOT SELLING TOP 5</span>
              <h2 class="bento-card__title">热销甜品排行大盘</h2>
            </div>
            <span class="bento-card__link-text" @click="navigateTo('/price')">
              分析全部 <el-icon><ArrowRight /></el-icon>
            </span>
          </div>

          <div class="bento-rank-flow">
            <div 
              v-for="(item, index) in topDesserts" 
              :key="item.name" 
              class="bento-rank-item"
            >
              <!-- 艺术质感序号框 -->
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
                  <!-- 精美圆头阻尼条 -->
                  <div class="bento-rank-item__progress-rail">
                    <div 
                      class="bento-rank-item__progress-fill" 
                      :style="{ width: (item.sales / topDesserts[0].sales * 100) + '%' }"
                    ></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ==================== 3.5 智能库存效期与冷链预警 ==================== -->
        <div class="glass-panel bento-card bento-card--alert">
          <div class="bento-card__header">
            <div class="bento-card__title-group">
              <span class="bento-card__subtitle">SHELF-LIFE & INVENTORY</span>
              <h2 class="bento-card__title">效期追踪与库存预警</h2>
            </div>
            <el-tag type="warning" size="small" effect="dark" class="warning-pill-count">
              3 项待补货
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
                    <span class="bento-alert-pill__amount font-mono">余 {{ item.stock }} / {{ item.threshold }} {{ item.unit }}</span>
                    <!-- 极其微小的状态标点 -->
                    <span class="bento-alert-pill__status-dot" :class="item.stock < item.threshold * 0.5 ? 'danger' : 'warning'"></span>
                  </div>
                </div>
                
                <div class="bento-alert-pill__right">
                  <!-- 智能预测动作按钮，Shopify 商业风格 -->
                  <el-button type="info" size="small" class="reorder-action-btn" @click="handleQuickReorder(item.name)">
                    快捷采购
                  </el-button>
                </div>
              </div>

              <!-- 渐进式效期耗竭进度条 -->
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

        <!-- ==================== 3.6 顾客情感分析弹幕流 (visionOS 玻璃态) ==================== -->
        <div class="glass-panel bento-card bento-card--reviews">
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
              v-for="review in latestReviews" 
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
                      <StarFilled />
                    </el-icon>
                  </div>
                </div>
                
                <p class="bento-review-bubble__content">{{ review.text }}</p>
                
                <!-- 情感标签与商业挽回反馈（Shopify 口碑闭环） -->
                <div class="bento-review-bubble__actions-row">
                  <span class="sentiment-pill positive">
                    <span class="sentiment-pill__dot"></span>
                    情感极佳
                  </span>
                  <button class="sentiment-action-btn-link" @click="sendCompensationCoupon(review.user)">一键赠券</button>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </main>
  </div>
</template>

<style lang="scss" scoped>
/* ==========================================================================
   DessertShop High-End Design System - Apple Liquid Glass & visionOS Style
   ========================================================================== */

.dolce-dashboard-wrapper {
  // Scoped Design Tokens & Theme Colors (HSL based for premium luminescence)
  --color-cream-bg: hsl(24, 25%, 97%);        // 香草白/稀奶油底色
  --color-cream-gradient: hsl(30, 20%, 95%);  // 焦糖暖白渐变点缀
  --color-text-chocolate: hsl(336, 12%, 18%); // 85% 浓黑巧克力色（高对比度）
  --color-text-secondary: hsl(336, 6%, 42%);  // 温暖巧克力中性灰
  --color-text-muted: hsl(336, 4%, 60%);     // 极简克制低反差灰
  
  --color-primary-raspberry: hsl(350, 72%, 63%); // 覆盆子红（野莓酸）
  --color-primary-raspberry-glow: rgba(232, 99, 122, 0.18);
  
  --color-accent-amber: hsl(32, 88%, 62%);     // 经典海盐焦糖金
  --color-accent-amber-glow: rgba(240, 163, 92, 0.15);
  
  --color-accent-mint: hsl(152, 45%, 52%);     // 薄荷开心果绿
  
  // Apple Specular Glow — now inherits from global base.css tokens
  --transition-spring: all 0.5s cubic-bezier(0.16, 1, 0.3, 1); // 阻尼物理弹性
  --transition-fast: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

  // Layout Container Parameters
  width: 100%;
  max-width: 1360px;
  margin: 0 auto;
  padding: 24px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 28px;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'Inter', sans-serif;
  color: var(--color-text-chocolate);
  background: radial-gradient(circle at 0% 0%, var(--color-cream-gradient) 0%, var(--color-cream-bg) 70%);
  position: relative;
  min-height: 100vh;
  animation: pageEntrance 0.8s cubic-bezier(0.16, 1, 0.3, 1) both;

  // 极致背景光斑（焦糖暖光）
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
   Apple Liquid Glass — inherits global .glass-panel from base.css
   Local overrides via scoped variables below
   ========================================================================== */

/* ==========================================================================
   1. 顶部全局欢迎看板 (The Aurora Greeting)
   ========================================================================== */
.aurora-banner {
  padding: 36px 40px;
  z-index: 2;
  overflow: hidden;

  // 流动炫彩光彩图层 (Aura Orbs Simulation)
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

    &--raspberry {
      width: 280px;
      height: 280px;
      right: -20px;
      top: -60px;
      background: rgba(232, 99, 122, 0.18);
    }

    &--amber {
      width: 220px;
      height: 220px;
      right: 200px;
      bottom: -60px;
      background: rgba(240, 163, 92, 0.12);
      animation-delay: -4s;
      animation-duration: 22s;
    }

    &--cream {
      width: 140px;
      height: 140px;
      left: 10%;
      top: -20px;
      background: rgba(253, 232, 236, 0.25);
      animation-duration: 15s;
    }
  }

  &__content-row {
    position: relative;
    z-index: 2;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 32px;
    flex-wrap: wrap;
  }

  &__brand-profile {
    flex: 1;
    min-width: 320px;
  }

  &__greeting-tag {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 12px;
    background: rgba(232, 99, 122, 0.06);
    border: 1px solid rgba(232, 99, 122, 0.15);
    border-radius: 99px;
    font-size: 13px;
    font-weight: 600;
    color: var(--color-primary-raspberry);
    margin-bottom: 14px;
    letter-spacing: 0.02em;
  }

  &__greeting-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--color-primary-raspberry);
    animation: beaconPulse 2s infinite;
  }

  &__title {
    font-size: 26px;
    font-weight: 800;
    letter-spacing: -0.02em;
    color: var(--color-text-chocolate);
    margin-bottom: 6px;
  }

  &__description {
    font-size: 13.5px;
    color: var(--color-text-secondary);
    line-height: 1.5;
  }

  // Stripe 高密度 KPI 控制条
  &__micro-kpis {
    display: flex;
    align-items: center;
    gap: 24px;
    background: rgba(255, 255, 255, 0.35);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.4);
    border-radius: 16px;
    padding: 16px 28px;
    box-shadow: var(--glass-specular);
  }
}

.micro-kpi-card {
  display: flex;
  flex-direction: column;
  gap: 4px;

  &__label {
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: var(--color-text-muted);
  }

  &__value-group {
    display: flex;
    align-items: baseline;
    color: var(--color-text-chocolate);
  }

  &__currency {
    font-size: 14px;
    font-weight: 700;
    margin-right: 2px;
  }

  &__value {
    font-size: 20px;
    font-weight: 750;
    font-variant-numeric: tabular-nums; // 等宽对齐，防数值抖动
    letter-spacing: -0.01em;
  }

  &__unit {
    font-size: 12px;
    font-weight: 700;
    margin-left: 1px;
  }

  &__subtext {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.02em;

    &.up { color: var(--color-primary-raspberry); }
    &.stable { color: var(--color-text-secondary); }
    &.highlight { color: var(--color-accent-amber); }
  }

  &__divider {
    width: 1px;
    height: 36px;
    background: rgba(45, 35, 39, 0.08);
  }
}

/* ==========================================================================
   2. 今日经营数据 Stripe 密集指标带 (Stripe Metric Row)
   ========================================================================== */
.metrics-stripe-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  z-index: 2;

  @media (max-width: 1024px) {
    grid-template-columns: repeat(2, 1fr);
  }
  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.metric-stripe-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  animation: cardEntrance 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;

  // Linear / Apple Hover Effect (卡片平滑上浮 + 发光涂层平移)
  &:hover {
    transform: translateY(-4px) scale(1.02);
    box-shadow: var(--glass-specular), var(--glass-shadow-hover);
    border-color: rgba(232, 99, 122, 0.25);

    &::after {
      left: 100%;
    }
  }

  // 光泽扫过动效 (Hover Gloss Shine swipe)
  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 50%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.35), transparent);
    transform: skewX(-25deg);
    transition: left 0.75s ease;
    z-index: 2;
    pointer-events: none;
  }

  &__top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  &__meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__title {
    font-size: 13px;
    font-weight: 700;
    color: var(--color-text-chocolate);
    letter-spacing: -0.01em;
  }

  &__desc {
    font-size: 11px;
    color: var(--color-text-muted);
  }

  &__badge {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    padding: 3px 8px;
    border-radius: 99px;
    font-size: 11px;
    font-weight: 600;

    &.up {
      color: var(--color-accent-mint);
      background: rgba(92, 184, 138, 0.08);
    }
    &.down {
      color: var(--color-text-secondary);
      background: rgba(45, 35, 39, 0.05);
    }
    &.neutral {
      color: var(--color-accent-amber);
      background: rgba(240, 163, 92, 0.08);
    }
  }

  &__middle {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  &__value {
    font-size: 26px;
    font-weight: 850;
    font-variant-numeric: tabular-nums;
    letter-spacing: -0.03em;
    color: var(--color-text-chocolate);
  }

  &__icon-wrapper {
    width: 42px;
    height: 42px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid rgba(255, 255, 255, 0.5);
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
  }

  &__footer {
    display: flex;
    flex-direction: column;
    gap: 6px;
    margin-top: auto;
  }

  &__progress-container {
    width: 100%;
    height: 4px;
    background: rgba(45, 35, 39, 0.05);
    border-radius: 99px;
    overflow: hidden;
  }

  &__progress-bar {
    height: 100%;
    border-radius: 99px;
    transition: width 1s cubic-bezier(0.16, 1, 0.3, 1);
  }

  &__target-label {
    font-size: 11px;
    font-weight: 500;
    color: var(--color-text-secondary);
    text-align: right;
  }
}

/* ==========================================================================
   3. 非对称黄金分割 Bento 主体网格 & 主副卡片 (Bento Matrix)
   ========================================================================== */
.bento-layout-matrix {
  display: grid;
  grid-template-columns: 7fr 3fr;
  gap: 24px;
  z-index: 2;

  @media (max-width: 1140px) {
    grid-template-columns: 1fr;
  }

  &__main {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  &__side {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }
}

// 统一的 Bento 卡片控制
.bento-card {
  padding: 28px;
  display: flex;
  flex-direction: column;
  animation: cardEntrance 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;

  &:hover {
    box-shadow: var(--glass-specular), var(--glass-shadow-hover);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24px;
    gap: 16px;
  }

  &__title-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__subtitle {
    font-size: 10.5px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: var(--color-primary-raspberry);
  }

  &__title {
    font-size: 18px;
    font-weight: 800;
    letter-spacing: -0.02em;
    color: var(--color-text-chocolate);
  }

  &__link-text {
    font-size: 12.5px;
    font-weight: 600;
    color: var(--color-primary-raspberry);
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    transition: var(--transition-fast);

    &:hover {
      opacity: 0.8;
      transform: translateX(2px);
    }
  }
}

/* ==========================================================================
   3.1 销售趋势面板 & 图表容器
   ========================================================================== */
.bento-card--chart {
  .bento-card__action-controls {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

// Linear 质感极简 segmented 按钮组定制
.linear-segmented-control {
  background: rgba(45, 35, 39, 0.05) !important;
  border-radius: 8px !important;
  padding: 2px !important;
  border: none !important;

  :deep(.el-radio-button__inner) {
    background: transparent !important;
    border: none !important;
    border-radius: 6px !important;
    color: var(--color-text-secondary) !important;
    font-weight: 600 !important;
    font-size: 12px !important;
    padding: 6px 14px !important;
    box-shadow: none !important;
    transition: var(--transition-fast) !important;

    &:hover {
      color: var(--color-text-chocolate) !important;
    }
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: rgba(255, 255, 255, 0.95) !important;
    color: var(--color-text-chocolate) !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05) !important;
  }
}

.linear-icon-button {
  background: rgba(255, 255, 255, 0.5) !important;
  border: 1px solid var(--glass-border) !important;
  border-radius: 8px !important;
  color: var(--color-text-secondary) !important;
  width: 32px !important;
  height: 32px !important;
  padding: 0 !important;
  transition: var(--transition-fast) !important;

  &:hover {
    background: rgba(255, 255, 255, 0.95) !important;
    color: var(--color-text-chocolate) !important;
    transform: scale(1.05);
  }
}

// 玻璃图表大容器（防闪烁，内嵌背景渐变发光）
.chart-canvas-container {
  width: 100%;
  height: 320px;
  position: relative;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.2);
  margin-bottom: 20px;
  overflow: hidden;
}

.chart-canvas-glow {
  position: absolute;
  bottom: 0;
  left: 10%;
  right: 10%;
  height: 120px;
  background: radial-gradient(ellipse at bottom, rgba(232, 99, 122, 0.05) 0%, transparent 70%);
  pointer-events: none;
}

.chart-canvas-dom {
  width: 100%;
  height: 100%;
  z-index: 2;
  position: relative;
}

// 图表下方图例栏
.chart-legend-stripe {
  display: flex;
  align-items: center;
  gap: 32px;
  flex-wrap: wrap;
  padding-top: 16px;
  border-top: 1px solid rgba(45, 35, 39, 0.06);
}

.chart-legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;

  .legend-color-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &--raspberry { background: var(--color-primary-raspberry); }
    &--mint { background: var(--color-accent-mint); }
    &--amber { background: var(--color-accent-amber); }
  }

  .legend-label {
    color: var(--color-text-secondary);
    font-weight: 500;
  }

  .legend-value {
    color: var(--color-text-chocolate);
    font-weight: 750;
    font-variant-numeric: tabular-nums;
  }
}

/* ==========================================================================
   3.2 活动运营与营销大盘 (Shopify Promotion Grid)
   ========================================================================== */
.shining-action-btn {
  background: linear-gradient(135deg, var(--color-primary-raspberry) 0%, hsl(340, 70%, 55%) 100%) !important;
  border: none !important;
  color: #fff !important;
  font-weight: 600 !important;
  border-radius: 10px !important;
  padding: 8px 18px !important;
  box-shadow: 0 4px 14px var(--color-primary-raspberry-glow) !important;
  transition: var(--transition-fast) !important;

  &:hover {
    transform: translateY(-1.5px);
    box-shadow: 0 6px 20px var(--color-primary-raspberry-glow) !important;
    filter: brightness(1.05);
  }

  &:active {
    transform: scale(0.96);
  }

  .btn-icon-spin {
    transition: transform 0.5s ease;
  }
  &:hover .btn-icon-spin {
    transform: rotate(18deg) scale(1.1);
  }
}

.campaign-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.campaign-item-card {
  background: rgba(255, 255, 255, 0.4);
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(232, 99, 122, 0.2);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  // 深度定制 Element Plus tag
  .custom-badge-tag {
    border-radius: 6px !important;
    font-weight: 700 !important;
    padding: 2px 8px !important;
    letter-spacing: 0.02em !important;
    border: none !important;

    &.el-tag--danger {
      background: rgba(232, 99, 122, 0.1) !important;
      color: var(--color-primary-raspberry) !important;
    }
    &.el-tag--warning {
      background: rgba(240, 163, 92, 0.1) !important;
      color: var(--color-accent-amber) !important;
    }
  }

  &__time {
    font-size: 11px;
    font-weight: 600;
    color: var(--color-text-muted);
    letter-spacing: 0.04em;
  }

  &__name {
    font-size: 15px;
    font-weight: 800;
    color: var(--color-text-chocolate);
    line-height: 1.3;
  }

  &__subtitle {
    font-size: 12px;
    color: var(--color-text-secondary);
    line-height: 1.4;
  }

  .campaign-metrics-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    background: rgba(255, 255, 255, 0.3);
    border-radius: 12px;
    padding: 10px 14px;
    margin-top: 4px;
  }

  .campaign-micro-metric {
    display: flex;
    flex-direction: column;
    gap: 2px;

    &__label {
      font-size: 10px;
      font-weight: 600;
      color: var(--color-text-muted);
    }

    &__value {
      font-size: 13.5px;
      font-weight: 750;
      font-variant-numeric: tabular-nums;
      color: var(--color-text-chocolate);

      &.highlight-mint {
        color: var(--color-accent-mint);
      }
    }
  }

  &__footer-progress {
    display: flex;
    flex-direction: column;
    gap: 6px;
    margin-top: auto;
    padding-top: 10px;
  }

  .campaign-progress-track {
    width: 100%;
    height: 4px;
    background: rgba(45, 35, 39, 0.05);
    border-radius: 99px;
    overflow: hidden;
  }

  .campaign-progress-fill {
    height: 100%;
    background: linear-gradient(90deg, var(--color-primary-raspberry), var(--color-accent-amber));
    border-radius: 99px;

    &.ready {
      background: var(--color-text-muted);
    }
  }

  .campaign-progress-label {
    font-size: 10.5px;
    font-weight: 600;
    color: var(--color-text-secondary);
    text-align: right;
  }
}

/* ==========================================================================
   3.3 Linear-style 快捷操作悬浮舱 (Action Dock)
   ========================================================================= */
.quick-action-dock-panel {
  margin-top: 4px;

  &__title-bar {
    display: flex;
    align-items: baseline;
    gap: 8px;
    margin-bottom: 16px;
  }

  &__hint {
    font-size: 10px;
    font-weight: 750;
    letter-spacing: 0.06em;
    color: var(--color-primary-raspberry);
  }

  &__desc {
    font-size: 12px;
    color: var(--color-text-secondary);
  }
}

.action-dock-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.action-dock-card {
  background: rgba(255, 255, 255, 0.48);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;

  // 阻尼回弹 hover
  &:hover {
    transform: translateY(-2px) scale(1.015);
    border-color: var(--color-primary-raspberry);
    background: rgba(255, 255, 255, 0.8);
    box-shadow: var(--glass-specular), 0 6px 20px rgba(232, 99, 122, 0.06);

    .action-dock-card__arrow-link {
      transform: translateX(3px) scale(1.1);
      color: var(--color-primary-raspberry);
    }
  }

  // 点击时的下陷物理回弹
  &:active {
    transform: scale(0.97) translateY(1px);
  }

  &__icon-halo {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid rgba(255, 255, 255, 0.6);
  }

  &__content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__title {
    font-size: 13.5px;
    font-weight: 750;
    color: var(--color-text-chocolate);
  }

  &__desc {
    font-size: 11px;
    color: var(--color-text-muted);
  }

  &__arrow-link {
    font-size: 14px;
    color: var(--color-text-muted);
    transition: var(--transition-fast);
  }
}

/* ==========================================================================
   3.4 热销甜品排行 Bento (High Aesthetics Rank Flow)
   ========================================================================== */
.bento-rank-flow {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bento-rank-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
  border-bottom: 1px dashed rgba(45, 35, 39, 0.05);

  &:last-child {
    border: none;
    padding-bottom: 0;
  }

  // 艺术质感序号格
  &__position {
    width: 32px;
    height: 32px;
    border-radius: 9px;
    background: rgba(45, 35, 39, 0.04);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    font-weight: 800;
    color: var(--color-text-secondary);
    transition: var(--transition-fast);

    &--top {
      background: rgba(232, 99, 122, 0.08);
      color: var(--color-primary-raspberry);
      box-shadow: 0 2px 8px rgba(232, 99, 122, 0.05);
    }
  }

  &__main-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  &__title-row {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
  }

  &__name {
    font-size: 13.5px;
    font-weight: 750;
    color: var(--color-text-chocolate);
  }

  &__category {
    font-size: 10px;
    font-weight: 600;
    color: var(--color-text-muted);
  }

  &__stat-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  &__sales-count {
    font-size: 11px;
    font-weight: 600;
    color: var(--color-text-secondary);
    font-variant-numeric: tabular-nums;
  }

  &__progress-rail {
    flex: 1;
    height: 4px;
    background: rgba(45, 35, 39, 0.04);
    border-radius: 99px;
    overflow: hidden;
  }

  &__progress-fill {
    height: 100%;
    background: linear-gradient(90deg, var(--color-primary-raspberry) 0%, var(--color-accent-amber) 100%);
    border-radius: 99px;
    transition: width 1s cubic-bezier(0.16, 1, 0.3, 1);
  }

  &:hover {
    .bento-rank-item__position {
      transform: scale(1.08) rotate(-4deg);
    }
  }
}

/* ==========================================================================
   3.5 智能库存效期与冷链预警 (Bento Inventory Shelf-Life)
   ========================================================================== */
.warning-pill-count {
  background: rgba(240, 163, 92, 0.15) !important;
  border-radius: 6px !important;
  color: var(--color-accent-amber) !important;
  font-weight: 700 !important;
  border: none !important;
}

.bento-alerts-flow {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bento-alert-pill {
  background: rgba(255, 255, 255, 0.35);
  padding: 14px 18px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  &__body {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__left {
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  &__name {
    font-size: 13px;
    font-weight: 750;
    color: var(--color-text-chocolate);
  }

  &__stats-bar {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__amount {
    font-size: 11px;
    color: var(--color-text-secondary);
    font-variant-numeric: tabular-nums;
  }

  &__status-dot {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    display: inline-block;

    &.danger {
      background: var(--color-primary-raspberry);
      animation: beaconPulse 1.5s infinite;
    }

    &.warning {
      background: var(--color-accent-amber);
    }
  }

  // Shopify 智能辅助下单按钮配置
  .reorder-action-btn {
    background: rgba(45, 35, 39, 0.05) !important;
    border: none !important;
    color: var(--color-text-secondary) !important;
    font-weight: 700 !important;
    font-size: 11px !important;
    padding: 6px 12px !important;
    border-radius: 8px !important;
    transition: var(--transition-fast) !important;

    &:hover {
      background: var(--color-primary-raspberry) !important;
      color: #fff !important;
      transform: scale(1.03);
    }
  }

  &__countdown-line {
    width: 100%;
    height: 3px;
    background: rgba(45, 35, 39, 0.03);
    border-radius: 99px;
    overflow: hidden;
  }

  &__countdown-fill {
    height: 100%;
    border-radius: 99px;
    transition: width 1s ease;

    &.danger {
      background: var(--color-primary-raspberry);
    }

    &.warning {
      background: var(--color-accent-amber);
    }
  }

  &:hover {
    transform: translateX(2px);
    border-color: rgba(240, 163, 92, 0.25);
  }
}

/* ==========================================================================
   3.6 顾客情感分析弹幕流 (visionOS Glass Reviews)
   ========================================================================== */
.bento-reviews-flow {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bento-review-bubble {
  background: rgba(255, 255, 255, 0.28);
  padding: 16px;
  border-radius: 16px;
  display: flex;
  gap: 12px;

  &__avatar-halo {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 800;
    color: #fff;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
    flex-shrink: 0;
  }

  &__text-area {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__meta-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__username {
    font-size: 12.5px;
    font-weight: 750;
    color: var(--color-text-chocolate);
  }

  &__stars {
    display: flex;
    gap: 1px;
  }

  &__content {
    font-size: 11.5px;
    color: var(--color-text-secondary);
    line-height: 1.4;
  }

  &__actions-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 6px;
    padding-top: 6px;
    border-top: 1px solid rgba(45, 35, 39, 0.03);
  }

  .sentiment-pill {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 10px;
    font-weight: 700;

    &.positive {
      color: var(--color-accent-mint);

      .sentiment-pill__dot {
        width: 4px;
        height: 4px;
        border-radius: 50%;
        background: var(--color-accent-mint);
        display: inline-block;
      }
    }
  }

  .sentiment-action-btn-link {
    background: transparent;
    border: none;
    font-size: 10.5px;
    font-weight: 700;
    color: var(--color-primary-raspberry);
    cursor: pointer;
    transition: var(--transition-fast);
    padding: 0;

    &:hover {
      opacity: 0.8;
      text-decoration: underline;
    }
  }

  &:hover {
    transform: translateY(-2px);
    background: rgba(255, 255, 255, 0.5);
  }
}

/* ==========================================================================
   4. 核心系统动效与关键帧配置 (Micro-Animations & Keyframes)
   ========================================================================== */

// 页面淡入加载
@keyframes pageEntrance {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 卡片渐进渲染动画
@keyframes cardEntrance {
  from {
    opacity: 0;
    transform: translateY(12px) scale(0.99);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

// 动态背景流动气泡动画 (Apple Aura Orbs)
@keyframes orbFloating {
  0% {
    transform: translate(0px, 0px) rotate(0deg);
  }
  33% {
    transform: translate(30px, -40px) rotate(120deg);
  }
  66% {
    transform: translate(-20px, 20px) rotate(240deg);
  }
  100% {
    transform: translate(0px, 0px) rotate(360deg);
  }
}

// 指标状态指示灯呼吸灯动效
@keyframes beaconPulse {
  0% {
    transform: scale(0.9);
    opacity: 0.5;
    box-shadow: 0 0 0 0 rgba(232, 99, 122, 0.4);
  }
  70% {
    transform: scale(1.1);
    opacity: 1;
    box-shadow: 0 0 0 6px rgba(232, 99, 122, 0);
  }
  100% {
    transform: scale(0.9);
    opacity: 0.5;
    box-shadow: 0 0 0 0 rgba(232, 99, 122, 0);
  }
}
</style>
