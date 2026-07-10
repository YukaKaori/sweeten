<script setup>
/**
 * FluidGradient.vue — 「静谧揭示」背景（quiet reveal）
 * ───────────────────────────────────────────────────────────────
 * 概念：默认状态是干净安静的底色（--bg-canvas + 极淡的、几乎不可察觉的
 * 大尺度渐变）；只有光标周围一圈揭示区内，流体渐变才被「揭示」出来。
 * 静止时页面近乎无声，动起来才发现别有洞天。
 *
 * 三档策略：
 *   full   — OGL WebGL 真流体，揭示遮罩在 shader 内实现（见 OglFluid.vue），
 *            组件懒加载，ogl 不进主 bundle
 *   lite   — CSS 简化版：mesh 色团层被 radial-gradient mask 圈出，
 *            JS 驱动 --rx/--ry/--rr 跟随光标（lerp 惰性）、静止 4s 淡出、
 *            离开窗口淡出；触屏改 20s 周期自动游走
 *   static — 纯干净底色（+ 静态微渐变与颗粒），无揭示效果，页面依然耐看
 */
import { computed, defineAsyncComponent, onMounted, onUnmounted, ref } from 'vue'
import { useMotionPref } from '@/composables/useMotionPref'

const props = defineProps({
  // 是否播放动画。父组件按 useMotionPref 的 tier 传入（static 档传 false）
  animated: { type: Boolean, default: true },
  // 是否请求 WebGL 真流体（能力不足时自动回退 CSS 揭示）
  webgl: { type: Boolean, default: false },
  // 首次进入时揭示区自动「点亮-展开-收拢」引导（full/lite 均支持）
  intro: { type: Boolean, default: false },
  // 引导动画延迟（ms），与登录页入场编排对齐
  introDelay: { type: Number, default: 800 },
})

const { isFull, hasWebGL } = useMotionPref()

// 仅 full 档 + 支持 WebGL + 请求 webgl 且允许动画时启用 OGL
const useWebGL = computed(() => props.webgl && props.animated && isFull.value && hasWebGL)

// OGL 组件懒加载：动态 import → 独立 chunk
const OglFluid = defineAsyncComponent(() => import('./OglFluid.vue'))

// ── lite 档 CSS 揭示（full 档由 shader 接管；static 档 animated=false 不启用） ──
const cssReveal = computed(() => props.animated && !useWebGL.value)
const meshRef = ref(null)

const R_BASE = 240
const IDLE_MS = 4000
const INTRO_MS = 1500
const st = {
  x: 0.5, y: 0.45, tx: 0.5, ty: 0.45, // 归一化位置（CSS 坐标，y 向下）
  s: 0, ts: 0,                        // 强度（→ mesh opacity）
  lastMove: 0,
}
let coarse = false
let introStart = 0
let rafId = null
let cleanups = []

const lerp = (a, b, t) => a + (b - a) * t

function frame() {
  const el = meshRef.value
  if (!el) { rafId = null; return }
  const now = performance.now()
  const rect = el.getBoundingClientRect()
  let rScale = 1
  const introP = introStart ? (now - introStart) / INTRO_MS : 1
  if (introStart && introP < 1 && st.lastMove < introStart) {
    // 引导「点亮-展开-收拢」：品牌名附近；光标一动立即让位
    st.tx = 0.5
    st.ty = 0.42
    st.s = introP < 0.25 ? introP / 0.25 : introP < 0.6 ? 1 : 1 - (introP - 0.6) / 0.4
    rScale = introP < 0.55 ? 0.3 + 0.85 * (introP / 0.55) : 1.15 - 0.65 * ((introP - 0.55) / 0.45)
    st.x = lerp(st.x, st.tx, 0.12)
    st.y = lerp(st.y, st.ty, 0.12)
  } else {
    if (introStart) { introStart = 0; if (!coarse) st.ts = 0 }
    if (coarse) {
      // 触屏：20s 周期平滑游走
      const w = (now / 1000) * (Math.PI * 2 / 20)
      st.tx = 0.5 + 0.30 * Math.sin(w)
      st.ty = 0.45 + 0.20 * Math.sin(w * 1.6 + 1.3)
      st.ts = 0.9
    } else if (st.ts > 0 && now - st.lastMove > IDLE_MS) {
      st.ts = 0 // 静止 4s → 缓慢淡出
    }
    st.x = lerp(st.x, st.tx, coarse ? 0.03 : 0.08)
    st.y = lerp(st.y, st.ty, coarse ? 0.03 : 0.08)
    st.s = lerp(st.s, st.ts, st.ts > st.s ? 0.09 : 0.016)
  }
  el.style.setProperty('--rx', `${(st.x * rect.width).toFixed(1)}px`)
  el.style.setProperty('--ry', `${(st.y * rect.height).toFixed(1)}px`)
  el.style.setProperty('--rr', `${(R_BASE * rScale * (0.55 + 0.45 * st.s)).toFixed(1)}px`)
  el.style.opacity = st.s.toFixed(3)
  // 完全安静时休眠 rAF（事件到来再唤醒）
  if (!coarse && !introStart && st.s < 0.002 && st.ts === 0) { rafId = null; return }
  rafId = requestAnimationFrame(frame)
}

function kick() {
  if (!rafId) rafId = requestAnimationFrame(frame)
}

onMounted(() => {
  if (!cssReveal.value) return
  coarse = window.matchMedia && window.matchMedia('(pointer: coarse)').matches

  if (!coarse) {
    const onMove = (e) => {
      const el = meshRef.value
      if (!el) return
      const rect = el.getBoundingClientRect()
      if (!rect.width || !rect.height) return
      st.tx = (e.clientX - rect.left) / rect.width
      st.ty = (e.clientY - rect.top) / rect.height
      st.ts = 1
      st.lastMove = performance.now()
      kick()
    }
    const calmDown = () => { st.ts = 0; kick() }
    window.addEventListener('mousemove', onMove, { passive: true })
    window.addEventListener('blur', calmDown)
    document.documentElement.addEventListener('mouseleave', calmDown)
    cleanups.push(() => {
      window.removeEventListener('mousemove', onMove)
      window.removeEventListener('blur', calmDown)
      document.documentElement.removeEventListener('mouseleave', calmDown)
    })
  }

  // 页面隐藏暂停，回前台恢复
  const onVisibility = () => { if (document.hidden) { if (rafId) { cancelAnimationFrame(rafId); rafId = null } } else kick() }
  document.addEventListener('visibilitychange', onVisibility)
  cleanups.push(() => document.removeEventListener('visibilitychange', onVisibility))

  if (props.intro && !coarse) {
    const timer = setTimeout(() => {
      if (performance.now() - st.lastMove > 1200 || !st.lastMove) {
        introStart = performance.now()
        kick()
      }
    }, props.introDelay)
    cleanups.push(() => clearTimeout(timer))
  }

  if (coarse) kick()
})

onUnmounted(() => {
  if (rafId) { cancelAnimationFrame(rafId); rafId = null }
  cleanups.forEach((fn) => fn())
  cleanups = []
})
</script>

<template>
  <div class="fluid-gradient" aria-hidden="true">
    <!-- 静谧底：干净底色 + 极淡大尺度渐变（所有档位的默认状态，::before 实现） -->

    <!-- lite 档揭示层：mesh 色团被 radial-gradient mask 圈出，JS 驱动跟随光标 -->
    <div v-if="cssReveal" ref="meshRef" class="fluid-gradient__mesh">
      <span class="fluid-gradient__blob fluid-gradient__blob--rose"></span>
      <span class="fluid-gradient__blob fluid-gradient__blob--amber"></span>
      <span class="fluid-gradient__blob fluid-gradient__blob--matcha"></span>
    </div>

    <!-- 颗粒噪声：静态材质，静止时也给底色一点质感 -->
    <div class="fluid-gradient__grain"></div>

    <!-- WebGL 真流体（full 档）：揭示遮罩在 shader 内实现 -->
    <component
      :is="OglFluid"
      v-if="useWebGL"
      class="fluid-gradient__webgl"
      :intro="intro"
      :intro-delay="introDelay"
    />
  </div>
</template>

<style scoped>
.fluid-gradient {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: var(--bg-canvas, #fdf7f2);
  pointer-events: none;
  z-index: 0;
}

/* 极淡的大尺度渐变：几乎不可察觉的暖意，保证「安静」不等于「死白」 */
.fluid-gradient::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(120% 90% at 12% 0%, rgba(var(--rose-rgb), 0.04), transparent 62%),
    radial-gradient(130% 100% at 100% 100%, rgba(var(--amber-rgb), 0.045), transparent 60%);
}

/* ── lite 档揭示层 ──
   mesh 色团整层被 radial-gradient mask 圈出：圆心 --rx/--ry、半径 --rr，
   52%→100% 的渐变停靠即羽化（≈半径的一半，绝无生硬边界）。
   opacity 由 JS 包络驱动（入场引导 / 静止淡出 / 离开窗口淡出）。 */
.fluid-gradient__mesh {
  position: absolute;
  inset: -10%;
  opacity: 0;
  -webkit-mask-image: radial-gradient(circle var(--rr, 240px) at var(--rx, 50%) var(--ry, 45%), #000 52%, transparent 100%);
  mask-image: radial-gradient(circle var(--rr, 240px) at var(--rx, 50%) var(--ry, 45%), #000 52%, transparent 100%);
  -webkit-mask-repeat: no-repeat;
  mask-repeat: no-repeat;
}

/* 每团用软性 radial-gradient（自带柔边），仅 transform 漂移 → GPU 合成 */
.fluid-gradient__blob {
  position: absolute;
  display: block;
  width: 62%;
  height: 62%;
  border-radius: 50%;
  animation: fluid-drift 22s ease-in-out infinite alternate;
}

.fluid-gradient__blob--rose {
  top: -8%;
  left: -6%;
  background: radial-gradient(circle at 50% 50%, var(--mesh-1) 0%, transparent 68%);
  animation-duration: 24s;
}

.fluid-gradient__blob--amber {
  top: -12%;
  right: -8%;
  left: auto;
  background: radial-gradient(circle at 50% 50%, var(--mesh-2) 0%, transparent 68%);
  animation-duration: 28s;
  animation-delay: -6s;
}

.fluid-gradient__blob--matcha {
  bottom: -14%;
  left: 4%;
  top: auto;
  width: 70%;
  height: 70%;
  background: radial-gradient(circle at 50% 50%, var(--mesh-3) 0%, transparent 70%);
  animation-duration: 32s;
  animation-delay: -12s;
}

/* SVG feTurbulence 噪声贴层：静态材质，压色带 + 质感 */
.fluid-gradient__grain {
  position: absolute;
  inset: 0;
  opacity: var(--mesh-noise-opacity, 0.05);
  mix-blend-mode: soft-light;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 140px 140px;
  pointer-events: none;
}

.fluid-gradient__webgl {
  position: absolute;
  inset: 0;
}

/* 双保险：系统级 reduced-motion 直接冻结漂移（揭示层 JS 侧已由 animated=false 关闭） */
@media (prefers-reduced-motion: reduce) {
  .fluid-gradient__blob { animation: none; }
}

@keyframes fluid-drift {
  0% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(4%, 6%, 0) scale(1.08); }
  100% { transform: translate3d(-3%, -4%, 0) scale(1.04); }
}
</style>
