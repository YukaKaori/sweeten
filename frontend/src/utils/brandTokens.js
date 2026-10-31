/**
 * brandTokens.js — 图表配色桥接层
 *
 * base.css 定义 --chart-1-rgb … --chart-6-rgb（浅色 / 可可夜各一套）；Canvas 与 ECharts
 * 不能直接使用 CSS var()，这里用 getComputedStyle 读出 RGB 三元组，再拼成 rgb()/rgba()。
 * 改色只改 base.css 一处，JS 侧自动同步。
 */

const KEYS = {
  cocoa: 1,
  strawberry: 2,
  milktea: 3,
  sage: 4,
  mist: 5,
  honey: 6,
}

// 旧调用方的色名 → 新调色板
const ALIASES = {
  ink: 'cocoa',
  rose: 'strawberry',
  violet: 'milktea',
  matcha: 'sage',
  blueberry: 'mist',
  amber: 'honey',
  gold: 'honey',
  'gold-bright': 'honey',
}

// 读不到 CSS 变量时的兜底（与 base.css 浅色值一致）
const FALLBACK_RGB = {
  1: '73, 59, 53',
  2: '214, 136, 132',
  3: '196, 165, 128',
  4: '121, 150, 114',
  5: '118, 141, 171',
  6: '205, 160, 90',
}

/** 读取某个品牌图表色的 RGB 三元组字符串，如 "73, 59, 53" */
export function brandRgb(key) {
  const name = ALIASES[key] || key
  const index = KEYS[name] || 1
  if (typeof window === 'undefined' || !window.getComputedStyle) return FALLBACK_RGB[index]
  const v = getComputedStyle(document.documentElement).getPropertyValue(`--chart-${index}-rgb`).trim()
  return v || FALLBACK_RGB[index]
}

/**
 * 组合品牌色为 css 颜色字符串
 * @param {string} key cocoa | strawberry | milktea | sage | mist | honey（兼容旧色名）
 * @param {number} alpha 0–1
 */
export function brand(key, alpha = 1) {
  const rgb = brandRgb(key)
  return alpha >= 1 ? `rgb(${rgb})` : `rgba(${rgb}, ${alpha})`
}

/** 图表系列默认顺序（构建图表时调用，确保读到当前主题的值） */
export function chartPalette() {
  return ['strawberry', 'cocoa', 'milktea', 'sage', 'mist', 'honey'].map((k) => brand(k))
}

/** 兼容旧接口 */
export function brandPalette() {
  return Object.fromEntries(Object.keys(KEYS).map((k) => [k, brand(k)]))
}
