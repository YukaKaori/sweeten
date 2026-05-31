<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  const crumbs = [{ path: '/', title: '首页' }]
  matched.forEach(item => {
    if (item.meta.title !== '首页') {
      crumbs.push({ path: item.path, title: item.meta.title })
    }
  })
  return crumbs
})

const handleClick = (crumb) => {
  router.push(crumb.path)
}
</script>

<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item
      v-for="(crumb, index) in breadcrumbs"
      :key="crumb.path"
    >
      <span
        v-if="index < breadcrumbs.length - 1"
        class="breadcrumb-link"
        @click="handleClick(crumb)"
      >{{ crumb.title }}</span>
      <span v-else class="breadcrumb-current">{{ crumb.title }}</span>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<style scoped>
.breadcrumb-link {
  cursor: pointer;
  color: var(--color-text-muted);
  transition: color var(--transition-fast);
}

.breadcrumb-link:hover {
  color: var(--color-primary);
}

.breadcrumb-current {
  color: var(--color-text-primary);
  font-weight: 500;
}
</style>
