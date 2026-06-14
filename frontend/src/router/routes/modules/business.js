/**
 * 业务管理路由（价格 + 订单）
 */
export const businessRoutes = [
  {
    path: 'dessert',
    name: 'dessert',
    component: () => import('@/views/dessert/index.vue'),
    meta: { title: '甜品管理' },
  },
  {
    path: 'price',
    name: 'price',
    component: () => import('@/views/price/index.vue'),
    meta: { title: '价格管理' },
  },
  {
    path: 'order',
    name: 'order',
    component: () => import('@/views/order/index.vue'),
    meta: { title: '订单管理' },
  },
  {
    path: 'dessert/:id',
    name: 'dessertDetail',
    component: () => import('@/views/dessert/detail.vue'),
    meta: { title: '甜品详情' },
  },
  {
    path: 'inventory',
    name: 'inventory',
    component: () => import('@/views/inventory/index.vue'),
    meta: { title: '库存管理' },
  },
]
