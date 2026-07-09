import { ref, computed } from 'vue'

/**
 * useTheme —「奶油玻璃（浅）/ 霓虹甜品（暗）」双外观。
 * 说明：原先的 5 套配色预设（草莓/抹茶/黑巧/蓝莓/樱花）并无 CSS 落地，
 * 全部回退到同一套令牌，属失效装饰，已随品牌统一移除。
 * 现在只保留真实可用的「浅色 / 深色」两种外观。
 */

const MODE_KEY = 'dessertshop-color-mode'

const mode = ref('light')

// 初始化：读取本地存储，否则跟随系统
const initTheme = () => {
  const storedMode = localStorage.getItem(MODE_KEY)
  if (storedMode === 'dark' || storedMode === 'light') {
    mode.value = storedMode
  } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
    mode.value = 'dark'
  }
  applyTheme()
}

// 应用外观
const applyTheme = () => {
  const html = document.documentElement
  html.classList.add('theme-transitioning')
  if (mode.value === 'dark') {
    html.setAttribute('data-theme', 'dark')
  } else {
    html.removeAttribute('data-theme')
  }
  setTimeout(() => html.classList.remove('theme-transitioning'), 450)
}

// 切换浅/深
const toggleMode = () => {
  mode.value = mode.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem(MODE_KEY, mode.value)
  applyTheme()
}

// 兼容旧接口
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
    toggleTheme, // 兼容旧接口
    toggleMode,
    applyTheme,
  }
}
