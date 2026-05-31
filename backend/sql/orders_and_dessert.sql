-- =============================================
-- DessertShop 数据库初始化 + 升级脚本
-- 可在 DataGrip 中直接运行，重复执行不会报错
-- =============================================

-- 订单表
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

-- 订单表初始数据（IGNORE 跳过已存在的记录）
INSERT IGNORE INTO `orders` (`order_no`, `customer_name`, `phone`, `items`, `amount`, `status`, `address`, `create_time`, `update_time`) VALUES
('DS20240523001', '林晚晴', '13800138001', '草莓慕斯蛋糕 x1, 抹茶拿铁 x2', 168.00, 4, '朝阳区建国路88号', '2024-05-23 09:30:00', '2024-05-23 09:30:00'),
('DS20240523002', '苏念安', '13800138002', '提拉米苏 x1', 88.00, 3, '海淀区中关村大街1号', '2024-05-23 10:15:00', '2024-05-23 10:15:00'),
('DS20240523003', '陈知许', '13800138003', '巧克力熔岩蛋糕 x2, 鲜榨果汁 x1', 236.00, 2, '西城区金融街15号', '2024-05-23 11:00:00', '2024-05-23 11:00:00'),
('DS20240523004', '沈予初', '13800138004', '法式可颂 x3', 45.00, 1, '东城区王府井大街100号', '2024-05-23 11:45:00', '2024-05-23 11:45:00'),
('DS20240523005', '温以宁', '13800138005', '芒果千层蛋糕 x1', 128.00, 0, '丰台区南三环西路16号', '2024-05-23 12:30:00', '2024-05-23 12:30:00'),
('DS20240523006', '顾清禾', '13800138006', '蓝莓芝士蛋糕 x1, 美式咖啡 x1', 108.00, 5, '通州区万达广场B座', '2024-05-23 13:00:00', '2024-05-23 13:00:00'),
('DS20240523007', '叶舒窈', '13800138007', '红丝绒蛋糕 x1', 98.00, 4, '昌平区回龙观东大街', '2024-05-23 14:20:00', '2024-05-23 14:20:00'),
('DS20240523008', '江映竹', '13800138008', '舒芙蕾 x2, 拿铁 x1', 156.00, 2, '大兴区亦庄经济开发区', '2024-05-23 15:10:00', '2024-05-23 15:10:00');


-- 甜品表（建表语句包含 stock 列，全新安装时自动创建）
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

-- =============================================
-- 升级步骤1：给已有表添加 stock 列（必须在 INSERT 之前执行）
-- 通过存储过程实现 MySQL 的 "IF NOT EXISTS ADD COLUMN" 逻辑
-- =============================================
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

-- =============================================
-- 升级步骤2：插入/更新甜品数据（stock 列已保证存在）
-- =============================================
INSERT IGNORE INTO `dessert` (`name`, `category`, `price`, `original_price`, `image`, `sales`, `stock`, `status`, `create_time`, `update_time`) VALUES
('草莓慕斯蛋糕', 'cake', 128.00, 158.00, '', 326, 45, 1, NOW(), NOW()),
('提拉米苏', 'cake', 88.00, 108.00, '', 218, 32, 1, NOW(), NOW()),
('巧克力熔岩蛋糕', 'cake', 98.00, 98.00, '', 156, 8, 1, NOW(), NOW()),
('芒果千层蛋糕', 'cake', 138.00, 168.00, '', 289, 56, 1, NOW(), NOW()),
('红丝绒蛋糕', 'cake', 108.00, 128.00, '', 175, 120, 0, NOW(), NOW()),
('法式可颂', 'bread', 15.00, 18.00, '', 1024, 200, 1, NOW(), NOW()),
('全麦吐司', 'bread', 22.00, 22.00, '', 856, 150, 1, NOW(), NOW()),
('肉松面包', 'bread', 12.00, 15.00, '', 967, 5, 1, NOW(), NOW()),
('脏脏包', 'bread', 18.00, 18.00, '', 543, 15, 1, NOW(), NOW()),
('抹茶拿铁', 'drink', 28.00, 32.00, '', 1892, 300, 1, NOW(), NOW()),
('鲜榨橙汁', 'drink', 22.00, 22.00, '', 1234, 80, 1, NOW(), NOW()),
('美式咖啡', 'drink', 18.00, 22.00, '', 2156, 250, 1, NOW(), NOW()),
('蓝莓芝士蛋糕', 'dessert', 68.00, 78.00, '', 432, 18, 1, NOW(), NOW()),
('舒芙蕾', 'dessert', 48.00, 58.00, '', 367, 10, 1, NOW(), NOW()),
('焦糖布丁', 'dessert', 32.00, 38.00, '', 589, 65, 1, NOW(), NOW()),
('香草冰淇淋', 'icecream', 25.00, 25.00, '', 765, 90, 1, NOW(), NOW()),
('抹茶冰淇淋', 'icecream', 28.00, 32.00, '', 654, 3, 1, NOW(), NOW());

-- 更新已有甜品的库存（INSERT IGNORE 跳过的旧记录在这里更新）
UPDATE `dessert` SET `stock` = 45  WHERE `name` = '草莓慕斯蛋糕';
UPDATE `dessert` SET `stock` = 32  WHERE `name` = '提拉米苏';
UPDATE `dessert` SET `stock` = 8   WHERE `name` = '巧克力熔岩蛋糕';
UPDATE `dessert` SET `stock` = 56  WHERE `name` = '芒果千层蛋糕';
UPDATE `dessert` SET `stock` = 120 WHERE `name` = '红丝绒蛋糕';
UPDATE `dessert` SET `stock` = 200 WHERE `name` = '法式可颂';
UPDATE `dessert` SET `stock` = 150 WHERE `name` = '全麦吐司';
UPDATE `dessert` SET `stock` = 5   WHERE `name` = '肉松面包';
UPDATE `dessert` SET `stock` = 15  WHERE `name` = '脏脏包';
UPDATE `dessert` SET `stock` = 300 WHERE `name` = '抹茶拿铁';
UPDATE `dessert` SET `stock` = 80  WHERE `name` = '鲜榨橙汁';
UPDATE `dessert` SET `stock` = 250 WHERE `name` = '美式咖啡';
UPDATE `dessert` SET `stock` = 18  WHERE `name` = '蓝莓芝士蛋糕';
UPDATE `dessert` SET `stock` = 10  WHERE `name` = '舒芙蕾';
UPDATE `dessert` SET `stock` = 65  WHERE `name` = '焦糖布丁';
UPDATE `dessert` SET `stock` = 90  WHERE `name` = '香草冰淇淋';
UPDATE `dessert` SET `stock` = 3   WHERE `name` = '抹茶冰淇淋';

-- =============================================
-- 升级步骤3：补充更多订单数据（覆盖近30天，使 Dashboard 营收图表有数据展示）
-- =============================================
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
('DS20240522001', '江映竹', '13800138008', '巧克力熔岩蛋糕 x1, 抹茶拿铁 x1', 126.00, 2, '大兴区亦庄经济开发区', '2024-05-22 10:00:00', '2024-05-22 10:00:00');
