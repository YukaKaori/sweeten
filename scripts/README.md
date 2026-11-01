# 开发脚本

仓库级的验证脚本（跨前后端）。前端品牌资源生成脚本在 `frontend/scripts/brand/`。

## smoke/ —— 端到端冒烟检查

2026-10-31 结构调整时用来验证"重组没有改变行为"的脚本，可在每次较大改动后复跑。
**它们会写入测试数据（下单、注册测试会员），只允许指向本机地址**；后端必须连接本地或一次性的数据库，绝不能是生产库。

| 脚本 | 依赖 | 检查内容 |
|---|---|---|
| `api-smoke.mjs` | Node 20+ | 员工登录与权限、商城菜单 / 详情、服务端核价下单与扣库存、下单事件注册会员与新人券、查单、履约流转与积分、全部统计接口、审计日志 |
| `browser-smoke.mjs` | Node 20+、Chrome、`puppeteer-core`（在 `frontend/` 执行 `npm i -D puppeteer-core`） | 本地存储键迁移、工作台登录与全部页面、商城菜单 → 结算 → 成功页 → 我的订单 → 会员中心、`/mobile.html` 跳转、404；并统计控制台错误 |
| `diff-openapi.mjs` | Node 20+ | 比较两个后端版本的 `/v3/api-docs`：路径、方法、参数与请求体字段 |

```bash
# 1) 一个用 init_all.sql 建好的本地库 + 启动后端（默认 8080）
node scripts/smoke/api-smoke.mjs http://127.0.0.1:8080

# 2) 前端生产构建 + 预览（/api 代理到后端）
cd frontend && npm run build && npx vite preview --port 4173
SWEETEN_WEB=http://127.0.0.1:4173 node scripts/smoke/browser-smoke.mjs
```

需要真实 MySQL 又不想碰本机已有数据时，可以起一个一次性的实例：
`mysqld --no-defaults --initialize-insecure --datadir=<临时目录>`，再以 `--no-defaults --port=3317 --bind-address=127.0.0.1` 启动，
执行 `source sql/init_all.sql` 后让后端用 `DB_URL=jdbc:mysql://127.0.0.1:3317/dessertshop` 连接（`--no-defaults` 防止读到本机正式配置）。

登录接口按 IP 每分钟最多 10 次，连续快速运行脚本会被限流（429），稍等一分钟即可。
