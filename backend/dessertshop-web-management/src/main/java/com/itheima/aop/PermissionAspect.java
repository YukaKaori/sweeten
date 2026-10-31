package com.itheima.aop;

import com.itheima.annotation.RequirePermission;
import com.itheima.context.CurrentUser;
import com.itheima.context.CurrentUserHolder;
import com.itheima.exception.ForbiddenException;
import com.itheima.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

/**
 * 权限校验切面 — 拦截 @RequirePermission 标注的接口
 * <p>
 * 校验规则:
 * 1. 未登录 (无 CurrentUser 上下文) → 401
 * 2. super_admin 角色 → 直接放行
 * 3. 其余角色 → 任一角色拥有所需权限码即放行, 否则 403
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    public static final String SUPER_ADMIN = "super_admin";

    private final PermissionCache permissionCache;

    @Before("@annotation(requirePermission)")
    public void checkPermission(RequirePermission requirePermission) {
        CurrentUser user = CurrentUserHolder.get();
        if (user == null) {
            throw new UnauthorizedException();
        }

        List<String> roles = user.getRoles();
        if (CollectionUtils.isEmpty(roles)) {
            log.warn("权限校验拒绝: 用户 {} 未绑定任何角色", user.getUsername());
            throw new ForbiddenException("当前账号未分配角色，请联系管理员");
        }

        if (roles.contains(SUPER_ADMIN)) {
            return;
        }

        List<String> required = List.of(requirePermission.value());
        for (String role : roles) {
            Set<String> granted = permissionCache.getPermissions(role);
            if (required.stream().anyMatch(granted::contains)) {
                return;
            }
        }

        log.warn("权限校验拒绝: 用户 {} (角色 {}) 缺少权限 {}", user.getUsername(), roles, required);
        throw new ForbiddenException("无权限执行该操作");
    }
}
