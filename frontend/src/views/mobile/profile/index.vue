<template>
  <div class="profile-page">
    <!-- Member Card -->
    <div class="member-card" v-if="profile.exists">
      <!-- Level Banner -->
      <div class="level-banner" :class="levelClass">
        <div class="level-glow"></div>
        <div class="level-banner-content">
          <div class="member-avatar">{{ member.name?.charAt(0) || '😊' }}</div>
          <div class="member-meta">
            <h2 class="member-name">{{ member.name }}</h2>
            <span class="level-badge">{{ profile.levelName }}</span>
          </div>
        </div>
        <!-- Growth Progress -->
        <div class="growth-bar-wrap" v-if="profile.growthProgress < 100">
          <div class="growth-bar">
            <div class="growth-fill" :style="{ width: profile.growthProgress + '%' }"></div>
          </div>
          <span class="growth-text">距下一级还需 {{ nextGrowthNeeded }} 成长值</span>
        </div>
        <div class="growth-bar-wrap" v-else>
          <span class="growth-text max">🎉 已达最高等级</span>
        </div>
      </div>

      <!-- Points & Stats -->
      <div class="stats-row">
        <div class="stat-item">
          <span class="stat-value">{{ member.points || 0 }}</span>
          <span class="stat-label">当前积分</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">¥{{ formatAmount(member.totalSpent) }}</span>
          <span class="stat-label">累计消费</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">{{ member.orderCount || 0 }}</span>
          <span class="stat-label">累计订单</span>
        </div>
      </div>
    </div>

    <!-- Non-member card -->
    <div class="member-card guest-card" v-else>
      <div class="guest-inner">
        <div class="guest-icon">🍰</div>
        <h2>加入 DessertShop 会员</h2>
        <p>消费攒积分 · 升级享折扣 · 生日双倍积分</p>
        <p class="guest-hint">下单后自动成为会员</p>
      </div>
    </div>

    <!-- Coupon Section -->
    <div class="section" v-if="profile.exists && coupons.length">
      <div class="section-title">
        <span>🎫 我的优惠券</span>
        <span class="section-count">{{ coupons.length }}张</span>
      </div>
      <div class="coupon-list">
        <div
          v-for="c in coupons"
          :key="c.id"
          class="coupon-card"
          :class="couponClass(c.couponType)"
        >
          <div class="cc-left">
            <div class="cc-amount">
              <template v-if="c.couponType === 2">
                <span class="cc-num">{{ formatDiscount(c.discount) }}</span><span class="cc-unit">折</span>
              </template>
              <template v-else>
                <span class="cc-unit">¥</span><span class="cc-num">{{ c.reduceAmount }}</span>
              </template>
            </div>
            <div class="cc-condition" v-if="c.threshold > 0">满 ¥{{ c.threshold }} 可用</div>
            <div class="cc-condition" v-else>无门槛</div>
          </div>
          <div class="cc-right">
            <div class="cc-name">{{ c.couponName }}</div>
            <div class="cc-expire">{{ formatExpire(c.expireAt) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Points Exchange -->
    <div class="section" v-if="profile.exists">
      <div class="section-title">🎁 积分兑换</div>
      <div class="exchange-list">
        <div
          v-for="t in exchangeList"
          :key="t.id"
          class="exchange-item"
          :class="{ disabled: (member.points || 0) < (t.pointsCost || 0) }"
        >
          <div class="ex-info">
            <div class="ex-name">{{ t.name }}</div>
            <div class="ex-meta" v-if="t.threshold > 0">满 ¥{{ t.threshold }} 可用 · 有效期 {{ t.validDays }} 天</div>
            <div class="ex-meta" v-else>无门槛 · 有效期 {{ t.validDays }} 天</div>
          </div>
          <button
            class="ex-btn"
            :disabled="(member.points || 0) < (t.pointsCost || 0)"
            @click="doRedeem(t)"
          >
            <template v-if="t.pointsCost > 0">{{ t.pointsCost }} 积分</template>
            <template v-else>免费</template>
          </button>
        </div>
      </div>
    </div>

    <!-- Points History -->
    <div class="section" v-if="profile.exists && pointsLog.length">
      <div class="section-title">📋 积分记录</div>
      <div class="log-list">
        <div v-for="log in pointsLog" :key="log.id" class="log-item">
          <div class="log-left">
            <span class="log-icon">{{ typeIcon(log.type) }}</span>
            <div>
              <div class="log-reason">{{ log.reason }}</div>
              <div class="log-time">{{ formatTime(log.createdAt) }}</div>
            </div>
          </div>
          <span class="log-points" :class="log.points > 0 ? 'positive' : 'negative'">
            {{ log.points > 0 ? '+' : '' }}{{ log.points }}
          </span>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <transition name="toast-fade">
      <div v-if="toastMsg" class="toast-pop">{{ toastMsg }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMemberProfile, getExchangeList, redeemCoupon } from '@/api/modules/member'

const phone = ref('')
const profile = ref({ exists: false })
const member = computed(() => profile.value.member || {})
const coupons = computed(() => profile.value.coupons || [])
const pointsLog = computed(() => profile.value.recentPointsLog || [])
const exchangeList = ref([])
const toastMsg = ref('')

const levelClass = computed(() => {
  const map = { 0: 'bronze', 1: 'silver', 2: 'gold', 3: 'diamond' }
  return map[member.value.level] || 'bronze'
})

const nextGrowthNeeded = computed(() => {
  const g = member.value.growth || 0
  const thresholds = [1000, 3000, 10000]
  const level = member.value.level || 0
  if (level >= 3) return 0
  return thresholds[level] - g
})

function formatAmount(v) {
  if (!v) return '0'
  return Number(v).toFixed(0)
}

function formatDiscount(d) {
  if (!d) return ''
  return ((1 - d) * 10).toFixed(0)
}

function formatExpire(dateStr) {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    const now = new Date()
    const days = Math.ceil((d - now) / (1000 * 60 * 60 * 24))
    return days <= 0 ? '已过期' : `有效期至 ${d.getMonth()+1}/${d.getDate()} (剩${days}天)`
  } catch { return dateStr }
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  try { return new Date(dateStr).toLocaleString('zh-CN') } catch { return dateStr }
}

function typeIcon(type) {
  const map = { 1: '🛒', 2: '🎂', 3: '🎉', 4: '🎁', 5: '⏰', 6: '🔧' }
  return map[type] || '📌'
}

function couponClass(type) {
  const map = { 1: 'c-fixed', 2: 'c-discount', 3: 'c-cash' }
  return map[type] || ''
}

function showToast(msg) {
  toastMsg.value = msg
  setTimeout(() => toastMsg.value = '', 2000)
}

async function loadProfile(p) {
  try {
    const res = await getMemberProfile(p)
    if (res?.code === 1) profile.value = res.data
  } catch { /* ignore */ }
}

async function loadExchange() {
  try {
    const res = await getExchangeList()
    if (res?.code === 1) exchangeList.value = res.data || []
  } catch { /* ignore */ }
}

async function doRedeem(template) {
  if (!profile.value.exists) return
  if ((member.value.points || 0) < (template.pointsCost || 0)) {
    showToast('积分不足')
    return
  }
  try {
    const res = await redeemCoupon(member.value.id, template.id)
    if (res?.code === 1) {
      showToast('兑换成功！🎉')
      await loadProfile(phone.value)
    } else {
      showToast(res?.msg || '兑换失败')
    }
  } catch {
    showToast('网络异常')
  }
}

onMounted(async () => {
  // Try to get phone from last order's localStorage or prompt
  const cached = localStorage.getItem('last_order_phone') || ''
  if (cached) {
    phone.value = cached
    await loadProfile(cached)
  }
  await loadExchange()
})

// Expose for parent to trigger
defineExpose({ loadProfile })
</script>

<style scoped>
.profile-page { padding: 16px 18px 120px; display: flex; flex-direction: column; gap: 16px; }

/* ── Member Card ── */
.member-card {
  border-radius: 24px; overflow: hidden;
  background: rgba(255,255,255,0.75);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 0.5px solid rgba(255,255,255,0.55);
  box-shadow: 0 8px 28px rgba(0,0,0,0.04), inset 0 0.5px 0 rgba(255,255,255,0.6);
}

/* Level Banner */
.level-banner {
  position: relative; overflow: hidden; padding: 24px 20px 20px;
}
.level-glow {
  position: absolute; top: -50%; right: -20%; width: 200px; height: 200px;
  border-radius: 50%; filter: blur(40px); opacity: 0.3;
}
.level-banner.bronze { background: linear-gradient(145deg, #f5f0ec 0%, #e8ddd3 100%); }
.level-banner.bronze .level-glow { background: rgba(180,140,100,0.4); }
.level-banner.silver  { background: linear-gradient(145deg, #eef2f7 0%, #d4dce8 100%); }
.level-banner.silver  .level-glow { background: rgba(140,160,200,0.4); }
.level-banner.gold    { background: linear-gradient(145deg, #fdf6e8 0%, #f0dcb0 100%); }
.level-banner.gold    .level-glow { background: rgba(220,180,80,0.4); }
.level-banner.diamond { background: linear-gradient(145deg, #f0e8fc 0%, #d4c0f0 100%); }
.level-banner.diamond .level-glow { background: rgba(160,120,220,0.4); }

.level-banner-content {
  display: flex; align-items: center; gap: 14px; position: relative; z-index: 1;
}
.member-avatar {
  width: 52px; height: 52px; border-radius: 50%;
  background: rgba(255,255,255,0.7); border: 1px solid rgba(255,255,255,0.8);
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 650; flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0,0,0,0.04);
}
.member-name { font-size: 18px; font-weight: 650; color: #1d1d1f; margin: 0; letter-spacing: -0.2px; }
.level-badge {
  display: inline-block; margin-top: 4px;
  padding: 3px 10px; border-radius: 999px;
  font-size: 10px; font-weight: 650;
  background: rgba(255,255,255,0.6);
  color: rgba(0,0,0,0.55);
  letter-spacing: 0.04em;
}

/* Growth bar */
.growth-bar-wrap { margin-top: 16px; position: relative; z-index: 1; }
.growth-bar {
  height: 5px; border-radius: 3px;
  background: rgba(0,0,0,0.06); overflow: hidden;
}
.growth-fill {
  height: 100%; border-radius: 3px;
  background: linear-gradient(90deg, #d9574a, #e88a7e);
  transition: width 0.6s cubic-bezier(0.16,1,0.3,1);
}
.growth-text { font-size: 11px; color: rgba(0,0,0,0.4); margin-top: 6px; display: block; }
.growth-text.max { color: rgba(0,0,0,0.35); }

/* Stats */
.stats-row {
  display: flex; align-items: center; padding: 18px 20px;
  border-top: 0.5px solid rgba(0,0,0,0.04);
}
.stat-item { flex: 1; text-align: center; }
.stat-value { display: block; font-size: 20px; font-weight: 650; color: #1d1d1f; letter-spacing: -0.3px; }
.stat-label { font-size: 11px; color: #aeaeb2; margin-top: 3px; display: block; }
.stat-divider { width: 0.5px; height: 32px; background: rgba(0,0,0,0.06); }

/* Guest */
.guest-card { border-radius: 24px; }
.guest-inner { padding: 48px 24px; text-align: center; }
.guest-icon { font-size: 56px; margin-bottom: 16px; filter: drop-shadow(0 6px 12px rgba(217,87,74,0.12)); }
.guest-inner h2 { font-size: 18px; font-weight: 650; color: #1d1d1f; margin: 0 0 10px; letter-spacing: -0.2px; }
.guest-inner p { font-size: 13px; color: #aeaeb2; margin: 0 0 4px; }
.guest-hint { font-size: 12px; color: #d9574a; margin-top: 8px; }

/* ── Section ── */
.section {
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-radius: 22px; padding: 20px;
  border: 0.5px solid rgba(255,255,255,0.5);
  box-shadow: 0 4px 16px rgba(0,0,0,0.02), inset 0 0.5px 0 rgba(255,255,255,0.6);
}
.section-title { font-size: 15px; font-weight: 640; color: #1d1d1f; margin-bottom: 14px; display: flex; justify-content: space-between; align-items: center; letter-spacing: -0.15px; }
.section-count { font-size: 11px; font-weight: 500; color: #aeaeb2; }

/* ── Coupons ── */
.coupon-list { display: flex; flex-direction: column; gap: 10px; }
.coupon-card {
  display: flex; border-radius: 16px; overflow: hidden;
  border: 0.5px solid rgba(255,255,255,0.5);
}
.cc-left {
  width: 100px; flex-shrink: 0; display: flex; flex-direction: column;
  align-items: center; justify-content: center; padding: 14px 8px;
}
.c-fixed .cc-left { background: linear-gradient(145deg, #fff2ec, #fde8de); }
.c-discount .cc-left { background: linear-gradient(145deg, #f0ecfb, #e6def8); }
.c-cash .cc-left { background: linear-gradient(145deg, #ecf8f0, #daf0e4); }
.cc-num { font-size: 28px; font-weight: 700; color: #1d1d1f; letter-spacing: -1px; line-height: 1; }
.cc-unit { font-size: 14px; font-weight: 600; color: #1d1d1f; }
.cc-condition { font-size: 10px; color: #aeaeb2; margin-top: 4px; font-weight: 500; }
.cc-right {
  flex: 1; padding: 14px; display: flex; flex-direction: column; justify-content: center;
  background: rgba(255,255,255,0.5);
}
.cc-name { font-size: 14px; font-weight: 600; color: #1d1d1f; margin-bottom: 4px; }
.cc-expire { font-size: 11px; color: #aeaeb2; }

/* ── Exchange ── */
.exchange-list { display: flex; flex-direction: column; gap: 8px; }
.exchange-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px; border-radius: 16px;
  background: rgba(245,245,247,0.6);
  border: 0.5px solid rgba(0,0,0,0.04);
  transition: opacity 0.2s;
}
.exchange-item.disabled { opacity: 0.5; }
.ex-info { flex: 1; min-width: 0; }
.ex-name { font-size: 13px; font-weight: 600; color: #1d1d1f; margin-bottom: 3px; }
.ex-meta { font-size: 11px; color: #aeaeb2; }
.ex-btn {
  flex-shrink: 0; margin-left: 12px;
  padding: 8px 18px; border-radius: 999px; border: none;
  background: #d9574a; color: #fff;
  font-size: 12px; font-weight: 600; cursor: pointer;
  transition: all 0.2s;
}
.ex-btn:active:not(:disabled) { transform: scale(0.95); }
.ex-btn:disabled { background: #d1d1d6; cursor: not-allowed; }

/* ── Points Log ── */
.log-list { display: flex; flex-direction: column; gap: 10px; }
.log-item { display: flex; align-items: center; justify-content: space-between; }
.log-left { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.log-icon { font-size: 18px; flex-shrink: 0; }
.log-reason { font-size: 13px; color: #1d1d1f; font-weight: 500; }
.log-time  { font-size: 11px; color: #aeaeb2; margin-top: 1px; }
.log-points { font-size: 15px; font-weight: 650; flex-shrink: 0; }
.log-points.positive { color: #34c759; }
.log-points.negative { color: #d9574a; }

/* ── Toast ── */
.toast-pop {
  position: fixed; bottom: 120px; left: 50%; transform: translateX(-50%);
  background: rgba(29,29,31,0.88); backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  color: #fff; padding: 10px 24px; border-radius: 999px;
  font-size: 13px; font-weight: 500; z-index: 300;
  border: 0.5px solid rgba(255,255,255,0.1);
}
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s cubic-bezier(0.16,1,0.3,1); }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translateX(-50%) translateY(10px); }
</style>
