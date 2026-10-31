-- 修复password列长度，BCrypt哈希需要60个字符
ALTER TABLE emp MODIFY COLUMN password VARCHAR(100) NOT NULL DEFAULT '123456';
