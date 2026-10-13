# 🍰 甜品店管理系统 — 后端

Spring Boot 3 + MyBatis + MySQL + Redis 的甜品店后台管理系统：员工/角色权限、甜品与库存、订单、会员积分、数据看板，以及移动端 H5 的下单与会员接口。

- 后端（本仓库）：https://gitee.com/yuka-kaori/newdessertshop
- 前端：https://gitee.com/yuka-kaori/dessertshop-frontend

## 本地运行

**环境**：JDK 22+、Maven 3.6+、MySQL 8.0+、Node 20.19+（前端用）。Redis 可选，没有时自动直查数据库。

### 1. 建库导数据

在本仓库根目录执行（建库、表结构、角色权限、演示数据、默认管理员 `admin / 123456` 一次完成）：

```bash
mysql -u root -p -e "source sql/init_all.sql"
```

装了 Docker 的话也可以不装 MySQL，直接 `docker compose up -d`：启动 MySQL 8（root / 1234）和 Redis，首次启动自动执行上面的脚本。

### 2. 启动后端

MySQL 密码不是 `1234` 时，先设置环境变量 `DB_PASSWORD`（用户名不是 `root` 时同时设置 `DB_USERNAME`）。

```bash
mvn -f dessert-parent/pom.xml clean package -DskipTests
java -jar dessertshop-web-management/target/dessertshop-web-management-0.0.1-SNAPSHOT.jar
```

看到 `Started DessertshopWebManagementApplication` 即成功，监听 `http://localhost:8080`（接口文档 `/doc.html`）。

### 3. 启动前端

另开一个终端，在后端仓库目录之外执行：

```bash
git clone https://gitee.com/yuka-kaori/dessertshop-frontend.git
cd dessertshop-frontend
npm install
npm run dev
```

打开终端里提示的地址（默认 `http://localhost:5173`），用 `admin / 123456` 登录。

> 图片上传依赖阿里云 OSS 凭证（`OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET`），本地不配置也能正常运行，只是上传会失败。

## 常见问题

| 现象 | 处理 |
|------|------|
| `Access denied for user 'root'` | 设置 `DB_PASSWORD` 为本机 MySQL 密码 |
| `Unknown database 'dessertshop'` | 没执行第 1 步建库 |
| `mysql` 不是内部或外部命令 | 把 MySQL 安装目录下的 `bin` 加入 PATH |
| `Failed to open file 'sql/...'` | 没在仓库根目录执行第 1 步 |
| 登录提示用户名或密码错误 | 第 1 步没执行完整，重新执行（可重复执行） |
| `release version 22 not supported` / `无效的目标发行版: 22` | JDK 版本低于 22 |
| Maven 下载依赖很慢或失败 | 在 Maven `settings.xml` 配置国内镜像 |
| `npm install` 提示 `EBADENGINE`，或 `npm run dev` 报错 | Node 版本过低，升级到 20.19+ 或 22.12+ |

## 项目结构

```
dessert-parent/               # 父工程，Maven 构建入口（根目录没有 pom）
dessertshop-pojo/             # 实体类
dessertshop-utils/            # JWT、OSS、BCrypt 工具
dessertshop-web-management/   # Web 应用（controller / service / mapper / aop / filter ...）
sql/                          # 建表、种子与迁移脚本
```

## 功能

- **认证与权限**：JWT 登录；5 个角色、38 个权限码，`@RequirePermission` 服务端校验（未登录 401，无权限 403）
- **员工与部门**：增删改查、工作经历、修改密码
- **甜品与库存**：上下架、调价、原料库存与预警、评论
- **订单**：状态流转由服务端校验；下单按库内价格重算并原子扣库存；完成发积分、取消回补库存
- **客户与会员**：积分流水、积分兑换、优惠券
- **移动端 H5**（`/mobile/**`，免登录）：商品、下单、查单、会员
- **看板与报表**：营收趋势、热销排行、库存预警
- **操作日志**：AOP 记录关键操作，密码参数脱敏

除 `/login` 与 `/mobile/**` 外，接口需在请求头 `token` 中携带 JWT。

## 配置

开发配置在 `dessertshop-web-management/src/main/resources/application.yml`，可用环境变量覆盖：

| 环境变量 | 说明 |
|------|------|
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号，默认 `root` / `1234`，库地址 `localhost:3306/dessertshop` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis，默认 `localhost:6379` 无密码 |
| `JWT_SECRET` | JWT 签名密钥（开发环境有默认值） |
| `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` | 阿里云 OSS，上传功能需要 |

## 测试

```bash
mvn -f dessert-parent/pom.xml test
```

集成测试使用 H2 内存库。`DessertshopWebManagementApplicationTests` 与 `PasswordMigrationTest` 会直连本机 MySQL。

## 生产部署

```bash
java -jar dessertshop-web-management-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

prod 下 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET` 必须通过环境变量提供，使用开发默认 JWT 密钥会拒绝启动。生产建库请分步执行 `schema_baseline.sql`、`rbac_init.sql`，**不要用 `init_all.sql`，也不要导入 `dev_seed_admin.sql`**（含默认弱口令账号）。前端由 Nginx 托管，以 `/api/` 前缀转发到后端。已有旧库升级时按顺序执行 `sql/order_stock_and_images.sql`、`sql/order_no_unique.sql`（会删除重复订单，先备份）、`sql/product_image_cache_headers.sql`。

## 已知限制

- H5 会员只靠手机号识别，没有短信验证；没有接入支付
- 前端未按角色过滤菜单（后端已强校验）；没有角色管理界面
- 灰度期所有员工都绑定 `super_admin`，退出灰度的 SQL 在 `sql/rbac_init.sql` 文末

## 许可证

仅供学习交流使用。
