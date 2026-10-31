-- =====================================================================
-- 2026-10-13  商品图换成带 Cache-Control 的 OSS 对象
--   同一张草莓蛋糕图经加固后的 POST /upload 重新上传：对象带
--   Cache-Control: public, max-age=31536000, immutable（缩放后的衍生图同样带该头），
--   浏览器不再重复请求。内容与原文件 SHA-256 一致。旧对象仍保留在 OSS 中，未删除。
-- 执行：mysql -uroot -p dessertshop < sql/product_image_cache_headers.sql
-- =====================================================================
UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/2026/10/5f6bcc06-e40d-46af-9200-78a8c143e842.png' WHERE image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/2026/10/a05e38ab-0be5-41d4-8a74-a2d9f32bb9ee.png';

-- 回滚：
-- UPDATE dessert SET image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/2026/10/a05e38ab-0be5-41d4-8a74-a2d9f32bb9ee.png' WHERE image = 'https://yuka-javaweb.oss-cn-beijing.aliyuncs.com/2026/10/5f6bcc06-e40d-46af-9200-78a8c143e842.png';
