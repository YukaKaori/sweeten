package com.itheima.exception;

/**
 * 409 资源冲突异常
 * 当请求的资源已存在，无法创建时抛出（如重复用户名、重复手机号等）
 */
public class DuplicateException extends RuntimeException {

    public DuplicateException(String message) {
        super(message);
    }

    public DuplicateException(String resource, String field, Object value) {
        super(String.format("%s 已存在, %s: %s", resource, field, value));
    }
}
