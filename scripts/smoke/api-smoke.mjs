// SWEETEN 后端端到端冒烟检查（Node 20+，无第三方依赖）
//
//   node scripts/smoke/api-smoke.mjs [http://127.0.0.1:8080]
//
// 前提：后端连接的是用 backend/sql/init_all.sql 建出的本地 / 一次性数据库（需要演示商品与 admin / 123456）。
// 会写数据：下一笔测试订单并推进到「已完成」，注册一个随机手机号的测试会员。只允许指向本机地址。
// 登录接口按 IP 每分钟限 10 次，本脚本每次运行登录 3 次，连续运行过快会得到 429。

const BASE = process.argv[2] || process.env.SWEETEN_API || 'http://127.0.0.1:8080'
const host = new URL(BASE).hostname
if (!['localhost', '127.0.0.1', '::1'].includes(host) && !process.env.SWEETEN_SMOKE_ALLOW_REMOTE) {
  console.error(`拒绝运行：${BASE} 不是本机地址（本脚本会写入测试订单与会员）`)
  process.exit(2)
}

let pass = 0
let fail = 0
const check = (name, ok, detail = '') => {
  if (ok) { pass++; console.log(`  ok   ${name}`) } else { fail++; console.log(`  FAIL ${name} ${detail}`) }
}
async function call(method, path, { body, token, query } = {}) {
  const url = new URL(BASE + path)
  for (const [k, v] of Object.entries(query || {})) url.searchParams.set(k, v)
  const res = await fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { token } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON 响应 */ }
  return { status: res.status, json, text }
}

async function main() {
console.log('[identity] 员工登录')
const bad = await call('POST', '/login', { body: { username: 'admin', password: 'wrong-password' } })
check('错误密码被拒绝', bad.status === 200 && bad.json?.code === 0)
const noPwd = await call('POST', '/login', { body: { username: 'admin' } })
check('缺少密码返回业务错误而不是 500', noPwd.status === 200 && noPwd.json?.code === 0, `status=${noPwd.status}`)
const login = await call('POST', '/login', { body: { username: 'admin', password: '123456' } })
const token = login.json?.data?.token
check('admin 登录签发 JWT', !!token, login.status === 429 ? '（被登录限流，稍后再试）' : '')
if (!token) return // 无法登录时后续检查都没有意义
check('角色为 super_admin 且带权限码', JSON.stringify(login.json.data.roles) === '["super_admin"]' && login.json.data.permissions.length > 0)
const noToken = await call('GET', '/orders', { query: { page: 1, pageSize: 1 } })
check('工作台接口无令牌 → 401', noToken.status === 401)

console.log('[storefront] 浏览')
const products = await call('GET', '/mobile/products', { query: { category: 'all' } })
const allDesserts = (await call('GET', '/desserts', { token, query: { page: 1, pageSize: 100 } })).json.data.rows
const onSale = allDesserts.filter((d) => d.status === 1).length
check(`菜单只列在售商品（${onSale}/${allDesserts.length}）`, products.json?.code === 1 && products.json.data.length === onSale)
const cats = await call('GET', '/mobile/categories')
check('分类列表', cats.json?.code === 1 && cats.json.data[0].key === 'all')
const [p1, p2] = products.json.data
const detail = await call('GET', `/mobile/products/${p1.id}`)
check('商品详情', detail.json?.data?.id === p1.id)

console.log('[order] 结算 → 下单事件 → 会员')
const phone = '139' + String(Math.floor(Math.random() * 1e8)).padStart(8, '0')
const before = (await call('GET', `/desserts/${p1.id}`, { token })).json.data.stock
const total = Math.round((p1.price * 2 + p2.price) * 100) / 100
const placed = await call('POST', '/mobile/orders', {
  body: {
    customerName: '冒烟测试', phone, address: '测试路 1 号', totalAmount: total,
    items: [
      { productId: p1.id, productName: p1.name, price: p1.price, qty: 2 },
      { productId: p2.id, productName: p2.name, price: p2.price, qty: 1 },
    ],
  },
})
const orderNo = placed.json?.data?.orderNo
check('下单成功，订单号为 DS 开头', /^DS\d{18}$/.test(orderNo || ''), JSON.stringify(placed.json))
const after = (await call('GET', `/desserts/${p1.id}`, { token })).json.data.stock
check('库存原子扣减（-2）', after === before - 2, `${before} -> ${after}`)
const tampered = await call('POST', '/mobile/orders', {
  body: { customerName: '冒烟测试', phone, address: '测试路 1 号', totalAmount: 0.01,
    items: [{ productId: p1.id, productName: p1.name, price: 0.01, qty: 1 }] },
})
check('篡改的客户端价格被拒绝', tampered.json?.code === 0)
const profile = await call('GET', '/mobile/member/profile', { query: { phone } })
check('OrderPlacedEvent 注册了会员', profile.json?.data?.exists === true)
check('新会员获得新人券', (profile.json?.data?.coupons || []).some((c) => /新人/.test(c.couponName)))
const lookup = await call('GET', `/mobile/orders/${orderNo}`, { query: { phone } })
check('订单号 + 手机号查单', lookup.json?.data?.status === 1)
const wrongPhone = await call('GET', `/mobile/orders/${orderNo}`, { query: { phone: '13900000000' } })
check('手机号不匹配 → 404', wrongPhone.status === 404)

console.log('[order] 履约流转 → 会员积分')
const listed = await call('GET', '/orders', { token, query: { page: 1, pageSize: 5, orderNo } })
const orderId = listed.json?.data?.rows?.[0]?.id
check('工作台能查到该订单', !!orderId)
const skip = await call('PUT', '/orders/status', { token, query: { id: orderId, status: 4 } })
check('非法跳转 待接单 → 已完成 被拒绝', skip.json?.code === 0)
const pointsBefore = (await call('GET', '/members/by-phone', { token, query: { phone } })).json?.data?.points ?? 0
for (const s of [2, 3, 4]) {
  const r = await call('PUT', '/orders/status', { token, query: { id: orderId, status: s } })
  check(`流转 → ${s}`, r.json?.code === 1, JSON.stringify(r.json))
}
const member = await call('GET', '/members/by-phone', { token, query: { phone } })
check('OrderStatusChangedEvent 发放积分（1 元 1 分）', member.json?.data?.points - pointsBefore === Math.trunc(total),
  `增加 ${member.json?.data?.points - pointsBefore}，应为 ${Math.trunc(total)}`)
const cancelDone = await call('PUT', '/orders/status', { token, query: { id: orderId, status: 5 } })
check('已完成的订单不能取消', cancelDone.json?.code === 0)

console.log('[analytics] 只读统计模型')
for (const path of ['/dashboard/stats', '/dashboard/revenue', '/dashboard/ranking', '/dashboard/stock-alert',
  '/dashboard/report-stats', '/dashboard/report-revenue', '/dashboard/report-category', '/dashboard/report-weekday',
  '/dashboard/report-ranking', '/report/empGenderData', '/report/empJobData']) {
  const r = await call('GET', path, { token })
  check(`GET ${path}`, r.status === 200 && r.json?.code === 1, `status=${r.status} ${r.text.slice(0, 120)}`)
}

console.log('[audit] 操作日志')
const logs = await call('GET', '/operateLogs', { token, query: { page: 1, pageSize: 5 } })
const latest = logs.json?.data?.rows?.[0]
check('写入审计日志（新类名）', latest?.className === 'com.sweeten.order.web.OrderController', latest?.className)
check('操作人取自登录上下文', latest?.operateEmpId === login.json.data.id, `${latest?.operateEmpId}`)
}

await main()
console.log(`\n${pass} passed, ${fail} failed`)
// 用 exitCode 而不是 process.exit()：Windows 上 fetch 的连接尚在关闭时调用 process.exit() 会触发 libuv 断言崩溃
process.exitCode = fail ? 1 : 0
