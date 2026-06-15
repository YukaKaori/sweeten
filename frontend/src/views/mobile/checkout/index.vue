<template>
  <div class="checkout-page">
    <!-- 顶部返回 -->
    <div class="page-nav">
      <button class="back-btn" @click="router.back()">
        <span class="back-arrow">←</span> 返回
      </button>
      <h2 class="page-title">确认订单</h2>
    </div>

    <!-- 订单明细卡片 -->
    <div class="section-card">
      <div class="section-title">🛒 购物明细</div>
      <div class="order-items">
        <div v-for="item in cartStore.items" :key="item.id" class="order-item">
          <div class="order-item__icon">{{ item.icon }}</div>
          <div class="order-item__info">
            <p class="order-item__name">{{ item.name }}</p>
            <p class="order-item__price">¥{{ item.price.toFixed(2) }} × {{ item.qty }}</p>
          </div>
          <div class="order-item__subtotal">¥{{ (item.price * item.qty).toFixed(2) }}</div>
        </div>
      </div>
      <div class="order-summary">
        <span>共 {{ cartStore.totalQty }} 件商品</span>
        <span class="summary-total">合计 <strong>¥{{ cartStore.totalAmount.toFixed(2) }}</strong></span>
      </div>
    </div>

    <!-- 收货信息表单 -->
    <div class="section-card">
      <div class="section-title">📋 收货信息</div>
      <div class="form">
        <div class="form-field" :class="{ error: errors.customerName }">
          <label class="form-label">姓名</label>
          <input
            id="field-name"
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
            id="field-phone"
            v-model="form.phone"
            class="form-input"
            type="tel"
            maxlength="11"
            placeholder="请输入手机号"
            @input="clearError('phone')"
          />
          <p v-if="errors.phone" class="form-error">{{ errors.phone }}</p>
        </div>

        <div class="form-field" :class="{ error: errors.address }">
          <label class="form-label">配送地址</label>
          <textarea
            id="field-address"
            v-model="form.address"
            class="form-textarea"
            rows="3"
            placeholder="请输入详细配送地址（街道、门牌号等）"
            @input="clearError('address')"
          ></textarea>
          <p v-if="errors.address" class="form-error">{{ errors.address }}</p>
        </div>
      </div>
    </div>

    <!-- 费用明细 -->
    <div class="section-card price-card">
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

    <!-- 提交按钮 -->
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

const form = reactive({
  customerName: '',
  phone: '',
  address: '',
})

const errors = reactive({
  customerName: '',
  phone: '',
  address: '',
})

function clearError(field) {
  errors[field] = ''
}

function validate() {
  let valid = true

  if (!form.customerName.trim()) {
    errors.customerName = '请输入姓名'
    valid = false
  } else if (form.customerName.trim().length > 50) {
    errors.customerName = '姓名不能超过 50 个字符'
    valid = false
  }

  const phoneReg = /^1[3-9]\d{9}$/
  if (!form.phone) {
    errors.phone = '请输入手机号'
    valid = false
  } else if (!phoneReg.test(form.phone)) {
    errors.phone = '请输入正确的 11 位手机号'
    valid = false
  }

  if (!form.address.trim()) {
    errors.address = '请输入配送地址'
    valid = false
  } else if (form.address.trim().length > 200) {
    errors.address = '地址不能超过 200 个字符'
    valid = false
  }

  return valid
}

async function submitOrder() {
  if (!validate()) return
  if (cartStore.isEmpty) return

  submitting.value = true
  try {
    const payload = {
      customerName: form.customerName.trim(),
      phone: form.phone,
      address: form.address.trim(),
      totalAmount: cartStore.totalAmount,
      items: cartStore.items.map((i) => ({
        productId: i.id,
        productName: i.name,
        price: i.price,
        qty: i.qty,
      })),
    }

    const res = await createMobileOrder(payload)

    if (res?.code === 1) {
      const orderNo = res.data?.orderNo || ''
      cartStore.clear()
      router.replace({ name: 'mobileSuccess', query: { orderNo } })
    } else {
      alert(res?.msg || '下单失败，请稍后重试')
    }
  } catch (e) {
    alert('网络异常，请检查网络后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.checkout-page {
  padding: 0 0 120px;
}

/* ── 顶部导航 ── */
.page-nav {
  display: flex;
  align-items: center;
  padding: 18px 16px 14px;
  gap: 12px;
  position: relative;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(232, 99, 122, 0.15);
  border-radius: 999px;
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #6b5b63;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 2px 8px rgba(45, 35, 39, 0.02);
  backdrop-filter: blur(4px);
}

.back-btn:active { 
  transform: scale(0.95); 
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(232, 99, 122, 0.3);
}

.back-arrow { font-size: 15px; }

.page-title {
  font-size: 16px;
  font-weight: 800;
  color: #2d2327;
  margin: 0;
  letter-spacing: 0.02em;
}

/* ── 卡片区块 ── */
.section-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  margin: 0 16px 16px;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 8px 30px rgba(45, 35, 39, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.section-title {
  font-size: 14px;
  font-weight: 800;
  color: #2d2327;
  margin-bottom: 16px;
  letter-spacing: 0.03em;
}

/* ── 订单明细 ── */
.order-items {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 16px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-item__icon { 
  font-size: 28px; 
  flex-shrink: 0; 
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.05));
}

.order-item__info { flex: 1; }
.order-item__name { font-size: 13px; font-weight: 700; color: #2d2327; margin: 0 0 3px; }
.order-item__price { font-size: 12px; color: #a3949b; margin: 0; font-weight: 500; }

.order-item__subtotal {
  font-size: 14px;
  font-weight: 700;
  color: #e8637a;
}

.order-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px dashed rgba(232, 99, 122, 0.1);
  font-size: 13px;
  color: #a3949b;
  font-weight: 500;
}

.summary-total strong {
  color: #e8637a;
  font-size: 17px;
  font-weight: 800;
  font-family: var(--font-body);
}

/* ── 表单 ── */
.form { display: flex; flex-direction: column; gap: 16px; }

.form-field { display: flex; flex-direction: column; gap: 6px; }

.form-label {
  font-size: 12px;
  font-weight: 700;
  color: #6b5b63;
  letter-spacing: 0.04em;
  padding-left: 2px;
}

.form-input, .form-textarea {
  padding: 12px 16px;
  border: 1px solid rgba(232, 99, 122, 0.15);
  border-radius: 14px;
  font-size: 14px;
  color: #2d2327;
  background: rgba(255, 255, 255, 0.4);
  outline: none;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: inherit;
  resize: none;
}

.form-input:focus, .form-textarea:focus {
  border-color: #e8637a;
  background: #ffffff;
  box-shadow: 
    0 8px 20px rgba(232, 99, 122, 0.05),
    0 0 0 3.5px rgba(232, 99, 122, 0.08);
}

.form-input::placeholder, .form-textarea::placeholder { color: #c0b0b8; }

.form-field.error .form-input,
.form-field.error .form-textarea {
  border-color: rgba(248, 113, 113, 0.4);
  background: rgba(254, 242, 242, 0.6);
}

.form-field.error .form-input:focus,
.form-field.error .form-textarea:focus {
  border-color: #f87171;
  background: #ffffff;
  box-shadow: 
    0 8px 20px rgba(248, 113, 113, 0.05),
    0 0 0 3.5px rgba(248, 113, 113, 0.1);
}

.form-error {
  font-size: 11px;
  color: #f87171;
  margin: 0;
  font-weight: 500;
  padding-left: 2px;
}

/* ── 费用明细 ── */
.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #6b5b63;
  padding: 8px 0;
  font-weight: 500;
}

.price-free { color: #22c55e; font-weight: 700; }

.price-row--total {
  border-top: 1px dashed rgba(232, 99, 122, 0.1);
  margin-top: 6px;
  padding-top: 14px;
  font-weight: 700;
  color: #2d2327;
}

.price-final {
  font-size: 22px;
  font-weight: 800;
  color: #e8637a;
  font-family: var(--font-body);
}

/* ── 提交按钮 ── */
.submit-area {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 430px;
  padding: 16px 20px calc(16px + env(safe-area-inset-bottom, 0px));
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(28px) saturate(210%);
  -webkit-backdrop-filter: blur(28px) saturate(210%);
  border-top: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow: 
    inset 0 1px 0 0 rgba(255, 255, 255, 0.5),
    0 -10px 30px rgba(45, 35, 39, 0.03);
  z-index: 10;
}

.submit-btn {
  width: 100%;
  padding: 15px;
  background: linear-gradient(135deg, #e8637a 0%, #c94d63 100%);
  color: #fff;
  border: none;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 
    0 8px 24px rgba(232, 99, 122, 0.35),
    inset 0 1px 0 0 rgba(255, 255, 255, 0.2);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  letter-spacing: 0.03em;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 52px;
  position: relative;
  overflow: hidden;
}

/* 流光闪烁效果 (Shimmer Effect) */
.submit-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -150%;
  width: 50%;
  height: 100%;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0) 0%,
    rgba(255, 255, 255, 0.25) 50%,
    rgba(255, 255, 255, 0) 100%
  );
  transform: skewX(-20deg);
  animation: shimmer-btn 6s infinite ease-in-out;
}

@keyframes shimmer-btn {
  0% { left: -150%; }
  30% { left: 150%; }
  100% { left: 150%; }
}

.submit-btn:active:not(:disabled) { 
  transform: scale(0.97); 
  box-shadow: 0 4px 12px rgba(232, 99, 122, 0.25);
}

.submit-btn:disabled { 
  opacity: 0.5; 
  cursor: not-allowed; 
  box-shadow: none;
}

/* 加载点动画 */
.loading-dots {
  display: flex;
  gap: 6px;
  align-items: center;
}

.loading-dots span {
  width: 7px;
  height: 7px;
  background: rgba(255,255,255,0.8);
  border-radius: 50%;
  animation: bounce 1.2s infinite;
}

.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-6px); }
}
</style>
