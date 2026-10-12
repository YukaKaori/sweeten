-- =====================================================================
-- 2026-10-12  下单扣库存 + 商品图统一
--   1) orders.items 由 VARCHAR(500) 放宽到 VARCHAR(2000)：
--      移动端下单以 JSON 存明细（含 productId，取消订单时据此回补库存），
--      7~8 种商品就会超过 500 字符导致插入失败。只放宽长度，不改动任何已有数据。
--   2) 商品图统一为草莓蛋糕（临时）：原 dessert-images/*.jpg 在 OSS 上均已 404，
--      新图经项目自身 POST /upload（AliyunOSSOperator）上传，已校验可访问且与源文件 SHA-256 一致。
--      前端另有 VITE_PRODUCT_IMAGE_URL 统一覆盖（见 dessertshop/src/config/productImage.js），
--      本步保证遗留页面（public/mobile.html、后端 static/index.html）与新增数据同样一致。
-- 执行：mysql -uroot -p dessertshop < sql/order_stock_and_images.sql
-- 回滚：见文末注释
-- =====================================================================

ALTER TABLE `orders` MODIFY `items` VARCHAR(2000) DEFAULT NULL COMMENT '商品明细';

UPDATE `dessert`
SET `image` = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/2026/10/a05e38ab-0be5-41d4-8a74-a2d9f32bb9ee.png';

-- ---------------------------------------------------------------------
-- 回滚（按需手动执行）
-- 注意：items 收窄前需确认没有超过 500 字符的明细，否则会截断/报错：
--   SELECT id FROM orders WHERE CHAR_LENGTH(items) > 500;
-- ALTER TABLE `orders` MODIFY `items` VARCHAR(500) DEFAULT NULL COMMENT '商品明细';
--
-- 商品图恢复为执行前的值（2026-10-12 本机库快照）：
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/strawberry_mousse_cake.jpg' WHERE id = 1;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/tiramisu.jpg' WHERE id = 2;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/chocolate_lava_cake.jpg' WHERE id = 3;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/mango_crepe_cake.jpg' WHERE id = 4;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/red_velvet_cake.jpg' WHERE id = 5;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/croissant.jpg' WHERE id = 6;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/whole_wheat_toast.jpg' WHERE id = 7;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/meat_floss_bread.jpg' WHERE id = 8;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/dirty_bread.jpg' WHERE id = 9;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/matcha_latte.jpg' WHERE id = 10;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/orange_juice.jpg' WHERE id = 11;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/americano_coffee.jpg' WHERE id = 12;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/blueberry_cheesecake.jpg' WHERE id = 13;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/souffle.jpg' WHERE id = 14;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/creme_brulee.jpg' WHERE id = 15;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/vanilla_icecream.jpg' WHERE id = 16;
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/dessert-images/matcha_icecream.jpg' WHERE id = 17;
