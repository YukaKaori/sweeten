// 导入全局样式
import './assets/main.css'

// Vue 核心导入
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// Element Plus 核心导入
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 中文语言包
import zhCn from 'element-plus/es/locale/lang/zh-cn'
// Element Plus 所有图标（批量导入）
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 初始化主题（在 Vue 挂载前执行，避免闪烁）
import { useTheme } from '@/composables/useTheme'
const { initTheme } = useTheme()
initTheme()

// 创建 Vue 应用实例
const app = createApp(App)

// 1. 注册 Pinia 状态管理
const pinia = createPinia()
app.use(pinia)

// 2. 注册 Element Plus（核心：补充配置中文语言包）
app.use(ElementPlus, {
  locale: zhCn,
  size: 'default'
})

// 3. 注册路由
app.use(router)

// 4. 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 挂载应用到 #app 节点
app.mount('#app')
