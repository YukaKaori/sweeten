<script setup>
/**
 * OglFluid.vue — 登录页 WebGL「静谧揭示」流体（终极打磨轮）
 * ───────────────────────────────────────────────────────────────
 * 概念变更：不再满屏流体。默认状态是干净安静的底色（奶油白/暖黑 +
 * 几乎不可察觉的大尺度渐变），流体渐变只在光标周围一圈「揭示区」内
 * 可见——像手电筒照亮暗处、擦开雾面玻璃露出内里的色彩。
 *   · 揭示遮罩在 shader 内实现：以 uReveal 为圆心、基准 240px 半径的圆，
 *     smoothstep 羽化 ~100px 柔边；遮罩外的像素提前返回（不采样 fbm）
 *   · 圆心 lerp(0.08) 平滑跟随光标（带一点「追不上」的惰性）；
 *     移动越快半径轻微增大（上限 +15%），停下后缓慢回落；
 *     静止 >4s 揭示区缓慢收缩并淡出（~2s）；离开窗口 / 失焦优雅淡出
 *   · 首次进入：品牌名附近自动一次「点亮-展开-收拢」引导（~1.5s，
 *     由 intro/introDelay prop 编排），期间光标一动立即交还控制权
 *   · 触屏（pointer:coarse）：无光标 → 揭示区沿 20s 周期平滑路径自动游走
 *   · 时间感知：清晨(5-9)奶油粉橘 / 白天(9-17)标准 / 傍晚(17-21)焦糖琥珀 /
 *     深夜(21-5)降饱和降亮 ~15%——只改颜色 uniform 的插值目标，每帧缓慢过渡
 *   · 原有「流体被光标推开」扰动保留，但乘以揭示遮罩 → 只在揭示区内发生，
 *     且力度调轻（揭示效果为主，扰动为辅）
 *   · DPR ≤ 1.5、失焦暂停 rAF；持续掉帧（均帧 >24ms）自动降 DPR=1
 *     减小羽化采样成本
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { Renderer, Program, Mesh, Triangle } from 'ogl'
import { brandRgb } from '@/utils/brandTokens'

const props = defineProps({
  // 首次进入时播放「点亮-展开-收拢」引导动画
  intro: { type: Boolean, default: false },
  // 引导动画相对组件挂载的延迟（ms），与登录页入场编排对齐
  introDelay: { type: Number, default: 800 },
})

const canvasRef = ref(null)
let renderer = null
let program = null
let mesh = null
let rafId = null
let running = false
let cleanupFns = []

const vertex = /* glsl */ `
  attribute vec2 uv;
  attribute vec2 position;
  varying vec2 vUv;
  void main() {
    vUv = uv;
    gl_Position = vec4(position, 0.0, 1.0);
  }
`

const fragment = /* glsl */ `
  precision highp float;
  varying vec2 vUv;
  uniform float uTime;
  uniform vec2 uRes;
  uniform vec2 uMouse;          // 扰动中心（0..1，y 向上）
  uniform float uMouseStrength; // 扰动强度包络（0..1，JS 侧平滑起落）
  uniform vec2 uReveal;         // 揭示圆心（0..1，y 向上）
  uniform float uRevealR;       // 揭示半径（设备像素）
  uniform float uFeather;       // 羽化宽度（设备像素）
  uniform float uRevealS;       // 揭示强度包络（0..1）
  uniform vec3 uBase;
  uniform vec3 uColor1;
  uniform vec3 uColor2;
  uniform vec3 uColor3;

  // Ashima 2D simplex noise
  vec3 mod289(vec3 x){return x-floor(x*(1.0/289.0))*289.0;}
  vec2 mod289(vec2 x){return x-floor(x*(1.0/289.0))*289.0;}
  vec3 permute(vec3 x){return mod289(((x*34.0)+1.0)*x);}
  float snoise(vec2 v){
    const vec4 C = vec4(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);
    vec2 i  = floor(v + dot(v, C.yy));
    vec2 x0 = v -   i + dot(i, C.xx);
    vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod289(i);
    vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
    vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);
    m = m*m; m = m*m;
    vec3 x = 2.0 * fract(p * C.www) - 1.0;
    vec3 h = abs(x) - 0.5;
    vec3 ox = floor(x + 0.5);
    vec3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0*a0 + h*h);
    vec3 g;
    g.x  = a0.x  * x0.x  + h.x  * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return 130.0 * dot(m, g);
  }

  float fbm(vec2 p){
    float s = 0.0, a = 0.5;
    for (int i = 0; i < 4; i++) { s += a * snoise(p); p *= 2.0; a *= 0.5; }
    return s;
  }

  void main(){
    // ── 静谧底：干净底色 + 几乎不可察觉的大尺度渐变（默认状态） ──
    vec3 clean = uBase;
    float g1 = smoothstep(0.0, 2.0, vUv.x + (1.0 - vUv.y));   // ↖ → ↘ 极淡暖调
    float g2 = smoothstep(2.0, 0.0, vUv.x + vUv.y);           // ↙ 一丝品牌色
    clean = mix(clean, uColor2, 0.030 * g1);
    clean = mix(clean, uColor1, 0.020 * g2);

    // ── 圆形揭示遮罩：像素空间距离 + smoothstep 羽化（绝无生硬边界） ──
    float dPx = length((vUv - uReveal) * uRes);
    float feather = min(uFeather, uRevealR * 0.85);
    float mask = (1.0 - smoothstep(uRevealR - feather, uRevealR, dPx)) * uRevealS;

    // 遮罩外提前返回 → fbm 采样成本只花在揭示区内（降级/省电关键）
    if (mask < 0.004) { gl_FragColor = vec4(clean, 1.0); return; }

    vec2 uv = vUv;
    float aspect = uRes.x / uRes.y;
    uv.x *= aspect;
    float t = uTime * 0.06;

    // 光标「推开」扰动：乘以遮罩 → 只在揭示区内部发生，且比上一版更轻
    vec2 m = uMouse;
    m.x *= aspect;
    vec2 dm = uv - m;
    float md = length(dm);
    float influence = exp(-md * md * 10.0) * uMouseStrength * mask;
    uv += (md > 0.001 ? dm / md : vec2(0.0)) * influence * 0.04;

    // 域扭曲：让色团像流体一样卷曲流动
    vec2 q = vec2(fbm(uv * 1.4 + vec2(0.0, t)),
                  fbm(uv * 1.4 + vec2(5.2, t * 0.8)));
    vec2 r = vec2(fbm(uv * 1.4 + 2.0 * q + vec2(1.7, 9.2) + t * 0.5),
                  fbm(uv * 1.4 + 2.0 * q + vec2(8.3, 2.8) + t * 0.4));
    float n = fbm(uv * 1.4 + 3.0 * r);

    float f1 = smoothstep(0.05, 0.75, r.x * 0.5 + 0.5);
    float f2 = smoothstep(0.15, 0.90, r.y * 0.5 + 0.5);
    float f3 = smoothstep(0.10, 0.85, n * 0.5 + 0.5);

    vec3 col = uBase;
    col = mix(col, uColor1, f1 * 0.9);
    col = mix(col, uColor2, f2 * 0.7);
    col = mix(col, uColor3, f3 * 0.6);

    gl_FragColor = vec4(mix(clean, col, mask), 1.0);
  }
`

function toVec(key, fallback) {
  const rgb = brandRgb(key)
  const parts = rgb.split(',').map((s) => parseFloat(s) / 255)
  if (parts.length === 3 && parts.every((v) => !Number.isNaN(v))) return parts
  return fallback
}

function readColors() {
  const dark = typeof document !== 'undefined' &&
    document.documentElement.getAttribute('data-theme') === 'dark'
  if (dark) {
    // 暗色：暖黑底 + 金 / 香槟琥珀 / 高光金流光
    return {
      dark,
      base: [0.055, 0.047, 0.039],
      c1: toVec('gold', [0.831, 0.655, 0.278]),
      c2: toVec('amber', [0.878, 0.690, 0.408]),
      c3: toVec('gold-bright', [0.941, 0.788, 0.424]),
    }
  }
  // 浅色：奶油底 + 玫瑰 / 琥珀 / 抹茶
  return {
    dark,
    base: [0.992, 0.969, 0.949],
    c1: toVec('rose', [0.910, 0.388, 0.478]),
    c2: toVec('amber', [0.941, 0.639, 0.361]),
    c3: toVec('matcha', [0.361, 0.722, 0.541]),
  }
}

// ── 时间感知：按本地小时调整流体色的「插值目标」（UI 不做任何提示） ──
function timeMoodColors() {
  const { dark, base, c1, c2, c3 } = readColors()
  const h = new Date().getHours()
  const mixT = (c, t, k) => c.map((v, i) => v + (t[i] - v) * k)
  const desat = (c, k) => {
    const l = 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
    return c.map((v) => v + (l - v) * k)
  }
  const dim = (c, k) => c.map((v) => v * k)
  let fn = (c) => c
  if (h >= 5 && h < 9) {
    // 清晨：奶油粉橘（暗色 → 金色偏一丝玫瑰暖）
    fn = dark ? (c) => mixT(c, [0.95, 0.62, 0.48], 0.10) : (c) => mixT(c, [1.0, 0.78, 0.66], 0.20)
  } else if (h >= 17 && h < 21) {
    // 傍晚：焦糖琥珀（暗色 → 金色更暖）
    fn = dark ? (c) => mixT(c, [0.92, 0.58, 0.22], 0.12) : (c) => mixT(c, [0.82, 0.52, 0.25], 0.22)
  } else if (h >= 21 || h < 5) {
    // 深夜：降饱和 + 降亮约 15%（暗色收敛为 10%）
    fn = dark ? (c) => dim(desat(c, 0.10), 0.90) : (c) => dim(desat(c, 0.15), 0.85)
  }
  return { base, c1: fn(c1), c2: fn(c2), c3: fn(c3) }
}

function lerp(a, b, t) {
  return a + (b - a) * t
}
const easeOutCubic = (p) => 1 - Math.pow(1 - p, 3)
const easeInOut = (p) => (p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2)

// ── 扰动状态（保留自上一轮：位置 lerp、强度包络、1.5s 静止衰减） ──
const MOUSE_IDLE_MS = 1500
const mouse = {
  x: 0.5, y: 0.5, tx: 0.5, ty: 0.5,
  strength: 0, target: 0, lastMove: 0,
}

// ── 揭示区状态机 ──
const REVEAL_R = 240        // 基准半径（CSS px）
const FEATHER = 100         // 羽化宽度（CSS px）
const REVEAL_IDLE_MS = 4000 // 静止超过 4s → 缓慢收缩淡出
const INTRO_MS = 1500
const reveal = {
  x: 0.5, y: 0.60, tx: 0.5, ty: 0.60, // 平滑位置 / 目标（uv，y 向上）
  s: 0, ts: 0,                        // 强度包络（0..1）
  boost: 0, tboost: 0,                // 速度半径增益（0..1 → +15%）
  lastMove: 0,
  lastClientX: 0, lastClientY: 0, lastMoveT: 0,
}
let coarse = false
let introStart = 0   // >0 表示引导动画进行中
let introTimer = null

// 自适应降级：持续掉帧 → 降 DPR 减小羽化采样成本
let avgFrame = 16
let lastRafT = 0
let frameCount = 0
let dprDropped = false

function loop(t) {
  if (!running) return
  const now = performance.now()
  program.uniforms.uTime.value = t * 0.001

  // ── 帧时长监控（CPU 4x throttling 保障） ──
  if (lastRafT) avgFrame = avgFrame * 0.9 + (t - lastRafT) * 0.1
  lastRafT = t
  frameCount++
  if (!dprDropped && frameCount > 90 && avgFrame > 24) {
    dprDropped = true
    renderer.dpr = 1
    resizeNow()
  }

  // ── 扰动包络（沿用上一轮） ──
  if (mouse.target > 0 && now - mouse.lastMove > MOUSE_IDLE_MS) mouse.target = 0
  mouse.x = lerp(mouse.x, mouse.tx, 0.07)
  mouse.y = lerp(mouse.y, mouse.ty, 0.07)
  mouse.strength = lerp(mouse.strength, mouse.target, mouse.target > mouse.strength ? 0.06 : 0.025)
  program.uniforms.uMouse.value[0] = mouse.x
  program.uniforms.uMouse.value[1] = mouse.y
  program.uniforms.uMouseStrength.value = mouse.strength

  // ── 揭示区状态机 ──
  const dpr = renderer.dpr
  let radius
  const introP = introStart ? (now - introStart) / INTRO_MS : 1
  if (introStart && introP < 1 && reveal.lastMove < introStart) {
    // 引导动画：品牌名附近「点亮-展开-收拢」；光标一动（lastMove 更新）立即让位
    reveal.tx = 0.5
    reveal.ty = 0.60
    reveal.x = lerp(reveal.x, reveal.tx, 0.12)
    reveal.y = lerp(reveal.y, reveal.ty, 0.12)
    reveal.s = introP < 0.25 ? easeOutCubic(introP / 0.25)
      : introP < 0.6 ? 1
      : 1 - easeInOut((introP - 0.6) / 0.4)
    const rEnv = introP < 0.55
      ? 0.3 + 0.85 * easeOutCubic(introP / 0.55)
      : 1.15 - 0.65 * easeInOut((introP - 0.55) / 0.45)
    radius = REVEAL_R * dpr * rEnv
  } else {
    if (introStart) { introStart = 0; reveal.ts = coarse ? reveal.ts : 0 } // 收拢后交还光标
    if (coarse) {
      // 触屏：20s 周期的平滑游走路径（李萨如），常亮
      const w = (now / 1000) * (Math.PI * 2 / 20)
      reveal.tx = 0.5 + 0.30 * Math.sin(w)
      reveal.ty = 0.55 + 0.20 * Math.sin(w * 1.6 + 1.3)
      reveal.ts = 0.9
    } else if (reveal.ts > 0 && now - reveal.lastMove > REVEAL_IDLE_MS) {
      reveal.ts = 0 // 静止 4s → 缓慢收缩淡出（下方慢系数 ≈2s）
    }
    reveal.x = lerp(reveal.x, reveal.tx, coarse ? 0.03 : 0.08)
    reveal.y = lerp(reveal.y, reveal.ty, coarse ? 0.03 : 0.08)
    reveal.s = lerp(reveal.s, reveal.ts, reveal.ts > reveal.s ? 0.09 : 0.016)
    // 速度增益：动得快半径微增（≤+15%），停手后缓慢回落
    if (now - reveal.lastMove > 160) reveal.tboost = 0
    reveal.boost = lerp(reveal.boost, reveal.tboost, reveal.tboost > reveal.boost ? 0.25 : 0.02)
    // 淡出时半径同步收缩（0.55 ~ 1.0 倍）
    radius = REVEAL_R * dpr * (1 + 0.15 * reveal.boost) * (0.55 + 0.45 * reveal.s)
  }
  program.uniforms.uReveal.value[0] = reveal.x
  program.uniforms.uReveal.value[1] = reveal.y
  program.uniforms.uRevealR.value = radius
  program.uniforms.uFeather.value = FEATHER * dpr
  program.uniforms.uRevealS.value = Math.max(reveal.s, 0)

  // ── 时间感知配色：uniform 每帧向目标缓慢插值 ──
  const tc = colorTargets
  for (const [uni, target] of [
    [program.uniforms.uColor1.value, tc.c1],
    [program.uniforms.uColor2.value, tc.c2],
    [program.uniforms.uColor3.value, tc.c3],
  ]) {
    uni[0] = lerp(uni[0], target[0], 0.03)
    uni[1] = lerp(uni[1], target[1], 0.03)
    uni[2] = lerp(uni[2], target[2], 0.03)
  }

  renderer.render({ scene: mesh })
  rafId = requestAnimationFrame(loop)
}

let colorTargets = { c1: [0, 0, 0], c2: [0, 0, 0], c3: [0, 0, 0] }
let resizeNow = () => {}

function start() {
  if (running) return
  running = true
  lastRafT = 0
  rafId = requestAnimationFrame(loop)
}

function stop() {
  running = false
  if (rafId) { cancelAnimationFrame(rafId); rafId = null }
}

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return

  renderer = new Renderer({
    canvas,
    dpr: Math.min(window.devicePixelRatio || 1, 1.5),
    alpha: false,
    antialias: false,
  })
  const gl = renderer.gl

  resizeNow = () => {
    const w = canvas.parentElement?.clientWidth || window.innerWidth
    const h = canvas.parentElement?.clientHeight || window.innerHeight
    renderer.setSize(w, h)
    if (program) program.uniforms.uRes.value = [gl.canvas.width, gl.canvas.height]
  }

  const moods = timeMoodColors()
  colorTargets = { c1: moods.c1, c2: moods.c2, c3: moods.c3 }
  const geometry = new Triangle(gl)
  program = new Program(gl, {
    vertex,
    fragment,
    uniforms: {
      uTime: { value: 0 },
      uRes: { value: [gl.canvas.width, gl.canvas.height] },
      uMouse: { value: [0.5, 0.5] },
      uMouseStrength: { value: 0 },
      uReveal: { value: [0.5, 0.60] },
      uRevealR: { value: REVEAL_R },
      uFeather: { value: FEATHER },
      uRevealS: { value: 0 },
      uBase: { value: moods.base },
      uColor1: { value: [...moods.c1] },
      uColor2: { value: [...moods.c2] },
      uColor3: { value: [...moods.c3] },
    },
  })
  mesh = new Mesh(gl, { geometry, program })

  resizeNow()
  window.addEventListener('resize', resizeNow)
  cleanupFns.push(() => window.removeEventListener('resize', resizeNow))

  // 页面失焦暂停 rAF（省电），回到前台恢复
  const onVisibility = () => (document.hidden ? stop() : start())
  document.addEventListener('visibilitychange', onVisibility)
  cleanupFns.push(() => document.removeEventListener('visibilitychange', onVisibility))

  coarse = window.matchMedia && window.matchMedia('(pointer: coarse)').matches
  if (!coarse) {
    const onMouseMove = (e) => {
      const rect = canvas.getBoundingClientRect()
      if (!rect.width || !rect.height) return
      const nowM = performance.now()
      // 扰动目标
      mouse.tx = (e.clientX - rect.left) / rect.width
      mouse.ty = 1 - (e.clientY - rect.top) / rect.height
      mouse.target = 1
      mouse.lastMove = nowM
      // 揭示目标 + 速度增益
      reveal.tx = mouse.tx
      reveal.ty = mouse.ty
      reveal.ts = 1
      const dt = Math.max(nowM - reveal.lastMoveT, 8)
      const speed = Math.hypot(e.clientX - reveal.lastClientX, e.clientY - reveal.lastClientY) / dt
      reveal.tboost = Math.min(speed / 1.8, 1)
      reveal.lastClientX = e.clientX
      reveal.lastClientY = e.clientY
      reveal.lastMoveT = nowM
      reveal.lastMove = nowM
    }
    // 失焦 / 光标离开窗口 → 扰动与揭示区都优雅淡出
    const calmDown = () => { mouse.target = 0; reveal.ts = 0 }
    window.addEventListener('mousemove', onMouseMove, { passive: true })
    window.addEventListener('blur', calmDown)
    document.documentElement.addEventListener('mouseleave', calmDown)
    cleanupFns.push(() => {
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('blur', calmDown)
      document.documentElement.removeEventListener('mouseleave', calmDown)
    })

    // 首次进入的引导动画（触屏不需要：游走已自证交互）
    if (props.intro) {
      introTimer = setTimeout(() => {
        // 用户已经在动光标 → 不打扰
        if (performance.now() - reveal.lastMove > 1200 || !reveal.lastMove) {
          introStart = performance.now()
        }
      }, props.introDelay)
      cleanupFns.push(() => clearTimeout(introTimer))
    }
  }

  start()
})

onUnmounted(() => {
  stop()
  cleanupFns.forEach((fn) => fn())
  cleanupFns = []
  try { renderer?.gl?.getExtension('WEBGL_lose_context')?.loseContext() } catch { /* noop */ }
  renderer = null
  program = null
  mesh = null
})
</script>

<template>
  <canvas ref="canvasRef" class="ogl-fluid" aria-hidden="true"></canvas>
</template>

<style scoped>
.ogl-fluid {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
}
</style>
