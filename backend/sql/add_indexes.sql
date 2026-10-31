-- =============================================
-- 数据库索引优化脚本
-- 执行此脚本可以提升查询性能
-- =============================================

-- 订单表索引
-- 订单号查询（唯一索引）
CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_order_no ON orders(order_no);

-- 订单状态筛选
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);

-- 创建时间范围查询
CREATE INDEX IF NOT EXISTS idx_orders_create_time ON orders(create_time);

-- 客户姓名模糊查询
CREATE INDEX IF NOT EXISTS idx_orders_customer_name ON orders(customer_name);

-- 甜品表索引
-- 分类查询
CREATE INDEX IF NOT EXISTS idx_dessert_category ON dessert(category);

-- 状态筛选
CREATE INDEX IF NOT EXISTS idx_dessert_status ON dessert(status);

-- 库存预警查询
CREATE INDEX IF NOT EXISTS idx_dessert_stock ON dessert(stock);

-- 员工表索引
-- 用户名登录查询（唯一索引）
CREATE UNIQUE INDEX IF NOT EXISTS idx_emp_username ON emp(username);

-- 部门关联查询
CREATE INDEX IF NOT EXISTS idx_emp_dept_id ON emp(dept_id);

-- 姓名查询
CREATE INDEX IF NOT EXISTS idx_emp_name ON emp(name);

-- 入职时间查询
CREATE INDEX IF NOT EXISTS idx_emp_entry_date ON emp(entry_date);

-- 员工工作经历表索引
-- 员工ID关联查询
CREATE INDEX IF NOT EXISTS idx_emp_expr_emp_id ON emp_expr(emp_id);

-- 部门表索引
-- 部门名称查询
CREATE INDEX IF NOT EXISTS idx_dept_name ON dept(name);

-- 操作日志表索引
-- 操作时间查询
CREATE INDEX IF NOT EXISTS idx_operate_log_time ON operate_log(operate_time);

-- 操作人员查询
CREATE INDEX IF NOT EXISTS idx_operate_log_emp_id ON operate_log(operate_emp_id);

-- 员工日志表索引
-- 操作时间查询
CREATE INDEX IF NOT EXISTS idx_emp_log_time ON emp_log(operate_time);

-- =============================================
-- 查看表的索引信息（验证用）
-- =============================================
-- SHOW INDEX FROM orders;
-- SHOW INDEX FROM dessert;
-- SHOW INDEX FROM emp;
-- SHOW INDEX FROM emp_expr;
-- SHOW INDEX FROM dept;
-- SHOW INDEX FROM operate_log;
-- SHOW INDEX FROM emp_log;
