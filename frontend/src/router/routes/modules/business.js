/**
 * 业务管理路由（价格 + 订单）
 */
export const businessRoutes = [
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
]
