<template>
  <div class="success-page">
    <!-- 粒子效果画布 -->
    <canvas ref="canvasRef" class="confetti-canvas"></canvas>

    <!-- 成功内容 -->
    <div class="success-content">
      <!-- 成功图标 -->
      <div class="success-icon-wrap">
        <div class="success-ring ring-3"></div>
        <div class="success-ring ring-2"></div>
        <div class="success-ring ring-1"></div>
        <div class="success-icon">
          <svg viewBox="0 0 52 52" class="checkmark">
            <circle class="checkmark__circle" cx="26" cy="26" r="25" fill="none" />
            <path class="checkmark__check" fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8" />
          </svg>
        </div>
      </div>

      <h1 class="success-title">下单成功！</h1>
      <p class="success-subtitle">您的甜品正在准备中，请耐心等候～</p>

      <!-- 订单号 -->
      <div class="order-no-card" v-if="orderNo">
        <p class="order-no-label">订单编号</p>
        <p class="order-no-value">{{ orderNo }}</p>
        <button class="copy-btn" @click="copyOrderNo">{{ copied ? '✅ 已复制' : '📋 复制' }}</button>
      </div>

      <!-- 配送提示 -->
      <div class="delivery-tips">
        <div class="tip-item">
          <span class="tip-icon">⏱️</span>
          <div>
            <p class="tip-title">预计配送时间</p>
            <p class="tip-desc">30 ~ 45 分钟</p>
          </div>
        </div>
        <div class="tip-item">
          <span class="tip-icon">📞</span>
          <div>
            <p class="tip-title">如有问题请联系</p>
            <p class="tip-desc">客服将在 5 分钟内响应</p>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-btns">
        <button class="btn-secondary" @click="router.replace('/m/')">
          🛍️ 继续购物
        </button>
        <button class="btn-primary" @click="router.replace('/m/')">
          返回首页
        </button>
      </div>

      <!-- 装饰性甜品 emoji -->
      <div class="deco-emojis">
        <span v-for="(e, i) in decoEmojis" :key="i" class="deco-emoji" :style="decoStyle(i)">{{ e }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const canvasRef = ref(null)
const orderNo = ref(route.query.orderNo || '')
const copied = ref(false)

const decoEmojis = ['🎂', '🧁', '🍰', '🍮', '🍡', '🌸', '⭐', '✨']

function decoStyle(i) {
  const angles = [15, 45, 75, 120, 165, 200, 250, 300]
  const radii = [140, 160, 155, 145, 150, 158, 148, 162]
  const angle = (angles[i] * Math.PI) / 180
  const r = radii[i]
  return {
    left: `calc(50% + ${Math.cos(angle) * r}px)`,
    top: `calc(50% + ${Math.sin(angle) * r}px)`,
    animationDelay: `${i * 0.15}s`,
    fontSize: `${20 + (i % 3) * 6}px`,
  }
}

async function copyOrderNo() {
  try {
    await navigator.clipboard.writeText(orderNo.value)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    // fallback
  }
}

// ── 撒花粒子动画 ─────────────────────────────────────────────
let animFrame = null
const COLORS = ['#e8637a', '#f0a35c', '#a78bfa', '#34d399', '#60a5fa', '#fbbf24', '#f472b6']

function launchConfetti() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const particles = Array.from({ length: 90 }, () => ({
    x: Math.random() * canvas.width,
    y: -20,
    vx: (Math.random() - 0.5) * 4,
    vy: Math.random() * 3 + 1.5,
    size: Math.random() * 8 + 4,
    color: COLORS[Math.floor(Math.random() * COLORS.length)],
    spin: Math.random() * 360,
    spinSpeed: (Math.random() - 0.5) * 8,
    shape: Math.random() > 0.5 ? 'rect' : 'circle',
    opacity: 1,
  }))

  let tick = 0

  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    tick++

    particles.forEach((p) => {
      p.x += p.vx
      p.y += p.vy
      p.spin += p.spinSpeed
      p.vy += 0.04 // gravity
      if (tick > 120) p.opacity -= 0.008
      p.opacity = Math.max(0, p.opacity)

      ctx.save()
      ctx.globalAlpha = p.opacity
      ctx.translate(p.x, p.y)
      ctx.rotate((p.spin * Math.PI) / 180)
      ctx.fillStyle = p.color

      if (p.shape === 'rect') {
        ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2)
      } else {
        ctx.beginPath()
        ctx.arc(0, 0, p.size / 2, 0, Math.PI * 2)
        ctx.fill()
      }

      ctx.restore()
    })

    if (particles.some((p) => p.opacity > 0 && p.y < canvas.height + 20)) {
      animFrame = requestAnimationFrame(draw)
    } else {
      ctx.clearRect(0, 0, canvas.width, canvas.height)
    }
  }

  draw()
}

onMounted(() => {
  setTimeout(launchConfetti, 300)
})

onUnmounted(() => {
  if (animFrame) cancelAnimationFrame(animFrame)
})
</script>

<style scoped>
.success-page {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  position: relative;
  overflow: hidden;
}

.confetti-canvas {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.success-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 380px;
  width: 100%;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 28px;
  padding: 40px 24px;
  box-shadow: 
    0 16px 48px rgba(45, 35, 39, 0.06),
    inset 0 1px 0 0 rgba(255, 255, 255, 0.4);
}

/* ── 成功图标 ── */
.success-icon-wrap {
  position: relative;
  width: 100px;
  height: 100px;
  margin: 10px auto 28px;
}

.success-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid rgba(232, 99, 122, 0.18);
  animation: ring-expand 2s cubic-bezier(0.16, 1, 0.3, 1) forwards infinite;
}
.ring-2 { animation-delay: 0.5s; }
.ring-3 { animation-delay: 1s; }

@keyframes ring-expand {
  0% { transform: scale(0.6); opacity: 1; }
  100% { transform: scale(2.2); opacity: 0; }
}

.success-icon {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #e8637a 0%, #c94d63 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 
    0 12px 32px rgba(232, 99, 122, 0.35),
    inset 0 2px 4px rgba(255, 255, 255, 0.3);
  animation: icon-pop 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

@keyframes icon-pop {
  0% { transform: scale(0); }
  100% { transform: scale(1); }
}

/* SVG 对勾动画 */
.checkmark {
  width: 52px;
  height: 52px;
}

.checkmark__circle {
  stroke: rgba(255,255,255,0.4);
  stroke-width: 2;
  stroke-dasharray: 166;
  stroke-dashoffset: 166;
  animation: stroke 0.6s cubic-bezier(0.65, 0, 0.45, 1) 0.3s forwards;
}

.checkmark__check {
  stroke: white;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 48;
  stroke-dashoffset: 48;
  animation: stroke 0.4s cubic-bezier(0.65, 0, 0.45, 1) 0.7s forwards;
}

@keyframes stroke {
  100% { stroke-dashoffset: 0; }
}

/* ── 文字 ── */
.success-title {
  font-size: 24px;
  font-weight: 800;
  color: #2d2327;
  margin: 0 0 10px;
  animation: fade-up 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.3s both;
  letter-spacing: 0.02em;
}

.success-subtitle {
  font-size: 13px;
  color: #a3949b;
  margin: 0 0 28px;
  line-height: 1.5;
  animation: fade-up 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.4s both;
  font-weight: 500;
}

@keyframes fade-up {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ── 订单号卡片 ── */
.order-no-card {
  background: rgba(253, 232, 236, 0.45);
  border: 1px solid rgba(232, 99, 122, 0.15);
  border-radius: 20px;
  padding: 16px 20px;
  margin-bottom: 24px;
  animation: fade-up 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.5s both;
  box-shadow: inset 0 1px 0 0 rgba(255,255,255,0.4);
}

.order-no-label {
  font-size: 11px;
  font-weight: 700;
  color: #a3949b;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin: 0 0 6px;
}

.order-no-value {
  font-size: 16px;
  font-weight: 800;
  color: #2d2327;
  letter-spacing: 0.04em;
  margin: 0 0 12px;
  font-family: 'SF Mono', SFMono-Regular, Consolas, monospace;
}

.copy-btn {
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(232, 99, 122, 0.25);
  color: #e8637a;
  font-size: 12px;
  font-weight: 700;
  padding: 5px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 2px 6px rgba(232, 99, 122, 0.04);
}

.copy-btn:active { 
  transform: scale(0.96); 
  background: #fff;
  border-color: #e8637a;
}

/* ── 配送提示 ── */
.delivery-tips {
  background: rgba(255, 255, 255, 0.45);
  border-radius: 20px;
  padding: 18px;
  margin-bottom: 28px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  animation: fade-up 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.6s both;
  text-align: left;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.tip-icon { 
  font-size: 20px; 
  flex-shrink: 0; 
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255,255,255,0.8);
  box-shadow: 0 2px 6px rgba(0,0,0,0.02);
}

.tip-title { font-size: 11px; font-weight: 700; color: #a3949b; margin: 0 0 1px; text-transform: uppercase; letter-spacing: 0.02em; }
.tip-desc { font-size: 13px; font-weight: 700; color: #2d2327; margin: 0; }

/* ── 操作按钮 ── */
.action-btns {
  display: flex;
  gap: 12px;
  animation: fade-up 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.7s both;
}

.btn-primary, .btn-secondary {
  flex: 1;
  padding: 14px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-primary {
  background: linear-gradient(135deg, #e8637a, #c94d63);
  color: #fff;
  box-shadow: 
    0 8px 20px rgba(232, 99, 122, 0.25),
    inset 0 1px 0 0 rgba(255, 255, 255, 0.2);
}

.btn-secondary {
  background: rgba(255, 255, 255, 0.85);
  color: #e8637a;
  border: 1px solid rgba(232, 99, 122, 0.25);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.01);
}

.btn-primary:active { transform: scale(0.96); box-shadow: 0 4px 10px rgba(232, 99, 122, 0.15); }
.btn-secondary:active { transform: scale(0.96); background: #fff; }

/* ── 装饰 Emoji ── */
.deco-emojis {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.deco-emoji {
  position: absolute;
  transform: translate(-50%, -50%);
  animation: float-spin 8s ease-in-out infinite;
  opacity: 0.6;
  filter: drop-shadow(0 4px 8px rgba(0,0,0,0.06));
}

@keyframes float-spin {
  0%, 100% { transform: translate(-50%, -50%) rotate(0deg) scale(1); }
  33% { transform: translate(-50%, -54%) rotate(12deg) scale(1.06); }
  66% { transform: translate(-50%, -46%) rotate(-10deg) scale(0.94); }
}
</style>
