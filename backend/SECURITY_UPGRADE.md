# 安全性提升说明

## 提升内容

### 1. 密码加密 (BCrypt)

**改动说明：**
- 使用BCrypt算法对密码进行加密存储
- 登录时使用BCrypt验证密码
- 修改密码时使用BCrypt加密新密码

**相关文件：**
- `dessertshop-utils/src/main/java/com/itheima/utils/BCryptUtils.java` - BCrypt工具类
- `dessertshop-web-management/src/main/java/com/itheima/service/impl/EmpServiceImpl.java` - 业务逻辑

**使用方法：**
```java
// 加密密码
String hashedPassword = BCryptUtils.hashPassword("123456");

// 验证密码
boolean isValid = BCryptUtils.verifyPassword("123456", hashedPassword);
```

### 2. JWT密钥外部化

**改动说明：**
- JWT密钥从硬编码改为配置文件
- 支持通过环境变量覆盖配置
- JWT工具类改为Spring Bean

**相关文件：**
- `dessertshop-utils/src/main/java/com/itheima/utils/JwtUtils.java` - JWT工具类
- `dessertshop-web-management/src/main/resources/application.yml` - 配置文件

**配置说明：**
```yaml
jwt:
  # 签名密钥（生产环境请使用更复杂的密钥）
  secret: DessertShop@2024#SecureKey!@#$%^&*
  # Token过期时间（毫秒），默认12小时
  expire: 43200000
```

### 3. 完善认证上下文

**改动说明：**
- AOP日志切面从JWT中解析真实用户ID
- 不再硬编码返回用户ID

**相关文件：**
- `dessertshop-web-management/src/main/java/com/itheima/aop/OperationLogAspect.java` - 操作日志切面

### 4. 清理冗余代码

**改动说明：**
- 删除被注释的TokenInterceptor
- 清理WebConfig中注释掉的代码
- 使用FilterRegistrationBean注册Filter
- 移除@ServletComponentScan注解

**相关文件：**
- `dessertshop-web-management/src/main/java/com/itheima/filter/TokenFilter.java` - Token过滤器
- `dessertshop-web-management/src/main/java/com/itheima/config/FilterConfig.java` - Filter配置
- `dessertshop-web-management/src/main/java/com/itheima/interceptor/WebConfig.java` - Web配置

## 数据库迁移

### 密码迁移

如果数据库中已有明文密码，需要进行迁移。提供三种方式：

#### 方式一：使用测试类迁移（推荐开发环境）

```bash
# 1. 进入项目目录
cd dessertshop-web-management

# 2. 运行测试类
mvn test -Dtest=PasswordMigrationTest#migratePasswords
```

或者在IDE中直接运行 `PasswordMigrationTest.migratePasswords()` 方法。

#### 方式二：使用应用启动时自动迁移

1. 修改 `application.yml` 配置：
```yaml
spring:
  password:
    migration:
      enabled: true
```

2. 启动应用，迁移会自动执行

3. 迁移完成后，将配置改回 `false`

#### 方式三：使用专用迁移Profile

```bash
# 使用迁移专用配置启动应用
java -jar dessertshop-web-management.jar --spring.profiles.active=migration
```

#### 方式四：使用SQL脚本迁移

1. **备份数据库**
```bash
mysqldump -u root -p dessertshop > dessertshop_backup.sql
```

2. **执行迁移脚本**
```bash
mysql -u root -p dessertshop < sql/password_migration.sql
```

### 迁移验证

运行以下测试验证迁移结果：

```bash
# 检查密码状态
mvn test -Dtest=PasswordMigrationTest#checkPasswordStatus

# 测试BCrypt加密功能
mvn test -Dtest=PasswordMigrationTest#testBCrypt
```

## 测试验证

### 1. 测试登录功能

```bash
# 登录请求
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

### 2. 测试密码修改

```bash
# 修改密码请求
curl -X PUT "http://localhost:8080/emps/password?id=1&oldPassword=123456&newPassword=654321" \
  -H "token: <your-jwt-token>"
```

### 3. 测试Token认证

```bash
# 带Token的请求
curl -X GET http://localhost:8080/emps \
  -H "token: <your-jwt-token>"
```

## 注意事项

1. **生产环境配置**
   - 请使用更复杂的JWT密钥
   - 建议使用环境变量配置敏感信息
   - 定期更换JWT密钥

2. **密码策略**
   - 建议设置密码最小长度
   - 建议包含大小写字母、数字和特殊字符
   - 定期要求用户更换密码

3. **安全建议**
   - 启用HTTPS
   - 配置CORS策略
   - 添加请求频率限制
   - 记录登录日志

## 依赖说明

新增依赖：
```xml
<!-- BCrypt密码加密依赖-->
<dependency>
    <groupId>at.favre.lib</groupId>
    <artifactId>bcrypt</artifactId>
    <version>0.10.2</version>
</dependency>
```
