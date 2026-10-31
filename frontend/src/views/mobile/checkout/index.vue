<script setup>
/**
 * 确认订单：收货信息 + 订单明细 + 金额
 * 进入时按最新在售商品校正购物车；提交 POST /mobile/orders，后端按库内价格重算金额并原子扣库存。
 * 本店尚未接入在线支付：提交后订单进入门店「待接单」，页面如实说明。
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/modules/cart'
import { useCatalog } from '@/composables/useCatalog'
import { createMobileOrder } from '@/api/modules/mobile'
import { saveMyOrder } from '@/stores/modules/myOrders'
import { STORE_COPY } from '@/config/brand'
import { formatPrice } from '@/utils/format'
import ProductImg from '@/components/ProductImg.vue'
import SwIcon from '@/components/brand/SwIcon.vue'

const router = useRouter()
const cart = useCartStore()
const catalog = useCatalog()

const submitting = ref(false)
const syncing = ref(false)
const syncNotice = ref('')
const submitError = ref('')

function goBack() {
  if (window.history.state?.back) router.back()
  else router.replace('/m')
}

// 购物车存在本机，价格/库存可能已变化：进入结算页时按最新在售商品校正一次
async function syncCart() {
  if (cart.isEmpty) return
  syncing.value = true
  try {
    await catalog.load(true)
    if (catalog.error.value) return // 同步失败不阻断结算，下单时后端仍会核价与校验库存
    const { removed, adjusted, repriced } = cart.syncWithProducts(catalog.products.value)
    const notes = []
    if (removed.length) notes.push(`${removed.join('、')} 已下架或售罄，已移出购物车`)
    if (adjusted.length) notes.push(`${adjusted.join('、')} 库存不足，数量已调整`)
    if (repriced.length) notes.push(`${repriced.join('、')} 价格有更新，请确认后再提交`)
    syncNotice.value = notes.join('；')
  } finally {
    syncing.value = false
  }
}

onMounted(syncCart)

// 回头客：预填上次下单的联系信息（仅存本机）
const CONTACT_KEY = 'checkout_contact'
const lastContact = (() => {
  try { return JSON.parse(localStorage.getItem(CONTACT_KEY) || '{}') || {} } catch { return {} }
})()

const form = reactive({
  customerName: lastContact.customerName || '',
  phone: lastContact.phone || '',
  address: lastContact.address || '',
})
const errors = reactive({ customerName: '', phone: '', address: '' })
const fieldRefs = { customerName: ref(null), phone: ref(null), address: ref(null) }
const nameInput = fieldRefs.customerName
const phoneInput = fieldRefs.phone
const addressInput = fieldRefs.address

function clearError(field) {
  errors[field] = ''
  submitError.value = ''
}

function validate() {
  errors.customerName = !form.customerName.trim()
    ? '请填写收货人姓名'
    : form.customerName.trim().length > 50 ? '姓名不超过 50 个字符' : ''
  errors.phone = !form.phone
    ? '请填写手机号'
    : !/^1[3-9]\d{9}$/.test(form.phone) ? '请输入正确的 11 位手机号' : ''
  errors.address = !form.address.trim()
    ? '请填写配送地址'
    : form.address.trim().length > 200 ? '地址不超过 200 个字符' : ''
  const firstInvalid = ['customerName', 'phone', 'address'].find((f) => errors[f])
  if (firstInvalid) fieldRefs[firstInvalid].value?.focus()
  return !firstInvalid
}

const canSubmit = computed(() => !submitting.value && !syncing.value && !cart.isEmpty)

async function submitOrder() {
  if (!canSubmit.value || !validate()) return
  submitting.value = true
  submitError.value = ''
  try {
    const payload = {
      customerName: form.customerName.trim(),
      phone: form.phone,
      address: form.address.trim(),
      totalAmount: cart.totalAmount,
      items: cart.items.map((i) => ({ productId: i.id, productName: i.name, price: i.price, qty: i.qty })),
    }
    const res = await createMobileOrder(payload)
    if (res?.code === 1) {
      const orderNo = res.data?.orderNo || ''
      const amount = res.data?.amount ?? payload.totalAmount
      saveMyOrder({
        orderNo,
        phone: payload.phone,
        amount,
        items: JSON.stringify(payload.items.map(({ productName, price, qty }) => ({ productName, price, qty }))),
        createTime: new Date().toISOString(),
      })
      try {
        localStorage.setItem(CONTACT_KEY, JSON.stringify({
          customerName: payload.customerName, phone: payload.phone, address: payload.address,
        }))
      } catch { /* ignore */ }
      cart.clear()
      catalog.load(true) // 库存已变化，刷新目录
      router.replace({ name: 'mobileSuccess', query: { orderNo, amount: String(amount) } })
    } else {
      submitError.value = res?.msg || '下单失败，请稍后重试'
    }
  } catch (e) {
    // 后端原因（库存不足 / 已下架 / 价格已更新）已由拦截器提示；同步一次购物车让顾客看到最新情况
    submitError.value = e?.displayMessage || '下单失败，请稍后重试'
    if (e?.response?.status === 400) await syncCart()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="co store-page">
    <header class="co__head">
      <button
        type="button"
        class="co__back"
        aria-label="返回"
        @click="goBack"
      >
        <SwIcon
          name="chevron-left"
          :size="20"
        />
      </button>
      <h1 class="co__title sw-display">{{ STORE_COPY.titles.checkout }}</h1>
    </header>

    <div
      v-if="syncNotice"
      class="co__notice"
      role="status"
    >
      <SwIcon
        name="info"
        :size="16"
      />
      <span>{{ syncNotice }}</span>
    </div>

    <div
      v-if="cart.isEmpty"
      class="co__empty"
    >
      <p class="co__empty-title">购物车是空的</p>
      <p class="co__empty-desc">先去挑选几款喜欢的甜品吧。</p>
      <router-link
        :to="{ path: '/m', hash: '#menu' }"
        class="btn btn--primary btn--sm"
      >浏览今日甜品</router-link>
    </div>

    <div
      v-else
      class="co__grid"
    >
      <!-- 收货信息 -->
      <section
        class="card co__form"
        aria-labelledby="co-contact"
      >
        <h2
          id="co-contact"
          class="card__title"
        >收货信息</h2>
        <form
          novalidate
          @submit.prevent="submitOrder"
        >
          <div
            class="field"
            :class="{ 'has-error': errors.customerName }"
          >
            <label
              for="co-name"
              class="field__label"
            >收货人</label>
            <input
              id="co-name"
              ref="nameInput"
              v-model="form.customerName"
              class="field__input"
              type="text"
              autocomplete="name"
              maxlength="50"
              placeholder="怎么称呼您"
              :aria-invalid="!!errors.customerName"
              aria-describedby="co-name-err"
              @input="clearError('customerName')"
            >
            <p
              v-if="errors.customerName"
              id="co-name-err"
              class="field__error"
            >{{ errors.customerName }}</p>
          </div>
          <div
            class="field"
            :class="{ 'has-error': errors.phone }"
          >
            <label
              for="co-phone"
              class="field__label"
            >手机号</label>
            <input
              id="co-phone"
              ref="phoneInput"
              v-model="form.phone"
              class="field__input sw-num"
              type="tel"
              inputmode="numeric"
              autocomplete="tel"
              maxlength="11"
              placeholder="用于门店联系与查询订单"
              :aria-invalid="!!errors.phone"
              aria-describedby="co-phone-err"
              @input="clearError('phone')"
            >
            <p
              v-if="errors.phone"
              id="co-phone-err"
              class="field__error"
            >{{ errors.phone }}</p>
          </div>
          <div
            class="field"
            :class="{ 'has-error': errors.address }"
          >
            <label
              for="co-address"
              class="field__label"
            >配送地址</label>
            <textarea
              id="co-address"
              ref="addressInput"
              v-model="form.address"
              class="field__input field__input--area"
              rows="2"
              autocomplete="street-address"
              maxlength="200"
              placeholder="小区 / 楼栋 / 门牌号"
              :aria-invalid="!!errors.address"
              aria-describedby="co-address-err"
              @input="clearError('address')"
            ></textarea>
            <p
              v-if="errors.address"
              id="co-address-err"
              class="field__error"
            >{{ errors.address }}</p>
          </div>
          <p class="co__privacy">联系信息只保存在本机，方便下次自动填写。</p>
        </form>

        <div class="co__pay">
          <SwIcon
            name="card"
            :size="20"
          />
          <div>
            <p class="co__pay-title">支付方式</p>
            <p class="co__pay-desc">{{ STORE_COPY.paymentNote }}</p>
          </div>
        </div>
      </section>

      <!-- 订单明细 -->
      <aside
        class="card co__summary"
        aria-labelledby="co-items"
      >
        <h2
          id="co-items"
          class="card__title"
        >
          订单明细
          <span class="card__count">{{ cart.totalQty }} 件</span>
        </h2>
        <ul class="co__items">
          <li
            v-for="item in cart.items"
            :key="item.id"
            class="co__item"
          >
            <span class="co__thumb"><ProductImg
              :item="item"
              :width="56"
              ratio="1/1"
              :alt="item.name"
            /></span>
            <span class="co__item-info">
              <span class="co__item-name">{{ item.name }}</span>
              <span class="co__item-unit sw-num">{{ formatPrice(item.price) }} × {{ item.qty }}</span>
            </span>
            <span class="co__item-total sw-num">{{ formatPrice(item.price * item.qty) }}</span>
          </li>
        </ul>
        <dl class="co__sum">
          <div><dt>商品金额</dt><dd class="sw-num">{{ formatPrice(cart.totalAmount) }}</dd></div>
          <div><dt>配送费</dt><dd class="is-free">免费</dd></div>
          <div class="is-total"><dt>应付合计</dt><dd class="sw-num">{{ formatPrice(cart.totalAmount) }}</dd></div>
        </dl>
        <p class="co__verify">金额以门店核算为准：提交时会按最新价格与库存再次核对。</p>

        <p
          v-if="submitError"
          class="co__error"
          role="alert"
        >
          <SwIcon
            name="alert"
            :size="16"
          />
          <span>{{ submitError }}</span>
        </p>

        <button
          type="button"
          class="btn btn--primary btn--block co__submit"
          :disabled="!canSubmit"
          @click="submitOrder"
        >
          <template v-if="submitting">正在提交…</template>
          <template v-else-if="syncing">正在核对价格与库存…</template>
          <template v-else>提交订单 · <span class="sw-num">{{ formatPrice(cart.totalAmount) }}</span></template>
        </button>
      </aside>
    </div>

    <!-- 手机：底部提交栏 -->
    <div
      v-if="!cart.isEmpty"
      class="submitbar"
    >
      <div class="submitbar__sum">
        <span class="submitbar__label">应付</span>
        <span class="submitbar__total sw-num">{{ formatPrice(cart.totalAmount) }}</span>
      </div>
      <button
        type="button"
        class="btn btn--primary submitbar__btn"
        :disabled="!canSubmit"
        @click="submitOrder"
      >
        {{ submitting ? '正在提交…' : syncing ? '核对中…' : '提交订单' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.store-page {
  animation: fadeIn var(--sw-dur-3) var(--sw-ease) both;
}

.co {
  max-width: 1080px;
  margin: 0 auto;
  padding: 16px var(--store-pad, 20px) 0;
}

.co__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.co__back {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  margin-left: -10px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--sw-ink);
  cursor: pointer;
}

.co__back:hover {
  background: var(--sw-fill-soft);
}

.co__title {
  font-size: clamp(26px, 3vw, 32px);
  color: var(--sw-ink);
}

.co__notice {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding: 12px 14px;
  border-radius: var(--sw-r-md);
  background: var(--sw-warning-bg);
  color: var(--sw-warning);
  font-size: 13px;
  line-height: 1.6;
}

.co__notice .sw-icon {
  margin-top: 3px;
  flex-shrink: 0;
}

.co__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.card {
  padding: 24px;
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-lg);
  background: var(--sw-surface);
}

.card__title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 18px;
  font-size: 17px;
  font-weight: 600;
}

.card__count {
  font-size: 13px;
  font-weight: 400;
  color: var(--sw-ink-3);
}

.field {
  margin-bottom: 16px;
}

.field__label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--sw-ink-2);
}

.field__input {
  width: 100%;
  min-height: 48px;
  padding: 12px 14px;
  border: 1px solid var(--sw-border);
  border-radius: 12px;
  background: var(--sw-surface);
  color: var(--sw-ink);
  font-family: inherit;
  font-size: 15px;
  line-height: 1.5;
  outline: none;
  transition: border-color var(--sw-dur-1) var(--sw-ease), box-shadow var(--sw-dur-1) var(--sw-ease);
}

.field__input--area {
  resize: vertical;
  min-height: 76px;
}

.field__input::placeholder {
  color: var(--sw-ink-4);
}

.field__input:hover {
  border-color: var(--el-border-color-hover);
}

.field__input:focus {
  border-color: var(--sw-ink-2);
  box-shadow: 0 0 0 3px rgba(var(--ink-rgb), 0.08);
}

.field.has-error .field__input {
  border-color: var(--sw-danger);
  box-shadow: 0 0 0 3px var(--sw-danger-bg);
}

.field__error {
  margin-top: 6px;
  font-size: 12px;
  color: var(--sw-danger);
}

.co__privacy {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.co__pay {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding: 14px 16px;
  border-radius: var(--sw-r-md);
  background: var(--sw-fill-soft);
  color: var(--sw-ink-2);
}

.co__pay .sw-icon {
  margin-top: 2px;
}

.co__pay-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--sw-ink);
}

.co__pay-desc {
  margin-top: 2px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--sw-ink-3);
}

.co__summary {
  position: sticky;
  top: 96px;
}

.co__items {
  list-style: none;
  max-height: 340px;
  overflow-y: auto;
}

.co__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
}

.co__item + .co__item {
  border-top: 1px solid var(--sw-border-soft);
}

.co__thumb {
  width: 56px;
  height: 56px;
  border-radius: var(--sw-r-sm);
  overflow: hidden;
  flex-shrink: 0;
}

.co__item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.co__item-name {
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.co__item-unit {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.co__item-total {
  font-size: 14px;
  font-weight: 600;
}

.co__sum {
  margin-top: 12px;
  padding-top: 14px;
  border-top: 1px solid var(--sw-border);
}

.co__sum div {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 14px;
  color: var(--sw-ink-2);
}

.co__sum .is-free {
  color: var(--sw-success);
  font-weight: 600;
}

.co__sum .is-total {
  margin-top: 6px;
  padding-top: 10px;
  border-top: 1px dashed var(--sw-border);
  color: var(--sw-ink);
  font-weight: 600;
}

.co__sum .is-total dd {
  font-size: 22px;
  font-weight: 700;
}

.co__verify {
  margin-top: 8px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.co__error {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: var(--sw-r-md);
  background: var(--sw-danger-bg);
  color: var(--sw-danger);
  font-size: 13px;
  line-height: 1.5;
}

.co__error .sw-icon {
  margin-top: 2px;
  flex-shrink: 0;
}

.co__submit {
  margin-top: 18px;
}

.co__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 80px 16px;
  text-align: center;
}

.co__empty-title {
  font-size: 17px;
  font-weight: 600;
}

.co__empty-desc {
  margin-bottom: 12px;
  font-size: 14px;
  color: var(--sw-ink-3);
}

.submitbar {
  display: none;
}

@media (max-width: 767px) {
  .co {
    padding-bottom: calc(96px + env(safe-area-inset-bottom));
  }

  .co__head {
    margin-bottom: 16px;
  }

  .co__grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .co__summary {
    position: static;
    order: -1;
  }

  .card {
    padding: 18px 16px;
  }

  .co__submit {
    display: none;
  }

  .submitbar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 110;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
    background: var(--sw-surface);
    border-top: 1px solid var(--sw-border);
  }

  .submitbar__sum {
    display: flex;
    flex-direction: column;
  }

  .submitbar__label {
    font-size: 12px;
    color: var(--sw-ink-3);
  }

  .submitbar__total {
    font-size: 20px;
    font-weight: 700;
  }

  .submitbar__btn {
    flex: 1;
  }
}
</style>
