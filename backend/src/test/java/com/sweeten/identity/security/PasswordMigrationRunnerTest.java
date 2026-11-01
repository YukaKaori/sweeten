package com.sweeten.identity.security;

import com.sweeten.identity.mapper.AccountMapper;
import com.sweeten.identity.model.StaffAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 密码格式迁移（纯单元测试，替代原先直连真实数据库执行迁移的 PasswordMigrationTest）
 */
@DisplayName("密码格式迁移")
class PasswordMigrationRunnerTest {

    private static StaffAccount account(int id, String username, String password) {
        StaffAccount a = new StaffAccount();
        a.setId(id);
        a.setUsername(username);
        a.setPassword(password);
        return a;
    }

    @Test
    @DisplayName("默认关闭：不读取也不修改任何账号")
    void disabledByDefault() {
        AccountMapper mapper = mock(AccountMapper.class);
        new PasswordMigrationRunner(mapper, false).run();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("只迁移明文密码；任何版本的 BCrypt 哈希（$2a$/$2b$/$2y$）与空密码都不改动")
    void migratesOnlyPlaintext() {
        AccountMapper mapper = mock(AccountMapper.class);
        when(mapper.listAll()).thenReturn(List.of(
                account(1, "plain", "Aaa111"),
                account(2, "hashed2a", "$2a$12$EomIDadCnBiJUlmkayLOxOwLOapjnNGEwowEoq7aojNH65qzjYskC"),
                account(3, "hashed2b", "$2b$12$EomIDadCnBiJUlmkayLOxOwLOapjnNGEwowEoq7aojNH65qzjYskC"),
                account(4, "hashed2y", "$2y$12$EomIDadCnBiJUlmkayLOxOwLOapjnNGEwowEoq7aojNH65qzjYskC"),
                account(5, "empty", null)));

        new PasswordMigrationRunner(mapper, true).run();

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(mapper, times(1)).updatePassword(eq(1), hash.capture());
        verify(mapper, times(1)).updatePassword(anyInt(), anyString());
        assertTrue(BCryptUtils.verifyPassword("Aaa111", hash.getValue()));
    }
}
