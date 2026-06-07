// 导入全局样式
import './assets/main.css'

// Vue 核心导入
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { config } from './config'

// Element Plus 核心导入
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 初始化主题（在 Vue 挂载前执行，避免闪烁）
import { useTheme } from '@/composables/useTheme'
const { initTheme } = useTheme()
initTheme()

// 创建 Vue 应用实例
const app = createApp(App)

// 1. 注册 Pinia 状态管理
app.use(createPinia())

// 2. 注册 Element Plus（中文语言包）
app.use(ElementPlus, {
  locale: zhCn,
  size: 'default',
})

// 3. 注册路由
app.use(router)

// 4. 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 设置 HTML 标题
document.title = config.appTitle

// 挂载应用
app.mount('#app')
