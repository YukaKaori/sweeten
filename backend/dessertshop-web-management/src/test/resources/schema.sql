-- =============================================
-- H2 兼容测试建表脚本
-- 覆盖核心表：emp, dept, dessert, orders
-- =============================================

-- 部门表
CREATE TABLE IF NOT EXISTS dept (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 员工表
CREATE TABLE IF NOT EXISTS emp (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    name VARCHAR(50) NOT NULL,
    gender TINYINT DEFAULT 1,
    phone VARCHAR(20),
    job TINYINT,
    salary DECIMAL(10,2),
    image VARCHAR(500),
    entry_date DATE,
    dept_id INT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 员工工作经历表
CREATE TABLE IF NOT EXISTS emp_expr (
    id INT PRIMARY KEY AUTO_INCREMENT,
    emp_id INT NOT NULL,
    company VARCHAR(100),
    job VARCHAR(50),
    "begin" DATE,
    "end" DATE
);

-- 甜品表
CREATE TABLE IF NOT EXISTS dessert (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(20) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    image VARCHAR(500),
    description VARCHAR(200),
    icon VARCHAR(10),
    sales INT DEFAULT 0,
    stock INT DEFAULT 100,
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    customer_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    items VARCHAR(2000),
    amount DECIMAL(10,2),
    status INT DEFAULT 0,
    address VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 操作日志表 (@LogOperation 切面写入)
CREATE TABLE IF NOT EXISTS operate_log (
    id INT PRIMARY KEY AUTO_INCREMENT,
    operate_emp_id INT,
    operate_time TIMESTAMP,
    class_name VARCHAR(255),
    method_name VARCHAR(100),
    method_params TEXT,
    return_value TEXT,
    cost_time BIGINT
);

-- 员工操作日志表 (EmpLogService 写入)
CREATE TABLE IF NOT EXISTS emp_log (
    id INT PRIMARY KEY AUTO_INCREMENT,
    operate_time TIMESTAMP,
    info TEXT
);

-- ============ RBAC 四表 (与 sql/rbac_init.sql 对应) ============
CREATE TABLE IF NOT EXISTS sys_role (
    id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    module VARCHAR(50) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id INT PRIMARY KEY AUTO_INCREMENT,
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    UNIQUE (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    UNIQUE (user_id, role_id)
);

-- 会员 / 积分 / 优惠券（订单完成发积分、取消扣回、积分兑换测试用）
CREATE TABLE IF NOT EXISTS member (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    level INT NOT NULL DEFAULT 0,
    points INT NOT NULL DEFAULT 0,
    total_points INT NOT NULL DEFAULT 0,
    growth INT NOT NULL DEFAULT 0,
    total_spent DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    order_count INT NOT NULL DEFAULT 0,
    birthday DATE,
    registered_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS points_log (
    id INT PRIMARY KEY AUTO_INCREMENT,
    member_id INT NOT NULL,
    type TINYINT NOT NULL,
    points INT NOT NULL,
    balance INT NOT NULL,
    reason VARCHAR(100),
    related_id INT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS coupon_template (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    type TINYINT NOT NULL,
    threshold DECIMAL(10,2) DEFAULT 0.00,
    discount DECIMAL(4,2),
    reduce_amount DECIMAL(10,2),
    valid_days INT NOT NULL DEFAULT 30,
    total_stock INT DEFAULT -1,
    issued_count INT NOT NULL DEFAULT 0,
    points_cost INT DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS member_coupon (
    id INT PRIMARY KEY AUTO_INCREMENT,
    member_id INT NOT NULL,
    template_id INT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    acquired_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    expire_at TIMESTAMP NOT NULL,
    order_id INT
);
