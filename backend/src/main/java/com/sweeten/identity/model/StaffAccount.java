package com.sweeten.identity.model;

import lombok.Data;
import lombok.ToString;

/**
 * 员工登录账号（emp 表中与身份认证相关的列）。
 * <p>
 * 员工档案（姓名、部门、薪资、工作经历……）属于 staff 模块；账号凭证（用户名、密码哈希）属于 identity 模块。
 * 两者目前共用 emp 表，但各自只读写自己负责的列。密码哈希只在服务端校验，不进日志、不进任何响应。
 */
@Data
public class StaffAccount {
    private Integer id;
    private String username;
    @ToString.Exclude
    private String password;
    private String name;
    private String image;
}
