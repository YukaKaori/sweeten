<script setup>
/**
 * 我的订单（H5 无登录体系）：本机记录的订单号 + 手机号，向后端查询实时状态。
 * 换了设备可以用「订单号 + 下单手机号」查回订单（两者都匹配才返回，见 GET /mobile/orders/:orderNo）。
 */
import { ref, reactive, onMounted } from 'vue'
import { getMobileOrder } from '@/api/modules/mobile'
import { loadMyOrders, saveMyOrder } from '@/stores/modules/myOrders'
import { useStoreUi } from '@/stores/modules/storeUi'
import { STORE_COPY } from '@/config/brand'
import { ORDER_STATUS, ORDER_PROGRESS, parseOrderItems } from '@/utils/constants'
import { formatPrice, formatShortDateTime } from '@/utils/format'
import StatusBadge from '@/components/StatusBadge.vue'
import SwIcon from '@/components/brand/SwIcon.vue'

const ui = useStoreUi()
const orders = ref(loadMyOrders().map((o) => ({ ...o, status: o.status ?? null, lookupError: '' })))
const refreshing = ref(false)

const steps = ORDER_PROGRESS.map((v) => ORDER_STATUS.find((s) => s.value === v))
const statusDesc = (status) => ORDER_STATUS.find((s) => s.value === status)?.desc || '正在查询最新状态'
const stepState = (order, value) => {
  if (order.status == null || order.status === 5 || order.status === 0) return ''
  if (value < order.status) return 'is-done'
  if (value === order.status) return order.status === 4 ? 'is-done' : 'is-current'
  return ''
}

async function refreshOne(o) {
  try {
    const r = await getMobileOrder(o.orderNo, o.phone)
    if (r?.code === 1 && r.data) {
      Object.assign(o, {
        status: r.data.status,
        amount: r.data.amount,
        items: r.data.items,
        createTime: r.data.createTime || o.createTime,
        lookupError: '',
      })
    }
  } catch (e) {
    o.lookupError = e?.response?.status === 404 ? '门店未查到该订单，可能已被删除' : '状态获取失败，请稍后刷新'
  }
}

async function refreshAll() {
  refreshing.value = true
  try {
    await Promise.all(orders.value.map(refreshOne))
  } finally {
    refreshing.value = false
  }
}

// ---- 查找订单 ----
const lookupOpen = ref(false)
const lookup = reactive({ orderNo: '', phone: '', error: '', loading: false })

async function findOrder() {
  lookup.error = ''
  const orderNo = lookup.orderNo.trim().toUpperCase()
  if (!orderNo) {
    lookup.error = '请输入订单编号'
    return
  }
  if (!/^1[3-9]\d{9}$/.test(lookup.phone)) {
    lookup.error = '请输入下单时填写的 11 位手机号'
    return
  }
  lookup.loading = true
  try {
    const r = await getMobileOrder(orderNo, lookup.phone)
    if (r?.code === 1 && r.data) {
      const record = {
        orderNo: r.data.orderNo,
        phone: lookup.phone,
        amount: r.data.amount,
        items: r.data.items,
        createTime: r.data.createTime,
      }
      saveMyOrder(record)
      orders.value = [
        { ...record, status: r.data.status, lookupError: '' },
        ...orders.value.filter((o) => o.orderNo !== record.orderNo),
      ]
      lookup.orderNo = ''
      lookupOpen.value = false
      ui.showToast('已找到订单，并保存到本机', 'success')
    }
  } catch (e) {
    lookup.error = e?.response?.status === 404 ? '没有找到这笔订单，请核对订单编号与手机号' : '查询失败，请检查网络后重试'
  } finally {
    lookup.loading = false
  }
}

onMounted(() => {
  if (orders.value.length) refreshAll()
})
</script>

<template>
  <div class="orders store-page">
    <header class="orders__head">
      <div>
        <h1 class="orders__title sw-display">{{ STORE_COPY.titles.orders }}</h1>
        <p class="orders__sub">订单记录保存在本机浏览器，状态实时向门店查询</p>
      </div>
      <div class="orders__tools">
        <button
          v-if="orders.length"
          type="button"
          class="btn btn--secondary btn--sm"
          :disabled="refreshing"
          @click="refreshAll"
        >
          <SwIcon
            name="refresh"
            :size="16"
          />
          {{ refreshing ? '刷新中…' : '刷新状态' }}
        </button>
        <button
          type="button"
          class="btn btn--ghost btn--sm"
          :aria-expanded="lookupOpen"
          aria-controls="order-lookup"
          @click="lookupOpen = !lookupOpen"
        >
          <SwIcon
            name="search"
            :size="16"
          />
          查找订单
        </button>
      </div>
    </header>

    <form
      v-show="lookupOpen"
      id="order-lookup"
      class="lookup"
      novalidate
      @submit.prevent="findOrder"
    >
      <p class="lookup__title">在其他设备下过单？输入订单编号和下单手机号找回订单</p>
      <div class="lookup__row">
        <label class="lookup__field">
          <span class="sw-visually-hidden">订单编号</span>
          <input
            v-model="lookup.orderNo"
            type="text"
            placeholder="订单编号，如 DS2026…"
            autocomplete="off"
            maxlength="32"
          >
        </label>
        <label class="lookup__field">
          <span class="sw-visually-hidden">下单手机号</span>
          <input
            v-model="lookup.phone"
            type="tel"
            inputmode="numeric"
            maxlength="11"
            placeholder="下单手机号"
            autocomplete="tel"
          >
        </label>
        <button
          type="submit"
          class="btn btn--primary btn--sm"
          :disabled="lookup.loading"
        >{{ lookup.loading ? '查询中…' : '查询' }}</button>
      </div>
      <p
        v-if="lookup.error"
        class="lookup__error"
        role="alert"
      >{{ lookup.error }}</p>
    </form>

    <div
      v-if="!orders.length"
      class="orders__empty"
    >
      <svg
        viewBox="0 0 64 64"
        aria-hidden="true"
        class="orders__empty-art"
      >
        <circle
          cx="32"
          cy="32"
          r="30"
        />
        <path d="M17 34c4.5 10.5 25.5 10.5 30 0" />
      </svg>
      <p class="orders__empty-title">还没有订单</p>
      <p class="orders__empty-desc">在这台设备上下的订单会出现在这里，随时查看制作与配送进度。</p>
      <router-link
        :to="{ path: '/m', hash: '#menu' }"
        class="btn btn--primary btn--sm"
      >去挑选甜品</router-link>
    </div>

    <ul
      v-else
      class="orders__list"
    >
      <li
        v-for="o in orders"
        :key="o.orderNo"
        class="order"
      >
        <header class="order__head">
          <div class="order__id">
            <span class="order__label">订单编号</span>
            <span class="order__no sw-num">{{ o.orderNo }}</span>
          </div>
          <StatusBadge :status="o.status" />
        </header>

        <p class="order__desc">{{ statusDesc(o.status) }}</p>

        <ol
          v-if="o.status != null && o.status !== 5 && o.status !== 0"
          class="track"
          aria-label="订单进度"
        >
          <li
            v-for="s in steps"
            :key="s.value"
            :class="stepState(o, s.value)"
          >
            <span
              class="track__dot"
              aria-hidden="true"
            ></span>
            <span class="track__label">{{ s.label }}</span>
          </li>
        </ol>

        <ul class="order__items">
          <li
            v-for="(line, idx) in parseOrderItems(o.items)"
            :key="idx"
          >
            <span class="order__item-name">{{ line.name }}</span>
            <span class="order__item-qty sw-num">{{ line.qty ? `× ${line.qty}` : '' }}</span>
          </li>
        </ul>

        <footer class="order__foot">
          <span class="order__time">{{ formatShortDateTime(o.createTime) }}</span>
          <span class="order__amount">合计 <strong class="sw-num">{{ formatPrice(o.amount) }}</strong></span>
        </footer>
        <p
          v-if="o.lookupError"
          class="order__error"
        >{{ o.lookupError }}</p>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.store-page {
  animation: fadeIn var(--sw-dur-3) var(--sw-ease) both;
}

.orders {
  max-width: 820px;
  margin: 0 auto;
  padding: 24px var(--store-pad, 16px) 0;
}

.orders__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.orders__title {
  font-size: clamp(26px, 3vw, 32px);
  color: var(--sw-ink);
}

.orders__sub {
  margin-top: 4px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.orders__tools {
  display: flex;
  gap: 8px;
}

.lookup {
  margin-bottom: 20px;
  padding: 16px;
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-lg);
  background: var(--sw-surface);
}

.lookup__title {
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--sw-ink-2);
}

.lookup__row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.lookup__field {
  flex: 1 1 200px;
}

.lookup__field input {
  width: 100%;
  height: 40px;
  padding: 0 14px;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-pill);
  background: var(--sw-surface);
  color: var(--sw-ink);
  font-size: 14px;
  outline: none;
}

.lookup__field input:focus {
  border-color: var(--sw-ink-2);
  box-shadow: 0 0 0 3px rgba(var(--ink-rgb), 0.08);
}

.lookup__error {
  margin-top: 10px;
  font-size: 13px;
  color: var(--sw-danger);
}

.orders__list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.order {
  padding: 20px;
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-lg);
  background: var(--sw-surface);
}

.order__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.order__id {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.order__label {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.order__no {
  font-family: var(--sw-font-mono);
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order__desc {
  margin-top: 10px;
  font-size: 13px;
  color: var(--sw-ink-2);
}

.track {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  list-style: none;
  margin: 16px 0 4px;
}

.track li {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--sw-ink-4);
}

.track li + li::before {
  content: '';
  position: absolute;
  top: 5px;
  right: 50%;
  width: 100%;
  height: 2px;
  background: var(--sw-border);
}

.track li.is-done + li::before,
.track li.is-done + li.is-current::before {
  background: var(--sw-blush);
}

.track__dot {
  position: relative;
  z-index: 1;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid var(--sw-border);
  background: var(--sw-surface);
}

.track .is-done {
  color: var(--sw-ink-2);
}

.track .is-done .track__dot {
  border-color: var(--sw-blush);
  background: var(--sw-blush);
}

.track .is-current {
  color: var(--sw-ink);
  font-weight: 600;
}

.track .is-current .track__dot {
  border-color: var(--sw-cherry);
  background: var(--sw-cherry);
  box-shadow: 0 0 0 4px var(--sw-blush-soft);
}

.order__items {
  list-style: none;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--sw-border);
}

.order__items li {
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
  font-size: 14px;
}

.order__item-qty {
  color: var(--sw-ink-3);
}

.order__foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--sw-border-soft);
}

.order__time {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.order__amount {
  font-size: 13px;
  color: var(--sw-ink-2);
}

.order__amount strong {
  margin-left: 4px;
  font-size: 17px;
  color: var(--sw-ink);
}

.order__error {
  margin-top: 10px;
  font-size: 12px;
  color: var(--sw-danger);
}

.orders__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 64px 16px;
  text-align: center;
}

.orders__empty-art {
  width: 72px;
  margin-bottom: 8px;
  fill: var(--sw-fill-soft);
  stroke: var(--sw-ink-4);
  stroke-width: 2.4;
  stroke-linecap: round;
}

.orders__empty-art path {
  fill: none;
}

.orders__empty-title {
  font-size: 17px;
  font-weight: 600;
}

.orders__empty-desc {
  max-width: 320px;
  margin-bottom: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--sw-ink-3);
}

@media (max-width: 767px) {
  .orders {
    padding-top: 16px;
  }

  .order {
    padding: 16px;
  }
}
</style>
