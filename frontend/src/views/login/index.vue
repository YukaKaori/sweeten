<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/modules/user'
import { useMotionPref } from '@/composables/useMotionPref'
// 登录页主视觉与全站商品图同源（本地内置版本，登录页不依赖外网图床即可完整显示）
import { LOCAL_PRODUCT_IMAGE as heroImage } from '@/config/productImage'

const REMEMBER_KEY = 'ds-remember-username'

const router = useRouter()
const userStore = useUserStore()
const { allowMotion } = useMotionPref()

const rememberedName = (() => {
  try { return localStorage.getItem(REMEMBER_KEY) || '' } catch { return '' }
})()

const loginForm = ref({ username: rememberedName, password: '' })
const remember = ref(!!rememberedName)
const loading = ref(false)
const leaving = ref(false)
const usernameInput = ref(null)
const passwordInput = ref(null)

// ── 主视觉图加载完成后淡入，避免大图逐行出现 ──
const heroLoaded = ref(false)

// ── 字段级错误 + 表单级错误 ──
const fieldError = ref({ username: '', password: '' })
const errorMsg = ref('')
const shake = ref(false)

const clearErrors = (field) => {
  if (field) fieldError.value[field] = ''
  errorMsg.value = ''
}

function failFeedback(msg) {
  errorMsg.value = msg
  if (!allowMotion.value) return
  shake.value = false
  requestAnimationFrame(() => { shake.value = true })
}

// ── 大写锁定提示（密码框内） ──
const capsLock = ref(false)
const detectCaps = (e) => {
  if (typeof e.getModifierState === 'function') capsLock.value = e.getModifierState('CapsLock')
}

function validate() {
  const { username, password } = loginForm.value
  fieldError.value = {
    username: username.trim() ? '' : '请输入账号',
    password: password ? '' : '请输入密码',
  }
  if (fieldError.value.username) {
    usernameInput.value?.focus()
    return false
  }
  if (fieldError.value.password) {
    passwordInput.value?.focus()
    return false
  }
  return true
}

const login = async () => {
  if (loading.value) return
  errorMsg.value = ''
  if (!validate()) return

  loading.value = true
  try {
    const result = await userStore.login({
      username: loginForm.value.username.trim(),
      password: loginForm.value.password,
    })
    if (result.success) {
      try {
        if (remember.value) localStorage.setItem(REMEMBER_KEY, loginForm.value.username.trim())
        else localStorage.removeItem(REMEMBER_KEY)
      } catch { /* 隐私模式等场景忽略 */ }
      ElMessage.success(`欢迎回来，${result.data?.name || result.data?.username || ''}`)
      if (allowMotion.value) {
        leaving.value = true
        await new Promise((resolve) => setTimeout(resolve, 260))
      }
      router.push('/')
    } else {
      loginForm.value.password = ''
      failFeedback(result.msg || '账号或密码错误')
      passwordInput.value?.focus()
    }
  } catch (e) {
    failFeedback(e?.displayMessage || '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

let focusTimer = null
onMounted(() => {
  // 记住了账号就直接定位到密码框
  focusTimer = setTimeout(() => {
    ;(rememberedName ? passwordInput : usernameInput).value?.focus()
  }, 50)
})
onUnmounted(() => clearTimeout(focusTimer))
</script>

<template>
  <div class="login-page" :class="{ 'motion-on': allowMotion, 'is-leaving': leaving }">
    <!-- 主视觉：品牌甜品摄影 + 品牌叙事 -->
    <aside class="hero" aria-hidden="true">
      <img
        class="hero__img"
        :class="{ 'is-loaded': heroLoaded }"
        :src="heroImage"
        alt=""
        decoding="async"
        fetchpriority="high"
        @load="heroLoaded = true"
      />
      <div class="hero__scrim"></div>

      <div class="hero__brand">
        <svg width="22" height="22" viewBox="0 0 32 32" fill="none">
          <path d="M11 20c0-4.5 2.5-9 5-9s5 4.5 5 9" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
          <path d="M8 20h16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
          <circle cx="16" cy="8.5" r="1.3" fill="currentColor" />
        </svg>
        <span>DessertShop</span>
      </div>

      <div class="hero__copy">
        <p class="hero__eyebrow">Pâtisserie · 门店运营台</p>
        <h2 class="hero__title">让每一份甜，<br />都准时出炉、稳稳送达</h2>
        <p class="hero__sub">商品上新、订单流转、库存与会员，在一处从容打理。</p>
      </div>
    </aside>

    <!-- 登录面板 -->
    <main class="panel">
      <div class="panel__inner" :class="{ 'is-shake': shake }" @animationend="shake = false">
        <header class="panel__head">
          <div class="mark">
            <svg width="30" height="30" viewBox="0 0 32 32" fill="none">
              <circle cx="16" cy="16" r="14" fill="rgba(232,99,122,0.12)" stroke="var(--color-primary)" stroke-width="1.5" />
              <path d="M11 20c0-4.5 2.5-9 5-9s5 4.5 5 9" stroke="var(--color-primary)" stroke-width="1.5" stroke-linecap="round" fill="none" />
              <path d="M8 20h16" stroke="var(--color-primary)" stroke-width="1.5" stroke-linecap="round" />
            </svg>
            <span class="wordmark">DessertShop</span>
          </div>
          <h1 class="title">欢迎回来</h1>
          <p class="tagline">登录以进入管理后台</p>
        </header>

        <form class="form" novalidate @submit.prevent="login">
          <div class="field" :class="{ 'has-error': fieldError.username }">
            <label class="field__label" for="login-username">账号</label>
            <el-input
              id="login-username"
              ref="usernameInput"
              v-model="loginForm.username"
              size="large"
              name="username"
              autocomplete="username"
              placeholder="请输入员工账号"
              :prefix-icon="User"
              :disabled="loading"
              @input="clearErrors('username')"
            />
            <p v-if="fieldError.username" class="field__error">{{ fieldError.username }}</p>
          </div>

          <div class="field" :class="{ 'has-error': fieldError.password }">
            <label class="field__label" for="login-password">密码</label>
            <el-input
              id="login-password"
              ref="passwordInput"
              v-model="loginForm.password"
              type="password"
              show-password
              size="large"
              name="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              :disabled="loading"
              @input="clearErrors('password')"
              @keydown="detectCaps"
              @keyup="detectCaps"
              @blur="capsLock = false"
            />
            <p v-if="fieldError.password" class="field__error">{{ fieldError.password }}</p>
            <p v-else-if="capsLock" class="field__hint">大写锁定已开启</p>
          </div>

          <div class="form__row">
            <el-checkbox v-model="remember" :disabled="loading">记住账号</el-checkbox>
          </div>

          <div class="form-error-slot" aria-live="polite">
            <Transition name="err">
              <p v-if="errorMsg" class="form-error" role="alert">{{ errorMsg }}</p>
            </Transition>
          </div>

          <el-button
            class="submit"
            type="primary"
            size="large"
            native-type="submit"
            :loading="loading"
          >
            {{ loading ? '正在登录…' : '登录' }}
          </el-button>
        </form>

        <div class="panel__alt">
          <span>不是门店员工？</span>
          <router-link to="/m" class="panel__link">去顾客点单 →</router-link>
        </div>
      </div>

      <footer class="panel__foot">© DessertShop · 甜品门店管理系统</footer>
    </main>
  </div>
</template>

<style scoped>
/* 品牌名 Fraunces 子集（仅含 "DessertShop" 11 个字母）/ 标语思源宋体子集（仅含"登录以进入管理后台"）；
   其它文字一律使用系统无衬线，避免子集缺字导致的混排 */
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
  --hero-ink: 38, 22, 18;
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(440px, 1fr);
  background: var(--bg-canvas);
  color: var(--color-text-primary);
}

/* ============ 主视觉 ============ */
.hero {
  position: relative;
  margin: 16px 0 16px 16px;
  border-radius: 28px;
  overflow: hidden;
  isolation: isolate;
  background: #efe4dc;   /* 图片加载前的暖色底，与照片桌面同色系 */
  box-shadow: var(--shadow-lg);
}

.hero__img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 46% 52%;
  opacity: 0;
  transition: opacity 0.6s ease;
}

.hero__img.is-loaded {
  opacity: 1;
}

.motion-on .hero__img.is-loaded {
  animation: hero-settle 1.6s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes hero-settle {
  from { transform: scale(1.045); }
  to { transform: scale(1); }
}

/* 顶部轻压暗保证品牌标识可读，底部加深承载文案（文字对比度 ≥ 4.5:1） */
.hero__scrim {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(var(--hero-ink), 0.28) 0%, rgba(var(--hero-ink), 0) 22%),
    linear-gradient(0deg, rgba(var(--hero-ink), 0.78) 0%, rgba(var(--hero-ink), 0.42) 30%, rgba(var(--hero-ink), 0) 58%);
}

.hero__brand {
  position: absolute;
  top: 24px;
  left: 24px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px 8px 10px;
  border-radius: 999px;
  color: #fff;
  font-family: 'Fraunces', Georgia, serif;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.01em;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.28);
  backdrop-filter: blur(12px) saturate(140%);
  -webkit-backdrop-filter: blur(12px) saturate(140%);
}

.hero__copy {
  position: absolute;
  left: 44px;
  right: 44px;
  bottom: 44px;
  color: #fff;
  max-width: 560px;
}

.hero__eyebrow {
  margin: 0 0 14px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.82);
}

.hero__title {
  margin: 0 0 14px;
  color: #fff;   /* 覆盖全局 h2 文字色 */
  font-size: clamp(28px, 2.6vw, 40px);
  font-weight: 650;
  line-height: 1.28;
  letter-spacing: 0.01em;
  text-shadow: 0 1px 18px rgba(var(--hero-ink), 0.35);
}

.hero__sub {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.86);
}

/* ============ 登录面板 ============ */
.panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 40px 28px;
  min-width: 0;
}

.panel__inner {
  width: 100%;
  max-width: 380px;
  margin: auto 0;
}

.motion-on .panel__inner > * {
  animation: rise-in 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.motion-on .panel__inner > :nth-child(2) { animation-delay: 0.06s; }
.motion-on .panel__inner > :nth-child(3) { animation-delay: 0.12s; }

@keyframes rise-in {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: none; }
}

/* 登录失败：一次轻微横向抖动 */
.panel__inner.is-shake {
  animation: shake 0.32s cubic-bezier(0.36, 0.07, 0.19, 0.97);
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(5px); }
  60% { transform: translateX(-3px); }
  80% { transform: translateX(2px); }
}

/* 登录成功：面板淡出，随后交给 View Transitions 进入仪表盘 */
.is-leaving .panel__inner {
  opacity: 0;
  transform: translateY(-6px);
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.panel__head {
  margin-bottom: 32px;
}

.mark {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
  /* 与侧边栏 logo 同名：登录后 View Transitions 会把它平滑过渡到侧边栏位置 */
  view-transition-name: app-logo;
}

.wordmark {
  font-family: 'Fraunces', Georgia, 'Times New Roman', serif;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-primary);
  letter-spacing: 0.005em;
}

.title {
  margin: 0 0 8px;
  font-size: 30px;
  font-weight: 650;
  line-height: 1.2;
  letter-spacing: -0.01em;
}

.tagline {
  margin: 0;
  font-family: 'Noto Serif SC', 'Songti SC', serif;
  font-size: 15px;
  letter-spacing: 0.12em;
  color: var(--color-text-secondary);
}

/* ---- 表单 ---- */
.form {
  display: flex;
  flex-direction: column;
}

.field {
  margin-bottom: 18px;
}

.field__label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.field :deep(.el-input__wrapper) {
  min-height: 48px;
  padding: 0 14px;
  border-radius: 12px;
  background: var(--color-bg-secondary);
  box-shadow: 0 0 0 1px var(--color-border) inset;
  transition: box-shadow var(--transition-fast), background-color var(--transition-fast);
}

.field :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--color-text-muted) inset;
}

.field :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-primary) inset, 0 0 0 5px rgba(var(--rose-rgb), 0.12);
}

.field.has-error :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1.5px rgba(var(--rose-rgb), 0.9) inset;
}

.field :deep(.el-input__inner) {
  font-size: 15px;
  caret-color: var(--color-primary);
}

.field :deep(.el-input__prefix) {
  color: var(--color-text-muted);
}

.field__error,
.field__hint {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.4;
}

.field__error {
  color: var(--color-primary-dark);
}

.field__hint {
  color: var(--color-accent);
}

.form__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -2px 0 4px;
}

.form__row :deep(.el-checkbox__label) {
  color: var(--color-text-secondary);
  font-size: 13px;
}

/* 表单级错误：固定占位，避免出现/消失时布局跳动 */
.form-error-slot {
  min-height: 24px;
  margin: 6px 0 6px;
}

.form-error {
  margin: 0;
  font-size: 13px;
  line-height: 24px;
  color: var(--color-primary-dark);
}

.err-enter-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.err-leave-active { transition: opacity 0.15s ease; }
.err-enter-from { opacity: 0; transform: translateY(-3px); }
.err-leave-to { opacity: 0; }

.submit {
  width: 100%;
  height: 50px;
  border: none !important;
  border-radius: 12px !important;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.04em;
  background: var(--color-primary) !important;
  box-shadow: var(--glow-sm);
  transition: background-color var(--transition-fast), box-shadow var(--transition-base), transform var(--transition-fast);
}

.submit:hover {
  background: var(--color-primary-dark) !important;
  box-shadow: var(--glow-md);
}

.submit:active {
  transform: translateY(1px);
}

.submit:focus-visible {
  outline: 3px solid rgba(var(--rose-rgb), 0.35);
  outline-offset: 2px;
}

/* loading 时保持品牌色（覆盖 Element 的淡化） */
.submit.is-loading {
  opacity: 1;
  background: var(--color-primary) !important;
}

.panel__alt {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-light);
  font-size: 13px;
  color: var(--color-text-muted);
}

.panel__link {
  color: var(--color-primary-dark);
  font-weight: 600;
  text-decoration: none;
}

.panel__link:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.panel__foot {
  margin-top: 32px;
  font-size: 12px;
  color: var(--color-text-muted);
  letter-spacing: 0.02em;
}

/* 暗色主题：主视觉与面板沿用金色强调 */
[data-theme='dark'] .wordmark {
  color: var(--gold-bright, #f0c96c);
}

[data-theme='dark'] .submit {
  background: var(--gold, #d4a747) !important;
  color: var(--color-text-inverse) !important;
  box-shadow: none;
}

[data-theme='dark'] .submit:hover {
  background: var(--gold-bright, #f0c96c) !important;
}

[data-theme='dark'] .field :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--gold, #d4a747) inset, 0 0 0 5px rgba(212, 167, 71, 0.14);
}

[data-theme='dark'] .panel__link {
  color: var(--gold-bright, #f0c96c);
}

/* ============ 响应式 ============ */
@media (max-width: 1100px) {
  .login-page {
    grid-template-columns: minmax(0, 1fr) minmax(400px, 1fr);
  }

  .hero__copy {
    left: 32px;
    right: 32px;
    bottom: 32px;
  }
}

/* 平板/手机：主视觉变为顶部横幅，面板以卡片形式叠在图片下沿 */
@media (max-width: 860px) {
  .login-page {
    display: block;
  }

  .hero {
    margin: 0;
    border-radius: 0 0 28px 28px;
    height: clamp(240px, 38vh, 360px);
    box-shadow: none;
  }

  .hero__brand {
    top: 16px;
    left: 16px;
  }

  .hero__copy {
    left: 24px;
    right: 24px;
    bottom: 48px;
  }

  .hero__eyebrow {
    margin-bottom: 8px;
    font-size: 11px;
  }

  .hero__title {
    margin-bottom: 0;
    font-size: 24px;
  }

  .hero__sub {
    display: none;
  }

  .hero__scrim {
    background:
      linear-gradient(180deg, rgba(var(--hero-ink), 0.30) 0%, rgba(var(--hero-ink), 0) 26%),
      linear-gradient(0deg, rgba(var(--hero-ink), 0.82) 0%, rgba(var(--hero-ink), 0.5) 38%, rgba(var(--hero-ink), 0) 72%);
  }

  .panel {
    position: relative;
    margin: -24px 12px 0;
    padding: 32px 24px 24px;
    border-radius: 24px;
    background: var(--color-bg-secondary);
    box-shadow: var(--shadow-lg);
  }

  .panel__head {
    margin-bottom: 24px;
  }

  .mark {
    margin-bottom: 18px;
  }

  .title {
    font-size: 26px;
  }

  .panel__foot {
    margin-top: 24px;
    padding-bottom: env(safe-area-inset-bottom);
  }
}

@media (max-width: 360px) {
  .hero__title {
    font-size: 21px;
  }

  .panel {
    padding: 28px 18px 20px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero__img,
  .motion-on .hero__img.is-loaded,
  .motion-on .panel__inner > *,
  .panel__inner.is-shake {
    animation: none;
    transition: none;
  }
}
</style>
