<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { brand } from '@/utils/brandTokens'
import { useMotionPref } from '@/composables/useMotionPref'

const props = defineProps({
  count: { type: Number, default: 15 },
  color: { type: String, default: 'primary' },
  speed: { type: Number, default: 1 },
  // 'ambient' = 常驻漂浮粒子场；'burst' = 一次性撒糖爆发（0.6s 自清）
  mode: { type: String, default: 'ambient' },
  burstCount: { type: Number, default: 90 },
})

const { allowMotion } = useMotionPref()

const canvasRef = ref(null)
let ctx = null
let particles = []
let animationId = null
let cleanupResize = null

// 颜色来自 base.css 品牌令牌（经 brandTokens 读取），不再硬编码——浅色/暗色自动跟随
const buildColorMap = () => ({
  primary: [brand('rose', 0.08), brand('rose', 0.06), brand('rose', 0.1)],
  matcha: [brand('matcha', 0.08), brand('matcha', 0.06), brand('matcha', 0.1)],
  amber: [brand('amber', 0.08), brand('amber', 0.06), brand('amber', 0.1)],
  blue: [brand('blueberry', 0.08), brand('blueberry', 0.06), brand('blueberry', 0.1)],
})

// 撒糖爆发用高饱和撞色（喜庆），复用同一粒子隐喻
const buildBurstColors = () => [
  brand('rose'), brand('amber'), brand('matcha'),
  brand('blueberry'), brand('violet'), '#ffffff',
]

// ── 常驻漂浮粒子 ────────────────────────────────
class Particle {
  constructor(w, h, colors) {
    this.x = Math.random() * w
    this.y = Math.random() * h
    this.radius = Math.random() * 4 + 1.5
    this.color = colors[Math.floor(Math.random() * colors.length)]
    this.vx = (Math.random() - 0.5) * 0.3
    this.vy = (Math.random() - 0.5) * 0.3 - 0.15
    this.opacity = Math.random() * 0.5 + 0.3
    this.phase = Math.random() * Math.PI * 2
  }

  update(w, h, time) {
    this.x += this.vx + Math.sin(time * 0.001 + this.phase) * 0.08
    this.y += this.vy + Math.cos(time * 0.0013 + this.phase) * 0.06
    if (this.x < -10) this.x = w + 10
    if (this.x > w + 10) this.x = -10
    if (this.y < -10) this.y = h + 10
    if (this.y > h + 10) this.y = -10
  }

  draw(ctx, time) {
    const pulse = 1 + Math.sin(time * 0.002 + this.phase) * 0.2
    const r = this.radius * pulse
    ctx.beginPath()
    ctx.arc(this.x, this.y, r, 0, Math.PI * 2)
    ctx.fillStyle = this.color
    ctx.fill()
  }
}

// ── 撒糖爆发粒子（径向抛射 + 重力 + 淡出）────────
class BurstParticle {
  constructor(w, h, colors) {
    this.x = w / 2 + (Math.random() - 0.5) * w * 0.18
    this.y = h * 0.42
    const angle = Math.random() * Math.PI * 2
    const power = Math.random() * 9 + 4
    this.vx = Math.cos(angle) * power
    this.vy = Math.sin(angle) * power - 3     // 略偏上
    this.radius = Math.random() * 4 + 2
    this.color = colors[Math.floor(Math.random() * colors.length)]
    this.alpha = 1
    this.spin = Math.random() * Math.PI
    this.spinV = (Math.random() - 0.5) * 0.4
    this.shape = Math.random() > 0.5 ? 'rect' : 'circle'
  }

  update() {
    this.x += this.vx
    this.y += this.vy
    this.vy += 0.35    // 重力
    this.vx *= 0.98
    this.alpha -= 0.028 // ~0.6s 内淡尽
  }

  draw(ctx) {
    if (this.alpha <= 0) return
    ctx.save()
    ctx.globalAlpha = Math.max(0, this.alpha)
    ctx.translate(this.x, this.y)
    ctx.rotate(this.spin)
    ctx.fillStyle = this.color
    if (this.shape === 'rect') ctx.fillRect(-this.radius, -this.radius / 2, this.radius * 2, this.radius)
    else { ctx.beginPath(); ctx.arc(0, 0, this.radius, 0, Math.PI * 2); ctx.fill() }
    ctx.restore()
    this.spin += this.spinV
  }
}

function setupCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return null
  ctx = canvas.getContext('2d')
  const resize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)
  cleanupResize = () => window.removeEventListener('resize', resize)
  return canvas
}

function runAmbient() {
  const canvas = setupCanvas()
  if (!canvas) return
  const colorMap = buildColorMap()
  const colors = colorMap[props.color] || colorMap.primary
  particles = Array.from({ length: props.count }, () =>
    new Particle(canvas.width, canvas.height, colors)
  )
  const animate = (time) => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    particles.forEach((p) => {
      p.update(canvas.width, canvas.height, time)
      p.draw(ctx, time)
    })
    animationId = requestAnimationFrame(animate)
  }
  animationId = requestAnimationFrame(animate)
}

/** 一次性撒糖爆发：0.6s 内抛射淡出并自动清屏 */
function burst() {
  const canvas = setupCanvas()
  if (!canvas) return
  const colors = buildBurstColors()
  particles = Array.from({ length: props.burstCount }, () =>
    new BurstParticle(canvas.width, canvas.height, colors)
  )
  const start = performance.now()
  const animate = (t) => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    let alive = false
    particles.forEach((p) => {
      p.update()
      if (p.alpha > 0 && p.y < canvas.height + 40) { p.draw(ctx); alive = true }
    })
    if (alive && t - start < 900) {
      animationId = requestAnimationFrame(animate)
    } else {
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      particles = []
      animationId = null
    }
  }
  animationId = requestAnimationFrame(animate)
}

onMounted(() => {
  // static 档（reduced-motion / 低端）：不播放任何粒子
  if (!allowMotion.value) return
  if (props.mode === 'burst') burst()
  else runAmbient()
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  cleanupResize?.()
})

// 供父组件手动触发（如需要复用 burst）
defineExpose({ burst })
</script>

<template>
  <canvas ref="canvasRef" class="floating-particles" :class="{ 'is-burst': mode === 'burst' }"></canvas>
</template>

<style scoped>
.floating-particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  opacity: 0.7;
}

/* 撒糖层需盖在成功卡之上（前景喜庆），且不拦截点击 */
.floating-particles.is-burst {
  z-index: 20;
  opacity: 1;
}
</style>
