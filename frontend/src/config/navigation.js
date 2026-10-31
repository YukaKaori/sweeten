/**
 * 门店工作台导航 —— 侧栏与 ⌘K 命令面板共用的单一来源
 *
 * permission 与对应路由 meta.permission、后端 @RequirePermission 一致；
 * 当前员工没有权限的入口不会显示（后端仍做最终校验）。
 */
export const NAV_GROUPS = [
  {
    title: '',
    items: [
      { path: '/index', label: '经营概览', icon: 'home', permission: 'dashboard:stats:read', desc: '今日与本月经营数据', keywords: 'home dashboard 首页 概览 仪表盘' },
    ],
  },
  {
    title: '门店运营',
    items: [
      { path: '/order', label: '订单管理', icon: 'receipt', permission: 'order:order:list', desc: '接单、制作、配送与完成', keywords: 'order 订单 接单' },
      { path: '/customer', label: '客户管理', icon: 'user', permission: 'customer:customer:list', desc: '客户资料与消费记录', keywords: 'customer 客户 顾客' },
    ],
  },
  {
    title: '商品与库存',
    items: [
      { path: '/dessert', label: '甜品管理', icon: 'cake', permission: 'dessert:dessert:list', desc: '上新、编辑、图片与库存', keywords: 'dessert 甜品 商品 蛋糕 面包 饮品' },
      { path: '/price', label: '价格管理', icon: 'tag', permission: 'dessert:dessert:list', desc: '调价与上下架', keywords: 'price 价格 调价 上架 下架' },
      { path: '/inventory', label: '库存管理', icon: 'box', permission: 'inventory:inventory:list', desc: '原料、物料与补货', keywords: 'inventory 库存 原料 物料' },
    ],
  },
  {
    title: '数据',
    items: [
      { path: '/report', label: '数据报表', icon: 'chart', permission: 'report:report:read', desc: '营收趋势与销售分析', keywords: 'report 报表 数据 统计' },
      { path: '/log', label: '操作日志', icon: 'doc', permission: 'log:operate:list', desc: '关键操作审计', keywords: 'log 日志 审计' },
    ],
  },
  {
    title: '组织',
    items: [
      { path: '/emp', label: '员工管理', icon: 'users', permission: 'emp:emp:list', desc: '员工资料与账号', keywords: 'emp 员工 人事' },
      { path: '/dept', label: '部门管理', icon: 'building', permission: 'dept:dept:list', desc: '组织架构', keywords: 'dept 部门' },
    ],
  },
]

export const NAV_ITEMS = NAV_GROUPS.flatMap((g) => g.items)

/** 当前员工可见的导航分组（过滤掉无权限项与空分组） */
export function visibleNavGroups(can) {
  return NAV_GROUPS.map((g) => ({ ...g, items: g.items.filter((i) => can(i.permission)) })).filter((g) => g.items.length)
}

/** 第一个有权限的页面（登录后落地 / 无权限时的去处） */
export function firstAllowedPath(can) {
  return NAV_ITEMS.find((i) => can(i.permission))?.path || '/forbidden'
}
