package com.itheima.exception;

/**
 * 401 未认证异常
 * 当用户未登录或 token 无效时抛出
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException() {
        super("请先登录");
    }
}
