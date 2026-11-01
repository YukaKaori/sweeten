import { staticRoutes } from './static'
import { storefrontRoutes } from './storefront'
import { workspaceRoutes } from './workspace'

/**
 * 两个产品界面共用一个应用：
 *   顾客商城 /m/*      —— 公开，品牌浅色（views/storefront）
 *   门店工作台 /        —— 员工登录后使用，按权限码过滤（views/workspace）
 */
export const dynamicRoutes = [
  {
    path: '/',
    name: 'layout',
    component: () => import('@/views/workspace/layout/index.vue'),
    redirect: '/index',
    children: workspaceRoutes,
  },
]

/** 所有路由 = 工作台 + 顾客商城 + 静态路由（登录、404，必须最后：含兜底匹配） */
export const allRoutes = [...dynamicRoutes, ...storefrontRoutes, ...staticRoutes]
