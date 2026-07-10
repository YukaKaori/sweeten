/**
 * brandTokens.js
 * ───────────────────────────────────────────────────────────────
 * 品牌色的「单一来源」桥接层。
 *
 * base.css 里定义了 --rose-rgb / --amber-rgb / ... 令牌；Canvas 与 ECharts
 * 无法直接吃 CSS var()，所以这里用 getComputedStyle 读出 RGB 三元组，
 * 再拼成 rgb()/rgba() 供 JS 使用。这样「令牌在 base.css、硬编码色在 JS」
 * 的脑裂被彻底消除——改一处，浅色/暗色、CSS/JS 全同步。
 */

// 读不到 CSS 变量时的兜底（与 base.css 浅色值保持一致）
const FALLBACK_RGB = {
  rose: '232, 99, 122',
  amber: '240, 163, 92',
  matcha: '92, 184, 138',
  blueberry: '107, 140, 206',
  violet: '167, 139, 250',
  ink: '45, 35, 39',
  // 黑金令牌（仅在 [data-theme="dark"] 定义；供暗色图表金→香槟渐变读取）
  gold: '212, 167, 71',
  'gold-bright': '240, 201, 108',
}

/** 读取某个品牌色的 RGB 三元组字符串，如 "232, 99, 122" */
export function brandRgb(key) {
  const fallback = FALLBACK_RGB[key] || FALLBACK_RGB.rose
  if (typeof window === 'undefined' || !window.getComputedStyle) return fallback
  const v = getComputedStyle(document.documentElement)
    .getPropertyValue(`--${key}-rgb`)
    .trim()
  return v || fallback
}

/**
 * 组合品牌色为 css 颜色字符串。
 * @param {string} key   rose | amber | matcha | blueberry | violet | ink
 * @param {number} alpha 0–1，默认 1（不透明）
 * @returns {string} rgb() 或 rgba()
 */
export function brand(key, alpha = 1) {
  const rgb = brandRgb(key)
  return alpha >= 1 ? `rgb(${rgb})` : `rgba(${rgb}, ${alpha})`
}

/** 一次性取回一组常用品牌色（在图表构建时调用，确保读到当前主题的值） */
export function brandPalette() {
  return {
    rose: brand('rose'),
    amber: brand('amber'),
    matcha: brand('matcha'),
    blueberry: brand('blueberry'),
    violet: brand('violet'),
    ink: brand('ink'),
  }
}
