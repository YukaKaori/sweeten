<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/stores/modules/user'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'

const loginForm = ref({ username: '', password: '' })
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

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

const clear = () => {
  loginForm.value = { username: '', password: '' }
}

const handleKeyup = (e) => {
  if (e.key === 'Enter') login()
}
</script>

<template>
  <div class="login-page">
    <!-- Left decorative panel -->
    <div class="login-left">
      <div class="left-content">
        <div class="brand-area">
          <div class="brand-icon">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <circle cx="24" cy="24" r="22" stroke="white" stroke-width="2" fill="rgba(255,255,255,0.1)"/>
              <path d="M16 28c0-6 4-12 8-12s8 6 8 12" stroke="white" stroke-width="2" stroke-linecap="round" fill="none"/>
              <path d="M12 28h24" stroke="white" stroke-width="2" stroke-linecap="round"/>
              <circle cx="20" cy="20" r="2" fill="rgba(255,255,255,0.6)"/>
              <circle cx="28" cy="18" r="1.5" fill="rgba(255,255,255,0.4)"/>
              <circle cx="24" cy="15" r="1" fill="rgba(255,255,255,0.5)"/>
            </svg>
          </div>
          <h1 class="brand-title">DessertShop</h1>
          <p class="brand-subtitle">甜品管理系统</p>
        </div>
        <div class="features">
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>高效管理甜品订单与库存</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>实时数据统计与分析</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>员工与部门协同管理</span>
          </div>
        </div>
      </div>
      <!-- Floating decorative elements -->
      <div class="float-circle c1"></div>
      <div class="float-circle c2"></div>
      <div class="float-circle c3"></div>
      <div class="float-circle c4"></div>
    </div>

    <!-- Right login form -->
    <div class="login-right">
      <div class="login-form-wrapper glass-panel">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>请登录您的账户以继续</p>
        </div>
        <el-form :model="loginForm" class="login-form" @keyup="handleKeyup">
          <el-form-item>
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item>
            <el-input
              type="password"
              v-model="loginForm.password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          <el-form-item>
            <el-button
              class="login-btn"
              type="primary"
              size="large"
              :loading="loading"
              @click="login"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
          <el-form-item>
            <el-button
              class="reset-btn"
              size="large"
              @click="clear"
            >
              重 置
            </el-button>
          </el-form-item>
        </el-form>
        <div class="form-footer">
          <span class="footer-text">DessertShop Management System</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  overflow: hidden;
}

/* ---- Left Panel ---- */
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #e8637a 0%, #c94d63 40%, #a33d52 70%, #7d2f40 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 48px;
}

.left-content {
  position: relative;
  z-index: 2;
  color: white;
  animation: fadeInUp 0.8s ease forwards;
}

.brand-area {
  margin-bottom: 48px;
}

.brand-icon {
  margin-bottom: 20px;
}

.brand-title {
  font-family: var(--font-display);
  font-size: 42px;
  font-weight: 700;
  color: white;
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

.brand-subtitle {
  font-size: 16px;
  opacity: 0.85;
  font-weight: 300;
  letter-spacing: 0.1em;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  opacity: 0.9;
  font-weight: 400;
}

.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
}

/* Floating circles */
.float-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  animation: float 6s ease-in-out infinite;
}

.c1 {
  width: 200px;
  height: 200px;
  top: -50px;
  right: -60px;
  animation-delay: 0s;
}

.c2 {
  width: 120px;
  height: 120px;
  bottom: 10%;
  left: -30px;
  animation-delay: 1.5s;
}

.c3 {
  width: 80px;
  height: 80px;
  top: 30%;
  right: 15%;
  background: rgba(255, 255, 255, 0.04);
  animation-delay: 3s;
}

.c4 {
  width: 160px;
  height: 160px;
  bottom: -40px;
  right: 20%;
  animation-delay: 2s;
}

/* ---- Right Panel ---- */
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-primary);
  padding: 48px;
}

.login-form-wrapper {
  width: 100%;
  max-width: 380px;
  animation: fadeInUp 0.6s ease 0.2s forwards;
  opacity: 0;
  padding: 40px 36px;
  border-radius: var(--radius-xl);
  /* glass-panel provides the glass background */
}

.form-header {
  margin-bottom: 40px;
}

.form-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.form-header p {
  font-size: 15px;
  color: var(--color-text-muted);
}

.login-form :deep(.el-input__wrapper) {
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 0 1px var(--color-border) inset !important;
  padding: 4px 12px;
  background: var(--color-bg-secondary);
  transition: all var(--transition-base);
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--color-primary-light) inset !important;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--color-primary) inset, var(--shadow-glow) !important;
}

.login-form :deep(.el-input__inner) {
  font-size: 15px;
  height: 24px;
}

.login-form :deep(.el-input__prefix) {
  color: var(--color-text-muted);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark)) !important;
  border: none !important;
  box-shadow: 0 4px 16px rgba(232, 99, 122, 0.3) !important;
  transition: all var(--transition-base) !important;
  letter-spacing: 0.1em;
}

.login-btn:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 8px 24px rgba(232, 99, 122, 0.4) !important;
}

.login-btn:active {
  transform: translateY(0) !important;
}

.reset-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  border-radius: var(--radius-md) !important;
  background: transparent !important;
  border: 1px solid var(--color-border) !important;
  color: var(--color-text-secondary) !important;
}

.reset-btn:hover {
  border-color: var(--color-primary-light) !important;
  color: var(--color-primary) !important;
}

.form-footer {
  margin-top: 48px;
  text-align: center;
}

.footer-text {
  font-size: 12px;
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
}

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .login-left {
    display: none;
  }

  .login-right {
    padding: 24px;
  }
}
</style>
