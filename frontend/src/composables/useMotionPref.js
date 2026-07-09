/**
 * useMotionPref.js
 * ───────────────────────────────────────────────────────────────
 * 统一的「动效降级」信号源。所有炫技特效都应订阅这里的 tier，
 * 而不是各自判断 prefers-reduced-motion / 设备性能。
 *
 * tier 三档：
 *   'full'    — 全特效：WebGL/流体、辉光、编排入场、粒子
 *   'lite'    — 轻量：保留位移/透明度过渡，关闭昂贵的持续动画与重辉光
 *   'static'  — 静止：尊重 prefers-reduced-motion，一切瞬时到位
 *
 * 判定依据：
 *   - prefers-reduced-motion: reduce      → 'static'（最高优先级）
 *   - 低端设备（核数 <4 / 内存 <4G / 无 WebGL）→ 'lite'
 *   - 其余                                  → 'full'
 *
 * reduced-motion 的变化会被实时监听并更新 tier（模块级单例，全局共享）。
 */

import { ref, computed, readonly } from 'vue'

// ── 一次性能力探测（不随时间变化的部分） ──────────────
function detectWebGL() {
  if (typeof document === 'undefined') return true
  try {
    const canvas = document.createElement('canvas')
    return !!(
      window.WebGLRenderingContext &&
      (canvas.getContext('webgl') || canvas.getContext('experimental-webgl'))
    )
  } catch {
    return false
  }
}

function detectLowEndDevice() {
  if (typeof navigator === 'undefined') return false
  const cores = navigator.hardwareConcurrency
  const memory = navigator.deviceMemory // 仅部分浏览器提供
  const lowCores = typeof cores === 'number' && cores > 0 && cores < 4
  const lowMemory = typeof memory === 'number' && memory > 0 && memory < 4
  const noWebGL = !detectWebGL()
  return lowCores || lowMemory || noWebGL
}

// 能力在会话内视为固定
const hasWebGL = detectWebGL()
const isLowEnd = detectLowEndDevice()

// ── reduced-motion：可变，需实时监听 ─────────────────
const REDUCED_QUERY = '(prefers-reduced-motion: reduce)'
const prefersReducedMotion = ref(false)

if (typeof window !== 'undefined' && window.matchMedia) {
  const mql = window.matchMedia(REDUCED_QUERY)
  prefersReducedMotion.value = mql.matches
  const onChange = (e) => {
    prefersReducedMotion.value = e.matches
  }
  // Safari < 14 只支持 addListener
  if (mql.addEventListener) mql.addEventListener('change', onChange)
  else if (mql.addListener) mql.addListener(onChange)
}

// ── tier：派生信号，reduced-motion 变化时自动重算 ──────
const tier = computed(() => {
  if (prefersReducedMotion.value) return 'static'
  if (isLowEnd) return 'lite'
  return 'full'
})

/**
 * @returns {{
 *   tier: import('vue').ComputedRef<'full'|'lite'|'static'>,
 *   prefersReducedMotion: Readonly<import('vue').Ref<boolean>>,
 *   hasWebGL: boolean,
 *   isLowEnd: boolean,
 *   isFull: import('vue').ComputedRef<boolean>,
 *   isStatic: import('vue').ComputedRef<boolean>,
 *   allowMotion: import('vue').ComputedRef<boolean>,
 * }}
 */
export function useMotionPref() {
  return {
    tier,
    prefersReducedMotion: readonly(prefersReducedMotion),
    hasWebGL,
    isLowEnd,
    isFull: computed(() => tier.value === 'full'),
    isStatic: computed(() => tier.value === 'static'),
    // 便捷判断：是否允许「非瞬时」动效（lite / full 均允许，static 不允许）
    allowMotion: computed(() => tier.value !== 'static'),
  }
}
