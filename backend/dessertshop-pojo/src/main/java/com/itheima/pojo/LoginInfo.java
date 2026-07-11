package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginInfo {
    private Integer id;
    private String username;
    private String name;
    private String token;
    private String image;
    private List<String> roles;       //角色码列表 (同时写入 JWT claims)
    private List<String> permissions; //权限码列表 (仅登录响应下发, 不进 token, 避免 token 膨胀)
}
