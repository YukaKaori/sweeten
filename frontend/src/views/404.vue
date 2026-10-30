<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'
import BrandLogo from '@/components/brand/BrandLogo.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 顾客迷路回商城，员工回工作台
const home = computed(() => {
  if (route.path.startsWith('/m')) return { to: '/m', label: '回到商城首页' }
  if (userStore.isLoggedIn) return { to: '/', label: '回到工作台' }
  return { to: '/m', label: '去商城逛逛' }
})

const goBack = () => {
  if (window.history.state?.back) router.back()
  else router.replace(home.value.to)
}
</script>

<template>
  <main class="not-found">
    <router-link
      to="/m"
      class="not-found__logo"
      aria-label="喜甜 SWEETEN 首页"
    >
      <BrandLogo
        variant="horizontal"
        :size="30"
      />
    </router-link>

    <section class="not-found__body">
      <p class="sw-eyebrow">404 · PAGE NOT FOUND</p>
      <h1 class="not-found__title">这一页走丢了</h1>
      <p class="not-found__desc">链接可能已经失效，或者页面被移动了。</p>
      <div class="not-found__actions">
        <el-button
          type="primary"
          size="large"
          @click="router.replace(home.to)"
        >{{ home.label }}</el-button>
        <el-button
          size="large"
          @click="goBack"
        >返回上一页</el-button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  padding: 28px clamp(20px, 5vw, 56px);
  background: var(--sw-bg);
}

.not-found__logo {
  align-self: flex-start;
}

.not-found__body {
  margin: auto 0;
  padding: 48px 0 96px;
  max-width: 520px;
}

.not-found__title {
  margin-top: 14px;
  font-size: clamp(30px, 4vw, 40px);
  font-weight: 600;
}

.not-found__desc {
  margin-top: 10px;
  font-size: 15px;
  color: var(--sw-ink-3);
}

.not-found__actions {
  display: flex;
  gap: 12px;
  margin-top: 32px;
  flex-wrap: wrap;
}
</style>
