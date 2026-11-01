# 喜甜 SWEETEN — 后端 `sweeten-server`

**SWEETEN YOUR DAY.** 喜甜线上甜品商城与门店运营的后端：Spring Boot 3.3 + MyBatis + MySQL + Redis，Java 21。
一个可部署单元，内部按业务模块组织（模块化单体），同时服务门店工作台（员工，需 JWT）与顾客商城（`/mobile/**`，公开）。

本目录是 [喜甜 SWEETEN 仓库](https://github.com/YukaKaori/sweeten) 的后端部分，前端在 [`../frontend`](../frontend)，架构说明见 [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)。下文命令均在 `backend/` 目录执行。

## 本地运行

**环境**：JDK 21+、Maven 3.6+、MySQL 8.0+、Node 20.19+（前端用）。Redis 可选，没有时自动直查数据库。

### 1. 建库导数据

建库、表结构、角色权限、演示数据（商品、订单、优惠券）、默认管理员 `admin / 123456` 一次完成，可重复执行：

```bash
mysql -u root -p -e "source sql/init_all.sql"
```

装了 Docker 的话也可以不装 MySQL，直接 `docker compose up -d`：启动 MySQL 8（root / 1234）和 Redis，首次启动自动执行上面的脚本。脚本目录说明见 [sql/README.md](sql/README.md)。

### 2. 启动后端

MySQL 密码不是 `1234` 时，先设置环境变量 `DB_PASSWORD`（用户名不是 `root` 时同时设置 `DB_USERNAME`）。

```bash
mvn clean package -DskipTests
java -jar target/sweeten-server.jar
```

看到 `Started SweetenApplication` 即成功，监听 `http://localhost:8080`（接口文档 `/doc.html`）。

### 3. 启动前端

另开一个终端，进入仓库的 `frontend/` 目录：

```bash
cd ../frontend
npm install
npm run dev
```

打开终端里提示的地址（默认 `http://localhost:5173`），用 `admin / 123456` 登录工作台，`/m` 是顾客商城。

> 图片上传依赖阿里云 OSS 凭证（`OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET`），本地不配置也能正常运行，只是上传会失败。

## 常见问题

| 现象 | 处理 |
|------|------|
| `Access denied for user 'root'` | 设置 `DB_PASSWORD` 为本机 MySQL 密码 |
| `Unknown database 'dessertshop'` | 没执行第 1 步建库 |
| `mysql` 不是内部或外部命令 | 把 MySQL 安装目录下的 `bin` 加入 PATH |
| `Failed to open file 'sql/...'` | 没在 `backend/` 目录执行第 1 步 |
| 登录提示用户名或密码错误 | 第 1 步没执行完整，重新执行（可重复执行） |
| `release version 21 not supported` / `无效的目标发行版: 21` | JDK 版本低于 21 |
| Maven 下载依赖很慢或失败 | 在 Maven `settings.xml` 配置国内镜像 |
| 升级后已登录的工作台提示重新登录 | 开发环境的默认 JWT 密钥已更换（2026-10-31），重新登录即可 |

## 项目结构

```
backend/
├── pom.xml                         # 单一工程 com.sweeten:sweeten-server（产物 target/sweeten-server.jar）
├── src/main/java/com/sweeten/
│   ├── SweetenApplication.java
│   ├── common/      # 统一响应与分页、异常处理、Redis 缓存与熔断、接口文档、CORS 与限流、生产自检
│   ├── identity/    # 员工登录（JWT）、RBAC 角色与权限码、TokenFilter、@RequirePermission、密码迁移
│   ├── staff/       # 员工档案、部门、工作经历
│   ├── catalog/     # 商品（甜品）、定价、上下架、成品库存、评价
│   ├── inventory/   # 原料 / 物料库存
│   ├── order/       # 订单生命周期 OrderStatus、结算 CheckoutService、订单事件
│   ├── customer/    # 客户档案与会员（等级、积分），监听订单事件
│   ├── promotion/   # 优惠券模板与会员持券
│   ├── storefront/  # 顾客商城公开接口 /mobile/**
│   ├── analytics/   # 经营概览与报表（只读统计模型）
│   ├── audit/       # 操作审计与员工变更日志
│   └── media/       # 图片上传（阿里云 OSS）
├── src/main/resources/
│   ├── application.yml / application-prod.yml / application-migration.yml
│   └── mapper/<module>/*.xml       # MyBatis XML，按模块存放
├── src/test/                       # JUnit 5；只用 H2 内存库
├── sql/                            # schema / seed / migrations / archive，见 sql/README.md
└── docker-compose.yml              # 本地 MySQL 8 + Redis
```

每个模块内部为 `web`（控制器）/ `service` / `mapper` / `model`，按需加 `event`、`security` 等专用包。
模块之间只允许单向依赖（依赖表在 `ModuleBoundaryTest`），新增依赖方向前先更新 [架构说明](../docs/ARCHITECTURE.md)。

## 功能

- **认证与权限**：JWT 登录；5 个角色、38 个权限码，`@RequirePermission` 服务端校验（未登录 401，无权限 403）；角色以数据库为准
- **员工与部门**：增删改查、工作经历、修改密码
- **商品与库存**：上下架、调价窄接口、成品库存原子扣减、原料库存与预警、评论
- **订单**：状态流转由 `OrderStatus` 统一校验；结算按库内价格重算并原子扣库存；完成发积分、取消回补库存并扣回积分（订单事件驱动）
- **客户与会员**：积分流水、积分兑换、优惠券、新人券
- **顾客商城**（`/mobile/**`，免登录）：商品、下单、查单、会员中心
- **看板与报表**：营收趋势、热销排行、库存预警、员工统计
- **操作日志**：AOP 记录关键操作，密码参数脱敏

除 `/login` 与 `/mobile/**` 外，接口需在请求头 `token` 中携带 JWT。路径 `/mobile/**` 源自早期的移动端 H5，现为全部顾客端的公开 API，作为对外契约保持不变。

## 配置

开发配置在 `src/main/resources/application.yml`，可用环境变量覆盖：

| 环境变量 | 说明 |
|------|------|
| `DB_URL` | 数据库连接，默认 `jdbc:mysql://localhost:3306/dessertshop` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号，默认 `root` / `1234` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DATABASE` | Redis，默认 `localhost:6379` 无密码、0 号库 |
| `JWT_SECRET` | JWT 签名密钥（开发环境有公开的默认值，prod 拒绝使用） |
| `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` | 阿里云 OSS 凭证，上传功能需要 |
| `OSS_ENDPOINT` / `OSS_BUCKET` / `OSS_REGION` | OSS 地域与 bucket，默认为现有 bucket |

生产配置见 [docs/DEPLOYMENT.md](../docs/DEPLOYMENT.md)。

## 测试

```bash
mvn test
```

全部测试只使用 H2 内存库（`src/test/resources/schema.sql` / `data.sql`），缓存关闭，不会连接本机 MySQL 或写入 Redis：
`TestDatabaseGuard` 会让任何数据源不是 H2 内存库的测试上下文直接失败。新写集成测试时继承 `support.BaseTest`，
需要独立数据的测试用 `@TestPropertySource` 指定自己的 H2 库名（`data.sql` 使用固定主键，不能在同一个库上重复执行）。

`ModuleBoundaryTest` 校验模块依赖方向，`OrderStatusTest` 校验订单流转表与前端一致。

## 生产部署

```bash
java -jar target/sweeten-server.jar --spring.profiles.active=prod
```

prod 下 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET` 必须通过环境变量提供，使用公开过的开发 JWT 密钥会拒绝启动。生产建库只执行 `sql/schema/` 下的脚本，**不要用 `init_all.sql`，也不要导入 `sql/seed/`**（含弱口令账号）。已有旧库升级时按文件名顺序执行 `sql/migrations/`。前端由 Nginx 托管，以 `/api/` 前缀转发到后端。

## 已知限制

- 顾客商城的会员只靠手机号识别，没有短信验证；没有接入支付；下单时还不能使用优惠券
- 前端已按权限码隐藏无权限的菜单与操作，但鉴权始终以后端为准；没有角色管理界面
- 灰度期所有员工都绑定 `super_admin`，退出灰度的 SQL 在 `sql/schema/rbac_init.sql` 文末（执行前需改写，见部署说明）；新员工默认角色在 `RoleAssignmentServiceImpl`

## 许可证

[MIT](../LICENSE)
