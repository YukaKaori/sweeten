<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/modules/user'
import { firstAllowedPath } from '@/config/navigation'
import EmptyState from '@/components/EmptyState.vue'

const userStore = useUserStore()
const fallback = computed(() => firstAllowedPath(userStore.can))
</script>

<template>
  <div class="forbidden">
    <EmptyState
      title="当前账号没有这个页面的权限"
      description="工作台会按你的岗位显示可用的功能。如需开通，请联系店长调整角色。"
    >
      <template #action>
        <el-button
          v-if="fallback !== '/forbidden'"
          type="primary"
          @click="$router.replace(fallback)"
        >回到我的工作台</el-button>
      </template>
    </EmptyState>
  </div>
</template>

<style scoped>
.forbidden {
  display: grid;
  place-items: center;
  min-height: calc(100vh - var(--sw-header-h) - 64px);
}
</style>
