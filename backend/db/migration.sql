-- ============================================
-- 甜品店移动端 — 数据库变更脚本
-- 在 dessertshop 数据库上执行
-- ============================================

-- 1. dessert 表增加移动端所需字段
ALTER TABLE dessert ADD COLUMN IF NOT EXISTS description VARCHAR(200) COMMENT '描述';
ALTER TABLE dessert ADD COLUMN IF NOT EXISTS icon VARCHAR(10) COMMENT '图标emoji';

-- 2. 插入种子数据（如果 dessert 表为空）
-- 先用变量判断是否已有数据，避免重复插入
INSERT INTO dessert (name, category, price, original_price, image, description, icon, sales, stock, status, create_time, update_time)
SELECT * FROM (
    -- 蛋糕
    SELECT '草莓奶油蛋糕' AS name, 'cake' AS category, 38.00 AS price, 45.00 AS original_price, NULL AS image, '新鲜草莓 · 动物奶油' AS description, '🍓' AS icon, 256 AS sales, 50 AS stock, 1 AS status, NOW() AS create_time, NOW() AS update_time UNION ALL
    SELECT '提拉米苏', 'cake', 32.00, 38.00, NULL, '进口马斯卡彭 · 咖啡', '☕', 189, 40, 1, NOW(), NOW() UNION ALL
    SELECT '芒果班戟', 'cake', 18.00, 22.00, NULL, '大块芒果 · 薄皮Q弹', '🥭', 312, 60, 1, NOW(), NOW() UNION ALL
    SELECT '巧克力熔岩', 'cake', 28.00, 35.00, NULL, '72%黑巧 · 流心爆浆', '🍫', 178, 30, 1, NOW(), NOW() UNION ALL
    SELECT '抹茶千层', 'cake', 35.00, 42.00, NULL, '20层薄皮 · 抹茶奶油', '🍵', 145, 25, 1, NOW(), NOW() UNION ALL
    -- 面包
    SELECT '北海道吐司', 'bread', 22.00, 26.00, NULL, '绵软拉丝 · 奶香浓郁', '🍞', 203, 45, 1, NOW(), NOW() UNION ALL
    SELECT '可颂牛角包', 'bread', 16.00, 19.00, NULL, '法国黄油 · 层层酥脆', '🥐', 267, 55, 1, NOW(), NOW() UNION ALL
    SELECT '蒜香法棍', 'bread', 12.00, 15.00, NULL, '外脆里软 · 蒜香四溢', '🥖', 134, 35, 1, NOW(), NOW() UNION ALL
    SELECT '红豆面包', 'bread', 10.00, 12.00, NULL, '自制红豆沙 · 松软', '🫘', 198, 40, 1, NOW(), NOW() UNION ALL
    -- 饮品
    SELECT '杨枝甘露', 'drink', 24.00, 28.00, NULL, '芒果西柚 · 椰浆打底', '🥤', 289, 70, 1, NOW(), NOW() UNION ALL
    SELECT '草莓奶昔', 'drink', 26.00, 30.00, NULL, '鲜草莓 · 冰淇淋顶', '🍓', 210, 50, 1, NOW(), NOW() UNION ALL
    SELECT '冰拿铁', 'drink', 20.00, 24.00, NULL, '浓缩咖啡 · 冰牛乳', '🧊', 156, 45, 1, NOW(), NOW() UNION ALL
    SELECT '柠檬气泡水', 'drink', 15.00, 18.00, NULL, '鲜榨柠檬 · 气泡爽口', '🍋', 178, 60, 1, NOW(), NOW() UNION ALL
    -- 冰淇淋
    SELECT '香草冰淇淋', 'icecream', 18.00, 22.00, NULL, '马达加斯加香草荚', '🍦', 334, 80, 1, NOW(), NOW() UNION ALL
    SELECT '抹茶冰淇淋', 'icecream', 22.00, 26.00, NULL, '宇治抹茶 · 微苦回甘', '🍵', 198, 55, 1, NOW(), NOW() UNION ALL
    SELECT '芒果雪芭', 'icecream', 20.00, 24.00, NULL, '纯芒果果泥 · 零脂', '🥭', 167, 45, 1, NOW(), NOW() UNION ALL
    -- 小食
    SELECT '黄油曲奇', 'snack', 15.00, 18.00, NULL, '酥到掉渣 · 奶香十足', '🍪', 245, 65, 1, NOW(), NOW() UNION ALL
    SELECT '马卡龙礼盒', 'snack', 48.00, 58.00, NULL, '6枚装 · 多色多味', '🌸', 123, 30, 1, NOW(), NOW() UNION ALL
    SELECT '卡仕达泡芙', 'snack', 12.00, 15.00, NULL, '一口爆浆 · 酥皮薄壳', '💛', 289, 55, 1, NOW(), NOW() UNION ALL
    SELECT '葡式蛋挞', 'snack', 8.00, 10.00, NULL, '酥皮千层 · 焦糖顶', '🥧', 345, 70, 1, NOW(), NOW() UNION ALL
    SELECT '焦糖布丁', 'snack', 16.00, 20.00, NULL, '入口即化 · 焦糖脆壳', '🍮', 198, 40, 1, NOW(), NOW() UNION ALL
    -- 礼盒
    SELECT '甜品礼篮', 'gift', 168.00, 198.00, NULL, '精选6款 · 送礼首选', '🧺', 45, 15, 1, NOW(), NOW() UNION ALL
    SELECT '生日蛋糕礼盒', 'gift', 258.00, 298.00, NULL, '含蜡烛·贺卡·刀叉', '🎂', 67, 10, 1, NOW(), NOW() UNION ALL
    SELECT '巧克力礼盒', 'gift', 128.00, 158.00, NULL, '手工松露 · 12枚装', '💝', 89, 20, 1, NOW(), NOW()
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dessert LIMIT 1);
