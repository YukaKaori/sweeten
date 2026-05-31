<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import {
  Postcard,
  Lock,
  SwitchButton
} from '@element-plus/icons-vue'

const emit = defineEmits(['open-profile', 'open-password', 'logout'])

const userStore = useUserStore()
const visible = ref(false)

const userName = computed(() => userStore.userInfo?.name || userStore.username || '用户')
const userAccount = computed(() => userStore.username || '-')

const toggle = () => {
  visible.value = !visible.value
}

const handleClickOutside = (e) => {
  if (!e.target.closest('.user-profile-wrapper')) {
    visible.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="user-profile-wrapper">
    <div class="user-info" @click.stop="toggle">
      <div class="user-avatar">
        {{ userName.charAt(0).toUpperCase() }}
      </div>
      <span class="user-name">{{ userName }}</span>
    </div>

    <Transition name="dropdown">
      <div v-if="visible" class="profile-dropdown glass-panel" @click.stop>
        <div class="profile-header">
          <div class="profile-avatar">
            {{ userName.charAt(0).toUpperCase() }}
          </div>
          <div class="profile-info">
            <span class="profile-name">{{ userName }}</span>
            <span class="profile-account">@{{ userAccount }}</span>
          </div>
        </div>

        <div class="profile-menu">
          <div class="profile-menu-item" @click="emit('open-profile')">
            <el-icon :size="16"><Postcard /></el-icon>
            <span>个人信息</span>
          </div>
          <div class="profile-menu-item" @click="emit('open-password')">
            <el-icon :size="16"><Lock /></el-icon>
            <span>修改密码</span>
          </div>
          <div class="profile-divider"></div>
          <div class="profile-menu-item danger" @click="emit('logout')">
            <el-icon :size="16"><SwitchButton /></el-icon>
            <span>退出登录</span>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.user-profile-wrapper {
  position: relative;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.user-info:hover {
  background: rgba(45, 35, 39, 0.06);
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(232, 99, 122, 0.25);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.profile-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 260px;
  border-radius: 16px;
  overflow: hidden;
  z-index: var(--z-dropdown);
  box-shadow: 0 16px 48px rgba(45, 35, 39, 0.15);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 18px;
  border-bottom: 1px solid var(--glass-border);
}

.profile-avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(232, 99, 122, 0.3);
  flex-shrink: 0;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.profile-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.profile-account {
  font-size: 13px;
  color: var(--color-text-muted);
}

.profile-menu {
  padding: 6px;
}

.profile-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  color: var(--color-text-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.profile-menu-item:hover {
  background: rgba(45, 35, 39, 0.05);
}

.profile-menu-item.danger {
  color: #ef6b6b;
}

.profile-menu-item.danger:hover {
  background: rgba(239, 107, 107, 0.08);
}

.profile-divider {
  height: 1px;
  background: var(--glass-border);
  margin: 4px 14px;
}

.dropdown-enter-active {
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.dropdown-leave-active {
  transition: all 0.15s ease;
}

.dropdown-enter-from {
  opacity: 0;
  transform: scale(0.95) translateY(-8px);
}

.dropdown-leave-to {
  opacity: 0;
  transform: scale(0.98) translateY(-4px);
}
</style>
