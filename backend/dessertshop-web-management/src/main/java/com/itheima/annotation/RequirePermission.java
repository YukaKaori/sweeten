package com.itheima.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限校验注解
 * <p>
 * 标注在 Controller 方法上, 由 PermissionAspect 切面执行校验:
 * 当前用户 (CurrentUserHolder) 的角色须拥有指定权限码, 否则返回 403。
 * super_admin 角色直接放行。未标注该注解的接口默认仅要求登录 (TokenFilter 保底)。
 * <p>
 * 权限码格式: 模块:资源:操作, 如 dessert:dessert:update、emp:emp:delete
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 所需权限码
     */
    String value();
}
