<script setup>
/**
 * 订单状态徽标 —— 顾客端与工作台共用同一套状态色（见 utils/constants.js ORDER_STATUS.tone）
 * 也可直接传 tone + label 作通用状态徽标（如 在售 / 已下架 / 低库存）。
 */
import { computed } from 'vue'
import { ORDER_STATUS } from '@/utils/constants'

const props = defineProps({
  /** 订单状态值（0–5） */
  status: { type: Number, default: null },
  tone: { type: String, default: '' },
  label: { type: String, default: '' },
  size: { type: String, default: 'md' },
})

const meta = computed(() => ORDER_STATUS.find((s) => s.value === props.status))
const text = computed(() => props.label || meta.value?.label || (props.status == null ? '查询中' : '未知'))
const toneClass = computed(() => props.tone || meta.value?.tone || 'neutral')
</script>

<template>
  <span
    class="status-badge"
    :class="[`is-${toneClass}`, `is-${size}`]"
  >
    <span
      class="status-badge__dot"
      aria-hidden="true"
    ></span>
    {{ text }}
  </span>
</template>

<style scoped>
.status-badge {
  --tone: var(--sw-neutral);
  --tone-bg: var(--sw-neutral-bg);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 24px;
  padding: 0 10px;
  border-radius: var(--sw-r-pill);
  background: var(--tone-bg);
  color: var(--tone);
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.status-badge.is-sm {
  height: 20px;
  padding: 0 8px;
  font-size: 11px;
}

.status-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

.is-warning { --tone: var(--sw-warning); --tone-bg: var(--sw-warning-bg); }
.is-success { --tone: var(--sw-success); --tone-bg: var(--sw-success-bg); }
.is-danger { --tone: var(--sw-danger); --tone-bg: var(--sw-danger-bg); }
.is-info { --tone: var(--sw-info); --tone-bg: var(--sw-info-bg); }
.is-brand { --tone: var(--sw-accent); --tone-bg: var(--sw-accent-soft); }
.is-muted { --tone: var(--sw-ink-3); --tone-bg: var(--sw-fill-soft); }
.is-muted .status-badge__dot { background: transparent; box-shadow: inset 0 0 0 1.5px currentColor; }
</style>
