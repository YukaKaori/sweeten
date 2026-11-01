-- =====================================================================
-- ⚠ 已归档的历史脚本（2026-10-31 归档）：仅保留作历史记录，请勿执行。
-- operate_log / emp_log 建表语句，已并入 schema_baseline.sql。
-- 新库请用 sql/init_all.sql（本地）或 sql/schema/（生产），已有库升级见 sql/migrations/，说明见 sql/README.md。
-- =====================================================================

-- ============================================
-- 操作日志表 — @LogOperation AOP 切面写入
-- ============================================
CREATE TABLE IF NOT EXISTS `operate_log` (
    `id`              INT(11)      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `operate_emp_id`  INT(11)      DEFAULT NULL COMMENT '操作人ID',
    `operate_time`    DATETIME     DEFAULT NULL COMMENT '操作时间',
    `class_name`      VARCHAR(255) DEFAULT NULL COMMENT '操作类名',
    `method_name`     VARCHAR(100) DEFAULT NULL COMMENT '操作方法名',
    `method_params`   TEXT         DEFAULT NULL COMMENT '方法参数',
    `return_value`    TEXT         DEFAULT NULL COMMENT '返回值',
    `cost_time`       BIGINT(20)   DEFAULT NULL COMMENT '耗时(ms)',
    PRIMARY KEY (`id`),
    KEY `idx_operate_log_time`   (`operate_time`),
    KEY `idx_operate_log_emp_id` (`operate_emp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================
-- 员工操作日志表 — EmpLogService 写入
-- ============================================
CREATE TABLE IF NOT EXISTS `emp_log` (
    `id`           INT(11)   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `operate_time` DATETIME  DEFAULT NULL COMMENT '操作时间',
    `info`         TEXT      DEFAULT NULL COMMENT '操作详情',
    PRIMARY KEY (`id`),
    KEY `idx_emp_log_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工操作日志表';
