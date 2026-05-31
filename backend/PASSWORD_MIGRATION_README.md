# 密码迁移快速指南

## 概述

本指南帮助你将数据库中的明文密码迁移为BCrypt加密格式。

## 前置条件

1. 已备份数据库
2. 已编译项目（`mvn clean compile`）

## 快速开始

### 步骤1：备份数据库

```bash
mysqldump -u root -p dessertshop > dessertshop_backup_$(date +%Y%m%d).sql
```

### 步骤2：检查当前密码状态

```bash
cd dessertshop-web-management
mvn test -Dtest=PasswordMigrationTest#checkPasswordStatus
```

输出示例：
```
===========================================
密码状态检查
===========================================
用户: admin, 状态: 未加密, 密码前20位: 123456
用户: zhangsan, 状态: 未加密, 密码前20位: 123456
===========================================
```

### 步骤3：执行密码迁移

选择以下任一方式：

#### 方式A：运行测试类（推荐）

```bash
mvn test -Dtest=PasswordMigrationTest#migratePasswords
```

#### 方式B：使用应用启动迁移

1. 编辑 `application.yml`：
```yaml
spring:
  password:
    migration:
      enabled: true
```

2. 启动应用：
```bash
mvn spring-boot:run
```

3. 看到迁移完成日志后，停止应用

4. 将配置改回 `false`：
```yaml
spring:
  password:
    migration:
      enabled: false
```

### 步骤4：验证迁移结果

```bash
mvn test -Dtest=PasswordMigrationTest#checkPasswordStatus
```

输出示例：
```
===========================================
密码状态检查
===========================================
用户: admin, 状态: 已加密, 密码前20位: $2a$12$N.zmdr9k7uO...
用户: zhangsan, 状态: 已加密, 密码前20位: $2a$12$N.zmdr9k7uO...
===========================================
```

### 步骤5：测试登录功能

```bash
# 启动应用
mvn spring-boot:run

# 测试登录
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

预期响应：
```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "name": "管理员",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

## 常见问题

### Q1: 迁移后登录失败怎么办？

A: 检查以下几点：
1. 确认密码迁移已完成
2. 确认使用的密码是迁移前的明文密码
3. 检查日志中是否有错误信息

### Q2: 如何回滚迁移？

A: 如果备份了数据库，可以使用以下命令回滚：
```bash
mysql -u root -p dessertshop < dessertshop_backup_20240101.sql
```

### Q3: 新增用户时密码会自动加密吗？

A: 是的，修改后的代码会在新增用户时自动加密密码。

### Q4: 修改密码时会自动加密吗？

A: 是的，修改密码时会自动加密新密码。

## 文件说明

| 文件 | 说明 |
|------|------|
| `PasswordMigrationTest.java` | 测试类，用于手动执行迁移 |
| `PasswordMigrationRunner.java` | 启动时自动迁移（需配置启用） |
| `application-migration.yml` | 迁移专用配置文件 |
| `password_migration.sql` | SQL迁移脚本 |

## 注意事项

1. **务必备份数据库**后再执行迁移
2. 迁移完成后，**禁用自动迁移**配置
3. 生产环境建议使用**方式A**（测试类）进行迁移
4. 如果用户有不同的密码，需要分别生成BCrypt哈希值

## 技术支持

如遇问题，请检查：
1. 数据库连接配置是否正确
2. 项目是否编译成功
3. 查看应用日志获取详细错误信息
