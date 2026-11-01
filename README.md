<p align="center">
  <img src="frontend/public/brand/sweeten-logo-horizontal.svg" alt="喜甜 SWEETEN — SWEETEN YOUR DAY." width="520">
</p>

# 喜甜 SWEETEN

**SWEETEN YOUR DAY.** 让每一天，甜一点。

喜甜是一家**线上优先**的单门店甜品品牌（甜品 · 烘焙 · 饮品）。本仓库是它的完整系统：

- **顾客商城** `/m`：浏览菜单、结算下单（服务端核价、实时扣库存）、凭订单号查单、会员积分与优惠券。
- **门店工作台** `/`：接单与履约（待接单 → 制作中 → 配送中 → 已完成）、商品与价格、原料库存、客户与会员、经营数据、员工与权限。

| 目录 | 内容 |
|---|---|
| [`frontend/`](frontend/) | Vue 3 + Vite + Pinia + Element Plus 单页应用：`views/storefront`（顾客商城）与 `views/workspace`（门店工作台） |
| [`backend/`](backend/) | `sweeten-server`：Spring Boot 3 + MyBatis 的模块化单体（Java 21）；[`backend/sql/`](backend/sql/) 为建库、种子与迁移脚本 |
| [`docs/`](docs/) | [架构说明](docs/ARCHITECTURE.md)、[部署说明](docs/DEPLOYMENT.md)、运维手册与历史文档 |

## 架构一览

```
 顾客 /m ──┐                              ┌── MySQL（库名 dessertshop）
           ├── Nginx ── /api ──▶ sweeten-server ──┼── Redis（可选缓存）
 员工 /  ──┘                              └── 阿里云 OSS（图片）
```

后端按业务划分模块：`catalog`（商品与定价）· `order`（订单生命周期与结算）· `customer`（客户与会员）· `promotion`（优惠券）·
`inventory`（原料库存）· `storefront`（顾客商城公开 API）· `staff`（员工与部门）· `identity`（认证与权限）·
`analytics`（经营分析）· `audit`（审计）· `media`（图片）· `common`。模块依赖单向、无环，由测试校验；订单通过领域事件驱动会员积分，
支付、履约、配送等后续能力也从这些事件接入。详见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 本地运行

需要 JDK 21+、Maven 3.6+、MySQL 8.0+、Node 20.19+ 或 22.12+；Redis 可选（不可用时自动直查数据库）。

1. 建库并导入演示数据（同时创建**仅供本地开发**的管理员 `admin / 123456`）：

   ```bash
   cd backend
   mysql -u root -p -e "source sql/init_all.sql"
   ```

   装了 Docker 也可以在 `backend/` 执行 `docker compose up -d`，启动 MySQL 8 与 Redis 并自动导入。

2. 启动后端（`http://localhost:8080`，接口文档 `/doc.html`）：

   ```bash
   mvn -f backend/pom.xml clean package -DskipTests
   java -jar backend/target/sweeten-server.jar
   ```

3. 另开终端启动前端：

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   打开 `http://localhost:5173`：`/m` 是顾客商城，`/login` 是员工登录。

更多说明见 [frontend/README.md](frontend/README.md) 与 [backend/README.md](backend/README.md)，生产部署见 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)。

## 测试

```bash
mvn -f backend/pom.xml test        # 后端：只使用 H2 内存库，不会连接本机 MySQL
cd frontend && npx eslint src --ext .vue,.js
```

端到端冒烟检查（会写入测试订单，只能指向本地 / 一次性环境）见 [scripts/README.md](scripts/README.md)。

## 配置与密钥

仓库不保存任何生产密钥。

- **生产**：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET` 必须通过环境变量提供，prod profile 缺少任何一项都会拒绝启动，使用仓库里公开过的开发 JWT 密钥同样会拒绝启动。阿里云 OSS 凭证只从环境变量 `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` 读取。
- **本地开发默认值**：数据库 `root / 1234`、开发 JWT 密钥、`backend/sql/seed/dev_seed_admin.sql` 中的 `admin / 123456` 仅用于本机运行与演示，不能用于任何对外环境。
- 前端 `frontend/.env*` 只包含公开配置（标题、接口前缀、公开的商品图地址）。

## 品牌与标识

品牌规范、色彩与标志资源见 [frontend/BRAND.md](frontend/BRAND.md)，标志与图标全部由 `frontend/scripts/brand/build_brand_assets.py` 生成。

工程标识统一为 SWEETEN：Java 包 `com.sweeten`、Maven `com.sweeten:sweeten-server`、前端包 `sweeten-web`。
为兼容已有数据与外部契约，以下标识**保持不变**：数据库名 `dessertshop` 与表名、全部接口路径（含顾客商城的 `/mobile/**`）、商城地址 `/m`、订单号前缀 `DS`、环境变量名。完整清单与原因见 [架构说明 §6](docs/ARCHITECTURE.md#6-标识与兼容性)。

## 仓库历史

本仓库于 2026-10-30 由原前端仓库 dessertshop-frontend 与原后端仓库 dessertshop-backend 合并而成，两者的完整提交历史分别保留在 `frontend/` 与 `backend/` 路径下；2026-10-31 的结构调整全部以文件移动完成，可用 `git log --follow <文件>` 追溯单个文件的历史。原仓库保持不变，作为历史备份。

## 许可证

[MIT](LICENSE)
