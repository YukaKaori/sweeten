# 数据库脚本

按**用途与风险**分目录。所有命令都在 `backend/` 目录执行（脚本里的 `source` 路径相对于当前目录）。

| 目录 / 文件 | 用途 | 可以在哪执行 |
|---|---|---|
| `init_all.sql` | 本地一键初始化：建库 → `schema/` → `seed/`。可重复执行 | **仅本地 / 演示环境**（docker compose 首次启动自动执行） |
| `schema/` | 表结构基线与必需的参考数据（角色、权限码） | 所有环境（新库） |
| `seed/` | 演示商品与订单、演示优惠券模板、本地管理员 `admin / 123456` | **仅本地 / 演示环境，生产禁止** |
| `migrations/` | 已有库的增量升级，文件名带日期，按顺序执行，每个文件内含回滚说明 | 需要升级的已有库（先备份） |
| `archive/` | 已被基线取代的历史脚本。有的会 `DROP TABLE`、有的会重置全部密码，文件头均有警告 | **不要执行**，仅作历史记录 |

## 新库

本地开发：

```bash
mysql -u root -p -e "source sql/init_all.sql"
```

生产（不导入 `seed/`）：

```sql
CREATE DATABASE dessertshop DEFAULT CHARSET utf8mb4;
USE dessertshop;
source sql/schema/schema_baseline.sql
source sql/schema/rbac_init.sql
```

然后按 [docs/DEPLOYMENT.md](../../docs/DEPLOYMENT.md) 的模板创建第一个管理员（仓库不提供生产账号）。

## 已有库升级

与本机库同源的旧环境按文件名顺序执行 `migrations/` 下的脚本（`2026-10-13_1_order_no_unique.sql` 会删除重复订单，执行前务必 `mysqldump` 并验证可恢复）。

新增迁移脚本的约定：`YYYY-MM-DD[_序号]_说明.sql`，脚本头写明背景、执行方式与回滚方式；
如改动影响全新库，同时更新 `schema/schema_baseline.sql`，保持"新库 = 基线"。
后续引入 Flyway 时，`migrations/` 可直接改名为 `V<版本>__<说明>.sql`。

## 兼容标识

- 库名 `dessertshop`、全部表名与列名保持不变（已有数据都在其中）。应用默认连接 `dessertshop`，新环境如需别的库名，用环境变量 `DB_URL` 指定。
- 订单号前缀 `DS` 写在已有订单数据里，顾客凭订单号查单，不随品牌更改。

## 测试库

后端测试使用 H2 内存库，结构与种子在 `src/test/resources/schema.sql`、`data.sql`，与本目录无关；
测试不会、也无法连接 MySQL（见 `src/test/java/com/sweeten/support/TestDatabaseGuard.java`）。
