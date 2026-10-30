<script setup>
/**
 * 购物车面板：桌面端右侧抽屉，手机端底部面板。
 * 打开时按最新在售商品校正购物车（价格 / 库存 / 下架），并如实提示变化；
 * 最终金额与库存以下单时后端核算为准。
 */
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/modules/cart'
import { useStoreUi } from '@/stores/modules/storeUi'
import { useCatalog } from '@/composables/useCatalog'
import { formatPrice } from '@/utils/format'
import { STORE_COPY } from '@/config/brand'
import ProductImg from '@/components/ProductImg.vue'
import QtyStepper from '@/components/QtyStepper.vue'
import SwIcon from '@/components/brand/SwIcon.vue'

const router = useRouter()
const cart = useCartStore()
const ui = useStoreUi()
const catalog = useCatalog()

const panelRef = ref(null)
const syncNotes = ref([])
const confirmingClear = ref(false)
let lastFocus = null

const open = computed(() => ui.cartOpen)

async function syncCart() {
  syncNotes.value = []
  if (cart.isEmpty) return
  await catalog.load()
  if (catalog.error.value) return // 拉取失败时不校正，避免误删
  const { removed, adjusted, repriced } = cart.syncWithProducts(catalog.products.value)
  const notes = []
  if (removed.length) notes.push(`${removed.join('、')} 已下架或售罄，已移出购物车`)
  if (adjusted.length) notes.push(`${adjusted.join('、')} 库存不足，数量已调整`)
  if (repriced.length) notes.push(`${repriced.join('、')} 价格有更新`)
  syncNotes.value = notes
}

watch(open, async (v) => {
  if (v) {
    lastFocus = document.activeElement
    confirmingClear.value = false
    document.documentElement.style.overflow = 'hidden'
    await nextTick()
    panelRef.value?.querySelector('[data-autofocus]')?.focus()
    syncCart()
  } else {
    document.documentElement.style.overflow = ''
    lastFocus?.focus?.()
  }
})

function close() {
  ui.closeCart()
}

function onKey(e) {
  if (!open.value) return
  if (e.key === 'Escape') close()
  // 简单焦点圈定：Tab 不离开面板
  if (e.key === 'Tab' && panelRef.value) {
    const nodes = [...panelRef.value.querySelectorAll('button:not([disabled]), a[href], input')]
    if (!nodes.length) return
    const first = nodes[0]
    const last = nodes[nodes.length - 1]
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault()
      last.focus()
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault()
      first.focus()
    }
  }
}

onMounted(() => document.addEventListener('keydown', onKey))
onUnmounted(() => {
  document.removeEventListener('keydown', onKey)
  document.documentElement.style.overflow = ''
})

function increase(item) {
  if (!cart.addItem(item)) ui.showToast(`「${item.name}」库存只剩 ${item.stock} 件`, 'warning')
}

function checkout() {
  close()
  router.push('/m/checkout')
}

function browse() {
  close()
  router.push({ path: '/m', hash: '#menu' })
}

function clearCart() {
  if (!confirmingClear.value) {
    confirmingClear.value = true
    return
  }
  cart.clear()
  confirmingClear.value = false
  syncNotes.value = []
}

const atLimit = (item) => item.stock != null && item.qty >= item.stock
</script>

<template>
  <Teleport to="body">
    <Transition name="cart">
      <div
        v-if="open"
        class="cart-layer"
        @click.self="close"
      >
        <section
          ref="panelRef"
          class="cart"
          role="dialog"
          aria-modal="true"
          aria-labelledby="cart-title"
        >
          <header class="cart__head">
            <div>
              <h2
                id="cart-title"
                class="cart__title sw-display"
              >{{ STORE_COPY.titles.cart }}</h2>
              <p
                v-if="!cart.isEmpty"
                class="cart__count"
              >共 {{ cart.totalQty }} 件</p>
            </div>
            <button
              type="button"
              class="cart__close"
              aria-label="关闭购物车"
              data-autofocus
              @click="close"
            >
              <SwIcon
                name="close"
                :size="20"
              />
            </button>
          </header>

          <div
            v-if="syncNotes.length"
            class="cart__notice"
            role="status"
          >
            <SwIcon
              name="info"
              :size="16"
            />
            <span>{{ syncNotes.join('；') }}</span>
          </div>

          <div
            v-if="cart.isEmpty"
            class="cart__empty"
          >
            <svg
              viewBox="0 0 64 64"
              aria-hidden="true"
              class="cart__empty-art"
            >
              <circle
                cx="32"
                cy="32"
                r="30"
              />
              <path d="M17 34c4.5 10.5 25.5 10.5 30 0" />
            </svg>
            <p class="cart__empty-title">购物车还是空的</p>
            <p class="cart__empty-desc">挑一份喜欢的甜品，让今天甜一点。</p>
            <button
              type="button"
              class="btn btn--primary"
              @click="browse"
            >去看看今日甜品</button>
          </div>

          <ul
            v-else
            class="cart__list"
          >
            <li
              v-for="item in cart.items"
              :key="item.id"
              class="line"
            >
              <router-link
                :to="`/m/product/${item.id}`"
                class="line__thumb"
                tabindex="-1"
                @click="close"
              >
                <ProductImg
                  :item="item"
                  :width="72"
                  ratio="1/1"
                  :alt="item.name"
                />
              </router-link>
              <div class="line__info">
                <router-link
                  :to="`/m/product/${item.id}`"
                  class="line__name"
                  @click="close"
                >{{ item.name }}</router-link>
                <p class="line__unit sw-num">{{ formatPrice(item.price) }} / 件</p>
                <p
                  v-if="atLimit(item)"
                  class="line__limit"
                >已达库存上限（仅剩 {{ item.stock }} 件）</p>
                <div class="line__row">
                  <QtyStepper
                    :model-value="item.qty"
                    :max="item.stock ?? Infinity"
                    size="sm"
                    allow-remove
                    :label="`${item.name} 数量`"
                    @increase="increase(item)"
                    @decrease="cart.decreaseItem(item.id)"
                  />
                  <span class="line__total sw-num">{{ formatPrice(item.price * item.qty) }}</span>
                </div>
              </div>
            </li>
          </ul>

          <footer
            v-if="!cart.isEmpty"
            class="cart__foot"
          >
            <div class="cart__sum">
              <span>小计</span>
              <strong class="sw-num">{{ formatPrice(cart.totalAmount) }}</strong>
            </div>
            <p class="cart__hint">配送费全免 · 下单时按门店最新价格与库存核对</p>
            <button
              type="button"
              class="btn btn--primary btn--block"
              @click="checkout"
            >
              去结算
              <SwIcon
                name="arrow-right"
                :size="18"
              />
            </button>
            <div class="cart__actions">
              <button
                type="button"
                class="link-btn"
                @click="browse"
              >继续挑选</button>
              <button
                type="button"
                class="link-btn"
                :class="{ 'is-danger': confirmingClear }"
                @click="clearCart"
              >
                {{ confirmingClear ? '再点一次确认清空' : '清空购物车' }}
              </button>
            </div>
          </footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.cart-layer {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  justify-content: flex-end;
  background: rgba(36, 26, 22, 0.38);
}

.cart {
  position: relative;
  display: flex;
  flex-direction: column;
  width: min(440px, 100%);
  height: 100%;
  background: var(--sw-bg);
  box-shadow: var(--sw-shadow-lg);
}

.cart__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 24px 24px 16px;
}

.cart__title {
  font-size: 24px;
  color: var(--sw-ink);
}

.cart__count {
  margin-top: 2px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.cart__close {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  margin: -6px -8px 0 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--sw-ink-2);
  cursor: pointer;
}

.cart__close:hover {
  background: var(--sw-fill-soft);
}

.cart__notice {
  display: flex;
  gap: 8px;
  margin: 0 24px 12px;
  padding: 10px 12px;
  border-radius: var(--sw-r-md);
  background: var(--sw-warning-bg);
  color: var(--sw-warning);
  font-size: 13px;
  line-height: 1.5;
}

.cart__notice .sw-icon {
  margin-top: 2px;
}

.cart__list {
  flex: 1;
  overflow-y: auto;
  list-style: none;
  padding: 0 24px;
}

.line {
  display: flex;
  gap: 14px;
  padding: 16px 0;
  border-top: 1px solid var(--sw-border-soft);
}

.line__thumb {
  width: 76px;
  height: 76px;
  border-radius: var(--sw-r-md);
  overflow: hidden;
  flex-shrink: 0;
}

.line__info {
  flex: 1;
  min-width: 0;
}

.line__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--sw-ink);
}

.line__unit {
  margin-top: 2px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.line__limit {
  margin-top: 4px;
  font-size: 12px;
  color: var(--sw-berry);
}

.line__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.line__total {
  font-size: 15px;
  font-weight: 600;
}

.cart__foot {
  padding: 16px 24px calc(20px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--sw-border);
  background: var(--sw-surface);
}

.cart__sum {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  font-size: 14px;
  color: var(--sw-ink-2);
}

.cart__sum strong {
  font-size: 22px;
  color: var(--sw-ink);
}

.cart__hint {
  margin: 4px 0 14px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.cart__actions {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}

.cart__empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 32px 64px;
  text-align: center;
}

.cart__empty-art {
  width: 72px;
  margin-bottom: 16px;
  fill: var(--sw-fill-soft);
  stroke: var(--sw-ink-4);
  stroke-width: 2.4;
  stroke-linecap: round;
}

.cart__empty-art path {
  fill: none;
}

.cart__empty-title {
  font-size: 16px;
  font-weight: 600;
}

.cart__empty-desc {
  margin: 6px 0 20px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

/* 手机：底部面板 */
@media (max-width: 767px) {
  .cart-layer {
    align-items: flex-end;
  }

  .cart {
    width: 100%;
    height: auto;
    max-height: 86dvh;
    border-radius: var(--sw-r-xl) var(--sw-r-xl) 0 0;
  }

  .cart__head {
    padding: 20px 20px 12px;
  }

  .cart__head::before {
    content: '';
    position: absolute;
    left: 50%;
    top: 8px;
    width: 36px;
    height: 4px;
    margin-left: -18px;
    border-radius: 2px;
    background: var(--sw-border);
  }

  .cart__list {
    padding: 0 20px;
  }

  .cart__notice {
    margin: 0 20px 12px;
  }

  .cart__foot {
    padding: 14px 20px calc(16px + env(safe-area-inset-bottom));
  }

  .cart__empty {
    padding: 16px 24px 40px;
  }
}

.cart-enter-active,
.cart-leave-active {
  transition: background-color var(--sw-dur-3) var(--sw-ease);
}

.cart-enter-active .cart,
.cart-leave-active .cart {
  transition: transform var(--sw-dur-3) var(--sw-ease);
}

.cart-enter-from,
.cart-leave-to {
  background-color: rgba(36, 26, 22, 0);
}

.cart-enter-from .cart,
.cart-leave-to .cart {
  transform: translateX(100%);
}

@media (max-width: 767px) {
  .cart-enter-from .cart,
  .cart-leave-to .cart {
    transform: translateY(100%);
  }
}
</style>
