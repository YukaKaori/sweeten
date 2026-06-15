/**
 * useLiquidGlass.js
 * Apple-style liquid glass interactive effects for Vue 3
 *
 * Provides three composables:
 *   useGlassSpotlight  — cursor-follow specular highlight on glass panels
 *   useMagneticTilt    — 3D card tilt toward cursor position
 *   useGlassRipple     — expanding glass ripple on click
 */

import { onMounted, onUnmounted, ref, watch } from 'vue'

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

  const isHovering = ref(false)
  let rafId = null
  let currentX = 0.5
  let currentY = 0.5
  let targetX = 0.5
  let targetY = 0.5

  function lerp(a, b, t) {
    return a + (b - a) * t
  }

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
    if (!enabled) return
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
