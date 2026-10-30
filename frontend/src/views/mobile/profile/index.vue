<script setup>
/**
 * 喜甜会员中心
 * H5 没有登录体系：会员信息按「本机最近一次下单的手机号」查询（GET /mobile/member/profile）。
 * 积分兑换优惠券走 POST /mobile/member/redeem（后端校验会员 ID 与手机号匹配、积分是否足够）。
 */
import { ref, computed, onMounted } from 'vue'
import { getMemberProfile, getExchangeList, redeemCoupon } from '@/api/modules/member'
import { useStoreUi } from '@/stores/modules/storeUi'
import { BRAND, STORE_COPY } from '@/config/brand'
import { formatCount, formatShortDateTime } from '@/utils/format'
import BrandLogo from '@/components/brand/BrandLogo.vue'
import SwIcon from '@/components/brand/SwIcon.vue'

const ui = useStoreUi()

const phone = ref('')
const loading = ref(true)
const loadFailed = ref(false)
const profile = ref({ exists: false })
const exchangeList = ref([])
const confirmingId = ref(null)
const redeemingId = ref(null)

const member = computed(() => profile.value.member || {})
const coupons = computed(() => profile.value.coupons || [])
const pointsLog = computed(() => profile.value.recentPointsLog || [])
const points = computed(() => member.value.points || 0)
const isTopLevel = computed(() => (member.value.level || 0) >= 3)
const growthPercent = computed(() => Math.min(Math.max(profile.value.growthProgress || 0, 0), 100))
const growthNeeded = computed(() => Math.max((profile.value.nextLevelGrowth || 0) - (member.value.growth || 0), 0))
const maskedPhone = computed(() => (phone.value ? `${phone.value.slice(0, 3)} **** ${phone.value.slice(-4)}` : ''))

/** 折扣券 discount 为折后比例：0.9 → 9 折、0.88 → 8.8 折 */
const formatDiscount = (d) => `${Number((Number(d) * 10).toFixed(1))}`
const formatYuan = (v) => Number(v || 0).toFixed(0)

function couponValue(c) {
  return c.couponType === 2 ? { num: formatDiscount(c.discount), unit: '折', prefix: '' } : { num: formatYuan(c.reduceAmount), unit: '', prefix: '¥' }
}

function couponRule(threshold) {
  return Number(threshold) > 0 ? `满 ¥${formatYuan(threshold)} 可用` : '无门槛'
}

function expireText(dateStr) {
  if (!dateStr) return ''
  const d = new Date(String(dateStr).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return ''
  const days = Math.ceil((d - Date.now()) / 86400000)
  return days <= 0 ? '已过期' : `${d.getMonth() + 1}月${d.getDate()}日前有效 · 剩 ${days} 天`
}

const LOG_ICONS = { 1: 'bag', 2: 'gift', 3: 'sparkle', 4: 'tag', 5: 'clock', 6: 'edit' }

async function loadProfile() {
  if (!phone.value) return
  loading.value = true
  loadFailed.value = false
  try {
    const res = await getMemberProfile(phone.value)
    if (res?.code === 1) profile.value = res.data || { exists: false }
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

async function loadExchange() {
  try {
    const res = await getExchangeList()
    // 只有消耗积分的券才能兑换（后端对 pointsCost <= 0 的券返回「该券不支持积分兑换」；新人券、等级券为自动发放）
    if (res?.code === 1) exchangeList.value = (res.data || []).filter((t) => (t.pointsCost || 0) > 0)
  } catch {
    exchangeList.value = []
  }
}

async function redeem(t) {
  if (!profile.value.exists || redeemingId.value) return
  if (points.value < (t.pointsCost || 0)) {
    ui.showToast('积分不足', 'warning')
    return
  }
  // 积分兑换不可撤销：先确认一次
  if (confirmingId.value !== t.id) {
    confirmingId.value = t.id
    return
  }
  redeemingId.value = t.id
  try {
    const res = await redeemCoupon(member.value.id, member.value.phone, t.id)
    if (res?.code === 1) {
      ui.showToast(`已兑换「${t.name}」`, 'success')
      await loadProfile()
    } else {
      ui.showToast(res?.msg || '兑换失败', 'warning')
    }
  } catch (e) {
    ui.showToast(e?.displayMessage || '网络异常，请稍后重试', 'warning')
  } finally {
    redeemingId.value = null
    confirmingId.value = null
  }
}

onMounted(async () => {
  phone.value = localStorage.getItem('last_order_phone') || ''
  if (!phone.value) loading.value = false
  await Promise.all([loadProfile(), loadExchange()])
})
</script>

<template>
  <div class="member store-page">
    <header class="member__head">
      <h1 class="member__title sw-display">{{ STORE_COPY.titles.member }}</h1>
      <p
        v-if="maskedPhone"
        class="member__sub"
      >按本机最近一次下单的手机号 <span class="sw-num">{{ maskedPhone }}</span> 显示</p>
    </header>

    <!-- 加载中 -->
    <div
      v-if="loading"
      class="mcard is-loading"
      aria-busy="true"
    ><span class="skeleton-shimmer"></span></div>

    <!-- 会员卡 -->
    <section
      v-else-if="profile.exists"
      class="mcard"
      aria-label="会员卡"
    >
      <div class="mcard__top">
        <BrandLogo
          variant="horizontal"
          tone="inverse"
          :size="22"
        />
        <span class="mcard__level">{{ profile.levelName }}</span>
      </div>
      <p class="mcard__name">{{ member.name }}</p>
      <div class="mcard__points">
        <span class="mcard__points-num sw-num">{{ formatCount(points) }}</span>
        <span class="mcard__points-label">可用积分</span>
      </div>
      <div class="mcard__growth">
        <div
          class="mcard__bar"
          role="progressbar"
          :aria-valuenow="growthPercent"
          aria-valuemin="0"
          aria-valuemax="100"
          aria-label="会员成长进度"
        >
          <span :style="{ width: `${growthPercent}%` }"></span>
        </div>
        <p class="mcard__growth-text">
          <template v-if="isTopLevel">已是最高等级，感谢一路相伴</template>
          <template v-else>成长值 {{ formatCount(member.growth) }} · 再积累 {{ formatCount(growthNeeded) }} 升级</template>
        </p>
      </div>
    </section>

    <!-- 非会员 / 失败 -->
    <section
      v-else
      class="guest"
    >
      <BrandLogo
        variant="mark"
        :size="44"
      />
      <h2 class="guest__title">{{ loadFailed ? '会员信息暂时无法加载' : `成为${BRAND.nameCn}会员` }}</h2>
      <p
        v-if="loadFailed"
        class="guest__desc"
      >请检查网络后刷新页面。</p>
      <template v-else>
        <p class="guest__desc">在喜甜下单即自动成为会员，无需另外注册。</p>
        <ul class="guest__list">
          <li><SwIcon
            name="star"
            :size="17"
          />订单完成后，按实付金额累积积分</li>
          <li><SwIcon
            name="tag"
            :size="17"
          />积分可兑换满减券、折扣券</li>
          <li><SwIcon
            name="sparkle"
            :size="17"
          />累积成长值，升级银卡、金卡、钻石会员</li>
        </ul>
        <router-link
          :to="{ path: '/m', hash: '#menu' }"
          class="btn btn--primary btn--sm"
        >去挑选甜品</router-link>
      </template>
    </section>

    <!-- 会员统计 -->
    <dl
      v-if="profile.exists"
      class="stats"
    >
      <div><dt>累计消费</dt><dd class="sw-num">¥{{ formatCount(Math.round(member.totalSpent || 0)) }}</dd></div>
      <div><dt>累计订单</dt><dd class="sw-num">{{ formatCount(member.orderCount) }}</dd></div>
      <div><dt>可用优惠券</dt><dd class="sw-num">{{ coupons.length }}</dd></div>
    </dl>

    <!-- 优惠券 -->
    <section
      v-if="profile.exists && coupons.length"
      class="block"
      aria-labelledby="coupon-title"
    >
      <h2
        id="coupon-title"
        class="block__title"
      >我的优惠券 <span>{{ coupons.length }} 张</span></h2>
      <ul class="coupons">
        <li
          v-for="c in coupons"
          :key="c.id"
          class="coupon"
        >
          <div class="coupon__value">
            <span class="coupon__num sw-num"><small v-if="couponValue(c).prefix">{{ couponValue(c).prefix }}</small>{{ couponValue(c).num }}<small>{{ couponValue(c).unit }}</small></span>
            <span class="coupon__rule">{{ couponRule(c.threshold) }}</span>
          </div>
          <div class="coupon__info">
            <p class="coupon__name">{{ c.couponName }}</p>
            <p class="coupon__exp">{{ expireText(c.expireAt) }}</p>
          </div>
        </li>
      </ul>
      <p class="block__note">目前在线下单暂不支持使用优惠券抵扣。</p>
    </section>

    <!-- 积分兑换 -->
    <section
      v-if="profile.exists && exchangeList.length"
      class="block"
      aria-labelledby="exchange-title"
    >
      <h2
        id="exchange-title"
        class="block__title"
      >积分兑换 <span>可用 {{ formatCount(points) }} 积分</span></h2>
      <ul class="exchange">
        <li
          v-for="t in exchangeList"
          :key="t.id"
          class="exchange__item"
          :class="{ 'is-short': points < (t.pointsCost || 0) }"
        >
          <div class="exchange__info">
            <p class="exchange__name">{{ t.name }}</p>
            <p class="exchange__meta">{{ couponRule(t.threshold) }} · 领取后 {{ t.validDays }} 天内有效</p>
          </div>
          <button
            type="button"
            class="btn btn--sm"
            :class="confirmingId === t.id ? 'btn--primary' : 'btn--soft'"
            :disabled="points < (t.pointsCost || 0) || redeemingId === t.id"
            @click="redeem(t)"
          >
            <template v-if="redeemingId === t.id">兑换中…</template>
            <template v-else-if="confirmingId === t.id">确认兑换</template>
            <template v-else>{{ t.pointsCost }} 积分</template>
          </button>
        </li>
      </ul>
    </section>

    <!-- 积分记录 -->
    <section
      v-if="profile.exists && pointsLog.length"
      class="block"
      aria-labelledby="log-title"
    >
      <h2
        id="log-title"
        class="block__title"
      >积分记录 <span>最近 {{ pointsLog.length }} 条</span></h2>
      <ul class="log">
        <li
          v-for="log in pointsLog"
          :key="log.id"
          class="log__item"
        >
          <span class="log__icon"><SwIcon
            :name="LOG_ICONS[log.type] || 'star'"
            :size="17"
          /></span>
          <span class="log__text">
            <span class="log__reason">{{ log.reason }}</span>
            <span class="log__time">{{ formatShortDateTime(log.createdAt) }}</span>
          </span>
          <span
            class="log__points sw-num"
            :class="log.points > 0 ? 'is-plus' : 'is-minus'"
          >{{ log.points > 0 ? '+' : '' }}{{ log.points }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.store-page {
  animation: fadeIn var(--sw-dur-3) var(--sw-ease) both;
}

.member {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px var(--store-pad, 16px) 0;
}

.member__head {
  margin-bottom: 20px;
}

.member__title {
  font-size: clamp(26px, 3vw, 32px);
  color: var(--sw-ink);
}

.member__sub {
  margin-top: 4px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

/* ---- 会员卡 ---- */
.mcard {
  position: relative;
  overflow: hidden;
  padding: 24px;
  border-radius: var(--sw-r-xl);
  background: var(--sw-cocoa);
  color: #fffefc;
  box-shadow: var(--sw-shadow-md);
}

.mcard::after {
  content: '';
  position: absolute;
  right: -40px;
  bottom: -60px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  border: 28px solid rgba(230, 164, 161, 0.14);
  pointer-events: none;
}

.mcard.is-loading {
  height: 200px;
  background: var(--sw-surface);
  border: 1px solid var(--sw-border-soft);
  box-shadow: none;
}

.mcard.is-loading::after {
  display: none;
}

.mcard.is-loading .skeleton-shimmer {
  display: block;
  height: 100%;
  border-radius: var(--sw-r-md);
}

.mcard__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mcard__level {
  padding: 4px 12px;
  border-radius: var(--sw-r-pill);
  background: rgba(230, 164, 161, 0.2);
  color: var(--sw-blush);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.mcard__name {
  margin-top: 28px;
  font-size: 16px;
  color: rgba(255, 254, 252, 0.86);
}

.mcard__points {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-top: 4px;
}

.mcard__points-num {
  font-size: 40px;
  font-weight: 700;
  line-height: 1.1;
}

.mcard__points-label {
  font-size: 13px;
  color: rgba(255, 254, 252, 0.7);
}

.mcard__growth {
  position: relative;
  z-index: 1;
  margin-top: 18px;
}

.mcard__bar {
  height: 6px;
  overflow: hidden;
  border-radius: 3px;
  background: rgba(255, 254, 252, 0.16);
}

.mcard__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--sw-blush);
  transition: width 0.6s var(--sw-ease);
}

.mcard__growth-text {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(255, 254, 252, 0.72);
}

/* ---- 非会员 ---- */
.guest {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 36px 24px;
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-xl);
  background: var(--sw-surface);
  text-align: center;
}

.guest__title {
  margin-top: 16px;
  font-size: 20px;
  font-weight: 600;
}

.guest__desc {
  margin-top: 6px;
  font-size: 14px;
  color: var(--sw-ink-3);
}

.guest__list {
  list-style: none;
  margin: 20px 0 24px;
  text-align: left;
}

.guest__list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  font-size: 14px;
  color: var(--sw-ink-2);
}

.guest__list .sw-icon {
  color: var(--sw-berry);
}

/* ---- 统计 ---- */
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 16px;
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-lg);
  background: var(--sw-surface);
}

.stats div {
  padding: 16px 12px;
  text-align: center;
}

.stats div + div {
  border-left: 1px solid var(--sw-border-soft);
}

.stats dt {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.stats dd {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 700;
}

/* ---- 区块 ---- */
.block {
  margin-top: 28px;
}

.block__title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 17px;
  font-weight: 600;
}

.block__title span {
  font-size: 13px;
  font-weight: 400;
  color: var(--sw-ink-3);
}

.block__note {
  margin-top: 10px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.coupons {
  list-style: none;
  display: grid;
  gap: 10px;
}

.coupon {
  display: flex;
  overflow: hidden;
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-lg);
  background: var(--sw-surface);
}

.coupon__value {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 108px;
  padding: 14px 8px;
  border-right: 1px dashed var(--sw-milktea);
  background: var(--sw-blush-wash);
  flex-shrink: 0;
}

.coupon__num {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--sw-berry);
}

.coupon__num small {
  font-size: 13px;
  font-weight: 600;
}

.coupon__rule {
  margin-top: 4px;
  font-size: 11px;
  color: var(--sw-ink-3);
}

.coupon__info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 14px 16px;
  min-width: 0;
}

.coupon__name {
  font-size: 14px;
  font-weight: 600;
}

.coupon__exp {
  margin-top: 4px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.exchange {
  list-style: none;
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-lg);
  background: var(--sw-surface);
}

.exchange__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
}

.exchange__item + .exchange__item {
  border-top: 1px solid var(--sw-border-soft);
}

.exchange__item.is-short .exchange__name {
  color: var(--sw-ink-3);
}

.exchange__name {
  font-size: 14px;
  font-weight: 600;
}

.exchange__meta {
  margin-top: 2px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.log {
  list-style: none;
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-lg);
  background: var(--sw-surface);
}

.log__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}

.log__item + .log__item {
  border-top: 1px solid var(--sw-border-soft);
}

.log__icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--sw-fill-soft);
  color: var(--sw-ink-2);
  flex-shrink: 0;
}

.log__text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.log__reason {
  font-size: 14px;
}

.log__time {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.log__points {
  font-size: 15px;
  font-weight: 700;
}

.log__points.is-plus {
  color: var(--sw-success);
}

.log__points.is-minus {
  color: var(--sw-berry);
}

@media (max-width: 480px) {
  .mcard {
    padding: 20px;
  }

  .mcard__points-num {
    font-size: 34px;
  }

  .stats dd {
    font-size: 17px;
  }
}
</style>
