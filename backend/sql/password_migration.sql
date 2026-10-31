-- =====================================================
-- 密码迁移脚本
-- =====================================================
-- 说明：此脚本用于将明文密码转换为BCrypt加密格式
-- 注意：请先备份数据库再执行此脚本！
-- =====================================================

-- 1. 备份原始数据（可选，但强烈建议）
-- CREATE TABLE emp_backup_20240101 AS SELECT * FROM emp;

-- 2. 查看当前员工数据
SELECT id, username, password,
       CASE
           WHEN password LIKE '$2a$%' THEN '已加密'
           ELSE '未加密'
       END AS status
FROM emp;

-- =====================================================
-- 方式一：使用默认密码加密（适用于所有用户使用相同密码的情况）
-- =====================================================

-- 默认密码：123456
-- BCrypt哈希值（由Java程序生成）：
-- $2a$12$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi

-- 更新所有未加密的密码
-- UPDATE emp
-- SET password = '$2a$12$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi'
-- WHERE password NOT LIKE '$2a$%';

-- =====================================================
-- 方式二：针对不同密码分别加密（推荐使用Java程序）
-- =====================================================
-- 如果不同用户有不同的密码，建议使用Java程序进行迁移
-- 参考：PasswordMigrationTest.java 或 PasswordMigrationRunner.java

-- =====================================================
-- 验证迁移结果
-- =====================================================
SELECT id, username,
       CASE
           WHEN password LIKE '$2a$%' THEN '已加密'
           ELSE '未加密'
       END AS status,
       LEFT(password, 20) AS password_preview
FROM emp;

-- =====================================================
-- 回滚脚本（如果需要回滚）
-- =====================================================
-- 如果备份了数据，可以使用以下命令回滚：
-- TRUNCATE TABLE emp;
-- INSERT INTO emp SELECT * FROM emp_backup_20240101;
