package com.sweeten.audit.aspect;

import com.sweeten.audit.mapper.OperateLogMapper;
import com.sweeten.audit.model.OperateLog;
import com.sweeten.identity.security.CurrentUser;
import com.sweeten.identity.security.CurrentUserHolder;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperateLogMapper operateLogMapper;

    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint joinPoint, LogOperation log) throws Throwable {
        // 开始时间
        long startTime = System.currentTimeMillis();

        // 执行方法
        Object result = joinPoint.proceed();

        // 结束时间
        Long endTime = System.currentTimeMillis();
        // 耗时
        Long costTime = endTime - startTime;

        // 构建日志对象
        OperateLog operateLog = new OperateLog();
        operateLog.setOperateEmpId(getCurrentUserId());
        operateLog.setOperateTime(LocalDateTime.now());
        operateLog.setClassName(joinPoint.getTarget().getClass().getName());
        operateLog.setMethodName(joinPoint.getSignature().getName());
        operateLog.setMethodParams(maskSensitive(Arrays.toString(joinPoint.getArgs())));
        operateLog.setReturnValue(result.toString());
        operateLog.setCostTime(costTime);

        // 插入日志
        operateLogMapper.insert(operateLog);

        return result;
    }

    /**
     * 脱敏方法参数中的密码字段
     * 匹配 Lombok toString 输出中的 password/oldPassword/newPassword 等字段值
     */
    private static String maskSensitive(String params) {
        if (params == null) {
            return null;
        }
        return params.replaceAll("(?i)(\\w*password\\w*=)[^,)\\]]*", "$1***");
    }

    /**
     * 当前操作人 ID：取 TokenFilter 已校验并写入的登录上下文（不再重复解析 JWT）；匿名请求记为 0
     */
    private static Integer getCurrentUserId() {
        CurrentUser user = CurrentUserHolder.get();
        return user != null && user.getId() != null ? user.getId() : 0;
    }
}
