<template>
  <div class="home-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-input-wrap">
        <span class="search-icon">🔍</span>
        <input
          v-model="searchQuery"
          class="search-input"
          type="text"
          placeholder="搜索你喜欢的甜品..."
          @input="filterProducts"
        />
        <span v-if="searchQuery" class="search-clear" @click="clearSearch">✕</span>
      </div>
    </div>

    <!-- 分类 Tab -->
    <div class="category-tabs-wrap">
      <div class="category-tabs" ref="tabsRef">
        <button
          v-for="cat in categories"
          :key="cat.key"
          class="cat-tab"
          :class="{ active: activeCategory === cat.key }"
          @click="switchCategory(cat.key)"
        >
          <span class="cat-tab__emoji">{{ categoryEmoji[cat.key] }}</span>
          <span class="cat-tab__label">{{ cat.label }}</span>
          <span v-if="cat.count && cat.key !== 'all'" class="cat-tab__count">{{ cat.count }}</span>
        </button>
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="products-section">
      <!-- 骨架屏 -->
      <template v-if="loading">
        <div class="products-grid">
          <div v-for="i in 6" :key="i" class="product-skeleton">
            <div class="ske ske-img"></div>
            <div class="ske ske-title"></div>
            <div class="ske ske-price"></div>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-else-if="displayProducts.length === 0" class="empty-state">
        <div class="empty-emoji">🍽️</div>
        <p class="empty-title">暂无商品</p>
        <p class="empty-desc">试试其他分类吧～</p>
      </div>

      <!-- 商品网格 -->
      <div v-else class="products-grid">
        <div
          v-for="product in displayProducts"
          :key="product.id"
          class="product-card"
          :class="{ 'sold-out': product.stock === 0 }"
        >
          <!-- 商品图 / Emoji -->
          <div class="product-card__img-wrap">
            <img
              v-if="product.image"
              :src="product.image"
              :alt="product.name"
              class="product-card__img"
              loading="lazy"
              @error="(e) => (e.target.style.display = 'none')"
            />
            <div v-else class="product-card__emoji">{{ product.icon || '🍰' }}</div>
            <!-- 售罄蒙层 -->
            <div v-if="product.stock === 0" class="sold-out-mask">
              <span>已售罄</span>
            </div>
            <!-- 销量角标 -->
            <div v-if="product.sales > 0" class="sales-badge">🔥 {{ product.sales }}</div>
          </div>

          <!-- 商品信息 -->
          <div class="product-card__info">
            <p class="product-card__name">{{ product.name }}</p>
            <p v-if="product.description" class="product-card__desc">{{ product.description }}</p>
            <div class="product-card__price-row">
              <div class="price-group">
                <span class="price-current">¥{{ product.price.toFixed(2) }}</span>
                <span
                  v-if="product.originalPrice && product.originalPrice > product.price"
                  class="price-original"
                >¥{{ product.originalPrice.toFixed(2) }}</span>
              </div>

              <!-- 步进器 / 加购按钮 -->
              <div class="stepper" v-if="product.stock !== 0">
                <template v-if="cartStore.getQty(product.id) > 0">
                  <button class="stepper__btn minus" @click.stop="cartStore.decreaseItem(product.id)">−</button>
                  <span class="stepper__qty">{{ cartStore.getQty(product.id) }}</span>
                  <button class="stepper__btn plus" @click.stop="cartStore.addItem(product)">+</button>
                </template>
                <template v-else>
                  <button class="add-btn" @click.stop="handleAdd(product)">
                    <span>+</span>
                  </button>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部购物车栏 -->
    <transition name="cart-bar">
      <div v-if="!cartStore.isEmpty" class="cart-bar">
        <div class="cart-bar__icon-wrap" @click="toggleCartSheet">
          <span class="cart-bar__icon">🛒</span>
          <span class="cart-bar__badge">{{ cartStore.totalQty }}</span>
        </div>
        <div class="cart-bar__total" @click="toggleCartSheet">
          <span class="cart-bar__amount">¥{{ cartStore.totalAmount.toFixed(2) }}</span>
          <span class="cart-bar__hint">已选 {{ cartStore.totalQty }} 件</span>
        </div>
        <button class="cart-bar__checkout" @click="goCheckout">去结算</button>
      </div>
    </transition>

    <!-- 购物车明细 Sheet -->
    <transition name="sheet">
      <div v-if="showCartSheet" class="cart-sheet-mask" @click.self="toggleCartSheet">
        <div class="cart-sheet">
          <div class="cart-sheet__header">
            <span class="cart-sheet__title">已选商品</span>
            <button class="cart-sheet__clear" @click="cartStore.clear()">清空</button>
          </div>
          <div class="cart-sheet__list">
            <div v-for="item in cartStore.items" :key="item.id" class="cart-item">
              <div class="cart-item__icon">{{ item.icon }}</div>
              <div class="cart-item__info">
                <p class="cart-item__name">{{ item.name }}</p>
                <p class="cart-item__price">¥{{ item.price.toFixed(2) }}</p>
              </div>
              <div class="cart-item__stepper">
                <button @click="cartStore.decreaseItem(item.id)">−</button>
                <span>{{ item.qty }}</span>
                <button @click="cartStore.addItem(item)">+</button>
              </div>
            </div>
          </div>
          <div class="cart-sheet__footer">
            <div class="cart-sheet__total">合计：<strong>¥{{ cartStore.totalAmount.toFixed(2) }}</strong></div>
            <button class="cart-sheet__go" @click="goCheckout">去结算</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMobileProducts, getMobileCategories } from '@/api/modules/mobile'
import { useCartStore } from '@/stores/modules/cart'

const router = useRouter()
const cartStore = useCartStore()

// ── 数据 ─────────────────────────────────────────────────────
const loading = ref(true)
const allProducts = ref([])
const categories = ref([{ key: 'all', label: '全部' }])
const activeCategory = ref('all')
const searchQuery = ref('')
const showCartSheet = ref(false)

const categoryEmoji = {
  all: '🍽️', cake: '🎂', bread: '🥐', drink: '🧋',
  icecream: '🍦', snack: '🍡', gift: '🎁', dessert: '🍮',
}

// ── 过滤后的商品列表 ─────────────────────────────────────────
const displayProducts = computed(() => {
  let list = allProducts.value
  if (searchQuery.value.trim()) {
    const kw = searchQuery.value.trim().toLowerCase()
    list = list.filter(
      (p) => p.name.toLowerCase().includes(kw) || (p.description || '').toLowerCase().includes(kw)
    )
  }
  return list
})

// ── API 调用 ─────────────────────────────────────────────────
async function loadCategories() {
  try {
    const res = await getMobileCategories()
    if (res?.code === 1 && res.data) {
      categories.value = res.data
    }
  } catch {
    // 保持默认分类
  }
}

async function loadProducts(category = 'all') {
  loading.value = true
  try {
    const res = await getMobileProducts(category)
    if (res?.code === 1) {
      allProducts.value = res.data || []
    }
  } catch {
    allProducts.value = []
  } finally {
    loading.value = false
  }
}

// ── 分类切换 ──────────────────────────────────────────────────
function switchCategory(key) {
  if (activeCategory.value === key) return
  activeCategory.value = key
  searchQuery.value = ''
  loadProducts(key)
}

function filterProducts() {
  // 搜索时切换到"全部"分类以获取完整数据
  if (searchQuery.value && activeCategory.value !== 'all') {
    activeCategory.value = 'all'
    loadProducts('all')
  }
}

function clearSearch() {
  searchQuery.value = ''
}

// ── 购物车 ────────────────────────────────────────────────────
function handleAdd(product) {
  cartStore.addItem(product)
  // 添加时做一个简单的弹跳动画反馈
}

function toggleCartSheet() {
  showCartSheet.value = !showCartSheet.value
}

function goCheckout() {
  showCartSheet.value = false
  router.push('/m/checkout')
}

// ── 初始化 ────────────────────────────────────────────────────
onMounted(async () => {
  await Promise.all([loadCategories(), loadProducts('all')])
})
</script>

<style scoped>
.home-page {
  padding-bottom: 120px; /* 留出底部购物车栏高度 */
}

/* ── 搜索栏 ── */
.search-bar {
  padding: 18px 16px 10px;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 999px;
  padding: 0 18px;
  gap: 10px;
  box-shadow: 
    inset 0 1px 1px 0 rgba(255, 255, 255, 0.3),
    0 4px 20px rgba(45, 35, 39, 0.04);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.search-input-wrap:focus-within {
  background: #ffffff;
  border-color: rgba(232, 99, 122, 0.35);
  box-shadow: 
    0 10px 30px rgba(232, 99, 122, 0.08),
    0 1px 3px rgba(232, 99, 122, 0.02);
  transform: translateY(-1px);
}

.search-icon { 
  font-size: 14px; 
  opacity: 0.6;
  transition: opacity 0.2s;
}

.search-input-wrap:focus-within .search-icon {
  opacity: 0.9;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 12px 0;
  font-size: 14px;
  background: transparent;
  color: #2d2327;
  font-weight: 500;
}

.search-input::placeholder { color: #bcaeb5; }

.search-clear {
  cursor: pointer;
  color: #a3949b;
  font-size: 14px;
  line-height: 1;
  padding: 4px;
  opacity: 0.7;
  transition: opacity 0.2s;
}
.search-clear:hover {
  opacity: 1;
}

/* ── 分类 Tab ── */
.category-tabs-wrap {
  overflow-x: auto;
  scrollbar-width: none;
  padding: 4px 16px 14px;
}

.category-tabs-wrap::-webkit-scrollbar { display: none; }

.category-tabs {
  display: flex;
  gap: 8px;
  min-width: max-content;
}

.cat-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 999px;
  border: 1px solid rgba(232, 99, 122, 0.1);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 2px 8px rgba(45, 35, 39, 0.02);
  color: #6b5b63;
}

.cat-tab:active { transform: scale(0.96); }

.cat-tab.active {
  background: linear-gradient(135deg, rgba(232, 99, 122, 0.9), rgba(201, 77, 99, 0.95));
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-color: rgba(232, 99, 122, 0.2);
  color: #fff;
  box-shadow: 
    0 8px 20px rgba(232, 99, 122, 0.25),
    inset 0 1px 0 0 rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.cat-tab__emoji { 
  font-size: 15px; 
  transition: transform 0.3s ease;
}
.cat-tab.active .cat-tab__emoji {
  transform: scale(1.15);
}

.cat-tab__label { 
  font-size: 13px; 
  font-weight: 600; 
  color: inherit; 
}

.cat-tab__count {
  font-size: 10px;
  background: rgba(232, 99, 122, 0.08);
  color: #e8637a;
  padding: 1px 6px;
  border-radius: 999px;
  font-weight: 700;
}

.cat-tab.active .cat-tab__count {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

/* ── 商品网格 ── */
.products-section { padding: 0 16px; }

.products-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

/* 骨架屏 */
.product-skeleton {
  background: rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  overflow: hidden;
  padding-bottom: 14px;
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.ske {
  background: linear-gradient(90deg, #f5f0ec 25%, #faf7f5 50%, #f5f0ec 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  border-radius: 6px;
}

.ske-img { height: 160px; border-radius: 0; }
.ske-title { height: 14px; margin: 16px 14px 10px; }
.ske-price { height: 12px; width: 60%; margin: 0 14px; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  grid-column: 1 / -1;
}

.empty-emoji { font-size: 52px; margin-bottom: 16px; filter: drop-shadow(0 4px 8px rgba(0,0,0,0.05)); }
.empty-title { font-size: 16px; font-weight: 700; color: #2d2327; margin: 0 0 6px; }
.empty-desc { font-size: 13px; color: #a3949b; margin: 0; }

/* ── 商品卡片 ── */
.product-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(45, 35, 39, 0.04);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  border: 1px solid rgba(255, 255, 255, 0.6);
  display: flex;
  flex-direction: column;
}

.product-card:active { 
  transform: scale(0.96) translateY(1px); 
  box-shadow: 0 4px 12px rgba(45, 35, 39, 0.02);
}

.product-card.sold-out { opacity: 0.6; }

.product-card__img-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: linear-gradient(135deg, #fde8ec 0%, #faf7f5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.product-card:active .product-card__img-wrap {
  transform: scale(0.98);
}

.product-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.product-card:hover .product-card__img {
  transform: scale(1.05);
}

.product-card__emoji {
  font-size: 56px;
  filter: drop-shadow(0 6px 12px rgba(232, 99, 122, 0.18));
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.product-card:hover .product-card__emoji {
  transform: scale(1.1) rotate(5deg);
}

.sold-out-mask {
  position: absolute;
  inset: 0;
  background: rgba(45, 35, 39, 0.45);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.sales-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: linear-gradient(135deg, rgba(232, 99, 122, 0.9), rgba(201, 77, 99, 0.95));
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 999px;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  box-shadow: 0 4px 10px rgba(232, 99, 122, 0.2);
}

.product-card__info { 
  padding: 12px 14px 14px; 
  display: flex;
  flex-direction: column;
  flex: 1;
}

.product-card__name {
  font-size: 14px;
  font-weight: 700;
  color: #2d2327;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  letter-spacing: 0.01em;
}

.product-card__desc {
  font-size: 11px;
  color: #a3949b;
  margin: 0 0 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.3;
}

.product-card__price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}

.price-group {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.price-current {
  font-size: 16px;
  font-weight: 800;
  color: #e8637a;
  font-family: var(--font-body);
}

.price-original {
  font-size: 11px;
  color: #c4b5bc;
  text-decoration: line-through;
  font-weight: 500;
}

/* ── 步进器 & 加购按钮 ── */
.stepper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stepper__btn {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.stepper__btn.minus {
  background: rgba(245, 240, 236, 0.8);
  color: #6b5b63;
  border: 1px solid rgba(45, 35, 39, 0.05);
}
.stepper__btn.plus {
  background: linear-gradient(135deg, #e8637a, #c94d63);
  color: #fff;
  box-shadow: 0 3px 8px rgba(232, 99, 122, 0.25);
}
.stepper__btn:active { transform: scale(0.85); }

.stepper__qty {
  font-size: 13px;
  font-weight: 700;
  color: #2d2327;
  min-width: 16px;
  text-align: center;
}

.add-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #e8637a, #c94d63);
  color: #fff;
  font-size: 18px;
  font-weight: 400;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 
    0 4px 12px rgba(232, 99, 122, 0.3),
    inset 0 1px 0 0 rgba(255, 255, 255, 0.2);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.add-btn:active { transform: scale(0.85); }

/* ── 底部购物车栏 ── */
.cart-bar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: calc(100% - 32px);
  max-width: 398px;
  background: rgba(35, 27, 30, 0.82);
  backdrop-filter: blur(28px) saturate(210%) brightness(0.95);
  -webkit-backdrop-filter: blur(28px) saturate(210%) brightness(0.95);
  border-radius: 999px;
  padding: 8px 8px 8px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 
    0 16px 40px rgba(15, 10, 12, 0.25),
    inset 0 1px 0 0 rgba(255, 255, 255, 0.15),
    0 0 0 1px rgba(232, 99, 122, 0.1) inset;
  z-index: 90;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.cart-bar:active {
  transform: translateX(-50%) scale(0.98);
}

.cart-bar__icon-wrap {
  position: relative;
  flex-shrink: 0;
}

.cart-bar__icon { 
  font-size: 24px; 
  filter: drop-shadow(0 2px 8px rgba(232, 99, 122, 0.2));
}

.cart-bar__badge {
  position: absolute;
  top: -8px;
  right: -8px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background: linear-gradient(135deg, #e8637a, #c94d63);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid #231b1e;
  box-shadow: 0 2px 5px rgba(0,0,0,0.2);
  animation: badge-pop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes badge-pop {
  0% { transform: scale(0); }
  100% { transform: scale(1); }
}

.cart-bar__total { flex: 1; }

.cart-bar__amount {
  display: block;
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  line-height: 1.2;
  font-family: var(--font-body);
}

.cart-bar__hint {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 0.02em;
}

.cart-bar__checkout {
  flex-shrink: 0;
  background: linear-gradient(135deg, #e8637a 0%, #c94d63 100%);
  color: #fff;
  border: none;
  border-radius: 999px;
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 
    0 4px 14px rgba(232, 99, 122, 0.3),
    inset 0 1px 0 0 rgba(255, 255, 255, 0.2);
  transition: all 0.2s;
}

.cart-bar__checkout:active {
  transform: scale(0.95);
  box-shadow: 0 2px 6px rgba(232, 99, 122, 0.2);
}

/* 购物车栏动画 */
.cart-bar-enter-active, .cart-bar-leave-active {
  transition: opacity 0.35s, transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.cart-bar-enter-from, .cart-bar-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(28px) scale(0.92);
}

/* ── 购物车 Sheet ── */
.cart-sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(45, 35, 39, 0.4);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 200;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.cart-sheet {
  width: 100%;
  max-width: 430px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 30px 30px 0 0;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 -10px 40px rgba(45, 35, 39, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-bottom: none;
}

.cart-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 24px 16px;
  border-bottom: 1px solid rgba(232, 99, 122, 0.06);
}

.cart-sheet__title { 
  font-size: 17px; 
  font-weight: 800; 
  color: #2d2327; 
  letter-spacing: 0.02em;
}

.cart-sheet__clear {
  font-size: 13px;
  color: #a3949b;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  font-weight: 500;
  transition: color 0.2s;
}
.cart-sheet__clear:active {
  color: #e8637a;
}

.cart-sheet__list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cart-item__icon { 
  font-size: 30px; 
  flex-shrink: 0; 
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.05));
}

.cart-item__info { flex: 1; }
.cart-item__name { font-size: 14px; font-weight: 700; color: #2d2327; margin: 0 0 3px; }
.cart-item__price { font-size: 13px; color: #e8637a; font-weight: 700; margin: 0; }

.cart-item__stepper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cart-item__stepper button {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px solid rgba(232, 99, 122, 0.25);
  background: #fff;
  color: #e8637a;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.cart-item__stepper button:active { transform: scale(0.85); background: #fde8ec; }

.cart-item__stepper span {
  font-size: 14px;
  font-weight: 700;
  color: #2d2327;
  min-width: 20px;
  text-align: center;
}

.cart-sheet__footer {
  padding: 18px 24px 28px;
  border-top: 1px solid rgba(232, 99, 122, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.95);
}

.cart-sheet__total { font-size: 14px; color: #6b5b63; font-weight: 500; }
.cart-sheet__total strong { color: #e8637a; font-size: 20px; font-weight: 800; font-family: var(--font-body); }

.cart-sheet__go {
  background: linear-gradient(135deg, #e8637a, #c94d63);
  color: #fff;
  border: none;
  border-radius: 999px;
  padding: 12px 32px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 
    0 8px 20px rgba(232, 99, 122, 0.25),
    inset 0 1px 0 0 rgba(255, 255, 255, 0.2);
  transition: transform 0.2s, box-shadow 0.2s;
}
.cart-sheet__go:active {
  transform: scale(0.96);
}

/* Sheet 动画 */
.sheet-enter-active, .sheet-leave-active {
  transition: opacity 0.3s;
}
.sheet-enter-active .cart-sheet, .sheet-leave-active .cart-sheet {
  transition: transform 0.45s cubic-bezier(0.16, 1, 0.3, 1);
}
.sheet-enter-from, .sheet-leave-to {
  opacity: 0;
}
.sheet-enter-from .cart-sheet, .sheet-leave-to .cart-sheet {
  transform: translateY(100%);
}
</style>
