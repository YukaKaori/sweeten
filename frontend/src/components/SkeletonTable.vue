<script setup>
defineProps({
  rows: { type: Number, default: 5 },
  columns: { type: Number, default: 4 },
  showHeader: { type: Boolean, default: true }
})
</script>

<template>
  <div class="skeleton-table">
    <!-- Header -->
    <div v-if="showHeader" class="skeleton-header">
      <div v-for="c in columns" :key="c" class="skeleton-cell skeleton-header-cell">
        <div class="skeleton-bar skeleton-bar--header"></div>
      </div>
    </div>
    <!-- Rows -->
    <div v-for="r in rows" :key="r" class="skeleton-row" :style="{ animationDelay: `${r * 0.06}s` }">
      <div v-for="c in columns" :key="c" class="skeleton-cell">
        <div class="skeleton-bar" :style="{ width: `${50 + Math.random() * 40}%` }"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-table {
  padding: 4px;
}

.skeleton-header {
  display: flex;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border-light);
}

.skeleton-header-cell {
  flex: 1;
}

.skeleton-row {
  display: flex;
  gap: 16px;
  padding: 16px;
  border-bottom: 1px solid var(--color-border-light);
  animation: skeletonFadeIn 0.4s ease both;
}

.skeleton-cell {
  flex: 1;
  display: flex;
  align-items: center;
}

.skeleton-bar {
  height: 14px;
  border-radius: var(--radius-sm);
  background: linear-gradient(
    90deg,
    var(--skeleton-base) 25%,
    var(--skeleton-sheen) 50%,
    var(--skeleton-base) 75%
  );
  background-size: 200% 100%;
  animation: skeletonShimmer 1.5s ease-in-out infinite;
}

.skeleton-bar--header {
  height: 12px;
  width: 60%;
  opacity: 0.6;
}

@keyframes skeletonShimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@keyframes skeletonFadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
