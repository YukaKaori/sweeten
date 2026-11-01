package com.sweeten.identity.security;

import com.sweeten.identity.mapper.AccountMapper;
import com.sweeten.identity.model.StaffAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 密码格式迁移：启动时把仍为明文的员工密码转换为 BCrypt 哈希。
 * <p>
 * 默认关闭；只通过 migration profile 开启：{@code java -jar sweeten-server.jar --spring.profiles.active=migration}
 * （步骤见 docs/runbooks/PASSWORD_MIGRATION.md）。已经是 BCrypt 哈希（$2a$ / $2b$ / $2y$）的账号一律跳过，
 * 重复执行是安全的；空密码的账号只记录告警、不做改动。
 */
@Slf4j
@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private final AccountMapper accountMapper;
    private final boolean migrationEnabled;

    public PasswordMigrationRunner(AccountMapper accountMapper,
                                   @Value("${spring.password.migration.enabled:false}") boolean migrationEnabled) {
        this.accountMapper = accountMapper;
        this.migrationEnabled = migrationEnabled;
    }

    /** BCrypt 的各个版本前缀都视为已加密，避免把哈希当作明文再哈希一次（会导致该账号无法登录） */
    static boolean isBcryptHash(String password) {
        return password != null && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    @Override
    public void run(String... args) {
        if (!migrationEnabled) {
            log.info("密码迁移已禁用，跳过执行");
            return;
        }

        log.info("开始执行密码迁移...");
        List<StaffAccount> accounts = accountMapper.listAll();
        log.info("共找到 {} 个员工账号", accounts.size());

        int migrated = 0;
        int skipped = 0;
        for (StaffAccount account : accounts) {
            String password = account.getPassword();
            if (isBcryptHash(password)) {
                log.debug("跳过已加密的账号: {}", account.getUsername());
                skipped++;
                continue;
            }
            if (password == null || password.isEmpty()) {
                log.warn("账号 {} 没有密码，跳过（请为其重置密码）", account.getUsername());
                skipped++;
                continue;
            }
            accountMapper.updatePassword(account.getId(), BCryptUtils.hashPassword(password));
            log.info("迁移成功 - 账号: {}", account.getUsername());
            migrated++;
        }

        log.info("密码迁移完成：已迁移 {} 个，已跳过 {} 个。请关闭 migration profile 后正常启动。", migrated, skipped);
    }
}
