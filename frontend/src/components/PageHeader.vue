<script setup>
/**
 * 工作台页头：标题 + 一句说明 + 右侧操作区（#actions）。可选 #meta 放标题下的统计信息。
 */
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<template>
  <header class="page-header">
    <div class="page-header__text">
      <h1 class="page-header__title">{{ title }}</h1>
      <p
        v-if="description"
        class="page-header__desc"
      >{{ description }}</p>
      <div
        v-if="$slots.meta"
        class="page-header__meta"
      >
        <slot name="meta" />
      </div>
    </div>
    <div
      v-if="$slots.actions"
      class="page-header__actions"
    >
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px 24px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.page-header__text {
  min-width: 0;
}

.page-header__title {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.25;
  color: var(--sw-ink);
}

.page-header__desc {
  margin-top: 4px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.page-header__meta {
  margin-top: 10px;
}

.page-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 640px) {
  .page-header {
    align-items: flex-start;
  }

  .page-header__title {
    font-size: 20px;
  }
}
</style>
