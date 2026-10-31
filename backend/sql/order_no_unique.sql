-- =====================================================================
-- 2026-10-13  orders.order_no 唯一索引（含历史重复数据清理）
--
-- 背景：orders_and_dessert.sql 用 INSERT IGNORE 插入种子订单，但 orders 表没有唯一键，
--      脚本每重复执行一次就整批复制一遍。本机库 85 条订单只有 23 个不同订单号，
--      23 组重复全部是逐字段完全相同的副本（62 条），没有任何表引用这些副本的 id
--      （member_coupon.order_id 均为空，points_log 不引用订单）。
--      重复订单会让营收/报表/客户订单历史成倍虚高，并使"按订单号查单"返回多行而报错。
--
-- 做法（只删"与更早一条完全相同"的副本，内容不同的重复不会被删）：
--   1) 把待删副本完整备份到 orders_dup_backup_20261013
--   2) 删除这些副本（保留每组 id 最小的一条）
--   3) 加唯一索引 uk_order_no —— 若还存在内容不同的同号订单，这一步会报错中止，需人工处理
--
-- 执行前：先做整库备份（mysqldump），并确认可以恢复。
-- 执行：mysql -uroot -p dessertshop < sql/order_no_unique.sql
-- 回滚：见文末
-- =====================================================================

-- 1) 备份待删除的完全重复副本
CREATE TABLE orders_dup_backup_20261013 AS
SELECT o.*
FROM orders o
WHERE EXISTS (
    SELECT 1 FROM orders k
    WHERE k.order_no = o.order_no
      AND k.id < o.id
      AND k.customer_name <=> o.customer_name
      AND k.phone <=> o.phone
      AND k.items <=> o.items
      AND k.amount <=> o.amount
      AND k.status <=> o.status
      AND k.address <=> o.address
      AND k.create_time <=> o.create_time
      AND k.update_time <=> o.update_time
);

-- 2) 删除备份过的副本
DELETE o FROM orders o
JOIN orders_dup_backup_20261013 b ON b.id = o.id;

-- 3) 订单号唯一
ALTER TABLE orders ADD UNIQUE KEY uk_order_no (order_no);

-- 校验
SELECT COUNT(*) AS orders_now, COUNT(DISTINCT order_no) AS distinct_order_no FROM orders;
SELECT COUNT(*) AS backed_up_duplicates FROM orders_dup_backup_20261013;

-- ---------------------------------------------------------------------
-- 回滚（按需手动执行）：
-- ALTER TABLE orders DROP INDEX uk_order_no;
-- INSERT INTO orders SELECT * FROM orders_dup_backup_20261013;
-- 确认无误、不再需要回滚后可删除备份表：
-- DROP TABLE orders_dup_backup_20261013;
