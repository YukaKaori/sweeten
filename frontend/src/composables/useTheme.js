import { ref, computed } from 'vue'

/**
 * useTheme — 浅色（品牌标准）/ 可可夜（深色）两种外观
 *
 * 深色外观只用于门店工作台：顾客商城、登录页、404 始终保持品牌浅色。
 * 工作台布局挂载时调用 enableTheme()、卸载时调用 disableTheme()；
 * 首次加载时如果直接打开的就是工作台页面，initTheme() 会提前应用，避免闪烁。
 */

const MODE_KEY = 'dessertshop-color-mode'

const mode = ref('light')
let active = false

const isBrandSurface = (path) => path === '/login' || path === '/m' || path.startsWith('/m/') || path.endsWith('.html')

const write = (dark, animate = true) => {
  const html = document.documentElement
  if (animate) html.classList.add('theme-transitioning')
  if (dark) html.setAttribute('data-theme', 'dark')
  else html.removeAttribute('data-theme')
  if (animate) setTimeout(() => html.classList.remove('theme-transitioning'), 350)
}

const applyTheme = (animate = true) => write(active && mode.value === 'dark', animate)

// 初始化：读取本地存储，否则跟随系统
const initTheme = () => {
  const storedMode = localStorage.getItem(MODE_KEY)
  if (storedMode === 'dark' || storedMode === 'light') {
    mode.value = storedMode
  } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
    mode.value = 'dark'
  }
  active = !isBrandSurface(window.location.pathname)
  applyTheme(false)
}

/** 进入工作台：应用用户选择的外观 */
const enableTheme = () => {
  active = true
  applyTheme(false)
}

/** 离开工作台：恢复品牌浅色 */
const disableTheme = () => {
  active = false
  applyTheme(false)
}

// 切换浅/深
const toggleMode = () => {
  mode.value = mode.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem(MODE_KEY, mode.value)
  applyTheme()
}

const theme = computed(() => mode.value)
const toggleTheme = toggleMode

// 跟随系统主题变化（仅在用户未手动设定时）
if (typeof window !== 'undefined') {
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
    if (!localStorage.getItem(MODE_KEY)) {
      mode.value = e.matches ? 'dark' : 'light'
      applyTheme()
    }
  })
}

export function useTheme() {
  return {
    theme,
    mode,
    initTheme,
    enableTheme,
    disableTheme,
    toggleTheme,
    toggleMode,
    applyTheme,
  }
}
