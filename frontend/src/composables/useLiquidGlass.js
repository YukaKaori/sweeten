/**
 * useLiquidGlass.js
 * Apple-style liquid glass interactive effects for Vue 3
 *
 * Provides these composables:
 *   useGlassSpotlight  — cursor-follow specular highlight on glass panels
 *   useParallaxDepth   — cursor parallax across [data-depth] layers
 *   useMagnetic        — displacement-follow magnetic pull (not tilt)
 *   useMagneticTilt    — 3D card tilt toward cursor position
 *   useGlassRipple     — expanding glass ripple on click
 */

import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useMotionPref } from '@/composables/useMotionPref'

// 粗指针（触屏）设备：磁吸/聚光一律关闭
function _isCoarsePointer() {
  return typeof window !== 'undefined' && window.matchMedia
    ? window.matchMedia('(pointer: coarse)').matches
    : false
}

// 共享 lerp：spotlight 平滑跟随 / parallax 视差均用它
function lerp(a, b, t) {
  return a + (b - a) * t
}

// ──────────────────────────────────────────────
// useGlassSpotlight
// Tracks cursor position and sets CSS vars --mx, --my
// Add class "glass-spotlight" to target elements
// ──────────────────────────────────────────────
export function useGlassSpotlight(targetRef, options = {}) {
  const {
    enabled = true,
    selector = '.glass-spotlight', // child selector; use ':self' for the target itself
    smoothing = 0.12,              // lerp factor (0 = instant, 0.15 = smooth)
  } = options

  const { allowMotion } = useMotionPref()
  const isHovering = ref(false)
  let rafId = null
  let currentX = 0.5
  let currentY = 0.5
  let targetX = 0.5
  let targetY = 0.5

  function animate() {
    if (!isHovering.value && (Math.abs(currentX - targetX) < 0.001) && (Math.abs(currentY - targetY) < 0.001)) {
      rafId = null
      return
    }

    currentX = lerp(currentX, targetX, smoothing)
    currentY = lerp(currentY, targetY, smoothing)

    const el = _resolveElement(targetRef, selector)
    if (el) {
      const rect = el.getBoundingClientRect()
      const px = currentX * rect.width
      const py = currentY * rect.height
      el.style.setProperty('--mx', px + 'px')
      el.style.setProperty('--my', py + 'px')
    }

    rafId = requestAnimationFrame(animate)
  }

  function onMove(e) {
    const el = _resolveElement(targetRef, selector)
    if (!el) return
    const rect = el.getBoundingClientRect()
    targetX = (e.clientX - rect.left) / rect.width
    targetY = (e.clientY - rect.top) / rect.height

    if (!rafId) {
      rafId = requestAnimationFrame(animate)
    }
  }

  function onEnter() {
    isHovering.value = true
    if (!rafId) {
      rafId = requestAnimationFrame(animate)
    }
  }

  function onLeave() {
    isHovering.value = false
    targetX = 0.5
    targetY = 0.5
  }

  onMounted(() => {
    // static 档（reduced-motion / 低端）或触屏 → 不点亮聚光
    if (!enabled || !allowMotion.value || _isCoarsePointer()) return
    const el = _resolveElement(targetRef, selector)
    if (!el) return

    el.addEventListener('mousemove', onMove, { passive: true })
    el.addEventListener('mouseenter', onEnter)
    el.addEventListener('mouseleave', onLeave)
  })

  onUnmounted(() => {
    if (rafId) cancelAnimationFrame(rafId)
    const el = _resolveElement(targetRef, selector)
    if (el) {
      el.removeEventListener('mousemove', onMove)
      el.removeEventListener('mouseenter', onEnter)
      el.removeEventListener('mouseleave', onLeave)
    }
  })

  return { isHovering }
}

// ──────────────────────────────────────────────
// useParallaxDepth
// 光标视差纵深：容器内所有 [data-depth] 层随光标「反向」位移，
// data-depth = 该层最大位移（px），值越大越像远景。lerp 平滑跟随，
// 光标离开容器时归位。订阅 useMotionPref：static 档与触屏（pointer:coarse）
// 不绑事件 → 各层保持 0 位移的静态分层。
// ──────────────────────────────────────────────
export function useParallaxDepth(targetRef, options = {}) {
  const {
    enabled = true,
    selector = '[data-depth]',
    smoothing = 0.08,        // 与 spotlight 同源的 lerp 平滑系数
  } = options

  const { allowMotion } = useMotionPref()
  let rafId = null
  let root = null
  let layers = []
  let currentX = 0
  let currentY = 0
  let targetX = 0
  let targetY = 0

  function animate() {
    currentX = lerp(currentX, targetX, smoothing)
    currentY = lerp(currentY, targetY, smoothing)
    for (const { el, depth } of layers) {
      // 反向位移：光标向右，层向左（远景幅度大 → 纵深感）
      el.style.transform = `translate3d(${(-currentX * depth).toFixed(2)}px, ${(-currentY * depth).toFixed(2)}px, 0)`
    }
    if (Math.abs(currentX - targetX) < 0.002 && Math.abs(currentY - targetY) < 0.002) {
      rafId = null
      return
    }
    rafId = requestAnimationFrame(animate)
  }

  function kick() {
    if (!rafId) rafId = requestAnimationFrame(animate)
  }

  function onMove(e) {
    const rect = root.getBoundingClientRect()
    // 归一化到 [-1, 1]，容器中心为原点
    targetX = ((e.clientX - rect.left) / rect.width) * 2 - 1
    targetY = ((e.clientY - rect.top) / rect.height) * 2 - 1
    kick()
  }

  function onLeave() {
    targetX = 0
    targetY = 0
    kick()
  }

  onMounted(() => {
    if (!enabled || !allowMotion.value || _isCoarsePointer()) return
    root = _resolveElement(targetRef, null)
    if (!root) return
    layers = Array.from(root.querySelectorAll(selector))
      .map((el) => ({ el, depth: parseFloat(el.dataset.depth) || 0 }))
      .filter(({ depth }) => depth !== 0)
    if (!layers.length) return
    root.addEventListener('mousemove', onMove, { passive: true })
    root.addEventListener('mouseleave', onLeave)
  })

  onUnmounted(() => {
    if (rafId) cancelAnimationFrame(rafId)
    if (root) {
      root.removeEventListener('mousemove', onMove)
      root.removeEventListener('mouseleave', onLeave)
    }
    layers.forEach(({ el }) => { el.style.transform = '' })
    layers = []
    root = null
  })
}

// ──────────────────────────────────────────────
// useMagnetic
// 位移跟随版磁吸（不是 3D 倾斜）：主体靠近光标时朝光标位移 ≤ maxDisplace，
// 轻微放大 scale，spring 回弹。可绑定容器下多个匹配元素。
// 订阅 useMotionPref：static 档与触屏直接关闭。
// ──────────────────────────────────────────────
export function useMagnetic(targetRef, options = {}) {
  const {
    enabled = true,
    selector = '.magnetic',   // 容器下的目标；':self' 表示容器本身
    maxDisplace = 6,          // 最大位移（px），克制在 ≤6
    scale = 1.03,             // 靠近时轻微放大，克制在 ≤1.03
    enterMs = 200,
    resetMs = 500,
  } = options

  const { allowMotion } = useMotionPref()
  const spring = 'cubic-bezier(0.34, 1.56, 0.64, 1)'
  const cleanups = []

  function bind(el) {
    let rafId = null
    let settleTimer = null

    const onMove = (e) => {
      const rect = el.getBoundingClientRect()
      const dx = e.clientX - (rect.left + rect.width / 2)
      const dy = e.clientY - (rect.top + rect.height / 2)
      // 归一化到 [-1,1] 并夹紧，朝光标方向位移（磁吸）
      const nx = Math.max(-1, Math.min(1, dx / (rect.width / 2)))
      const ny = Math.max(-1, Math.min(1, dy / (rect.height / 2)))
      if (!rafId) {
        rafId = requestAnimationFrame(() => {
          el.style.transform = `translate(${nx * maxDisplace}px, ${ny * maxDisplace}px) scale(${scale})`
          rafId = null
        })
      }
    }
    // will-change 只在悬停期间挂上，回弹落定后即撤（避免长期占用合成层）
    const onEnter = () => {
      clearTimeout(settleTimer)
      el.style.willChange = 'transform'
      el.style.transition = `transform ${enterMs}ms ${spring}`
    }
    const onLeave = () => {
      if (rafId) { cancelAnimationFrame(rafId); rafId = null }
      el.style.transition = `transform ${resetMs}ms ${spring}`
      el.style.transform = 'translate(0px, 0px) scale(1)'
      settleTimer = setTimeout(() => { el.style.willChange = '' }, resetMs + 50)
    }

    el.addEventListener('mousemove', onMove, { passive: true })
    el.addEventListener('mouseenter', onEnter)
    el.addEventListener('mouseleave', onLeave)
    cleanups.push(() => {
      if (rafId) cancelAnimationFrame(rafId)
      clearTimeout(settleTimer)
      el.removeEventListener('mousemove', onMove)
      el.removeEventListener('mouseenter', onEnter)
      el.removeEventListener('mouseleave', onLeave)
      el.style.transform = ''
      el.style.transition = ''
      el.style.willChange = ''
    })
  }

  onMounted(() => {
    if (!enabled || !allowMotion.value || _isCoarsePointer()) return
    const root = _resolveElement(targetRef, null)
    if (!root) return
    const els = selector === ':self' ? [root] : Array.from(root.querySelectorAll(selector))
    els.forEach(bind)
  })

  onUnmounted(() => {
    cleanups.forEach((fn) => fn())
  })
}

// ──────────────────────────────────────────────
// useMagneticTilt
// Cards tilt in 3D toward cursor position
// ──────────────────────────────────────────────
export function useMagneticTilt(targetRef, options = {}) {
  const {
    enabled = true,
    selector = '.magnetic-hover',
    maxTilt = 6,            // max degrees of tilt
    perspective = 800,
    scale = 1.02,           // subtle grow on hover
    resetDuration = 600,    // ms
  } = options

  let rafId = null
  let targetEl = null

  function onMove(e) {
    if (!targetEl) return
    const rect = targetEl.getBoundingClientRect()
    const centerX = rect.left + rect.width / 2
    const centerY = rect.top + rect.height / 2
    const deltaX = (e.clientX - centerX) / (rect.width / 2)
    const deltaY = (e.clientY - centerY) / (rect.height / 2)

    const tiltY = deltaX * maxTilt
    const tiltX = -deltaY * maxTilt

    if (!rafId) {
      rafId = requestAnimationFrame(() => {
        if (targetEl) {
          targetEl.style.setProperty('--tilt-x', tiltX + 'deg')
          targetEl.style.setProperty('--tilt-y', tiltY + 'deg')
          targetEl.style.transform = `perspective(${perspective}px) rotateX(${tiltX}deg) rotateY(${tiltY}deg) scale3d(${scale}, ${scale}, 1)`
        }
        rafId = null
      })
    }
  }

  function onLeave() {
    if (!targetEl) return
    targetEl.style.transition = `transform ${resetDuration}ms cubic-bezier(0.34, 1.56, 0.64, 1)`
    targetEl.style.transform = `perspective(${perspective}px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)`
    targetEl.style.removeProperty('--tilt-x')
    targetEl.style.removeProperty('--tilt-y')

    setTimeout(() => {
      if (targetEl) {
        targetEl.style.transition = ''
      }
    }, resetDuration)
  }

  function onEnter() {
    if (!targetEl) return
    targetEl.style.transition = 'transform 0.15s ease'
  }

  onMounted(() => {
    if (!enabled) return
    targetEl = _resolveElement(targetRef, selector)
    if (!targetEl) return

    targetEl.addEventListener('mousemove', onMove, { passive: true })
    targetEl.addEventListener('mouseenter', onEnter)
    targetEl.addEventListener('mouseleave', onLeave)
  })

  onUnmounted(() => {
    if (targetEl) {
      targetEl.removeEventListener('mousemove', onMove)
      targetEl.removeEventListener('mouseenter', onEnter)
      targetEl.removeEventListener('mouseleave', onLeave)
    }
  })
}

// ──────────────────────────────────────────────
// useGlassRipple
// Expands a glass ripple from the click point
// ──────────────────────────────────────────────
export function useGlassRipple(targetRef, options = {}) {
  const {
    enabled = true,
    selector = '.glass-ripple',
    color = 'rgba(255, 255, 255, 0.5)',
    duration = 800,
    maxSize = 300,  // px diameter at peak
  } = options

  function onClick(e) {
    const el = _resolveElement(targetRef, selector)
    if (!el) return

    const rect = el.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top

    const wave = document.createElement('span')
    wave.className = 'glass-ripple__wave'
    wave.style.left = x + 'px'
    wave.style.top = y + 'px'
    wave.style.width = maxSize + 'px'
    wave.style.height = maxSize + 'px'
    wave.style.background = color
    wave.style.animationDuration = duration + 'ms'

    el.appendChild(wave)

    wave.addEventListener('animationend', () => {
      wave.remove()
    })
  }

  onMounted(() => {
    if (!enabled) return
    const el = _resolveElement(targetRef, selector)
    if (!el) return
    el.addEventListener('click', onClick)
  })

  onUnmounted(() => {
    const el = _resolveElement(targetRef, selector)
    if (el) {
      el.removeEventListener('click', onClick)
    }
  })
}

// ──────────────────────────────────────────────
// helper: resolve element from ref or selector
// ──────────────────────────────────────────────
function _resolveElement(targetRef, selector) {
  // targetRef is a template ref (Ref<HTMLElement>) or an element
  const raw = targetRef?.value ?? targetRef
  if (raw instanceof HTMLElement) {
    if (selector === ':self') return raw
    if (selector) return raw.querySelector(selector)
    return raw
  }
  if (typeof raw === 'string') {
    return document.querySelector(raw)
  }
  return null
}
