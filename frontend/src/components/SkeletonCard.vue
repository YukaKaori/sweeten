<script setup>
defineProps({
  count: { type: Number, default: 4 },
  columns: { type: Number, default: 4 }
})
</script>

<template>
  <div class="skeleton-grid" :style="{ gridTemplateColumns: `repeat(${columns}, 1fr)` }">
    <div
      v-for="i in count"
      :key="i"
      class="skeleton-card"
      :style="{ animationDelay: `${i * 0.08}s` }"
    >
      <div class="skeleton-card__image"></div>
      <div class="skeleton-card__body">
        <div class="skeleton-bar skeleton-bar--title"></div>
        <div class="skeleton-bar skeleton-bar--text"></div>
        <div class="skeleton-bar skeleton-bar--text short"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-grid {
  display: grid;
  gap: 20px;
}

.skeleton-card {
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  animation: skeletonFadeIn 0.5s ease both;
}

.skeleton-card__image {
  height: 140px;
  background: linear-gradient(
    90deg,
    var(--skeleton-base) 25%,
    var(--skeleton-sheen) 50%,
    var(--skeleton-base) 75%
  );
  background-size: 200% 100%;
  animation: skeletonShimmer 1.5s ease-in-out infinite;
}

.skeleton-card__body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
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

.skeleton-bar--title {
  height: 18px;
  width: 70%;
}

.skeleton-bar--text {
  width: 90%;
}

.skeleton-bar--text.short {
  width: 50%;
}

@keyframes skeletonShimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@keyframes skeletonFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
