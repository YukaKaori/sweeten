<template>
  <div class="home-page">
    <!-- Search Bar -->
    <div class="search-bar-wrap">
      <div class="search-bar" :class="{ focused: searchFocused }">
        <span class="search-icon">🔍</span>
        <input
          ref="searchInput"
          v-model="searchQuery"
          class="search-input"
          type="text"
          placeholder="搜索甜品..."
          @focus="searchFocused = true"
          @blur="searchFocused = false"
          @input="onSearch"
        />
        <button v-if="searchQuery" class="search-clear" @click="clearSearch">✕</button>
      </div>
    </div>

    <!-- Hero Carousel -->
    <div class="hero-wrap" v-if="!searchQuery">
      <div class="hero" ref="heroRef"
           @touchstart="onTouchStart" @touchend="onTouchEnd">
        <div class="hero-track" :style="{ transform: `translateX(-${heroIdx * 100}%)` }">
          <div class="hero-slide s1"><div class="hero-content">
            <span class="hero-kicker">New Collection</span>
            <h2>夏日新品<br>限时登场</h2>
            <p class="hero-sub">精选时令鲜果，每日手作</p>
          </div></div>
          <div class="hero-slide s2"><div class="hero-content">
            <span class="hero-kicker">Bespoke</span>
            <h2>生日蛋糕<br>专属定制</h2>
            <p class="hero-sub">提前 24 小时预约，为您匠心手作</p>
          </div></div>
          <div class="hero-slide s3"><div class="hero-content">
            <span class="hero-kicker">Seasonal</span>
            <h2>冰淇淋季<br>清爽来袭</h2>
            <p class="hero-sub">意式 Gelato，低脂更丝滑</p>
          </div></div>
        </div>
        <div class="hero-dots">
          <span v-for="i in 3" :key="i" class="dot" :class="{ on: heroIdx === i-1 }"
                @click="goSlide(i-1)"></span>
        </div>
      </div>
    </div>

    <!-- Category Pills -->
    <div class="cat-scroll" ref="catScrollRef" v-if="!searchQuery">
      <button v-for="cat in categories" :key="cat.key" class="cat-pill"
              :class="{ active: activeCategory === cat.key }"
              @click="switchCategory(cat.key)">{{ cat.label }}</button>
    </div>

    <!-- Section Header -->
    <div class="section-head">
      {{ searchQuery ? '搜索结果' : categoryTitle }}
      <span class="count" v-if="!loading">({{ displayProducts.length }})</span>
    </div>

    <!-- Products -->
    <div class="products-area" ref="productsArea">
      <!-- Skeletons -->
      <div v-if="loading" class="product-grid">
        <div v-for="i in 6" :key="i" class="skeleton-card">
          <div class="sk-img"></div><div class="sk-info"><div class="sk-line"></div><div class="sk-line short"></div><div class="sk-line tiny"></div></div>
        </div>
      </div>

      <!-- Empty -->
      <div v-else-if="!displayProducts.length" class="empty-wrap">
        <div class="empty-icon">{{ searchQuery ? '🔍' : '🍽️' }}</div>
        <div class="empty-title">{{ searchQuery ? '未找到相关甜品' : '暂无商品' }}</div>
        <div class="empty-desc">{{ searchQuery ? '试试其他关键词' : '看看其他分类吧' }}</div>
      </div>

      <!-- Product Grid -->
      <div v-else class="product-grid">
        <div v-for="p in displayProducts" :key="p.id" class="product-card"
             :class="{ 'sold-out': p.stock === 0 }"
             @click="goDetail(p)">
          <div class="img-area">
            <img v-if="p.image" :src="p.image" :alt="p.name" loading="lazy"
                 @error="e => e.target.style.display='none'" />
            <span class="emoji-fallback" :style="{ display: p.image?'none':'flex' }">{{ p.icon || '🍰' }}</span>
            <div v-if="p.stock===0" class="sold-out-overlay"><span>售罄</span></div>
          </div>
          <div class="card-info">
            <div class="card-name">{{ p.name }}</div>
            <div class="card-desc">{{ p.description || '' }}</div>
            <div class="card-row">
              <div class="price-current"><span class="yen">¥</span>{{ p.price.toFixed(2) }}</div>
              <div class="stepper" v-if="p.stock !== 0" @click.stop>
                <template v-if="cartStore.getQty(p.id) > 0">
                  <button class="step-btn minus" @click.stop="cartStore.decreaseItem(p.id)">−</button>
                  <span class="step-qty">{{ cartStore.getQty(p.id) }}</span>
                  <button class="step-btn plus" @click.stop="handleAdd(p, $event)">+</button>
                </template>
                <button v-else class="add-btn" @click.stop="handleAdd(p, $event)">+</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Fly animation dot -->
    <div class="fly-dot" ref="flyDot"></div>

    <!-- Floating Cart Pill -->
    <transition name="pill-fade">
      <div v-if="!cartStore.isEmpty" class="cart-pill-wrap" @click="toggleCartSheet">
        <div class="cart-pill">
          <div class="pill-icon-wrap"><span class="pill-icon">🛒</span><span class="pill-badge">{{ cartStore.totalQty }}</span></div>
          <div class="pill-info"><div class="pill-total">¥{{ cartStore.totalAmount.toFixed(2) }}</div><div class="pill-hint">已选 {{ cartStore.totalQty }} 件</div></div>
          <button class="pill-checkout" @click.stop="goCheckout">结算</button>
        </div>
      </div>
    </transition>

    <!-- Cart Bottom Sheet -->
    <transition name="sheet">
      <div v-if="showCartSheet" class="cart-sheet-mask" @click.self="toggleCartSheet">
        <div class="cart-sheet">
          <div class="sheet-handle"></div>
          <div class="sheet-header"><h3>🛒 购物车</h3><button class="clear-btn" @click="cartStore.clear()">清空</button></div>
          <div class="sheet-list">
            <div v-if="cartStore.isEmpty" class="cart-empty"><span>🛒</span><p>购物车是空的</p></div>
            <div v-for="item in cartStore.items" :key="item.id" class="cart-item">
              <div class="ci-icon">{{ item.icon }}</div>
              <div class="ci-mid"><div class="ci-name">{{ item.name }}</div><div class="ci-price">¥{{ item.price.toFixed(2) }}</div></div>
              <div class="stepper">
                <button @click.stop="cartStore.decreaseItem(item.id)">−</button>
                <span>{{ item.qty }}</span>
                <button @click.stop="cartStore.addItem(item)">+</button>
              </div>
            </div>
          </div>
          <div class="sheet-footer">
            <div class="ft-total">合计 <em>¥{{ cartStore.totalAmount.toFixed(2) }}</em></div>
            <button class="ft-btn" :disabled="cartStore.isEmpty" @click="goCheckout">去结算</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getMobileProducts, getMobileCategories } from '@/api/modules/mobile'
import { useCartStore } from '@/stores/modules/cart'

const router = useRouter()
const cartStore = useCartStore()

const loading = ref(true)
const allProducts = ref([])
const categories = ref([{ key:'all', label:'全部' }])
const activeCategory = ref('all')
const searchQuery = ref('')
const searchFocused = ref(false)
const showCartSheet = ref(false)
const heroIdx = ref(0)
const heroCount = 3
let heroTimer = null
const flyDot = ref(null)

const categoryTitle = computed(() => {
  const m = { all:'全部甜品', cake:'蛋糕', bread:'面包', drink:'饮品', icecream:'冰淇淋', snack:'小食', gift:'礼盒' }
  return m[activeCategory.value] || '全部甜品'
})

const displayProducts = computed(() => {
  if (!searchQuery.value.trim()) return allProducts.value
  const kw = searchQuery.value.trim().toLowerCase()
  return allProducts.value.filter(p => p.name.toLowerCase().includes(kw) || (p.description||'').toLowerCase().includes(kw))
})

// ── Hero ──
function goSlide(i) { heroIdx.value = i }
function nextSlide() { heroIdx.value = (heroIdx.value+1) % heroCount }
function startTimer() { clearInterval(heroTimer); heroTimer = setInterval(nextSlide, 4200) }
let touchX = 0
function onTouchStart(e) { touchX = e.touches[0].clientX; clearInterval(heroTimer) }
function onTouchEnd(e) {
  const dx = touchX - e.changedTouches[0].clientX
  if (Math.abs(dx) > 40) goSlide(dx>0 ? (heroIdx.value+1)%heroCount : (heroIdx.value-1+heroCount)%heroCount)
  startTimer()
}

// ── API ──
async function loadCategories() {
  try { const r = await getMobileCategories(); if (r?.code===1&&r.data) categories.value = r.data } catch { /* */ }
}
async function loadProducts(cat='all') {
  loading.value = true
  try { const r = await getMobileProducts(cat); if (r?.code===1) allProducts.value = r.data||[] } catch { allProducts.value=[] }
  finally { loading.value = false }
}
function switchCategory(key) { if (activeCategory.value===key) return; activeCategory.value=key; loadProducts(key) }

// ── Search ──
let searchTimer
function onSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    if (searchQuery.value && activeCategory.value !== 'all') {
      activeCategory.value = 'all'
      loadProducts('all')
    }
  }, 300)
}
function clearSearch() { searchQuery.value = ''; searchFocused.value = false }

// ── Cart ──
function handleAdd(product, event) {
  if (product.stock === 0) return
  cartStore.addItem(product)
  // Fly animation
  if (event && flyDot.value) {
    const btn = event.target.closest('button')
    if (btn) {
      const btnRect = btn.getBoundingClientRect()
      const pill = document.querySelector('.cart-pill-wrap')
      if (pill) {
        const pillRect = pill.getBoundingClientRect()
        const dot = flyDot.value
        dot.style.setProperty('--sx', btnRect.left + btnRect.width/2 + 'px')
        dot.style.setProperty('--sy', btnRect.top + btnRect.height/2 + 'px')
        dot.style.setProperty('--ex', pillRect.left + pillRect.width/2 + 'px')
        dot.style.setProperty('--ey', pillRect.top + pillRect.height/2 + 'px')
        dot.classList.remove('fly')
        void dot.offsetWidth
        dot.classList.add('fly')
      }
    }
  }
  window.__showToast?.('已加入购物车 🛒')
}

function toggleCartSheet() { showCartSheet.value = !showCartSheet.value }
function goCheckout() { showCartSheet.value = false; router.push('/m/checkout') }
function goDetail(product) { router.push(`/m/product/${product.id}`) }

// Expose for bottom tab
window.__openCartSheet = () => { showCartSheet.value = true }

onMounted(async () => {
  startTimer()
  await Promise.all([loadCategories(), loadProducts('all')])
})

onUnmounted(() => { clearInterval(heroTimer); window.__openCartSheet = undefined })
</script>

<style scoped>
.home-page { padding-bottom: 20px; }

/* ── Search ── */
.search-bar-wrap { padding: 12px 18px 8px; }
.search-bar {
  display: flex; align-items: center; gap: 8px;
  background: rgba(255,255,255,0.62);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 0.5px solid rgba(255,255,255,0.5);
  border-radius: 999px; padding: 0 16px;
  box-shadow:
    0 8px 24px rgba(0,0,0,0.03),
    inset 0 0.5px 0 rgba(255,255,255,0.5);
  transition: all 0.35s cubic-bezier(0.16,1,0.3,1);
}
.search-bar.focused {
  background: rgba(255,255,255,0.88);
  border-color: rgba(0,0,0,0.08);
  box-shadow:
    0 12px 32px rgba(0,0,0,0.06),
    inset 0 0.5px 0 rgba(255,255,255,0.6);
  transform: scale(1.01);
}
.search-icon { font-size: 14px; opacity: 0.5; }
.search-input {
  flex: 1; border: none; outline: none; padding: 11px 0;
  font-size: 14px; background: transparent; color: #1d1d1f; font-weight: 500;
}
.search-input::placeholder { color: #aeaeb2; }
.search-clear { background: none; border: none; font-size: 14px; color: #aeaeb2; cursor: pointer; padding: 4px; }

/* ── Hero ── */
.hero-wrap { margin: 4px 18px 18px; }
.hero {
  border-radius: 24px; height: 260px; position: relative; overflow: hidden;
  box-shadow:
    0 12px 40px rgba(0,0,0,0.08),
    0 2px 6px rgba(0,0,0,0.04),
    inset 0 1px 0 rgba(255,255,255,0.1);
}
.hero-track { display: flex; height: 100%; transition: transform 0.65s cubic-bezier(0.16,1,0.3,1); }
.hero-slide { min-width: 100%; height: 100%; padding: 44px 32px; display: flex; align-items: flex-end; color: #fff; position: relative; overflow: hidden; }
.hero-slide::before { content:''; position:absolute; top:-40%; right:-20%; width:280px; height:280px; border-radius:50%; background:rgba(255,255,255,0.06); filter:blur(60px); }
.hero-slide.s1 { background:linear-gradient(155deg,#3d1c22,#8b3a44 35%,#c94d63 70%,#e8a0a8); }
.hero-slide.s2 { background:linear-gradient(155deg,#2d1f14,#7a5230 35%,#c4844a 70%,#e8c89e); }
.hero-slide.s3 { background:linear-gradient(155deg,#1a2c24,#3d7060 35%,#5b9e8a 70%,#a8d8c8); }
.hero-content { position:relative; z-index:1; }
.hero-kicker { display:block; font-size:10px; font-weight:600; letter-spacing:0.22em; text-transform:uppercase; opacity:0.5; margin-bottom:8px; }
.hero-slide h2 { font-size:34px; font-weight:640; letter-spacing:-0.6px; line-height:1.08; margin:0 0 6px; text-shadow:0 1px 2px rgba(0,0,0,0.08); }
.hero-sub { font-size:13px; font-weight:440; opacity:0.72; letter-spacing:-0.1px; }
.hero-dots { position:absolute; bottom:16px; left:50%; transform:translateX(-50%); display:flex; gap:8px; }
.hero-dots .dot { width:7px; height:7px; border-radius:4px; background:rgba(255,255,255,0.3); backdrop-filter:blur(8px); border:0.5px solid rgba(255,255,255,0.15); transition:all 0.5s cubic-bezier(0.16,1,0.3,1); cursor:pointer; }
.hero-dots .dot.on { width:26px; background:rgba(255,255,255,0.85); }

/* ── Categories ── */
.cat-scroll { padding:2px 18px 14px; overflow-x:auto; display:flex; gap:8px; scrollbar-width:none; }
.cat-scroll::-webkit-scrollbar { display:none; }
.cat-pill {
  flex-shrink:0; padding:8px 18px; border-radius:999px; font-size:13px; font-weight:500; cursor:pointer;
  white-space:nowrap; border:0.5px solid rgba(255,255,255,0.5);
  background:rgba(255,255,255,0.68); backdrop-filter:blur(20px); -webkit-backdrop-filter:blur(20px);
  color:#86868b;
  box-shadow:
    0 4px 12px rgba(0,0,0,0.02),
    inset 0 0.5px 0 rgba(255,255,255,0.6);
  transition:all 0.35s cubic-bezier(0.16,1,0.3,1);
}
.cat-pill:active { transform:scale(0.92); }
.cat-pill.active {
  background:#1d1d1f; color:#fff; font-weight:560;
  border-color:rgba(255,255,255,0.08);
  box-shadow:
    0 8px 24px rgba(0,0,0,0.15),
    inset 0 1px 0 rgba(255,255,255,0.15);
  transform:translateY(-1px);
}

/* ── Section ── */
.section-head { padding:4px 18px 10px; font-size:19px; font-weight:640; letter-spacing:-0.35px; color:#1d1d1f; display:flex; align-items:baseline; gap:6px; }
.section-head .count { font-size:13px; font-weight:480; color:#aeaeb2; }

/* ── Product Grid ── */
.products-area { padding:0 4px; }
.product-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px; padding:0 14px; }

.skeleton-card { border-radius:18px; overflow:hidden; background:rgba(255,255,255,0.5); border:0.5px solid rgba(255,255,255,0.4); }
.sk-img { aspect-ratio:1; background:linear-gradient(105deg,#ececee 25%,#f5f5f6 50%,#ececee 75%); background-size:200% 100%; animation:shimmer 1.5s infinite; }
.sk-info { padding:12px; }
.sk-line { height:12px; border-radius:6px; margin-bottom:8px; background:linear-gradient(105deg,#ececee 25%,#f5f5f6 50%,#ececee 75%); background-size:200% 100%; animation:shimmer 1.5s infinite; }
.sk-line.short { width:58%; } .sk-line.tiny { width:36%; height:17px; margin-top:10px; }
@keyframes shimmer { 0%{background-position:200% 0;} 100%{background-position:-200% 0;} }

.empty-wrap { text-align:center; padding:60px 24px; grid-column:1/-1; }
.empty-icon { font-size:52px; margin-bottom:10px; opacity:0.7; }
.empty-title { font-size:15px; font-weight:600; color:#1d1d1f; margin-bottom:4px; }
.empty-desc { font-size:12px; color:#86868b; }

.product-card {
  border-radius:18px; overflow:hidden; cursor:pointer;
  background:rgba(255,255,255,0.72); backdrop-filter:blur(18px) saturate(160%);
  -webkit-backdrop-filter:blur(18px) saturate(160%);
  border:0.5px solid rgba(255,255,255,0.55);
  box-shadow:
    0 8px 32px rgba(0,0,0,0.04),
    0 2px 6px rgba(0,0,0,0.02),
    inset 0 0.5px 0 rgba(255,255,255,0.7);
  transition:all 0.45s cubic-bezier(0.16,1,0.3,1);
  /* Liquid glass depth */
  position: relative;
}

/* Glass specular sheen on product cards */
.product-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    165deg,
    rgba(255, 255, 255, 0.2) 0%,
    transparent 50%
  );
  pointer-events: none;
  z-index: 1;
  border-radius: inherit;
  transition: opacity 0.3s;
}

.product-card:active {
  transform:scale(0.955);
  box-shadow:
    0 4px 12px rgba(0,0,0,0.03),
    inset 0 0.5px 0 rgba(255,255,255,0.6);
}
.product-card.sold-out { opacity:0.6; }
.img-area { aspect-ratio:1; display:flex; align-items:center; justify-content:center; background:linear-gradient(145deg,#fafafa,#f0f0f2); position:relative; overflow:hidden; }
.img-area::after { content:''; position:absolute; inset:0; background:radial-gradient(circle at 50% 40%,rgba(255,255,255,0.4) 0%,transparent 60%); pointer-events:none; }
.img-area img { position:absolute; inset:0; width:100%; height:100%; object-fit:cover; z-index:0; }
.emoji-fallback { position:relative; z-index:1; font-size:60px; filter:drop-shadow(0 8px 16px rgba(217,87,74,0.12)); transition:transform 0.4s; }
.product-card:active .emoji-fallback { transform:scale(1.08); }
.sold-out-overlay { position:absolute; inset:0; background:rgba(0,0,0,0.4); backdrop-filter:blur(2px); display:flex; align-items:center; justify-content:center; color:#fff; font-size:14px; font-weight:650; z-index:2; }
.card-info { padding:11px 13px 14px; }
.card-name { font-size:13px; font-weight:580; color:#1d1d1f; margin-bottom:3px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.card-desc { font-size:11px; color:#86868b; margin-bottom:10px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.card-row { display:flex; align-items:center; justify-content:space-between; }
.price-current { font-size:17px; font-weight:650; color:#1d1d1f; letter-spacing:-0.3px; }
.price-current .yen { font-size:11px; font-weight:480; color:#86868b; }
.stepper { display:flex; align-items:center; gap:8px; }
.step-btn { width:26px; height:26px; border-radius:50%; border:none; font-size:14px; font-weight:700; cursor:pointer; display:flex; align-items:center; justify-content:center; transition:all 0.25s cubic-bezier(0.34,1.56,0.64,1); }
.step-btn.minus { background:rgba(0,0,0,0.04); color:#1d1d1f; }
.step-btn.plus { background:#d9574a; color:#fff; box-shadow:0 3px 8px rgba(217,87,74,0.25); }
.step-btn:active { transform:scale(0.82); }
.step-qty { font-size:13px; font-weight:650; min-width:16px; text-align:center; }
.add-btn { width:30px; height:30px; border-radius:50%; border:none; font-size:18px; cursor:pointer; display:flex; align-items:center; justify-content:center; background:rgba(217,87,74,0.08); color:#d9574a; transition:all 0.3s cubic-bezier(0.34,1.56,0.64,1); }
.add-btn:active { transform:scale(0.78); }

/* ── Fly Animation ── */
.fly-dot {
  position:fixed; z-index:300; pointer-events:none;
  width:16px; height:16px; border-radius:50%;
  background:#d9574a; opacity:0;
}
.fly-dot.fly {
  animation: fly-to-cart 0.5s cubic-bezier(0.16,1,0.3,1) forwards;
}
@keyframes fly-to-cart {
  0% { left:var(--sx); top:var(--sy); opacity:1; transform:scale(1); }
  60% { opacity:1; transform:scale(1.3); }
  100% { left:var(--ex); top:var(--ey); opacity:0; transform:scale(0.3); }
}

/* ── Cart Pill ── */
.cart-pill-wrap { position:fixed; bottom:90px; left:50%; transform:translateX(-50%); width:calc(100% - 32px); max-width:398px; z-index:95; cursor:pointer; }
.cart-pill {
  display:flex; align-items:center; gap:14px;
  background:rgba(29,29,31,0.78);
  backdrop-filter:blur(40px) saturate(220%);
  -webkit-backdrop-filter:blur(40px) saturate(220%);
  border-radius:999px; padding:8px 8px 8px 20px;
  border:0.5px solid rgba(255,255,255,0.12);
  box-shadow:
    0 24px 56px rgba(0,0,0,0.2),
    0 4px 12px rgba(0,0,0,0.08),
    inset 0 1px 0 rgba(255,255,255,0.12);
  transition:transform 0.3s, box-shadow 0.3s;
  /* Liquid glass sheen */
  position: relative;
  overflow: hidden;
}

.cart-pill::before {
  content: '';
  position: absolute;
  top: 0; left: 20%; right: 20%; height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255,255,255,0.25),
    transparent
  );
  pointer-events: none;
}

.cart-pill:active { transform:scale(0.975); }
.pill-icon-wrap { position:relative; flex-shrink:0; }
.pill-icon { font-size:22px; }
.pill-badge { position:absolute; top:-8px; right:-8px; min-width:18px; height:18px; padding:0 4px; background:#d9574a; color:#fff; font-size:10px; font-weight:650; border-radius:9px; display:flex; align-items:center; justify-content:center; border:1.5px solid #1d1d1f; }
.pill-info { flex:1; }
.pill-total { font-size:18px; font-weight:650; color:#fff; letter-spacing:-0.3px; }
.pill-hint { font-size:11px; color:rgba(255,255,255,0.4); }
.pill-checkout { flex-shrink:0; border:none; background:rgba(255,255,255,0.14); color:#fff; font-size:14px; font-weight:590; padding:12px 22px; border-radius:999px; cursor:pointer; }
.pill-fade-enter-active,.pill-fade-leave-active { transition:opacity 0.35s,transform 0.45s cubic-bezier(0.16,1,0.3,1); }
.pill-fade-enter-from,.pill-fade-leave-to { opacity:0; transform:translateX(-50%) translateY(28px) scale(0.9); }

/* ── Cart Sheet ── */
.cart-sheet-mask { position:fixed; inset:0; z-index:200; background:rgba(0,0,0,0.32); backdrop-filter:blur(6px); display:flex; align-items:flex-end; justify-content:center; }
.cart-sheet { width:100%; max-width:430px; max-height:64dvh; display:flex; flex-direction:column; background:rgba(255,255,255,0.86); backdrop-filter:blur(36px) saturate(200%); -webkit-backdrop-filter:blur(36px) saturate(200%); border-radius:24px 24px 0 0; overflow:hidden; border:0.5px solid rgba(255,255,255,0.55); border-bottom:none; box-shadow:0 -10px 40px rgba(0,0,0,0.08),inset 0 0.5px 0 rgba(255,255,255,0.7); padding-bottom:env(safe-area-inset-bottom,16px); }
.sheet-handle { width:38px; height:4.5px; border-radius:3px; background:rgba(0,0,0,0.12); margin:10px auto; }
.sheet-header { display:flex; justify-content:space-between; align-items:center; padding:0 20px 12px; }
.sheet-header h3 { font-size:17px; font-weight:620; }
.clear-btn { font-size:13px; color:#86868b; background:none; border:none; cursor:pointer; }
.sheet-list { flex:1; overflow-y:auto; padding:4px 18px; display:flex; flex-direction:column; gap:8px; -webkit-overflow-scrolling:touch; }
.cart-empty { text-align:center; padding:42px 0; font-size:44px; color:#d1d1d6; }
.cart-empty p { font-size:13px; color:#999; }
.cart-item { display:flex; align-items:center; gap:12px; padding:10px 6px; border-bottom:0.5px solid rgba(0,0,0,0.04); }
.cart-item:last-child { border-bottom:none; }
.ci-icon { font-size:34px; flex-shrink:0; }
.ci-mid { flex:1; min-width:0; }
.ci-name { font-size:14px; font-weight:560; }
.ci-price { font-size:12px; color:#86868b; }
.cart-item .stepper button { width:28px; height:28px; border-radius:50%; border:1px solid rgba(0,0,0,0.1); background:#fff; font-size:15px; cursor:pointer; display:flex; align-items:center; justify-content:center; transition:all 0.2s; }
.cart-item .stepper button:active { transform:scale(0.82); }
.cart-item .stepper span { font-size:14px; font-weight:580; min-width:18px; text-align:center; }
.sheet-footer { padding:14px 20px; display:flex; align-items:center; justify-content:space-between; border-top:0.5px solid rgba(0,0,0,0.06); }
.ft-total { font-size:13px; color:#86868b; }
.ft-total em { font-size:20px; font-weight:650; font-style:normal; }
.ft-btn { padding:12px 30px; border-radius:999px; border:none; background:#1d1d1f; color:#fff; font-size:14px; font-weight:570; cursor:pointer; box-shadow:0 4px 14px rgba(0,0,0,0.1); }
.ft-btn:disabled { background:#d1d1d6; box-shadow:none; cursor:not-allowed; }
.sheet-enter-active,.sheet-leave-active { transition:opacity 0.3s; }
.sheet-enter-active .cart-sheet,.sheet-leave-active .cart-sheet { transition:transform 0.45s cubic-bezier(0.16,1,0.3,1); }
.sheet-enter-from,.sheet-leave-to { opacity:0; }
.sheet-enter-from .cart-sheet,.sheet-leave-to .cart-sheet { transform:translateY(100%); }
</style>
