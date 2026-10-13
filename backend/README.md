# 🍰 甜品店管理系统 (Dessert Shop Management System)

基于 **Spring Boot 3 + MyBatis + MySQL + Redis** 的甜品店后台管理系统后端，提供员工/角色权限、甜品与库存、订单、会员积分、数据看板等功能，并为移动端 H5 提供公开的下单与会员接口。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 22 | 运行环境（编译目标 22） |
| Spring Boot | 3.3.7 | 应用框架 |
| MyBatis | 3.0.5 | ORM 框架 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6+（可选） | 查询缓存，不可用时自动熔断直查数据库 |
| JWT (jjwt) | 0.12.6 | 身份认证 |
| BCrypt | 0.10.2 | 密码加密 |
| PageHelper | 1.4.7 | 分页插件 |
| Knife4j | 4.5.0 | 接口文档（仅开发环境） |
| 阿里云 OSS | 3.17.4 | 文件存储 |
| Lombok | 1.18.42 | 代码简化 |
| H2 | - | 集成测试数据库 |
| Maven | 3.6+ | 项目构建 |

## 项目结构

```
newdessertshop/
├── dessert-parent/                    # 父工程（聚合模块，Maven 构建入口）
│   └── pom.xml
├── dessertshop-pojo/                  # 实体类模块
├── dessertshop-utils/                 # 工具类模块（JWT、OSS、BCrypt）
├── dessertshop-web-management/        # Web 应用模块
│   └── src/main/
│       ├── java/com/itheima/
│       │   ├── controller/            # 控制器
│       │   ├── service/               # 业务层
│       │   ├── mapper/                # 数据访问层
│       │   ├── filter/                # TokenFilter（认证 + 当前用户上下文）
│       │   ├── context/               # CurrentUserHolder、角色加载
│       │   ├── aop/                   # 操作日志切面、权限校验切面
│       │   ├── annotation/            # @LogOperation、@RequirePermission
│       │   ├── interceptor/           # 登录限流
│       │   ├── config/                # Redis、Knife4j、生产环境安全检查
│       │   ├── runner/                # 密码迁移
│       │   └── exception/             # 全局异常处理
│       └── resources/
│           ├── application.yml        # 开发配置
│           ├── application-prod.yml   # 生产配置
│           └── com/itheima/mapper/    # MyBatis XML 映射文件
└── sql/                               # 建表、种子与迁移脚本
```

## 功能模块

### 🔐 认证与权限（RBAC）
- 用户名/密码登录，返回 JWT Token；登录响应附带角色与权限码
- 5 个内置角色：`super_admin` / `manager` / `cashier` / `warehouse` / `finance`，38 个权限码
- 接口通过 `@RequirePermission` 做服务端强校验：未登录 401，无权限 403
- 角色与账号存在性以数据库为准：删除员工后其旧 token 立即失效，角色调整 30 秒内生效
- 登录接口限流；支持修改密码（需验证旧密码，只能改本人）

### 👤 员工与部门 (`/emps`、`/depts`)
- 员工分页查询（按姓名、性别、入职日期筛选）、增删改查、批量删除
- 员工工作经历、个人信息
- 部门增删改查

### 🧁 甜品与库存 (`/desserts`、`/inventory`、`/comments`)
- 甜品分页查询（按名称、分类筛选）、增删改查、上下架
- 调价窄接口 `PUT /desserts/price`
- 原料库存管理、库存调整、库存预警
- 商品评论管理

### 📦 订单 (`/orders`)
- 订单分页查询（按订单号、客户名、状态、日期区间筛选）
- 状态流转经 `PUT /orders/status` 由服务端校验：待支付 → 待接单 → 制作中 → 配送中 → 已完成 / 已取消
- 下单在同一事务内按库内价格重算金额、原子扣减库存并记录销量
- 订单完成时发放会员积分；取消（仅待支付/待接单）时回补库存，已发积分扣回
- 订单号唯一，冲突自动重试；只能删除已完成/已取消的订单

### 🎁 客户、会员与优惠券 (`/customers`、`/members`、`/coupons`)
- 客户管理与客户订单查询
- 会员积分流水、积分兑换、发放优惠券

### 📱 移动端 H5 (`/mobile/**`，无需登录)
- 商品列表、详情、分类
- 下单、按订单号 + 手机号查单
- 会员资料、积分兑换、优惠券、积分流水

### 📊 数据看板与报表 (`/dashboard`、`/report`)
- 统计卡片：月营收、月订单数、客单价、完成率
- 营收趋势（近 7 天 / 30 天）、热销排行、成品库存预警
- 员工性别/职位分布、营收与分类报表

### 📋 操作日志 (`/operateLogs`、`/empLogs`)
- 基于 AOP 自动记录关键操作，参数中的密码字段自动脱敏

### 📤 文件上传 (`POST /upload`)
- 上传至阿里云 OSS；需甜品或员工编辑权限
- 按文件内容（魔数）识别类型，单文件上限 5MB

## 快速开始

### 环境要求

- JDK 22+
- Maven 3.6+
- MySQL 8.0+
- Redis 6+（可选）

### 1. 克隆项目

```bash
git clone https://github.com/YukaKaori/dessertshop-backend.git
cd dessertshop-backend
```

### 2. 初始化数据库

```sql
CREATE DATABASE dessertshop DEFAULT CHARACTER SET utf8mb4;
```

```bash
# 全部表结构（已包含 sql/ 下各迁移脚本的结果）
mysql -u root -p dessertshop < sql/schema_baseline.sql

# 角色与权限种子
mysql -u root -p dessertshop < sql/rbac_init.sql

# 演示数据（可选，生产环境不要导入）
mysql -u root -p dessertshop < sql/orders_and_dessert.sql
```

已有旧库升级时，按顺序执行 `sql/order_stock_and_images.sql`、`sql/order_no_unique.sql`（会删除重复订单，执行前务必备份）、`sql/product_image_cache_headers.sql`。

### 3. 创建管理员账号

仓库不内置任何默认账号。用 BCrypt（cost 12）为自选的强密码生成哈希后执行：

```sql
INSERT INTO emp (username, password, name, gender, phone, create_time, update_time)
VALUES ('<用户名>', '<BCrypt 哈希>', '<姓名>', 1, '<11位手机号>', NOW(), NOW());

INSERT INTO sys_user_role (user_id, role_id)
SELECT e.id, r.id FROM emp e JOIN sys_role r ON r.code = 'super_admin'
WHERE e.username = '<用户名>';
```

### 4. 配置

开发配置在 `dessertshop-web-management/src/main/resources/application.yml`，敏感项通过环境变量覆盖：

| 环境变量 | 说明 |
|------|------|
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号（库地址默认 `localhost:3306/dessertshop`） |
| `JWT_SECRET` | JWT 签名密钥，≥32 字节随机串 |
| `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` | 阿里云 OSS 凭证，上传功能需要 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DATABASE` | Redis 连接，无密码时留空 |
| `CORS_ALLOWED_ORIGINS` | 前端与接口不同源时填写，逗号分隔；默认不开放跨域 |
| `TRUST_FORWARDED_FOR` | 位于会覆盖 `X-Forwarded-For` 的反向代理之后时设为 `true` |

Token 有效期由 `jwt.expire` 配置（毫秒），默认 12 小时。

### 5. 构建并运行

项目根目录没有 pom，构建入口是 `dessert-parent`：

```bash
# 构建
mvn -f dessert-parent/pom.xml clean package -DskipTests

# 运行（开发配置）
java -jar dessertshop-web-management/target/dessertshop-web-management-0.0.1-SNAPSHOT.jar
```

应用启动后监听 `http://localhost:8080`，开发环境接口文档在 `http://localhost:8080/doc.html`。

### 6. 生产部署

```bash
java -jar dessertshop-web-management-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

prod profile 下 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET` 没有默认值，缺失时拒绝启动；使用仓库内置的开发 JWT 密钥同样拒绝启动。prod 还会关闭 SQL 日志、接口文档与静态资源映射，错误响应不回显异常信息。前端由 Nginx 托管，并以 `/api/` 前缀同源转发到后端。

## 测试

```bash
mvn -f dessert-parent/pom.xml test
```

集成测试使用 H2 内存库，覆盖登录、RBAC 权限矩阵、订单 × 库存 × 积分流转、安全加固与 Redis 熔断。

> `DessertshopWebManagementApplicationTests` 与 `PasswordMigrationTest` 未使用 test profile，会直连本机 MySQL。

## API 概览

| 模块 | 路径 | 说明 |
|------|------|------|
| 登录 | `POST /login` | 用户登录 |
| 员工 | `/emps/**` | 员工管理、个人信息、修改密码 |
| 部门 | `/depts/**` | 部门管理 |
| 甜品 | `/desserts/**` | 甜品管理、调价、上下架 |
| 库存 | `/inventory/**` | 原料库存与预警 |
| 评论 | `/comments/**` | 商品评论 |
| 订单 | `/orders/**` | 订单管理、状态流转 |
| 客户 | `/customers/**` | 客户与客户订单 |
| 会员 | `/members/**` | 会员、积分流水、发券 |
| 优惠券 | `/coupons/**` | 优惠券 |
| 看板 | `/dashboard/**` | 数据统计 |
| 报表 | `/report/**` | 数据报表 |
| 日志 | `/operateLogs`、`/empLogs` | 操作日志 |
| 上传 | `POST /upload` | 文件上传 |
| 移动端 | `/mobile/**` | H5 商品、下单、查单、会员（无需登录） |

除 `/login` 与 `/mobile/**` 外，所有接口都需在请求头 `token` 中携带 JWT。

## 安全机制

- **密码加密**：BCrypt 哈希存储，哈希不会出现在接口响应和日志中
- **JWT 认证 + RBAC**：Token 认证，接口级权限码校验，角色以数据库为准
- **防越权**：个人信息、修改密码只作用于当前登录用户
- **登录限流**：默认不信任 `X-Forwarded-For`，防止伪造 IP 绕过
- **上传防护**：需编辑权限，按内容识别文件类型，扩展名由服务端决定
- **操作审计**：AOP 自动记录关键操作，密码参数脱敏
- **输入校验**：Jakarta Validation 校验请求参数；非法请求返回 4xx

## 已知限制

- H5 会员身份只靠手机号识别，没有短信验证
- 没有接入支付，订单不经过真实收款
- 前端尚未按角色过滤菜单与路由（后端已强校验，越权返回 403）；没有角色管理界面，调整角色需直接改库
- 灰度期所有员工都绑定了 `super_admin`，新增员工默认也绑定该角色；退出灰度的 SQL 在 `sql/rbac_init.sql` 文末注释中

## 许可证

本项目仅供学习交流使用。
