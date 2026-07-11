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
    order_no VARCHAR(32) NOT NULL,
    customer_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    items VARCHAR(500),
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
