<script setup>
/**
 * 空状态 / 加载失败状态：品牌标志线稿 + 一句说明 + 可选操作
 */
defineProps({
  icon: { type: String, default: '' },
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' },
  compact: { type: Boolean, default: false },
})
</script>

<template>
  <div
    class="empty-state"
    :class="{ 'is-compact': compact }"
  >
    <svg
      class="empty-state__art"
      viewBox="0 0 64 64"
      aria-hidden="true"
    >
      <circle
        cx="32"
        cy="32"
        r="30"
        class="plate"
      />
      <path
        d="M17 34c4.5 10.5 25.5 10.5 30 0"
        class="smile"
      />
      <circle
        cx="32"
        cy="22.5"
        r="6"
        class="cherry"
      />
      <path
        d="M32.4 16.6c.6-3 2.6-5.2 5.8-5.9"
        class="stem"
      />
    </svg>
    <p class="empty-state__title">{{ title }}</p>
    <p
      v-if="description"
      class="empty-state__desc"
    >{{ description }}</p>
    <div
      v-if="$slots.action"
      class="empty-state__action"
    >
      <slot name="action" />
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 20px;
  text-align: center;
}

.empty-state.is-compact {
  padding: 28px 16px;
}

.empty-state__art {
  width: 72px;
  height: 72px;
  margin-bottom: 16px;
}

.is-compact .empty-state__art {
  width: 52px;
  height: 52px;
  margin-bottom: 10px;
}

.plate {
  fill: var(--sw-fill-soft);
  stroke: var(--sw-border);
  stroke-width: 1;
}

.smile,
.stem {
  fill: none;
  stroke: var(--sw-ink-4);
  stroke-width: 2.6;
  stroke-linecap: round;
}

.stem {
  stroke-width: 1.6;
}

.cherry {
  fill: var(--sw-blush);
}

.empty-state__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--sw-ink);
}

.empty-state__desc {
  max-width: 320px;
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--sw-ink-3);
}

.empty-state__action {
  margin-top: 18px;
}
</style>
