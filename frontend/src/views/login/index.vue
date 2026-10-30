<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/modules/user'
import { useMotionPref } from '@/composables/useMotionPref'
import { BRAND, STAFF_COPY } from '@/config/brand'
import { firstAllowedPath } from '@/config/navigation'
// 主视觉与全站商品图同源（本地内置版本：登录页不依赖外网图床也能完整显示）
import { BRAND_HERO_IMAGE as heroImage, PRODUCT_IMAGE_FOCUS } from '@/config/productImage'
import BrandLogo from '@/components/brand/BrandLogo.vue'
import SwIcon from '@/components/brand/SwIcon.vue'

const REMEMBER_KEY = 'ds-remember-username'
const copy = STAFF_COPY.login
const year = new Date().getFullYear()

const router = useRouter()
const route = useRoute()
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
const heroLoaded = ref(false)

// 字段级错误 + 表单级错误
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

// 大写锁定提示
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

/** 登录后去哪：带回跳地址就回去（仅站内路径），否则去第一个有权限的页面 */
function landingPath() {
  const r = route.query.redirect
  if (typeof r === 'string' && r.startsWith('/') && !r.startsWith('//') && !r.startsWith('/login')) return r
  return firstAllowedPath(userStore.can)
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
        await new Promise((resolve) => setTimeout(resolve, 220))
      }
      router.replace(landingPath())
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
  <div
    class="login"
    :class="{ 'motion-on': allowMotion, 'is-leaving': leaving }"
  >
    <!-- 主视觉：品牌甜品摄影 -->
    <aside
      class="hero"
      aria-hidden="true"
    >
      <img
        class="hero__img"
        :class="{ 'is-loaded': heroLoaded }"
        :src="heroImage"
        :style="{ objectPosition: PRODUCT_IMAGE_FOCUS }"
        alt=""
        decoding="async"
        fetchpriority="high"
        @load="heroLoaded = true"
      />
      <div class="hero__scrim"></div>
      <div class="hero__top">
        <BrandLogo
          variant="horizontal"
          tone="inverse"
          :size="30"
        />
      </div>
      <div class="hero__copy">
        <p class="hero__eyebrow">{{ copy.heroSub }}</p>
        <p class="hero__title sw-display">{{ copy.heroTitle }}</p>
        <p class="hero__cat">{{ BRAND.category }}</p>
      </div>
    </aside>

    <!-- 登录面板 -->
    <main class="panel">
      <div
        class="panel__inner"
        :class="{ 'is-shake': shake }"
        @animationend="shake = false"
      >
        <header class="panel__head">
          <BrandLogo
            class="panel__logo"
            variant="stacked"
            :size="46"
          />
          <p class="sw-eyebrow panel__eyebrow">{{ copy.eyebrow }}</p>
          <h1 class="panel__title sw-display">{{ copy.title }}</h1>
          <p class="panel__lead">{{ copy.lead }}</p>
        </header>

        <form
          class="form"
          novalidate
          @submit.prevent="login"
        >
          <div
            class="field"
            :class="{ 'has-error': fieldError.username }"
          >
            <label
              class="field__label"
              for="login-username"
            >账号</label>
            <el-input
              id="login-username"
              ref="usernameInput"
              v-model="loginForm.username"
              size="large"
              name="username"
              autocomplete="username"
              placeholder="员工账号"
              :prefix-icon="User"
              :disabled="loading"
              :aria-invalid="!!fieldError.username"
              aria-describedby="login-username-err"
              @input="clearErrors('username')"
            />
            <p
              v-if="fieldError.username"
              id="login-username-err"
              class="field__error"
            >{{ fieldError.username }}</p>
          </div>

          <div
            class="field"
            :class="{ 'has-error': fieldError.password }"
          >
            <label
              class="field__label"
              for="login-password"
            >密码</label>
            <el-input
              id="login-password"
              ref="passwordInput"
              v-model="loginForm.password"
              type="password"
              show-password
              size="large"
              name="password"
              autocomplete="current-password"
              placeholder="登录密码"
              :prefix-icon="Lock"
              :disabled="loading"
              :aria-invalid="!!fieldError.password"
              aria-describedby="login-password-err"
              @input="clearErrors('password')"
              @keydown="detectCaps"
              @keyup="detectCaps"
              @blur="capsLock = false"
            />
            <p
              v-if="fieldError.password"
              id="login-password-err"
              class="field__error"
            >{{ fieldError.password }}</p>
            <p
              v-else-if="capsLock"
              class="field__hint"
            >大写锁定已开启</p>
          </div>

          <div class="form__row">
            <el-checkbox
              v-model="remember"
              :disabled="loading"
            >记住账号</el-checkbox>
          </div>

          <div
            class="form__error-slot"
            aria-live="polite"
          >
            <Transition name="err">
              <p
                v-if="errorMsg"
                class="form__error"
                role="alert"
              >
                <SwIcon
                  name="alert"
                  :size="16"
                />
                {{ errorMsg }}
              </p>
            </Transition>
          </div>

          <el-button
            class="submit"
            type="primary"
            size="large"
            native-type="submit"
            :loading="loading"
          >
            {{ loading ? '正在登录…' : '登录工作台' }}
          </el-button>
        </form>

        <router-link
          to="/m"
          class="panel__alt"
        >
          <span>
            <span class="panel__alt-title">不是门店员工？</span>
            <span class="panel__alt-desc">去喜甜商城挑选今日甜品</span>
          </span>
          <SwIcon
            name="arrow-right"
            :size="18"
          />
        </router-link>
      </div>

      <footer class="panel__foot">© {{ year }} {{ BRAND.fullName }} · {{ BRAND.tagline }}</footer>
    </main>
  </div>
</template>

<style scoped>
.login {
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(460px, 1fr);
  background: var(--sw-cream);
  color: var(--sw-cocoa);
}

/* ============ 主视觉 ============ */
.hero {
  position: relative;
  margin: 14px 0 14px 14px;
  border-radius: 24px;
  overflow: hidden;
  isolation: isolate;
  background: #e9dccd;
}

.hero__img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.6s var(--sw-ease);
}

.hero__img.is-loaded {
  opacity: 1;
}

.motion-on .hero__img.is-loaded {
  animation: hero-settle 1.4s var(--sw-ease) both;
}

@keyframes hero-settle {
  from { transform: scale(1.04); }
  to { transform: scale(1); }
}

/* 顶部轻压暗承托标志，底部加深承载文案（白字对比度 ≥ 4.5:1） */
.hero__scrim {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(var(--sw-scrim), 0.32) 0%, rgba(var(--sw-scrim), 0) 24%),
    linear-gradient(0deg, rgba(var(--sw-scrim), 0.78) 0%, rgba(var(--sw-scrim), 0.36) 34%, rgba(var(--sw-scrim), 0) 60%);
}

.hero__top {
  position: absolute;
  top: 28px;
  left: 32px;
}

.hero__copy {
  position: absolute;
  left: 40px;
  right: 40px;
  bottom: 40px;
  color: var(--sw-cream-text);
}

.hero__eyebrow {
  font-family: var(--sw-font-latin);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.28em;
  color: rgba(247, 243, 236, 0.86);
}

.hero__title {
  margin-top: 14px;
  font-size: clamp(34px, 3.6vw, 52px);
  line-height: 1.2;
  color: var(--sw-cream-text);
}

.hero__cat {
  margin-top: 14px;
  font-size: 14px;
  letter-spacing: 0.2em;
  color: rgba(247, 243, 236, 0.82);
}

/* ============ 登录面板 ============ */
.panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 40px 24px;
  min-width: 0;
}

.panel__inner {
  width: 100%;
  max-width: 380px;
  margin: auto 0;
}

.motion-on .panel__inner > * {
  animation: rise-in 0.5s var(--sw-ease) both;
}
.motion-on .panel__inner > :nth-child(2) { animation-delay: 0.05s; }
.motion-on .panel__inner > :nth-child(3) { animation-delay: 0.1s; }

@keyframes rise-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: none; }
}

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

.is-leaving .panel__inner {
  opacity: 0;
  transform: translateY(-6px);
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.panel__head {
  margin-bottom: 28px;
}

.panel__logo {
  margin-bottom: 36px;
}

.panel__eyebrow {
  color: var(--sw-berry);
}

.panel__title {
  margin-top: 10px;
  font-size: 32px;
  line-height: 1.25;
  color: var(--sw-cocoa);
}

.panel__lead {
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--sw-cocoa-soft);
}

/* ---- 表单 ---- */
.form {
  display: flex;
  flex-direction: column;
}

.field {
  margin-bottom: 16px;
}

.field__label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--sw-cocoa-soft);
}

.field :deep(.el-input__wrapper) {
  min-height: 48px;
  padding: 0 14px;
  border-radius: 12px;
  background: var(--sw-paper);
}

.field.has-error :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1.5px var(--sw-danger) inset;
}

.field :deep(.el-input__inner) {
  font-size: 15px;
  caret-color: var(--sw-berry);
}

.field__error,
.field__hint {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.4;
}

.field__error {
  color: var(--sw-danger);
}

.field__hint {
  color: var(--sw-warning);
}

.form__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.form__row :deep(.el-checkbox__label) {
  color: var(--sw-cocoa-soft);
  font-size: 13px;
}

/* 表单级错误：固定占位，出现/消失不引起布局跳动 */
.form__error-slot {
  min-height: 28px;
  margin: 6px 0 8px;
}

.form__error {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  line-height: 28px;
  color: var(--sw-danger);
}

.err-enter-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.err-leave-active { transition: opacity 0.15s ease; }
.err-enter-from { opacity: 0; transform: translateY(-3px); }
.err-leave-to { opacity: 0; }

.submit {
  width: 100%;
  height: 50px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.submit.is-loading {
  opacity: 1;
}

.panel__alt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 28px;
  padding: 14px 16px;
  border: 1px solid var(--sw-line);
  border-radius: var(--sw-r-lg);
  background: var(--sw-paper);
  color: var(--sw-cocoa);
  transition: border-color var(--sw-dur-1) var(--sw-ease), background-color var(--sw-dur-1) var(--sw-ease);
}

.panel__alt:hover {
  border-color: var(--sw-milktea);
  background: var(--sw-blush-wash);
}

.panel__alt > span {
  display: flex;
  flex-direction: column;
}

.panel__alt-title {
  font-size: 13px;
  font-weight: 600;
}

.panel__alt-desc {
  font-size: 12px;
  color: var(--sw-muted);
}

.panel__foot {
  margin-top: 32px;
  font-size: 12px;
  color: var(--sw-muted);
  letter-spacing: 0.02em;
}

/* ============ 响应式 ============ */
@media (max-width: 1100px) {
  .login {
    grid-template-columns: minmax(0, 1fr) minmax(420px, 1fr);
  }

  .hero__copy {
    left: 32px;
    right: 32px;
    bottom: 32px;
  }
}

/* 平板 / 手机：主视觉变为顶部横幅，面板叠在图片下沿 */
@media (max-width: 860px) {
  .login {
    display: block;
  }

  .hero {
    margin: 0;
    height: clamp(240px, 40vh, 380px);
    border-radius: 0;
  }

  .hero__top {
    top: 18px;
    left: 20px;
  }

  /* 面板会上叠 28px，底部压暗加重并上移，保证照片上的标语可读 */
  .hero__scrim {
    background:
      linear-gradient(180deg, rgba(var(--sw-scrim), 0.42) 0%, rgba(var(--sw-scrim), 0) 30%),
      linear-gradient(0deg, rgba(var(--sw-scrim), 0.88) 0%, rgba(var(--sw-scrim), 0.66) 34%, rgba(var(--sw-scrim), 0) 74%);
  }

  .hero__copy {
    left: 22px;
    right: 22px;
    bottom: 52px;
  }

  .hero__title {
    margin-top: 8px;
    font-size: 28px;
  }

  .hero__cat {
    display: none;
  }

  .panel {
    position: relative;
    margin: -28px 0 0;
    padding: 32px 22px 24px;
    border-radius: 24px 24px 0 0;
    background: var(--sw-cream);
  }

  .panel__logo {
    display: none;
  }

  .panel__head {
    margin-bottom: 22px;
  }

  .panel__title {
    font-size: 26px;
  }

  .panel__foot {
    margin-top: 24px;
    padding-bottom: env(safe-area-inset-bottom);
  }
}

@media (max-width: 360px) {
  .hero__title {
    font-size: 24px;
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
