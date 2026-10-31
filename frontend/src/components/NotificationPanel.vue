<script setup>
/**
 * 页头通知：上架甜品的库存预警（后端 GET /dashboard/stock-alert，库存 < 20）。
 * 没有查看权限时不显示铃铛；点击某条可直接跳到甜品管理并定位该商品。
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { queryStockAlertApi } from '@/api/modules/dashboard'
import { useUserStore } from '@/stores/modules/user'
import SwIcon from '@/components/brand/SwIcon.vue'

const LOW_STOCK_THRESHOLD = 20 // 与后端 DashboardServiceImpl.selectLowStockAlerts(20) 一致
const SEEN_KEY = 'sw-stock-alerts-seen'

const router = useRouter()
const userStore = useUserStore()
const enabled = computed(() => userStore.can('dashboard:stats:read'))
const canOpenDessert = computed(() => userStore.can('dessert:dessert:list'))

const visible = ref(false)
const loading = ref(false)
const failed = ref(false)
const alerts = ref([])
const seen = ref(sessionStorage.getItem(SEEN_KEY) || '')

const signature = computed(() => alerts.value.map((a) => `${a.name}:${a.stock}`).join('|'))
const unread = computed(() => alerts.value.length > 0 && signature.value !== seen.value)

const load = async () => {
  if (!enabled.value) return
  loading.value = true
  failed.value = false
  try {
    const res = await queryStockAlertApi()
    alerts.value = res?.code && Array.isArray(res.data) ? res.data : []
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

const toggle = () => {
  visible.value = !visible.value
  if (visible.value) {
    load()
    seen.value = signature.value
    sessionStorage.setItem(SEEN_KEY, seen.value)
  }
}

const openItem = (item) => {
  if (!canOpenDessert.value) return
  visible.value = false
  router.push({ path: '/dessert', query: { name: item.name } })
}

const onDocClick = (e) => {
  if (!e.target.closest('.notify')) visible.value = false
}
const onKey = (e) => {
  if (e.key === 'Escape') visible.value = false
}

onMounted(() => {
  load()
  document.addEventListener('click', onDocClick)
  document.addEventListener('keydown', onKey)
})
onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKey)
})
</script>

<template>
  <div
    v-if="enabled"
    class="notify"
  >
    <button
      type="button"
      class="hdr-btn"
      :aria-label="unread ? `库存提醒（${alerts.length} 项）` : '库存提醒'"
      :aria-expanded="visible"
      @click.stop="toggle"
    >
      <SwIcon
        name="bell"
        :size="19"
      />
      <span
        v-if="unread"
        class="notify__dot"
        aria-hidden="true"
      ></span>
    </button>

    <Transition name="pop">
      <div
        v-if="visible"
        class="notify__panel"
        @click.stop
      >
        <header class="notify__head">
          <span class="notify__title">库存提醒</span>
          <span class="notify__hint">在售且库存低于 {{ LOW_STOCK_THRESHOLD }} 件</span>
        </header>

        <div
          v-if="loading && !alerts.length"
          class="notify__state"
        >正在加载…</div>
        <div
          v-else-if="failed"
          class="notify__state"
        >
          暂时无法获取提醒
          <button
            type="button"
            class="notify__retry"
            @click="load"
          >重试</button>
        </div>
        <div
          v-else-if="!alerts.length"
          class="notify__state"
        >所有在售甜品库存充足</div>

        <ul
          v-else
          class="notify__list"
        >
          <li
            v-for="item in alerts"
            :key="item.name"
          >
            <button
              type="button"
              class="notify__item"
              :disabled="!canOpenDessert"
              @click="openItem(item)"
            >
              <span
                class="notify__badge"
                :class="{ 'is-out': item.stock <= 0, 'is-low': item.stock > 0 && item.stock < 10 }"
              >
                {{ item.stock <= 0 ? '售罄' : item.stock }}
              </span>
              <span class="notify__text">
                <span class="notify__name">{{ item.name }}</span>
                <span class="notify__meta">{{ item.stock <= 0 ? '已售罄，顾客端显示为不可购买' : `剩余 ${item.stock} 件，建议补货` }}</span>
              </span>
              <SwIcon
                v-if="canOpenDessert"
                name="chevron-right"
                :size="16"
                class="notify__go"
              />
            </button>
          </li>
        </ul>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.notify {
  position: relative;
}

.notify__dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--sw-accent);
  box-shadow: 0 0 0 2px var(--sw-bg);
}

.notify__panel {
  position: absolute;
  top: calc(100% + 10px);
  right: -6px;
  width: min(340px, calc(100vw - 24px));
  overflow: hidden;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-lg);
  background: var(--sw-surface);
  box-shadow: var(--sw-shadow-lg);
  z-index: var(--z-dropdown);
}

.notify__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px 10px;
  border-bottom: 1px solid var(--sw-border-soft);
}

.notify__title {
  font-size: 14px;
  font-weight: 600;
}

.notify__hint {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.notify__state {
  padding: 28px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.notify__retry {
  margin-left: 6px;
  border: 0;
  background: none;
  color: var(--sw-accent);
  font-weight: 600;
  cursor: pointer;
}

.notify__list {
  list-style: none;
  max-height: 360px;
  overflow-y: auto;
  padding: 6px;
}

.notify__item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px;
  border: 0;
  border-radius: var(--sw-r-md);
  background: transparent;
  text-align: left;
  color: var(--sw-ink);
  cursor: pointer;
}

.notify__item:hover:not(:disabled) {
  background: var(--sw-fill-soft);
}

.notify__item:disabled {
  cursor: default;
}

.notify__badge {
  display: grid;
  place-items: center;
  min-width: 40px;
  height: 28px;
  padding: 0 8px;
  border-radius: var(--sw-r-sm);
  background: var(--sw-warning-bg);
  color: var(--sw-warning);
  font-size: 12px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.notify__badge.is-low,
.notify__badge.is-out {
  background: var(--sw-danger-bg);
  color: var(--sw-danger);
}

.notify__text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.notify__name {
  font-size: 13px;
  font-weight: 600;
}

.notify__meta {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.notify__go {
  color: var(--sw-ink-4);
}

.pop-enter-active,
.pop-leave-active {
  transition: opacity var(--sw-dur-2) var(--sw-ease), transform var(--sw-dur-2) var(--sw-ease);
}

.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
