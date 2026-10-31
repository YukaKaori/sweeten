<script setup>
/**
 * 商品详情：大图 + 名称价格 + 库存状态 + 数量 + 加入购物车 / 立即购买 + 同类推荐
 * 详情以 GET /mobile/products/:id 为准（已下架返回 404）；推荐取自在售目录。
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMobileProduct } from '@/api/modules/mobile'
import { useCartStore } from '@/stores/modules/cart'
import { useStoreUi } from '@/stores/modules/storeUi'
import { useCatalog } from '@/composables/useCatalog'
import { STORE_COPY, pageTitle } from '@/config/brand'
import { formatPrice, formatCount, savingOf } from '@/utils/format'
import { DESSERT_CATEGORIES } from '@/utils/constants'
import ProductImg from '@/components/ProductImg.vue'
import ProductCard from '@/components/store/ProductCard.vue'
import QtyStepper from '@/components/QtyStepper.vue'
import SwIcon from '@/components/brand/SwIcon.vue'

const LOW_STOCK = 10
const PROMISE_ICONS = { fresh: 'leaf', delivery: 'truck', points: 'star' }

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const ui = useStoreUi()
const catalog = useCatalog()

const product = ref(null)
const loading = ref(true)
const errorText = ref('')
const notFound = ref(false)
const qty = ref(1)

const soldOut = computed(() => product.value?.stock === 0)
const stockState = computed(() => {
  const s = product.value?.stock
  if (s == null) return { tone: 'ok', text: '现货供应' }
  if (s <= 0) return { tone: 'out', text: '今日已售罄' }
  if (s < LOW_STOCK) return { tone: 'low', text: `仅剩 ${s} 件` }
  return { tone: 'ok', text: `现货 ${s} 件` }
})
const saving = computed(() => savingOf(product.value))
const inCart = computed(() => (product.value ? cart.getQty(product.value.id) : 0))
// 还能加入的数量 = 库存 − 购物车已有
const maxQty = computed(() => {
  if (!product.value) return 0
  const left = cart.remaining(product.value)
  return Number.isFinite(left) ? left : 99
})
const categoryLabel = computed(() => catalog.categoryLabel(product.value?.category))
const categoryEn = computed(() => DESSERT_CATEGORIES.find((c) => c.value === product.value?.category)?.en || 'SWEETEN')
const related = computed(() =>
  catalog.products.value
    .filter((p) => p.category === product.value?.category && p.id !== product.value?.id && p.stock !== 0)
    .slice(0, 4)
)

watch(maxQty, (m) => {
  if (qty.value > m) qty.value = Math.max(m, 1)
})

async function load() {
  loading.value = true
  product.value = null
  errorText.value = ''
  notFound.value = false
  qty.value = 1
  try {
    const r = await getMobileProduct(route.params.id)
    if (r?.code === 1 && r.data) {
      product.value = r.data
      document.title = pageTitle(r.data.name)
    } else {
      notFound.value = true
      errorText.value = r?.msg || '商品不存在或已下架'
    }
  } catch (e) {
    notFound.value = e?.response?.status === 404
    errorText.value = notFound.value ? '这款甜品已下架，或者链接有误' : '加载失败，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

function addToCart(buyNow) {
  if (!product.value || soldOut.value) return
  if (maxQty.value <= 0) {
    ui.showToast('购物车里已是全部库存', 'warning')
    return
  }
  if (!cart.addItem(product.value, qty.value)) {
    ui.showToast(`最多还能加 ${maxQty.value} 件`, 'warning')
    return
  }
  if (buyNow) {
    router.push('/m/checkout')
  } else {
    ui.showToast(`已加入购物车 × ${qty.value}`, 'success')
    qty.value = 1
  }
}

function goBack() {
  if (window.history.state?.back) router.back()
  else router.replace('/m')
}

onMounted(() => {
  load()
  catalog.load()
})
watch(() => route.params.id, (id) => { if (id && route.name === 'mobileProduct') load() })
</script>

<template>
  <div class="pd store-page">
    <nav
      class="pd__crumbs"
      aria-label="当前位置"
    >
      <button
        type="button"
        class="pd__back"
        aria-label="返回"
        @click="goBack"
      >
        <SwIcon
          name="chevron-left"
          :size="20"
        />
      </button>
      <router-link to="/m">首页</router-link>
      <span aria-hidden="true">/</span>
      <router-link
        v-if="categoryLabel"
        :to="{ path: '/m', query: { category: product.category }, hash: '#menu' }"
      >{{ categoryLabel }}</router-link>
      <router-link
        v-else
        :to="{ path: '/m', hash: '#menu' }"
      >今日菜单</router-link>
      <template v-if="product">
        <span aria-hidden="true">/</span>
        <span
          class="pd__crumb-current"
          aria-current="page"
        >{{ product.name }}</span>
      </template>
    </nav>

    <!-- 加载中 -->
    <div
      v-if="loading"
      class="pd__grid"
      aria-busy="true"
    >
      <div class="pd__media skeleton-shimmer sk-media"></div>
      <div class="pd__info">
        <div class="skeleton-shimmer sk-line w30"></div>
        <div class="skeleton-shimmer sk-line w70 tall"></div>
        <div class="skeleton-shimmer sk-line w50"></div>
        <div class="skeleton-shimmer sk-line w40 tall"></div>
      </div>
    </div>

    <!-- 不存在 / 失败 -->
    <div
      v-else-if="!product"
      class="pd__state"
    >
      <p class="pd__state-title">{{ errorText }}</p>
      <p class="pd__state-desc">{{ notFound ? '看看今天还有哪些在售的甜品吧。' : '网络恢复后可以重新加载。' }}</p>
      <div class="pd__state-actions">
        <button
          v-if="!notFound"
          type="button"
          class="btn btn--secondary btn--sm"
          @click="load"
        >重新加载</button>
        <router-link
          :to="{ path: '/m', hash: '#menu' }"
          class="btn btn--primary btn--sm"
        >浏览今日甜品</router-link>
      </div>
    </div>

    <template v-else>
      <article class="pd__grid">
        <div class="pd__media">
          <ProductImg
            :item="product"
            sizes="(min-width: 1024px) 600px, 100vw"
            ratio="1/1"
            :alt="product.name"
            eager
          />
          <span
            v-if="soldOut"
            class="pd__soldout"
          >今日已售罄</span>
        </div>

        <div class="pd__info">
          <p class="sw-eyebrow pd__eyebrow">{{ categoryEn }}</p>
          <h1 class="pd__name">{{ product.name }}</h1>
          <p
            v-if="product.description"
            class="pd__desc"
          >{{ product.description }}</p>

          <div class="pd__price">
            <span class="pd__now sw-num">{{ formatPrice(product.price) }}</span>
            <s
              v-if="saving"
              class="pd__was sw-num"
            >{{ formatPrice(product.originalPrice) }}</s>
            <span
              v-if="saving"
              class="pd__save"
            >立省 ¥{{ saving }}</span>
          </div>

          <p class="pd__meta">
            <span
              class="pd__stock"
              :class="`is-${stockState.tone}`"
            >
              <span
                class="pd__stock-dot"
                aria-hidden="true"
              ></span>{{ stockState.text }}
            </span>
            <span class="pd__sales">已售 {{ formatCount(product.sales) }} 份</span>
          </p>

          <div class="pd__buy">
            <div class="pd__qty-row">
              <span class="pd__qty-label">数量</span>
              <QtyStepper
                v-model="qty"
                :min="1"
                :max="Math.max(maxQty, 1)"
                :disabled="soldOut || maxQty <= 0"
              />
              <span
                v-if="inCart"
                class="pd__incart"
              >购物车里已有 {{ inCart }} 件</span>
            </div>
            <div class="pd__actions">
              <button
                type="button"
                class="btn btn--secondary"
                :disabled="soldOut || maxQty <= 0"
                @click="addToCart(false)"
              >
                <SwIcon
                  name="bag"
                  :size="18"
                /> 加入购物车
              </button>
              <button
                type="button"
                class="btn btn--primary"
                :disabled="soldOut || maxQty <= 0"
                @click="addToCart(true)"
              >
                立即购买
              </button>
            </div>
            <p
              v-if="soldOut"
              class="pd__buy-note"
            >今日已售罄，明天再来看看吧。</p>
            <p
              v-else-if="maxQty <= 0"
              class="pd__buy-note"
            >购物车里已是这款甜品的全部库存。</p>
          </div>

          <ul class="pd__promises">
            <li
              v-for="p in STORE_COPY.promises"
              :key="p.key"
            >
              <SwIcon
                :name="PROMISE_ICONS[p.key]"
                :size="18"
              />
              <span><strong>{{ p.title }}</strong>{{ p.desc }}</span>
            </li>
          </ul>
        </div>
      </article>

      <section
        v-if="related.length"
        class="pd__related"
        aria-labelledby="related-title"
      >
        <h2
          id="related-title"
          class="pd__related-title"
        >同类推荐</h2>
        <div class="pd__related-grid">
          <ProductCard
            v-for="p in related"
            :key="p.id"
            :product="p"
          />
        </div>
      </section>

      <!-- 手机：底部购买栏 -->
      <div class="buybar">
        <button
          type="button"
          class="buybar__cart"
          :aria-label="`购物车，${cart.totalQty} 件`"
          @click="ui.openCart()"
        >
          <SwIcon
            name="bag"
            :size="22"
          />
          <span
            v-if="cart.totalQty"
            class="buybar__badge sw-num"
          >{{ cart.totalQty }}</span>
        </button>
        <QtyStepper
          v-model="qty"
          size="sm"
          :min="1"
          :max="Math.max(maxQty, 1)"
          :disabled="soldOut || maxQty <= 0"
          class="buybar__qty"
        />
        <button
          type="button"
          class="btn btn--secondary buybar__btn"
          :disabled="soldOut || maxQty <= 0"
          @click="addToCart(false)"
        >加入购物车</button>
        <button
          type="button"
          class="btn btn--primary buybar__btn"
          :disabled="soldOut || maxQty <= 0"
          @click="addToCart(true)"
        >
          {{ soldOut ? '已售罄' : '立即购买' }}
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.store-page {
  animation: fadeIn var(--sw-dur-3) var(--sw-ease) both;
}

.pd {
  max-width: var(--store-max, 1200px);
  margin: 0 auto;
  padding: 8px var(--store-pad, 20px) 0;
}

.pd__crumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 8px 0 20px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.pd__crumbs a:hover {
  color: var(--sw-ink);
}

.pd__crumb-current {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--sw-ink);
}

.pd__back {
  display: none;
}

.pd__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  gap: clamp(28px, 5vw, 64px);
  align-items: start;
}

.pd__media {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  background: var(--sw-fill);
}

.sk-media {
  aspect-ratio: 1 / 1;
  border-radius: 24px;
}

.sk-line {
  height: 16px;
  margin-bottom: 16px;
}

.sk-line.tall { height: 32px; }
.w30 { width: 30%; }
.w40 { width: 40%; }
.w50 { width: 50%; }
.w70 { width: 70%; }

.pd__soldout {
  position: absolute;
  top: 16px;
  left: 16px;
  padding: 6px 12px;
  border-radius: var(--sw-r-pill);
  background: rgba(36, 26, 22, 0.76);
  color: #fffefc;
  font-size: 13px;
  font-weight: 600;
}

.pd__info {
  position: sticky;
  top: 96px;
  padding-top: 8px;
}

.pd__eyebrow {
  color: var(--sw-berry);
}

.pd__name {
  margin-top: 10px;
  font-size: clamp(26px, 3vw, 34px);
  font-weight: 600;
  line-height: 1.25;
}

.pd__desc {
  margin-top: 10px;
  font-size: 16px;
  line-height: 1.7;
  color: var(--sw-ink-2);
}

.pd__price {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 24px;
}

.pd__now {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.pd__was {
  font-size: 16px;
  color: var(--sw-ink-4);
}

.pd__save {
  padding: 3px 10px;
  border-radius: var(--sw-r-pill);
  background: var(--sw-blush-soft);
  color: var(--sw-berry);
  font-size: 13px;
  font-weight: 600;
}

.pd__meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.pd__stock {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: var(--sw-success);
}

.pd__stock-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.pd__stock.is-low { color: var(--sw-berry); }
.pd__stock.is-out { color: var(--sw-ink-3); }

.pd__buy {
  margin-top: 28px;
  padding: 20px;
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-lg);
  background: var(--sw-surface);
}

.pd__qty-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.pd__qty-label {
  font-size: 14px;
  font-weight: 600;
}

.pd__incart {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.pd__actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 18px;
}

.pd__buy-note {
  margin-top: 10px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.pd__promises {
  list-style: none;
  margin-top: 24px;
}

.pd__promises li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--sw-border-soft);
  font-size: 13px;
  color: var(--sw-ink-3);
}

.pd__promises li:last-child {
  border-bottom: 1px solid var(--sw-border-soft);
}

.pd__promises .sw-icon {
  color: var(--sw-ink-2);
}

.pd__promises strong {
  margin-right: 10px;
  font-weight: 600;
  color: var(--sw-ink);
}

.pd__related {
  margin-top: 72px;
}

.pd__related-title {
  margin-bottom: 20px;
  font-size: 20px;
  font-weight: 600;
}

.pd__related-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.pd__state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 96px 16px;
  text-align: center;
}

.pd__state-title {
  font-size: 18px;
  font-weight: 600;
}

.pd__state-desc {
  font-size: 14px;
  color: var(--sw-ink-3);
}

.pd__state-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.buybar {
  display: none;
}

@media (max-width: 1023px) {
  .pd__related-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }

  .pd__related-grid > :nth-child(4) {
    display: none;
  }
}

@media (max-width: 767px) {
  .pd {
    padding: 0 0 calc(96px + env(safe-area-inset-bottom));
  }

  .pd__crumbs {
    padding: 8px var(--store-pad, 16px) 12px;
  }

  .pd__back {
    display: grid;
    place-items: center;
    width: 36px;
    height: 36px;
    margin-left: -8px;
    border: 0;
    border-radius: 50%;
    background: transparent;
    color: var(--sw-ink);
    cursor: pointer;
  }

  .pd__grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .pd__media {
    border-radius: 0;
  }

  .sk-media {
    border-radius: 0;
  }

  .pd__info {
    position: static;
    padding: 0 var(--store-pad, 16px);
  }

  .pd__name {
    font-size: 24px;
  }

  .pd__desc {
    font-size: 15px;
  }

  .pd__now {
    font-size: 28px;
  }

  .pd__buy {
    display: none;
  }

  .pd__related {
    margin-top: 40px;
    padding: 0 var(--store-pad, 16px);
  }

  .pd__related-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .pd__related-grid > :nth-child(4) {
    display: flex;
  }

  .buybar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 110;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
    background: var(--sw-surface);
    border-top: 1px solid var(--sw-border);
    box-shadow: 0 -8px 24px rgba(73, 59, 53, 0.06);
  }

  .buybar__cart {
    position: relative;
    display: grid;
    place-items: center;
    width: 44px;
    height: 44px;
    border: 0;
    border-radius: 50%;
    background: transparent;
    color: var(--sw-ink);
    flex-shrink: 0;
    cursor: pointer;
  }

  .buybar__badge {
    position: absolute;
    top: 2px;
    right: 0;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: 9px;
    background: var(--sw-berry);
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    line-height: 18px;
  }

  .buybar__qty {
    flex-shrink: 0;
  }

  .buybar__btn {
    flex: 1;
    height: 44px;
    padding: 0 10px;
    font-size: 14px;
  }
}

@media (max-width: 380px) {
  .buybar__qty {
    display: none;
  }
}
</style>
