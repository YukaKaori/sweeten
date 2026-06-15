<template>
  <div class="success-page">
    <!-- Confetti Canvas -->
    <canvas ref="canvasRef" class="confetti-canvas"></canvas>

    <!-- Liquid Glass Card -->
    <div class="success-card">
      <!-- Animated Checkmark -->
      <div class="checkmark-wrap">
        <div class="checkmark-ring ring-3"></div>
        <div class="checkmark-ring ring-2"></div>
        <div class="checkmark-ring ring-1"></div>
        <div class="checkmark-icon">
          <svg viewBox="0 0 52 52" class="checkmark-svg">
            <circle class="cm-circle" cx="26" cy="26" r="25" fill="none" />
            <path class="cm-check" fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8" />
          </svg>
        </div>
      </div>

      <h1 class="success-title">下单成功！</h1>
      <p class="success-subtitle">您的甜品正在准备中，请耐心等候</p>

      <!-- Order No -->
      <div class="order-no-card" v-if="orderNo">
        <p class="ono-label">订单编号</p>
        <p class="ono-value">{{ orderNo }}</p>
        <button class="ono-copy" @click="copyOrderNo">
          {{ copied ? '✅ 已复制' : '📋 复制' }}
        </button>
      </div>

      <!-- Delivery Info -->
      <div class="delivery-info">
        <div class="di-item">
          <span class="di-icon">⏱️</span>
          <div>
            <p class="di-title">预计送达</p>
            <p class="di-desc">30 ~ 45 分钟</p>
          </div>
        </div>
        <div class="di-item">
          <span class="di-icon">📞</span>
          <div>
            <p class="di-title">客服支持</p>
            <p class="di-desc">5 分钟内响应</p>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="action-row">
        <button class="act-btn act-secondary" @click="router.replace('/m/')">
          🛍️ 继续购物
        </button>
        <button class="act-btn act-primary" @click="router.replace('/m/')">
          返回首页
        </button>
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

async function copyOrderNo() {
  try {
    await navigator.clipboard.writeText(orderNo.value)
    copied.value = true
    setTimeout(() => copied.value = false, 2000)
  } catch { /* noop */ }
}

// ── Confetti ──
let animFrame = null
const COLORS = ['#d9574a','#f0a35c','#a78bfa','#34d399','#60a5fa','#fbbf24','#f472b6','#fff']

function launchConfetti() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const particles = Array.from({ length: 100 }, () => ({
    x: Math.random() * canvas.width,
    y: -30 - Math.random() * 60,
    vx: (Math.random() - 0.5) * 5,
    vy: Math.random() * 3 + 2,
    size: Math.random() * 7 + 3,
    color: COLORS[Math.floor(Math.random() * COLORS.length)],
    spin: Math.random() * 360,
    spinV: (Math.random() - 0.5) * 10,
    shape: Math.random() > 0.5 ? 'rect' : 'circle',
    alpha: 1,
  }))

  let frame = 0
  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    frame++
    let alive = false
    particles.forEach(p => {
      p.x += p.vx; p.y += p.vy; p.spin += p.spinV; p.vy += 0.045
      if (frame > 130) p.alpha -= 0.01
      p.alpha = Math.max(0, p.alpha)
      if (p.alpha <= 0 || p.y > canvas.height + 40) return
      alive = true
      ctx.save(); ctx.globalAlpha = p.alpha; ctx.translate(p.x, p.y)
      ctx.rotate(p.spin * Math.PI / 180); ctx.fillStyle = p.color
      if (p.shape === 'rect') ctx.fillRect(-p.size/2, -p.size/4, p.size, p.size/2)
      else { ctx.beginPath(); ctx.arc(0, 0, p.size/2, 0, Math.PI*2); ctx.fill() }
      ctx.restore()
    })
    if (alive) animFrame = requestAnimationFrame(draw)
  }
  requestAnimationFrame(draw)
}

onMounted(() => setTimeout(launchConfetti, 300))
onUnmounted(() => { if (animFrame) cancelAnimationFrame(animFrame) })
</script>

<style scoped>
.success-page {
  min-height: 100dvh; display: flex; align-items: center; justify-content: center;
  padding: 40px 20px; position: relative; overflow: hidden;
}
.confetti-canvas { position: fixed; inset: 0; pointer-events: none; z-index: 0; }

/* ── Glass Card ── */
.success-card {
  position: relative; z-index: 1; text-align: center;
  max-width: 360px; width: 100%;
  background: rgba(255,255,255,0.75);
  backdrop-filter: blur(32px) saturate(200%);
  -webkit-backdrop-filter: blur(32px) saturate(200%);
  border: 0.5px solid rgba(255,255,255,0.5);
  border-radius: 28px; padding: 42px 24px 32px;
  box-shadow:
    0 20px 52px rgba(0,0,0,0.06),
    inset 0 0.5px 0 rgba(255,255,255,0.6);
}

/* ── Checkmark ── */
.checkmark-wrap { position: relative; width: 100px; height: 100px; margin: 0 auto 28px; }
.checkmark-ring {
  position: absolute; inset: 0; border-radius: 50%;
  border: 2px solid rgba(217,87,74,0.15);
  animation: ring-expand 2s cubic-bezier(0.16,1,0.3,1) infinite;
}
.ring-2 { animation-delay: 0.5s; }
.ring-3 { animation-delay: 1s; }
@keyframes ring-expand {
  0% { transform: scale(0.6); opacity: 1; }
  100% { transform: scale(2.4); opacity: 0; }
}
.checkmark-icon {
  position: absolute; inset: 0; border-radius: 50%;
  background: linear-gradient(145deg, #d9574a 0%, #c54336 100%);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 12px 36px rgba(217,87,74,0.3), inset 0 2px 4px rgba(255,255,255,0.25);
  animation: icon-pop 0.6s cubic-bezier(0.34,1.56,0.64,1) forwards;
}
@keyframes icon-pop {
  0% { transform: scale(0); } 100% { transform: scale(1); }
}
.checkmark-svg { width: 52px; height: 52px; }
.cm-circle {
  stroke: rgba(255,255,255,0.35); stroke-width: 2;
  stroke-dasharray: 166; stroke-dashoffset: 166;
  animation: stroke 0.6s cubic-bezier(0.65,0,0.45,1) 0.3s forwards;
}
.cm-check {
  stroke: #fff; stroke-width: 3; stroke-linecap: round; stroke-linejoin: round;
  stroke-dasharray: 48; stroke-dashoffset: 48;
  animation: stroke 0.4s cubic-bezier(0.65,0,0.45,1) 0.7s forwards;
}
@keyframes stroke { 100% { stroke-dashoffset: 0; } }

/* ── Typography ── */
.success-title {
  font-size: 24px; font-weight: 650; color: #1d1d1f;
  letter-spacing: -0.35px; margin: 0 0 8px;
  animation: fade-up 0.5s cubic-bezier(0.16,1,0.3,1) 0.35s both;
}
.success-subtitle {
  font-size: 13px; color: #86868b; margin: 0 0 24px; line-height: 1.5;
  animation: fade-up 0.5s cubic-bezier(0.16,1,0.3,1) 0.45s both;
}
@keyframes fade-up {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ── Order No ── */
.order-no-card {
  background: rgba(217,87,74,0.05); border: 0.5px solid rgba(217,87,74,0.12);
  border-radius: 18px; padding: 16px 18px; margin-bottom: 22px;
  animation: fade-up 0.5s cubic-bezier(0.16,1,0.3,1) 0.55s both;
}
.ono-label { font-size: 10px; font-weight: 650; color: #aeaeb2; letter-spacing: 0.08em; text-transform: uppercase; margin: 0 0 5px; }
.ono-value {
  font-size: 16px; font-weight: 650; color: #1d1d1f;
  letter-spacing: 0.03em; margin: 0 0 10px;
  font-family: 'SF Mono', SFMono-Regular, Consolas, monospace;
}
.ono-copy {
  background: rgba(255,255,255,0.75); border: 0.5px solid rgba(217,87,74,0.2);
  color: #d9574a; font-size: 12px; font-weight: 600;
  padding: 5px 16px; border-radius: 999px; cursor: pointer;
  transition: all 0.2s;
}
.ono-copy:active { transform: scale(0.96); }

/* ── Delivery ── */
.delivery-info {
  background: rgba(255,255,255,0.45); border-radius: 18px; padding: 16px; margin-bottom: 26px;
  display: flex; flex-direction: column; gap: 12px; text-align: left;
  border: 0.5px solid rgba(255,255,255,0.4);
  animation: fade-up 0.5s cubic-bezier(0.16,1,0.3,1) 0.65s both;
}
.di-item { display: flex; align-items: center; gap: 14px; }
.di-icon {
  font-size: 20px; width: 38px; height: 38px; border-radius: 50%;
  background: rgba(255,255,255,0.6); border: 0.5px solid rgba(255,255,255,0.5);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.di-title { font-size: 11px; font-weight: 650; color: #aeaeb2; margin: 0 0 1px; text-transform: uppercase; letter-spacing: 0.02em; }
.di-desc  { font-size: 13px; font-weight: 650; color: #1d1d1f; margin: 0; }

/* ── Actions ── */
.action-row {
  display: flex; gap: 12px;
  animation: fade-up 0.5s cubic-bezier(0.16,1,0.3,1) 0.75s both;
}
.act-btn {
  flex: 1; padding: 14px; border-radius: 16px; border: none;
  font-size: 14px; font-weight: 600; cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 4px;
  transition: all 0.3s cubic-bezier(0.16,1,0.3,1);
}
.act-primary {
  background: #1d1d1f; color: #fff;
  box-shadow: 0 6px 18px rgba(0,0,0,0.08);
}
.act-secondary {
  background: rgba(255,255,255,0.85); color: #1d1d1f;
  border: 0.5px solid rgba(0,0,0,0.06);
}
.act-btn:active { transform: scale(0.96); }
</style>
