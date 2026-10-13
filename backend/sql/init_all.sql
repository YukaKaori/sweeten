-- =============================================================
-- 本地一键初始化：建库 + 表结构 + 角色权限 + 演示数据 + 默认管理员(admin / 123456)
-- 在仓库根目录执行（source 路径相对于当前目录）：
--   mysql -u root -p -e "source sql/init_all.sql"
-- 仅供本地开发，生产环境请按 README 分步执行且不要导入 dev_seed_admin.sql
-- =============================================================

-- 中文 Windows 上 mysql 客户端默认 gbk，不切换会把中文写成乱码或报 Data too long
charset utf8mb4

CREATE DATABASE IF NOT EXISTS dessertshop DEFAULT CHARACTER SET utf8mb4;
USE dessertshop;

source sql/schema_baseline.sql
source sql/rbac_init.sql
source sql/orders_and_dessert.sql
source sql/dev_seed_admin.sql
