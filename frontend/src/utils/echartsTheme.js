/**
 * echartsTheme.js — 全站 ECharts 视觉的单一来源
 *
 * 1) registerDessertThemes(echarts)：注册 'sweeten'（浅色）与 'sweeten-dark'（可可夜）两套结构主题，
 *    覆盖坐标轴 / 网格线 / 图例 / tooltip 的默认配色。业务代码用 echarts.init(el, currentChartTheme())。
 *    主题名沿用旧函数名导出，调用方无需改动。
 * 2) 一组样式构建函数（tooltip、坐标轴指示线、营收折线配色）。需要随主题实时变化的颜色
 *    通过 brandTokens.brand() 在构建 option 时读取。
 */
import { brand, chartPalette } from './brandTokens'

const LIGHT = {
  ink: '#493b35',
  textMuted: '#736b64',
  axisLine: '#e6ddd1',
  splitLine: '#efe8de',
  tooltipBg: '#fffefc',
  tooltipBorder: '#e6ddd1',
}

const DARK = {
  ink: '#f2ece4',
  textMuted: '#a79d92',
  axisLine: 'rgba(242, 236, 228, 0.14)',
  splitLine: 'rgba(242, 236, 228, 0.07)',
  tooltipBg: '#2a221d',
  tooltipBorder: 'rgba(242, 236, 228, 0.14)',
}

const FONT = '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif'

function buildThemeJson(p, colors) {
  return {
    color: colors,
    backgroundColor: 'transparent',
    textStyle: { fontFamily: FONT },
    title: { textStyle: { color: p.ink }, subtextStyle: { color: p.textMuted } },
    line: { symbol: 'circle', symbolSize: 6, smooth: false, lineStyle: { width: 2 } },
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
      splitLine: { show: true, lineStyle: { color: p.splitLine } },
    },
    legend: { textStyle: { color: p.textMuted, fontSize: 12 } },
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

const LIGHT_COLORS = ['#d68884', '#493b35', '#c4a580', '#799672', '#768dab', '#cda05a']
const DARK_COLORS = ['#e6a4a1', '#ece2d6', '#cdb292', '#96b48d', '#96acc9', '#d9b26b']

let registered = false

/** 注册 'sweeten' / 'sweeten-dark' 两套主题；重复调用安全 */
export function registerDessertThemes(echarts) {
  if (registered) return
  echarts.registerTheme('sweeten', buildThemeJson(LIGHT, LIGHT_COLORS))
  echarts.registerTheme('sweeten-dark', buildThemeJson(DARK, DARK_COLORS))
  registered = true
}

/** 当前是否深色外观（跟随 <html data-theme="dark">） */
export function isDarkTheme() {
  return typeof document !== 'undefined' && document.documentElement.getAttribute('data-theme') === 'dark'
}

/** 供 echarts.init(el, currentChartTheme()) 使用 */
export function currentChartTheme() {
  return isDarkTheme() ? 'sweeten-dark' : 'sweeten'
}

/** 实色 tooltip（与卡片同材质） */
export function glassTooltip(dark) {
  const p = dark ? DARK : LIGHT
  return {
    backgroundColor: p.tooltipBg,
    borderColor: p.tooltipBorder,
    borderWidth: 1,
    padding: [10, 12],
    textStyle: { color: p.ink, fontSize: 12 },
    extraCssText: `border-radius: 10px; box-shadow: 0 8px 24px rgba(0, 0, 0, ${dark ? 0.4 : 0.08});`,
  }
}

/** 坐标轴指示线颜色 */
export function axisPointerColor(dark) {
  return dark ? 'rgba(242, 236, 228, 0.3)' : 'rgba(73, 59, 53, 0.25)'
}

/** 营收折线配色：草莓主线 + 柔和面积 */
export function revenueLineGradient() {
  return {
    line: brand('strawberry'),
    dot: brand('strawberry'),
    area: (alpha) => brand('strawberry', alpha),
  }
}

/** 饼图 / 热力图切片分隔色（卡片底色） */
export function sliceBorderColor(dark) {
  return dark ? '#221b17' : '#fffefc'
}

/** 当前主题下的系列色 */
export { chartPalette }
