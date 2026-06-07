/**
 * 首页仪表盘路由
 */
export const dashboardRoutes = [
  {
    path: 'index',
    name: 'index',
    component: () => import('@/views/index/index.vue'),
    meta: { title: '首页' },
  },
]
