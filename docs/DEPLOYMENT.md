# 喜甜 SWEETEN 部署说明与就绪评估（2026-10-13；品牌升级 2026-10-30；结构调整 2026-10-31）

> 顾客商城入口为 `/m`，门店工作台为 `/`（登录 `/login`）。后端是单一可部署单元 `sweeten-server.jar`，架构见 [ARCHITECTURE.md](ARCHITECTURE.md)。
> 2026-10-31 起工程标识统一为 SWEETEN（Java 包、Maven 坐标、jar 名）；**库名 `dessertshop`、接口路径、环境变量、订单号前缀 `DS` 保持不变**（清单见 ARCHITECTURE.md §6）。

> 仓库结构：前端在 `frontend/`，后端在 `backend/`（数据库脚本在 `backend/sql/`，按 schema / seed / migrations / archive 分目录）。下文路径均相对仓库根目录。

> 结论先行：**可以做内部测试部署；暂不建议直接对公众开放的生产部署**——阻断项见 §7。

## 1. 运行环境

| 组件 | 要求 | 说明 |
|---|---|---|
| 后端运行时 | **JDK 21+**（LTS） | 编译目标为 21（class 版本 65）；JDK 21–23 均可运行（Spring Boot 3.3） |
| 数据库 | MySQL 8.0+（本机实测 9.1） | utf8mb4 |
| 缓存 | Redis 6+（可选） | 不可用时自动熔断直查数据库，功能不受影响 |
| 对象存储 | 阿里云 OSS（当前 bucket `yuka-javaweb`，cn-beijing，可用环境变量更换） | 商品图/头像上传 |
| 前端构建 | Node `^20.19.0 \|\| >=22.12.0` | 在 `frontend/` 执行 `npm ci && npm run build`，产出 `frontend/dist/` |
| 反向代理 | Nginx（或同类） | 托管前端静态文件 + 同源转发 API，见 §5 |

## 2. 后端构建与配置（`--spring.profiles.active=prod`）

构建（产物固定为 `backend/target/sweeten-server.jar`）：

```bash
mvn -f backend/pom.xml clean package -DskipTests
```

`application-prod.yml` 中密钥类配置**没有默认值**，缺失即启动失败；使用仓库里公开过的开发 JWT 密钥（当前默认值与更名前的旧默认值）也会被 `ProductionSafetyCheck` 拒绝启动（均已实测）。

| 环境变量 | 必填 | 说明 |
|---|---|---|
| `DB_URL` | ✅ | 如 `jdbc:mysql://db:3306/dessertshop?useSSL=true&serverTimezone=Asia/Shanghai` |
| `DB_USERNAME` / `DB_PASSWORD` | ✅ | 专用账号，勿用 root |
| `JWT_SECRET` | ✅ | ≥32 字节随机串，如 `openssl rand -base64 48`；更换会使所有已登录用户重新登录 |
| `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` | ✅（需上传时） | 由 OSS SDK 直接从环境变量读取；建议使用仅有该 bucket `PutObject` 权限的 RAM 子账号 |
| `OSS_ENDPOINT` / `OSS_BUCKET` / `OSS_REGION` | 可选 | 默认为现有 bucket（`https://oss-cn-beijing.aliyuncs.com`、`yuka-javaweb`、`cn-beijing`）；经营地域确定后按需更换 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DATABASE` | 可选 | 不内置默认密码 |
| `CORS_ALLOWED_ORIGINS` | 可选 | 仅当前端与 API 不同源时填写（逗号分隔）；同源部署留空 = 不开放跨域 |
| `TRUST_FORWARDED_FOR` | 可选 | 位于**会覆盖** `X-Forwarded-For` 的 Nginx 之后时设为 `true`，登录限流才能按真实客户端 IP 计数 |

prod profile 另外做了：关闭 SQL 日志（开发配置会把查询结果行——含密码哈希——打印到标准输出）、关闭接口文档与静态资源映射（`/doc.html`、`/v3/api-docs`、`/swagger-ui/**`、后端根路径的开发说明页均 404，已实测）、错误响应不回显异常信息。

启动示例：
```bash
java -jar sweeten-server.jar --spring.profiles.active=prod
```

## 3. 数据库（无迁移工具，按顺序手动执行）

脚本目录说明见 [backend/sql/README.md](../backend/sql/README.md)。

**全新库**：

1. `CREATE DATABASE dessertshop DEFAULT CHARSET utf8mb4;`（库名可以不同，`DB_URL` 指向即可）
2. `backend/sql/schema/schema_baseline.sql` —— 全部表结构（18 张表，由已验证的库导出并逐列比对）
3. `backend/sql/schema/rbac_init.sql` —— 角色与权限码（建表语句为 `IF NOT EXISTS`，会被跳过）
4. 创建第一个管理员（仓库不提供任何生产账号/密码）：用 BCrypt（cost 12）为自选的强密码生成哈希后执行
   ```sql
   INSERT INTO emp (username, password, name, gender, phone, create_time, update_time)
   VALUES ('<管理员用户名>', '<BCrypt 哈希>', '<姓名>', 1, '<11位手机号>', NOW(), NOW());
   INSERT INTO sys_user_role (user_id, role_id)
   SELECT e.id, r.id FROM emp e JOIN sys_role r ON r.code = 'super_admin' WHERE e.username = '<管理员用户名>';
   ```
5. `backend/sql/seed/` 下的演示数据（商品、订单、演示券、`admin / 123456`）**只用于本地与演示环境**，生产不要导入。

> 2026-10-31 在一次性的隔离 MySQL 实例（独立数据目录与端口，不接触任何已有数据库）上演练：`sql/init_all.sql` 连续执行两次结果一致（18 张表、5 个角色、38 个权限码、17 款商品、23 笔演示订单、10 个券模板），随后以该库启动 `sweeten-server.jar` 完成登录、下单、履约、积分、报表的端到端检查。

**已有库**（与本机库同源的环境）按文件名顺序执行以下迁移，均含回滚说明：

| 脚本 | 内容 | 破坏性 |
|---|---|---|
| `backend/sql/migrations/2026-10-12_order_stock_and_images.sql` | `orders.items` 放宽到 2000；商品图统一 | 否（仅放宽/更新 URL） |
| `backend/sql/migrations/2026-10-13_1_order_no_unique.sql` | 清理**逐字段完全相同**的重复种子订单（先整表备份到 `orders_dup_backup_20261013`）+ `uk_order_no` 唯一索引 | **是**：执行前务必 `mysqldump` 并验证可恢复 |
| `backend/sql/migrations/2026-10-13_2_product_image_cache_headers.sql` | 商品图换成带 `Cache-Control` 的 OSS 对象 | 否 |

`backend/sql/archive/` 中是已被基线取代的历史脚本（有的会 `DROP TABLE` 或重置全部密码），**不要执行**。

## 4. 从旧版本升级到 2026-10-31 版本

本次是代码结构与工程标识的调整，**数据库不需要任何迁移**，接口路径与数据格式不变（新旧两版的接口文档已逐个比对：76 个接口零差异）。部署时注意：

| 项目 | 变化 | 需要做什么 |
|---|---|---|
| 可执行包 | `dessertshop-web-management-0.0.1-SNAPSHOT.jar` → `sweeten-server.jar` | 更新部署 / 启动脚本与服务定义中的文件名 |
| 运行时 | JDK 22 → 21+ | JDK 21 或更高均可，原 JDK 22 环境无需改动 |
| Redis 缓存键 | 加 `sweeten:` 前缀 | 无需操作：旧键不再被读取，10 分钟内自然过期 |
| 开发 JWT 默认密钥 | 已更换 | 仅影响未设置 `JWT_SECRET` 的开发 / 演示环境：用户需重新登录一次 |
| `/mobile.html` | 旧版独立 H5 页面退役，改为跳转 `/m` | Nginx 的 `location /mobile/` 可以删除（见 §5） |
| 浏览器本地设置 | 外观、⌘K 最近访问、记住用户名的键名改为 `sw-*` | 无需操作：首次加载自动带上旧值 |

## 5. Nginx 参考配置

前端请求统一走 `/api/*`（见 `frontend/.env.production`），需剥掉前缀转发给后端。

```nginx
server {
    listen 443 ssl http2;
    server_name shop.example.com;
    # ssl_certificate ...; ssl_certificate_key ...;

    root /srv/sweeten/dist;
    client_max_body_size 6m;                     # 与后端 5MB 上传上限匹配

    location /assets/ {                          # 带 hash 的构建产物，长期缓存
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;       # 末尾 / 去掉 /api 前缀
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;   # 覆盖而非追加（配合 TRUST_FORWARDED_FOR=true）
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;        # History 路由
        add_header Cache-Control "no-cache";     # index.html 不缓存，保证发版即时生效
    }
}
```

> 早期配置中的 `location /mobile/ { proxy_pass http://127.0.0.1:8080; }` 只服务于已退役的 `mobile.html`（它直接请求 `/mobile/**`）。现在 `/mobile.html` 只是跳转到 `/m`，商城的全部请求都走 `/api/mobile/**`，该段可以删除；保留也不影响功能。

## 6. 前端生产构建

- `VITE_PRODUCT_IMAGE_URL`（`frontend/.env`）为当前统一商品图；留空则使用打包进来的本地图。`VITE_PRODUCT_IMAGE_UNIFORM=false` 改为展示各甜品自己上传的图片；可选 `VITE_PRODUCT_IMAGE_FOCUS` 调整取景焦点。
- `VITE_DEV_MOCK` 只在开发环境、且显式设为 `true` 时生效（GET 查询兜底演示数据），生产构建不受影响。
- 品牌字体是仓库内的小子集（`frontend/src/assets/fonts/sweeten-*.woff2`，合计约 25KB），随构建产物发布，不依赖外部字体服务；网站图标为 `frontend/public/` 下的 `favicon.svg`、`favicon.ico`、`favicon.png`、`apple-touch-icon.png` 与 `site.webmanifest`，标志文件在 `frontend/public/brand/`（构建后位于站点 `/brand/`，属于静态资源，可按 `/assets/` 之外的普通缓存策略托管）。
- 生产包已核对：无 mock 数据（网络故障时不再静默显示演示数据）、无 devtools、无 source map、无任何密钥。
- `frontend/index.html` 已移除未使用且会阻塞首屏的 Google Fonts 外链（中国大陆不可达）。

## 7. 就绪评估

### 已具备（已实测）
- 鉴权：上传接口匿名绕过（`/upload;.png`）已封堵；上传需甜品/员工编辑权限、按文件魔数识别类型；员工密码哈希不再出现在接口响应与日志中；**角色与账号存在性以数据库为准**（删除员工后其旧 token 立即 401，降级角色 30 秒内生效）；登录限流不再信任伪造的 XFF；CORS 不再对任意来源开放。
- 一致性：订单状态×库存×积分的规则统一（`OrderStatus` 与订单事件，见 ARCHITECTURE.md §2.5）；订单号唯一索引 + 冲突重试。
- 可靠性：Redis 缓存真正生效且故障时熔断降级；非法请求返回 4xx 而非 500。
- 运行时：JDK 21 LTS（原阻断项「JDK 22 非 LTS」已解决）。
- 测试：后端 105 个用例全部通过，且只使用 H2（`TestDatabaseGuard` 禁止连接真实库）；模块依赖方向由 `ModuleBoundaryTest` 校验；前端 lint 0 error、生产构建通过，浏览器端到端冒烟（商城下单全流程 + 工作台全部页面）通过。

### 生产阻断项（需要决策或额外开发）
1. **会员身份只靠手机号**：H5 无短信验证，知道手机号即可查看会员资料、兑换积分（已加"会员 ID+手机号匹配"，仅提高门槛）。上线前应接入短信验证码或微信登录。
2. **没有支付**：订单直接进入"待接单"，没有真实收款环节；对公众开放前需接入支付并定义"待支付"超时取消（模块规划见 ARCHITECTURE.md §4）。
3. **角色治理未收尾**：前端已按权限码隐藏无权限的菜单、页面与操作，后端仍强校验；但没有角色管理界面（调整角色只能改库），灰度期全员 `super_admin` 的退出 SQL 尚未执行（见 `backend/sql/schema/rbac_init.sql` 文末）——不执行它，所有人在界面上仍等同于店长权限；且该 SQL 现有写法会把没有 job 的管理员一并降级，执行前需改写。新员工默认角色集中在 `RoleAssignmentServiceImpl`，灰度退出前应改为按职位映射。

### 建议（非阻断）
- 登录 token 存于 localStorage，任何 XSS 都能窃取；中期可改为 HttpOnly Cookie + CSRF 防护。
- 多实例部署时，登录限流与角色缓存都是进程内的，需要改为 Redis 共享。
- 引入 Flyway 管理迁移脚本，替代手工按序执行（`backend/sql/migrations/` 已按日期命名）。
- 为 `orders(status, create_time)` 等高频查询补索引（归档的 `add_indexes.sql` 使用了 MySQL 不支持的语法，从未生效）。
- OSS 建议绑定 CDN 自定义域名（`VITE_PRODUCT_IMAGE_URL` 改为 CDN 地址即可），并给 bucket 配置防盗链。
- 经营概览与数据报表已改为只展示真实数据；后端 `/dashboard/stats` 仍返回写死的 trend/progress 字段，前端不再展示，建议后端后续删除或改为真实环比。
