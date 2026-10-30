<script setup>
/**
 * 页头用户菜单：头像 + 姓名 + 岗位角色；个人信息 / 修改密码 / 退出登录
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/modules/user'
import SwIcon from '@/components/brand/SwIcon.vue'

const emit = defineEmits(['open-profile', 'open-password', 'logout'])

const userStore = useUserStore()
const visible = ref(false)

// 角色码 → 岗位名称（与 sql/rbac_init.sql 一致），按权限从高到低取第一个
const ROLE_LABELS = [
  ['super_admin', '店长'],
  ['manager', '副店长'],
  ['cashier', '收银 · 运营'],
  ['warehouse', '仓管 · 后厨'],
  ['finance', '财务'],
]

const userName = computed(() => userStore.displayName || '同事')
const userAccount = computed(() => userStore.username || '')
const userImage = computed(() => userStore.userInfo?.image || '')
const roleLabel = computed(() => ROLE_LABELS.find(([code]) => userStore.roles.includes(code))?.[1] || '门店员工')
const initial = computed(() => userName.value.charAt(0).toUpperCase())

const toggle = () => {
  visible.value = !visible.value
}

const pick = (event) => {
  visible.value = false
  emit(event)
}

const onDocClick = (e) => {
  if (!e.target.closest('.user-menu')) visible.value = false
}
const onKey = (e) => {
  if (e.key === 'Escape') visible.value = false
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
  document.addEventListener('keydown', onKey)
})
onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKey)
})
</script>

<template>
  <div class="user-menu">
    <button
      type="button"
      class="user-menu__trigger"
      :aria-expanded="visible"
      aria-haspopup="menu"
      @click.stop="toggle"
    >
      <span class="user-menu__avatar">
        <img
          v-if="userImage"
          :src="userImage"
          alt=""
        />
        <span v-else>{{ initial }}</span>
      </span>
      <span class="user-menu__who">
        <span class="user-menu__name">{{ userName }}</span>
        <span class="user-menu__role">{{ roleLabel }}</span>
      </span>
      <SwIcon
        name="chevron-down"
        :size="14"
        class="user-menu__caret"
      />
    </button>

    <Transition name="pop">
      <div
        v-if="visible"
        class="user-menu__panel"
        role="menu"
        @click.stop
      >
        <div class="user-menu__head">
          <span class="user-menu__avatar is-lg">
            <img
              v-if="userImage"
              :src="userImage"
              alt=""
            />
            <span v-else>{{ initial }}</span>
          </span>
          <span class="user-menu__who">
            <span class="user-menu__name">{{ userName }}</span>
            <span class="user-menu__role">@{{ userAccount }} · {{ roleLabel }}</span>
          </span>
        </div>
        <button
          type="button"
          role="menuitem"
          class="user-menu__item"
          @click="pick('open-profile')"
        >
          <SwIcon
            name="user"
            :size="17"
          /> 个人信息
        </button>
        <button
          type="button"
          role="menuitem"
          class="user-menu__item"
          @click="pick('open-password')"
        >
          <SwIcon
            name="lock"
            :size="17"
          /> 修改密码
        </button>
        <div class="user-menu__sep"></div>
        <button
          type="button"
          role="menuitem"
          class="user-menu__item is-danger"
          @click="pick('logout')"
        >
          <SwIcon
            name="logout"
            :size="17"
          /> 退出登录
        </button>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.user-menu {
  position: relative;
}

.user-menu__trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  padding: 0 10px 0 4px;
  border: 0;
  border-radius: var(--sw-r-pill);
  background: transparent;
  color: var(--sw-ink);
  cursor: pointer;
  transition: background-color var(--sw-dur-1) var(--sw-ease);
}

.user-menu__trigger:hover,
.user-menu__trigger[aria-expanded='true'] {
  background: var(--sw-fill-soft);
}

.user-menu__avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  overflow: hidden;
  border-radius: 50%;
  background: var(--sw-blush-soft);
  color: var(--sw-cocoa);
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-menu__avatar.is-lg {
  width: 42px;
  height: 42px;
  font-size: 16px;
}

.user-menu__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-menu__who {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
  line-height: 1.25;
}

.user-menu__name {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.user-menu__role {
  font-size: 12px;
  color: var(--sw-ink-3);
  white-space: nowrap;
}

.user-menu__caret {
  color: var(--sw-ink-3);
}

.user-menu__panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 240px;
  padding: 6px;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-lg);
  background: var(--sw-surface);
  box-shadow: var(--sw-shadow-lg);
  z-index: var(--z-dropdown);
}

.user-menu__head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px 12px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--sw-border-soft);
}

.user-menu__item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 40px;
  padding: 0 10px;
  border: 0;
  border-radius: var(--sw-r-md);
  background: transparent;
  color: var(--sw-ink);
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.user-menu__item:hover {
  background: var(--sw-fill-soft);
}

.user-menu__item.is-danger {
  color: var(--sw-danger);
}

.user-menu__item.is-danger:hover {
  background: var(--sw-danger-bg);
}

.user-menu__sep {
  height: 1px;
  margin: 4px 6px;
  background: var(--sw-border-soft);
}

.pop-enter-active,
.pop-leave-active {
  transition: opacity var(--sw-dur-2) var(--sw-ease), transform var(--sw-dur-2) var(--sw-ease);
}

.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 768px) {
  .user-menu__who,
  .user-menu__caret {
    display: none;
  }

  .user-menu__panel .user-menu__who {
    display: flex;
  }

  .user-menu__trigger {
    padding: 0 4px;
  }
}
</style>
