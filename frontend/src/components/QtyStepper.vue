<script setup>
/**
 * 数量步进器（顾客端）：44px 触控目标、键盘可用、到上限时禁用并说明原因
 * v-model 绑定数量；或不用 v-model，监听 increase / decrease 事件直接操作购物车。
 */
import { computed } from 'vue'
import SwIcon from '@/components/brand/SwIcon.vue'

const props = defineProps({
  modelValue: { type: Number, default: 1 },
  min: { type: Number, default: 1 },
  max: { type: Number, default: Infinity },
  size: { type: String, default: 'md' }, // sm | md
  label: { type: String, default: '数量' },
  /** 为 true 时减到 min 以下也允许（购物车里减到 0 = 移除） */
  allowRemove: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'increase', 'decrease'])

const canDecrease = computed(() => !props.disabled && (props.allowRemove ? props.modelValue > 0 : props.modelValue > props.min))
const canIncrease = computed(() => !props.disabled && props.modelValue < props.max)

function dec() {
  if (!canDecrease.value) return
  emit('decrease')
  emit('update:modelValue', props.modelValue - 1)
}

function inc() {
  if (!canIncrease.value) return
  emit('increase')
  emit('update:modelValue', props.modelValue + 1)
}
</script>

<template>
  <div
    class="qty"
    :class="[`qty--${size}`, { 'is-disabled': disabled }]"
    role="group"
    :aria-label="label"
  >
    <button
      type="button"
      class="qty__btn"
      :disabled="!canDecrease"
      :aria-label="allowRemove && modelValue <= 1 ? '移除' : '减少一件'"
      @click.stop="dec"
    >
      <SwIcon
        :name="allowRemove && modelValue <= 1 ? 'trash' : 'minus'"
        :size="size === 'sm' ? 15 : 17"
      />
    </button>
    <span
      class="qty__value sw-num"
      aria-live="polite"
    >{{ modelValue }}</span>
    <button
      type="button"
      class="qty__btn"
      :disabled="!canIncrease"
      :aria-label="canIncrease ? '增加一件' : '已达库存上限'"
      :title="canIncrease ? undefined : '已达库存上限'"
      @click.stop="inc"
    >
      <SwIcon
        name="plus"
        :size="size === 'sm' ? 15 : 17"
      />
    </button>
  </div>
</template>

<style scoped>
.qty {
  display: inline-flex;
  align-items: center;
  height: 44px;
  padding: 0 2px;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-pill);
  background: var(--sw-surface);
  color: var(--sw-ink);
}

.qty__btn {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: inherit;
  cursor: pointer;
  transition: background-color var(--sw-dur-1) var(--sw-ease), color var(--sw-dur-1) var(--sw-ease);
}

.qty__btn:hover:not(:disabled) {
  background: var(--sw-fill-soft);
}

.qty__btn:disabled {
  color: var(--sw-ink-4);
  opacity: 0.55;
  cursor: not-allowed;
}

.qty__value {
  min-width: 28px;
  text-align: center;
  font-size: 15px;
  font-weight: 600;
}

/* 紧凑款：卡片与购物车行内，视觉 32px，触控区仍 ≥ 40px */
.qty--sm {
  height: 36px;
}

.qty--sm .qty__btn {
  width: 34px;
  height: 34px;
  position: relative;
}

.qty--sm .qty__btn::after {
  content: '';
  position: absolute;
  inset: -4px;
}

.qty--sm .qty__value {
  min-width: 22px;
  font-size: 14px;
}
</style>
