# 运维手册：员工密码迁移为 BCrypt

> 适用场景：从很早期的数据库升级（员工密码仍是明文）。现有库与 `sql/init_all.sql` 建出的新库都已是 BCrypt 哈希，**不需要**执行。
>
> 2026-10-31 修订：原先推荐的"运行测试类 `PasswordMigrationTest`"已删除——那个类会直连开发机的真实数据库并改写密码；
> 迁移只通过下面的 migration profile 执行。

## 原理

`com.sweeten.identity.security.PasswordMigrationRunner` 在应用启动时运行，仅当 `spring.password.migration.enabled=true`
（`application-migration.yml` 已设置）才会生效：

- 已经是 BCrypt 哈希的账号（`$2a$` / `$2b$` / `$2y$` 开头）一律跳过——重复执行是安全的，不会把哈希再哈希一次；
- 空密码的账号只记录告警，不做改动（需要单独为其重置密码）；
- 其余账号的明文密码用 BCrypt（cost 12）哈希后写回，原密码保持可用。

## 步骤

1. **备份数据库**并确认能恢复：

   ```bash
   mysqldump -u root -p dessertshop > dessertshop_backup_$(date +%Y%m%d).sql
   ```

2. （可选）只读检查有多少明文密码：

   ```sql
   SELECT COUNT(*) FROM emp WHERE password NOT LIKE '$2_$%' OR password IS NULL;
   ```

3. 用 migration profile 启动一次（数据库连接按正常方式通过 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 提供）：

   ```bash
   cd backend
   mvn -q package -DskipTests
   java -jar target/sweeten-server.jar --spring.profiles.active=migration
   ```

   看到 `密码迁移完成：已迁移 N 个，已跳过 M 个` 后停止应用。

4. 去掉 migration profile 正常启动，用原密码登录验证（`POST /login`）。

## 回滚

迁移只修改 `emp.password`。如需回滚，用第 1 步的备份恢复该列或整库：

```bash
mysql -u root -p dessertshop < dessertshop_backup_YYYYMMDD.sql
```

## 相关

- 新增员工与修改密码时由应用自动加密（`EmpServiceImpl.save`、`AuthServiceImpl.changePassword`），无需迁移。
- 早期的手工 SQL 方案已归档到 `backend/sql/archive/password_migration.sql`，不要再使用。
- 单元测试：`PasswordMigrationRunnerTest`（纯内存，不连接任何数据库）。
