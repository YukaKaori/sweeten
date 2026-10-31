/**
 * 系统管理路由（部门 / 员工 / 客户）
 */
export const systemRoutes = [
  {
    path: 'dept',
    name: 'dept',
    component: () => import('@/views/dept/index.vue'),
    meta: { title: '部门管理', permission: 'dept:dept:list' },
  },
  {
    path: 'emp',
    name: 'emp',
    component: () => import('@/views/emp/index.vue'),
    meta: { title: '员工管理', permission: 'emp:emp:list' },
  },
  {
    path: 'customer',
    name: 'customer',
    component: () => import('@/views/customer/index.vue'),
    meta: { title: '客户管理', permission: 'customer:customer:list' },
  },
]
