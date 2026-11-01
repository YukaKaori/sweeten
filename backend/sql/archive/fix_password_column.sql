-- =====================================================================
-- ⚠ 已归档的历史脚本（2026-10-31 归档）：仅保留作历史记录，请勿执行。
-- 一次性修复（password 列加长到 100），schema_baseline.sql 已包含结果；默认值 123456 为明文，不应再使用。
-- 新库请用 sql/init_all.sql（本地）或 sql/schema/（生产），已有库升级见 sql/migrations/，说明见 sql/README.md。
-- =====================================================================

-- 修复password列长度，BCrypt哈希需要60个字符
ALTER TABLE emp MODIFY COLUMN password VARCHAR(100) NOT NULL DEFAULT '123456';
