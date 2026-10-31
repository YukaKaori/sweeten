package com.itheima.exception;

/**
 * 业务异常
 * 用于处理业务逻辑中的异常情况
 */
public class BusinessException extends RuntimeException {

    private Integer code;
    private Integer httpStatus;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
        this.httpStatus = 400;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = 400;
    }

    public BusinessException(Integer code, Integer httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public Integer getCode() {
        return code;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}
