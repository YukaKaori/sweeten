/**
 * 业务管理路由（甜品 / 价格 / 订单 / 库存）
 * meta.permission 与后端 @RequirePermission 的查询权限一致，无权限时路由守卫不放行
 */
export const businessRoutes = [
  {
    path: 'dessert',
    name: 'dessert',
    component: () => import('@/views/dessert/index.vue'),
    meta: { title: '甜品管理', permission: 'dessert:dessert:list' },
  },
  {
    path: 'price',
    name: 'price',
    component: () => import('@/views/price/index.vue'),
    meta: { title: '价格管理', permission: 'dessert:dessert:list' },
  },
  {
    path: 'order',
    name: 'order',
    component: () => import('@/views/order/index.vue'),
    meta: { title: '订单管理', permission: 'order:order:list' },
  },
  {
    path: 'dessert/:id',
    name: 'dessertDetail',
    component: () => import('@/views/dessert/detail.vue'),
    meta: { title: '甜品详情', permission: 'dessert:dessert:list', parent: { path: '/dessert', title: '甜品管理' } },
  },
  {
    path: 'inventory',
    name: 'inventory',
    component: () => import('@/views/inventory/index.vue'),
    meta: { title: '库存管理', permission: 'inventory:inventory:list' },
  },
]
