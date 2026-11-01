# 喜甜 SWEETEN 架构说明

> 现行架构（2026-10-31 起）。本文描述代码的真实结构；早期的诊断与分阶段计划见 [ARCHITECTURE_PLAN.md](ARCHITECTURE_PLAN.md)（含历次 Changelog）。

喜甜 SWEETEN 是**单门店、线上优先**的甜品电商：顾客在线上商城浏览、下单、查单、积累会员权益；门店员工在工作台接单、制作、配送，并管理商品、库存、会员与数据。

## 1. 运行形态

```
 顾客浏览器 ──  /m/*  ──┐                         ┌──▶ MySQL   库名 dessertshop（唯一事实来源）
                        ├─▶ Nginx ── /api/* ──▶ sweeten-server ─┼──▶ Redis   只读缓存，可选；不可用时熔断直查库
 员工浏览器 ──  /, /login ┘  （托管前端 dist）                    └──▶ 阿里云 OSS  商品图 / 头像
```

| 部件 | 技术 | 位置 |
|---|---|---|
| 前端（一个 SPA，两个界面） | Vue 3 + Vite + Pinia + Element Plus | `frontend/` |
| 后端（一个可部署单元） | Spring Boot 3.3 + MyBatis，Java 21 | `backend/`，产物 `backend/target/sweeten-server.jar` |
| 数据库脚本 | MySQL 8+ | `backend/sql/`（见其 README） |
| 文档 | | `docs/` |

## 2. 后端：模块化单体

### 2.1 为什么是模块化单体

- 单门店、单团队、一个数据库：拆成微服务只会增加部署、事务与一致性成本，换不来收益。
- 但"一个包里放所有 controller / service / mapper / pojo"的教学项目结构，让商品、订单、会员、员工、权限互相缠绕（改动订单要碰会员服务、权限代码依赖员工档案、报表直接伸进各业务表）。
- 折中：**一个可部署单元，按业务模块组织代码**。`com.sweeten` 下每个一级包是一个模块，模块之间只允许单向依赖，由 `ModuleBoundaryTest` 在每次构建时校验。以后真需要拆分（例如独立的通知 / 配送回调进程），边界已经划好。

原先的四个 Maven 模块（`dessert-parent` / `dessertshop-pojo` / `dessertshop-utils` / `dessertshop-web-management`）是按"技术种类"切的：实体全在 pojo、工具全在 utils，没有任何隔离价值，已合并为单一工程 `com.sweeten:sweeten-server`，依赖版本与运行时类库保持不变（打包产物逐个比对过）。

### 2.2 模块与职责

| 模块 | 职责 | 对外接口 | 拥有的表 |
|---|---|---|---|
| `common` | 统一响应 `Result` / 分页、异常与全局处理、Redis 缓存与熔断、接口文档、CORS 与限流、生产启动自检 | — | — |
| `identity` | 员工认证（JWT）、RBAC 角色与权限码、`TokenFilter` / `@RequirePermission`、角色绑定、密码迁移 | `/login`、`PUT /emps/password`（实现） | `sys_role`、`sys_permission`、`sys_role_permission`、`sys_user_role`；`emp` 的账号凭证列 |
| `staff` | 员工档案、部门、工作经历 | `/emps`、`/depts` | `emp`（档案列）、`emp_expr`、`dept` |
| `catalog` | 商品（甜品）、定价、上下架、成品库存（可售量）、评价 | `/desserts`、`/comments` | `dessert`、`comments` |
| `inventory` | 原料 / 物料库存与预警 | `/inventory` | `inventory` |
| `order` | 订单生命周期（`OrderStatus`）、结算下单（`CheckoutService`）、订单事件 | `/orders` | `orders` |
| `customer` | 客户档案（CRM）与会员体系（等级、积分、成长值），响应订单事件 | `/customers`、`/members` | `customers`、`member`、`points_log` |
| `promotion` | 优惠券模板与会员持券 | `/coupons` | `coupon_template`、`member_coupon` |
| `storefront` | 顾客商城公开 API：组合 catalog / order / customer / promotion，只做适配，不含业务规则 | `/mobile/**` | — |
| `analytics` | 经营概览与报表的只读模型（自己的 `AnalyticsMapper` 跨表统计） | `/dashboard/**`、`/report/**` | 只读 |
| `audit` | 操作审计（`@LogOperation` 切面）与员工变更日志 | `/operateLogs`、`/empLogs` | `operate_log`、`emp_log` |
| `media` | 图片上传与对象存储适配（阿里云 OSS） | `/upload` | — |

### 2.3 依赖方向

```
                storefront
               /    |     \
       customer ── promotion \
           |                  \
         order ───────────▶ catalog          analytics（只读统计，不依赖业务模块代码）
           |                   |
   staff · inventory · media · promotion · catalog
                     \     |     /
                       audit
                         |
                      identity
                         |
                       common
```

规则（`backend/src/test/java/com/sweeten/ModuleBoundaryTest.java`）：

- 依赖只能自上而下，不允许环。新增模块或依赖方向时，先在本文说明理由，再更新测试中的依赖表。
- `order` 不依赖 `customer`：订单通过**领域事件**通知会员体系（见 2.5）。这条原先是双向依赖。
- `identity` 不依赖 `staff`：认证通过自己的 `AccountMapper` 读取 `emp` 表的账号列；`staff` 新增 / 删除员工时调用 `RoleAssignmentService`。原先登录与签发 JWT 写在员工服务里、权限过滤器又依赖员工 Mapper，是双向依赖。
- `analytics` 只通过自己的 SQL 读表，不调用业务模块；统计口径变化不影响业务模块。
- `storefront` 只做编排与对外数据裁剪，业务规则留在各自模块。

### 2.4 模块内部约定

```
com.sweeten.<module>
├── web/        REST 控制器（HTTP 适配）
├── service/    业务服务接口与实现
├── mapper/     MyBatis Mapper 接口；XML 在 resources/mapper/<module>/XxxMapper.xml
├── model/      实体、查询参数、请求 / 响应 DTO
└── event/ · security/ · aspect/ · storage/   按需的专用包
```

- 模块之间优先调用对方的 `service`；跨模块直接用对方 `mapper` 的地方（如 `customer` 发券用 `promotion` 的 `CouponMapper`）是待收口的技术债。
- 接口路径、JSON 字段名、权限码是对外契约，代码重组不改动它们（2026-10-31 用新旧两版的 OpenAPI 文档逐个接口比对：76 个接口的路径、方法、参数、请求体字段零差异）。

### 2.5 订单生命周期与事件

`order.model.OrderStatus` 是状态码与流转规则的唯一来源。前端 `utils/constants.js` 的 `ORDER_STATUS` / `ORDER_STATUS_FLOW` 是它的镜像，后端 `OrderStatusTest` 按同一张流转表断言，改动任一侧都要同步另一侧：

```
 待支付(0) ──支付确认──▶ 待接单(1) ──接单──▶ 制作中(2) ──出餐──▶ 配送中(3) ──送达──▶ 已完成(4)
     └────────取消────────┴───取消───▶ 已取消(5)          （制作中之后不可取消）
```

| 事件 | 何时发布 | 当前监听方 | 规划中的监听方 |
|---|---|---|---|
| `OrderPlacedEvent` | 结算成功、库存已扣减（下单事务内） | 会员：按手机号查找 / 注册，发新人券（保存点内执行，失败不影响下单） | 新单提醒、门店打印 |
| `OrderStatusChangedEvent` | 每次合法流转后（流转事务内；取消时库存已回补） | 会员：完成发积分、取消扣回 | 履约排产、配送派单、顾客通知 |

约定：同步监听方与发布方同属一个事务，失败会回滚整次流转；**对外部系统**（短信、支付、配送平台）的调用一律用 `@TransactionalEventListener(phase = AFTER_COMMIT)`，避免事务回滚后已对外生效。

### 2.6 横切关注点

- **安全**：`TokenFilter` 校验 JWT，并以数据库为准取当前角色（30 秒缓存，删除员工即时失效）→ `CurrentUserHolder` → `@RequirePermission` 切面校验权限码（`super_admin` 直接放行）。`/login` 与 `/mobile/**` 为公开接口；登录按 IP 限流。
- **审计**：`@LogOperation` 记录写操作（参数中的密码脱敏），操作人取自 `CurrentUserHolder`。
- **缓存**：只缓存商品列表 / 详情与经营概览，键前缀 `sweeten:`，写操作在事务提交后清除；Redis 故障时熔断直查数据库。
- **配置**：开发默认值在 `application.yml`；`prod` profile 中密钥类配置没有默认值，并拒绝使用仓库内公开过的开发 JWT 密钥。环境变量见 [DEPLOYMENT.md](DEPLOYMENT.md)。
- **测试**：只用 H2 内存库；`TestDatabaseGuard` 让任何指向非 H2 数据源的测试上下文立即失败。

## 3. 前端：一个应用，两个界面

```
frontend/src/
├── views/
│   ├── storefront/      顾客商城 /m/*：home · product · checkout · success · orders · profile · layout（公开，品牌浅色）
│   ├── workspace/       门店工作台 /：overview · order · customer · dessert · price · inventory · report · log · emp · dept
│   │                    · layout · login · 403（员工登录，按权限码过滤，支持深色「可可夜」）
│   └── 404.vue
├── components/
│   ├── storefront/      CartPanel · ProductCard · QtyStepper
│   ├── workspace/       PageHeader · Breadcrumb · CommandPalette · NotificationPanel · UserProfilePanel
│   ├── brand/           BrandLogo · SwIcon
│   └── *.vue            两个界面共用：ProductImg · StatusBadge · EmptyState · Skeleton*
├── api/modules/         storefront.js = 公开的 /mobile/** 接口；其余为工作台接口（需员工令牌）
├── router/routes/       storefront.js · workspace.js（分组与侧栏一致）· static.js（登录、404）
├── stores/modules/      商城：cart · myOrders · storefrontUi；工作台：user（权限判断）· app
├── config/              brand（品牌文案）· navigation（工作台菜单单一来源）· productImage
└── utils/               http（统一 /api 前缀）· constants（状态与分类，与后端一致）· storage（存储键迁移）…
```

- 两个界面的边界体现在目录上：商城页面不引用工作台组件，反之亦然；共享的只有设计系统、品牌组件与少数通用组件。
- 所有请求经 `/api` 前缀（开发：Vite 代理；生产：Nginx）转发到同一后端。
- 设计系统与品牌资源见 `frontend/BRAND.md`；未参与构建的原始素材在 `frontend/design-assets/`。

## 4. 线上业务的关键关系：现状与扩展点

尚未确定支付服务商、配送服务商与最终的经营地域配置，因此下面只确定**模块边界与接入点**，不绑定具体服务商。

| 关系 | 现状 | 扩展方式 |
|---|---|---|
| 商品 ↔ 规格 / 选项 | `dessert` 一行就是一个可售单品（无尺寸、口味等规格）；订单明细 `OrderLine` 保存下单时的商品名与单价快照 | 在 `catalog` 增加规格 / 选项表，可售单位改为"商品 + 规格"；`OrderLine` 增加规格快照字段（JSON 明细向后兼容） |
| 定价 ↔ 促销 | 价格只归 `catalog`（调价走窄接口 `PUT /desserts/price`）；结算由服务端按商品库价格重算，与客户端不一致即拒单；券模板与持券归 `promotion`，**下单时还不能用券**（`CouponMapper.useCoupon` 尚无调用方） | 结算流程：`catalog` 给出小计 → `promotion` 计算优惠并锁定券 → 订单保存应付金额与优惠快照；取消订单时由 `promotion` 监听事件退券 |
| 订单 ↔ 支付交易 | 未接入支付，下单直接进入「待接单」；`OrderStatus` 已有「待支付」及 0→1 流转 | 新增 `payment` 模块：`payment_transaction` 表（订单、渠道、金额、状态、渠道流水号、幂等键）+ `PaymentGateway` 接口与具体渠道适配器；下单进入「待支付」，支付回调幂等地推进到「待接单」；超时未付由定时任务取消并回补库存 |
| 库存预留 ↔ 库存变动 | 下单在事务内原子扣减成品库存（`dessert.stock`）并记销量，取消回补；原料库存（`inventory`）与商品没有配方关联 | 接入支付后，"下单扣减 + 超时释放"即预留语义；需要审计时增加库存流水表；原料与产品的配方（BOM）放在未来的生产模块 |
| 产能 ↔ 配送时段 | 没有时段与产能概念，地址为自由文本 | 新增 `fulfillment` 模块：可选时段与每时段产能，结算时选择时段并占用产能；监听订单事件排产 |
| 顾客身份 ↔ 会员 | 顾客没有登录体系，会员只凭手机号识别（上线公众服务前的阻断项）；`customers`（员工维护的客户档案）与 `member`（下单自动注册）是两套记录 | 在 `identity` 增加顾客身份（短信验证码 / 微信登录），与员工账号分开；之后以顾客 ID 关联订单与会员，并合并两套客户记录 |
| 履约 ↔ 配送状态 | `OrderStatus` 覆盖 制作中 / 配送中 / 已完成，由员工在工作台推进 | 新增 `delivery` 模块：配送单（骑手、渠道、轨迹、签收）与 `DeliveryProvider` 接口；监听 `OrderStatusChangedEvent` 派单，配送回调推进订单状态 |
| 地域假设 | 已外置：数据库连接（`DB_URL`）、OSS 地域与 bucket（`OSS_ENDPOINT` / `OSS_BUCKET` / `OSS_REGION`）。仍写在代码里：中国大陆手机号格式、人民币金额、服务器本地时区 | 确定经营地域后统一收进配置（`app.region.*`），时间统一按门店时区处理 |

## 5. 数据库

- 库名 `dessertshop` 与全部表名保持不变；脚本按用途分为 `schema/`（结构与必需参考数据）、`seed/`（仅本地演示）、`migrations/`（已有库的日期化增量）、`archive/`（历史脚本，禁止执行），说明见 `backend/sql/README.md`。
- 没有迁移工具，按序手工执行；建议下一步引入 Flyway（`migrations/` 可直接转为版本化脚本）。

## 6. 标识与兼容性

品牌与工程标识统一为 SWEETEN；**外部可见或已持久化**的标识保持不变，以免目录重组变成不兼容的应用迁移。

| 标识 | 处理 | 说明 |
|---|---|---|
| Java 包 `com.itheima` | **已迁移** → `com.sweeten.<module>` | 内部标识。审计日志里历史记录的类名仍是旧值；按类名搜索（如 `DeptController`）新旧记录都能命中 |
| Maven `com.itheima:dessert-parent` 等四个模块 | **已迁移** → `com.sweeten:sweeten-server` | 构建：`mvn -f backend/pom.xml …` |
| 可执行包 `dessertshop-web-management-0.0.1-SNAPSHOT.jar` | **已迁移** → `sweeten-server.jar` | 部署脚本需改用新文件名 |
| `spring.application.name`、前端 `package.json` 名称 | **已迁移** → `sweeten-server`、`sweeten-web` | |
| Redis 缓存键 `dessert:list::…` | **已迁移** → `sweeten:dessert:list::…` | 旧键中含旧类名、无法反序列化；新前缀不读旧键，旧键按 TTL（≤10 分钟）自然过期 |
| 本地开发 JWT 默认密钥 | **已迁移** | 仅开发环境：升级后需重新登录一次；prod 同时拒绝新旧两个公开默认值 |
| 浏览器存储 `dessertshop-color-mode` / `dessertshop-recent-pages` / `ds-remember-username` | **已迁移** → `sw-*` | 首次加载时自动带上旧值，用户设置不丢失 |
| 库名 `dessertshop`、表名、列名 | 保留 | 已有数据所在；如需新库名设置 `DB_URL` |
| 全部接口路径（含 `/mobile/**`）、JSON 字段、权限码 | 保留 | 对外契约；`/mobile/**` 现为顾客商城公开 API |
| 商城地址 `/m`、工作台路由路径 | 保留 | 顾客书签、二维码与员工书签 |
| 订单号前缀 `DS` | 保留 | 已有订单号与顾客查单凭据；如需新前缀须同时评估查单与对账 |
| 浏览器存储 `cart_items` / `my_orders` / `checkout_contact` / `last_order_phone` / `loginUser` | 保留 | 顾客购物车、订单记录与员工登录态 |
| 环境变量 | 保留并扩充 | 新增可选：开发环境 `DB_URL`、`OSS_ENDPOINT` / `OSS_BUCKET` / `OSS_REGION` |
| OSS bucket `yuka-javaweb` | 保留（可配置） | 已上传图片的 URL 指向它 |
| docker compose 项目与数据卷 | 保留 | compose 文件位置未变，已有本地数据卷继续可用 |
| `/mobile.html` | 保留地址，页面退役 | 旧版独立 H5 商城已由 `/m` 取代，地址重定向到 `/m` |

## 7. 下一步

按"先补上线阻断项、再扩展线上能力"的顺序：

1. **RBAC 收口**：默认角色改为按职位映射（只需改 `RoleAssignmentServiceImpl`），重写并执行灰度退出 SQL（当前写法会删掉没有 job 的唯一管理员），增加角色管理界面。
2. **顾客身份**：短信验证码 / 微信登录，取代"凭手机号识别会员"；`/mobile/**` 增加限流，会员资料接口最小化返回字段。
3. **支付**（`payment` 模块）：渠道无关的支付网关接口、交易流水、回调幂等、超时取消与库存释放。
4. **结算用券**：把 `promotion` 接入结算，订单保存优惠快照。
5. **履约与配送**（`fulfillment`、`delivery`）：时段与产能、配送单与状态回传，基于订单事件接入。
6. **工程**：引入 Flyway；为 `orders(status, create_time)` 等高频查询补索引；把 `scripts/smoke/` 的端到端冒烟脚本接入 CI（需把 `puppeteer-core` 列为前端开发依赖）；清理未使用的前端依赖（如 `ogl`）。
