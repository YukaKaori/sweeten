<template>
  <div class="orders-page">
    <div class="page-head">
      <h2 class="page-title">我的订单</h2>
      <button v-if="orders.length" class="refresh-btn" :disabled="refreshing" @click="refreshAll">
        {{ refreshing ? '刷新中…' : '刷新状态' }}
      </button>
    </div>

    <div v-if="!orders.length" class="empty-wrap">
      <div class="empty-icon">🧾</div>
      <div class="empty-title">还没有订单</div>
      <div class="empty-desc">在本机下的订单会显示在这里，可随时查看制作进度</div>
      <button class="empty-btn" @click="router.push('/m/')">去挑选甜品</button>
    </div>

    <div v-else class="order-list">
      <article v-for="o in orders" :key="o.orderNo" class="order-card">
        <header class="oc-head">
          <span class="oc-no">订单 {{ o.orderNo }}</span>
          <span class="oc-status" :class="`s${o.status ?? 'x'}`">{{ statusLabel(o) }}</span>
        </header>
        <ul class="oc-items">
          <li v-for="(line, idx) in parseOrderItems(o.items)" :key="idx">
            <span>{{ line.name }}</span>
            <span class="qty">{{ line.qty ? `×${line.qty}` : '' }}</span>
          </li>
        </ul>
        <footer class="oc-foot">
          <span class="oc-time">{{ formatTime(o.createTime) }}</span>
          <span class="oc-amount">实付 <strong>¥{{ Number(o.amount || 0).toFixed(2) }}</strong></span>
        </footer>
        <p v-if="o.lookupError" class="oc-error">{{ o.lookupError }}</p>
      </article>
      <p class="list-hint">订单记录保存在本机浏览器；状态以门店处理为准</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMobileOrder } from '@/api/modules/mobile'
import { loadMyOrders } from '@/stores/modules/myOrders'
import { ORDER_STATUS, getLabelByValue, parseOrderItems } from '@/utils/constants'

const router = useRouter()
const orders = ref(loadMyOrders().map((o) => ({ ...o, status: o.status ?? null, lookupError: '' })))
const refreshing = ref(false)

function statusLabel(o) {
  if (o.status === null || o.status === undefined) return '查询中'
  return getLabelByValue(ORDER_STATUS, o.status)
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return String(t).replace('T', ' ').slice(0, 16)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}月${d.getDate()}日 ${pad(d.getHours())}:${pad(d.getMinutes())}`
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

onMounted(() => {
  if (orders.value.length) refreshAll()
})
</script>

<style scoped>
.orders-page { padding: 0 0 40px; }
.page-head { display: flex; align-items: center; justify-content: space-between; padding: 20px 18px 14px; }
.page-title { font-size: 22px; font-weight: 700; color: #1d1d1f; margin: 0; letter-spacing: -0.4px; }
.refresh-btn {
  border: none; border-radius: 999px; padding: 7px 14px;
  background: rgba(0,0,0,0.05); color: #1d1d1f; font-size: 13px; font-weight: 600; cursor: pointer;
}
.refresh-btn:disabled { opacity: 0.5; }

.order-list { padding: 0 18px; display: flex; flex-direction: column; gap: 12px; }
.order-card {
  background: rgba(255,255,255,0.78);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 0.5px solid rgba(255,255,255,0.5);
  border-radius: 20px; padding: 16px 18px;
  box-shadow: 0 4px 18px rgba(0,0,0,0.04);
}
.oc-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 10px; }
.oc-no { font-size: 12px; color: #86868b; font-variant-numeric: tabular-nums; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.oc-status { flex-shrink: 0; font-size: 12px; font-weight: 650; padding: 3px 10px; border-radius: 999px; background: rgba(0,0,0,0.05); color: #6e6e73; }
.oc-status.s1 { background: rgba(240,163,92,0.14); color: #b86b1f; }
.oc-status.s2, .oc-status.s3 { background: rgba(107,140,206,0.14); color: #3d5f9e; }
.oc-status.s4 { background: rgba(92,184,138,0.14); color: #2f7d55; }
.oc-status.s5 { background: rgba(0,0,0,0.05); color: #86868b; }
.oc-items { list-style: none; margin: 0 0 12px; padding: 0; }
.oc-items li { display: flex; justify-content: space-between; font-size: 14px; color: #1d1d1f; padding: 3px 0; }
.oc-items .qty { color: #86868b; }
.oc-foot { display: flex; justify-content: space-between; align-items: baseline; padding-top: 10px; border-top: 0.5px solid rgba(0,0,0,0.06); }
.oc-time { font-size: 12px; color: #86868b; }
.oc-amount { font-size: 13px; color: #6e6e73; }
.oc-amount strong { font-size: 16px; color: #1d1d1f; }
.oc-error { margin: 8px 0 0; font-size: 12px; color: #d9574a; }
.list-hint { text-align: center; font-size: 12px; color: #a1a1a6; margin: 8px 0 0; }

.empty-wrap { text-align: center; padding: 80px 24px; }
.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-title { font-size: 16px; font-weight: 650; color: #1d1d1f; margin-bottom: 6px; }
.empty-desc { font-size: 13px; color: #86868b; margin-bottom: 20px; }
.empty-btn { border: none; border-radius: 999px; padding: 10px 22px; background: #d9574a; color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; }
</style>
