<template>
  <div class="m-layout" :class="{ dark: isDark }">
    <!-- Liquid Glass Header -->
    <header
      class="m-header"
      :style="{
        backdropFilter: `blur(${headerBlurAmount}px) saturate(200%)`,
        WebkitBackdropFilter: `blur(${headerBlurAmount}px) saturate(200%)`,
        background: isDark ? `rgba(28, 28, 30, ${headerOpacity})` : `rgba(245, 245, 247, ${headerOpacity})`
      }"
    >
      <div class="m-header__inner">
        <div class="m-brand" @click="$router.push('/m/')">
          <span class="m-brand__icon">🍰</span>
          <span class="m-brand__name">DessertShop</span>
        </div>
        <div class="m-header__actions">
          <button class="m-icon-btn" @click="toggleDark" :title="isDark ? '浅色模式' : '深色模式'">
            {{ isDark ? '☀️' : '🌙' }}
          </button>
        </div>
      </div>
    </header>

    <!-- Page Content -->
    <main class="m-main" ref="mainRef" @scroll="onMainScroll">
      <router-view v-slot="{ Component }">
        <transition name="m-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- Bottom Tab Bar · iOS Style -->
    <nav class="bottom-nav" ref="bottomNav">
      <div class="nav-slider" ref="navSlider"></div>
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab"
        :class="{ on: activeTab === tab.key }"
        :ref="el => setTabRef(tab.key, el)"
        @click="switchTab(tab)"
      >
        <span class="tb-emoji">{{ tab.emoji }}</span>
        <span class="tb-label">{{ tab.label }}</span>
        <span v-if="tab.key === 'cart' && cartQty" class="tb-badge">{{ cartQty }}</span>
      </button>
    </nav>

    <!-- Toast -->
    <transition name="toast-fade">
      <div v-if="toastMsg" class="global-toast">{{ toastMsg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCartStore } from '@/stores/modules/cart'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()

const isDark = ref(localStorage.getItem('theme') === 'dark')
const toastMsg = ref('')
const bottomNav = ref(null)
const navSlider = ref(null)
const mainRef = ref(null)
const tabRefs = {}

// ========== Scroll-aware header ==========
const scrollY = ref(0)
const headerBlurAmount = computed(() => Math.min(scrollY.value / 80, 1) * 32)
const headerOpacity = computed(() => {
  const isDarkMode = isDark.value
  const base = isDarkMode ? 0.72 : 0.68
  return base + Math.min(scrollY.value / 80, 1) * 0.2
})

const onMainScroll = () => {
  if (mainRef.value) {
    scrollY.value = mainRef.value.scrollTop
  }
}

const tabs = [
  { key: 'home',  emoji: '🏠', label: '首页',   route: '/m/' },
  { key: 'order', emoji: '📋', label: '订单',   route: '/m/orders' },
  { key: 'cart',  emoji: '🛒', label: '购物车', route: '' },
  { key: 'profile', emoji: '👤', label: '我的', route: '/m/profile' },
]

const activeTab = computed(() => {
  const p = route.path
  if (p === '/m/' || p === '/m') return 'home'
  if (p.startsWith('/m/orders')) return 'order'
  if (p.startsWith('/m/profile')) return 'profile'
  return 'home'
})

const cartQty = computed(() => cartStore.totalQty)

function setTabRef(key, el) { if (el) tabRefs[key] = el }

function switchTab(tab) {
  if (tab.key === 'cart') {
    // Emit event for home page to open cart sheet — store a flag
    window.__openCartSheet?.()
    return
  }
  router.push(tab.route)
}

function positionSlider() {
  if (!navSlider.value || !bottomNav.value) return
  const tab = tabRefs[activeTab.value]
  if (!tab) return
  const navRect = bottomNav.value.getBoundingClientRect()
  const tabRect = tab.getBoundingClientRect()
  navSlider.value.style.left  = (tabRect.left - navRect.left) + 'px'
  navSlider.value.style.width = tabRect.width + 'px'
}

watch(activeTab, () => nextTick(positionSlider))

function toggleDark() {
  isDark.value = !isDark.value
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  document.documentElement.classList.toggle('dark', isDark.value)
}

// Global toast
window.__showToast = (msg) => {
  toastMsg.value = msg
  setTimeout(() => toastMsg.value = '', 2000)
}

onMounted(() => {
  nextTick(positionSlider)
  window.addEventListener('resize', positionSlider)
  if (isDark.value) document.documentElement.classList.add('dark')
})
</script>

<style scoped>
/* ============================================================
   Apple-Inspired Mobile Layout · Liquid Glass
   ============================================================ */

:root {
  --bg: #f5f5f7;
  --card-glass: rgba(255,255,255,0.72);
  --text: #1d1d1f;
  --text-sec: #86868b;
  --text-ter: #aeaeb2;
  --hairline: rgba(0,0,0,0.06);
  --accent: #d9574a;
}

/* Dark mode */
.dark {
  --bg: #1c1c1e;
  --card-glass: rgba(44,44,46,0.72);
  --text: #f5f5f7;
  --text-sec: #98989d;
  --text-ter: #636366;
  --hairline: rgba(255,255,255,0.08);
}

.m-layout {
  min-height: 100dvh;
  background:
    radial-gradient(ellipse 70% 50% at 15% 5%,  rgba(217,87,74,0.05) 0%, transparent 55%),
    radial-gradient(ellipse 60% 45% at 85% 90%, rgba(217,87,74,0.03) 0%, transparent 55%),
    var(--bg);
  display: flex; flex-direction: column;
  -webkit-font-smoothing: antialiased;
  color: var(--text);
  transition: background 0.4s;
}

.dark .m-layout {
  background:
    radial-gradient(ellipse 70% 50% at 15% 5%,  rgba(217,87,74,0.08) 0%, transparent 55%),
    radial-gradient(ellipse 60% 45% at 85% 90%, rgba(217,87,74,0.05) 0%, transparent 55%),
    var(--bg);
}

/* ── Header ── */
.m-header {
  position: sticky; top: 0; z-index: 100;
  border-bottom: 0.5px solid var(--hairline);
  box-shadow: inset 0 0.5px 0 rgba(255,255,255,0.7);
  transition: background 0.2s ease, border-color 0.4s;
}
.dark .m-header {
  box-shadow: inset 0 0.5px 0 rgba(255,255,255,0.06);
}
.m-header__inner {
  max-width: 430px; margin: 0 auto; padding: 14px 18px;
  display: flex; align-items: center; justify-content: space-between;
}
.m-brand { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.m-brand__icon {
  font-size: 22px;
  filter: drop-shadow(0 2px 5px rgba(217,87,74,0.25));
  transition: transform 0.35s cubic-bezier(0.34,1.56,0.64,1);
}
.m-brand:active .m-brand__icon { transform: scale(1.2) rotate(10deg); }
.m-brand__name {
  font-size: 18px; font-weight: 620; letter-spacing: -0.3px;
  background: linear-gradient(135deg, #d9574a 20%, #b8453a 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.m-header__actions { display: flex; gap: 6px; }
.m-icon-btn {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  background: none; font-size: 18px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.2s;
}
.m-icon-btn:active { background: rgba(0,0,0,0.04); }

/* ── Main — scrollable for scroll-aware header ── */
.m-main {
  flex: 1; max-width: 430px; width: 100%; margin: 0 auto;
  padding-bottom: calc(82px + env(safe-area-inset-bottom, 16px));
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  /* Subtle fade at top when scrolled */
  mask-image: linear-gradient(to bottom, transparent 0%, black 6px);
  -webkit-mask-image: linear-gradient(to bottom, transparent 0%, black 6px);
}

/* ── Bottom Tab Bar ── */
.bottom-nav {
  position: fixed; bottom: 0; left: 50%; transform: translateX(-50%);
  width: 100%; max-width: 430px; z-index: 100;
  height: 82px;
  padding: 6px 10px env(safe-area-inset-bottom, 16px);
  background: rgba(245,245,247,0.70);
  backdrop-filter: blur(28px) saturate(190%);
  -webkit-backdrop-filter: blur(28px) saturate(190%);
  border-top: 0.5px solid var(--hairline);
  box-shadow: inset 0 0.5px 0 rgba(255,255,255,0.6);
  display: flex; justify-content: space-around;
  position: relative;
  transition: background 0.4s;
}
.dark .bottom-nav { background: rgba(28,28,30,0.72); }
.nav-slider {
  position: absolute; top: 7px; height: 48px; border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow:
    0 2px 10px rgba(0,0,0,0.05),
    0 0 0 0.5px rgba(0,0,0,0.03),
    inset 0 0.5px 0 rgba(255,255,255,0.8);
  /* Liquid spring transition */
  transition: left 0.45s cubic-bezier(0.34, 1.56, 0.64, 1),
              width 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
  pointer-events: none; z-index: 0;
}
.dark .nav-slider {
  background: rgba(58, 58, 60, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow:
    0 2px 10px rgba(0,0,0,0.15),
    0 0 0 0.5px rgba(255,255,255,0.06),
    inset 0 0.5px 0 rgba(255,255,255,0.08);
}
.tab {
  position: relative; z-index: 1;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 1px; padding: 0; background: none; border: none; cursor: pointer;
  font-size: 10px; font-weight: 480;
  color: #8e8e93; flex: 1; height: 52px;
  -webkit-tap-highlight-color: transparent;
  transition: color 0.25s;
}
.dark .tab { color: #636366; }
.tab .tb-emoji { font-size: 22px; transition: transform 0.3s cubic-bezier(0.34,1.56,0.64,1); }
.tab.on { color: var(--text); font-weight: 560; }
.tab.on .tb-emoji { transform: translateY(-2px); }
.tb-label { font-size: 10px; }
.tb-badge {
  position: absolute; top: 4px; right: calc(50% - 24px);
  min-width: 17px; height: 17px; padding: 0 5px;
  background: var(--accent); color: #fff; font-size: 10px; font-weight: 650;
  border-radius: 9px; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 6px rgba(217,87,74,0.25);
  transition: transform 0.3s cubic-bezier(0.34,1.56,0.64,1);
}

/* ── Global Toast ── */
.global-toast {
  position: fixed; bottom: 120px; left: 50%; transform: translateX(-50%);
  background: rgba(29,29,31,0.88); backdrop-filter: blur(20px);
  color: #fff; padding: 10px 24px; border-radius: 999px;
  font-size: 13px; font-weight: 500; z-index: 300;
  border: 0.5px solid rgba(255,255,255,0.1); white-space: nowrap;
}
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s cubic-bezier(0.16,1,0.3,1); }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translateX(-50%) translateY(10px); }

/* ── Page Transitions ── */
.m-fade-enter-active, .m-fade-leave-active { transition: opacity 0.25s, transform 0.25s; }
.m-fade-enter-from { opacity: 0; transform: translateY(10px); }
.m-fade-leave-to { opacity: 0; transform: translateY(-10px); }
</style>
