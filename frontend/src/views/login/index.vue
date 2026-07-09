<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/stores/modules/user'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import FluidGradient from '@/components/FluidGradient.vue'
import { useMotionPref } from '@/composables/useMotionPref'

const loginForm = ref({ username: '', password: '' })
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

// static 档（reduced-motion / 低端设备）→ 背景渐变静止
const { allowMotion } = useMotionPref()

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
  <div class="login-page">
    <!-- 奶油流体渐变背景（第一波 CSS 版；static 档静止） -->
    <FluidGradient :animated="allowMotion" />

    <div class="login-card glass">
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

/* 磨砂玻璃卡片（本页唯一的 backdrop-filter 层） */
.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  padding: 56px 48px 40px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: 22px;
  box-shadow: var(--glass-specular), var(--shadow-xl);
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

@media (max-width: 480px) {
  .login-card {
    padding: 44px 28px 32px;
  }
}
</style>
