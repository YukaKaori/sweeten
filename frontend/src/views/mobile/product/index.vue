<template>
  <div class="product-page">
    <div class="page-nav">
      <button class="back-btn" @click="goBack">
        <span class="back-arrow">←</span> 返回
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="pd-skeleton">
      <div class="sk-hero"></div>
      <div class="sk-line w60"></div>
      <div class="sk-line w40"></div>
    </div>

    <!-- Not found / off sale -->
    <div v-else-if="!product" class="pd-empty">
      <div class="pd-empty__icon">🍽️</div>
      <p class="pd-empty__title">{{ errorText }}</p>
      <button class="pd-empty__btn" @click="router.replace('/m/')">去看看其他甜品</button>
    </div>

    <template v-else>
      <div class="pd-hero">
        <ProductImg :item="product" :width="420" :alt="product.name" eager />
        <span v-if="soldOut" class="pd-hero__badge">已售罄</span>
      </div>

      <section class="pd-card">
        <div class="pd-price-row">
          <span class="pd-price"><span class="yen">¥</span>{{ product.price.toFixed(2) }}</span>
          <span v-if="product.originalPrice > product.price" class="pd-original">¥{{ product.originalPrice.toFixed(2) }}</span>
          <span v-if="discount" class="pd-discount">省 ¥{{ discount }}</span>
        </div>
        <h1 class="pd-name">{{ product.name }}</h1>
        <p v-if="product.description" class="pd-desc">{{ product.description }}</p>
        <div class="pd-meta">
          <span>已售 {{ product.sales ?? 0 }}</span>
          <span class="dot">·</span>
          <span :class="{ low: lowStock }">{{ stockText }}</span>
        </div>
      </section>

      <section class="pd-card pd-note">
        <p>🧁 每日现做，下单后由门店确认制作</p>
        <p>🚚 同城配送，配送费全免</p>
      </section>
    </template>

    <!-- Bottom action bar -->
    <div v-if="product" class="pd-bar">
      <button class="pd-cart" aria-label="查看购物车" @click="router.push({ path: '/m/', query: { cart: 1 } })">
        <span>🛒</span>
        <span v-if="cartStore.totalQty" class="pd-cart__badge">{{ cartStore.totalQty }}</span>
      </button>
      <div class="pd-qty" :class="{ disabled: soldOut }">
        <button :disabled="qty <= 1 || soldOut" aria-label="减少" @click="qty--">−</button>
        <span>{{ qty }}</span>
        <button :disabled="qty >= maxQty || soldOut" aria-label="增加" @click="qty++">+</button>
      </div>
      <button class="pd-add" :disabled="soldOut || maxQty === 0" @click="addToCart(false)">加入购物车</button>
      <button class="pd-buy" :disabled="soldOut || maxQty === 0" @click="addToCart(true)">立即购买</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMobileProduct } from '@/api/modules/mobile'
import { useCartStore } from '@/stores/modules/cart'
import ProductImg from '@/components/ProductImg.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const product = ref(null)
const loading = ref(true)
const errorText = ref('')
const qty = ref(1)

const soldOut = computed(() => product.value?.stock === 0)
const lowStock = computed(() => {
  const s = product.value?.stock
  return s != null && s > 0 && s < 10
})
const stockText = computed(() => {
  const s = product.value?.stock
  if (s == null) return '现货供应'
  if (s <= 0) return '今日已售罄'
  return s < 10 ? `仅剩 ${s} 件` : `库存 ${s} 件`
})
const discount = computed(() => {
  const p = product.value
  if (!p || !(p.originalPrice > p.price)) return ''
  return (p.originalPrice - p.price).toFixed(2)
})
// 还能加入的数量 = 库存 − 购物车已有
const maxQty = computed(() => {
  if (!product.value) return 0
  const left = cartStore.remaining(product.value)
  return Number.isFinite(left) ? left : 99
})

watch(maxQty, (m) => {
  if (qty.value > m) qty.value = Math.max(m, 1)
})

async function load() {
  loading.value = true
  product.value = null
  try {
    const r = await getMobileProduct(route.params.id)
    if (r?.code === 1 && r.data) {
      product.value = r.data
    } else {
      errorText.value = r?.msg || '商品不存在或已下架'
    }
  } catch (e) {
    errorText.value = e?.response?.status === 404 ? '这款甜品已下架或不存在' : '加载失败，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

function addToCart(buyNow) {
  if (!product.value) return
  if (!cartStore.addItem(product.value, qty.value)) {
    window.__showToast?.(maxQty.value > 0 ? `最多还能加 ${maxQty.value} 件` : '库存不足，购物车里已是全部库存')
    return
  }
  if (buyNow) {
    router.push('/m/checkout')
  } else {
    window.__showToast?.(`已加入购物车 ×${qty.value}`)
    qty.value = 1
  }
}

function goBack() {
  if (window.history.state?.back) router.back()
  else router.replace('/m/')
}

onMounted(load)
watch(() => route.params.id, (id) => { if (id) load() })
</script>

<style scoped>
.product-page { padding: 0 0 132px; }

.page-nav { display: flex; align-items: center; padding: 18px 18px 12px; }
.back-btn {
  display: flex; align-items: center; gap: 6px;
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px);
  border: 0.5px solid rgba(255,255,255,0.5);
  border-radius: 999px; padding: 7px 18px;
  font-size: 13px; font-weight: 600; color: #86868b; cursor: pointer;
}
.back-btn:active { transform: scale(0.95); }
.back-arrow { font-size: 15px; }

/* ── Hero ── */
.pd-hero {
  position: relative;
  margin: 0 18px 14px;
  aspect-ratio: 4 / 3;
  border-radius: 24px;
  overflow: hidden;
  background: linear-gradient(145deg, #fafafa, #f0f0f2);
  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
}
.pd-hero img { width: 100%; height: 100%; object-fit: cover; display: block; }
.pd-hero__badge {
  position: absolute; left: 14px; top: 14px;
  padding: 5px 12px; border-radius: 999px;
  background: rgba(29,29,31,0.72); color: #fff;
  font-size: 12px; font-weight: 600;
}

/* ── Info card ── */
.pd-card {
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  margin: 0 18px 14px; border-radius: 22px; padding: 20px;
  border: 0.5px solid rgba(255,255,255,0.5);
  box-shadow: 0 4px 18px rgba(0,0,0,0.03), inset 0 0.5px 0 rgba(255,255,255,0.6);
}
.pd-price-row { display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.pd-price { font-size: 28px; font-weight: 700; color: #d9574a; letter-spacing: -0.5px; }
.pd-price .yen { font-size: 15px; font-weight: 600; margin-right: 1px; }
.pd-original { font-size: 14px; color: #86868b; text-decoration: line-through; }
.pd-discount {
  font-size: 11px; font-weight: 600; color: #d9574a;
  background: rgba(217,87,74,0.08); padding: 2px 8px; border-radius: 999px;
}
.pd-name { margin: 10px 0 6px; font-size: 20px; font-weight: 680; color: #1d1d1f; letter-spacing: -0.3px; line-height: 1.3; }
.pd-desc { margin: 0 0 12px; font-size: 14px; color: #6e6e73; line-height: 1.6; }
.pd-meta { font-size: 12px; color: #86868b; display: flex; gap: 6px; align-items: center; }
.pd-meta .low { color: #d9574a; font-weight: 600; }
.pd-meta .dot { opacity: 0.6; }

.pd-note p { margin: 0; font-size: 13px; color: #6e6e73; line-height: 1.9; }

/* ── Bottom bar ── */
.pd-bar {
  /* 与首页购物车胶囊同一位置：底部 Tab 栏之上 */
  position: fixed; bottom: 90px; left: 50%; transform: translateX(-50%);
  width: calc(100% - 24px); max-width: 406px;
  z-index: 95;
  display: flex; align-items: center; gap: 8px;
  padding: 8px;
  background: rgba(255,255,255,0.86);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 0.5px solid rgba(0,0,0,0.06);
  border-radius: 22px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.10);
}
.pd-cart {
  position: relative; width: 42px; height: 42px; flex-shrink: 0;
  border: none; border-radius: 14px; background: rgba(0,0,0,0.04);
  font-size: 18px; cursor: pointer;
}
.pd-cart__badge {
  position: absolute; top: -4px; right: -4px;
  min-width: 18px; height: 18px; padding: 0 5px;
  border-radius: 9px; background: #d9574a; color: #fff;
  font-size: 11px; font-weight: 700; line-height: 18px;
}
.pd-qty { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.pd-qty button {
  width: 28px; height: 28px; border-radius: 50%; border: none;
  background: rgba(0,0,0,0.05); color: #1d1d1f; font-size: 16px; font-weight: 700; cursor: pointer;
}
.pd-qty button:disabled { opacity: 0.35; cursor: not-allowed; }
.pd-qty span { min-width: 18px; text-align: center; font-size: 15px; font-weight: 650; color: #1d1d1f; }
.pd-add, .pd-buy {
  flex: 1; height: 42px; border: none; border-radius: 14px;
  font-size: 14px; font-weight: 650; cursor: pointer; white-space: nowrap;
}
.pd-add { background: rgba(217,87,74,0.10); color: #d9574a; }
.pd-buy { background: #d9574a; color: #fff; box-shadow: 0 6px 16px rgba(217,87,74,0.28); }
.pd-add:disabled, .pd-buy:disabled { opacity: 0.45; box-shadow: none; cursor: not-allowed; }
.pd-add:active:not(:disabled), .pd-buy:active:not(:disabled) { transform: scale(0.97); }

/* ── Skeleton / empty ── */
.pd-skeleton { padding: 0 18px; }
.sk-hero, .sk-line {
  background: linear-gradient(105deg,#ececee 25%,#f5f5f6 50%,#ececee 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: 14px;
}
.sk-hero { aspect-ratio: 4 / 3; border-radius: 24px; margin-bottom: 18px; }
.sk-line { height: 18px; margin-bottom: 12px; }
.sk-line.w60 { width: 60%; }
.sk-line.w40 { width: 40%; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

.pd-empty { text-align: center; padding: 80px 24px; }
.pd-empty__icon { font-size: 48px; margin-bottom: 12px; }
.pd-empty__title { font-size: 15px; color: #6e6e73; margin: 0 0 20px; }
.pd-empty__btn {
  border: none; border-radius: 999px; padding: 10px 22px;
  background: #1d1d1f; color: #fff; font-size: 14px; font-weight: 600; cursor: pointer;
}

@media (prefers-reduced-motion: reduce) {
  .sk-hero, .sk-line { animation: none; }
}

@media (max-width: 360px) {
  .pd-qty { display: none; }
}
</style>
