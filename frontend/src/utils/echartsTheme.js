/**
 * echartsTheme.js
 * ───────────────────────────────────────────────────────────────
 * 全站 ECharts 视觉的「单一来源」。取代散落在 useDashboard.js /
 * views/report/index.vue 各 buildXxxOption 里各自重复的颜色/字体/
 * 网格/tooltip 配置。
 *
 * 两块职责：
 *   1) registerDessertThemes(echarts) —— 通过 echarts.registerTheme
 *      注册静态结构主题 'dessert'（浅色·玫瑰/琥珀）与 'dessert-dark'
 *      （暗色·金/香槟），覆盖坐标轴/网格线/图例/tooltip 的默认配色。
 *      业务代码改为 echarts.init(el, currentChartTheme())。
 *      注：registerTheme 只在模块加载时执行一次，无法像逐帧渲染那样
 *      读取「此刻」的 data-theme——所以这里的调色板是与 base.css
 *      token 对齐的静态常量（浅色 = :root 玫瑰/琥珀系，暗色 =
 *      [data-theme=dark] 金/香槟系），而不是 getComputedStyle 现读。
 *      需要随每次渲染变化的部分（渐变描边、逐扇颜色、热力图色阶）
 *      仍用 brandTokens.brand()（见下方 gradientLineColors 等）在
 *      构建 option 时实时读取，天然响应当前主题。
 *   2) 一组可复用的样式构建函数（tooltip 玻璃化、坐标轴文字、渐变色）。
 */
import { brand } from './brandTokens'

// ── 静态调色板：与 base.css 的 :root / [data-theme="dark"] token 对齐 ──
const LIGHT = {
  colors: ['#e8637a', '#f0a35c', '#6b8cce', '#5cb88a', '#a78bfa'],
  ink: '#2d2327',
  textMuted: '#a3949b',
  axisLine: '#ede6e1',
  splitLine: '#f5f0ec',
  tooltipBg: 'rgba(255, 255, 255, 0.92)',
  tooltipBorder: 'rgba(232, 99, 122, 0.18)',
  accent: '#e8637a',
}

const DARK = {
  colors: ['#d4a747', '#f0c96c', '#8aa0d6', '#7ac898', '#b8a2e8'],
  ink: '#ede4d6',
  textMuted: '#9a8c74',
  axisLine: 'rgba(212, 167, 71, 0.16)',
  splitLine: 'rgba(237, 228, 214, 0.06)',
  tooltipBg: 'rgba(23, 19, 16, 0.85)',
  tooltipBorder: 'rgba(212, 167, 71, 0.35)',
  accent: '#d4a747',
}

function buildThemeJson(p) {
  return {
    color: p.colors,
    backgroundColor: 'transparent',
    textStyle: {},
    title: { textStyle: { color: p.ink }, subtextStyle: { color: p.textMuted } },
    line: { symbol: 'circle', symbolSize: 6, smooth: true, lineStyle: { width: 3 } },
    categoryAxis: {
      axisLine: { lineStyle: { color: p.axisLine } },
      axisTick: { show: false },
      axisLabel: { color: p.textMuted, fontSize: 11 },
      splitLine: { show: false },
    },
    valueAxis: {
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: p.textMuted, fontSize: 11 },
      splitLine: { show: true, lineStyle: { color: p.splitLine, type: 'dashed' } },
    },
    legend: { textStyle: { color: p.textMuted, fontSize: 11 } },
    visualMap: { textStyle: { color: p.textMuted } },
    tooltip: {
      backgroundColor: p.tooltipBg,
      borderColor: p.tooltipBorder,
      borderWidth: 1,
      textStyle: { color: p.ink, fontSize: 12 },
    },
    pie: { itemStyle: { borderWidth: 2 } },
    bar: { itemStyle: { borderRadius: [4, 4, 0, 0] } },
  }
}

let registered = false

/** 注册 'dessert' / 'dessert-dark' 两套主题；重复调用安全（只注册一次）。 */
export function registerDessertThemes(echarts) {
  if (registered) return
  echarts.registerTheme('dessert', buildThemeJson(LIGHT))
  echarts.registerTheme('dessert-dark', buildThemeJson(DARK))
  registered = true
}

/** 当前是否深色外观（跟随 <html data-theme="dark">） */
export function isDarkTheme() {
  return (
    typeof document !== 'undefined' &&
    document.documentElement.getAttribute('data-theme') === 'dark'
  )
}

/** 供 echarts.init(el, currentChartTheme()) 使用 */
export function currentChartTheme() {
  return isDarkTheme() ? 'dessert-dark' : 'dessert'
}

/**
 * 玻璃化 tooltip：半透明 + backdrop-filter（extraCssText）+ 品牌色左边条；
 * 暗色模式下整体描边转金色发丝，左边条随调用方传入的 accent 着色。
 * @param {boolean} dark
 * @param {string} [accent] 左边条颜色；缺省取当前主题强调色（浅玫瑰/暗金）
 */
export function glassTooltip(dark, accent) {
  const p = dark ? DARK : LIGHT
  const stripe = accent || p.accent
  return {
    backgroundColor: p.tooltipBg,
    borderColor: p.tooltipBorder,
    borderWidth: 1,
    padding: [10, 14],
    textStyle: { color: p.ink, fontSize: 12 },
    extraCssText:
      `backdrop-filter: blur(16px) saturate(180%);` +
      `-webkit-backdrop-filter: blur(16px) saturate(180%);` +
      `border-left: 3px solid ${stripe};` +
      `box-shadow: 0 8px 24px rgba(0, 0, 0, ${dark ? 0.45 : 0.1});`,
  }
}

/** 坐标轴指示线（axisPointer）颜色 —— 浅色玫瑰 / 暗色金，随 accent 可覆盖 */
export function axisPointerColor(dark, accent) {
  return accent || (dark ? brand('gold', 0.5) : brand('rose', 0.4))
}

/**
 * 营收曲线渐变描边：浅色「玫瑰→琥珀」/ 暗色「金→香槟」，糖丝辉光。
 * 实时读取 brand()（会响应当前 data-theme），故不放进静态 registerTheme。
 */
export function revenueLineGradient(dark) {
  return {
    from: dark ? brand('gold') : brand('rose'),
    to: dark ? brand('gold-bright') : brand('amber'),
    dot: dark ? brand('gold') : brand('rose'),
    shadow: dark ? 'rgba(212, 167, 71, 0.18)' : brand('rose', 0.35),
    areaKey: dark ? 'gold' : 'rose',
  }
}

/** 饼图/热力图切片边框色（浅色卡面白 / 暗色卡面暖黑），用于分隔扇区/格子 */
export function sliceBorderColor(dark) {
  return dark ? 'rgba(23, 19, 16, 0.6)' : 'rgba(255, 255, 255, 0.6)'
}

/** 品类色阶（南丁格尔玫瑰图 / 分类占比饼图共用），随主题自动切换金系变体 */
export const CATEGORY_COLOR_KEYS = ['rose', 'amber', 'blueberry', 'matcha', 'violet']
