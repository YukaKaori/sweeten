<script setup>
/**
 * ⌘K / Ctrl+K 命令面板：快速跳转到工作台页面。
 * 只列出当前员工有权限的页面（config/navigation.js）；父组件可通过 ref 调用 open()。
 */
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'
import { NAV_ITEMS } from '@/config/navigation'
import SwIcon from '@/components/brand/SwIcon.vue'

const RECENT_KEY = 'dessertshop-recent-pages'
const MAX_RECENT = 3

const router = useRouter()
const userStore = useUserStore()
const visible = ref(false)
const searchText = ref('')
const inputRef = ref(null)
const activeIndex = ref(0)

const commands = computed(() => NAV_ITEMS.filter((c) => userStore.can(c.permission)))

const recents = ref([])
const loadRecents = () => {
  try {
    recents.value = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]')
  } catch {
    recents.value = []
  }
}
const saveRecent = (path) => {
  recents.value = [path, ...recents.value.filter((p) => p !== path)].slice(0, MAX_RECENT)
  try { localStorage.setItem(RECENT_KEY, JSON.stringify(recents.value)) } catch { /* ignore */ }
}
loadRecents()

const recentCommands = computed(() =>
  recents.value.map((path) => commands.value.find((c) => c.path === path)).filter(Boolean)
)

const filtered = computed(() => {
  const q = searchText.value.toLowerCase().trim()
  if (!q) return commands.value
  return commands.value.filter((c) =>
    c.label.toLowerCase().includes(q) || c.desc.toLowerCase().includes(q) || c.keywords.toLowerCase().includes(q)
  )
})

const showRecents = computed(() => !searchText.value.trim() && recentCommands.value.length > 0)
/** 键盘导航的扁平列表：最近访问在前 */
const flatList = computed(() => (showRecents.value ? [...recentCommands.value, ...filtered.value] : filtered.value))

let lastFocus = null
const open = () => {
  lastFocus = document.activeElement
  visible.value = true
  searchText.value = ''
  activeIndex.value = 0
  nextTick(() => inputRef.value?.focus())
}

const close = () => {
  visible.value = false
  nextTick(() => lastFocus?.focus?.())
}

const handleKeydown = (e) => {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    visible.value ? close() : open()
  }
}

const handleInputKeydown = (e) => {
  const n = flatList.value.length
  if (e.key === 'ArrowDown' && n) {
    e.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % n
  } else if (e.key === 'ArrowUp' && n) {
    e.preventDefault()
    activeIndex.value = (activeIndex.value - 1 + n) % n
  } else if (e.key === 'Enter') {
    e.preventDefault()
    const cmd = flatList.value[activeIndex.value]
    if (cmd) execute(cmd)
  } else if (e.key === 'Escape') {
    e.preventDefault()
    close()
  }
}

const execute = (cmd) => {
  saveRecent(cmd.path)
  visible.value = false
  router.push(cmd.path)
}

watch(searchText, () => {
  activeIndex.value = 0
})

onMounted(() => document.addEventListener('keydown', handleKeydown))
onUnmounted(() => document.removeEventListener('keydown', handleKeydown))

defineExpose({ open, close })
</script>

<template>
  <Teleport to="body">
    <Transition name="palette">
      <div
        v-if="visible"
        class="palette-overlay"
        @click.self="close"
      >
        <div
          class="palette"
          role="dialog"
          aria-modal="true"
          aria-label="快速跳转"
        >
          <div class="palette__search">
            <SwIcon
              name="search"
              :size="18"
            />
            <input
              ref="inputRef"
              v-model="searchText"
              class="palette__input"
              placeholder="搜索页面，如「订单」「库存」"
              aria-label="搜索页面"
              role="combobox"
              aria-expanded="true"
              aria-controls="palette-list"
              @keydown="handleInputKeydown"
            >
            <kbd class="palette__kbd">ESC</kbd>
          </div>

          <div
            id="palette-list"
            class="palette__list"
            role="listbox"
          >
            <template v-if="showRecents">
              <p class="palette__section">最近访问</p>
              <button
                v-for="(cmd, i) in recentCommands"
                :key="'r' + cmd.path"
                type="button"
                role="option"
                :aria-selected="activeIndex === i"
                class="palette__item"
                :class="{ 'is-active': activeIndex === i }"
                @click="execute(cmd)"
                @mouseenter="activeIndex = i"
              >
                <span class="palette__icon"><SwIcon
                  :name="cmd.icon"
                  :size="18"
                /></span>
                <span class="palette__text">
                  <span class="palette__label">{{ cmd.label }}</span>
                  <span class="palette__desc">{{ cmd.desc }}</span>
                </span>
              </button>
              <p class="palette__section">全部页面</p>
            </template>

            <button
              v-for="(cmd, i) in filtered"
              :key="cmd.path"
              type="button"
              role="option"
              :aria-selected="activeIndex === i + (showRecents ? recentCommands.length : 0)"
              class="palette__item"
              :class="{ 'is-active': activeIndex === i + (showRecents ? recentCommands.length : 0) }"
              @click="execute(cmd)"
              @mouseenter="activeIndex = i + (showRecents ? recentCommands.length : 0)"
            >
              <span class="palette__icon"><SwIcon
                :name="cmd.icon"
                :size="18"
              /></span>
              <span class="palette__text">
                <span class="palette__label">{{ cmd.label }}</span>
                <span class="palette__desc">{{ cmd.desc }}</span>
              </span>
            </button>

            <p
              v-if="!filtered.length"
              class="palette__empty"
            >没有找到「{{ searchText }}」相关的页面</p>
          </div>

          <div class="palette__foot">
            <span><kbd>↑</kbd><kbd>↓</kbd> 选择</span>
            <span><kbd>↵</kbd> 打开</span>
            <span><kbd>Ctrl</kbd><kbd>K</kbd> 随时唤起</span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.palette-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 14vh 16px 16px;
  background: rgba(36, 26, 22, 0.36);
}

.palette {
  width: min(560px, 100%);
  max-height: min(480px, 76vh);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-xl);
  background: var(--sw-surface);
  box-shadow: var(--sw-shadow-lg);
}

.palette__search {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--sw-border-soft);
  color: var(--sw-ink-3);
}

.palette__input {
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
  font-size: 15px;
  color: var(--sw-ink);
}

.palette__input::placeholder {
  color: var(--sw-ink-4);
}

kbd {
  display: inline-grid;
  place-items: center;
  min-width: 22px;
  height: 20px;
  padding: 0 5px;
  margin-right: 3px;
  border: 1px solid var(--sw-border);
  border-radius: 5px;
  background: var(--sw-surface-2);
  font-family: var(--sw-font-sans);
  font-size: 11px;
  color: var(--sw-ink-3);
}

.palette__kbd {
  margin: 0;
}

.palette__list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.palette__section {
  padding: 10px 10px 6px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.palette__item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px;
  border: 0;
  border-radius: var(--sw-r-md);
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: var(--sw-ink);
}

.palette__item.is-active {
  background: var(--sw-accent-soft);
}

.palette__icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: var(--sw-r-sm);
  background: var(--sw-fill-soft);
  color: var(--sw-ink-2);
}

.palette__item.is-active .palette__icon {
  background: var(--sw-surface);
  color: var(--sw-accent);
}

.palette__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.palette__label {
  font-size: 14px;
  font-weight: 600;
}

.palette__desc {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.palette__empty {
  padding: 28px 12px;
  text-align: center;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.palette__foot {
  display: flex;
  gap: 18px;
  padding: 10px 18px;
  border-top: 1px solid var(--sw-border-soft);
  font-size: 12px;
  color: var(--sw-ink-3);
}

.palette-enter-active,
.palette-leave-active {
  transition: opacity var(--sw-dur-2) var(--sw-ease);
}

.palette-enter-active .palette,
.palette-leave-active .palette {
  transition: transform var(--sw-dur-2) var(--sw-ease);
}

.palette-enter-from,
.palette-leave-to {
  opacity: 0;
}

.palette-enter-from .palette {
  transform: translateY(-8px) scale(0.99);
}

@media (max-width: 640px) {
  .palette__foot {
    display: none;
  }
}
</style>
