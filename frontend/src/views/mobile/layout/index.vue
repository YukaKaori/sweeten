<script setup>
/**
 * 喜甜 SWEETEN 顾客商城外框（/m/*，免登录）
 * 桌面：品牌页头 + 导航 + 购物车抽屉；手机：精简页头 + 底部标签栏 + 购物车底部面板
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '@/stores/modules/cart'
import { useStoreUi } from '@/stores/modules/storeUi'
import { BRAND, STORE_COPY } from '@/config/brand'
import { formatPrice } from '@/utils/format'
import BrandLogo from '@/components/brand/BrandLogo.vue'
import SwIcon from '@/components/brand/SwIcon.vue'
import CartPanel from '@/components/store/CartPanel.vue'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const ui = useStoreUi()
const year = new Date().getFullYear()

// 这些页面自带底部操作栏，不显示标签栏 / 悬浮购物车条
const FOCUSED_PAGES = ['mobileProduct', 'mobileCheckout', 'mobileSuccess']
const focused = computed(() => FOCUSED_PAGES.includes(route.name))
const onHome = computed(() => route.name === 'mobileHome')
const showCartBar = computed(() => onHome.value && !cart.isEmpty)

const tabs = [
  { key: 'home', label: '首页', icon: 'home', to: '/m' },
  { key: 'orders', label: '订单', icon: 'receipt', to: '/m/orders' },
  { key: 'cart', label: '购物车', icon: 'bag' },
  { key: 'profile', label: '我的', icon: 'user', to: '/m/profile' },
]

const activeTab = computed(() => {
  if (route.name === 'mobileOrders') return 'orders'
  if (route.name === 'mobileProfile') return 'profile'
  if (route.name === 'mobileHome') return 'home'
  return ''
})

function onTab(tab) {
  if (tab.key === 'cart') ui.openCart()
  else router.push(tab.to)
}

// 页头滚动后加发丝线
const scrolled = ref(false)
const onScroll = () => {
  scrolled.value = window.scrollY > 8
}

// 加购时购物车角标轻跳一下
const bump = ref(false)
watch(() => cart.totalQty, (now, before) => {
  if (now > before) {
    bump.value = false
    requestAnimationFrame(() => { bump.value = true })
  }
})

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
  // 兼容旧链接：/m/?cart=1 直接展开购物车
  if (route.query.cart) {
    ui.openCart()
    router.replace({ path: route.path, query: {} })
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  ui.closeCart()
})
</script>

<template>
  <div
    class="store"
    :class="{ 'is-focused': focused, 'has-cartbar': showCartBar }"
  >
    <a
      class="store__skip"
      href="#store-main"
    >跳到主要内容</a>

    <!-- ===== 页头 ===== -->
    <header
      class="sh"
      :class="{ 'is-scrolled': scrolled }"
    >
      <div class="sh__inner">
        <router-link
          to="/m"
          class="sh__logo"
          :aria-label="`${BRAND.fullName} 首页`"
        >
          <BrandLogo
            variant="horizontal"
            :size="30"
            class="sh__logo-full"
          />
          <BrandLogo
            variant="horizontal"
            :size="25"
            class="sh__logo-compact"
          />
        </router-link>

        <nav
          class="sh__nav"
          aria-label="商城导航"
        >
          <router-link
            to="/m"
            class="sh__link"
            :class="{ 'is-active': onHome && !route.hash }"
          >首页</router-link>
          <router-link
            :to="{ path: '/m', hash: '#menu' }"
            class="sh__link"
            :class="{ 'is-active': onHome && route.hash === '#menu' }"
          >甜品菜单</router-link>
          <router-link
            to="/m/orders"
            class="sh__link"
            active-class="is-active"
          >我的订单</router-link>
          <router-link
            to="/m/profile"
            class="sh__link"
            active-class="is-active"
          >会员中心</router-link>
        </nav>

        <div class="sh__actions">
          <button
            type="button"
            class="sh__cart"
            :aria-label="cart.totalQty ? `购物车，${cart.totalQty} 件，合计 ${formatPrice(cart.totalAmount)}` : '购物车，空'"
            @click="ui.openCart()"
          >
            <span class="sh__cart-icon">
              <SwIcon
                name="bag"
                :size="21"
              />
              <span
                v-if="cart.totalQty"
                class="sh__badge sw-num"
                :class="{ 'is-bump': bump }"
                @animationend="bump = false"
              >{{ cart.totalQty }}</span>
            </span>
            <span class="sh__cart-text">
              <span>购物车</span>
              <span
                v-if="cart.totalQty"
                class="sh__cart-sum sw-num"
              >{{ formatPrice(cart.totalAmount) }}</span>
            </span>
          </button>
        </div>
      </div>
    </header>

    <!-- ===== 内容 ===== -->
    <main
      id="store-main"
      class="store__main"
      tabindex="-1"
    >
      <router-view />
    </main>

    <!-- ===== 页脚 ===== -->
    <footer
      v-if="!focused"
      class="sf"
    >
      <div class="sf__inner">
        <div class="sf__brand">
          <BrandLogo
            variant="stacked"
            :size="44"
          />
          <p class="sf__tagline sw-display">{{ BRAND.taglineCn }}</p>
          <p class="sf__cat">{{ BRAND.category }}</p>
        </div>
        <div class="sf__cols">
          <div class="sf__col">
            <p class="sf__head">逛逛</p>
            <router-link :to="{ path: '/m', hash: '#menu' }">今日菜单</router-link>
            <router-link :to="{ path: '/m', hash: '#bestsellers' }">人气甜品</router-link>
          </div>
          <div class="sf__col">
            <p class="sf__head">我的</p>
            <router-link to="/m/orders">我的订单</router-link>
            <router-link to="/m/profile">会员中心</router-link>
          </div>
          <div class="sf__col sf__col--wide">
            <p class="sf__head">服务说明</p>
            <p
              v-for="p in STORE_COPY.promises"
              :key="p.key"
            >{{ p.title }} · {{ p.desc }}</p>
            <p>{{ STORE_COPY.paymentNote }}</p>
          </div>
        </div>
      </div>
      <div class="sf__bottom">
        <span>© {{ year }} {{ BRAND.fullName }} · {{ BRAND.tagline }}</span>
        <router-link
          to="/login"
          class="sf__staff"
        >员工登录</router-link>
      </div>
    </footer>

    <!-- ===== 手机：悬浮购物车条 ===== -->
    <Transition name="rise">
      <div
        v-if="showCartBar"
        class="cartbar"
      >
        <button
          type="button"
          class="cartbar__summary"
          :aria-label="`查看购物车，${cart.totalQty} 件`"
          @click="ui.openCart()"
        >
          <span class="cartbar__icon">
            <SwIcon
              name="bag"
              :size="20"
            />
            <span class="cartbar__count sw-num">{{ cart.totalQty }}</span>
          </span>
          <span class="cartbar__total sw-num">{{ formatPrice(cart.totalAmount) }}</span>
          <span class="cartbar__hint">配送费全免</span>
        </button>
        <router-link
          to="/m/checkout"
          class="cartbar__go"
        >去结算</router-link>
      </div>
    </Transition>

    <!-- ===== 手机：底部标签栏 ===== -->
    <nav
      v-if="!focused"
      class="tabbar"
      aria-label="商城主导航"
    >
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        class="tabbar__item"
        :class="{ 'is-active': activeTab === tab.key }"
        :aria-current="activeTab === tab.key ? 'page' : undefined"
        @click="onTab(tab)"
      >
        <span class="tabbar__icon">
          <SwIcon
            :name="tab.icon"
            :size="22"
          />
          <span
            v-if="tab.key === 'cart' && cart.totalQty"
            class="tabbar__badge sw-num"
          >{{ cart.totalQty }}</span>
        </span>
        <span class="tabbar__label">{{ tab.label }}</span>
      </button>
    </nav>

    <CartPanel />

    <!-- ===== 轻提示 ===== -->
    <div
      class="toast-region"
      aria-live="polite"
      aria-atomic="true"
    >
      <Transition name="toast">
        <div
          v-if="ui.toast"
          :key="ui.toast.id"
          class="toast"
          :class="`is-${ui.toast.tone}`"
        >
          <SwIcon
            v-if="ui.toast.tone === 'success'"
            name="check"
            :size="16"
            :stroke="2"
          />
          <SwIcon
            v-else-if="ui.toast.tone === 'warning'"
            name="info"
            :size="16"
          />
          {{ ui.toast.text }}
        </div>
      </Transition>
    </div>
  </div>
</template>

<style scoped>
.store {
  --store-max: 1200px;
  --store-pad: clamp(16px, 4vw, 40px);
  --tabbar-h: 64px;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  background: var(--sw-bg);
  color: var(--sw-ink);
}

.store__skip {
  position: absolute;
  left: 12px;
  top: -48px;
  z-index: 1000;
  padding: 8px 14px;
  border-radius: var(--sw-r-md);
  background: var(--sw-primary);
  color: var(--sw-on-primary);
}

.store__skip:focus {
  top: 12px;
}

/* ---------- 页头 ---------- */
.sh {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--sw-bg);
  border-bottom: 1px solid transparent;
  transition: border-color var(--sw-dur-2) var(--sw-ease);
}

.sh.is-scrolled {
  border-bottom-color: var(--sw-border);
}

.sh__inner {
  display: flex;
  align-items: center;
  gap: 32px;
  max-width: var(--store-max);
  height: 72px;
  margin: 0 auto;
  padding: 0 var(--store-pad);
}

.sh__logo {
  display: inline-flex;
  border-radius: var(--sw-r-sm);
}

.sh__logo-compact {
  display: none;
}

.sh__nav {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-right: auto;
}

.sh__link {
  position: relative;
  padding: 8px 14px;
  border-radius: var(--sw-r-pill);
  font-size: 14px;
  font-weight: 500;
  color: var(--sw-ink-2);
  transition: color var(--sw-dur-1) var(--sw-ease), background-color var(--sw-dur-1) var(--sw-ease);
}

.sh__link:hover {
  color: var(--sw-ink);
  background: var(--sw-fill-soft);
}

.sh__link.is-active {
  color: var(--sw-ink);
  font-weight: 600;
}

.sh__link.is-active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 2px;
  width: 5px;
  height: 5px;
  margin-left: -2.5px;
  border-radius: 50%;
  background: var(--sw-cherry);
}

.sh__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sh__cart {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  padding: 0 16px 0 12px;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-pill);
  background: var(--sw-surface);
  color: var(--sw-ink);
  cursor: pointer;
  transition: border-color var(--sw-dur-1) var(--sw-ease), background-color var(--sw-dur-1) var(--sw-ease);
}

.sh__cart:hover {
  border-color: var(--sw-milktea);
  background: var(--sw-blush-wash);
}

.sh__cart-icon {
  position: relative;
  display: inline-flex;
}

.sh__badge,
.tabbar__badge,
.cartbar__count {
  position: absolute;
  top: -6px;
  right: -9px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--sw-berry);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
}

.sh__badge.is-bump {
  animation: bump 0.4s var(--sw-ease);
}

@keyframes bump {
  0% { transform: scale(1); }
  40% { transform: scale(1.25); }
  100% { transform: scale(1); }
}

.sh__cart-text {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
}

.sh__cart-sum {
  font-weight: 700;
}

/* ---------- 内容 ---------- */
.store__main {
  flex: 1;
  width: 100%;
  outline: none;
}

/* ---------- 页脚 ---------- */
.sf {
  margin-top: 72px;
  border-top: 1px solid var(--sw-border);
  background: var(--sw-surface);
}

.sf__inner {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 2fr;
  gap: 48px;
  max-width: var(--store-max);
  margin: 0 auto;
  padding: 56px var(--store-pad) 40px;
}

.sf__tagline {
  margin-top: 22px;
  font-size: 20px;
  color: var(--sw-ink);
}

.sf__cat {
  margin-top: 6px;
  font-size: 13px;
  letter-spacing: 0.16em;
  color: var(--sw-ink-3);
}

.sf__cols {
  display: grid;
  grid-template-columns: 1fr 1fr 2fr;
  gap: 32px;
}

.sf__col {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 14px;
  color: var(--sw-ink-2);
}

.sf__col a:hover {
  color: var(--sw-accent);
}

.sf__col--wide p {
  font-size: 13px;
  line-height: 1.6;
  color: var(--sw-ink-3);
}

.sf__head {
  margin-bottom: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--sw-ink);
}

.sf__bottom {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  max-width: var(--store-max);
  margin: 0 auto;
  padding: 18px var(--store-pad) 28px;
  border-top: 1px solid var(--sw-border-soft);
  font-size: 12px;
  color: var(--sw-ink-3);
}

.sf__staff:hover {
  color: var(--sw-ink);
}

/* ---------- 手机：标签栏 + 悬浮购物车条 ---------- */
.tabbar,
.cartbar {
  display: none;
}

.toast-region {
  position: fixed;
  left: 50%;
  bottom: 32px;
  z-index: 2100;
  transform: translateX(-50%);
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: min(92vw, 420px);
  padding: 11px 18px;
  border-radius: var(--sw-r-pill);
  background: var(--sw-cocoa);
  color: #fffefc;
  font-size: 13px;
  font-weight: 500;
  box-shadow: var(--sw-shadow-md);
}

.toast.is-success .sw-icon {
  color: var(--sw-blush);
}

.toast.is-warning .sw-icon {
  color: #e9c98b;
}

.toast-enter-active,
.toast-leave-active {
  transition: opacity var(--sw-dur-2) var(--sw-ease), transform var(--sw-dur-2) var(--sw-ease);
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 1023px) {
  .sh__nav {
    display: none;
  }

  .sh__inner {
    justify-content: space-between;
  }

  .sf__inner {
    grid-template-columns: 1fr;
    gap: 32px;
  }
}

@media (max-width: 767px) {
  .sh__inner {
    height: 60px;
  }

  .sh__logo-full {
    display: none;
  }

  .sh__logo-compact {
    display: inline-flex;
  }

  .sh__cart {
    width: 44px;
    padding: 0;
    justify-content: center;
    border: 0;
    background: transparent;
  }

  .sh__cart-text {
    display: none;
  }

  .sf {
    margin-top: 48px;
  }

  .sf__inner {
    padding: 40px var(--store-pad) 24px;
  }

  .sf__cols {
    grid-template-columns: 1fr 1fr;
    gap: 24px;
  }

  .sf__col--wide {
    grid-column: 1 / -1;
  }

  .sf__bottom {
    flex-direction: column;
    gap: 6px;
  }

  .store:not(.is-focused) .sf {
    padding-bottom: calc(var(--tabbar-h) + env(safe-area-inset-bottom));
  }

  .store.has-cartbar .sf {
    padding-bottom: calc(var(--tabbar-h) + 72px + env(safe-area-inset-bottom));
  }

  .tabbar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 120;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    height: calc(var(--tabbar-h) + env(safe-area-inset-bottom));
    padding-bottom: env(safe-area-inset-bottom);
    background: var(--sw-surface);
    border-top: 1px solid var(--sw-border);
  }

  .tabbar__item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    border: 0;
    background: none;
    color: var(--sw-ink-3);
    font-size: 11px;
    font-weight: 500;
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;
  }

  .tabbar__item.is-active {
    color: var(--sw-ink);
    font-weight: 600;
  }

  .tabbar__item.is-active .tabbar__icon::after {
    content: '';
    position: absolute;
    left: 50%;
    top: -9px;
    width: 18px;
    height: 3px;
    margin-left: -9px;
    border-radius: 0 0 3px 3px;
    background: var(--sw-cherry);
  }

  .tabbar__icon {
    position: relative;
    display: inline-flex;
  }

  .cartbar {
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: calc(var(--tabbar-h) + 10px + env(safe-area-inset-bottom));
    z-index: 110;
    display: flex;
    align-items: center;
    gap: 8px;
    height: 60px;
    padding: 6px 6px 6px 8px;
    border-radius: var(--sw-r-pill);
    background: var(--sw-cocoa);
    color: #fffefc;
    box-shadow: var(--sw-shadow-lg);
  }

  .cartbar__summary {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 12px;
    height: 100%;
    padding: 0 8px;
    border: 0;
    border-radius: var(--sw-r-pill);
    background: transparent;
    color: inherit;
    text-align: left;
    cursor: pointer;
  }

  .cartbar__icon {
    position: relative;
    display: inline-flex;
  }

  .cartbar__count {
    background: var(--sw-blush);
    color: var(--sw-cocoa);
  }

  .cartbar__total {
    font-size: 17px;
    font-weight: 700;
  }

  .cartbar__hint {
    font-size: 12px;
    color: rgba(255, 254, 252, 0.7);
  }

  .cartbar__go {
    display: grid;
    place-items: center;
    height: 48px;
    padding: 0 24px;
    border-radius: var(--sw-r-pill);
    background: var(--sw-blush);
    color: var(--sw-cocoa);
    font-size: 15px;
    font-weight: 700;
  }

  .toast-region {
    bottom: calc(var(--tabbar-h) + 20px + env(safe-area-inset-bottom));
  }

  .store.has-cartbar .toast-region {
    bottom: calc(var(--tabbar-h) + 84px + env(safe-area-inset-bottom));
  }

  .store.is-focused .toast-region {
    bottom: calc(96px + env(safe-area-inset-bottom));
  }
}

.rise-enter-active,
.rise-leave-active {
  transition: opacity var(--sw-dur-2) var(--sw-ease), transform var(--sw-dur-3) var(--sw-ease);
}

.rise-enter-from,
.rise-leave-to {
  opacity: 0;
  transform: translateY(16px);
}
</style>
