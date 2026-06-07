package com.itheima.pojo;

import lombok.Data;

/**
 * 修改密码请求体 — 密码通过请求体传输，避免出现在 URL 查询参数中
 */
@Data
public class PasswordChangeRequest {
    private Integer id;
    private String oldPassword;
    private String newPassword;
}
