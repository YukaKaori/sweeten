-- =============================================================
-- 本地一键初始化：建库 + 表结构 + 角色权限 + 演示数据 + 默认管理员(admin / 123456)
-- 在 backend/ 目录执行（source 路径相对于当前目录）：
--   mysql -u root -p -e "source sql/init_all.sql"
-- docker compose 首次启动时也会自动执行本文件（见 docker/mysql-init.sh）。
-- 仅供本地开发；生产建库只执行 schema/ 下的脚本，不导入 seed/（含弱口令管理员），见 sql/README.md
-- 可重复执行：建表为 IF NOT EXISTS，种子数据均为幂等插入
-- =============================================================

-- 中文 Windows 上 mysql 客户端默认 gbk，不切换会把中文写成乱码或报 Data too long
charset utf8mb4

-- 库名 dessertshop 为沿用的兼容标识（应用默认连接此库，可通过 DB_URL 指向其它库名）
CREATE DATABASE IF NOT EXISTS dessertshop DEFAULT CHARACTER SET utf8mb4;
USE dessertshop;

-- 1. 结构与必需的参考数据
source sql/schema/schema_baseline.sql
source sql/schema/rbac_init.sql

-- 2. 演示数据与本地默认管理员（生产不要执行）
source sql/seed/orders_and_dessert.sql
source sql/seed/demo_coupons.sql
source sql/seed/dev_seed_admin.sql
