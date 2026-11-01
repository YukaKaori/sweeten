-- =============================================================
-- 本地开发用默认管理员：admin / 123456（超级管理员）
-- 仅供本地运行和演示，生产环境不要执行。
-- 依赖: schema_baseline.sql、rbac_init.sql 已执行；重复执行安全
-- =============================================================

-- 密码为 BCrypt(cost 12) 哈希，明文 123456
INSERT IGNORE INTO emp (username, password, name, gender, phone, create_time, update_time)
VALUES ('admin', '$2a$12$EomIDadCnBiJUlmkayLOxOwLOapjnNGEwowEoq7aojNH65qzjYskC', '管理员', 1, '13800000000', NOW(), NOW());

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT e.id, r.id FROM emp e JOIN sys_role r ON r.code = 'super_admin'
WHERE e.username = 'admin';
