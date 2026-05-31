<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="not-found-content glass-panel">
      <div class="error-code">404</div>
      <h2 class="error-title">页面不存在</h2>
      <p class="error-desc">抱歉，您访问的页面不存在或已被移除</p>
      <div class="error-actions">
        <el-button type="primary" size="large" @click="router.push('/')">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        <el-button size="large" @click="router.back()">
          <el-icon><Back /></el-icon>
          返回上页
        </el-button>
      </div>
    </div>
    <div class="error-decoration">
      <div class="deco-circle c1"></div>
      <div class="deco-circle c2"></div>
      <div class="deco-circle c3"></div>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-primary);
  position: relative;
  overflow: hidden;
}

.not-found-content {
  text-align: center;
  position: relative;
  z-index: 2;
  animation: fadeInUp 0.6s ease forwards;
  padding: 60px 80px;
  border-radius: var(--radius-xl);
  /* glass-panel provides the glass background */
}

.error-code {
  font-size: 120px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 16px;
  letter-spacing: -0.04em;
}

.error-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 12px;
}

.error-desc {
  font-size: 15px;
  color: var(--color-text-muted);
  margin-bottom: 36px;
}

.error-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.error-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  background: var(--color-primary-lighter);
  opacity: 0.5;
}

.c1 {
  width: 300px;
  height: 300px;
  top: -80px;
  right: -80px;
  animation: float 6s ease-in-out infinite;
}

.c2 {
  width: 200px;
  height: 200px;
  bottom: -60px;
  left: -40px;
  animation: float 6s ease-in-out infinite 2s;
}

.c3 {
  width: 120px;
  height: 120px;
  top: 40%;
  left: 15%;
  animation: float 6s ease-in-out infinite 4s;
}
</style>
