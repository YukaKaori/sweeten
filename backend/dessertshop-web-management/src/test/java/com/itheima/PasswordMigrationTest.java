package com.itheima;

import com.itheima.mapper.EmpMapper;
import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import com.itheima.utils.BCryptUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 密码迁移测试类
 * 用于手动执行密码迁移
 */
@Slf4j
@SpringBootTest
public class PasswordMigrationTest {

    @Autowired
    private EmpMapper empMapper;

    /**
     * 执行密码迁移
     */
    @Test
    public void migratePasswords() {
        log.info("开始执行密码迁移...");

        // 查询所有员工
        List<Emp> empList = empMapper.list(new EmpQueryParam());
        log.info("共找到 {} 条员工记录", empList.size());

        int migratedCount = 0;
        int skippedCount = 0;

        for (Emp emp : empList) {
            // 检查是否已经是BCrypt格式
            if (emp.getPassword() != null && emp.getPassword().startsWith("$2a$")) {
                log.debug("跳过已加密的用户: {}", emp.getUsername());
                skippedCount++;
                continue;
            }

            // 加密密码
            String originalPassword = emp.getPassword();
            String hashedPassword = BCryptUtils.hashPassword(originalPassword);

            // 更新数据库
            empMapper.updatePassword(emp.getId(), hashedPassword);

            log.info("迁移成功 - ID: {}, 用户名: {}",
                    emp.getId(), emp.getUsername());
            migratedCount++;
        }

        log.info("===========================================");
        log.info("密码迁移完成！");
        log.info("已迁移: {} 条", migratedCount);
        log.info("已跳过: {} 条", skippedCount);
        log.info("===========================================");
    }

    /**
     * 测试BCrypt加密
     */
    @Test
    public void testBCrypt() {
        String rawPassword = "123456";

        // 加密
        String hashedPassword = BCryptUtils.hashPassword(rawPassword);
        log.info("原始密码: {}", rawPassword);
        log.info("加密后: {}", hashedPassword);

        // 验证
        boolean isValid = BCryptUtils.verifyPassword(rawPassword, hashedPassword);
        log.info("验证结果: {}", isValid);

        // 验证错误密码
        boolean isInvalid = BCryptUtils.verifyPassword("654321", hashedPassword);
        log.info("错误密码验证结果: {}", isInvalid);
    }

    /**
     * 查看当前密码状态
     */
    @Test
    public void checkPasswordStatus() {
        List<Emp> empList = empMapper.list(new EmpQueryParam());

        log.info("===========================================");
        log.info("密码状态检查");
        log.info("===========================================");

        for (Emp emp : empList) {
            String status = (emp.getPassword() != null && emp.getPassword().startsWith("$2a$"))
                    ? "已加密" : "未加密";
            log.info("用户: {}, 状态: {}, 密码前20位: {}",
                    emp.getUsername(), status,
                    emp.getPassword() != null ? emp.getPassword().substring(0, Math.min(20, emp.getPassword().length())) + "..." : "null");
        }

        log.info("===========================================");
    }
}
