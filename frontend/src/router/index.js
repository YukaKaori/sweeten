import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/user'

NProgress.configure({ showSpinner: false, speed: 400 })

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '',
      component: () => import('@/views/layout/index.vue'),
      redirect: '/index',
      children: [
        {
          path: 'index',
          name: 'index',
          component: () => import('@/views/index/index.vue'),
          meta: { title: '首页' }
        },
        {
          path: 'dept',
          name: 'dept',
          component: () => import('@/views/dept/index.vue'),
          meta: { title: '部门管理' }
        },
        {
          path: 'emp',
          name: 'emp',
          component: () => import('@/views/emp/index.vue'),
          meta: { title: '员工管理' }
        },
        {
          path: 'log',
          name: 'log',
          component: () => import('@/views/log/index.vue'),
          meta: { title: '操作日志' }
        },
        {
          path: 'report',
          name: 'report',
          component: () => import('@/views/report/index.vue'),
          meta: { title: '数据报表' }
        },
        {
          path: 'price',
          name: 'price',
          component: () => import('@/views/price/index.vue'),
          meta: { title: '价格管理' }
        },
        {
          path: 'order',
          name: 'order',
          component: () => import('@/views/order/index.vue'),
          meta: { title: '订单管理' }
        }
      ]
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: () => import('@/views/404.vue'),
      meta: { title: '页面不存在' }
    }
  ]
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  NProgress.start()

  // 设置页面标题
  const title = to.meta.title ? `${to.meta.title} - DessertShop` : 'DessertShop'
  document.title = title

  // 通过 Pinia Store 检查登录状态
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

export default router
