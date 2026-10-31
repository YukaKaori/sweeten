-- 库存管理表
DROP TABLE IF EXISTS `inventory`;
CREATE TABLE `inventory` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(30) NOT NULL,
    `category` VARCHAR(20) NOT NULL COMMENT 'dairy/grain/sugar/fruit/chocolate/oil/package/other',
    `stock` INT NOT NULL DEFAULT 0,
    `unit` VARCHAR(10) NOT NULL DEFAULT 'kg',
    `safety_threshold` INT NOT NULL DEFAULT 10,
    `supplier` VARCHAR(50),
    `expiry_date` DATE,
    `remark` VARCHAR(100),
    `create_time` DATETIME,
    `update_time` DATETIME,
    INDEX `idx_category` (`category`),
    INDEX `idx_stock` (`stock`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `inventory` VALUES
(1, '法国淡奶油', 'dairy', 25, 'kg', 10, '北京乳品供应商', '2026-08-15', NULL, NOW(), NOW()),
(2, '低筋面粉', 'grain', 80, 'kg', 20, '河北面粉厂', '2026-12-31', NULL, NOW(), NOW()),
(3, '细砂糖', 'sugar', 45, 'kg', 15, '广州糖业', '2027-06-01', NULL, NOW(), NOW()),
(4, '新鲜草莓', 'fruit', 8, 'kg', 10, '本地农贸市场', '2026-06-20', NULL, NOW(), NOW()),
(5, '可可脂', 'chocolate', 12, 'kg', 5, '进口食品商行', '2027-01-15', NULL, NOW(), NOW()),
(6, '黄油', 'dairy', 18, 'kg', 8, '北京乳品供应商', '2026-09-30', NULL, NOW(), NOW()),
(7, '蛋糕盒', 'package', 200, 'pcs', 50, '包装材料厂', NULL, NULL, NOW(), NOW()),
(8, '蓝莓果酱', 'fruit', 6, 'kg', 8, '本地农贸市场', '2026-07-10', NULL, NOW(), NOW());

-- 客户管理表
DROP TABLE IF EXISTS `customers`;
CREATE TABLE `customers` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(30) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `gender` INT DEFAULT 1 COMMENT '1=男,2=女',
    `address` VARCHAR(200),
    `remark` VARCHAR(100),
    `create_time` DATETIME,
    `update_time` DATETIME,
    INDEX `idx_name` (`name`),
    INDEX `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 从订单中提取已有客户
INSERT INTO `customers` (`name`, `phone`, `address`, `create_time`, `update_time`)
SELECT DISTINCT customer_name, phone, MAX(address), NOW(), NOW()
FROM orders
WHERE customer_name IS NOT NULL AND customer_name != ''
GROUP BY customer_name, phone;

-- 评论表
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `dessert_id` INT NOT NULL,
    `content` VARCHAR(500) NOT NULL,
    `rating` INT NOT NULL DEFAULT 5,
    `user_id` INT,
    `username` VARCHAR(20),
    `avatar_bg` VARCHAR(20),
    `created_at` DATETIME NOT NULL,
    INDEX `idx_dessert_id` (`dessert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `comments` VALUES
(1, 1, '草莓慕斯口感细腻，甜度刚好，是我吃过最好吃的蛋糕！', 5, 7, '绵绵', '#e8637a', NOW()),
(2, 1, '卖相很好，配送也很快，推荐！', 4, 1, '柠欣', '#6b8cce', NOW()),
(3, 6, '可颂层次分明，黄油味很香，搭配咖啡绝了', 5, 3, '知柚', '#f0a35c', NOW()),
(4, 10, '抹茶味很正，不是特别甜，适合中国人口味', 4, 2, '惜音', '#5cb88a', NOW()),
(5, 2, '提拉米苏的咖啡味偏淡，希望可以加强', 3, 4, '晏歌', '#a78bfa', NOW());
