<script setup>
/**
 * 页头面包屑：工作台 / 上级页面 / 当前页面（上级来自路由 meta.parent）
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const crumbs = computed(() => {
  const list = []
  const parent = route.meta?.parent
  if (parent) list.push({ path: parent.path, title: parent.title })
  if (route.meta?.title) list.push({ path: route.path, title: route.meta.title })
  return list
})
</script>

<template>
  <nav
    class="crumbs"
    aria-label="当前位置"
  >
    <ol>
      <li
        v-for="(c, i) in crumbs"
        :key="c.path"
      >
        <router-link
          v-if="i < crumbs.length - 1"
          :to="c.path"
          class="crumbs__link"
        >{{ c.title }}</router-link>
        <span
          v-else
          class="crumbs__current"
          aria-current="page"
        >{{ c.title }}</span>
      </li>
    </ol>
  </nav>
</template>

<style scoped>
.crumbs ol {
  display: flex;
  align-items: center;
  list-style: none;
  min-width: 0;
}

.crumbs li {
  display: flex;
  align-items: center;
  min-width: 0;
  font-size: 14px;
}

.crumbs li + li::before {
  content: '/';
  margin: 0 10px;
  color: var(--sw-ink-4);
}

.crumbs__link {
  color: var(--sw-ink-3);
}

.crumbs__link:hover {
  color: var(--sw-ink);
}

.crumbs__current {
  font-weight: 600;
  color: var(--sw-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
