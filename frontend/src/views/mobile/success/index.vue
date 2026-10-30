<script setup>
/**
 * 下单成功：订单号（可复制）+ 实付金额（后端核算值）+ 接下来会发生什么
 */
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { STORE_COPY } from '@/config/brand'
import { ORDER_STATUS, ORDER_PROGRESS } from '@/utils/constants'
import { formatPrice } from '@/utils/format'
import SwIcon from '@/components/brand/SwIcon.vue'

const router = useRouter()
const route = useRoute()
const orderNo = typeof route.query.orderNo === 'string' ? route.query.orderNo : ''
const amount = Number(route.query.amount)
const amountText = Number.isFinite(amount) && amount > 0 ? formatPrice(amount) : ''
const copied = ref(false)

const steps = ORDER_PROGRESS.map((v) => ORDER_STATUS.find((s) => s.value === v))

async function copyOrderNo() {
  try {
    await navigator.clipboard.writeText(orderNo)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    copied.value = false
  }
}
</script>

<template>
  <div class="ok store-page">
    <div class="ok__card">
      <div
        class="ok__mark"
        aria-hidden="true"
      >
        <svg viewBox="0 0 64 64">
          <circle
            cx="32"
            cy="32"
            r="30"
            class="ok__ring"
          />
          <path
            d="M20 33.5l8 8 16-17"
            class="ok__check"
          />
        </svg>
      </div>

      <h1 class="ok__title sw-display">{{ STORE_COPY.titles.success }}</h1>
      <p class="ok__lead">门店已经收到你的订单，确认后就开始制作。</p>

      <dl
        v-if="orderNo"
        class="ok__facts"
      >
        <div>
          <dt>订单编号</dt>
          <dd class="ok__no sw-num">
            {{ orderNo }}
            <button
              type="button"
              class="ok__copy"
              :aria-label="copied ? '已复制订单编号' : '复制订单编号'"
              @click="copyOrderNo"
            >
              <SwIcon
                :name="copied ? 'check' : 'copy'"
                :size="15"
              />
              {{ copied ? '已复制' : '复制' }}
            </button>
          </dd>
        </div>
        <div v-if="amountText">
          <dt>应付金额</dt>
          <dd class="ok__amount sw-num">{{ amountText }}</dd>
        </div>
      </dl>

      <ol
        class="ok__steps"
        aria-label="订单进度"
      >
        <li
          v-for="(s, i) in steps"
          :key="s.value"
          :class="{ 'is-current': i === 0 }"
        >
          <span
            class="ok__dot"
            aria-hidden="true"
          ></span>
          <span class="ok__step-label">{{ s.label }}</span>
          <span
            v-if="i === 0"
            class="sw-visually-hidden"
          >（当前）</span>
        </li>
      </ol>

      <ul class="ok__notes">
        <li><SwIcon
          name="clock"
          :size="16"
        />可在「我的订单」随时查看制作与配送进度</li>
        <li><SwIcon
          name="star"
          :size="16"
        />订单完成后，会员积分自动到账</li>
        <li><SwIcon
          name="card"
          :size="16"
        />{{ STORE_COPY.paymentNote }}</li>
      </ul>

      <div class="ok__actions">
        <button
          type="button"
          class="btn btn--primary"
          @click="router.replace('/m/orders')"
        >查看我的订单</button>
        <button
          type="button"
          class="btn btn--secondary"
          @click="router.replace('/m')"
        >继续逛逛</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.store-page {
  animation: fadeIn var(--sw-dur-3) var(--sw-ease) both;
}

.ok {
  display: flex;
  justify-content: center;
  padding: clamp(24px, 6vw, 72px) var(--store-pad, 16px) calc(48px + env(safe-area-inset-bottom));
}

.ok__card {
  width: 100%;
  max-width: 520px;
  padding: clamp(28px, 5vw, 44px);
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-xl);
  background: var(--sw-surface);
  box-shadow: var(--sw-shadow-sm);
  text-align: center;
}

.ok__mark svg {
  width: 72px;
  height: 72px;
}

.ok__ring {
  fill: var(--sw-blush-soft);
  stroke: var(--sw-blush);
  stroke-width: 1.5;
}

.ok__check {
  fill: none;
  stroke: var(--sw-cocoa);
  stroke-width: 3.2;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 40;
  stroke-dashoffset: 40;
  animation: draw 0.5s var(--sw-ease) 0.15s forwards;
}

@keyframes draw {
  to { stroke-dashoffset: 0; }
}

@media (prefers-reduced-motion: reduce) {
  .ok__check {
    stroke-dashoffset: 0;
    animation: none;
  }
}

.ok__title {
  margin-top: 20px;
  font-size: 30px;
  color: var(--sw-ink);
}

.ok__lead {
  margin-top: 8px;
  font-size: 15px;
  color: var(--sw-ink-2);
}

.ok__facts {
  margin-top: 28px;
  border-top: 1px solid var(--sw-border-soft);
  text-align: left;
}

.ok__facts div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--sw-border-soft);
}

.ok__facts dt {
  font-size: 13px;
  color: var(--sw-ink-3);
}

.ok__no {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--sw-font-mono);
  font-size: 14px;
  font-weight: 600;
  word-break: break-all;
}

.ok__copy {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-pill);
  background: var(--sw-surface);
  color: var(--sw-ink-2);
  font-family: var(--sw-font-sans);
  font-size: 12px;
  cursor: pointer;
  flex-shrink: 0;
}

.ok__copy:hover {
  color: var(--sw-ink);
  border-color: var(--sw-milktea);
}

.ok__amount {
  font-size: 20px;
  font-weight: 700;
}

.ok__steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  list-style: none;
  margin-top: 28px;
}

.ok__steps li {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.ok__steps li + li::before {
  content: '';
  position: absolute;
  top: 6px;
  right: 50%;
  width: 100%;
  height: 2px;
  background: var(--sw-border);
}

.ok__dot {
  position: relative;
  z-index: 1;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--sw-surface);
  border: 2px solid var(--sw-border);
}

.ok__steps .is-current {
  color: var(--sw-ink);
  font-weight: 600;
}

.ok__steps .is-current .ok__dot {
  border-color: var(--sw-cherry);
  background: var(--sw-cherry);
  box-shadow: 0 0 0 4px var(--sw-blush-soft);
}

.ok__notes {
  list-style: none;
  margin-top: 28px;
  padding: 14px 16px;
  border-radius: var(--sw-r-md);
  background: var(--sw-fill-soft);
  text-align: left;
}

.ok__notes li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 5px 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--sw-ink-2);
}

.ok__notes .sw-icon {
  margin-top: 3px;
  color: var(--sw-ink-3);
}

.ok__actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 28px;
}

@media (max-width: 420px) {
  .ok__actions {
    grid-template-columns: 1fr;
  }
}
</style>
