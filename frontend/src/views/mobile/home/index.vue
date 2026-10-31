<script setup>
/**
 * 商城首页：品牌主视觉 → 服务承诺 → 人气甜品 → 今日菜单（分类 / 搜索 / 排序）
 * 商品数据全部来自后端在售商品（useCatalog），不使用演示数据。
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCatalog } from '@/composables/useCatalog'
import { STORE_COPY } from '@/config/brand'
import { formatPrice, formatCount } from '@/utils/format'
import { BRAND_HERO_IMAGE, PRODUCT_IMAGE_FOCUS } from '@/config/productImage'
import ProductCard from '@/components/store/ProductCard.vue'
import SwIcon from '@/components/brand/SwIcon.vue'

const copy = STORE_COPY
const route = useRoute()
const router = useRouter()
const { products, loading, error, load, categories, bestsellers } = useCatalog()

const titleParts = copy.hero.title.split('，')
const PROMISE_ICONS = { fresh: 'leaf', delivery: 'truck', points: 'star' }

const activeCategory = ref(typeof route.query.category === 'string' ? route.query.category : 'all')
const keyword = ref('')
const sort = ref('popular')

const heroPick = computed(() => bestsellers.value[0] || null)
const heroLoaded = ref(false)

const visible = computed(() => {
  let list = products.value
  if (activeCategory.value !== 'all') list = list.filter((p) => p.category === activeCategory.value)
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter((p) => p.name.toLowerCase().includes(kw) || (p.description || '').toLowerCase().includes(kw))
  }
  if (sort.value === 'price-asc') list = [...list].sort((a, b) => a.price - b.price)
  else if (sort.value === 'price-desc') list = [...list].sort((a, b) => b.price - a.price)
  // 售罄的放到最后，其余保持后端的销量顺序
  return [...list].sort((a, b) => (a.stock === 0) - (b.stock === 0))
})

const activeLabel = computed(() => categories.value.find((c) => c.value === activeCategory.value)?.label || '全部')

function pickCategory(value) {
  activeCategory.value = value
}

function clearFilters() {
  keyword.value = ''
  activeCategory.value = 'all'
}

function goMenu() {
  router.push({ path: '/m', hash: '#menu' })
}

onMounted(() => {
  load()
})
</script>

<template>
  <div class="home store-page">
    <!-- ===== 主视觉 ===== -->
    <section
      class="hero"
      aria-labelledby="hero-title"
    >
      <div class="hero__inner">
        <div class="hero__text">
          <p class="hero__eyebrow sw-eyebrow">{{ copy.hero.eyebrow }}</p>
          <h1
            id="hero-title"
            class="hero__title sw-display"
          >
            {{ titleParts[0] }}，<br>{{ titleParts[1] }}
          </h1>
          <p class="hero__lead">{{ copy.hero.lead }}</p>
          <div class="hero__cta">
            <button
              type="button"
              class="btn btn--primary"
              @click="goMenu"
            >
              {{ copy.hero.cta }}
              <SwIcon
                name="arrow-right"
                :size="18"
              />
            </button>
            <router-link
              to="/m/orders"
              class="btn btn--secondary hero__cta-secondary"
            >查看我的订单</router-link>
          </div>
          <router-link
            v-if="heroPick"
            :to="`/m/product/${heroPick.id}`"
            class="hero__pick"
          >
            <span class="hero__pick-label">人气之选</span>
            <span class="hero__pick-name">{{ heroPick.name }}</span>
            <span class="hero__pick-meta sw-num">{{ formatPrice(heroPick.price) }} · 已售 {{ formatCount(heroPick.sales) }} 份</span>
            <SwIcon
              name="arrow-right"
              :size="16"
            />
          </router-link>
        </div>

        <div class="hero__visual">
          <img
            class="hero__img"
            :class="{ 'is-loaded': heroLoaded }"
            :src="BRAND_HERO_IMAGE"
            :style="{ objectPosition: PRODUCT_IMAGE_FOCUS }"
            alt="喜甜招牌草莓奶油蛋糕"
            fetchpriority="high"
            decoding="async"
            @load="heroLoaded = true"
          >
        </div>
      </div>
    </section>

    <!-- ===== 服务承诺 ===== -->
    <section
      class="promises"
      aria-label="服务说明"
    >
      <ul class="promises__list">
        <li
          v-for="p in copy.promises"
          :key="p.key"
          class="promise"
        >
          <span class="promise__icon"><SwIcon
            :name="PROMISE_ICONS[p.key]"
            :size="20"
          /></span>
          <span>
            <span class="promise__title">{{ p.title }}</span>
            <span class="promise__desc">{{ p.desc }}</span>
          </span>
        </li>
      </ul>
    </section>

    <!-- ===== 人气甜品 ===== -->
    <section
      v-if="loading || bestsellers.length"
      id="bestsellers"
      class="section"
      aria-labelledby="best-title"
    >
      <header class="section__head">
        <div>
          <p class="sw-eyebrow">{{ copy.sections.bestsellers.eyebrow }}</p>
          <h2
            id="best-title"
            class="section__title sw-display"
          >{{ copy.sections.bestsellers.title }}</h2>
        </div>
        <p class="section__note">按累计销量排序</p>
      </header>

      <div
        v-if="loading && !bestsellers.length"
        class="best-grid"
      >
        <div
          v-for="i in 4"
          :key="i"
          class="skeleton-card is-tall"
        ><span class="skeleton-shimmer"></span></div>
      </div>
      <div
        v-else
        class="best-grid"
        role="list"
      >
        <ProductCard
          v-for="(p, i) in bestsellers"
          :key="p.id"
          role="listitem"
          :product="p"
          variant="feature"
          :rank="i + 1"
          :eager="i < 2"
          sizes="(min-width: 1200px) 280px, (min-width: 768px) 24vw, 62vw"
        />
      </div>
    </section>

    <!-- ===== 今日菜单 ===== -->
    <section
      id="menu"
      class="section menu"
      aria-labelledby="menu-title"
    >
      <header class="section__head">
        <div>
          <p class="sw-eyebrow">{{ copy.sections.menu.eyebrow }}</p>
          <h2
            id="menu-title"
            class="section__title sw-display"
          >{{ copy.sections.menu.title }}</h2>
        </div>
        <div class="menu__tools">
          <label class="search">
            <SwIcon
              name="search"
              :size="17"
            />
            <span class="sw-visually-hidden">搜索甜品</span>
            <input
              v-model="keyword"
              type="search"
              class="search__input"
              placeholder="搜索甜品、口味"
              enterkeyhint="search"
            >
            <button
              v-if="keyword"
              type="button"
              class="search__clear"
              aria-label="清除搜索"
              @click="keyword = ''"
            >
              <SwIcon
                name="close"
                :size="14"
              />
            </button>
          </label>
          <label class="sorter">
            <span class="sw-visually-hidden">排序</span>
            <select
              v-model="sort"
              class="sorter__select"
            >
              <option value="popular">人气优先</option>
              <option value="price-asc">价格从低到高</option>
              <option value="price-desc">价格从高到低</option>
            </select>
            <SwIcon
              name="chevron-down"
              :size="14"
              class="sorter__caret"
            />
          </label>
        </div>
      </header>

      <div
        class="chips"
        role="tablist"
        aria-label="甜品分类"
      >
        <button
          type="button"
          role="tab"
          class="chip"
          :class="{ 'is-active': activeCategory === 'all' }"
          :aria-selected="activeCategory === 'all'"
          @click="pickCategory('all')"
        >
          全部<span class="chip__count sw-num">{{ products.length }}</span>
        </button>
        <button
          v-for="c in categories"
          :key="c.value"
          type="button"
          role="tab"
          class="chip"
          :class="{ 'is-active': activeCategory === c.value }"
          :aria-selected="activeCategory === c.value"
          @click="pickCategory(c.value)"
        >
          {{ c.label }}<span class="chip__count sw-num">{{ c.count }}</span>
        </button>
      </div>

      <p
        class="menu__summary"
        aria-live="polite"
      >
        <template v-if="!loading && !error">
          {{ keyword ? `「${keyword}」` : activeLabel }} · {{ visible.length }} 款
        </template>
      </p>

      <!-- 加载中 -->
      <div
        v-if="loading && !products.length"
        class="menu-grid"
        aria-busy="true"
      >
        <div
          v-for="i in 8"
          :key="i"
          class="skeleton-card"
        ><span class="skeleton-shimmer"></span></div>
      </div>

      <!-- 加载失败 -->
      <div
        v-else-if="error && !products.length"
        class="state"
      >
        <p class="state__title">甜品菜单暂时没有加载出来</p>
        <p class="state__desc">{{ error }}</p>
        <button
          type="button"
          class="btn btn--secondary btn--sm"
          @click="load(true)"
        >
          <SwIcon
            name="refresh"
            :size="16"
          /> 重新加载
        </button>
      </div>

      <!-- 没有结果 -->
      <div
        v-else-if="!visible.length"
        class="state"
      >
        <p class="state__title">{{ keyword ? `没有找到与「${keyword}」相关的甜品` : '这个分类今天暂时没有在售甜品' }}</p>
        <p class="state__desc">换个关键词，或看看其他分类吧。</p>
        <button
          type="button"
          class="btn btn--secondary btn--sm"
          @click="clearFilters"
        >查看全部甜品</button>
      </div>

      <div
        v-else
        class="menu-grid"
        role="list"
      >
        <ProductCard
          v-for="p in visible"
          :key="p.id"
          role="listitem"
          :product="p"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.store-page {
  animation: fadeIn var(--sw-dur-3) var(--sw-ease) both;
}

.home {
  --pad: var(--store-pad, 20px);
  --max: var(--store-max, 1200px);
}

/* ---------- 主视觉 ---------- */
.hero__inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  align-items: center;
  gap: clamp(32px, 5vw, 72px);
  max-width: var(--max);
  margin: 0 auto;
  padding: clamp(24px, 4vw, 56px) var(--pad) clamp(32px, 4vw, 48px);
}

.hero__eyebrow {
  color: var(--sw-berry);
}

.hero__title {
  margin-top: 18px;
  font-size: clamp(40px, 5.2vw, 68px);
  line-height: 1.16;
  letter-spacing: 0.04em;
  color: var(--sw-ink);
}

.hero__lead {
  max-width: 420px;
  margin-top: 20px;
  font-size: 16px;
  line-height: 1.8;
  color: var(--sw-ink-2);
}

.hero__cta {
  display: flex;
  gap: 12px;
  margin-top: 32px;
  flex-wrap: wrap;
}

.hero__visual {
  position: relative;
  max-width: 480px;
  justify-self: end;
  width: 100%;
  aspect-ratio: 4 / 5;
  border-radius: 24px;
  overflow: hidden;
  background: var(--sw-fill);
  box-shadow: var(--sw-shadow-md);
}

.hero__img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.5s var(--sw-ease);
}

.hero__img.is-loaded {
  opacity: 1;
}

/* 人气之选：放在文案区，不压在主视觉上（主视觉是品牌图，不对应具体商品） */
.hero__pick {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  max-width: 100%;
  margin-top: 28px;
  padding: 10px 14px 10px 10px;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-pill);
  background: var(--sw-surface);
  color: var(--sw-ink);
  font-size: 13px;
  transition: border-color var(--sw-dur-1) var(--sw-ease), background-color var(--sw-dur-1) var(--sw-ease);
}

.hero__pick:hover {
  border-color: var(--sw-milktea);
  background: var(--sw-blush-wash);
}

.hero__pick-label {
  padding: 3px 9px;
  border-radius: var(--sw-r-pill);
  background: var(--sw-blush-soft);
  color: var(--sw-berry);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.hero__pick-name {
  font-weight: 600;
  white-space: nowrap;
}

.hero__pick-meta {
  color: var(--sw-ink-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---------- 服务承诺 ---------- */
.promises {
  max-width: var(--max);
  margin: 0 auto;
  padding: 0 var(--pad);
}

.promises__list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  list-style: none;
  border-top: 1px solid var(--sw-border);
  border-bottom: 1px solid var(--sw-border);
}

.promise {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 22px 8px;
}

.promise + .promise {
  padding-left: 28px;
  border-left: 1px solid var(--sw-border-soft);
}

.promise__icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--sw-blush-soft);
  color: var(--sw-cocoa);
  flex-shrink: 0;
}

.promise > span:last-child {
  display: flex;
  flex-direction: column;
}

.promise__title {
  font-size: 15px;
  font-weight: 600;
}

.promise__desc {
  font-size: 13px;
  color: var(--sw-ink-3);
}

/* ---------- 区块 ---------- */
.section {
  max-width: var(--max);
  margin: 0 auto;
  padding: clamp(48px, 6vw, 72px) var(--pad) 0;
  scroll-margin-top: 72px;
}

.section__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

.section__title {
  margin-top: 8px;
  font-size: clamp(26px, 3vw, 34px);
  line-height: 1.25;
  color: var(--sw-ink);
}

.section__note {
  font-size: 13px;
  color: var(--sw-ink-3);
}

.best-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

/* ---------- 菜单工具条 ---------- */
.menu__tools {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.search {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 260px;
  height: 44px;
  padding: 0 12px 0 14px;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-pill);
  background: var(--sw-surface);
  color: var(--sw-ink-3);
  transition: border-color var(--sw-dur-1) var(--sw-ease), box-shadow var(--sw-dur-1) var(--sw-ease);
}

.search:focus-within {
  border-color: var(--sw-ink-2);
  box-shadow: 0 0 0 3px rgba(var(--ink-rgb), 0.08);
}

.search__input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 14px;
  color: var(--sw-ink);
}

.search__input::-webkit-search-cancel-button {
  display: none;
}

.search__input::placeholder {
  color: var(--sw-ink-4);
}

.search__clear {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: var(--sw-fill);
  color: var(--sw-ink-2);
  cursor: pointer;
}

.sorter {
  position: relative;
  display: flex;
  align-items: center;
}

.sorter__select {
  height: 44px;
  padding: 0 36px 0 16px;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-pill);
  background: var(--sw-surface);
  color: var(--sw-ink);
  font-size: 14px;
  font-family: inherit;
  appearance: none;
  cursor: pointer;
}

.sorter__caret {
  position: absolute;
  right: 14px;
  color: var(--sw-ink-3);
  pointer-events: none;
}

.chips {
  position: sticky;
  top: 72px;
  z-index: 20;
  display: flex;
  gap: 8px;
  margin: 0 calc(var(--pad) * -1);
  padding: 10px var(--pad);
  overflow-x: auto;
  background: var(--sw-bg);
  scrollbar-width: none;
}

.chips::-webkit-scrollbar {
  display: none;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 16px;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-pill);
  background: var(--sw-surface);
  color: var(--sw-ink-2);
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color var(--sw-dur-1) var(--sw-ease), color var(--sw-dur-1) var(--sw-ease),
    border-color var(--sw-dur-1) var(--sw-ease);
}

.chip:hover {
  border-color: var(--sw-milktea);
  color: var(--sw-ink);
}

.chip.is-active {
  background: var(--sw-primary);
  border-color: var(--sw-primary);
  color: var(--sw-on-primary);
}

.chip__count {
  font-size: 12px;
  opacity: 0.6;
}

.menu__summary {
  min-height: 20px;
  margin: 8px 0 16px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

/* ---------- 状态 ---------- */
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 56px 16px;
  border: 1px dashed var(--sw-border);
  border-radius: var(--sw-r-lg);
  text-align: center;
}

.state__title {
  font-size: 15px;
  font-weight: 600;
}

.state__desc {
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.skeleton-card {
  display: block;
  aspect-ratio: 3 / 4;
  border-radius: var(--sw-r-lg);
  overflow: hidden;
  background: var(--sw-surface);
  border: 1px solid var(--sw-border-soft);
}

.skeleton-card.is-tall {
  aspect-ratio: 4 / 6;
}

.skeleton-card .skeleton-shimmer {
  display: block;
  width: 100%;
  height: 72%;
  border-radius: 0;
}

/* ---------- 响应式 ---------- */
@media (max-width: 1023px) {
  .menu-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }

  .best-grid {
    gap: 16px;
  }

  .hero__title {
    font-size: clamp(36px, 5.4vw, 52px);
  }
}

@media (max-width: 767px) {
  /* 手机主视觉：照片在上、文案在下（文字不压在照片上，保证可读） */
  .hero__inner {
    grid-template-columns: 1fr;
    gap: 22px;
    padding: 8px var(--pad) 28px;
  }

  .hero__visual {
    order: -1;
    max-width: none;
    aspect-ratio: 5 / 4;
    border-radius: 20px;
    box-shadow: none;
  }

  .hero__eyebrow {
    font-size: 10px;
  }

  .hero__title {
    margin-top: 10px;
    font-size: 34px;
  }

  .hero__lead {
    margin-top: 10px;
    font-size: 14px;
    line-height: 1.7;
  }

  .hero__cta {
    margin-top: 20px;
  }

  .hero__cta .btn {
    flex: 1;
  }

  .hero__cta-secondary {
    display: none;
  }

  .hero__pick {
    display: none;
  }

  .promises__list {
    grid-template-columns: 1fr;
    border-bottom: 0;
  }

  .promise {
    padding: 14px 4px;
    border-bottom: 1px solid var(--sw-border-soft);
  }

  .promise + .promise {
    padding-left: 4px;
    border-left: 0;
  }

  .promise__icon {
    width: 36px;
    height: 36px;
  }

  .section {
    padding-top: 40px;
    scroll-margin-top: 60px;
  }

  .section__head {
    margin-bottom: 16px;
  }

  /* 人气甜品横向滑动（scroll-padding 让吸附位置与正文左边对齐） */
  .best-grid {
    display: flex;
    gap: 12px;
    margin: 0 calc(var(--pad) * -1);
    padding: 0 var(--pad) 4px;
    overflow-x: auto;
    scroll-snap-type: x mandatory;
    scroll-padding-inline: var(--pad);
    scrollbar-width: none;
  }

  .best-grid::-webkit-scrollbar {
    display: none;
  }

  .best-grid > * {
    flex: 0 0 62%;
    scroll-snap-align: start;
  }

  .menu__tools {
    width: 100%;
    flex-wrap: nowrap;
  }

  .search {
    flex: 1 1 0;
    min-width: 0;
    width: auto;
  }

  .sorter__select {
    padding: 0 32px 0 14px;
  }

  .chips {
    top: 60px;
  }

  .menu-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
}
</style>
