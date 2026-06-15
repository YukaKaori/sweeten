<template>
  <div class="m-layout">
    <!-- 顶部 Header -->
    <header class="m-header">
      <div class="m-header__inner">
        <div class="m-brand">
          <span class="m-brand__icon">🍰</span>
          <span class="m-brand__name">DessertShop</span>
        </div>
        <div class="m-header__slogan">Premium Selection</div>
      </div>
    </header>

    <!-- 页面内容 -->
    <main class="m-main">
      <router-view v-slot="{ Component }">
        <transition name="m-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
// 移动端 Layout：轻量 header + 内容区，无侧边栏
</script>

<style scoped>
.m-layout {
  min-height: 100dvh;
  background: 
    radial-gradient(circle at 10% 20%, rgba(253, 232, 236, 0.6) 0%, transparent 40%),
    radial-gradient(circle at 90% 80%, rgba(245, 240, 252, 0.7) 0%, transparent 50%),
    linear-gradient(160deg, #fffafd 0%, #fdf8f5 60%, #f7f1fc 100%);
  display: flex;
  flex-direction: column;
}

/* ── Header ── */
.m-header {
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(30px) saturate(210%) brightness(1.02);
  -webkit-backdrop-filter: blur(30px) saturate(210%) brightness(1.02);
  background: rgba(255, 255, 255, 0.45);
  border-bottom: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow: 
    inset 0 1px 0 0 rgba(255, 255, 255, 0.6),
    0 8px 32px rgba(45, 35, 39, 0.03);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.m-header__inner {
  max-width: 430px;
  margin: 0 auto;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.m-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.m-brand__icon {
  font-size: 24px;
  line-height: 1;
  filter: drop-shadow(0 2px 5px rgba(232, 99, 122, 0.25));
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.m-brand:active .m-brand__icon {
  transform: scale(1.15) rotate(10deg);
}

.m-brand__name {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, #e8637a 30%, #c94d63 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.01em;
}

.m-header__slogan {
  font-size: 10px;
  font-weight: 600;
  color: #b0a0a7;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

/* ── Main ── */
.m-main {
  flex: 1;
  max-width: 430px;
  width: 100%;
  margin: 0 auto;
  padding-bottom: env(safe-area-inset-bottom, 16px);
}

/* ── 页面切换动画 ── */
.m-fade-enter-active,
.m-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.m-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.m-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
