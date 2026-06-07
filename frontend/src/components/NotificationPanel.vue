<script setup>
import { ref, computed, onMounted } from 'vue'
import { Bell, WarningFilled, Loading } from '@element-plus/icons-vue'
import { queryStockAlertApi } from '@/api/modules/dashboard'

const visible = ref(false)
const loading = ref(false)

// 从后端获取的库存预警通知
const notifications = ref([])

// 从后端加载库存预警数据
const loadNotifications = async () => {
  loading.value = true
  try {
    const res = await queryStockAlertApi()
    if (res.code && Array.isArray(res.data)) {
      notifications.value = res.data.map((item, index) => ({
        id: index + 1,
        type: 'warning',
        title: '库存预警',
        message: `${item.name} 库存不足，剩余 ${item.stock} 件`,
        read: false
      }))
    }
  } catch {
    notifications.value = []
  } finally {
    loading.value = false
  }
}

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

const toggle = () => {
  visible.value = !visible.value
  if (visible.value && notifications.value.length === 0) {
    loadNotifications()
  }
}

const markAsRead = (item) => {
  item.read = true
}

const markAllRead = () => {
  notifications.value.forEach(n => n.read = true)
}

// 点击外部关闭
const handleClickOutside = (e) => {
  if (!e.target.closest('.notification-wrapper')) {
    visible.value = false
  }
}

onMounted(() => {
  // 预加载未读数量
  loadNotifications()
  document.addEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="notification-wrapper">
    <div class="header-action" @click.stop="toggle">
      <el-icon :size="18"><Bell /></el-icon>
      <span v-if="unreadCount > 0" class="notification-dot"></span>
    </div>

    <Transition name="dropdown">
      <div v-if="visible" class="notification-dropdown glass-panel" @click.stop>
        <!-- 头部 -->
        <div class="notification-header">
          <span class="notification-title">通知中心</span>
          <el-button link size="small" @click="markAllRead" v-if="unreadCount > 0">
            全部已读
          </el-button>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="notification-loading">
          <el-icon class="is-loading" :size="20"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="notifications.length === 0" class="notification-empty">
          <span>暂无新通知</span>
        </div>

        <!-- 列表 -->
        <div v-else class="notification-list">
          <div
            v-for="item in notifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: !item.read }"
            @click="markAsRead(item)"
          >
            <div class="notification-item__icon" :class="item.type">
              <el-icon :size="16"><WarningFilled /></el-icon>
            </div>
            <div class="notification-item__content">
              <span class="notification-item__title">{{ item.title }}</span>
              <span class="notification-item__message">{{ item.message }}</span>
            </div>
            <div v-if="!item.read" class="notification-item__badge"></div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.notification-wrapper {
  position: relative;
}

.notification-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: -8px;
  width: 340px;
  max-height: 400px;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  overflow: hidden;
  z-index: var(--z-dropdown);
  box-shadow: 0 16px 48px rgba(45, 35, 39, 0.15);
}

/* 头部 */
.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid var(--glass-border);
}

.notification-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 加载和空状态 */
.notification-loading,
.notification-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 0;
  color: var(--color-text-muted);
  font-size: 13px;
}

/* 列表 */
.notification-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s ease;
  position: relative;
}

.notification-item:hover {
  background: rgba(240, 163, 92, 0.05);
}

.notification-item.unread {
  background: rgba(240, 163, 92, 0.04);
}

.notification-item__icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  flex-shrink: 0;
}

.notification-item__icon.warning {
  background: rgba(240, 163, 92, 0.1);
  color: #f0a35c;
}

.notification-item__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.notification-item__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.notification-item__message {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.notification-item__badge {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-primary);
  flex-shrink: 0;
  margin-top: 6px;
}

/* 过渡动画 */
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

/* 滚动条 */
.notification-list::-webkit-scrollbar {
  width: 4px;
}

.notification-list::-webkit-scrollbar-track {
  background: transparent;
}

.notification-list::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 2px;
}
</style>
