import { staticRoutes } from './static'
import { dashboardRoutes } from './modules/dashboard'
import { systemRoutes } from './modules/system'
import { businessRoutes } from './modules/business'
import { dataRoutes } from './modules/data'

/**
 * 动态路由（需要登录权限）
 * Layout 作为父路由包裹所有业务页面
 */
export const dynamicRoutes = [
  {
    path: '/',
    name: 'layout',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/index',
    children: [
      ...dashboardRoutes,
      ...systemRoutes,
      ...businessRoutes,
      ...dataRoutes,
    ],
  },
]

/** 所有路由 = 静态路由 + 动态路由 */
export const allRoutes = [...dynamicRoutes, ...staticRoutes]
