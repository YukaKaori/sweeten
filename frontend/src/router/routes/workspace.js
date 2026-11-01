/**
 * 门店工作台路由（员工登录后可见，挂在工作台外框 layout 之下）
 *
 * 分组与侧栏 config/navigation.js 一致；meta.permission 与后端 @RequirePermission 的查询权限一致，
 * 无权限时路由守卫不放行。路径（/index、/order、/dessert …）是员工书签与 ⌘K 的入口，保持不变。
 */
export const workspaceRoutes = [
  // —— 概览 ——
  {
    path: 'index',
    name: 'index',
    component: () => import('@/views/workspace/overview/index.vue'),
    meta: { title: '经营概览', permission: 'dashboard:stats:read' },
  },

  // —— 门店运营：订单、客户与会员 ——
  {
    path: 'order',
    name: 'order',
    component: () => import('@/views/workspace/order/index.vue'),
    meta: { title: '订单管理', permission: 'order:order:list' },
  },
  {
    path: 'customer',
    name: 'customer',
    component: () => import('@/views/workspace/customer/index.vue'),
    meta: { title: '客户管理', permission: 'customer:customer:list' },
  },

  // —— 商品与库存：甜品、价格、原料库存 ——
  {
    path: 'dessert',
    name: 'dessert',
    component: () => import('@/views/workspace/dessert/index.vue'),
    meta: { title: '甜品管理', permission: 'dessert:dessert:list' },
  },
  {
    path: 'dessert/:id',
    name: 'dessertDetail',
    component: () => import('@/views/workspace/dessert/detail.vue'),
    meta: { title: '甜品详情', permission: 'dessert:dessert:list', parent: { path: '/dessert', title: '甜品管理' } },
  },
  {
    path: 'price',
    name: 'price',
    component: () => import('@/views/workspace/price/index.vue'),
    meta: { title: '价格管理', permission: 'dessert:dessert:list' },
  },
  {
    path: 'inventory',
    name: 'inventory',
    component: () => import('@/views/workspace/inventory/index.vue'),
    meta: { title: '库存管理', permission: 'inventory:inventory:list' },
  },

  // —— 数据：报表与操作日志 ——
  {
    path: 'report',
    name: 'report',
    component: () => import('@/views/workspace/report/index.vue'),
    meta: { title: '数据报表', permission: 'report:report:read' },
  },
  {
    path: 'log',
    name: 'log',
    component: () => import('@/views/workspace/log/index.vue'),
    meta: { title: '操作日志', permission: 'log:operate:list' },
  },

  // —— 组织：员工与部门 ——
  {
    path: 'emp',
    name: 'emp',
    component: () => import('@/views/workspace/emp/index.vue'),
    meta: { title: '员工管理', permission: 'emp:emp:list' },
  },
  {
    path: 'dept',
    name: 'dept',
    component: () => import('@/views/workspace/dept/index.vue'),
    meta: { title: '部门管理', permission: 'dept:dept:list' },
  },

  // 没有任何页面权限时的去处
  {
    path: 'forbidden',
    name: 'forbidden',
    component: () => import('@/views/workspace/403.vue'),
    meta: { title: '暂无权限' },
  },
]
