/**
 * 首页仪表盘路由
 */
export const dashboardRoutes = [
  {
    path: 'index',
    name: 'index',
    component: () => import('@/views/index/index.vue'),
    meta: { title: '经营概览', permission: 'dashboard:stats:read' },
  },
  {
    path: 'forbidden',
    name: 'forbidden',
    component: () => import('@/views/403.vue'),
    meta: { title: '暂无权限' },
  },
]
