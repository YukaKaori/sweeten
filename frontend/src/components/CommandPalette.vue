<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'

import { useRouter } from 'vue-router'
import {
  HomeFilled, Goods, Wallet, UserFilled, Avatar,
  DataAnalysis, Document, Search, Position
} from '@element-plus/icons-vue'

const RECENT_KEY = 'dessertshop-recent-pages'
const MAX_RECENT = 4

const router = useRouter()
const visible = ref(false)
const searchText = ref('')
const inputRef = ref(null)
const activeIndex = ref(0)

// 命令列表
const commands = [
  { label: '首页', desc: '返回仪表盘', icon: HomeFilled, path: '/index', keywords: 'home 首页 仪表盘 dashboard' },
  { label: '甜品管理', desc: '管理甜品商品', icon: Goods, path: '/dessert', keywords: 'dessert 甜品 商品 蛋糕 面包' },
  { label: '价格管理', desc: '管理甜品价格和上下架', icon: Wallet, path: '/price', keywords: 'price 价格 甜品 dessert' },
  { label: '订单管理', desc: '查看和管理订单', icon: Goods, path: '/order', keywords: 'order 订单' },
  { label: '库存管理', desc: '管理原材料库存', icon: Document, path: '/inventory', keywords: 'inventory 库存 原料' },
  { label: '客户管理', desc: '管理客户信息', icon: UserFilled, path: '/customer', keywords: 'customer 客户 会员' },
  { label: '部门管理', desc: '管理组织架构', icon: UserFilled, path: '/dept', keywords: 'dept 部门 组织' },
  { label: '员工管理', desc: '管理人员信息', icon: Avatar, path: '/emp', keywords: 'emp 员工 人事' },
  { label: '数据报表', desc: '查看营业数据统计', icon: DataAnalysis, path: '/report', keywords: 'report 报表 数据 统计 分析' },
  { label: '操作日志', desc: '查看系统操作记录', icon: Document, path: '/log', keywords: 'log 日志 审计' },
]

// 最近访问页面
const recents = ref([])
const loadRecents = () => {
  try {
    recents.value = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]')
  } catch { recents.value = [] }
}
const saveRecent = (path) => {
  const filtered = recents.value.filter(p => p !== path)
  recents.value = [path, ...filtered].slice(0, MAX_RECENT)
  localStorage.setItem(RECENT_KEY, JSON.stringify(recents.value))
}
loadRecents()

// 获取最近访问的命令对象
const recentCommands = computed(() => {
  return recents.value
    .map(path => commands.find(c => c.path === path))
    .filter(Boolean)
    .slice(0, 3)
})

// 过滤结果
const filtered = computed(() => {
  const q = searchText.value.toLowerCase().trim()
  if (!q) return commands
  return commands.filter(cmd =>
    cmd.label.toLowerCase().includes(q) ||
    cmd.desc.toLowerCase().includes(q) ||
    cmd.keywords.toLowerCase().includes(q)
  )
})

// 是否显示最近访问（搜索为空时）
const showRecents = computed(() => !searchText.value.trim() && recentCommands.value.length > 0)

// 打开/关闭
const open = () => {
  visible.value = true
  searchText.value = ''
  activeIndex.value = 0
  nextTick(() => inputRef.value?.focus())
}

const close = () => {
  visible.value = false
}

const toggle = () => {
  visible.value ? close() : open()
}

// 键盘事件
const handleKeydown = (e) => {
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault()
    toggle()
  }
}

const handleInputKeydown = (e) => {
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % filtered.value.length
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value - 1 + filtered.value.length) % filtered.value.length
  } else if (e.key === 'Enter') {
    e.preventDefault()
    if (filtered.value[activeIndex.value]) {
      execute(filtered.value[activeIndex.value])
    }
  } else if (e.key === 'Escape') {
    close()
  }
}

// 执行命令
const execute = (cmd) => {
  saveRecent(cmd.path)
  router.push(cmd.path)
  close()
}

watch(searchText, () => {
  activeIndex.value = 0
})

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="palette">
      <div v-if="visible" class="command-overlay" @click.self="close">
        <div class="command-palette glass-panel">
          <!-- 搜索输入 -->
          <div class="command-input-wrapper">
            <el-icon :size="18" class="command-search-icon"><Search /></el-icon>
            <input
              ref="inputRef"
              v-model="searchText"
              class="command-input"
              placeholder="搜索页面、功能..."
              @keydown="handleInputKeydown"
            />
            <kbd class="command-kbd">ESC</kbd>
          </div>

          <!-- 结果列表 -->
          <div class="command-results">
            <!-- 最近访问（无搜索时显示）-->
            <div v-if="showRecents" class="command-section">
              <div class="command-section__title">最近访问</div>
              <div
                v-for="(cmd, index) in recentCommands"
                :key="'recent-' + cmd.path"
                class="command-item"
                :class="{ active: index === activeIndex }"
                @click="execute(cmd)"
                @mouseenter="activeIndex = index"
              >
                <div class="command-item__icon">
                  <el-icon :size="18"><component :is="cmd.icon" /></el-icon>
                </div>
                <div class="command-item__text">
                  <span class="command-item__label">{{ cmd.label }}</span>
                  <span class="command-item__desc">{{ cmd.desc }}</span>
                </div>
                <el-icon :size="14" class="command-item__arrow"><Position /></el-icon>
              </div>
            </div>

            <!-- 搜索结果 -->
            <div
              v-for="(cmd, index) in filtered"
              :key="cmd.path"
              class="command-item"
              :class="{ active: index === activeIndex }"
              @click="execute(cmd)"
              @mouseenter="activeIndex = index"
            >
              <div class="command-item__icon">
                <el-icon :size="18"><component :is="cmd.icon" /></el-icon>
              </div>
              <div class="command-item__text">
                <span class="command-item__label">{{ cmd.label }}</span>
                <span class="command-item__desc">{{ cmd.desc }}</span>
              </div>
              <el-icon :size="14" class="command-item__arrow"><Position /></el-icon>
            </div>

            <div v-if="filtered.length === 0 && !showRecents" class="command-empty">
              没有找到匹配的结果
            </div>
          </div>

          <!-- 底部提示 -->
          <div class="command-footer">
            <span><kbd>↑↓</kbd> 导航</span>
            <span><kbd>↵</kbd> 打开</span>
            <span><kbd>ESC</kbd> 关闭</span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.command-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 15vh;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
}

.command-palette {
  width: 560px;
  max-height: 420px;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  overflow: hidden;
  animation: commandSlideIn 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes commandSlideIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-12px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 输入区域 */
.command-input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--glass-border);
}

.command-search-icon {
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.command-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 16px;
  color: var(--color-text-primary);
  font-family: var(--font-body);
}

.command-input::placeholder {
  color: var(--color-text-muted);
}

.command-kbd {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  font-family: monospace;
}

/* 结果列表 */
.command-results {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.command-section {
  margin-bottom: 4px;
}

.command-section__title {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--color-text-muted);
  padding: 6px 12px 4px;
}

.command-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.command-item:hover,
.command-item.active {
  background: rgba(232, 99, 122, 0.08);
}

.command-item__icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--color-bg-tertiary);
  color: var(--color-primary);
  flex-shrink: 0;
}

.command-item__text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.command-item__label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.command-item__desc {
  font-size: 12px;
  color: var(--color-text-muted);
}

.command-item__arrow {
  color: var(--color-text-muted);
  opacity: 0;
  transition: opacity 0.15s ease;
}

.command-item.active .command-item__arrow,
.command-item:hover .command-item__arrow {
  opacity: 1;
}

.command-empty {
  text-align: center;
  padding: 32px;
  color: var(--color-text-muted);
  font-size: 14px;
}

/* 底部提示 */
.command-footer {
  display: flex;
  gap: 16px;
  padding: 10px 20px;
  border-top: 1px solid var(--glass-border);
  font-size: 12px;
  color: var(--color-text-muted);
}

.command-footer kbd {
  font-size: 11px;
  padding: 1px 5px;
  border-radius: 3px;
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border);
  font-family: monospace;
  margin-right: 2px;
}

/* 过渡动画 */
.palette-enter-active {
  transition: opacity 0.2s ease;
}

.palette-leave-active {
  transition: opacity 0.15s ease;
}

.palette-enter-from,
.palette-leave-to {
  opacity: 0;
}

/* 滚动条 */
.command-results::-webkit-scrollbar {
  width: 4px;
}

.command-results::-webkit-scrollbar-track {
  background: transparent;
}

.command-results::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 2px;
}
</style>
