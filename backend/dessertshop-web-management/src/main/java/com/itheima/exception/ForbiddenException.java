package com.itheima.exception;

/**
 * 403 权限不足异常
 * 当用户没有权限执行操作时抛出
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String resource, String action) {
        super(String.format("无权限执行操作: [%s] %s", action, resource));
    }
}
