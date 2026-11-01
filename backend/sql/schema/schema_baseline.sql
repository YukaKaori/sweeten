-- =====================================================================
-- DessertShop 完整表结构基线（仅结构，无数据）— 2026-10-13 由本机已验证的 dessertshop 库导出
-- 仓库中此前没有任何脚本创建 emp / dept / emp_expr，新环境无法仅凭 sql/ 目录建库；本文件补齐。
-- 已包含此前各迁移脚本的结果（orders.items VARCHAR(2000)、uk_order_no、RBAC 四表等）。
-- 新库：CREATE DATABASE dessertshop DEFAULT CHARSET utf8mb4; 然后执行本文件，再按需导入种子数据。
-- =====================================================================

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `comments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `dessert_id` int NOT NULL,
  `content` varchar(500) NOT NULL,
  `rating` int NOT NULL DEFAULT '5',
  `user_id` int DEFAULT NULL,
  `username` varchar(20) DEFAULT NULL,
  `avatar_bg` varchar(20) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_dessert_id` (`dessert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `coupon_template` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '券名称',
  `type` tinyint NOT NULL COMMENT '类型 1=满减券 2=折扣券 3=现金券',
  `threshold` decimal(10,2) DEFAULT '0.00' COMMENT '使用门槛（满X元可用）',
  `discount` decimal(4,2) DEFAULT NULL COMMENT '折扣率（0.85=85折）',
  `reduce_amount` decimal(10,2) DEFAULT NULL COMMENT '减免金额（满减券用）',
  `valid_days` int NOT NULL DEFAULT '30' COMMENT '有效天数（自领取起）',
  `total_stock` int DEFAULT '-1' COMMENT '总库存（-1=不限）',
  `issued_count` int NOT NULL DEFAULT '0' COMMENT '已发放数量',
  `points_cost` int DEFAULT '0' COMMENT '兑换所需积分（0=免费发放）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1=启用 0=停用',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券模板';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `customers` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `gender` int DEFAULT '1' COMMENT '1=男,2=女',
  `address` varchar(200) DEFAULT NULL,
  `remark` varchar(100) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `dept` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(20) NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `dessert` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(100) NOT NULL COMMENT '甜品名称',
  `category` varchar(20) NOT NULL COMMENT '分类 cake/bread/drink/dessert/icecream',
  `price` decimal(10,2) NOT NULL COMMENT '现价',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `image` varchar(500) DEFAULT NULL COMMENT '图片URL',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `icon` varchar(10) DEFAULT NULL COMMENT '图标',
  `sales` int DEFAULT '0' COMMENT '月销量',
  `stock` int DEFAULT '100' COMMENT '库存数量',
  `status` int DEFAULT '1' COMMENT '状态 1上架 0下架',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='甜品表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `emp` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(20) NOT NULL,
  `password` varchar(100) DEFAULT NULL,
  `name` varchar(20) NOT NULL,
  `gender` tinyint unsigned NOT NULL,
  `phone` char(11) NOT NULL,
  `job` tinyint unsigned DEFAULT NULL,
  `salary` int unsigned DEFAULT NULL,
  `image` varchar(300) DEFAULT NULL,
  `entry_date` date DEFAULT NULL,
  `dept_id` int unsigned DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `emp_expr` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `emp_id` int unsigned DEFAULT NULL,
  `begin` date DEFAULT NULL,
  `end` date DEFAULT NULL,
  `company` varchar(50) DEFAULT NULL,
  `job` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `emp_log` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `operate_time` datetime DEFAULT NULL,
  `info` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `inventory` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  `category` varchar(20) NOT NULL COMMENT 'dairy/grain/sugar/fruit/chocolate/oil/package/other',
  `stock` int NOT NULL DEFAULT '0',
  `unit` varchar(10) NOT NULL DEFAULT 'kg',
  `safety_threshold` int NOT NULL DEFAULT '10',
  `supplier` varchar(50) DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `remark` varchar(100) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_stock` (`stock`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `member` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '会员ID',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `phone` varchar(20) NOT NULL COMMENT '手机号（唯一标识）',
  `level` int NOT NULL DEFAULT '0' COMMENT '等级 0=普通 1=银卡 2=金卡 3=钻石',
  `points` int NOT NULL DEFAULT '0' COMMENT '当前积分',
  `total_points` int NOT NULL DEFAULT '0' COMMENT '累计获得积分',
  `growth` int NOT NULL DEFAULT '0' COMMENT '成长值（决定等级）',
  `total_spent` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '累计消费金额',
  `order_count` int NOT NULL DEFAULT '0' COMMENT '累计订单数',
  `birthday` date DEFAULT NULL COMMENT '生日（用于生日特权）',
  `registered_at` datetime NOT NULL COMMENT '注册时间',
  `updated_at` datetime DEFAULT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_level` (`level`),
  KEY `idx_points` (`points`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `member_coupon` (
  `id` int NOT NULL AUTO_INCREMENT,
  `member_id` int NOT NULL COMMENT '会员ID',
  `template_id` int NOT NULL COMMENT '券模板ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1=未使用 2=已使用 3=已过期',
  `acquired_at` datetime NOT NULL COMMENT '领取时间',
  `used_at` datetime DEFAULT NULL COMMENT '使用时间',
  `expire_at` datetime NOT NULL COMMENT '过期时间',
  `order_id` int DEFAULT NULL COMMENT '使用订单ID',
  PRIMARY KEY (`id`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员优惠券';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `operate_log` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `operate_emp_id` int unsigned DEFAULT NULL,
  `operate_time` datetime DEFAULT NULL,
  `class_name` varchar(100) DEFAULT NULL,
  `method_name` varchar(100) DEFAULT NULL,
  `method_params` varchar(1000) DEFAULT NULL,
  `return_value` varchar(2000) DEFAULT NULL,
  `cost_time` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `orders` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `customer_name` varchar(50) NOT NULL COMMENT '客户姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `items` varchar(2000) DEFAULT NULL COMMENT '商品明细',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '订单金额',
  `status` int DEFAULT '0' COMMENT '订单状态 0待支付 1待接单 2制作中 3配送中 4已完成 5已取消',
  `address` varchar(200) DEFAULT NULL COMMENT '配送地址',
  `create_time` datetime DEFAULT NULL COMMENT '下单时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `points_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `member_id` int NOT NULL COMMENT '会员ID',
  `type` tinyint NOT NULL COMMENT '类型 1=消费获取 2=生日赠送 3=活动赠送 4=积分兑换扣除 5=过期扣除 6=管理员调整',
  `points` int NOT NULL COMMENT '变动积分（正=增加，负=扣除）',
  `balance` int NOT NULL COMMENT '变动后余额',
  `reason` varchar(100) DEFAULT NULL COMMENT '变动原因',
  `related_id` int DEFAULT NULL COMMENT '关联ID（订单ID/兑换ID等）',
  `created_at` datetime NOT NULL COMMENT '变动时间',
  PRIMARY KEY (`id`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分变动日志';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(100) NOT NULL COMMENT '权限码, 格式: 模块:资源:操作',
  `name` varchar(100) NOT NULL COMMENT '权限名称',
  `module` varchar(50) NOT NULL COMMENT '所属模块',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(50) NOT NULL COMMENT '角色码',
  `name` varchar(50) NOT NULL COMMENT '角色名称',
  `description` varchar(200) DEFAULT NULL COMMENT '角色说明',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id` int NOT NULL AUTO_INCREMENT,
  `role_id` int NOT NULL,
  `permission_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '关联 emp.id',
  `role_id` int NOT NULL COMMENT '关联 sys_role.id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

