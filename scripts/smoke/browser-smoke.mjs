// SWEETEN 前端浏览器冒烟检查（顾客商城下单全流程 + 工作台全部页面 + 旧地址兼容）
//
//   SWEETEN_WEB=http://127.0.0.1:4173 node scripts/smoke/browser-smoke.mjs
//
// 前提：
//   1. 后端已启动并连接用 init_all.sql 建出的本地 / 一次性数据库（需要演示商品与 admin / 123456）；
//   2. 前端以生产构建运行：cd frontend && npm run build && npx vite preview（/api 按 vite.config.js 代理到后端）；
//   3. 已安装 puppeteer-core（尚未列为项目依赖）：cd frontend && npm i -D puppeteer-core；
//      Chrome 路径可用 CHROME_PATH 指定。
// 会写数据：下一笔测试订单、注册一个随机手机号的测试会员。只允许指向本机地址。
import { createRequire } from 'node:module'

const BASE = process.env.SWEETEN_WEB || 'http://127.0.0.1:4173'
if (!['localhost', '127.0.0.1', '::1'].includes(new URL(BASE).hostname) && !process.env.SWEETEN_SMOKE_ALLOW_REMOTE) {
  console.error(`拒绝运行：${BASE} 不是本机地址（本脚本会提交测试订单）`)
  process.exit(2)
}
let puppeteer
try {
  puppeteer = createRequire(new URL('../../frontend/package.json', import.meta.url))('puppeteer-core')
} catch {
  console.error('未找到 puppeteer-core：请先在 frontend/ 执行 npm i -D puppeteer-core')
  process.exit(2)
}
const CHROME = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe'

let pass = 0
let fail = 0
const check = (name, ok, detail = '') => {
  if (ok) { pass++; console.log(`  ok   ${name}`) } else { fail++; console.log(`  FAIL ${name} ${detail}`) }
}

const browser = await puppeteer.launch({ executablePath: CHROME, headless: true, args: ['--no-first-run', '--no-default-browser-check'] })
const page = await browser.newPage()
await page.setViewport({ width: 1366, height: 900 })
const problems = []
page.on('pageerror', (e) => problems.push(`pageerror @${page.url()}: ${e.message}`))
page.on('console', (m) => { if (m.type() === 'error') problems.push(`console @${page.url()}: ${m.text()}`) })

const go = (path) => page.goto(BASE + path, { waitUntil: 'domcontentloaded' })
const path = () => page.evaluate(() => location.pathname)
const appText = () => page.evaluate(() => document.querySelector('#app')?.innerText || '')
const ls = (k) => page.evaluate((key) => localStorage.getItem(key), k)
const waitText = (re, timeout = 15000) =>
  page.waitForFunction((src) => new RegExp(src).test(document.querySelector('#app')?.innerText || ''), { timeout }, re.source)
const phone = '137' + String(Math.floor(Math.random() * 1e8)).padStart(8, '0')

try {
  console.log('[本地存储] 旧品牌键名迁移，设置不丢失')
  await go('/m')
  await page.evaluate(() => {
    localStorage.clear()
    localStorage.setItem('ds-remember-username', 'admin')
    localStorage.setItem('dessertshop-color-mode', 'dark')
    localStorage.setItem('dessertshop-recent-pages', JSON.stringify(['/order']))
  })
  await go('/login')
  await page.waitForSelector('input[autocomplete="username"]')
  await page.waitForFunction(() => document.querySelector('input[autocomplete="username"]').value === 'admin', { timeout: 5000 })
    .then(() => check('记住的用户名在改键名后仍在', true))
    .catch(() => check('记住的用户名在改键名后仍在', false))
  check('sw-remember-username 已写入、旧键已删除', (await ls('sw-remember-username')) === 'admin' && (await ls('ds-remember-username')) === null)
  check('sw-color-mode 已写入、旧键已删除', (await ls('sw-color-mode')) === 'dark' && (await ls('dessertshop-color-mode')) === null)

  console.log('[工作台] 员工登录与全部页面')
  await page.type('input[autocomplete="current-password"]', '123456')
  await page.keyboard.press('Enter')
  await page.waitForFunction(() => location.pathname !== '/login', { timeout: 15000 })
  check('登录后进入工作台', (await path()) === '/index', await path())
  check('⌘K 最近访问的键名已迁移', (await ls('sw-recent-pages')) !== null && (await ls('dessertshop-recent-pages')) === null)
  await page.waitForSelector('a[href="/order"]', { timeout: 15000 })
  for (const p of ['/index', '/order', '/customer', '/dessert', '/dessert/1', '/price', '/inventory', '/report', '/log', '/emp', '/dept']) {
    await go(p)
    try {
      await page.waitForFunction(() => (document.querySelector('#app')?.innerText || '').length > 80, { timeout: 15000 })
      check(`工作台 ${p}`, (await path()) === p && !/页面不存在|暂无权限/.test(await appText()), `at ${await path()}`)
    } catch (e) {
      check(`工作台 ${p}`, false, e.message)
    }
  }

  console.log('[顾客商城] 菜单 → 详情 → 结算 → 下单成功 → 我的订单 → 会员中心')
  await go('/m')
  await page.waitForSelector('a[href^="/m/product/"]', { timeout: 20000 })
  const hrefs = await page.$$eval('a[href^="/m/product/"]', (as) => [...new Set(as.map((a) => a.getAttribute('href')))])
  check('菜单渲染商品', hrefs.length > 0, String(hrefs.length))
  await go(hrefs[0])
  const product = await page.evaluate(async (id) => (await (await fetch(`/api/mobile/products/${id}`)).json()).data, hrefs[0].split('/').pop())
  await waitText(new RegExp(product.name)).then(() => check('商品详情页', true)).catch(() => check('商品详情页', false))

  await page.evaluate((p) => {
    localStorage.setItem('cart_items', JSON.stringify([{ id: p.id, name: p.name, price: p.price, originalPrice: p.originalPrice, icon: p.icon, image: p.image, stock: p.stock, qty: 1 }]))
  }, product)
  await go('/m/checkout')
  await page.waitForSelector('#co-name')
  await page.type('#co-name', '浏览器测试')
  await page.type('#co-phone', phone)
  await page.type('#co-address', '测试路 2 号')
  let clicked = false
  for (const b of await page.$$('button')) {
    const label = await b.evaluate((el) => el.innerText)
    const visible = await b.evaluate((el) => !!(el.offsetWidth || el.offsetHeight))
    if (visible && label.includes('提交订单')) { await b.click(); clicked = true; break }
  }
  check('找到提交订单按钮', clicked)
  await page.waitForFunction(() => location.pathname === '/m/success', { timeout: 20000 })
    .then(() => check('下单后进入成功页', true))
    .catch(async () => check('下单后进入成功页', false, `${await path()} ${(await appText()).slice(0, 160)}`))
  const orderNo = await page.evaluate(() => new URLSearchParams(location.search).get('orderNo'))
  check('成功页显示 DS 订单号', /^DS\d{18}$/.test(orderNo || ''), orderNo)
  await go('/m/orders')
  await waitText(new RegExp(orderNo)).then(() => check('我的订单能查到新订单', true)).catch(() => check('我的订单能查到新订单', false))
  await go('/m/profile')
  await waitText(/新人/, 20000).then(() => check('会员中心显示新人券', true)).catch(() => check('会员中心显示新人券', false))

  console.log('[兼容] 旧地址')
  await go('/mobile.html')
  await page.waitForFunction(() => location.pathname === '/m', { timeout: 10000 })
    .then(() => check('/mobile.html 跳转到 /m', true)).catch(async () => check('/mobile.html 跳转到 /m', false, await path()))
  await go('/no-such-page')
  await waitText(/页面不存在|404/).then(() => check('未知路径显示 404 页', true)).catch(() => check('未知路径显示 404 页', false))
} catch (e) {
  fail++
  console.log('  FAIL (中止)', e.message)
} finally {
  await browser.close()
}

const relevant = problems.filter((p) => !/Failed to load resource|net::ERR|favicon/i.test(p))
console.log(`\n控制台 / 页面错误：共 ${problems.length} 条，非网络类 ${relevant.length} 条`)
relevant.slice(0, 15).forEach((p) => console.log('   ', p.slice(0, 220)))
console.log(`${pass} passed, ${fail} failed`)
process.exitCode = fail || relevant.length ? 1 : 0
