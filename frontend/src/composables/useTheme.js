import { ref, computed, watch } from 'vue'

const THEME_KEY = 'dessertshop-color-theme'
const MODE_KEY = 'dessertshop-color-mode'

// ========== 5 套主题预设 ==========
export const THEME_PRESETS = {
  strawberry: {
    name: '草莓甜心',
    icon: '🍓',
    description: '粉嫩少女 · 浪漫甜品',
    // CSS 变量通过 data-theme-preset 属性注入
  },
  matcha: {
    name: '抹茶风味',
    icon: '🍵',
    description: '日式极简 · 清新素雅',
  },
  chocolate: {
    name: '黑巧格调',
    icon: '🍫',
    description: '高端暗系 · 精致奢华',
  },
  blueberry: {
    name: '蓝莓之夜',
    icon: '🫐',
    description: '科技质感 · 沉稳专业',
  },
  sakura: {
    name: '樱花物语',
    icon: '🌸',
    description: '清新淡雅 · 春日气息',
  },
}

const preset = ref('strawberry')
const mode = ref('light')

// 初始化主题
const initTheme = () => {
  const storedPreset = localStorage.getItem(THEME_KEY)
  const storedMode = localStorage.getItem(MODE_KEY)

  if (storedPreset && THEME_PRESETS[storedPreset]) {
    preset.value = storedPreset
  }
  if (storedMode === 'dark' || storedMode === 'light') {
    mode.value = storedMode
  } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
    mode.value = 'dark'
  }

  applyTheme()
}

// 应用主题
const applyTheme = () => {
  const html = document.documentElement

  html.classList.add('theme-transitioning')

  // 设置主题预设
  html.setAttribute('data-theme-preset', preset.value)

  // 设置暗色/亮色模式
  if (mode.value === 'dark') {
    html.setAttribute('data-theme', 'dark')
  } else {
    html.removeAttribute('data-theme')
  }

  setTimeout(() => html.classList.remove('theme-transitioning'), 450)
}

// 切换暗色模式
const toggleMode = () => {
  mode.value = mode.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem(MODE_KEY, mode.value)
  applyTheme()
}

// 切换主题预设
const setPreset = (key) => {
  if (!THEME_PRESETS[key]) return
  preset.value = key
  localStorage.setItem(THEME_KEY, key)
  applyTheme()
}

// 兼容旧接口
const theme = computed(() => mode.value)
const toggleTheme = toggleMode

// 当前主题信息
const currentPreset = computed(() => THEME_PRESETS[preset.value])

// 监听系统主题变化
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
    preset,
    currentPreset,
    presets: THEME_PRESETS,
    initTheme,
    toggleTheme,     // 兼容旧接口
    toggleMode,
    setPreset,
    applyTheme,
  }
}
