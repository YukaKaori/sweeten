// 职位列表（与数据库 job 字段对应）
export const JOB_LIST = [
  { label: '经理', value: 1 },
  { label: '副经理', value: 2 },
  { label: '实习生', value: 3 },
  { label: '线上运营', value: 4 },
  { label: '财务出纳', value: 5 }
]

// 性别列表
export const GENDER_LIST = [
  { label: '男', value: 1 },
  { label: '女', value: 2 }
]

// 订单状态（前端模拟，数据库无此表）
export const ORDER_STATUS = [
  { label: '待支付', value: 0, type: 'info' },
  { label: '待接单', value: 1, type: 'warning' },
  { label: '制作中', value: 2, type: '' },
  { label: '配送中', value: 3, type: 'primary' },
  { label: '已完成', value: 4, type: 'success' },
  { label: '已取消', value: 5, type: 'danger' }
]

// 甜品分类（与后端 Dessert.category 校验正则的 7 类一一对应；color 为全站统一的分类主题色）
export const DESSERT_CATEGORIES = [
  { label: '蛋糕', value: 'cake', color: '#e8637a' },
  { label: '面包', value: 'bread', color: '#f0a35c' },
  { label: '饮品', value: 'drink', color: '#6b8cce' },
  { label: '甜点', value: 'dessert', color: '#5cb88a' },
  { label: '冰淇淋', value: 'icecream', color: '#a78bfa' },
  { label: '小食', value: 'snack', color: '#c98a5b' },
  { label: '礼盒', value: 'gift', color: '#d06c9a' }
]

// 分类 → 主题色
export const CATEGORY_COLORS = Object.fromEntries(DESSERT_CATEGORIES.map(c => [c.value, c.color]))

// 订单状态流转（与后端 OrderServiceImpl.STATUS_FLOW 一致，后端为准）
export const ORDER_STATUS_FLOW = {
  0: [1, 5],
  1: [2, 5],
  2: [3],
  3: [4],
  4: [],
  5: []
}

/**
 * 订单明细展示：移动端订单存 JSON（[{productName, price, qty}]），历史订单为 "名称 x数量" 文本
 * @returns {{ name: string, qty: number|null, price: number|null }[]}
 */
export const parseOrderItems = (items) => {
  if (!items) return []
  const text = String(items).trim()
  if (text.startsWith('[')) {
    try {
      return JSON.parse(text).map(i => ({
        name: i.productName || i.name || '商品',
        qty: Number(i.qty) || null,
        price: i.price != null ? Number(i.price) : null
      }))
    } catch {
      // 解析失败按纯文本处理
    }
  }
  return text.split(/[,，]\s*/).filter(Boolean).map(part => {
    const m = part.match(/^(.*?)\s*[x×*]\s*(\d+)$/i)
    return m ? { name: m[1], qty: Number(m[2]), price: null } : { name: part, qty: null, price: null }
  })
}

// 订单明细单行摘要："草莓慕斯 ×2，抹茶拿铁 ×1"
export const formatOrderItems = (items) =>
  parseOrderItems(items).map(i => (i.qty ? `${i.name} ×${i.qty}` : i.name)).join('，') || '-'

// 根据 value 查找 label
export const getLabelByValue = (list, value) => {
  const item = list.find(i => i.value === value)
  return item ? item.label : '-'
}

// 根据 value 查找 type
export const getTypeByValue = (list, value) => {
  const item = list.find(i => i.value === value)
  return item ? item.type : 'info'
}
