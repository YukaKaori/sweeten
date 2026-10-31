# ARCHITECTURE_PLAN — 模块边界 & RBAC 权限体系诊断与改造方案

> 生成日期:2026-07-10
> 性质:**纯诊断与方案文档,本轮不修改任何代码**
> 依据:通读全部 7 份既有报告(PROJECT_SUMMARY / DESSERTSHOP_MASTER_PLAN / NEXT_TASKS / SHOWCASE_REPORT / DEGRADATION_CHECK / IMPROVEMENT_REPORT / SECURITY_UPGRADE+PASSWORD_MIGRATION)+ 对前后端代码逐文件核对
> 路径约定:前端 = `dessertshop/src/...`,后端 = `newdessertshop/dessertshop-web-management/src/main/java/com/itheima/...`,POJO = `newdessertshop/dessertshop-pojo/.../pojo/...`
>
> **合并仓库后的路径对应**（2026-10-30）：文中 `dessertshop/` 即 `frontend/`，`newdessertshop/` 即 `backend/`。IMPROVEMENT_REPORT 在 `frontend/`，SECURITY_UPGRADE / PASSWORD_MIGRATION 在 `backend/`；PROJECT_SUMMARY、DESSERTSHOP_MASTER_PLAN、NEXT_TASKS、SHOWCASE_REPORT、DEGRADATION_CHECK 是早期工作文档，内容已被本文与现行代码取代，未纳入本仓库。

---

## 0. 全局结论(TL;DR)

| 问题 | 结论 | 严重度 |
|---|---|---|
| 问题一:模块边界 | **价格模块是甜品模块的"无头分身"**(无自己的 API/模型/控制器);**存在两套互不相干的"库存"**(dessert.stock 成品库存 vs inventory.stock 原料库存),仪表盘的"库存预警"读的是前者、库存页读的是后者;订单以 JSON 字符串存明细、以姓名字符串关联客户,导致销量/扣库存/客户统计三条链路全部断裂 | 🔴 |
| 问题二:RBAC | **全链路零角色**:数据库无角色表、Emp 无 role 字段、JWT 只带 `{id, username}`、TokenFilter 只验 token 有效性、前端守卫只判断"是否登录";前端存在一个**永远返回 false 的死权限脚手架** `usePermission.js`;既有报告中的 IDOR(任意改他人密码/查他人资料)与 RBAC 缺失是**同一根因**——服务端没有"当前用户是谁、能做什么"的授权层 | 🔴 |
| 对已完成视觉改造的影响 | **有两个共享触点必须协调**:`router/guards.js`(View Transitions 逻辑所在文件)和 `views/layout/index.vue` 侧边栏(glass-pill 导航胶囊所在)。登录页已封版,登录**流程** JS 需扩展存储角色但**模板/样式零改动**。Phase 3 的导航重分组是唯一会真正改变导航栏结构的改动,需单独确认 | 🟡 |

---

# 问题一:业务模块边界冲突

## 1.1 各模块现状盘点(数据模型 / API / 页面路由)

| 模块 | 后端数据模型 | 后端 API 前缀(Controller) | 前端 API 模块 | 前端路由 | 侧边栏分组 |
|---|---|---|---|---|---|
| 甜品 dessert | `Dessert.java`(含 `price/originalPrice/status/**stock**/**sales**`,category 正则 7 类) | `/desserts`(DessertController) | `api/modules/dessert.js` | `/dessert`、`/dessert/:id`(business.js) | 甜品管理 |
| **价格 price** | **无**(无 Price POJO) | **无**(无 PriceController) | **无**(无 price.js) | `/price`(business.js) | 甜品管理 |
| 订单 order | `Order.java`(`customerName` String、`items` String(JSON 文本)、`amount` Double、`status` Int) | `/orders`(OrderController)、`/mobile/orders` | `api/modules/order.js`、`mobile.js` | `/order`(business.js) | 甜品管理 |
| 库存 inventory | `Inventory.java`(原料:`name/category/stock/unit/safetyThreshold/supplier/expiryDate`) | `/inventory`(InventoryController) | `api/modules/inventory.js` | `/inventory`(business.js) | 甜品管理 |
| 客户 customer | `Customer.java` + member/points/coupon 表 | `/customers`、`/member`、`/coupon` | `customer.js`、`member.js` | `/customer`(**system.js**) | **系统管理** |
| 员工 emp | `Emp.java`(`job` Int、`salary`、`deptId`,**无 role**) | `/emps`(EmpController、LoginController) | `emp.js`、`auth.js` | `/emp`(system.js) | 系统管理 |
| 部门 dept | `Dept.java` | `/depts` | `dept.js` | `/dept`(system.js) | 系统管理 |
| 仪表盘 dashboard | 无自有模型,跨模块聚合 | `/dashboard`(DashboardController) | `dashboard.js` | `/index` | 首页 |

**依赖/调用关系图(现状):**

```
前端 views/price ──────┐
                        ├──> api/modules/dessert.js ──> 后端 /desserts (DessertController)
前端 views/dessert ────┘                                      │
                                                              ▼
前端 views/inventory ──> inventory.js ──> /inventory     dessert 表(stock/sales 孤儿列)
                                              │                ▲
                                              ▼                │ (直接注入 DessertMapper,越过 Service)
                                        inventory 表 <── DashboardServiceImpl ──> "库存预警"卡片
                                        (自己的 /inventory/alerts 预警,与上面不是同一数据!)

前端 views/order ──> order.js ──> /orders ──> orders 表
                                     │  items=JSON字符串 ──✂── 与 dessert 表无外键 → 扣库存/记销量不可能
                                     │  customerName=字符串 ──✂── 与 customers 无外键
CustomerServiceImpl ──selectOrdersByCustomerName(姓名匹配)──> orders 表
```

## 1.2 冲突清单(逐条,附代码位置证据)

### 冲突 ①:价格模块是甜品模块的无头 UI 分身 —— 同一数据两个入口维护

- **证据 A**:`dessertshop/src/views/price/index.vue:7` —— 价格页的全部 API 都 import 自 `@/api/modules/dessert`(`queryByCategoryApi / queryCategoryCountApi / updateApi / updateStatusApi`)。项目中**不存在** `api/modules/price.js`,后端**不存在** PriceController(controller 目录 16 个文件已逐一核对)。
- **证据 B(整对象覆写风险)**:`price/index.vue:84-87` `openEdit` 做 `editForm.value = { ...item }`,`:89-104` `savePrice` 把**整个甜品对象**发给 `PUT /desserts`(`DessertController.java:69-76`,文档自述"修改甜品信息(含价格调整)")。调价对话框只展示 3 个字段,但提交的是列表页快照的全字段——若另一人此刻在 `/dessert` 页改了描述/图片,价格页点"确认修改"会把旧值写回去(**后改覆盖先改,无乐观锁**)。
- **证据 C(功能重复)**:`price/index.vue:106-116` `toggleStatus` 调 `PUT /desserts/status`,与甜品管理页的上下架完全同源同能力;`:213-220` 价格卡片上的"评论"按钮直接 `router.push('/dessert/${item.id}')` 跳回甜品模块详情页——价格页自己也承认这些能力属于甜品域。
- **证据 D(分身还失真)**:`price/index.vue:119-125` `categoryColors` 只硬编码 5 个分类(cake/bread/drink/dessert/icecream),而后端 `Dessert.java` 的 category 校验正则是 **7 类**(多 snack、gift)——snack/gift 分类的卡片在价格页拿不到主题色。两处口径已经在漂移,这正是"同一数据两处维护"的典型症状。

### 冲突 ②:两套互不相干的"库存",共用同一个名字"库存预警"

- **证据 A(两套模型)**:`Dessert.java` 有 `stock`(成品库存)+ `sales`(销量)字段;`Inventory.java` 是**原料**库存(`unit/safetyThreshold/supplier/expiryDate`)。二者无任何关联(无配方/BOM 表)。
- **证据 B(两个预警口径)**:库存模块的预警是 `InventoryController.java:36-42` `GET /inventory/alerts`(按 `safetyThreshold` 判断**原料**);而首页仪表盘的"库存预警"是 `DashboardServiceImpl.java:46` 和 `:109` 的 `dessertMapper.selectLowStockAlerts(20)`(按硬编码阈值 20 判断**成品 dessert.stock**)。同一个界面词"库存预警",两处读的是完全不同的表——用户在库存页补了原料,首页预警不会消;首页报警的商品在库存页根本查不到。
- **证据 C(dessert.stock 是孤儿数据)**:
  - 前端 `views/dessert/index.vue` 的新增/编辑表单字段为 名称/分类/描述/原价/现价/图片/状态——**没有库存输入项**,即 UI 上无处维护 dessert.stock;
  - 后端 `OrderServiceImpl.java` 与 `MobileOrderServiceImpl.java` 中 grep `stock|sales` **零命中**——下单既不扣 dessert.stock 也不加 sales(与 PROJECT_SUMMARY §6 的 🔴"下单不扣库存/不记销量"互相印证);
  - 结论:dessert.stock 无人写入、无人扣减,却被仪表盘当作预警数据源展示。
- **证据 D(跨模块假动作)**:`composables/useDashboard.js:485-487` `handleReorder(itemName)` 对着**甜品**库存预警弹出 `toast.success('已生成 […] 的智能合并补货单')`——一个不调任何 API 的假"补货单",且"补货"本应是**库存(原料)模块**的动作,却挂在仪表盘的甜品预警上。这是边界混乱直接生产出的假功能。
- **证据 E(越层访问)**:`DashboardServiceImpl.java:20` 直接注入 `DessertMapper` 并在 `:46/:109` 直接查表,绕过 DessertService——聚合模块直接伸手进别的模块的持久层。

### 冲突 ③:订单模块与甜品/客户模块之间没有数据级关联(边界断裂的另一面)

- **证据 A**:`Order.java` 的 `items` 是 String;`MobileOrderServiceImpl.createOrder`(32-54 行)把购物车序列化成 JSON 字符串塞进该列,orderNo 为 `"DS"+时间戳+4位随机数`(报告已指出的碰撞风险,且无唯一索引)。没有 order_item 表、没有对 dessert.id 的外键 ⇒ 扣库存、记销量、按商品统计营收在数据结构上**不可能实现**——冲突②中 stock/sales 成为孤儿列的根子在这里。
- **证据 B**:`CustomerServiceImpl.java:62` `selectOrdersByCustomerName(customer.getName())` —— 客户与订单靠**姓名字符串**匹配。重名客户互看订单、客户改名即丢失全部历史,客户模块的消费统计天然不可靠。
- **证据 C**:`MobileOrderServiceImpl.createOrder` 的 `amount` 直接取自客户端请求(与 PROJECT_SUMMARY 🟠"移动端订单金额可被客户端操控"印证)——因为订单不回查 dessert 表的价格,金额只能信客户端。**这既是安全问题也是边界问题**:订单域没有从商品域(source of truth)取价。

### 冲突 ④:路由/导航分组与模块语义冲突,前端文件组织与导航又互相矛盾

- **证据 A(侧边栏)**:`views/layout/index.vue`(320-399 行,硬编码 `<router-link>`)的分组:**"甜品管理"组 = 甜品+价格+订单+库存**;**"系统管理"组 = 部门+员工+客户**。订单和(原料)库存被塞进"甜品管理",客户被当成"系统"配置——收银员找订单要去"甜品管理"里翻,客服找客户要进"系统管理"。
- **证据 B(路由文件自相矛盾)**:`router/routes/modules/business.js` 头注释写"业务管理路由(价格 + 订单)",实际却装了 dessert、price、order、dessert/:id、inventory 五条;`system.js` 注释"系统管理路由(部门 + 员工)",实际却包含 customer。注释、文件名、内容三者互不一致。

## 1.3 根因

1. **模块是按"页面/菜单"切的,不是按"数据所有权"切的。**"价格管理"作为一个菜单项存在,于是有了一个页面,但价格本来就是甜品实体的属性,于是这个页面只能借用甜品的 API 和模型,成为无头分身。
2. **两个"库存"来自两次独立加需求,从未对齐语义。**dessert.stock/sales 是商品表自带的展示字段,inventory 是后加的原料模块,没人回答过"成品库存到底归谁管、由什么事件驱动增减"。
3. **订单域是弱类型的(JSON 文本 + 姓名字符串),导致跨模块一致性无处安放。**扣库存、记销量、客户账单这些跨域动作需要外键和事务边界,现有 schema 不支持,于是所有本该联动的地方都变成"不做"(下单不扣库存)或"造假"(仪表盘假补货单)。

## 1.4 方案:重划模块边界

### 1.4.1 数据所有权(source of truth)表

| 数据 | 唯一属主 | 其他模块的访问方式 |
|---|---|---|
| 商品信息(含**价格、上下架状态**、分类、图片) | **dessert 模块** | 只读查询走 `/desserts`;调价走新增的**窄接口**(见下),任何模块不得整对象 PUT |
| 成品库存 dessert.stock、销量 sales | **dessert 模块**(由**订单事件**驱动增减,见方案 B) | 订单模块通过 DessertService 的显式方法 `deductStock(items)` 扣减(同事务);仪表盘只读走 DessertService,不再直连 Mapper |
| 原料库存 inventory | **inventory 模块** | 预警统一走 `GET /inventory/alerts`;仪表盘如需展示原料预警,调该接口 |
| 订单及订单明细 | **order 模块**(新增 order_item 表) | 客户/报表模块经 `customer_id` / `order_item.dessert_id` 外键 JOIN,不再按姓名匹配 |
| 客户/会员 | **customer 模块** | 订单持 `customer_id` 外键引用 |
| 员工/部门/角色 | **emp/dept + 新 RBAC 表**(见问题二) | — |

### 1.4.2 具体改法

**A. 价格模块并入甜品域(消灭无头分身)** — 推荐做法:
1. 后端在 DessertController 新增窄接口 `PUT /desserts/price`(仅收 `{id, price}`),Service 层只 update 这一列——从根上消除整对象覆写。
2. 前端 `/price` 页面**保留路由与视觉**(它是已完成视觉改造的一部分),但:`savePrice` 改调新窄接口;删除与甜品页重复的 `toggleStatus` 上下架按钮(或保留但明确它就是甜品域能力的快捷入口);补齐 `categoryColors` 缺的 snack/gift 两色,或直接改为从 `DESSERT_CATEGORIES` 常量派生颜色,消除双份口径。
3. 导航上把"价格管理"明确标注为甜品域的子视图(Phase 3 重分组时二者归同一组,见 1.4.2-D)。
4. *备选(更彻底但动导航)*:整页并成 `/dessert` 下的一个 Tab,删除 `/price` 路由。因会改变已封版的导航结构,**不默认采用**,列为可选项待确认。

**B. 成品库存:二选一,推荐激活**
- **方案 B1(推荐,对应 MASTER_PLAN 既有规划)**:新增 `order_item` 表(order_id, dessert_id, quantity, price 下单时快照);下单事务内:校验并扣减 dessert.stock、累加 sales、**金额由服务端按 dessert 当前价格重算**(顺带修复 🟠 金额可控问题);甜品表单增加库存字段;orders.order_no 加唯一索引。仪表盘成品预警继续读 dessert.stock,此后它是活数据。
- **方案 B2(低成本)**:删除 dessert 表的 stock 列与 `selectLowStockAlerts`,仪表盘"库存预警"改调 `/inventory/alerts`(原料预警)。不做扣库存。**只在不打算做 B1 时采用**。
- 无论 B1/B2:删除 `useDashboard.js:485-487` 的假补货 toast,或将其改为真实跳转 `/inventory`;`DashboardServiceImpl` 改为注入 Service 而非 Mapper。

**C. 订单↔客户强关联**:orders 加 `customer_id`(移动端下单时由 member 身份写入;后台手工单可空),`CustomerServiceImpl.java:62` 改为按 id 查;姓名列保留为冗余快照。

**D. 导航与路由文件重分组**(与侧边栏视觉协调,见 §4):
- 商品:甜品管理、价格管理(business.js → 拆出 `product.js`)
- 交易:订单管理(`trade.js`)
- 库存:库存管理(原料)(`inventory.js` 路由模块)
- 客户:客户管理(从 system.js 迁出,`customer.js`)
- 组织:部门、员工(system.js 只留这两个,名实相符)
- 数据:报表、日志(data.js 不动)

### 1.4.3 涉及的具体文件清单

**后端**:`DessertController.java`(+`PUT /desserts/price`)、`DessertService/Impl` + `DessertMapper(.xml)`(窄更新、deductStock)、`OrderServiceImpl.java` / `MobileOrderServiceImpl.java`(事务扣减、金额服务端重算)、`Order.java` + 新 `OrderItem.java` POJO、`OrderMapper.xml`、`CustomerServiceImpl.java:62` + `CustomerMapper.xml`、`DashboardServiceImpl.java`(:20/:46/:109 改走 Service)、`newdessertshop/sql/` 新迁移脚本(order_item 表、orders.customer_id、order_no 唯一索引)。

**前端**:`views/price/index.vue`(:7 换 API、:89-104 窄提交、:106-116 去重、:119-125 色板)、`views/dessert/index.vue`(表单加库存字段,B1 时)、`api/modules/dessert.js`(新增 updatePriceApi)、`composables/useDashboard.js`(:485-487)、`router/routes/modules/business.js` / `system.js`(重分组)、`views/layout/index.vue`(320-399 行导航分组文案与顺序)。

### 1.4.4 风险等级

| 子项 | 风险 | 说明 |
|---|---|---|
| A 价格窄接口 + 页面换 API | 🟢 低 | 纯收敛,行为向后兼容 |
| B1 下单扣库存/记销量/金额重算 | 🔴 高 | 动交易主链路 + DB 迁移 + 事务;必须先补集成测试,分库存校验/扣减两步灰度 |
| B2 删列改预警源 | 🟡 中 | 仪表盘卡片语义变化,需改文案 |
| C 客户外键 | 🟡 中 | 存量订单需按姓名回填 customer_id 的一次性脚本,重名订单需人工归属 |
| D 导航重分组 | 🟡 中 | 技术简单,但**改变已封版的导航结构**,需先过视觉协调(§4) |

---

# 问题二:完全缺失角色权限体系(RBAC)

## 2.1 现状证据(代码位置)

**数据/模型层——角色字段核查结果:不存在,但有可复用原料**
- `Emp.java`:字段为 `username/name/gender/job/salary/image/deptId/entryDate/...`——**无 role 字段**;`EmpMapper.xml` 中 `job` 出现 10 处,证实真实 DB 有 job 列。
- `newdessertshop/sql/` 全目录 grep "role" **零命中**;不存在 sys_role / sys_permission / sys_user_role / sys_user_role 任何 DDL。
- **可复用原料**:`dessertshop/src/utils/constants.js` 的 `JOB_LIST`(经理=1/副经理=2/实习生=3/线上运营=4/财务出纳=5,注释"与数据库 job 字段对应")——emp.job 是现成的"准角色"离散字段,可作为初始角色映射源。

**认证链路——token 不带角色、后端不做任何角色校验**
- `EmpServiceImpl.login()`:JWT claims 仅 `dataMap.put("id",…)` + `dataMap.put("username",…)`;返回 `LoginInfo(id, username, name, jwt, image)`——**登录响应与 token 中均无任何角色/权限信息**。
- `filter/TokenFilter.java`:白名单(`/login`、`/mobile` 前缀、swagger/doc、`.html/.css/.js/.ico/.png/.svg` 后缀)之外,仅执行 `jwtUtils.parseJWT(jwt)` 验证有效性即放行——**只有"登录/未登录"二态,零角色/权限逻辑**;解析出的 claims 也没有存入任何上下文供 Controller 使用。
- 未引入 Spring Security,无任何 `@PreAuthorize`/AOP 权限切面(controller 目录 16 个文件均无)。

**前端——守卫只判登录,权限脚手架是死代码**
- `router/guards.js` `beforeEach`:只检查 `userStore.isLoggedIn`(public 判定为 `to.meta?.public || '/login' || startsWith('/m')`),无任何角色分支;所有路由(`router/routes/modules/*.js`)的 meta 均无 roles/permissions 字段。
- `composables/usePermission.js:19,30`:`hasPermission/hasRole` 读取 `userStore.userInfo?.roles || []`——而 `stores/modules/user.js` 登录时存的 `result.data` 是 `{id, username, name, token, image}`,**roles 永远为 undefined**;grep 全项目,该 composable 除在 `composables/index.js` 里 re-export 外**没有任何视图使用**。它是一个等着后端喂 roles 的死脚手架(且 `:20` 的 `roles.includes('admin')` 把角色数组当权限数组用,本身也有设计混淆)。
- `views/layout/index.vue:320-399`:侧边栏为硬编码 `<router-link>`,任何登录用户看到全部 11 个菜单,无过滤。

**与既有报告交叉验证——同一根因**
- PROJECT_SUMMARY §5.3 的 🔴 IDOR:`EmpServiceImpl.updatePassword(Integer id, …)` 与 `getProfile(Integer id)` 均接受**请求方任意传入的 id**。这与 RBAC 缺失是同一根因:**服务端没有"当前用户"上下文**——TokenFilter 验完 token 就把身份丢掉了,Controller 只能信参数。做 RBAC 时在过滤器里建立 CurrentUser 上下文,IDOR 顺手修复(id 从上下文取而非参数)。
- PROJECT_SUMMARY 🔴"登录接口 @LogOperation 记录明文密码"(`LoginController.java` POST /login):属同一批认证链路整改,列入 Phase 1 附带修复。
- DESSERTSHOP_MASTER_PLAN Phase 2 与 NEXT_TASKS A3–A5 已有**完整但未实施**的 RBAC 蓝图(4 张表、8 个预置角色、`模块:资源:操作` 权限码、`@RequirePermission` + PermissionAspect、前端 v-permission + 403 页),且 MASTER_PLAN §16 已给出灰度策略:"默认所有用户绑定'店长'角色保持原有全部权限"。**本方案直接采纳该蓝图并做裁剪,不另起炉灶。**

## 2.2 根因

认证被设计成单一布尔态("有有效 token = 什么都能做")。数据模型里没有身份→权限的映射(无角色表、emp.job 只当档案字段用),JWT 载荷最小化,后端没有授权执行层(无 Spring Security、无切面),于是:横向越权(IDOR)、纵向越权(实习生可删员工/改价格/删甜品)在服务端全部畅通;前端那份 usePermission 死代码说明曾有人打算做,但因后端不供给 roles 而搁浅。

## 2.3 方案

### 2.3.1 角色划分(蓝图 8 角色裁剪为 5 + job 迁移映射)

| 角色码 | 名称 | 可见菜单/路由 | 初始迁移(emp.job →) |
|---|---|---|---|
| `super_admin` | 超级管理员/店长 | 全部 | job=1 经理(及灰度期**全员默认绑定**) |
| `manager` | 副店长 | 除员工薪资、日志外全部 | job=2 副经理 |
| `cashier` | 收银/运营 | 首页、订单、客户、甜品(只读)、价格(只读) | job=4 线上运营 |
| `warehouse` | 仓管/后厨 | 首页、库存、甜品(只读) | job=3 实习生(默认最小权限) |
| `finance` | 财务 | 首页、报表、订单(只读) | job=5 财务出纳 |

权限码沿用蓝图规范 `模块:资源:操作`(如 `dessert:price:update`、`emp:emp:delete`),约 40–50 个。

### 2.3.2 后端(必须真正校验——这是安全边界所在)

1. **数据层**:新建 `sys_role`、`sys_permission`、`sys_role_permission`、`sys_user_role` 四表 + 种子数据 + 迁移脚本(按上表把 emp.job 映射成初始角色;**灰度开关期全员先绑 super_admin,行为与现状等价,可安全上线**)。
2. **令牌与上下文**:`EmpServiceImpl.login()` 的 JWT claims 增加 `roles`(权限码列表不进 token,登录响应下发或按需拉取,避免 token 膨胀);`TokenFilter` 解析后将 `{id, username, roles}` 放入 ThreadLocal(新建 `CurrentUserHolder`),请求结束清理。
3. **执行层**:新建 `@RequirePermission("xxx:yyy:zzz")` 注解 + `PermissionAspect` 切面(查角色→权限缓存,无权抛 403);**逐 Controller 打注解**(16 个控制器,写操作全覆盖,读操作按角色表);未打注解的接口默认仅要求登录(保底不回退)。
4. **顺手修复同根因问题**:`updatePassword` / `getProfile` 的 id 改从 `CurrentUserHolder` 取;`/login` 移除 `@LogOperation` 或在日志切面里对 password 字段脱敏。
5. `/mobile/**` 仍走白名单(C 端体系),但其 IDOR/金额问题在问题一方案 B1 中处理。

### 2.3.3 前端(仅是体验层,不承担安全)

1. `stores/modules/user.js`:登录后存储 `roles` + `permissions`(localStorage 同步扩展,注意兼容旧缓存)。
2. 路由 meta 增加 `permissions: []`;`router/guards.js` 的 `beforeEach` 在现有登录判断之后**追加一个权限分支**(无权 → `/403`),新建 `views/error/403.vue`。**只增分支,不动同文件内的 View Transitions 机制**(见 §4)。
3. `views/layout/index.vue` 侧边栏:硬编码 `<router-link>` 外包 `v-if="hasPermission(...)"`(或改为由带 meta 的路由表 computed 派生)——**保持现有 DOM 结构与 class 不变**,只做条件渲染。
4. 激活 `usePermission.js`:修正 `:20` 的角色/权限混用(admin 直通改为读 roles,权限判断读 permissions),补 `v-permission` 指令用于按钮级隐藏(删除/调价等)。

### 2.3.4 前后端改动范围界定

| 端 | 范围 | 性质 |
|---|---|---|
| 后端 | 4 张新表+迁移、EmpServiceImpl(login/updatePassword/getProfile)、TokenFilter、新增 annotation/aspect/context 三个类、16 个 Controller 加注解、LoginController 日志脱敏 | **安全边界,必须先做**;前端未跟上时表现为"菜单可见但点了报 403",可接受 |
| 前端 | user store、guards.js(追加分支)、routes/modules/*.js 的 meta、layout 侧边栏 v-if、usePermission 修正、v-permission 指令、403 页 | 纯体验;即使全部回滚,后端仍然安全 |

### 2.3.5 涉及的具体文件清单

**后端(新建)**:`sql/xxx_rbac_init.sql`、`pojo/SysRole.java`/`SysPermission.java`、`annotation/RequirePermission.java`、`aspect/PermissionAspect.java`、`context/CurrentUserHolder.java`、`mapper/SysRoleMapper(.xml)` 等。
**后端(修改)**:`service/impl/EmpServiceImpl.java`(login claims、updatePassword、getProfile)、`pojo/LoginInfo.java`(+roles/permissions)、`filter/TokenFilter.java`、16 × `controller/*.java`、`controller/LoginController.java`(脱敏)。
**前端(修改)**:`stores/modules/user.js`、`router/guards.js`、`router/routes/modules/{business,system,data}.js`(meta)、`views/layout/index.vue`(320-399)、`composables/usePermission.js`、`api/modules/auth.js`。
**前端(新建)**:`views/error/403.vue`、`directives/permission.js`。

### 2.3.6 风险等级

| 子项 | 风险 | 缓解 |
|---|---|---|
| 后端 RBAC 数据层+注解 | 🟡 中 | 灰度:全员默认 super_admin ⇒ 上线即等价于现状,再逐角色收紧;每个 Controller 的注解可独立提交/回滚 |
| JWT claims 变更 | 🟡 中 | 旧 token 无 roles 字段 ⇒ TokenFilter 对缺失 roles 的合法 token 按 super_admin 兜底(灰度期)或强制重新登录 |
| IDOR 修复 | 🟢 低 | 行为收紧,只影响本就越权的调用 |
| 前端守卫/菜单过滤 | 🟡 中 | 触碰视觉封版文件,见 §4 协调规则;可整体 revert 不影响后端安全 |

---

# 3. 统一分阶段执行计划

**原则:① 安全相关的后端校验优先于前端体验改动;② 每阶段可独立验证、可独立回滚;③ 阶段间无强依赖时可并行,但不跨阶段合并提交。**

### Phase 1 — 后端角色数据模型 + 服务端强校验(安全优先)🔴必做
- 内容:RBAC 四表+种子+job 映射迁移(全员灰度绑 super_admin)→ JWT/LoginInfo 扩展 → TokenFilter 建 CurrentUser 上下文 → @RequirePermission 切面 → 16 控制器打注解 → IDOR 两处修复 + 登录日志脱敏。
- 验证:用不同角色 token 打接口(收银员 DELETE /emps → 403;店长 → 200);现有 21 个测试全绿;旧行为回归(灰度全员 super_admin 时所有接口与现状一致)。
- 回滚:迁移脚本可逆(drop 四表);注解按控制器逐个 revert;JWT 兜底逻辑保证旧 token 不炸。

### Phase 2 — 前端路由守卫 + 菜单过滤(体验跟进)
- 内容:user store 存 roles/permissions → 路由 meta → guards.js 追加权限分支 + 403 页 → 侧边栏 v-if 过滤 → v-permission 按钮级控制 → usePermission 修正。
- 验证:5 个角色各登录一遍,菜单/可达路由/按钮与 §2.3.1 表一致;View Transitions 导航动效(nav-pill)逐路由回归;深浅色主题下 403 页样式检查。
- 回滚:纯前端 revert,不影响 Phase 1 的服务端安全。

### Phase 3 — 模块边界重构(功能正确性)
按依赖排序,子步独立:
- 3a 价格窄接口 + price 页换 API/去重/补色板(🟢,可最先做,甚至与 Phase 2 并行);
- 3b orders.customer_id 外键 + 回填脚本 + CustomerServiceImpl 改查询(🟡);
- 3c order_item 表 + 下单事务扣库存/记销量/服务端重算金额 + order_no 唯一索引 + 甜品表单加库存字段(🔴,需先补订单链路集成测试);
- 3d 仪表盘预警源统一 + 删假补货 toast + DashboardServiceImpl 改走 Service(🟢);
- 3e 导航/路由文件重分组(🟡,**需先过 §4 视觉确认**)。
- 验证:每子步有独立的可观察断言(下单后库存-1 销量+1;重名客户订单不串;首页预警与库存页口径一致)。
- 回滚:各子步独立迁移脚本与提交。

---

# 4. 对已完成视觉改造成果的影响与协调清单

结论:**Phase 1 零影响(纯后端);Phase 2 有两个共享触点需按规则改;Phase 3e 是唯一真正改变导航栏结构的项,需单独确认。**

| # | 触点 | 视觉资产(SHOWCASE_REPORT 已封版内容) | 本方案的改动 | 协调规则 |
|---|---|---|---|---|
| 1 | `router/guards.js` | View Transitions 机制全部住在这个文件:vt-active class、nav-pill 快照握手、NProgress 180ms 延迟启动 | Phase 2 在 `beforeEach` 追加权限判断分支 | **只在现有登录判断之后插入一个 return 分支,不移动/不重排 VT 相关代码**;403 跳转同样会触发 VT,需验证 403 页转场不闪烁 |
| 2 | `views/layout/index.vue`(1542 行) | 侧边栏 glass-pill 导航胶囊(约 325 行)是 VT 共享元素;黑金主题;分组结构"甜品管理/系统管理/数据统计" | Phase 2:对现有 `<router-link>` 加 v-if 过滤;Phase 3e:重分组文案与归属 | Phase 2 **不增删 DOM 层级、不改 class、不动 glass-pill 元素**,仅条件渲染;菜单项被过滤后 nav-pill 定位需回归测试(当前项被隐藏时胶囊落点) |
| 3 | 登录页(BloomGarden,已封版) | 模板与样式封版 | 登录**响应处理 JS**(user store)需多存 roles/permissions | 模板/CSS **零改动**,仅 store 逻辑;不触碰封版承诺 |
| 4 | 导航分组结构 | 侧边栏三分组是视觉改造成果的一部分 | **Phase 3e 会将其改为六分组**(商品/交易/库存/客户/组织/数据) | 这是本方案中唯一与封版视觉**正面冲突**的项 ⇒ 保持 .nav-group 既有 markup/样式复用,仅变分组归属与文案;实施前需你单独确认是否接受导航结构变化,不接受则 3e 降级为"只把客户从系统管理移到新建'客户'组"的最小版,或完全搁置 |
| 5 | `views/price/index.vue` | 玻璃卡片网格是视觉成果 | Phase 3a 仅换 script 层 API 调用与色板补齐 | 模板/样式不动;若未来采纳"并入甜品 Tab"备选方案才涉及视觉,本轮不采纳 |
| 6 | 仪表盘(useDashboard/ECharts dessertTheme) | 视觉成果 | Phase 3d 改数据源与删假 toast | 卡片视觉不动,仅文案("成品库存预警"或"原料库存预警")与数据源 |

---

*本文档为诊断与方案。执行需按 Phase 1 → 2 → 3 顺序另行发起。*

---

# Changelog

## 2026-07-10 — Phase 1 完成(后端角色数据模型 + 服务端强校验)

**范围**:仅后端,前端零改动。按 §2.3.2 逐条执行;两项定案:B1 留待 Phase 3(本轮只为其打好 CurrentUserHolder 基础);旧 token(无 roles)一律 401,不做 super_admin 兜底。

### 数据层
- 新建 `newdessertshop/sql/rbac_init.sql`:sys_role / sys_permission / sys_role_permission / sys_user_role 四表 + 5 角色 + 38 权限码种子 + 角色权限映射(super_admin 38 / manager 36(除 log:*)/ cashier 16 / warehouse 8 / finance 3)+ emp.job → 角色初始迁移 + 灰度全员绑 super_admin(脚本内含灰度退出 SQL,注释态)。回滚脚本 `rbac_rollback.sql`(drop 四表)。**已在本机 MySQL dessertshop 库执行**,7 名员工全部绑定 super_admin + job 映射角色。
- 新增 POJO:`SysRole.java`、`SysPermission.java`;新增 `mapper/SysRoleMapper.java`(注解式)。

### 令牌与上下文
- `EmpServiceImpl.login()`:JWT claims 增加 `roles`;`LoginInfo` 增加 `roles`/`permissions` 字段(权限码只随登录响应下发,不进 token)。
- 新建 `context/CurrentUser.java` + `context/CurrentUserHolder.java`(ThreadLocal);`TokenFilter` 解析 token 后写入 {id, username, roles},finally 中清理;**token 缺 roles 字段 → 401**。
- `EmpServiceImpl.save()` 新增员工默认绑 super_admin(灰度期与存量行为一致,灰度退出后应改为按 job 映射);`deleteByIds()` 同步清理 sys_user_role。

### 执行层
- 新建 `annotation/RequirePermission.java` + `aop/PermissionAspect.java`(super_admin 短路放行;未登录 401;空角色/无权限 403,走既有 GlobalExceptionHandler)+ `aop/PermissionCache.java`(角色→权限码内存缓存,TTL 5 分钟)。
- 12 个业务 Controller 共 60 处方法级注解,写操作全覆盖、读操作按 §2.3.1 角色表。**保底状态(未打注解、仅要求登录)**:POST /upload、GET /emps/profile/{id}、PUT /emps/password;/login 及 /mobile/** 走白名单(见正文 §2.3.2-5)。

### 同根因修复
- IDOR:`EmpController.getProfile` / `updatePassword` 的 id 改从 CurrentUserHolder 取,忽略请求传入的 id(自动纠正为本人)。
- 登录日志:`LoginController./login` 移除 @LogOperation(此前记录明文密码+返回 token);`OperationLogAspect` 追加 password 字段通用脱敏(覆盖 updatePassword 等参数含密码的接口)。

### 验证(全部通过)
- 单测:49/49 全绿(38 个原有 + 11 个新增 `rbac/RbacIntegrationTest`,真实 TokenFilter+切面+H2 独立库)。
- 实测(本机 MySQL + 运行中应用):cashier DELETE /emps → 403;super_admin → 200;旧 token/无 token → 401;cashier/finance 角色矩阵抽查符合 §2.3.1;IDOR 两处按预期自动纠正;操作日志密码已脱敏为 `***`。
- 灰度回归:super_admin token 打全部 15 个模块读接口均 200,与现状行为一致;/mobile/** 白名单无 token 可访问;新增员工自动绑 super_admin 并可正常登录,删除员工时角色绑定同步清理。

### 已知限制 / 后续
- manager"除员工薪资外"是字段级控制,接口级注解无法表达,本阶段 manager 拥有 emp:emp:* 全部权限(待后续做字段级方案)。
- 权限缓存 TTL 5 分钟:调整 sys_role_permission 后最迟 5 分钟生效。
- 存量 operate_log 中有历史明文密码记录(改造前产生,如 id=28 的 login 记录),建议手工清理。
- Phase 2(前端)、Phase 3(模块边界)未开始。

## 2026-10-12 — 视觉/商品图统一 + Phase 3 部分落地(3a、3c 核心、3d 局部)

**范围**:登录页重做(取消 BloomGarden,改为甜品摄影分栏布局)、商品图统一、订单/商品链路修复。Phase 2(前端权限)仍未开始。

### Phase 3 对应项
- 3a ✅ `PUT /desserts/price` 窄接口(DessertPriceRequest);价格页改调窄接口,色板改由 `DESSERT_CATEGORIES`(7 类)派生。
- 3c ◐ 移动端下单在同一事务内:按库内价格重算金额(与客户端金额不一致即拒单)、条件更新原子扣库存 + 记销量、同商品多行合并;新增 `PUT /orders/status`(服务端校验流转规则,取消时按 JSON 明细回补库存);甜品表单加库存字段。**未做**:order_item 表、order_no 唯一索引(明细仍为 JSON 文本)。
- 3d ◐ 首页成品库存预警"快捷采购"假 toast → 跳转甜品管理并按名称定位。DashboardServiceImpl 仍直连 Mapper,未改。
- `orders.items` 放宽到 VARCHAR(2000)(`sql/order_stock_and_images.sql`,已在本机库执行,含回滚注释)。

### 其它修复
- 前端分页读 `records` 而后端返回 `rows`:甜品/客户/客户订单/评论/库存列表此前恒为空,已修。
- 订单"待支付"(status=0)筛选被 `|| ''` 吞掉;订单日期区间不含结束当天(LocalDate between)→ 改为 `[begin, end+1)`。
- 移动端商品卡片跳 `/m/product/:id`、底部 Tab 跳 `/m/orders` 均为 404 → 新增商品详情页、"我的订单"页(`GET /mobile/orders/{orderNo}?phone=` 双因子查单)。
- `MemberServiceImpl.afterOrder` 改 `Propagation.NESTED`:积分失败不再把整笔订单标记为 rollback-only。
- 全局异常:DuplicateKeyException → 409、NoResourceFoundException → 404、方法参数约束 → 400;前端拦截器 4xx 展示后端具体原因。
- 测试 profile 使用 Redis database 15,与开发库隔离。

### 验证
- 后端 62/62(新增 `order/OrderFlowIntegrationTest` 13 个);前端 lint 0 error、生产构建通过;本机真实库 API 实测 19 项与浏览器后台/H5 全流程实测通过(测试数据已清理)。

### 已知限制 / 后续
- 取消订单会回补库存,但不扣回已发放的会员积分。
- 本机 Redis 无密码而应用配置 `REDIS_PASSWORD:123456`,缓存始终降级直查库(功能正常,缓存未生效)。

## 2026-10-13 — 一致性、安全加固与部署就绪（详见 DEPLOYMENT.md）

### 订单 × 库存 × 积分（统一约定，见 OrderServiceImpl 类注释）
- 下单：扣库存/记销量 + 查找或注册会员（新人券），**不再下单即发积分**。
- → 已完成：发放积分/成长值/累计消费/订单数，每单一次（兼容旧规则已发放的历史订单，不重复发放）。
- → 已取消（仅待支付/待接单）：回补库存；若该单曾发积分则扣回一次（余额不足扣到 0 并记流水，type=7）。
- 重复请求无副作用；并发靠带原状态条件的 update；积分步骤失败整体回滚。删除仅限已完成/已取消；整单 PUT 不得改状态。
- 积分兑换：校验会员 ID 与手机号匹配；扣积分条件更新失败时不发券（修复并发白送券）。

### 安全
- 修复匿名上传绕过（`/upload;.png` 通过 TokenFilter 后缀白名单）：拒绝带 `;` 的 URI、白名单按规范化路径匹配、静态后缀仅限 GET/HEAD。
- 上传：`@RequirePermission` 支持多码任一；需甜品/员工编辑权限；按魔数识别类型、扩展名由服务端决定；OSS 失败返回可读错误。
- 角色与账号存在性以数据库为准（30s 缓存，删除员工即时失效）：删除员工后旧 token 401、降级即时生效。
- 员工密码哈希不再出现在接口响应与 emp_log；登录限流默认不信任 XFF；CORS 改为按配置白名单（默认不开放）。
- 非法 JSON / 空 ids / 超大文件 / 无此路由 → 4xx（原为 500）。

### Redis
- 原先缓存从未生效（本机 Redis 无密码而默认发 AUTH 123456；且序列化器不支持 LocalDateTime，写入全部失败）。修复后实测命中、改价即时失效；新增熔断降级（Redis 不可达时仅首个请求受影响，其后直查库约 15ms）与恢复后补清理；缓存失效改为事务提交后执行。

### 数据库
- `orders.order_no` 唯一索引；本机库 62 条逐字段完全相同的重复种子订单已备份到 `orders_dup_backup_20261013` 后删除（执行前整库备份并演练过迁移与回滚）。种子脚本补唯一键，重复执行不再复制订单。订单号冲突自动重试。
- 订单明细维持 JSON（不建 order_item 表）：现有功能（扣/补库存、展示）已满足，引入新表需改写报表与历史数据，收益不足以支撑本轮的迁移风险。
- 新增 `sql/schema_baseline.sql`（此前仓库无 emp/dept/emp_expr 建表脚本，新环境无法建库），已在空库演练至登录成功。

### 交付与前端
- `application-prod.yml` + `ProductionSafetyCheck`：缺密钥/用默认 JWT 密钥拒绝启动；关闭 SQL 日志、接口文档与静态映射（均实测）。
- 商品图：`ProductImg` 组件统一 srcset(1x/2x)/懒加载/预连接/失败回退；新上传对象带 `Cache-Control: immutable`。慢的根因是本机网络（TLS 握手 3–6s，访问百度同样），非 OSS。
- 生产包移除 mock 降级与未使用且阻塞首屏的 Google Fonts；员工新增表单补初始密码字段（此前 UI 新增员工必然失败）。

### 验证
- 后端 85/85；前端 lint 0 error、构建通过；真实 MySQL/Redis/OSS 与浏览器端到端实测通过，测试数据均已清理。

## 2026-10-30 — 品牌升级「喜甜 SWEETEN」+ Phase 2（前端权限）落地

### 品牌与设计系统
- 品牌定为 **喜甜 / SWEETEN**，标语 SWEETEN YOUR DAY.（让每一天，甜一点。）。标志、色彩、字体、图标与文案规范见前端 `BRAND.md`；技术标识（目录、包名、库名、接口、订单号前缀 `DS`）不变。
- `base.css` 重写为 `--sw-*` 令牌体系（奶油底 / 可可主色 / 腮红点缀），Element Plus 通过 `--el-*` 变量跟随；旧变量保留为别名。退役玻璃 / 辉光 / 漂浮粒子等装饰组件。深色「可可夜」只在工作台生效。

### 顾客商城（/m）
- 从 430px 手机单栏改为桌面 / 平板 / 手机自适应的品牌站：主视觉、服务承诺、人气甜品（按真实销量）、今日菜单（前端分类 / 搜索 / 排序）、详情页同类推荐、右侧抽屉 / 底部面板购物车（打开时按最新在售数据校正价格与库存）、两栏结算、订单进度条、按订单号 + 手机号查回订单。
- 修复：会员中心折扣券把 9 折显示成「1 折」（discount 存的是折后比例）；积分兑换列表不再展示后端不允许兑换的 0 积分券。
- 结算页如实说明「在线支付暂未开通」，按钮为「提交订单」。

### 门店工作台
- **Phase 2 完成**：路由 `meta.permission` + 守卫重定向、侧栏 / ⌘K / 概览快捷入口按权限码过滤、各页新增 / 编辑 / 删除 / 流转按钮按权限显示（`stores/user.can()`，菜单单一来源 `config/navigation.js`）。后端仍是唯一鉴权依据；灰度退出 SQL 仍未执行。
- 经营概览改为全部真实数据（删除模拟订单流、虚构活动 / 评价 / 「一键赠券」假成功提示、随机热力图；不展示后端写死的 trend/progress）。环比由近 30 日真实日数据计算。
- 数据报表删除演示兜底数据；「本周 / 本月 / 本年」此前不生效，已修；分类图标注为「按商品款数」、排行标注「不随日期变化」，与后端口径一致。
- 订单页：动作化流转按钮（接单制作 / 开始配送 / 确认完成）、进度条、可打印小票；客户页新增「会员」标签（现有 `/members` 接口）。
- 修复：工作台内对话框 / 抽屉的遮罩被困在内容区（内容区 view-transition-name 造成的层叠上下文）。
- 开发环境 mock 降级改为显式开启（`VITE_DEV_MOCK=true`）且仅限 GET —— 原实现会在后端不可达时把顾客下单等写操作伪造成功。

### 验证
- 后端 85/85；前端 lint 0 error（警告 1674 → 1270）、构建通过。
- 浏览器端到端（真实后端 / MySQL / Redis）：加购 → 结算校验 → 下单（库存 −2、销量 +2、未新增会员）→ 我的订单 → 工作台取消（库存回补）→ 删除，全部通过，数据回到基线。收银角色界面权限过滤通过。
- 过程中一笔测试订单被推进到「制作中」（后端规则不允许取消），已用一次事务按其确切影响回退（库存 +2、销量 −2、删除该单）并通过甜品状态接口清缓存。
