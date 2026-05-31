<script setup>
defineProps({
  icon: { type: String, default: 'Document' },
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' }
})

defineEmits(['action'])
</script>

<template>
  <div class="empty-state">
    <div class="empty-icon">
      <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
        <circle cx="32" cy="32" r="28" stroke="var(--color-border)" stroke-width="2" fill="none"/>
        <path d="M24 28h16M24 36h10" stroke="var(--color-text-muted)" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </div>
    <p class="empty-title">{{ title }}</p>
    <p v-if="description" class="empty-desc">{{ description }}</p>
    <div v-if="$slots.action" class="empty-action">
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
  padding: 60px 20px;
}

.empty-icon {
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.empty-desc {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 16px;
}

.empty-action {
  margin-top: 4px;
}
</style>
