<script setup>
/**
 * 商城商品卡片：图片 / 名称 / 一句描述 / 价格 / 库存状态 / 加购
 * variant: grid（菜单网格，1:1）| feature（人气推荐，4:5 竖图）
 */
import { computed } from 'vue'
import { useCartStore } from '@/stores/modules/cart'
import { useStoreUi } from '@/stores/modules/storeUi'
import { formatPrice, savingOf } from '@/utils/format'
import ProductImg from '@/components/ProductImg.vue'
import QtyStepper from '@/components/QtyStepper.vue'
import SwIcon from '@/components/brand/SwIcon.vue'

const LOW_STOCK = 10

const props = defineProps({
  product: { type: Object, required: true },
  variant: { type: String, default: 'grid' },
  eager: { type: Boolean, default: false },
  sizes: { type: String, default: '(min-width: 1200px) 280px, (min-width: 768px) 30vw, 46vw' },
  rank: { type: Number, default: 0 },
})

const cart = useCartStore()
const ui = useStoreUi()

const soldOut = computed(() => props.product.stock === 0)
const lowStock = computed(() => {
  const s = props.product.stock
  return s != null && s > 0 && s < LOW_STOCK
})
const qty = computed(() => cart.getQty(props.product.id))
const remaining = computed(() => cart.remaining(props.product))
const saving = computed(() => savingOf(props.product))
const to = computed(() => `/m/product/${props.product.id}`)

function add() {
  if (soldOut.value) return
  if (!cart.addItem(props.product)) {
    ui.showToast(`「${props.product.name}」库存只剩 ${props.product.stock} 件，已全部在购物车里`, 'warning')
    return
  }
  ui.showToast(`已加入购物车：${props.product.name}`, 'success')
}

/** 步进器 +1：不重复弹「已加入」，只在到达库存上限时提示 */
function increase() {
  if (!cart.addItem(props.product)) {
    ui.showToast(`「${props.product.name}」库存只剩 ${props.product.stock} 件`, 'warning')
  }
}

function decrease() {
  cart.decreaseItem(props.product.id)
}
</script>

<template>
  <article
    class="pcard"
    :class="[`pcard--${variant}`, { 'is-soldout': soldOut }]"
  >
    <router-link
      :to="to"
      class="pcard__media"
      :aria-label="`${product.name}，查看详情`"
      tabindex="-1"
    >
      <ProductImg
        :item="product"
        :sizes="sizes"
        :ratio="variant === 'feature' ? '4/5' : '1/1'"
        :alt="product.name"
        :eager="eager"
      />
      <span
        v-if="rank"
        class="pcard__rank"
      >TOP {{ rank }}</span>
      <span
        v-if="soldOut"
        class="pcard__flag is-out"
      >今日售罄</span>
      <span
        v-else-if="lowStock"
        class="pcard__flag"
      >仅剩 {{ product.stock }} 件</span>
    </router-link>

    <div class="pcard__body">
      <router-link
        :to="to"
        class="pcard__name"
      >{{ product.name }}</router-link>
      <p
        v-if="product.description"
        class="pcard__desc"
      >{{ product.description }}</p>

      <div class="pcard__foot">
        <div class="pcard__price">
          <span class="pcard__now sw-num">{{ formatPrice(product.price) }}</span>
          <s
            v-if="saving"
            class="pcard__was sw-num"
            :aria-label="`原价 ${formatPrice(product.originalPrice)}`"
          >{{ formatPrice(product.originalPrice) }}</s>
        </div>

        <span
          v-if="soldOut"
          class="pcard__unavailable"
        >已售罄</span>
        <QtyStepper
          v-else-if="qty > 0"
          :model-value="qty"
          :max="qty + remaining"
          size="sm"
          allow-remove
          :label="`${product.name} 数量`"
          @increase="increase"
          @decrease="decrease"
        />
        <button
          v-else
          type="button"
          class="pcard__add"
          :aria-label="`加入购物车：${product.name}`"
          @click="add"
        >
          <SwIcon
            name="plus"
            :size="18"
            :stroke="1.9"
          />
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.pcard {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  border-radius: var(--sw-r-lg);
  background: var(--sw-surface);
  border: 1px solid var(--sw-border-soft);
  transition: box-shadow var(--sw-dur-2) var(--sw-ease), border-color var(--sw-dur-2) var(--sw-ease);
}

.pcard:hover {
  border-color: var(--sw-border);
  box-shadow: var(--sw-shadow-md);
}

.pcard__media {
  position: relative;
  display: block;
  overflow: hidden;
}

.pcard__media :deep(.pimg__img) {
  transition: opacity var(--sw-dur-3) var(--sw-ease), transform 0.6s var(--sw-ease);
}

.pcard:hover .pcard__media :deep(.pimg__img) {
  transform: scale(1.03);
}

.is-soldout .pcard__media :deep(.pimg__img) {
  filter: grayscale(0.55) opacity(0.75);
}

.pcard__rank {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 8px;
  border-radius: var(--sw-r-pill);
  background: rgba(255, 254, 252, 0.92);
  color: var(--sw-cocoa);
  font-family: var(--sw-font-latin);
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.14em;
}

.pcard__flag {
  position: absolute;
  left: 10px;
  bottom: 10px;
  padding: 4px 9px;
  border-radius: var(--sw-r-pill);
  background: rgba(255, 254, 252, 0.94);
  color: var(--sw-berry);
  font-size: 12px;
  font-weight: 600;
}

.pcard__flag.is-out {
  background: rgba(36, 26, 22, 0.72);
  color: #fffefc;
}

.pcard__body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 12px 14px 14px;
}

.pcard__name {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--sw-ink);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pcard__name:hover {
  color: var(--sw-accent);
}

.pcard__desc {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--sw-ink-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pcard__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
}

.pcard__price {
  display: flex;
  align-items: baseline;
  gap: 6px;
  min-width: 0;
  flex-wrap: wrap;
}

.pcard__now {
  font-size: 17px;
  font-weight: 700;
  color: var(--sw-ink);
}

.pcard__was {
  font-size: 12px;
  color: var(--sw-ink-4);
}

.pcard__add {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: 0;
  border-radius: 50%;
  background: var(--sw-primary);
  color: var(--sw-on-primary);
  cursor: pointer;
  flex-shrink: 0;
  position: relative;
  transition: background-color var(--sw-dur-1) var(--sw-ease), transform var(--sw-dur-1) var(--sw-ease);
}

.pcard__add::after {
  content: '';
  position: absolute;
  inset: -4px;
}

.pcard__add:hover {
  background: var(--sw-primary-hover);
}

.pcard__add:active {
  transform: scale(0.94);
}

.pcard__unavailable {
  font-size: 12px;
  color: var(--sw-ink-3);
}

/* 人气推荐：竖图大卡 */
.pcard--feature .pcard__body {
  padding: 14px 16px 16px;
}

.pcard--feature .pcard__name {
  font-size: 16px;
}

@media (max-width: 420px) {
  .pcard__body {
    padding: 10px 10px 12px;
  }

  .pcard__name {
    font-size: 14px;
  }

  .pcard__now {
    font-size: 16px;
  }

  .pcard__was {
    display: none;
  }
}
</style>
