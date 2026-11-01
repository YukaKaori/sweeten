package com.sweeten.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("测试安全闸")
class TestDatabaseGuardTest {

    @Test
    @DisplayName("拒绝开发机真实 MySQL（application.yml 的默认数据源）与任何非 H2 内存库")
    void rejectsRealDatabases() {
        assertThrows(IllegalStateException.class,
                () -> TestDatabaseGuard.check("jdbc:mysql://localhost:3306/dessertshop"));
        assertThrows(IllegalStateException.class, () -> TestDatabaseGuard.check("jdbc:h2:file:./data/db"));
        assertThrows(IllegalStateException.class, () -> TestDatabaseGuard.check(null));
    }

    @Test
    @DisplayName("允许 H2 内存库")
    void allowsInMemoryH2() {
        assertDoesNotThrow(() -> TestDatabaseGuard.check("jdbc:h2:mem:testdb;MODE=MySQL"));
    }
}
