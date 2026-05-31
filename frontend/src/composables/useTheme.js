import { ref, watch } from 'vue'

const THEME_KEY = 'dessertshop-theme'
const theme = ref('light')

// 初始化主题
const initTheme = () => {
  const stored = localStorage.getItem(THEME_KEY)
  if (stored === 'dark' || stored === 'light') {
    theme.value = stored
  } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
    theme.value = 'dark'
  }
  applyTheme(theme.value)
}

// 应用主题
const applyTheme = (value) => {
  const html = document.documentElement

  // 添加过渡类
  html.classList.add('theme-transitioning')

  if (value === 'dark') {
    html.setAttribute('data-theme', 'dark')
  } else {
    html.removeAttribute('data-theme')
  }

  // 移除过渡类
  setTimeout(() => html.classList.remove('theme-transitioning'), 450)
}

// 切换主题
const toggleTheme = () => {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem(THEME_KEY, theme.value)
  applyTheme(theme.value)
}

// 监听系统主题变化
if (typeof window !== 'undefined') {
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
    if (!localStorage.getItem(THEME_KEY)) {
      theme.value = e.matches ? 'dark' : 'light'
      applyTheme(theme.value)
    }
  })
}

export function useTheme() {
  return {
    theme,
    initTheme,
    toggleTheme
  }
}
