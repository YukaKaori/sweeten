package com.itheima.aop;

import com.itheima.annotation.LogOperation;
import com.itheima.mapper.OperateLogMapper;
import com.itheima.pojo.OperateLog;
import com.itheima.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面
 */
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Autowired
    private JwtUtils jwtUtils;

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
        operateLog.setMethodParams(Arrays.toString(joinPoint.getArgs()));
        operateLog.setReturnValue(result.toString());
        operateLog.setCostTime(costTime);

        // 插入日志
        operateLogMapper.insert(operateLog);

        return result;
    }

    /**
     * 获取当前登录用户ID
     * 从JWT令牌中解析用户ID
     */
    private Integer getCurrentUserId() {
        try {
            // 获取当前请求
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // 获取token
                String token = request.getHeader("token");
                if (token != null) {
                    // 解析JWT
                    Claims claims = jwtUtils.parseJWT(token);
                    // 获取用户ID
                    Object id = claims.get("id");
                    if (id instanceof Integer) {
                        return (Integer) id;
                    } else if (id instanceof Long) {
                        return ((Long) id).intValue();
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败返回默认值
            return 0;
        }
        return 0;
    }
}
