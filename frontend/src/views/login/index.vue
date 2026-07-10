<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/modules/user'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import BloomGarden from '@/components/BloomGarden.vue'
import { useMotionPref } from '@/composables/useMotionPref'
import { useMagnetic, useParallaxDepth } from '@/composables/useLiquidGlass'

const WORDMARK = 'DessertShop'
const ENTRANCE_KEY = 'ds-login-entrance-played'

const loginForm = ref({ username: '', password: '' })
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const pageRef = ref(null)

// static 档（reduced-motion / 低端设备）→ 全部瞬时到位
const { allowMotion } = useMotionPref()

// ── 入场编排：首次进入播完整 1.2s 版，登出返回播 0.4s 简化版 ──
const firstVisit = (() => {
  try { return !sessionStorage.getItem(ENTRANCE_KEY) } catch { return true }
})()

// v-motion 统一编排辅助：full/quick 两条时间线；static 档返回空变体（瞬时可见）
const spring = { type: 'spring', stiffness: 220, damping: 26 }
function mv(from, delayFull, delayQuick) {
  if (!allowMotion.value) return { initial: undefined, enter: undefined }
  return {
    initial: { ...from, opacity: 0 },
    enter: {
      y: 0,
      rotate: 0,
      opacity: 1,
      transition: { ...spring, delay: firstVisit ? delayFull : delayQuick },
    },
  }
}
const iconM = mv({ y: 12 }, 160, 0)
const letterM = (i) => mv({ y: 16, rotate: 6 }, 200 + i * 40, i * 15)
const taglineM = mv({ y: 10 }, 560, 60)
const cardM = mv({ y: 24 }, 450, 0)
const fieldM = (i) => mv({ y: 14 }, 650 + i * 60, 100 + i * 40)
const footM = mv({ y: 10 }, 830, 220)

// 卡片边缘辉光：1100ms 一次性左→右扫过，之后静止（仅首次完整版）
const cardSweep = ref(false)
let sweepTimer = null
onMounted(() => {
  try { sessionStorage.setItem(ENTRANCE_KEY, '1') } catch { /* noop */ }
  if (allowMotion.value && firstVisit) {
    sweepTimer = setTimeout(() => { cardSweep.value = true }, 1100)
  }
})
onUnmounted(() => clearTimeout(sweepTimer))

// 主按钮磁吸（位移跟随，克制 ≤6px）；static/触屏自动关闭
useMagnetic(pageRef, { selector: '.login-btn', maxDisplace: 6, scale: 1.03 })

// 视差纵深：花园大/中/小花(13/9/6px) / 颗粒噪声(8px) / 玻璃卡片(4px) 反向位移
useParallaxDepth(pageRef)

// 输入聚焦 → 卡片一次极轻的呼吸辉光，≤1.5s 后停在极淡静态微光
const cardGlow = ref(false)
const onCardFocusin = (e) => {
  if (!allowMotion.value || e.target?.tagName !== 'INPUT') return
  cardGlow.value = false
  requestAnimationFrame(() => { cardGlow.value = true })
}
const onCardFocusout = () => { cardGlow.value = false }

// ── 浮动标签：聚焦或有值时上浮缩小 ──
const userFocus = ref(false)
const pwdFocus = ref(false)
const userFloat = computed(() => userFocus.value || !!loginForm.value.username)
const pwdFloat = computed(() => pwdFocus.value || !!loginForm.value.password)

// ── 密码可见性切换：眼睛图标 150ms 眨眼微动画 ──
const showPwd = ref(false)
const pwdBlink = ref(false)
const togglePwd = () => {
  showPwd.value = !showPwd.value
  if (!allowMotion.value) return
  pwdBlink.value = false
  requestAnimationFrame(() => { pwdBlink.value = true })
}

// ── 登录失败反馈：卡片 shake + 输入框短暂草莓红 + 错误文字淡入 ──
const errorMsg = ref('')
const shake = ref(false)
const inputFlash = ref(false)
let flashTimer = null
function failFeedback(msg) {
  errorMsg.value = msg
  inputFlash.value = true
  clearTimeout(flashTimer)
  flashTimer = setTimeout(() => { inputFlash.value = false }, 900)
  if (!allowMotion.value) return
  shake.value = false
  requestAnimationFrame(() => { shake.value = true })
}
const clearError = () => { errorMsg.value = '' }

// 登录成功 → 卡片退场（缩小+淡出），再交给 View Transitions 进仪表盘
const leaving = ref(false)

const login = async () => {
  if (loading.value) return
  if (!loginForm.value.username || !loginForm.value.password) {
    failFeedback('请输入用户名和密码')
    return
  }
  errorMsg.value = ''
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
      failFeedback(result.msg || '登录失败')
    }
  } catch (e) {
    failFeedback('登录失败，请稍后重试')
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
    <!-- 背景：盛放花园（程序化 SVG 花朵，0-1s 自边缘向内绽放）。
         视差深度层在组件内部（大花 13 / 中花 9 / 小花 6），由下方
         useParallaxDepth 统一驱动，故此 wrapper 不再挂 data-depth -->
    <div class="depth-layer depth-layer--bg">
      <BloomGarden :quick="!firstVisit" />
    </div>

    <!-- 深度层 2（中景，视差 ≤8px）：极细颗粒噪声 -->
    <div class="depth-layer depth-layer--grain" data-depth="8" aria-hidden="true"></div>

    <!-- 深度层 3（近景，视差 ≤4px）：玻璃卡片。
         视差 transform 写在外层 wrapper，入场/退场/shake 写在卡片本体，互不打架 -->
    <div class="card-depth" data-depth="4">
      <div
        class="login-card glass"
        :class="{
          'is-glow': cardGlow,
          'is-leaving': leaving,
          'is-shake': shake,
          'is-sweep': cardSweep,
          'card-in-full': allowMotion && firstVisit,
          'card-in-quick': allowMotion && !firstVisit,
        }"
        v-motion
        :initial="cardM.initial"
        :enter="cardM.enter"
        @focusin="onCardFocusin"
        @focusout="onCardFocusout"
      >
        <div class="brand">
          <div class="brand-mark" v-motion :initial="iconM.initial" :enter="iconM.enter">
            <svg width="28" height="28" viewBox="0 0 32 32" fill="none">
              <path d="M9 20c0-5 3-10 7-10s7 5 7 10" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" fill="none"/>
              <path d="M6 20h20" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/>
              <circle cx="16" cy="7.5" r="1.4" fill="currentColor"/>
            </svg>
          </div>
        </div>

        <div class="head">
          <!-- 动态品牌名：大号衬线 + 逐字上浮旋转归正入场 + 品牌渐变填充；
               ::after 复制层负责每 10s 一次的 1.2s 高光扫过（其余时间完全静止） -->
          <h1 class="wordmark" :class="{ 'wm-live': allowMotion }" :data-text="WORDMARK">
            <span
              v-for="(ch, i) in WORDMARK.split('')"
              :key="i"
              class="wm-letter"
              v-motion
              :initial="letterM(i).initial"
              :enter="letterM(i).enter"
            >{{ ch }}</span>
          </h1>
          <p class="tagline" v-motion :initial="taglineM.initial" :enter="taglineM.enter">登录以进入管理后台</p>
        </div>

        <el-form :model="loginForm" class="login-form" @keyup="handleKeyup">
          <div
            class="field field--float"
            :class="{ 'is-float': userFloat, 'is-focus': userFocus, 'is-invalid': inputFlash }"
            v-motion
            :initial="fieldM(0).initial"
            :enter="fieldM(0).enter"
          >
            <el-input
              v-model="loginForm.username"
              size="large"
              @focus="userFocus = true"
              @blur="userFocus = false"
              @input="clearError"
            />
            <label class="float-label">账户</label>
          </div>
          <div
            class="field field--float"
            :class="{ 'is-float': pwdFloat, 'is-focus': pwdFocus, 'is-invalid': inputFlash }"
            v-motion
            :initial="fieldM(1).initial"
            :enter="fieldM(1).enter"
          >
            <el-input
              v-model="loginForm.password"
              :type="showPwd ? 'text' : 'password'"
              size="large"
              @focus="pwdFocus = true"
              @blur="pwdFocus = false"
              @input="clearError"
            >
              <template #suffix>
                <button
                  type="button"
                  class="pwd-eye"
                  :class="{ 'is-blink': pwdBlink }"
                  :aria-label="showPwd ? '隐藏密码' : '显示密码'"
                  @mousedown.prevent
                  @click="togglePwd"
                >
                  <svg v-if="showPwd" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M2 12s3.5-6.5 10-6.5S22 12 22 12s-3.5 6.5-10 6.5S2 12 2 12Z"/>
                    <circle cx="12" cy="12" r="2.6"/>
                  </svg>
                  <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 13c2.2 2.4 5.2 3.9 9 3.9s6.8-1.5 9-3.9"/>
                    <path d="M12 16.9v2.6M6.2 15.9l-1.5 2.2M17.8 15.9l1.5 2.2"/>
                  </svg>
                </button>
              </template>
            </el-input>
            <label class="float-label">密码</label>
          </div>

          <!-- 错误文字淡入（固定占位，避免布局跳动） -->
          <div class="form-error-slot" aria-live="polite">
            <Transition name="err">
              <p v-if="errorMsg" class="form-error" role="alert">{{ errorMsg }}</p>
            </Transition>
          </div>

          <el-button
            class="login-btn"
            :class="{ 'is-busy': loading }"
            type="primary"
            size="large"
            :disabled="loading"
            v-motion
            :initial="fieldM(2).initial"
            :enter="fieldM(2).enter"
            @click="login"
          >
            <span class="btn-label">登录</span>
            <span v-if="loading" class="btn-progress" aria-hidden="true"><i></i></span>
          </el-button>
        </el-form>

        <p class="foot" v-motion :initial="footM.initial" :enter="footM.enter">DessertShop Management System</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── 登录页局部字体：仅子集（Fraunces 品牌名 11 字母 / 思源宋体标语 9 字），
      各 ~2-3KB，font-display:swap 不阻塞渲染 ── */
@font-face {
  font-family: 'Fraunces';
  src: url('@/assets/fonts/fraunces-wordmark.woff2') format('woff2');
  font-weight: 600;
  font-style: normal;
  font-display: swap;
}

@font-face {
  font-family: 'Noto Serif SC';
  src: url('@/assets/fonts/noto-serif-sc-tagline.woff2') format('woff2');
  font-weight: 500;
  font-style: normal;
  font-display: swap;
}

.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  background: var(--bg-canvas);   /* 兜底，实际由 BloomGarden 覆盖 */
}

/* ---- 视差深度层 ---- */
.depth-layer--bg {
  position: absolute;
  inset: -16px;
  z-index: 0;
}

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

/* 视差 wrapper：transform 由 useParallaxDepth 写，
   卡片本体的入场/退场/shake transform 与它互不覆盖 */
.card-depth {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 400px;
}

/* ---- 磨砂玻璃卡片 ---- */
.login-card {
  position: relative;
  width: 100%;
  padding: 56px 48px 40px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: 22px;
  box-shadow:
    var(--glass-specular),
    -1px -1px 0 0 rgba(var(--rose-rgb), 0.14),
    1px 1px 0 0 rgba(var(--matcha-rgb), 0.12),
    var(--shadow-xl);
}

[data-theme='dark'] .login-card {
  box-shadow:
    var(--glass-specular),
    -1px -1px 0 0 rgba(var(--gold-rgb), 0.2),
    1px 1px 0 0 rgba(var(--rose-rgb), 0.1),
    var(--shadow-xl);
}

/* 入场 blur 渐清晰：与 v-motion 的 y/opacity spring 并行（filter 独立通道） */
.login-card.card-in-full {
  animation: card-deblur 0.5s cubic-bezier(0.22, 1, 0.36, 1) 0.45s both;
}

.login-card.card-in-quick {
  animation: card-deblur 0.3s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes card-deblur {
  from { filter: blur(8px); }
  to { filter: blur(0); }
}

/* 边缘辉光扫过（1100ms 触发一次，左→右，之后静止）：
   渐变画在 ::before，mask 挖成 1.5px 圆角环，只动 background-position/opacity */
.login-card::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  padding: 1.5px;
  background: linear-gradient(100deg, transparent 34%, rgba(var(--rose-rgb), 0.85) 50%, transparent 66%);
  background-size: 260% 100%;
  background-repeat: no-repeat;
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  mask-composite: exclude;
  opacity: 0;
  pointer-events: none;
}

[data-theme='dark'] .login-card::before {
  background-image: linear-gradient(100deg, transparent 34%, rgba(var(--gold-bright-rgb), 0.9) 50%, transparent 66%);
}

.login-card.is-sweep::before {
  animation: edge-sweep 1s cubic-bezier(0.4, 0, 0.2, 1) 1 forwards;
}

@keyframes edge-sweep {
  0% { opacity: 0; background-position: 200% 0; }
  12% { opacity: 1; }
  88% { opacity: 1; }
  100% { opacity: 0; background-position: -60% 0; }
}

/* 呼吸辉光层：输入聚焦时一次起伏，只动 opacity */
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

/* 登录失败：4px 幅度 3 次往复 shake（240ms）。
   CSS animation 的 transform 优先级高于 v-motion 留下的行内值，结束后自动还原 */
.login-card.is-shake {
  animation: card-shake 0.24s cubic-bezier(0.36, 0.07, 0.19, 0.97);
}

@keyframes card-shake {
  0%, 100% { transform: translateX(0); }
  16% { transform: translateX(-4px); }
  33% { transform: translateX(4px); }
  50% { transform: translateX(-4px); }
  66% { transform: translateX(4px); }
  83% { transform: translateX(-2px); }
}

/* 登录成功退场：缩小 + 淡出，随后交给 View Transitions 进仪表盘 */
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

/* ---- Brand mark ---- */
.brand {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
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

/* ---- 动态品牌名（kinetic wordmark） ---- */
.head {
  text-align: center;
  margin-bottom: 32px;
}

.wordmark {
  position: relative;
  display: inline-block;
  font-family: 'Fraunces', Georgia, 'Times New Roman', serif;
  font-size: 36px;
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: 0.005em;
  margin-bottom: 8px;
  white-space: nowrap;
}

/* 品牌渐变填充：垂直渐变逐字一致 → 整词视觉无缝；浅色玫瑰糖衣 / 暗色鎏金 */
.wm-letter {
  display: inline-block;
  background-image: linear-gradient(
    180deg,
    color-mix(in srgb, var(--rose) 62%, #fff) 0%,
    var(--rose) 52%,
    color-mix(in srgb, var(--rose) 62%, var(--amber)) 100%
  );
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

[data-theme='dark'] .wm-letter {
  background-image: linear-gradient(
    180deg,
    var(--gold-bright, #f0c96c) 0%,
    var(--gold, #d4a747) 55%,
    color-mix(in srgb, var(--gold, #d4a747) 68%, #5e4413) 100%
  );
}

/* 高光扫过：::after 复制整词（attr(data-text)），窄高光带 10s 周期内
   只占 12%（=1.2s）扫一次，其余 8.8s 完全静止；浅色白光 / 暗色金光 */
.wordmark::after {
  content: attr(data-text);
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: linear-gradient(115deg, transparent 42%, rgba(255, 255, 255, 0.85) 50%, transparent 58%);
  background-size: 280% 100%;
  background-repeat: no-repeat;
  background-position: 130% 0;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  opacity: 0;
}

[data-theme='dark'] .wordmark::after {
  background-image: linear-gradient(115deg, transparent 42%, rgba(255, 228, 158, 0.95) 50%, transparent 58%);
}

.wordmark.wm-live::after {
  animation: wm-sheen 10s linear 2.4s infinite;
}

@keyframes wm-sheen {
  0% { background-position: 130% 0; opacity: 0; }
  1.5% { opacity: 1; }
  10.5% { opacity: 1; }
  12% { background-position: -40% 0; opacity: 0; }
  12.001%, 100% { background-position: 130% 0; opacity: 0; }
}

.tagline {
  font-family: 'Noto Serif SC', 'Songti SC', 'SimSun', serif;
  font-size: 14px;
  color: var(--color-text-muted);
  letter-spacing: 0.14em;
}

/* ---- 浮动标签输入 ---- */
.field--float {
  position: relative;
  margin-bottom: 18px;
}

.field--float :deep(.el-input__wrapper) {
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 0 1px var(--color-border) inset !important;
  background: var(--color-bg-secondary);
  padding: 22px 14px 6px;
  transition: box-shadow var(--transition-fast);
}

.field--float :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--color-text-muted) inset !important;
}

.field--float :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-primary) inset !important;
}

/* 登录失败：输入框边缘短暂草莓红（≈0.9s 后由 JS 摘类淡回） */
.field--float.is-invalid :deep(.el-input__wrapper) {
  box-shadow:
    0 0 0 2px rgba(var(--rose-rgb), 0.9) inset,
    0 0 16px -4px rgba(var(--rose-rgb), 0.45) !important;
}

.field--float :deep(.el-input__inner) {
  font-size: 15px;
  height: 24px;
  caret-color: var(--color-primary);   /* 品牌色 caret */
}

.float-label {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  transform-origin: left center;
  font-size: 15px;
  color: var(--color-text-muted);
  letter-spacing: 0.02em;
  pointer-events: none;
  transition: transform 0.18s cubic-bezier(0.4, 0, 0.2, 1), color 0.18s ease;
}

.field--float.is-float .float-label {
  transform: translateY(-108%) scale(0.76);
}

.field--float.is-focus .float-label {
  color: var(--color-primary);
}

/* ---- 密码可见性切换（150ms 眨眼） ---- */
.pwd-eye {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 0;
  border: none;
  background: none;
  color: var(--color-text-muted);
  cursor: pointer;
  border-radius: 6px;
  transition: color 0.15s ease;
}

.pwd-eye:hover {
  color: var(--color-text-secondary);
}

.pwd-eye.is-blink svg {
  animation: eye-blink 0.15s ease;
}

@keyframes eye-blink {
  0%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(0.15); }
}

/* ---- 错误文字（固定占位淡入） ---- */
.form-error-slot {
  min-height: 20px;
  margin: -6px 0 2px;
}

.form-error {
  font-size: 13px;
  line-height: 20px;
  color: var(--rose);
}

.err-enter-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.err-leave-active {
  transition: opacity 0.15s ease;
}

.err-enter-from {
  opacity: 0;
  transform: translateY(-3px);
}

.err-leave-to {
  opacity: 0;
}

/* ---- 主按钮（自定义 loading：文字淡出 + 细线不确定进度，弃用 Element 转圈） ---- */
.login-btn {
  position: relative;
  width: 100%;
  height: 48px;
  margin-top: 8px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: -0.01em;
  border-radius: var(--radius-md) !important;
  background: var(--color-primary) !important;
  border: none !important;
  box-shadow: var(--glow-sm) !important;
  transition: background-color var(--transition-fast), box-shadow var(--transition-base), transform var(--transition-fast) !important;
}

.login-btn:hover {
  background: var(--color-primary-dark) !important;
  box-shadow: var(--glow-md) !important;
}

.login-btn:active {
  transform: scale(0.99);
}

/* loading 态：保持品牌底色（覆盖 Element disabled 淡化），文字淡出 */
.login-btn.is-busy,
.login-btn.is-busy:hover {
  background: var(--color-primary) !important;
  opacity: 1;
  cursor: default;
  pointer-events: none;
}

.btn-label {
  transition: opacity 0.2s ease;
}

.login-btn.is-busy .btn-label {
  opacity: 0;
}

/* 细线不确定进度：轨道 + 往复滑块，只动 transform */
.btn-progress {
  position: absolute;
  left: 24%;
  right: 24%;
  top: 50%;
  height: 2px;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.28);
  border-radius: 1px;
  overflow: hidden;
}

.btn-progress i {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 36%;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 1px;
  animation: btn-indeterminate 1s cubic-bezier(0.45, 0.05, 0.55, 0.95) infinite;
}

@keyframes btn-indeterminate {
  0% { transform: translateX(-110%); }
  100% { transform: translateX(290%); }
}

/* ---- Footer ---- */
.foot {
  margin-top: 28px;
  text-align: center;
  font-size: 12px;
  color: var(--color-text-muted);
  letter-spacing: 0.01em;
}

/* 双保险：static 档（JS 已早退）+ 系统 reduced-motion 下，
   全部装饰动画瞬时/停止 → 一张干净安静的静态卡片 */
@media (prefers-reduced-motion: reduce) {
  .login-card.card-in-full,
  .login-card.card-in-quick,
  .login-card.is-sweep::before,
  .login-card.is-glow::after,
  .login-card.is-shake,
  .login-card.is-leaving,
  .wordmark.wm-live::after,
  .pwd-eye.is-blink svg {
    animation: none;
  }

  .float-label,
  .btn-label,
  .err-enter-active,
  .err-leave-active {
    transition: none;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 44px 28px 32px;
  }

  .wordmark {
    font-size: 30px;
  }
}
</style>
