import { createRouter, createWebHistory } from 'vue-router'
import { allRoutes } from './routes'
import { setupRouterGuards } from './guards'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: allRoutes,
})

// 设置全局路由守卫
setupRouterGuards(router)

export default router
