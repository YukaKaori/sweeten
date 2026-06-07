import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/modules/user'

NProgress.configure({ showSpinner: false, speed: 400 })

/**
 * 设置全局路由守卫
 * @param {import('vue-router').Router} router
 */
export function setupRouterGuards(router) {
  router.beforeEach((to, from, next) => {
    NProgress.start()

    // 设置页面标题
    const title = to.meta.title ? `${to.meta.title} - DessertShop` : 'DessertShop'
    document.title = title

    // 检查登录状态
    const userStore = useUserStore()
    userStore.initFromStorage()

    // 未登录 → 只允许访问 /login
    if (!userStore.isLoggedIn && to.path !== '/login') {
      next('/login')
      return
    }

    // 已登录 → 不允许再访问 /login
    if (userStore.isLoggedIn && to.path === '/login') {
      next('/')
      return
    }

    next()
  })

  router.afterEach(() => {
    NProgress.done()
  })
}
