-- =============================================================
-- RBAC 权限体系初始化脚本 (ARCHITECTURE_PLAN §2.3.2 Phase 1)
-- 执行库: dessertshop
-- 回滚脚本: rbac_rollback.sql
--
-- 角色划分 (§2.3.1):
--   super_admin 超级管理员/店长  全部权限 (切面短路放行, 同时种子全量权限便于审计)
--   manager     副店长          除日志外全部 (员工薪资为字段级控制, 本阶段接口级无法拆分, 见文档说明)
--   cashier     收银/运营       首页、订单、客户、会员、甜品(只读)
--   warehouse   仓管/后厨       首页、库存、甜品(只读)
--   finance     财务            首页、报表、订单(只读)
--
-- 灰度策略: 全员额外绑定 super_admin, 上线后行为与现状完全一致;
--           收紧时删除灰度绑定 (见文末"灰度退出"注释), job 映射的角色绑定保留。
-- =============================================================

-- ---------- 1. 角色表 ----------
CREATE TABLE IF NOT EXISTS sys_role (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL UNIQUE COMMENT '角色码',
    name        VARCHAR(50)  NOT NULL COMMENT '角色名称',
    description VARCHAR(200) COMMENT '角色说明',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '系统角色表';

-- ---------- 2. 权限表 ----------
CREATE TABLE IF NOT EXISTS sys_permission (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(100) NOT NULL UNIQUE COMMENT '权限码, 格式: 模块:资源:操作',
    name        VARCHAR(100) NOT NULL COMMENT '权限名称',
    module      VARCHAR(50)  NOT NULL COMMENT '所属模块',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '系统权限表';

-- ---------- 3. 角色-权限关联表 ----------
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    role_id       INT NOT NULL,
    permission_id INT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) COMMENT '角色权限关联表';

-- ---------- 4. 用户-角色关联表 ----------
CREATE TABLE IF NOT EXISTS sys_user_role (
    id      INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL COMMENT '关联 emp.id',
    role_id INT NOT NULL COMMENT '关联 sys_role.id',
    UNIQUE KEY uk_user_role (user_id, role_id)
) COMMENT '用户角色关联表';

-- ---------- 5. 角色种子 ----------
INSERT IGNORE INTO sys_role (code, name, description) VALUES
('super_admin', '超级管理员/店长', '全部权限'),
('manager',     '副店长',         '除日志外全部权限'),
('cashier',     '收银/运营',      '首页、订单、客户、会员、甜品只读'),
('warehouse',   '仓管/后厨',      '首页、库存、甜品只读'),
('finance',     '财务',           '首页、报表、订单只读');

-- ---------- 6. 权限种子 (模块:资源:操作) ----------
INSERT IGNORE INTO sys_permission (code, name, module) VALUES
-- 部门
('dept:dept:list',          '查询部门',     'dept'),
('dept:dept:create',        '新增部门',     'dept'),
('dept:dept:update',        '修改部门',     'dept'),
('dept:dept:delete',        '删除部门',     'dept'),
-- 员工
('emp:emp:list',            '查询员工',     'emp'),
('emp:emp:create',          '新增员工',     'emp'),
('emp:emp:update',          '修改员工',     'emp'),
('emp:emp:delete',          '删除员工',     'emp'),
-- 甜品 (价格调整属甜品域, 走 dessert:dessert:update; Phase 3 窄接口时再拆 dessert:price:update)
('dessert:dessert:list',    '查询甜品',     'dessert'),
('dessert:dessert:create',  '新增甜品',     'dessert'),
('dessert:dessert:update',  '修改甜品(含价格/上下架)', 'dessert'),
('dessert:dessert:delete',  '删除甜品',     'dessert'),
-- 评论
('dessert:comment:list',    '查询评论',     'dessert'),
('dessert:comment:create',  '新增评论',     'dessert'),
('dessert:comment:delete',  '删除评论',     'dessert'),
-- 订单
('order:order:list',        '查询订单',     'order'),
('order:order:create',      '新增订单',     'order'),
('order:order:update',      '修改订单',     'order'),
('order:order:delete',      '删除订单',     'order'),
-- 库存(原料)
('inventory:inventory:list',   '查询库存/预警', 'inventory'),
('inventory:inventory:create', '新增物料',     'inventory'),
('inventory:inventory:update', '修改物料',     'inventory'),
('inventory:inventory:delete', '删除物料',     'inventory'),
('inventory:stock:adjust',     '库存出入库调整', 'inventory'),
-- 客户
('customer:customer:list',   '查询客户',     'customer'),
('customer:customer:create', '新增客户',     'customer'),
('customer:customer:update', '修改客户',     'customer'),
('customer:customer:delete', '删除客户',     'customer'),
('customer:order:list',      '查询客户订单', 'customer'),
-- 会员/优惠券
('member:member:list',      '查询会员',     'member'),
('member:points:list',      '查询积分日志', 'member'),
('member:coupon:list',      '查询券模板',   'member'),
('member:coupon:create',    '创建券模板',   'member'),
('member:coupon:issue',     '发放优惠券',   'member'),
-- 仪表盘(首页)
('dashboard:stats:read',    '查看仪表盘',   'dashboard'),
-- 报表
('report:report:read',      '查看数据报表', 'report'),
-- 日志
('log:operate:list',        '查询操作日志', 'log'),
('log:emp:list',            '查询员工日志', 'log');

-- ---------- 7. 角色-权限映射 ----------
-- super_admin: 全量 (切面对 super_admin 短路放行, 此处种子仅为审计/查询一致性)
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.code = 'super_admin';

-- manager: 除日志外全部
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.code = 'manager' AND p.code NOT LIKE 'log:%';

-- cashier: 首页、订单、客户、会员、甜品只读、评论只读
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.code = 'cashier' AND p.code IN (
    'dashboard:stats:read',
    'order:order:list', 'order:order:create', 'order:order:update', 'order:order:delete',
    'customer:customer:list', 'customer:customer:create', 'customer:customer:update',
    'customer:customer:delete', 'customer:order:list',
    'member:member:list', 'member:points:list', 'member:coupon:list', 'member:coupon:issue',
    'dessert:dessert:list', 'dessert:comment:list'
);

-- warehouse: 首页、库存、甜品只读
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.code = 'warehouse' AND p.code IN (
    'dashboard:stats:read',
    'inventory:inventory:list', 'inventory:inventory:create', 'inventory:inventory:update',
    'inventory:inventory:delete', 'inventory:stock:adjust',
    'dessert:dessert:list', 'dessert:comment:list'
);

-- finance: 首页、报表、订单只读
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.code = 'finance' AND p.code IN (
    'dashboard:stats:read',
    'report:report:read',
    'order:order:list'
);

-- ---------- 8. 用户-角色初始迁移 (emp.job → 角色) ----------
-- job: 1=经理 2=副经理 3=实习生 4=线上运营 5=财务出纳 (与前端 JOB_LIST 一致)
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT e.id, r.id
FROM emp e
JOIN sys_role r ON r.code = CASE e.job
    WHEN 1 THEN 'super_admin'
    WHEN 2 THEN 'manager'
    WHEN 3 THEN 'warehouse'
    WHEN 4 THEN 'cashier'
    WHEN 5 THEN 'finance'
END;

-- ---------- 9. 灰度: 全员额外绑定 super_admin (上线后行为与现状一致) ----------
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT e.id, (SELECT id FROM sys_role WHERE code = 'super_admin')
FROM emp e;

-- =============================================================
-- 灰度退出 (确认前端 Phase 2 就绪后手动执行, 本脚本不执行):
-- DELETE ur FROM sys_user_role ur
--   JOIN sys_role r ON ur.role_id = r.id AND r.code = 'super_admin'
--   JOIN emp e ON ur.user_id = e.id
--  WHERE e.job IS NULL OR e.job <> 1;
-- =============================================================
