import { createRouter, createWebHistory } from 'vue-router'
import { allRoutes } from './routes'
import { setupRouterGuards } from './guards'

const prefersReducedMotion = () =>
  typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: allRoutes,
  // 后退恢复原位置；锚点（如商城 #menu）滚到对应区块并让出吸顶页头；其余换页回到顶部
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) {
      return new Promise((resolve) => {
        requestAnimationFrame(() =>
          resolve({ el: to.hash, top: 84, behavior: prefersReducedMotion() ? 'auto' : 'smooth' })
        )
      })
    }
    if (to.path !== from.path) return { top: 0 }
    return false
  },
})

// 设置全局路由守卫
setupRouterGuards(router)

export default router
