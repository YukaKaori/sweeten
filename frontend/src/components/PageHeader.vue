<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' }
})
</script>

<template>
  <div class="page-header animate-fade-in-up">
    <div class="page-header-left">
      <h1 class="page-title">{{ title }}</h1>
      <p v-if="description" class="page-desc">{{ description }}</p>
    </div>
    <div class="page-header-right">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-header-left {
  flex: 1;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 4px;
  line-height: 1.3;
  /* View Transitions 共享元素：换页时新旧页标题平滑形变。
     每页至多一个 PageHeader，名称全页面唯一 */
  view-transition-name: page-title;
}

.page-desc {
  font-size: 14px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
</style>
