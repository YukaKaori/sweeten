<script setup>
/**
 * BloomGarden.vue — 「盛放花园」登录页背景
 * ───────────────────────────────────────────────────────────────
 * 概念：高调奶油底色上，程序化生成的矢量花朵从四角与边缘向内生长，
 * 中央留白给玻璃卡片。像法式甜品店橱窗里的花艺 —— 明亮但不刺眼。
 *
 * 全部花朵由代码参数化生成（SVG path + 渐变填充），零位图资源：
 *   rosette — 多层圆瓣蔷薇型（8/7/5 三层）
 *   daisy   — 尖瓣雏菊型（12 瓣 + 花蕊点）
 *   clover  — 简单四瓣小花
 *
 * 动效分档（useMotionPref）：
 *   full   — 绽放入场 + 常驻微摆 + 花瓣飘落 + 光标趋近绽放 + 层间视差
 *   lite   — 仅保留绽放入场（.is-anim），关闭持续动画与光标交互
 *   static — 全部瞬时到位：花朵以完全盛放的静态构图呈现
 *
 * 暗色模式：不是调暗，而是换一种画法 —— 「金线描花」。花瓣填充降为
 * 半透明暖色，轮廓线换金色，花蕊鎏金，如黑丝绒上的金线刺绣。
 * 摆动/绽放/飘落逻辑完全共用，只切换渲染颜色参数（纯 CSS 覆盖）。
 *
 * 视差：三个 [data-depth] 层（13/9/6），由登录页的 useParallaxDepth
 * 统一驱动 —— 大花位移多、远处小花位移少。
 */
import { onMounted, onUnmounted, ref } from 'vue'
import { useMotionPref } from '@/composables/useMotionPref'

const props = defineProps({
  // 二次进入（登出返回）走 0.4s 快速版入场，与卡片编排对齐
  quick: { type: Boolean, default: false },
})

const { isFull, allowMotion } = useMotionPref()

// ── 确定性随机：固定种子 → 构图稳定，又带手绘般的自然抖动 ──
function mulberry32(seed) {
  let a = seed >>> 0
  return () => {
    a |= 0
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}
const rng = mulberry32(20260710)
const fx = (n) => +n.toFixed(2)

// ── 参数化花瓣：从基部 (0,0) 指向上方 (0,-len) 的闭合贝塞尔轮廓 ──
// tip 为瓣尖宽度系数（0 → 尖瓣，接近 1 → 宽圆瓣）
function petalPath(len, hw, tip) {
  const L = -len
  const tw = hw * tip
  if (tw < 1.2) {
    // 尖瓣（雏菊）：两侧对称鼓肚，收于一点
    return `M0 0C${fx(-hw)} ${fx(L * 0.28)} ${fx(-hw)} ${fx(L * 0.72)} 0 ${fx(L)}C${fx(hw)} ${fx(L * 0.72)} ${fx(hw)} ${fx(L * 0.28)} 0 0Z`
  }
  // 圆瓣：瓣尖以二次贝塞尔收出饱满弧顶
  return `M0 0C${fx(-hw)} ${fx(L * 0.3)} ${fx(-hw * 0.97)} ${fx(L * 0.74)} ${fx(-tw)} ${fx(L * 0.92)}Q0 ${fx(L * 1.04)} ${fx(tw)} ${fx(L * 0.92)}C${fx(hw * 0.97)} ${fx(L * 0.74)} ${fx(hw)} ${fx(L * 0.3)} 0 0Z`
}

function leafPath(l, w) {
  return `M0 0Q${fx(-w)} ${fx(-l * 0.45)} 0 ${fx(-l)}Q${fx(w)} ${fx(-l * 0.55)} 0 0Z`
}

// 花茎：从花心下方伸出的轻微 S 形曲线（组内 +y 方向，由外层 rotate 指向边缘）
function stemControls(len, bend) {
  return [
    { x: 0, y: 3 },
    { x: bend, y: len * 0.3 },
    { x: -bend * 0.7, y: len * 0.62 },
    { x: bend * 0.4, y: len },
  ]
}
function stemPath(c) {
  return `M${fx(c[0].x)} ${fx(c[0].y)}C${fx(c[1].x)} ${fx(c[1].y)} ${fx(c[2].x)} ${fx(c[2].y)} ${fx(c[3].x)} ${fx(c[3].y)}`
}
function bezPoint(c, t) {
  const u = 1 - t
  return {
    x: u * u * u * c[0].x + 3 * u * u * t * c[1].x + 3 * u * t * t * c[2].x + t * t * t * c[3].x,
    y: u * u * u * c[0].y + 3 * u * u * t * c[1].y + 3 * u * t * t * c[2].y + t * t * t * c[3].y,
  }
}

// ── 三种花型原型（viewBox -50..50，实际尺寸由 --sz 缩放） ──
const TYPES = {
  rosette: {
    rings: [
      { n: 8, len: 46, hw: 17, tip: 0.5 },
      { n: 7, len: 33, hw: 14, tip: 0.5 },
      { n: 5, len: 20, hw: 11, tip: 0.45 },
    ],
    heart: 7,
  },
  daisy: {
    rings: [{ n: 12, len: 46, hw: 9, tip: 0.1 }],
    heart: 11,
    stamens: true,
  },
  clover: {
    rings: [{ n: 4, len: 42, hw: 24, tip: 0.75 }],
    heart: 8,
  },
}

// ── 构图：14 朵花，从四角与边缘向内生长，中央留白给登录卡片 ──
// rot 为花茎组旋转角：茎在组内向 +y 伸展，rotate 后指向最近的边缘
const LAYOUT = [
  { x: 7, y: 13, sz: 148, type: 'rosette', color: 'rose', rot: 140 },
  { x: 17.5, y: 5, sz: 78, type: 'daisy', color: 'blueberry', rot: 175 },
  { x: 3.5, y: 30, sz: 56, type: 'clover', color: 'amber', rot: 100, minor: true },
  { x: 90, y: 9, sz: 118, type: 'daisy', color: 'amber', rot: 215 },
  { x: 80.5, y: 4.5, sz: 70, type: 'rosette', color: 'blueberry', rot: 190 },
  { x: 96.5, y: 26, sz: 54, type: 'clover', color: 'rose', rot: 258, minor: true },
  { x: 42, y: 3, sz: 44, type: 'clover', color: 'matcha', rot: 180, minor: true },
  { x: 6, y: 86, sz: 124, type: 'daisy', color: 'rose', rot: 40 },
  { x: 16, y: 95, sz: 66, type: 'clover', color: 'blueberry', rot: 15 },
  { x: 2.5, y: 66, sz: 60, type: 'rosette', color: 'amber', rot: 88, minor: true },
  { x: 92.5, y: 84, sz: 156, type: 'rosette', color: 'rose', rot: 322 },
  { x: 82, y: 95, sz: 84, type: 'daisy', color: 'amber', rot: 350 },
  { x: 97.5, y: 62, sz: 58, type: 'clover', color: 'blueberry', rot: 272, minor: true },
  { x: 57, y: 97.5, sz: 46, type: 'clover', color: 'rose', rot: 5, minor: true },
]

// 入场次序：按离中心距离降序（边缘先开、向内推进），60-944ms 覆盖 0-1s 窗口
const rankOf = []
LAYOUT.map((l, i) => ({ i, d: Math.hypot(l.x - 50, l.y - 50) }))
  .sort((a, b) => b.d - a.d)
  .forEach((r, idx) => {
    rankOf[r.i] = idx
  })

const FLOWERS = LAYOUT.map((l, i) => {
  const spec = TYPES[l.type]
  const idx = rankOf[i]
  const headDelay = props.quick ? idx * 16 : 60 + idx * 68
  const stemDelay = props.quick ? 0 : Math.max(0, headDelay - 260)
  const leafDelay = stemDelay + (props.quick ? 100 : 300)

  // 花瓣环：层间角度错开 + 每瓣微抖（长度 ±5%、角度 ±2°、延迟 0-40ms）
  const rings = spec.rings.map((rs, ri) => {
    const step = 360 / rs.n
    const petals = []
    for (let p = 0; p < rs.n; p++) {
      petals.push({
        a: fx(p * step + (rng() * 4 - 2)),
        d: petalPath(rs.len * (0.95 + rng() * 0.1), rs.hw * (0.96 + rng() * 0.08), rs.tip),
        style: { '--pd': String(Math.round(headDelay + ri * 70 + rng() * 40)) },
      })
    }
    // 注：曾计划的“层间反向慢漂移”被移除 —— SVG 内部 transform 动画
    // 每帧强制整张 SVG 重绘，CPU 4x 下实测 -11fps；微摆已足够“活”
    return { off: fx(ri * step * 0.5 + (rng() * 8 - 4)), petals }
  })

  // 花蕊点（雏菊）
  const stamens = []
  if (spec.stamens) {
    for (let s = 0; s < 6; s++) {
      const a = (s / 6) * Math.PI * 2 + rng()
      stamens.push({ x: fx(Math.cos(a) * spec.heart * 0.62), y: fx(Math.sin(a) * spec.heart * 0.62), r: 1.3 })
    }
  }

  // 花茎 + 叶
  const stemLen = 70 + rng() * 22
  const bend = (rng() < 0.5 ? -1 : 1) * (5 + rng() * 5)
  const ctrl = stemControls(stemLen, bend)
  const leafCount = l.sz >= 65 ? 2 : 1
  const leaves = []
  for (let k = 0; k < leafCount; k++) {
    const t = k === 0 ? 0.38 + rng() * 0.1 : 0.66 + rng() * 0.1
    const pt = bezPoint(ctrl, t)
    const side = k % 2 === 0 ? 1 : -1
    leaves.push({
      tf: `translate(${fx(pt.x)} ${fx(pt.y)}) rotate(${fx(side * (48 + rng() * 20))})`,
      d: leafPath(13 + rng() * 8, 4.5 + rng() * 2.5),
      style: { '--ld': String(Math.round(leafDelay + rng() * 120)) },
    })
  }

  return {
    id: i,
    minor: !!l.minor,
    color: l.color,
    x: l.x,
    y: l.y,
    bucket: l.sz >= 110 ? 13 : l.sz >= 65 ? 9 : 6,
    stemRot: l.rot,
    stem: stemPath(ctrl),
    leaves,
    rings,
    heartR: spec.heart,
    stamens,
    style: {
      left: `${l.x}%`,
      top: `${l.y}%`,
      '--sz': String(l.sz),
      '--d': String(headDelay),
      '--sd': String(stemDelay),
      // 常驻微摆：每朵相位/周期不同（6-10s），幅度 ±0.9-1.5°
      '--sw-dur': `${(6 + rng() * 4).toFixed(1)}s`,
      '--sw-del': `${(-rng() * 6).toFixed(1)}s`,
      '--sw-amp': `${(0.9 + rng() * 0.6).toFixed(2)}deg`,
    },
  }
})

// 三个视差深度层：小花远（6px）→ 中花（9px）→ 大花近（13px，位移最多）
const layers = [
  { depth: 6, flowers: FLOWERS.filter((f) => f.bucket === 6) },
  { depth: 9, flowers: FLOWERS.filter((f) => f.bucket === 9) },
  { depth: 13, flowers: FLOWERS.filter((f) => f.bucket === 13) },
]

// ── 光标趋近绽放（仅 full 档 + 精确指针）──
// 180px 内：开得更满（scale +8%）、微亮、向光标倾身 ≤4°；离开后缓慢回落。
// 统一 rAF lerp 循环，全部收敛后休眠，mousemove 再唤醒 → 静止零开销。
const POP_R = 180
const bloomEls = new Map()
function setBloomEl(id, el) {
  if (el) bloomEls.set(id, el)
  else bloomEls.delete(id)
}
const centers = new Map()
let centersDirty = true
function computeCenters() {
  bloomEls.forEach((el, id) => {
    const r = el.getBoundingClientRect()
    centers.set(id, { x: r.left + r.width / 2, y: r.top + r.height / 2 })
  })
  centersDirty = false
}
const mouse = { x: 0, y: 0, on: false }
const pops = new Map()
let rafId = null

function popFrame() {
  rafId = null
  if (centersDirty) computeCenters()
  let busy = false
  bloomEls.forEach((el, id) => {
    const c = centers.get(id)
    if (!c) return
    let t = 0
    let lt = 0
    if (mouse.on) {
      const dx = mouse.x - c.x
      const dist = Math.hypot(dx, mouse.y - c.y)
      if (dist < POP_R) {
        t = 1 - dist / POP_R
        lt = Math.max(-1, Math.min(1, dx / POP_R)) * t * 4
      }
    }
    let s = pops.get(id)
    if (!s) {
      s = { p: 0, l: 0 }
      pops.set(id, s)
    }
    s.p += (t - s.p) * (t > s.p ? 0.14 : 0.06) // 靠近快起、远离缓落
    s.l += (lt - s.l) * 0.1
    if (Math.abs(t - s.p) > 0.002 || Math.abs(lt - s.l) > 0.02) busy = true
    el.style.setProperty('--pop', s.p < 0.004 ? '0' : s.p.toFixed(3))
    el.style.setProperty('--lean', `${Math.abs(s.l) < 0.02 ? 0 : s.l.toFixed(2)}deg`)
    // filter 只挂在正被“唤醒”的花上：静止时全场零 filter（软件合成下很贵）
    el.classList.toggle('is-pop', s.p > 0.01)
  })
  if (busy) rafId = requestAnimationFrame(popFrame)
}
function kick() {
  if (!rafId) rafId = requestAnimationFrame(popFrame)
}

// ── 花瓣飘落（仅 full 档）：每 15-25s 随机一朵花落下 1-2 瓣，全屏 ≤3 片 ──
const falls = ref([])
let fallTimer = null
let fallSeq = 0
const fallRemovals = new Set()

function spawnFall() {
  if (document.hidden) return
  const room = 3 - falls.value.length
  if (room <= 0) return
  const narrow = window.innerWidth <= 640
  const pool = FLOWERS.filter((f) => !narrow || !f.minor)
  const src = pool[Math.floor(Math.random() * pool.length)]
  const count = Math.min(room, Math.random() < 0.45 ? 2 : 1)
  for (let i = 0; i < count; i++) {
    const id = ++fallSeq
    const dur = 4200 + Math.random() * 1800
    falls.value.push({
      id,
      color: src.color,
      d: petalPath(14 + Math.random() * 8, 6 + Math.random() * 4, 0.4),
      style: {
        left: `${src.x}%`,
        top: `${src.y}%`,
        '--pf-dur': `${Math.round(dur)}ms`,
        '--pf-fall': `${Math.round(180 + Math.random() * 120)}px`,
        '--pf-x1': `${Math.round(-30 + Math.random() * 60)}px`,
        '--pf-x2': `${Math.round(-40 + Math.random() * 80)}px`,
        '--pf-rot': `${Math.round((120 + Math.random() * 140) * (Math.random() < 0.5 ? -1 : 1))}deg`,
      },
    })
    const timer = setTimeout(() => {
      fallRemovals.delete(timer)
      falls.value = falls.value.filter((p) => p.id !== id)
    }, dur + 100)
    fallRemovals.add(timer)
  }
}
function scheduleFall() {
  fallTimer = setTimeout(() => {
    spawnFall()
    scheduleFall()
  }, 15000 + Math.random() * 10000)
}

let cleanups = []
onMounted(() => {
  if (isFull.value) {
    const coarse = window.matchMedia && window.matchMedia('(pointer: coarse)').matches
    if (!coarse) {
      const onMove = (e) => {
        mouse.x = e.clientX
        mouse.y = e.clientY
        mouse.on = true
        kick()
      }
      const onLeave = () => {
        mouse.on = false
        kick()
      }
      const onResize = () => {
        centersDirty = true
      }
      window.addEventListener('mousemove', onMove, { passive: true })
      window.addEventListener('blur', onLeave)
      document.documentElement.addEventListener('mouseleave', onLeave)
      window.addEventListener('resize', onResize)
      cleanups.push(() => {
        window.removeEventListener('mousemove', onMove)
        window.removeEventListener('blur', onLeave)
        document.documentElement.removeEventListener('mouseleave', onLeave)
        window.removeEventListener('resize', onResize)
      })
    }
    scheduleFall()
    const onVis = () => {
      if (document.hidden) {
        if (rafId) {
          cancelAnimationFrame(rafId)
          rafId = null
        }
      } else kick()
    }
    document.addEventListener('visibilitychange', onVis)
    cleanups.push(() => document.removeEventListener('visibilitychange', onVis))
  }
})

onUnmounted(() => {
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = null
  }
  clearTimeout(fallTimer)
  fallRemovals.forEach((t) => clearTimeout(t))
  fallRemovals.clear()
  cleanups.forEach((fn) => fn())
  cleanups = []
})
</script>

<template>
  <div
    class="bloom-garden"
    :class="{ 'is-anim': allowMotion, 'is-live': isFull, 'is-quick': quick }"
    aria-hidden="true"
  >
    <!-- 共享渐变定义（隐藏 SVG，同文档内 url(#id) 全局可引用）：
         花瓣渐变自瓣尖(深)到基部(浅)，颜色经 CSS class 上的 stop-color 定义，
         暗色模式仅覆盖这些 class → 同一批 path 换一套“金线描花”着色 -->
    <svg class="bloom-garden__defs" width="0" height="0" focusable="false" aria-hidden="true">
      <defs>
        <linearGradient id="bgq-grad-rose" x1="0" y1="0" x2="0" y2="1">
          <stop class="bgq-gs-rose-a" offset="0" />
          <stop class="bgq-gs-rose-b" offset="1" />
        </linearGradient>
        <linearGradient id="bgq-grad-amber" x1="0" y1="0" x2="0" y2="1">
          <stop class="bgq-gs-amber-a" offset="0" />
          <stop class="bgq-gs-amber-b" offset="1" />
        </linearGradient>
        <linearGradient id="bgq-grad-matcha" x1="0" y1="0" x2="0" y2="1">
          <stop class="bgq-gs-matcha-a" offset="0" />
          <stop class="bgq-gs-matcha-b" offset="1" />
        </linearGradient>
        <linearGradient id="bgq-grad-blueberry" x1="0" y1="0" x2="0" y2="1">
          <stop class="bgq-gs-blueberry-a" offset="0" />
          <stop class="bgq-gs-blueberry-b" offset="1" />
        </linearGradient>
        <radialGradient id="bgq-grad-heart" cx="0.5" cy="0.45" r="0.62">
          <stop class="bgq-gs-heart-a" offset="0" />
          <stop class="bgq-gs-heart-b" offset="1" />
        </radialGradient>
      </defs>
    </svg>

    <!-- 三个视差深度层：由登录页 useParallaxDepth 统一驱动反向位移 -->
    <div
      v-for="layer in layers"
      :key="layer.depth"
      class="bloom-garden__layer"
      :data-depth="layer.depth"
    >
      <div
        v-for="f in layer.flowers"
        :key="f.id"
        :ref="(el) => setBloomEl(f.id, el)"
        class="bloom"
        :class="{ 'bloom--minor': f.minor }"
        :style="f.style"
      >
        <!-- 结构：sway(常驻微摆) > pop(光标绽放) > svg(茎/叶/花头)。
             CSS transform 各占独立元素，与 SVG attribute transform 互不覆盖 -->
        <div class="bloom__sway">
          <div class="bloom__pop">
            <svg class="bloom__svg" viewBox="-50 -50 100 100">
              <g :transform="`rotate(${f.stemRot})`">
                <path class="bgq-stem" :d="f.stem" pathLength="1" />
                <g
                  v-for="(lf, k) in f.leaves"
                  :key="k"
                  class="bgq-leaf-g"
                  :transform="lf.tf"
                  :style="lf.style"
                >
                  <path class="bgq-leaf" :d="lf.d" />
                </g>
              </g>
              <g class="bloom__head">
                <g v-for="(ring, ri) in f.rings" :key="ri" :transform="`rotate(${ring.off})`">
                  <g v-for="(p, pi) in ring.petals" :key="pi" :transform="`rotate(${p.a})`">
                    <g class="bgq-petal-g" :style="p.style">
                      <path
                        class="bgq-petal"
                        :class="`bgq-petal--${f.color}`"
                        :d="p.d"
                        :fill="`url(#bgq-grad-${f.color})`"
                      />
                    </g>
                  </g>
                </g>
                <g>
                  <circle class="bgq-heart" :r="f.heartR" fill="url(#bgq-grad-heart)" />
                  <circle
                    v-for="(st, si) in f.stamens"
                    :key="si"
                    class="bgq-stamen"
                    :cx="st.x"
                    :cy="st.y"
                    :r="st.r"
                  />
                </g>
              </g>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- 花瓣飘落层（full 档，≤3 片并发，绝不做全屏花瓣雨） -->
    <div class="bloom-garden__falls">
      <div v-for="p in falls" :key="p.id" class="pfall" :style="p.style">
        <div class="pfall__x">
          <svg class="pfall__svg" viewBox="-14 -26 28 28">
            <path
              class="bgq-petal"
              :class="`bgq-petal--${p.color}`"
              :d="p.d"
              :fill="`url(#bgq-grad-${p.color})`"
            />
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── 高调奶油底：比 --bg-canvas 更透亮，四角极淡地呼应花色，中央提亮留白 ── */
.bloom-garden {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  background:
    radial-gradient(55% 48% at 50% 46%, #fffefb 0%, rgba(255, 254, 251, 0) 70%),
    radial-gradient(105% 80% at 8% 6%, rgba(var(--rose-rgb), 0.1) 0%, transparent 58%),
    radial-gradient(100% 85% at 94% 92%, rgba(var(--amber-rgb), 0.1) 0%, transparent 56%),
    radial-gradient(85% 70% at 92% 5%, rgba(var(--blueberry-rgb), 0.075) 0%, transparent 55%),
    radial-gradient(85% 70% at 5% 95%, rgba(var(--matcha-rgb), 0.085) 0%, transparent 55%),
    linear-gradient(180deg, #fffdf9 0%, #fdf5ed 100%);
}

.bloom-garden__defs {
  position: absolute;
}

/* 视差层出血 18px：位移 ≤13px 时边缘不露底 */
.bloom-garden__layer {
  position: absolute;
  inset: -18px;
}

.bloom {
  position: absolute;
  width: calc(var(--sz) * 1px);
  aspect-ratio: 1;
  transform: translate(-50%, -50%);
}

.bloom__sway,
.bloom__pop {
  width: 100%;
  height: 100%;
}

.bloom__svg {
  display: block;
  width: 100%;
  height: 100%;
  overflow: visible; /* 花茎伸出花朵盒之外 */
}

/* ── 静态着色（= static 档的最终构图，也是动画的终点状态） ──
   描边宽度按 calc(目标px * 100 / --sz) 换算为 viewBox 单位，
   等效恒定屏幕像素细线。不用 vector-effect: non-scaling-stroke ——
   它让描边依赖屏幕 CTM，微摆的每一帧都会强制重栅格化整张 SVG
   （CPU 4x 实测 -25fps） */
.bgq-petal {
  stroke-width: calc(80px / var(--sz, 100));
  stroke-opacity: 0.45;
  fill-opacity: 0.95;
}

.bgq-petal--rose { stroke: color-mix(in srgb, rgb(var(--rose-rgb)) 62%, #a3244a); }
.bgq-petal--amber { stroke: color-mix(in srgb, rgb(var(--amber-rgb)) 60%, #b25d12); }
.bgq-petal--matcha { stroke: color-mix(in srgb, rgb(var(--matcha-rgb)) 60%, #1f6a44); }
.bgq-petal--blueberry { stroke: color-mix(in srgb, rgb(var(--blueberry-rgb)) 60%, #46418f); }

/* 花瓣渐变：瓣尖深一档（饱和度提一档但不腻）→ 基部近白，透出“糖霜”感 */
.bgq-gs-rose-a { stop-color: color-mix(in srgb, rgb(var(--rose-rgb)) 74%, #c2185b); }
.bgq-gs-rose-b { stop-color: color-mix(in srgb, rgb(var(--rose-rgb)) 30%, #fff); }
.bgq-gs-amber-a { stop-color: color-mix(in srgb, rgb(var(--amber-rgb)) 76%, #ef6c00); }
.bgq-gs-amber-b { stop-color: color-mix(in srgb, rgb(var(--amber-rgb)) 28%, #fff); }
.bgq-gs-matcha-a { stop-color: color-mix(in srgb, rgb(var(--matcha-rgb)) 72%, #2e7d52); }
.bgq-gs-matcha-b { stop-color: color-mix(in srgb, rgb(var(--matcha-rgb)) 26%, #fff); }
.bgq-gs-blueberry-a { stop-color: color-mix(in srgb, rgb(var(--blueberry-rgb)) 74%, #5e35b1); }
.bgq-gs-blueberry-b { stop-color: color-mix(in srgb, rgb(var(--blueberry-rgb)) 28%, #fff); }
.bgq-gs-heart-a { stop-color: color-mix(in srgb, rgb(var(--amber-rgb)) 45%, #fff); }
.bgq-gs-heart-b { stop-color: color-mix(in srgb, rgb(var(--amber-rgb)) 82%, #c96a1b); }

.bgq-stamen {
  fill: color-mix(in srgb, rgb(var(--amber-rgb)) 78%, #b25d12);
  opacity: 0.85;
}

.bgq-stem {
  fill: none;
  stroke: color-mix(in srgb, rgb(var(--matcha-rgb)) 62%, #33684a);
  stroke-width: calc(160px / var(--sz, 100));
  stroke-linecap: round;
  opacity: 0.75;
}

.bgq-leaf {
  fill: color-mix(in srgb, rgb(var(--matcha-rgb)) 55%, #fff);
  fill-opacity: 0.9;
  stroke: color-mix(in srgb, rgb(var(--matcha-rgb)) 62%, #2e7d52);
  stroke-width: calc(70px / var(--sz, 100));
  stroke-opacity: 0.5;
}

/* ── 绽放入场（.is-anim = full/lite；static 档无此类 → 直接盛放静态构图） ──
   花茎先以 dashoffset“长”出来，随后花头自花苞弹开、花瓣逐层舒展（缩时摄影感）。
   dash 属性只写在 .is-anim 分支下：static 档的茎天然完整可见 */
.is-anim .bgq-stem {
  stroke-dasharray: 1;
  stroke-dashoffset: 1;
  animation: bgq-stem-in 0.7s cubic-bezier(0.33, 1, 0.68, 1) calc(var(--sd) * 1ms) forwards;
}

@keyframes bgq-stem-in {
  to { stroke-dashoffset: 0; }
}

/* 叶片：茎长到位后淡入（只动 opacity，避免覆盖 attribute transform） */
.is-anim .bgq-leaf-g {
  animation: bgq-leaf-in 0.5s ease calc(var(--ld) * 1ms) backwards;
}

@keyframes bgq-leaf-in {
  from { opacity: 0; }
}

/* 花头：从花苞（scale .2）弹开，带一次轻微过冲。
   transform-box: view-box → 原点 = viewBox 中心 = 花心，不受子瓣动画影响 */
.is-anim .bloom__head {
  transform-box: view-box;
  transform-origin: 50% 50%;
  animation: bgq-head-in 0.85s cubic-bezier(0.25, 0.6, 0.35, 1) calc(var(--d) * 1ms) backwards;
}

@keyframes bgq-head-in {
  0% { opacity: 0; transform: scale(0.2); }
  18% { opacity: 1; }
  55% { transform: scale(1.06); }
  78% { transform: scale(0.97); }
  100% { opacity: 1; transform: scale(1); }
}

/* 花瓣：自基部由收拢（旋转 26° + scale .3）舒展归位，外层环先开、内层随后 */
.is-anim .bgq-petal-g {
  transform-box: fill-box;
  transform-origin: 50% 100%;
  animation: bgq-petal-in 0.9s cubic-bezier(0.22, 1, 0.36, 1) calc(var(--pd, var(--d)) * 1ms) backwards;
}

@keyframes bgq-petal-in {
  0% { opacity: 0; transform: rotate(26deg) scale(0.3); }
  20% { opacity: 1; }
  70% { transform: rotate(-5deg) scale(1.05); }
  100% { opacity: 1; transform: rotate(0deg) scale(1); }
}

/* 二次进入：同一编排压缩到 ~0.4s，与卡片快速版对齐 */
.is-quick.is-anim .bgq-stem { animation-duration: 0.3s; }
.is-quick.is-anim .bgq-leaf-g { animation-duration: 0.25s; }
.is-quick.is-anim .bloom__head { animation-duration: 0.4s; }
.is-quick.is-anim .bgq-petal-g { animation-duration: 0.45s; }

/* ── 常驻微摆（.is-live = 仅 full 档）：以茎基为轴 ±0.9-1.5°，6-10s 异相 ── */
.is-live .bloom__sway {
  transform-origin: 50% 88%;
  animation: bgq-sway var(--sw-dur, 8s) ease-in-out var(--sw-del, 0s) infinite alternate;
}

@keyframes bgq-sway {
  from { transform: rotate(calc(var(--sw-amp, 1.2deg) * -1)); }
  to { transform: rotate(var(--sw-amp, 1.2deg)); }
}

/* ── 光标趋近绽放：--pop/--lean 由 rAF lerp 写入（仅 full 档挂载监听）。
   filter 只在 .is-pop（该朵正被唤醒）时挂载：静止时全场零 filter 层 ── */
.is-live .bloom__pop {
  transform: scale(calc(1 + 0.08 * var(--pop, 0))) rotate(var(--lean, 0deg));
}

.is-live .bloom.is-pop .bloom__pop {
  filter: saturate(calc(1 + 0.25 * var(--pop, 0))) brightness(calc(1 + 0.06 * var(--pop, 0)));
}

/* ── 花瓣飘落：外层竖直下坠(ease-in) × 内层水平摇曳 × svg 旋转淡出 ── */
.bloom-garden__falls {
  position: absolute;
  inset: 0;
}

.pfall {
  position: absolute;
  width: 26px;
  height: 26px;
  margin: -13px 0 0 -13px;
  animation: bgq-pfall-y var(--pf-dur, 5s) cubic-bezier(0.5, 0.05, 0.9, 0.6) forwards;
}

.pfall__x {
  width: 100%;
  height: 100%;
  animation: bgq-pfall-x var(--pf-dur, 5s) ease-in-out forwards;
}

.pfall__svg {
  display: block;
  width: 100%;
  height: 100%;
  overflow: visible;
  animation: bgq-pfall-r var(--pf-dur, 5s) linear forwards;
}

@keyframes bgq-pfall-y {
  from { transform: translateY(0); }
  to { transform: translateY(var(--pf-fall, 220px)); }
}

@keyframes bgq-pfall-x {
  0% { transform: translateX(0); }
  40% { transform: translateX(var(--pf-x1, 20px)); }
  75% { transform: translateX(var(--pf-x2, -16px)); }
  100% { transform: translateX(calc(var(--pf-x1, 20px) * 0.6)); }
}

@keyframes bgq-pfall-r {
  0% { transform: rotate(0deg); opacity: 0.9; }
  70% { opacity: 0.7; }
  100% { transform: rotate(var(--pf-rot, 180deg)); opacity: 0; }
}

/* ── 暗色模式：「金线描花」—— 黑丝绒上的金线刺绣，逻辑全共用只换着色。
   注意：scoped 下必须写成 :global([data-theme='dark'] .xxx) 完整选择器 ── */
:global([data-theme='dark'] .bloom-garden) {
  background:
    radial-gradient(55% 48% at 50% 46%, rgba(26, 21, 14, 0.55) 0%, transparent 70%),
    radial-gradient(105% 80% at 8% 6%, rgba(var(--gold-rgb), 0.07) 0%, transparent 58%),
    radial-gradient(100% 85% at 94% 92%, rgba(var(--gold-rgb), 0.06) 0%, transparent 56%),
    radial-gradient(85% 70% at 92% 5%, rgba(var(--rose-rgb), 0.05) 0%, transparent 55%),
    linear-gradient(180deg, #14100b 0%, var(--bg-canvas, #0e0c0a) 100%);
}

:global([data-theme='dark'] .bloom-garden .bgq-petal) {
  fill-opacity: 0.2;
  stroke: var(--gold, #d4a747);
  stroke-opacity: 0.8;
}

:global([data-theme='dark'] .bgq-gs-rose-a) { stop-color: color-mix(in srgb, rgb(var(--rose-rgb)) 55%, var(--gold, #d4a747)); }
:global([data-theme='dark'] .bgq-gs-rose-b) { stop-color: var(--gold-bright, #f0c96c); }
:global([data-theme='dark'] .bgq-gs-amber-a) { stop-color: var(--gold, #d4a747); }
:global([data-theme='dark'] .bgq-gs-amber-b) { stop-color: var(--gold-bright, #f0c96c); }
:global([data-theme='dark'] .bgq-gs-matcha-a) { stop-color: color-mix(in srgb, rgb(var(--matcha-rgb)) 40%, var(--gold, #d4a747)); }
:global([data-theme='dark'] .bgq-gs-matcha-b) { stop-color: var(--gold-bright, #f0c96c); }
:global([data-theme='dark'] .bgq-gs-blueberry-a) { stop-color: color-mix(in srgb, rgb(var(--blueberry-rgb)) 45%, var(--gold, #d4a747)); }
:global([data-theme='dark'] .bgq-gs-blueberry-b) { stop-color: var(--gold-bright, #f0c96c); }
:global([data-theme='dark'] .bgq-gs-heart-a) { stop-color: var(--gold-bright, #f0c96c); }
:global([data-theme='dark'] .bgq-gs-heart-b) { stop-color: var(--gold, #d4a747); }

:global([data-theme='dark'] .bloom-garden .bgq-heart) {
  fill-opacity: 0.85;
}

:global([data-theme='dark'] .bloom-garden .bgq-stamen) {
  fill: var(--gold-bright, #f0c96c);
}

:global([data-theme='dark'] .bloom-garden .bgq-stem) {
  stroke: rgba(var(--gold-rgb), 0.55);
}

:global([data-theme='dark'] .bloom-garden .bgq-leaf) {
  fill: rgba(var(--gold-rgb), 0.14);
  stroke: rgba(var(--gold-rgb), 0.5);
}

/* 光标趋近时该朵金线微微发光（替代浅色的饱和度提升） */
:global([data-theme='dark'] .bloom-garden.is-live .bloom.is-pop .bloom__pop) {
  filter:
    saturate(calc(1 + 0.1 * var(--pop, 0)))
    brightness(calc(1 + 0.12 * var(--pop, 0)))
    drop-shadow(0 0 calc(var(--pop, 0) * 12px) rgba(var(--gold-rgb), 0.4));
}

/* ── 移动端：隐藏 6 朵点缀小花，其余缩至 72%，构图不挤压中央表单 ── */
@media (max-width: 640px) {
  .bloom { width: calc(var(--sz) * 0.72px); }
  .bloom--minor { display: none; }
}

/* ── 双保险：系统 reduced-motion 下全部动画停用，静态构图完整呈现 ── */
@media (prefers-reduced-motion: reduce) {
  .bloom__head,
  .bgq-petal-g,
  .bgq-leaf-g,
  .bloom__sway,
  .bgq-stem,
  .pfall,
  .pfall__x,
  .pfall__svg {
    animation: none !important;
  }

  .bgq-stem {
    stroke-dasharray: none !important;
    stroke-dashoffset: 0 !important;
  }

  .bloom__pop {
    transform: none !important;
    filter: none !important;
  }

  .pfall {
    display: none;
  }
}
</style>
