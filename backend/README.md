# 🍰 甜品店管理系统 (Dessert Shop Management System)

基于 **Spring Boot 3 + MyBatis + MySQL** 的甜品店后台管理系统，提供员工管理、甜品管理、订单管理、数据看板等功能。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 22 | 运行环境 |
| Spring Boot | 3.5.7 | 应用框架 |
| MyBatis | 3.0.5 | ORM 框架 |
| MySQL | - | 数据库 |
| JWT (jjwt) | 0.12.6 | 身份认证 |
| BCrypt | 0.10.2 | 密码加密 |
| PageHelper | 1.4.7 | 分页插件 |
| 阿里云 OSS | 3.17.4 | 文件存储 |
| Lombok | 1.18.42 | 代码简化 |
| Maven | - | 项目构建 |

## 项目结构

```
newdessertshop/
├── dessert-parent/                    # 父工程（聚合模块）
│   └── pom.xml
├── dessertshop-pojo/                  # 实体类模块
│   └── src/main/java/com/itheima/pojo/
├── dessertshop-utils/                 # 工具类模块
│   └── src/main/java/com/itheima/utils/
├── dessertshop-web-management/        # Web 应用模块
│   └── src/main/
│       ├── java/com/itheima/
│       │   ├── controller/            # 控制器
│       │   ├── service/               # 业务层
│       │   ├── mapper/                # 数据访问层
│       │   ├── filter/                # 过滤器（Token 认证）
│       │   ├── config/                # 配置类
│       │   ├── aop/                   # AOP 日志切面
│       │   ├── annotation/            # 自定义注解
│       │   └── exception/             # 全局异常处理
│       └── resources/
│           ├── application.yml        # 应用配置
│           └── com/itheima/mapper/    # MyBatis XML 映射文件
└── sql/                               # 数据库脚本
```

## 功能模块

### 🔐 认证与安全
- 用户名/密码登录，返回 JWT Token
- BCrypt 密码加密存储
- Token 过滤器拦截未认证请求
- 支持修改密码（需验证旧密码）

### 👤 员工管理 (`/emps`)
- 员工列表分页查询（支持按姓名、性别、入职日期筛选）
- 员工增删改查（支持批量删除）
- 员工工作经历管理
- 员工个人信息查看

### 🏢 部门管理 (`/depts`)
- 部门增删改查

### 🧁 甜品管理 (`/desserts`)
- 甜品列表分页查询（支持按名称、分类筛选）
- 甜品增删改查
- 分类统计（蛋糕、面包、饮品、甜品、冰淇淋）
- 上下架状态管理

### 📦 订单管理 (`/orders`)
- 订单列表分页查询（支持按订单号、客户名、状态、日期筛选）
- 订单增删改查
- 订单状态流转：待支付 → 待接单 → 制作中 → 配送中 → 已完成 / 已取消

### 📊 数据看板 (`/dashboard`)
- 统计卡片：月营收、月订单数、客单价、完成率
- 营收趋势图（近 7 天 / 30 天）
- 热销甜品排行
- 库存预警（库存低于 20）

### 📋 操作日志
- 基于 AOP 的操作日志自动记录
- 操作日志分页查询（`/operateLogs`）
- 员工日志分页查询（`/empLogs`）

### 📤 文件上传
- 文件上传至阿里云 OSS，返回访问 URL

## 快速开始

### 环境要求

- JDK 22+
- Maven 3.6+
- MySQL 8.0+

### 1. 克隆项目

```bash
git clone https://github.com/YukaKaori/newdessertshop.git
cd newdessertshop
```

### 2. 初始化数据库

创建数据库并执行 SQL 脚本：

```sql
CREATE DATABASE dessertshop DEFAULT CHARACTER SET utf8mb4;
```

```bash
# 导入表结构和初始数据
mysql -u root -p dessertshop < sql/orders_and_dessert.sql

# 添加索引（可选，提升查询性能）
mysql -u root -p dessertshop < sql/add_indexes.sql
```

### 3. 修改配置

编辑 `dessertshop-web-management/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dessertshop
    username: root
    password: your_password    # 修改为实际密码

jwt:
  secret: your_secret_key     # 建议修改为自定义密钥
  expiration: 43200000        # Token 有效期（毫秒），默认 12 小时
```

### 4. 构建并运行

```bash
# 构建项目
mvn clean package -DskipTests

# 运行应用
java -jar dessertshop-web-management/target/dessertshop-web-management-1.0-SNAPSHOT.jar
```

应用启动后访问 `http://localhost:8080`。

### 5. 默认登录

```
用户名: admin
密码:   1234
```

## API 概览

| 模块 | 路径 | 说明 |
|------|------|------|
| 登录 | `POST /login` | 用户登录 |
| 员工 | `GET/POST/PUT/DELETE /emps/**` | 员工管理 |
| 部门 | `GET/POST/PUT/DELETE /depts/**` | 部门管理 |
| 甜品 | `GET/POST/PUT/DELETE /desserts/**` | 甜品管理 |
| 订单 | `GET/POST/PUT/DELETE /orders/**` | 订单管理 |
| 看板 | `GET /dashboard/**` | 数据统计 |
| 报表 | `GET /report/**` | 数据报表 |
| 日志 | `GET /operateLogs`, `GET /empLogs` | 操作日志 |
| 上传 | `POST /upload` | 文件上传 |

## 安全机制

- **密码加密**：使用 BCrypt 哈希存储密码
- **JWT 认证**：登录后返回 Token，请求时通过 `token` Header 传递
- **操作审计**：AOP 自动记录关键操作的执行信息
- **输入校验**：Jakarta Validation 校验请求参数

## 许可证

本项目仅供学习交流使用。
