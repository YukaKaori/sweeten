/**
 * 顾客商城路由（/m/*，全部公开，无需登录）
 * 首页不设标题 → 浏览器标题为「喜甜 SWEETEN · 甜品 · 烘焙 · 饮品」
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
        meta: { public: true },
      },
      {
        path: 'product/:id',
        name: 'mobileProduct',
        component: () => import('@/views/mobile/product/index.vue'),
        meta: { title: '甜品详情', public: true },
      },
      {
        path: 'orders',
        name: 'mobileOrders',
        component: () => import('@/views/mobile/orders/index.vue'),
        meta: { title: '我的订单', public: true },
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
        meta: { title: '订单已提交', public: true },
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
