-- =============================================
-- H2 兼容测试种子数据
-- =============================================

-- 部门
INSERT INTO dept (id, name) VALUES (1, '技术部');
INSERT INTO dept (id, name) VALUES (2, '运营部');
INSERT INTO dept (id, name) VALUES (3, '市场部');

-- 员工 (密码: 123456 的 BCrypt hash)
INSERT INTO emp (id, username, password, name, gender, phone, job, salary, dept_id, entry_date)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '管理员', 1, '13800000000', 1, 10000.00, 1, '2020-01-01');
INSERT INTO emp (id, username, password, name, gender, phone, job, salary, dept_id, entry_date)
VALUES (2, 'chef01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '张厨师', 1, '13800000001', 2, 8000.00, 2, '2021-06-01');

-- 甜品
INSERT INTO dessert (id, name, category, price, original_price, sales, stock, status)
VALUES (1, '草莓慕斯', 'cake', 38.00, 45.00, 100, 50, 1);
INSERT INTO dessert (id, name, category, price, original_price, sales, stock, status)
VALUES (2, '提拉米苏', 'cake', 32.00, 38.00, 80, 30, 1);
INSERT INTO dessert (id, name, category, price, original_price, sales, stock, status)
VALUES (3, '抹茶拿铁', 'drink', 28.00, 32.00, 200, 100, 1);
INSERT INTO dessert (id, name, category, price, original_price, sales, stock, status)
VALUES (4, '红丝绒蛋糕', 'cake', 108.00, 128.00, 50, 0, 0);

-- 订单
INSERT INTO orders (id, order_no, customer_name, phone, items, amount, status, address)
VALUES (1, 'DS20240601001', '测试用户A', '13900000001', '草莓慕斯 x1', 38.00, 4, '北京市朝阳区');
INSERT INTO orders (id, order_no, customer_name, phone, items, amount, status, address)
VALUES (2, 'DS20240601002', '测试用户B', '13900000002', '提拉米苏 x2', 64.00, 2, '北京市海淀区');
INSERT INTO orders (id, order_no, customer_name, phone, items, amount, status, address)
VALUES (3, 'DS20240601003', '测试用户C', '13900000003', '抹茶拿铁 x1, 草莓慕斯 x1', 66.00, 0, '北京市西城区');
