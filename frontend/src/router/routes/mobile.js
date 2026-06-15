/**
 * 移动端路由（全部为公开路由，无需登录）
 */
export const mobileRoutes = [
  {
    path: '/m',
    component: () => import('@/views/mobile/layout/index.vue'),
    meta: { public: true },
    children: [
      {
        path: '',
        name: 'mobileHome',
        component: () => import('@/views/mobile/home/index.vue'),
        meta: { title: '甜品点餐', public: true },
      },
      {
        path: 'checkout',
        name: 'mobileCheckout',
        component: () => import('@/views/mobile/checkout/index.vue'),
        meta: { title: '确认订单', public: true },
      },
      {
        path: 'success',
        name: 'mobileSuccess',
        component: () => import('@/views/mobile/success/index.vue'),
        meta: { title: '下单成功', public: true },
      },
      {
        path: 'profile',
        name: 'mobileProfile',
        component: () => import('@/views/mobile/profile/index.vue'),
        meta: { title: '会员中心', public: true },
      },
    ],
  },
]
