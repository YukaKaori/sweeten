-- =============================================================
-- 演示用优惠券模板（仅本地开发 / 演示环境；由 init_all.sql 调用）
--
-- 会员体系依赖券模板：新会员首单自动发放名称含「新人」且 points_cost = 0 的券
-- （MemberServiceImpl.issueNewMemberCoupons），会员中心可用积分兑换 points_cost > 0 的券。
-- 此前这些模板只存在于 archive/member_points_coupon.sql（开头会 DROP 会员相关表），
-- init_all.sql / docker 建出的新库因此没有任何券，新人券与积分兑换无法演示。
--
-- 幂等：coupon_template 没有唯一键，只在表为空时整批插入，已有券的库不会产生重复。
-- 生产环境的券由运营在工作台创建，不要导入本文件。
-- =============================================================

INSERT INTO `coupon_template` (`name`, `type`, `threshold`, `discount`, `reduce_amount`, `valid_days`, `total_stock`, `issued_count`, `points_cost`, `status`, `created_at`)
SELECT * FROM (
    SELECT '新人专享 · 满50减10' AS name, 1 AS type, 50.00 AS threshold, NULL AS discount, 10.00 AS reduce_amount, 30 AS valid_days, -1 AS total_stock, 0 AS issued_count, 0 AS points_cost, 1 AS status, NOW() AS created_at UNION ALL
    SELECT '新人专享 · 9折券',      2, 0,      0.90, NULL,  30, -1,  0, 0,   1, NOW() UNION ALL
    SELECT '银卡会员 · 满100减20',  1, 100.00, NULL, 20.00, 60, -1,  0, 0,   1, NOW() UNION ALL
    SELECT '金卡会员 · 85折券',     2, 0,      0.85, NULL,  90, -1,  0, 0,   1, NOW() UNION ALL
    SELECT '钻石会员 · 满200减50',  1, 200.00, NULL, 50.00, 90, -1,  0, 0,   1, NOW() UNION ALL
    SELECT '积分兑换 · 满80减15',   1, 80.00,  NULL, 15.00, 30, -1,  0, 200, 1, NOW() UNION ALL
    SELECT '积分兑换 · 88折券',     2, 0,      0.88, NULL,  30, -1,  0, 300, 1, NOW() UNION ALL
    SELECT '积分兑换 · 5元现金券',  3, 0,      NULL, 5.00,  30, -1,  0, 100, 1, NOW() UNION ALL
    SELECT '生日特权 · 8折券',      2, 0,      0.80, NULL,  30, -1,  0, 0,   1, NOW() UNION ALL
    SELECT '周末特惠 · 9折券',      2, 0,      0.90, NULL,  14, 500, 0, 0,   1, NOW()
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM `coupon_template` LIMIT 1);
