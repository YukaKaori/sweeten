-- =============================================
-- 会员积分优惠券系统 · 完整建表脚本
-- 在 dessertshop 数据库上执行
-- =============================================

-- ── 1. 会员表 ──────────────────────────────────
DROP TABLE IF EXISTS `member`;
CREATE TABLE `member` (
    `id`            INT PRIMARY KEY AUTO_INCREMENT COMMENT '会员ID',
    `name`          VARCHAR(50) NOT NULL COMMENT '姓名',
    `phone`         VARCHAR(20) NOT NULL COMMENT '手机号（唯一标识）',
    `level`         INT NOT NULL DEFAULT 0 COMMENT '等级 0=普通 1=银卡 2=金卡 3=钻石',
    `points`        INT NOT NULL DEFAULT 0 COMMENT '当前积分',
    `total_points`  INT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    `growth`        INT NOT NULL DEFAULT 0 COMMENT '成长值（决定等级）',
    `total_spent`   DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '累计消费金额',
    `order_count`   INT NOT NULL DEFAULT 0 COMMENT '累计订单数',
    `birthday`      DATE COMMENT '生日（用于生日特权）',
    `registered_at` DATETIME NOT NULL COMMENT '注册时间',
    `updated_at`    DATETIME COMMENT '最后更新时间',
    UNIQUE INDEX `uk_phone` (`phone`),
    INDEX `idx_level` (`level`),
    INDEX `idx_points` (`points`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

-- ── 2. 积分变动日志表 ──────────────────────────
DROP TABLE IF EXISTS `points_log`;
CREATE TABLE `points_log` (
    `id`            INT PRIMARY KEY AUTO_INCREMENT,
    `member_id`     INT NOT NULL COMMENT '会员ID',
    `type`          TINYINT NOT NULL COMMENT '类型 1=消费获取 2=生日赠送 3=活动赠送 4=积分兑换扣除 5=过期扣除 6=管理员调整',
    `points`        INT NOT NULL COMMENT '变动积分（正=增加，负=扣除）',
    `balance`       INT NOT NULL COMMENT '变动后余额',
    `reason`        VARCHAR(100) COMMENT '变动原因',
    `related_id`    INT COMMENT '关联ID（订单ID/兑换ID等）',
    `created_at`    DATETIME NOT NULL COMMENT '变动时间',
    INDEX `idx_member_id` (`member_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分变动日志';

-- ── 3. 优惠券模板表 ────────────────────────────
DROP TABLE IF EXISTS `coupon_template`;
CREATE TABLE `coupon_template` (
    `id`              INT PRIMARY KEY AUTO_INCREMENT,
    `name`            VARCHAR(50) NOT NULL COMMENT '券名称',
    `type`            TINYINT NOT NULL COMMENT '类型 1=满减券 2=折扣券 3=现金券',
    `threshold`       DECIMAL(10,2) DEFAULT 0 COMMENT '使用门槛（满X元可用）',
    `discount`        DECIMAL(4,2) COMMENT '折扣率（0.85=85折）',
    `reduce_amount`   DECIMAL(10,2) COMMENT '减免金额（满减券用）',
    `valid_days`      INT NOT NULL DEFAULT 30 COMMENT '有效天数（自领取起）',
    `total_stock`     INT DEFAULT -1 COMMENT '总库存（-1=不限）',
    `issued_count`    INT NOT NULL DEFAULT 0 COMMENT '已发放数量',
    `points_cost`     INT DEFAULT 0 COMMENT '兑换所需积分（0=免费发放）',
    `status`          TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1=启用 0=停用',
    `created_at`      DATETIME NOT NULL,
    INDEX `idx_type` (`type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

-- ── 4. 会员持有优惠券表 ─────────────────────────
DROP TABLE IF EXISTS `member_coupon`;
CREATE TABLE `member_coupon` (
    `id`              INT PRIMARY KEY AUTO_INCREMENT,
    `member_id`       INT NOT NULL COMMENT '会员ID',
    `template_id`     INT NOT NULL COMMENT '券模板ID',
    `status`          TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1=未使用 2=已使用 3=已过期',
    `acquired_at`     DATETIME NOT NULL COMMENT '领取时间',
    `used_at`         DATETIME COMMENT '使用时间',
    `expire_at`       DATETIME NOT NULL COMMENT '过期时间',
    `order_id`        INT COMMENT '使用订单ID',
    INDEX `idx_member_id` (`member_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员优惠券';

-- ── 5. 种子数据：优惠券模板 ─────────────────────
INSERT INTO `coupon_template` (`name`, `type`, `threshold`, `discount`, `reduce_amount`, `valid_days`, `total_stock`, `points_cost`, `status`, `created_at`) VALUES
('新人专享 · 满50减10',   1, 50.00,  NULL, 10.00, 30, -1,   0, 1, NOW()),
('新人专享 · 9折券',      2, 0,     0.90, NULL,  30, -1,   0, 1, NOW()),
('银卡会员 · 满100减20',  1, 100.00, NULL, 20.00, 60, -1,   0, 1, NOW()),
('金卡会员 · 85折券',     2, 0,     0.85, NULL,  90, -1,   0, 1, NOW()),
('钻石会员 · 满200减50',  1, 200.00, NULL, 50.00, 90, -1,   0, 1, NOW()),
('积分兑换 · 满80减15',   1, 80.00,  NULL, 15.00, 30, -1, 200, 1, NOW()),
('积分兑换 · 88折券',     2, 0,     0.88, NULL,  30, -1, 300, 1, NOW()),
('积分兑换 · 5元现金券',  3, 0,     NULL, 5.00,  30, -1, 100, 1, NOW()),
('生日特权 · 8折券',      2, 0,     0.80, NULL,  30, -1,   0, 1, NOW()),
('周末特惠 · 9折券',      2, 0,     0.90, NULL,  14, 500,  0, 1, NOW());

-- ── 6. 从已有订单自动注册会员 ───────────────────
INSERT INTO `member` (`name`, `phone`, `level`, `points`, `total_points`, `growth`, `total_spent`, `order_count`, `registered_at`, `updated_at`)
SELECT
    t.customer_name,
    t.phone,
    CASE
        WHEN t.total_spent >= 10000 THEN 3
        WHEN t.total_spent >= 3000  THEN 2
        WHEN t.total_spent >= 1000  THEN 1
        ELSE 0
    END AS level,
    FLOOR(t.total_spent) AS points,
    FLOOR(t.total_spent) AS total_points,
    FLOOR(t.total_spent) AS growth,
    t.total_spent,
    t.cnt AS order_count,
    t.first_order AS registered_at,
    NOW() AS updated_at
FROM (
    SELECT
        customer_name,
        phone,
        COALESCE(SUM(CASE WHEN status IN (3,4) THEN amount ELSE 0 END), 0) AS total_spent,
        COUNT(*) AS cnt,
        MIN(create_time) AS first_order
    FROM orders
    WHERE customer_name IS NOT NULL AND customer_name != ''
      AND phone IS NOT NULL AND phone != ''
    GROUP BY customer_name, phone
) t
WHERE NOT EXISTS (SELECT 1 FROM member WHERE member.phone = t.phone);

-- ── 7. 为已有会员生成初始积分日志 ────────────────
INSERT INTO `points_log` (`member_id`, `type`, `points`, `balance`, `reason`, `created_at`)
SELECT
    m.id,
    1,
    m.points,
    m.points,
    '历史订单积分导入',
    NOW()
FROM member m
WHERE m.points > 0
  AND NOT EXISTS (SELECT 1 FROM points_log WHERE points_log.member_id = m.id);
