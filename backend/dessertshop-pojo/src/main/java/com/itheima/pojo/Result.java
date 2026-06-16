package com.itheima.pojo;

import lombok.Data;

/**
 * 后端统一返回结果
 * <p>
 * 约定：code=1 成功，code=0 失败（兼容前端已有逻辑）
 */
@Data
public class Result {

    private Integer code; //编码：1成功，0为失败
    private String msg;  //提示信息
    private Object data; //数据
    private Long timestamp; //响应时间戳

    // ==================== 成功响应 ====================

    public static Result success() {
        Result result = new Result();
        result.code = 1;
        result.msg = "success";
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    public static Result success(Object object) {
        Result result = new Result();
        result.data = object;
        result.code = 1;
        result.msg = "success";
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    // ==================== 失败响应 ====================

    public static Result error(String msg) {
        Result result = new Result();
        result.msg = msg;
        result.code = 0;
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    public static Result error(int code, String msg) {
        Result result = new Result();
        result.code = code;
        result.msg = msg;
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    // ==================== HTTP 风格便捷方法 ====================

    public static Result unauthorized(String msg) {
        return error(401, msg != null ? msg : "请先登录");
    }

    public static Result forbidden(String msg) {
        return error(403, msg != null ? msg : "无权限访问");
    }

    public static Result notFound(String msg) {
        return error(404, msg != null ? msg : "资源不存在");
    }

    public static Result conflict(String msg) {
        return error(409, msg != null ? msg : "资源冲突");
    }

    public static Result serverError(String msg) {
        return error(500, msg != null ? msg : "系统繁忙，请稍后重试");
    }

}