<template>
  <div class="checkout-page">
    <!-- Back Nav -->
    <div class="page-nav">
      <button class="back-btn" @click="router.back()">
        <span class="back-arrow">←</span> 返回
      </button>
      <h2 class="page-title">确认订单</h2>
    </div>

    <!-- Order Items Card -->
    <div class="glass-card">
      <div class="card-title">🛒 购物明细</div>
      <div class="order-items">
        <div v-for="item in cartStore.items" :key="item.id" class="order-item">
          <div class="oi-icon">{{ item.icon }}</div>
          <div class="oi-info">
            <p class="oi-name">{{ item.name }}</p>
            <p class="oi-detail">¥{{ item.price.toFixed(2) }} × {{ item.qty }}</p>
          </div>
          <div class="oi-subtotal">¥{{ (item.price * item.qty).toFixed(2) }}</div>
        </div>
      </div>
      <div class="order-divider"></div>
      <div class="order-summary">
        <span>共 {{ cartStore.totalQty }} 件商品</span>
        <span class="summary-total">合计 <strong>¥{{ cartStore.totalAmount.toFixed(2) }}</strong></span>
      </div>
    </div>

    <!-- Contact Form Card -->
    <div class="glass-card">
      <div class="card-title">📋 收货信息</div>
      <div class="form">
        <div class="form-field" :class="{ error: errors.customerName }">
          <label class="form-label">姓名</label>
          <input
            v-model="form.customerName"
            class="form-input"
            type="text"
            placeholder="请输入您的姓名"
            @input="clearError('customerName')"
          />
          <p v-if="errors.customerName" class="form-error">{{ errors.customerName }}</p>
        </div>
        <div class="form-field" :class="{ error: errors.phone }">
          <label class="form-label">手机号</label>
          <input
            v-model="form.phone"
            class="form-input"
            type="tel" maxlength="11"
            placeholder="请输入手机号"
            @input="clearError('phone')"
          />
          <p v-if="errors.phone" class="form-error">{{ errors.phone }}</p>
        </div>
        <div class="form-field" :class="{ error: errors.address }">
          <label class="form-label">配送地址</label>
          <input
            v-model="form.address"
            class="form-input"
            type="text"
            placeholder="街道、门牌号"
            @input="clearError('address')"
          />
          <p v-if="errors.address" class="form-error">{{ errors.address }}</p>
        </div>
      </div>
    </div>

    <!-- Price Summary -->
    <div class="glass-card price-card">
      <div class="price-row">
        <span>商品金额</span>
        <span>¥{{ cartStore.totalAmount.toFixed(2) }}</span>
      </div>
      <div class="price-row">
        <span>配送费</span>
        <span class="price-free">免费</span>
      </div>
      <div class="price-row price-row--total">
        <span>实付款</span>
        <span class="price-final">¥{{ cartStore.totalAmount.toFixed(2) }}</span>
      </div>
    </div>

    <!-- Submit -->
    <div class="submit-area">
      <button
        class="submit-btn"
        :class="{ loading: submitting }"
        :disabled="submitting || cartStore.isEmpty"
        @click="submitOrder"
      >
        <span v-if="submitting" class="loading-dots">
          <span></span><span></span><span></span>
        </span>
        <span v-else>立即下单 · ¥{{ cartStore.totalAmount.toFixed(2) }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/modules/cart'
import { createMobileOrder } from '@/api/modules/mobile'

const router = useRouter()
const cartStore = useCartStore()

const submitting = ref(false)

const form = reactive({ customerName: '', phone: '', address: '' })
const errors = reactive({ customerName: '', phone: '', address: '' })

function clearError(field) { errors[field] = '' }

function validate() {
  let valid = true
  if (!form.customerName.trim()) { errors.customerName = '请输入姓名'; valid = false }
  else if (form.customerName.trim().length > 50) { errors.customerName = '姓名不超过 50 个字符'; valid = false }

  if (!form.phone) { errors.phone = '请输入手机号'; valid = false }
  else if (!/^1[3-9]\d{9}$/.test(form.phone)) { errors.phone = '请输入正确的 11 位手机号'; valid = false }

  if (!form.address.trim()) { errors.address = '请输入配送地址'; valid = false }
  else if (form.address.trim().length > 200) { errors.address = '地址不超过 200 个字符'; valid = false }

  return valid
}

async function submitOrder() {
  if (!validate() || cartStore.isEmpty) return
  submitting.value = true
  try {
    const res = await createMobileOrder({
      customerName: form.customerName.trim(),
      phone: form.phone,
      address: form.address.trim(),
      totalAmount: cartStore.totalAmount,
      items: cartStore.items.map(i => ({
        productId: i.id, productName: i.name, price: i.price, qty: i.qty,
      })),
    })
    if (res?.code === 1) {
      const orderNo = res.data?.orderNo || ''
      cartStore.clear()
      router.replace({ name: 'mobileSuccess', query: { orderNo } })
    } else {
      alert(res?.msg || '下单失败')
    }
  } catch { alert('网络异常，请稍后重试') }
  finally { submitting.value = false }
}
</script>

<style scoped>
.checkout-page { padding: 0 0 120px; }

/* ── Nav ── */
.page-nav {
  display: flex; align-items: center; padding: 18px 18px 14px; gap: 12px;
}
.back-btn {
  display: flex; align-items: center; gap: 6px;
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px);
  border: 0.5px solid rgba(255,255,255,0.5);
  border-radius: 999px; padding: 7px 18px;
  font-size: 13px; font-weight: 600; color: #86868b; cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16,1,0.3,1);
}
.back-btn:active { transform: scale(0.95); }
.back-arrow { font-size: 15px; }
.page-title { font-size: 16px; font-weight: 640; color: #1d1d1f; margin: 0; letter-spacing: -0.2px; }

/* ── Glass Card ── */
.glass-card {
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  margin: 0 18px 14px; border-radius: 22px; padding: 20px;
  border: 0.5px solid rgba(255,255,255,0.5);
  box-shadow: 0 4px 18px rgba(0,0,0,0.03), inset 0 0.5px 0 rgba(255,255,255,0.6);
}
.card-title { font-size: 14px; font-weight: 640; color: #1d1d1f; margin-bottom: 16px; letter-spacing: -0.15px; }

/* ── Order Items ── */
.order-items { display: flex; flex-direction: column; gap: 14px; margin-bottom: 14px; }
.order-item { display: flex; align-items: center; gap: 12px; }
.oi-icon { font-size: 30px; flex-shrink: 0; }
.oi-info { flex: 1; }
.oi-name { font-size: 13px; font-weight: 580; color: #1d1d1f; margin: 0 0 3px; }
.oi-detail { font-size: 12px; color: #aeaeb2; margin: 0; font-weight: 500; }
.oi-subtotal { font-size: 14px; font-weight: 650; color: #1d1d1f; }
.order-divider { border-top: 0.5px dashed rgba(0,0,0,0.06); margin-bottom: 12px; }
.order-summary { display: flex; justify-content: space-between; align-items: center; font-size: 13px; color: #86868b; }
.summary-total strong { color: #1d1d1f; font-size: 17px; font-weight: 650; }

/* ── Form ── */
.form { display: flex; flex-direction: column; gap: 14px; }
.form-field { display: flex; flex-direction: column; gap: 5px; }
.form-label { font-size: 11px; font-weight: 600; color: #86868b; letter-spacing: 0.03em; padding-left: 2px; }
.form-input {
  padding: 13px 16px; border: 1px solid rgba(0,0,0,0.06); border-radius: 14px;
  font-size: 14px; font-family: inherit; color: #1d1d1f;
  background: rgba(245,245,247,0.6); outline: none;
  transition: all 0.25s cubic-bezier(0.16,1,0.3,1);
}
.form-input:focus {
  border-color: rgba(0,0,0,0.15); background: #fff;
  box-shadow: 0 0 0 3px rgba(0,0,0,0.02);
}
.form-input::placeholder { color: #c0b0b8; }
.form-field.error .form-input { border-color: rgba(248,113,113,0.5); background: rgba(254,242,242,0.6); }
.form-error { font-size: 11px; color: #f87171; margin: 0; font-weight: 500; }

/* ── Price ── */
.price-row { display: flex; justify-content: space-between; align-items: center; font-size: 13px; color: #86868b; padding: 8px 0; }
.price-free { color: #34c759; font-weight: 650; }
.price-row--total { border-top: 0.5px dashed rgba(0,0,0,0.06); margin-top: 6px; padding-top: 14px; font-weight: 600; color: #1d1d1f; }
.price-final { font-size: 22px; font-weight: 650; color: #1d1d1f; letter-spacing: -0.3px; }

/* ── Submit ── */
.submit-area {
  position: fixed; bottom: 0; left: 50%; transform: translateX(-50%);
  width: 100%; max-width: 430px;
  padding: 16px 18px calc(16px + env(safe-area-inset-bottom, 0px));
  background: rgba(245,245,247,0.60);
  backdrop-filter: blur(28px) saturate(200%);
  -webkit-backdrop-filter: blur(28px) saturate(200%);
  border-top: 0.5px solid rgba(0,0,0,0.05);
  box-shadow: inset 0 0.5px 0 rgba(255,255,255,0.5);
  z-index: 10;
}
.submit-btn {
  width: 100%; padding: 15px; border: none; border-radius: 16px;
  background: #1d1d1f; color: #fff;
  font-size: 16px; font-weight: 590; cursor: pointer;
  letter-spacing: -0.15px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  transition: all 0.3s cubic-bezier(0.16,1,0.3,1);
  display: flex; align-items: center; justify-content: center;
  min-height: 52px; position: relative; overflow: hidden;
}
.submit-btn::after {
  content: ''; position: absolute; top: 0; left: -150%; width: 50%; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.18), transparent);
  transform: skewX(-20deg);
  animation: btn-shimmer 6s infinite ease-in-out;
}
@keyframes btn-shimmer {
  0% { left: -150%; } 30% { left: 150%; } 100% { left: 150%; }
}
.submit-btn:active:not(:disabled) { transform: scale(0.97); }
.submit-btn:disabled { background: #d1d1d6; box-shadow: none; cursor: not-allowed; }
.loading-dots { display: flex; gap: 6px; align-items: center; }
.loading-dots span {
  width: 7px; height: 7px; background: rgba(255,255,255,0.8); border-radius: 50%;
  animation: bounce 1.2s infinite;
}
.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-6px); }
}
</style>
