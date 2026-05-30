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

-- 订单表测试数据
INSERT INTO `orders` (`order_no`, `customer_name`, `phone`, `items`, `amount`, `status`, `address`, `create_time`, `update_time`) VALUES
('DS20240523001', '林晚晴', '13800138001', '草莓慕斯蛋糕 x1, 抹茶拿铁 x2', 168.00, 4, '朝阳区建国路88号', '2024-05-23 09:30:00', '2024-05-23 09:30:00'),
('DS20240523002', '苏念安', '13800138002', '提拉米苏 x1', 88.00, 3, '海淀区中关村大街1号', '2024-05-23 10:15:00', '2024-05-23 10:15:00'),
('DS20240523003', '陈知许', '13800138003', '巧克力熔岩蛋糕 x2, 鲜榨果汁 x1', 236.00, 2, '西城区金融街15号', '2024-05-23 11:00:00', '2024-05-23 11:00:00'),
('DS20240523004', '沈予初', '13800138004', '法式可颂 x3', 45.00, 1, '东城区王府井大街100号', '2024-05-23 11:45:00', '2024-05-23 11:45:00'),
('DS20240523005', '温以宁', '13800138005', '芒果千层蛋糕 x1', 128.00, 0, '丰台区南三环西路16号', '2024-05-23 12:30:00', '2024-05-23 12:30:00'),
('DS20240523006', '顾清禾', '13800138006', '蓝莓芝士蛋糕 x1, 美式咖啡 x1', 108.00, 5, '通州区万达广场B座', '2024-05-23 13:00:00', '2024-05-23 13:00:00'),
('DS20240523007', '叶舒窈', '13800138007', '红丝绒蛋糕 x1', 98.00, 4, '昌平区回龙观东大街', '2024-05-23 14:20:00', '2024-05-23 14:20:00'),
('DS20240523008', '江映竹', '13800138008', '舒芙蕾 x2, 拿铁 x1', 156.00, 2, '大兴区亦庄经济开发区', '2024-05-23 15:10:00', '2024-05-23 15:10:00');


-- 甜品表
CREATE TABLE IF NOT EXISTS `dessert` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `name` VARCHAR(100) NOT NULL COMMENT '甜品名称',
    `category` VARCHAR(20) NOT NULL COMMENT '分类 cake/bread/drink/dessert/icecream',
    `price` DECIMAL(10,2) NOT NULL COMMENT '现价',
    `original_price` DECIMAL(10,2) COMMENT '原价',
    `image` VARCHAR(500) COMMENT '图片URL',
    `sales` INT DEFAULT 0 COMMENT '月销量',
    `status` INT DEFAULT 1 COMMENT '状态 1上架 0下架',
    `create_time` DATETIME COMMENT '创建时间',
    `update_time` DATETIME COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='甜品表';

-- 甜品表测试数据
INSERT INTO `dessert` (`name`, `category`, `price`, `original_price`, `image`, `sales`, `status`, `create_time`, `update_time`) VALUES
('草莓慕斯蛋糕', 'cake', 128.00, 158.00, '', 326, 1, NOW(), NOW()),
('提拉米苏', 'cake', 88.00, 108.00, '', 218, 1, NOW(), NOW()),
('巧克力熔岩蛋糕', 'cake', 98.00, 98.00, '', 156, 1, NOW(), NOW()),
('芒果千层蛋糕', 'cake', 138.00, 168.00, '', 289, 1, NOW(), NOW()),
('红丝绒蛋糕', 'cake', 108.00, 128.00, '', 175, 0, NOW(), NOW()),
('法式可颂', 'bread', 15.00, 18.00, '', 1024, 1, NOW(), NOW()),
('全麦吐司', 'bread', 22.00, 22.00, '', 856, 1, NOW(), NOW()),
('肉松面包', 'bread', 12.00, 15.00, '', 967, 1, NOW(), NOW()),
('脏脏包', 'bread', 18.00, 18.00, '', 543, 1, NOW(), NOW()),
('抹茶拿铁', 'drink', 28.00, 32.00, '', 1892, 1, NOW(), NOW()),
('鲜榨橙汁', 'drink', 22.00, 22.00, '', 1234, 1, NOW(), NOW()),
('美式咖啡', 'drink', 18.00, 22.00, '', 2156, 1, NOW(), NOW()),
('蓝莓芝士蛋糕', 'dessert', 68.00, 78.00, '', 432, 1, NOW(), NOW()),
('舒芙蕾', 'dessert', 48.00, 58.00, '', 367, 1, NOW(), NOW()),
('焦糖布丁', 'dessert', 32.00, 38.00, '', 589, 1, NOW(), NOW()),
('香草冰淇淋', 'icecream', 25.00, 25.00, '', 765, 1, NOW(), NOW()),
('抹茶冰淇淋', 'icecream', 28.00, 32.00, '', 654, 1, NOW(), NOW());
