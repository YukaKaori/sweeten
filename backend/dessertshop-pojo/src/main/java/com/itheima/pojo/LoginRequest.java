package com.itheima.pojo;

import lombok.Data;

/**
 * 登录请求 DTO — 避免复用 Emp 实体，确保只暴露登录所需字段
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}
