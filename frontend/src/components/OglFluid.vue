<script setup>
/**
 * OglFluid.vue — 登录页 WebGL 真流体渐变（第二波）
 * ───────────────────────────────────────────────────────────────
 * OGL（~8KB）跑一个片元着色器：simplex noise 域扭曲（domain warp）驱动的
 * 三色 mesh，像融化糖浆 / 熔金在缓慢呼吸。
 *   · DPR ≤ 1.5，页面失焦暂停 rAF（省电）
 *   · 配色取自 base.css 品牌令牌：浅色 玫瑰/琥珀/抹茶 · 暗色 金/琥珀/香槟
 *   · 本组件由 FluidGradient 动态 import() 懒加载 → ogl 不进主 bundle
 *   · 光标感知：uMouse/uMouseStrength 让光标经过处产生轻微「推开」扰动
 *     （高斯衰减的 domain 位移，像水面被风吹过）；位置 lerp 平滑，
 *     长时间不动 / 窗口失焦 / 光标离开 → 强度自然衰减到 0。触屏不绑定。
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { Renderer, Program, Mesh, Triangle } from 'ogl'
import { brandRgb } from '@/utils/brandTokens'

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
  uniform vec2 uMouse;          // 光标位置（0..1，y 向上，未做纵横比修正）
  uniform float uMouseStrength; // 扰动强度包络（0..1，JS 侧平滑起落）
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
    // 保持长宽比，避免拉伸
    vec2 uv = vUv;
    float aspect = uRes.x / uRes.y;
    uv.x *= aspect;
    float t = uTime * 0.06;

    // 光标「推开」扰动：以光标为中心的高斯衰减 domain 位移。
    // 力度刻意很轻（≤0.05 uv 单位），像水面被风吹过，不是被搅动。
    vec2 m = uMouse;
    m.x *= aspect;
    vec2 dm = uv - m;
    float md = length(dm);
    float influence = exp(-md * md * 10.0) * uMouseStrength;
    uv += (md > 0.001 ? dm / md : vec2(0.0)) * influence * 0.05;

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

    gl_FragColor = vec4(col, 1.0);
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
      base: [0.055, 0.047, 0.039],
      c1: toVec('gold', [0.831, 0.655, 0.278]),
      c2: toVec('amber', [0.878, 0.690, 0.408]),
      c3: toVec('gold-bright', [0.941, 0.788, 0.424]),
    }
  }
  // 浅色：奶油底 + 玫瑰 / 琥珀 / 抹茶
  return {
    base: [0.992, 0.969, 0.949],
    c1: toVec('rose', [0.910, 0.388, 0.478]),
    c2: toVec('amber', [0.941, 0.639, 0.361]),
    c3: toVec('matcha', [0.361, 0.722, 0.541]),
  }
}

// ── 光标扰动状态（JS 侧做 lerp 平滑与强度包络，shader 只读结果） ──
const MOUSE_IDLE_MS = 1500  // 光标静止超过此时长 → 扰动开始衰减
const mouse = {
  x: 0.5, y: 0.5,       // 平滑后的位置
  tx: 0.5, ty: 0.5,     // 目标位置
  strength: 0,          // 平滑后的强度
  target: 0,            // 目标强度（0 或 1）
  lastMove: 0,
}

function lerp(a, b, t) {
  return a + (b - a) * t
}

function loop(t) {
  if (!running) return
  program.uniforms.uTime.value = t * 0.001

  // 静止超时 → 目标强度归 0（自然衰减，而非硬切）
  if (mouse.target > 0 && performance.now() - mouse.lastMove > MOUSE_IDLE_MS) {
    mouse.target = 0
  }
  mouse.x = lerp(mouse.x, mouse.tx, 0.07)
  mouse.y = lerp(mouse.y, mouse.ty, 0.07)
  // 起势稍快、衰减更慢 → 像风过水面后余波慢慢平复
  mouse.strength = lerp(mouse.strength, mouse.target, mouse.target > mouse.strength ? 0.06 : 0.025)
  program.uniforms.uMouse.value[0] = mouse.x
  program.uniforms.uMouse.value[1] = mouse.y
  program.uniforms.uMouseStrength.value = mouse.strength

  renderer.render({ scene: mesh })
  rafId = requestAnimationFrame(loop)
}

function start() {
  if (running) return
  running = true
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

  const resize = () => {
    const w = canvas.parentElement?.clientWidth || window.innerWidth
    const h = canvas.parentElement?.clientHeight || window.innerHeight
    renderer.setSize(w, h)
    if (program) program.uniforms.uRes.value = [gl.canvas.width, gl.canvas.height]
  }

  const { base, c1, c2, c3 } = readColors()
  const geometry = new Triangle(gl)
  program = new Program(gl, {
    vertex,
    fragment,
    uniforms: {
      uTime: { value: 0 },
      uRes: { value: [gl.canvas.width, gl.canvas.height] },
      uMouse: { value: [0.5, 0.5] },
      uMouseStrength: { value: 0 },
      uBase: { value: base },
      uColor1: { value: c1 },
      uColor2: { value: c2 },
      uColor3: { value: c3 },
    },
  })
  mesh = new Mesh(gl, { geometry, program })

  resize()
  window.addEventListener('resize', resize)
  cleanupFns.push(() => window.removeEventListener('resize', resize))

  // 页面失焦暂停 rAF（省电），回到前台恢复
  const onVisibility = () => (document.hidden ? stop() : start())
  document.addEventListener('visibilitychange', onVisibility)
  cleanupFns.push(() => document.removeEventListener('visibilitychange', onVisibility))

  // 光标感知：canvas 自身 pointer-events:none（在装饰层里），监听挂 window。
  // 触屏（pointer:coarse）无悬停光标 → 不绑定，流体保持自呼吸。
  const coarse = window.matchMedia && window.matchMedia('(pointer: coarse)').matches
  if (!coarse) {
    const onMouseMove = (e) => {
      const rect = canvas.getBoundingClientRect()
      if (!rect.width || !rect.height) return
      mouse.tx = (e.clientX - rect.left) / rect.width
      mouse.ty = 1 - (e.clientY - rect.top) / rect.height  // uv 的 y 向上
      mouse.target = 1
      mouse.lastMove = performance.now()
    }
    // 失焦 / 光标离开窗口 → 扰动衰减到 0
    const calmDown = () => { mouse.target = 0 }
    window.addEventListener('mousemove', onMouseMove, { passive: true })
    window.addEventListener('blur', calmDown)
    document.documentElement.addEventListener('mouseleave', calmDown)
    cleanupFns.push(() => {
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('blur', calmDown)
      document.documentElement.removeEventListener('mouseleave', calmDown)
    })
  }

  start()
})

onUnmounted(() => {
  stop()
  cleanupFns.forEach((fn) => fn())
  cleanupFns = []
  // 释放 WebGL 上下文
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
