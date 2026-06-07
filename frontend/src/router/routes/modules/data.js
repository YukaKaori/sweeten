/**
 * 数据统计路由（报表 + 日志）
 */
export const dataRoutes = [
  {
    path: 'report',
    name: 'report',
    component: () => import('@/views/report/index.vue'),
    meta: { title: '数据报表' },
  },
  {
    path: 'log',
    name: 'log',
    component: () => import('@/views/log/index.vue'),
    meta: { title: '操作日志' },
  },
]
