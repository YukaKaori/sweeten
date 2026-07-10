<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/stores/modules/user'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import FluidGradient from '@/components/FluidGradient.vue'
import { useMotionPref } from '@/composables/useMotionPref'
import { useMagnetic, useParallaxDepth } from '@/composables/useLiquidGlass'

const loginForm = ref({ username: '', password: '' })
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const pageRef = ref(null)

// static 档（reduced-motion / 低端设备）→ 背景渐变静止
const { allowMotion } = useMotionPref()

// 主按钮磁吸（位移跟随，克制 ≤6px）；static/触屏自动关闭
useMagnetic(pageRef, { selector: '.login-btn', maxDisplace: 6, scale: 1.03 })

// 视差纵深：背景流体(12px) / 颗粒噪声(8px) / 玻璃卡片(4px) 三层反向位移，
// lerp 平滑跟随；static 档与触屏不绑事件 → 保留静态分层
useParallaxDepth(pageRef)

// 输入聚焦 → 卡片一次极轻的呼吸辉光（浅色玫瑰 / 暗色金），≤1.5s 后停在
// 极淡的静态微光，不循环；失焦淡出。static 档不触发。
const cardGlow = ref(false)
const onCardFocusin = (e) => {
  if (!allowMotion.value || e.target?.tagName !== 'INPUT') return
  // 先摘类再下一帧挂回 → 换字段聚焦时呼吸动画能重新播放
  cardGlow.value = false
  requestAnimationFrame(() => { cardGlow.value = true })
}
const onCardFocusout = () => { cardGlow.value = false }

// 登录成功 → 卡片退场（缩小+淡出，非平移），再交给 View Transitions 进仪表盘
const leaving = ref(false)

const login = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const result = await userStore.login(loginForm.value)
    if (result.success) {
      ElMessage.success('登录成功')
      if (allowMotion.value) {
        leaving.value = true
        await new Promise((resolve) => setTimeout(resolve, 300))
      }
      router.push('/')
    } else {
      ElMessage.error(result.msg || '登录失败')
    }
  } catch (e) {
    ElMessage.error('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleKeyup = (e) => {
  if (e.key === 'Enter') login()
}
</script>

<template>
  <div class="login-page" ref="pageRef">
    <!-- 深度层 1（远景，视差 ≤12px）：流体渐变背景。
         full 档 + WebGL → OGL 真流体（含光标扰动）；否则 CSS mesh 兜底 -->
    <div class="depth-layer depth-layer--bg" data-depth="12">
      <FluidGradient :animated="allowMotion" webgl />
    </div>

    <!-- 深度层 2（中景，视差 ≤8px）：极细颗粒噪声。
         叠在流体（含 WebGL 画布）之上，同时压色带、给玻璃卡片留质感衬底 -->
    <div class="depth-layer depth-layer--grain" data-depth="8" aria-hidden="true"></div>

    <!-- 深度层 3（近景，视差 ≤4px）：玻璃卡片 -->
    <div
      class="login-card glass"
      data-depth="4"
      :class="{ 'is-glow': cardGlow, 'is-leaving': leaving }"
      @focusin="onCardFocusin"
      @focusout="onCardFocusout"
    >
      <div class="brand" style="--i: 0">
        <div class="brand-mark">
          <svg width="28" height="28" viewBox="0 0 32 32" fill="none">
            <path d="M9 20c0-5 3-10 7-10s7 5 7 10" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" fill="none"/>
            <path d="M6 20h20" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/>
            <circle cx="16" cy="7.5" r="1.4" fill="currentColor"/>
          </svg>
        </div>
      </div>

      <div class="head" style="--i: 1">
        <h1>DessertShop</h1>
        <p>登录以进入管理后台</p>
      </div>

      <el-form :model="loginForm" class="login-form" @keyup="handleKeyup">
        <div class="field" style="--i: 2">
          <label>账户</label>
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            size="large"
          />
        </div>
        <div class="field" style="--i: 3">
          <label>密码</label>
          <el-input
            type="password"
            v-model="loginForm.password"
            placeholder="密码"
            size="large"
            show-password
          />
        </div>
        <el-button
          class="login-btn"
          style="--i: 4"
          type="primary"
          size="large"
          :loading="loading"
          @click="login"
        >
          {{ loading ? '登录中…' : '登录' }}
        </el-button>
      </el-form>

      <p class="foot" style="--i: 5">DessertShop Management System</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  background: var(--mesh-base);   /* 兜底，实际由 FluidGradient 覆盖 */
}

/* ---- 视差深度层 ----
   三层随光标反向位移（useParallaxDepth 按 data-depth 驱动）：
   背景流体 12px > 颗粒噪声 8px > 卡片 4px → 纵深感。
   容器 inset 外扩 > 最大位移，保证移动时不露边。 */
.depth-layer--bg {
  position: absolute;
  inset: -16px;
  z-index: 0;
}

/* 极细颗粒噪声（中景层）：叠在流体之上 —— WebGL 画布不透明会盖掉
   FluidGradient 内部的 CSS 噪声，这层同时负责给真流体压色带 */
.depth-layer--grain {
  position: absolute;
  inset: -14px;
  z-index: 1;
  opacity: var(--mesh-noise-opacity, 0.05);
  mix-blend-mode: soft-light;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 140px 140px;
  pointer-events: none;
}

/* 磨砂玻璃卡片（本页唯一的 backdrop-filter 层，近景视差层） */
.login-card {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 400px;
  padding: 56px 48px 40px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: 22px;
  /* 边缘色散：两层 1px 错位色影模拟折射色边（暖玫瑰↖ / 冷抹茶↘），
     克制的暗示，不追求真实光学 */
  box-shadow:
    var(--glass-specular),
    -1px -1px 0 0 rgba(var(--rose-rgb), 0.14),
    1px 1px 0 0 rgba(var(--matcha-rgb), 0.12),
    var(--shadow-xl);
}

/* 暗色：色散换 金↖ / 玫瑰↘（黑金语言） */
[data-theme='dark'] .login-card {
  box-shadow:
    var(--glass-specular),
    -1px -1px 0 0 rgba(var(--gold-rgb), 0.2),
    1px 1px 0 0 rgba(var(--rose-rgb), 0.1),
    var(--shadow-xl);
}

/* 呼吸辉光层：输入聚焦时一次起伏（≤1.5s），随后停在极淡的静态微光；
   只动 opacity（辉光预先画在 ::after 上），不重绘、不循环 */
.login-card::after {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.5s ease;
  box-shadow:
    0 0 0 1px rgba(var(--rose-rgb), 0.22),
    0 0 44px -6px rgba(var(--rose-rgb), 0.38);
}

.login-card.is-glow::after {
  animation: card-breath 1.4s cubic-bezier(0.33, 1, 0.68, 1) 1 forwards;
}

[data-theme='dark'] .login-card::after {
  box-shadow:
    0 0 0 1px rgba(var(--gold-rgb), 0.24),
    0 0 44px -6px rgba(var(--gold-rgb), 0.34);
}

@keyframes card-breath {
  0% { opacity: 0; }
  40% { opacity: 1; }
  100% { opacity: 0.22; }
}

/* 登录成功退场：缩小 + 淡出（非平移），随后交给 View Transitions 进仪表盘 */
.login-card.is-leaving {
  animation: card-exit 0.3s cubic-bezier(0.4, 0, 0.2, 1) forwards;
  pointer-events: none;
}

@keyframes card-exit {
  to {
    opacity: 0;
    transform: scale(0.93);
  }
}

/* ---- Staggered entrance — restrained fade-up ---- */
.brand,
.head,
.field,
.login-btn,
.foot {
  opacity: 0;
  transform: translateY(10px);
  animation: fadeInUp 0.55s cubic-bezier(0.25, 0.1, 0.25, 1) forwards;
  animation-delay: calc(var(--i) * 0.07s + 0.05s);
}

/* ---- Brand mark ---- */
.brand {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.brand-mark {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  background: var(--color-primary-lighter);
}

/* ---- Heading ---- */
.head {
  text-align: center;
  margin-bottom: 36px;
}

.head h1 {
  font-size: 26px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.head p {
  font-size: 15px;
  color: var(--color-text-muted);
  letter-spacing: -0.01em;
}

/* ---- Fields ---- */
.field {
  margin-bottom: 18px;
}

.field label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
  letter-spacing: -0.01em;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 0 1px var(--color-border) inset !important;
  background: var(--color-bg-secondary);
  padding: 6px 14px;
  transition: box-shadow var(--transition-fast);
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--color-text-muted) inset !important;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-primary) inset !important;
}

.login-form :deep(.el-input__inner) {
  font-size: 15px;
  height: 26px;
}

/* ---- Primary action ---- */
.login-btn {
  width: 100%;
  height: 48px;
  margin-top: 12px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: -0.01em;
  border-radius: var(--radius-md) !important;
  background: var(--color-primary) !important;   /* 品牌草莓玫瑰 */
  border: none !important;
  box-shadow: var(--glow-sm) !important;          /* 玫瑰柔光 */
  transition: background-color var(--transition-fast), box-shadow var(--transition-base), transform var(--transition-fast) !important;
}

.login-btn:hover {
  background: var(--color-primary-dark) !important;
  box-shadow: var(--glow-md) !important;
}

.login-btn:active {
  transform: scale(0.99);
}

/* ---- Footer ---- */
.foot {
  margin-top: 32px;
  text-align: center;
  font-size: 12px;
  color: var(--color-text-muted);
  letter-spacing: 0.01em;
}

/* 双保险：static 档（JS 已早退）+ 系统 reduced-motion 下，
   入场 stagger / 呼吸辉光 / 退场全部瞬时 → 一张干净的静态渐变卡片 */
@media (prefers-reduced-motion: reduce) {
  .brand,
  .head,
  .field,
  .login-btn,
  .foot {
    animation: none;
    opacity: 1;
    transform: none;
  }

  .login-card.is-glow::after,
  .login-card.is-leaving {
    animation: none;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 44px 28px 32px;
  }
}
</style>
