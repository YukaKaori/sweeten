package com.itheima.exception;

/**
 * 404 资源未找到异常
 * 当请求的资源不存在时抛出
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String resource, Object id) {
        super(String.format("%s 不存在, id: %s", resource, id));
    }

    public NotFoundException(String resource, String field, Object value) {
        super(String.format("%s 不存在, %s: %s", resource, field, value));
    }
}
