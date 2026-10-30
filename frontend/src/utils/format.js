/**
 * 展示格式化工具（金额 / 时间 / 数量），顾客端与工作台共用
 */

/** ¥128.00；无效值显示 ¥0.00 */
export function formatPrice(value, { symbol = true, decimals = 2 } = {}) {
  const n = Number(value)
  const text = (Number.isFinite(n) ? n : 0).toFixed(decimals)
  return symbol ? `¥${text}` : text
}

/** 带千分位的金额：¥12,860.50 */
export function formatMoney(value, decimals = 2) {
  const n = Number(value)
  const safe = Number.isFinite(n) ? n : 0
  return `¥${safe.toLocaleString('zh-CN', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })}`
}

/** 千分位整数：1,024 */
export function formatCount(value) {
  const n = Number(value)
  return (Number.isFinite(n) ? n : 0).toLocaleString('zh-CN')
}

/** 原价高于现价时返回节省金额（字符串，两位小数），否则空串 */
export function savingOf(product) {
  const price = Number(product?.price)
  const original = Number(product?.originalPrice)
  if (!Number.isFinite(price) || !Number.isFinite(original) || original <= price) return ''
  return (original - price).toFixed(2)
}

const pad = (n) => String(n).padStart(2, '0')

function toDate(value) {
  if (!value) return null
  const d = value instanceof Date ? value : new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(d.getTime()) ? null : d
}

/** 2026-10-30 14:05 */
export function formatDateTime(value) {
  const d = toDate(value)
  if (!d) return value ? String(value).replace('T', ' ').slice(0, 16) : ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 10月30日 14:05（今年内省略年份） */
export function formatShortDateTime(value) {
  const d = toDate(value)
  if (!d) return value ? String(value).replace('T', ' ').slice(5, 16) : ''
  const sameYear = d.getFullYear() === new Date().getFullYear()
  const date = sameYear ? `${d.getMonth() + 1}月${d.getDate()}日` : `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
  return `${date} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 刚刚 / 5 分钟前 / 3 小时前 / 超过一天显示短日期 */
export function formatRelative(value) {
  const d = toDate(value)
  if (!d) return ''
  const diff = (Date.now() - d.getTime()) / 1000
  if (diff < 0) return formatShortDateTime(d)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  return formatShortDateTime(d)
}

/** 按时段问候 */
export function greeting(date = new Date()) {
  const h = date.getHours()
  if (h < 5) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 13) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
}
