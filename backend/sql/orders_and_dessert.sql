-- =============================================
-- DessertShop 数据库完整初始化脚本
-- 可直接在 DataGrip 中全选运行，重复执行安全
-- =============================================

-- ── 1. 订单表 ──────────────────────────────────
CREATE TABLE IF NOT EXISTS `orders` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `customer_name` VARCHAR(50) NOT NULL COMMENT '客户姓名',
    `phone` VARCHAR(20) COMMENT '联系电话',
    `items` VARCHAR(500) COMMENT '商品明细',
    `amount` DECIMAL(10,2) COMMENT '订单金额',
    `status` INT DEFAULT 0 COMMENT '订单状态 0待支付 1待接单 2制作中 3配送中 4已完成 5已取消',
    `address` VARCHAR(200) COMMENT '配送地址',
    `create_time` DATETIME COMMENT '下单时间',
    `update_time` DATETIME COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ── 2. 甜品表 ──────────────────────────────────
CREATE TABLE IF NOT EXISTS `dessert` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `name` VARCHAR(100) NOT NULL COMMENT '甜品名称',
    `category` VARCHAR(20) NOT NULL COMMENT '分类 cake/bread/drink/dessert/icecream',
    `price` DECIMAL(10,2) NOT NULL COMMENT '现价',
    `original_price` DECIMAL(10,2) COMMENT '原价',
    `image` VARCHAR(500) COMMENT '图片URL',
    `sales` INT DEFAULT 0 COMMENT '月销量',
    `stock` INT DEFAULT 100 COMMENT '库存数量',
    `status` INT DEFAULT 1 COMMENT '状态 1上架 0下架',
    `create_time` DATETIME COMMENT '创建时间',
    `update_time` DATETIME COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='甜品表';

-- ── 3. 补 stock 列（旧表升级用） ────────────────
DROP PROCEDURE IF EXISTS add_stock_column;
DELIMITER $$
CREATE PROCEDURE add_stock_column()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'dessert'
          AND COLUMN_NAME = 'stock'
    ) THEN
        ALTER TABLE `dessert` ADD COLUMN `stock` INT DEFAULT 100 COMMENT '库存数量' AFTER `sales`;
    END IF;
END $$
DELIMITER ;
CALL add_stock_column();
DROP PROCEDURE IF EXISTS add_stock_column;

-- ── 4. 清理重复数据 + 建立 name 唯一索引 ─────────
DROP PROCEDURE IF EXISTS dedupe_and_index;
DELIMITER $$
CREATE PROCEDURE dedupe_and_index()
BEGIN
    -- 4a. 去重：保留每个 name 下 id 最小的那行
    DELETE FROM `dessert`
    WHERE `id` IN (
        SELECT id FROM (
            SELECT t1.id
            FROM dessert t1
            INNER JOIN dessert t2 ON t1.name = t2.name AND t1.id > t2.id
        ) AS dup
    );

    -- 4b. 建唯一索引（不存在时才建）
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'dessert'
          AND INDEX_NAME = 'uk_name'
    ) THEN
        ALTER TABLE `dessert` ADD UNIQUE INDEX `uk_name` (`name`);
    END IF;
END $$
DELIMITER ;
CALL dedupe_and_index();
DROP PROCEDURE IF EXISTS dedupe_and_index;

-- ── 5. 插入/更新 17 个甜品（含 OSS CDN 图片） ────
INSERT INTO `dessert` (`name`, `category`, `price`, `original_price`, `image`, `sales`, `stock`, `status`, `create_time`, `update_time`) VALUES
('草莓慕斯蛋糕',   'cake',     128.00, 158.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/strawberry_mousse_cake.jpg', 326,  45,  1, NOW(), NOW()),
('提拉米苏',       'cake',      88.00, 108.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/tiramisu.jpg',              218,  32,  1, NOW(), NOW()),
('巧克力熔岩蛋糕', 'cake',      98.00,  98.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/chocolate_lava_cake.jpg',   156,   8,  1, NOW(), NOW()),
('芒果千层蛋糕',   'cake',     138.00, 168.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/mango_crepe_cake.jpg',      289,  56,  1, NOW(), NOW()),
('红丝绒蛋糕',     'cake',     108.00, 128.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/red_velvet_cake.jpg',       175, 120,  0, NOW(), NOW()),
('法式可颂',       'bread',     15.00,  18.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/croissant.jpg',            1024, 200,  1, NOW(), NOW()),
('全麦吐司',       'bread',     22.00,  22.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/whole_wheat_toast.jpg',     856, 150,  1, NOW(), NOW()),
('肉松面包',       'bread',     12.00,  15.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/meat_floss_bread.jpg',      967,   5,  1, NOW(), NOW()),
('脏脏包',         'bread',     18.00,  18.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/dirty_bread.jpg',           543,  15,  1, NOW(), NOW()),
('抹茶拿铁',       'drink',     28.00,  32.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/matcha_latte.jpg',         1892, 300,  1, NOW(), NOW()),
('鲜榨橙汁',       'drink',     22.00,  22.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/orange_juice.jpg',         1234,  80,  1, NOW(), NOW()),
('美式咖啡',       'drink',     18.00,  22.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/americano_coffee.jpg',     2156, 250,  1, NOW(), NOW()),
('蓝莓芝士蛋糕',   'dessert',   68.00,  78.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/blueberry_cheesecake.jpg',  432,  18,  1, NOW(), NOW()),
('舒芙蕾',         'dessert',   48.00,  58.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/souffle.jpg',               367,  10,  1, NOW(), NOW()),
('焦糖布丁',       'dessert',   32.00,  38.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/creme_brulee.jpg',          589,  65,  1, NOW(), NOW()),
('香草冰淇淋',     'icecream',  25.00,  25.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/vanilla_icecream.jpg',      765,  90,  1, NOW(), NOW()),
('抹茶冰淇淋',     'icecream',  28.00,  32.00, 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/matcha_icecream.jpg',       654,   3,  1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `price`          = VALUES(`price`),
    `original_price` = VALUES(`original_price`),
    `image`          = VALUES(`image`),
    `sales`          = VALUES(`sales`),
    `stock`          = VALUES(`stock`),
    `status`         = VALUES(`status`),
    `update_time`    = NOW();

-- ── 6. 订单初始数据 ──────────────────────────────
INSERT IGNORE INTO `orders` (`order_no`, `customer_name`, `phone`, `items`, `amount`, `status`, `address`, `create_time`, `update_time`) VALUES
('DS20240501001', '林晚晴', '13800138001', '草莓慕斯蛋糕 x1, 抹茶拿铁 x2', 168.00, 4, '朝阳区建国路88号', '2024-05-01 09:30:00', '2024-05-01 09:30:00'),
('DS20240502001', '苏念安', '13800138002', '提拉米苏 x1', 88.00, 4, '海淀区中关村大街1号', '2024-05-02 10:15:00', '2024-05-02 10:15:00'),
('DS20240503001', '陈知许', '13800138003', '巧克力熔岩蛋糕 x2, 鲜榨果汁 x1', 236.00, 4, '西城区金融街15号', '2024-05-03 11:00:00', '2024-05-03 11:00:00'),
('DS20240505001', '沈予初', '13800138004', '法式可颂 x3', 45.00, 4, '东城区王府井大街100号', '2024-05-05 11:45:00', '2024-05-05 11:45:00'),
('DS20240506001', '温以宁', '13800138005', '芒果千层蛋糕 x1', 128.00, 4, '丰台区南三环西路16号', '2024-05-06 12:30:00', '2024-05-06 12:30:00'),
('DS20240508001', '叶舒窈', '13800138007', '红丝绒蛋糕 x1', 98.00, 4, '昌平区回龙观东大街', '2024-05-08 14:20:00', '2024-05-08 14:20:00'),
('DS20240510001', '江映竹', '13800138008', '舒芙蕾 x2, 拿铁 x1', 156.00, 4, '大兴区亦庄经济开发区', '2024-05-10 15:10:00', '2024-05-10 15:10:00'),
('DS20240512001', '林晚晴', '13800138001', '全麦吐司 x2, 肉松面包 x3', 80.00, 4, '朝阳区建国路88号', '2024-05-12 08:30:00', '2024-05-12 08:30:00'),
('DS20240513001', '苏念安', '13800138002', '焦糖布丁 x2', 64.00, 4, '海淀区中关村大街1号', '2024-05-13 16:00:00', '2024-05-13 16:00:00'),
('DS20240515001', '陈知许', '13800138003', '抹茶冰淇淋 x3', 84.00, 4, '西城区金融街15号', '2024-05-15 10:00:00', '2024-05-15 10:00:00'),
('DS20240516001', '沈予初', '13800138004', '草莓慕斯蛋糕 x1', 128.00, 4, '东城区王府井大街100号', '2024-05-16 11:00:00', '2024-05-16 11:00:00'),
('DS20240518001', '温以宁', '13800138005', '脏脏包 x5, 鲜榨橙汁 x2', 134.00, 4, '丰台区南三环西路16号', '2024-05-18 09:00:00', '2024-05-18 09:00:00'),
('DS20240520001', '顾清禾', '13800138006', '提拉米苏 x2, 美式咖啡 x1', 194.00, 4, '通州区万达广场B座', '2024-05-20 14:00:00', '2024-05-20 14:00:00'),
('DS20240521001', '叶舒窈', '13800138007', '香草冰淇淋 x2', 50.00, 4, '昌平区回龙观东大街', '2024-05-21 15:30:00', '2024-05-21 15:30:00'),
('DS20240522001', '江映竹', '13800138008', '巧克力熔岩蛋糕 x1, 抹茶拿铁 x1', 126.00, 2, '大兴区亦庄经济开发区', '2024-05-22 10:00:00', '2024-05-22 10:00:00'),
('DS20240523001', '林晚晴', '13800138001', '草莓慕斯蛋糕 x1, 抹茶拿铁 x2', 168.00, 4, '朝阳区建国路88号', '2024-05-23 09:30:00', '2024-05-23 09:30:00'),
('DS20240523002', '苏念安', '13800138002', '提拉米苏 x1', 88.00, 3, '海淀区中关村大街1号', '2024-05-23 10:15:00', '2024-05-23 10:15:00'),
('DS20240523003', '陈知许', '13800138003', '巧克力熔岩蛋糕 x2, 鲜榨果汁 x1', 236.00, 2, '西城区金融街15号', '2024-05-23 11:00:00', '2024-05-23 11:00:00'),
('DS20240523004', '沈予初', '13800138004', '法式可颂 x3', 45.00, 1, '东城区王府井大街100号', '2024-05-23 11:45:00', '2024-05-23 11:45:00'),
('DS20240523005', '温以宁', '13800138005', '芒果千层蛋糕 x1', 128.00, 0, '丰台区南三环西路16号', '2024-05-23 12:30:00', '2024-05-23 12:30:00'),
('DS20240523006', '顾清禾', '13800138006', '蓝莓芝士蛋糕 x1, 美式咖啡 x1', 108.00, 5, '通州区万达广场B座', '2024-05-23 13:00:00', '2024-05-23 13:00:00'),
('DS20240523007', '叶舒窈', '13800138007', '红丝绒蛋糕 x1', 98.00, 4, '昌平区回龙观东大街', '2024-05-23 14:20:00', '2024-05-23 14:20:00'),
('DS20240523008', '江映竹', '13800138008', '舒芙蕾 x2, 拿铁 x1', 156.00, 2, '大兴区亦庄经济开发区', '2024-05-23 15:10:00', '2024-05-23 15:10:00');
