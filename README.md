<p align="center">
  <img src="frontend/public/brand/sweeten-logo-horizontal.svg" alt="喜甜 SWEETEN — SWEETEN YOUR DAY." width="520">
</p>

# 喜甜 SWEETEN

**SWEETEN YOUR DAY.** 让每一天，甜一点。

喜甜（甜品 · 烘焙 · 饮品）门店的完整系统：面向顾客的线上商城（浏览、下单、查单、会员积分），以及面向员工的门店工作台（订单、商品、库存、会员、报表、员工与权限）。本仓库包含前端、后端、数据库脚本与文档。

| 目录 | 内容 |
|---|---|
| [`frontend/`](frontend/) | Vue 3 + Vite + Pinia + Element Plus：顾客商城 `/m` 与门店工作台 `/` |
| [`backend/`](backend/) | Spring Boot 3 + MyBatis + MySQL + Redis；[`backend/sql/`](backend/sql/) 为建库、种子与迁移脚本 |
| [`docs/`](docs/) | [部署说明与就绪评估](docs/DEPLOYMENT.md)、[模块边界与 RBAC 方案](docs/ARCHITECTURE_PLAN.md) |

## 本地运行

需要 JDK 22+、Maven 3.6+、MySQL 8.0+、Node 20.19+ 或 22.12+；Redis 可选（不可用时自动直查数据库）。

1. 建库并导入演示数据（同时创建**仅供本地开发**的管理员 `admin / 123456`）：

   ```bash
   cd backend
   mysql -u root -p -e "source sql/init_all.sql"
   ```

   装了 Docker 也可以在 `backend/` 执行 `docker compose up -d`，启动 MySQL 8 与 Redis 并自动导入。

2. 启动后端（`http://localhost:8080`）：

   ```bash
   mvn -f dessert-parent/pom.xml clean package -DskipTests
   java -jar dessertshop-web-management/target/dessertshop-web-management-0.0.1-SNAPSHOT.jar
   ```

3. 另开终端启动前端：

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   打开 `http://localhost:5173`：`/m` 是顾客商城，`/login` 是员工登录。

更多说明见 [frontend/README.md](frontend/README.md) 与 [backend/README.md](backend/README.md)，生产部署见 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)。

## 配置与密钥

仓库不保存任何生产密钥。

- **生产**：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET` 必须通过环境变量提供，prod profile 缺少任何一项都会拒绝启动，使用仓库内置的开发 JWT 密钥同样会拒绝启动。阿里云 OSS 凭证只从环境变量 `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` 读取。
- **本地开发默认值**：数据库 `root / 1234`、开发 JWT 密钥、`backend/sql/dev_seed_admin.sql` 中的 `admin / 123456` 仅用于本机运行与演示，不能用于任何对外环境。
- 前端 `frontend/.env*` 只包含公开配置（标题、接口前缀、公开的商品图地址）。

## 品牌与技术标识

品牌规范、色彩与标志资源见 [frontend/BRAND.md](frontend/BRAND.md)：标志矢量母版与 PNG 导出在 [`frontend/public/brand/`](frontend/public/brand/)，网站图标与应用图标在 `frontend/public/`，全部由 `frontend/scripts/brand/build_brand_assets.py` 生成。

品牌名只出现在界面与文档中；技术标识保持不变：Java 包名、Maven 坐标、数据库名 `dessertshop`、接口路径、环境变量名与订单号前缀 `DS`。

## 仓库历史

本仓库于 2026-10-30 由原前端仓库 dessertshop-frontend 与原后端仓库 dessertshop-backend 合并而成，两者的完整提交历史分别保留在 `frontend/` 与 `backend/` 路径下（`git log -- frontend/` 可查看前端历史）。原仓库保持不变，作为历史备份。

## 许可证

[MIT](LICENSE)
