package com.itheima.runner;

import com.itheima.mapper.EmpMapper;
import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import com.itheima.utils.BCryptUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 密码迁移启动器
 * 应用启动时自动将明文密码转换为BCrypt加密格式
 *
 * 使用方法：
 * 1. 首次运行：设置 spring.password.migration.enabled=true
 * 2. 迁移完成后：设置 spring.password.migration.enabled=false 或删除此配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordMigrationRunner implements CommandLineRunner {

    private final EmpMapper empMapper;

    @Value("${spring.password.migration.enabled:false}")
    private boolean migrationEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (!migrationEnabled) {
            log.info("密码迁移已禁用，跳过执行");
            return;
        }

        log.info("===========================================");
        log.info("开始执行密码迁移...");
        log.info("===========================================");

        try {
            // 查询所有员工
            List<Emp> empList = empMapper.list(new EmpQueryParam());
            log.info("共找到 {} 条员工记录", empList.size());

            int migratedCount = 0;
            int skippedCount = 0;

            for (Emp emp : empList) {
                // 检查是否已经是BCrypt格式（BCrypt哈希以$2a$开头）
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

                log.info("迁移成功 - 用户: {}", emp.getUsername());
                migratedCount++;
            }

            log.info("===========================================");
            log.info("密码迁移完成！");
            log.info("已迁移: {} 条", migratedCount);
            log.info("已跳过: {} 条", skippedCount);
            log.info("===========================================");
            log.info("请设置 spring.password.migration.enabled=false 以禁用迁移");
            log.info("===========================================");

        } catch (Exception e) {
            log.error("密码迁移失败", e);
            throw e;
        }
    }
}
