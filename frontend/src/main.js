// Vue 核心导入
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { config } from './config'

// Element Plus 核心导入（品牌色通过 base.css 中的 --el-* 变量覆盖）
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 品牌设计系统（assets/base.css）必须在 Element Plus 样式之后引入：
// 两者都在 :root 上定义 --el-* 变量，后引入的生效
import './assets/main.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 初始化外观（在挂载前执行，避免闪烁；深色只在门店工作台生效）
import { useTheme } from '@/composables/useTheme'
import { preconnectImageOrigin } from '@/config/productImage'
const { initTheme } = useTheme()
initTheme()

// 预连接商品图床域名（提前完成 TLS 握手）
preconnectImageOrigin()

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

// 设置 HTML 标题（各页面由路由守卫再细化）
document.title = config.appTitle

// 挂载应用
app.mount('#app')
