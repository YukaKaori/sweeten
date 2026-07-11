-- =============================================================
-- RBAC 回滚脚本 — 撤销 rbac_init.sql 的全部变更
-- 注意: 回滚后须同时回滚后端代码 (旧代码不读这些表), 否则登录接口查询角色会报错
-- =============================================================
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
