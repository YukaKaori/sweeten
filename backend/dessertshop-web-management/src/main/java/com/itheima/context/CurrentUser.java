package com.itheima.context;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息 — 由 TokenFilter 从 JWT 解析后写入 CurrentUserHolder
 */
@Data
@AllArgsConstructor
public class CurrentUser {
    private Integer id;
    private String username;
    private List<String> roles;

    public boolean hasRole(String roleCode) {
        return roles != null && roles.contains(roleCode);
    }
}
