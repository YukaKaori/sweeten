/**
 * 顾客商城路由（/m/*，全部公开，无需登录）
 * 首页不设标题 → 浏览器标题为「喜甜 SWEETEN · 甜品 · 烘焙 · 饮品」
 *
 * /m 是对外公开的商城地址（顾客书签、分享链接、二维码），保持不变。
 */
export const storefrontRoutes = [
  {
    path: '/m',
    component: () => import('@/views/storefront/layout/index.vue'),
    meta: { public: true },
    children: [
      {
        path: '',
        name: 'storefrontHome',
        component: () => import('@/views/storefront/home/index.vue'),
        meta: { public: true },
      },
      {
        path: 'product/:id',
        name: 'storefrontProduct',
        component: () => import('@/views/storefront/product/index.vue'),
        meta: { title: '甜品详情', public: true },
      },
      {
        path: 'orders',
        name: 'storefrontOrders',
        component: () => import('@/views/storefront/orders/index.vue'),
        meta: { title: '我的订单', public: true },
      },
      {
        path: 'checkout',
        name: 'storefrontCheckout',
        component: () => import('@/views/storefront/checkout/index.vue'),
        meta: { title: '确认订单', public: true },
      },
      {
        path: 'success',
        name: 'storefrontSuccess',
        component: () => import('@/views/storefront/success/index.vue'),
        meta: { title: '订单已提交', public: true },
      },
      {
        path: 'profile',
        name: 'storefrontProfile',
        component: () => import('@/views/storefront/profile/index.vue'),
        meta: { title: '会员中心', public: true },
      },
    ],
  },
]
