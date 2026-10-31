/**
 * 全局常量定义（TypeScript）
 */

/** 下拉选项通用类型 */
export interface SelectOption {
  label: string
  value: number
  type?: string
}

// 职位列表（与数据库 job 字段对应）
export const JOB_LIST: SelectOption[] = [
  { label: '经理', value: 1 },
  { label: '副经理', value: 2 },
  { label: '实习生', value: 3 },
  { label: '线上运营', value: 4 },
  { label: '财务出纳', value: 5 },
]

// 性别列表
export const GENDER_LIST: SelectOption[] = [
  { label: '男', value: 1 },
  { label: '女', value: 2 },
]

// 订单状态
export interface OrderStatusOption {
  label: string
  value: number
  type: string
}

export const ORDER_STATUS: OrderStatusOption[] = [
  { label: '待支付', value: 0, type: 'info' },
  { label: '待接单', value: 1, type: 'warning' },
  { label: '制作中', value: 2, type: '' },
  { label: '配送中', value: 3, type: 'primary' },
  { label: '已完成', value: 4, type: 'success' },
  { label: '已取消', value: 5, type: 'danger' },
]

// 甜品分类
export interface DessertCategoryOption {
  label: string
  value: string
}

export const DESSERT_CATEGORIES: DessertCategoryOption[] = [
  { label: '蛋糕', value: 'cake' },
  { label: '面包', value: 'bread' },
  { label: '饮品', value: 'drink' },
  { label: '甜点', value: 'dessert' },
  { label: '冰淇淋', value: 'icecream' },
]

/**
 * 根据 value 查找 label
 */
export function getLabelByValue(list: SelectOption[] | OrderStatusOption[], value: number): string {
  const item = list.find(i => i.value === value)
  return item ? item.label : '-'
}

/**
 * 根据 value 查找 type
 */
export function getTypeByValue(list: OrderStatusOption[], value: number): string {
  const item = list.find(i => i.value === value)
  return item ? item.type : 'info'
}
