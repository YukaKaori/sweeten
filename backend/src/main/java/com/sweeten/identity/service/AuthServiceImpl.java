package com.sweeten.identity.service;

import com.sweeten.identity.mapper.AccountMapper;
import com.sweeten.identity.mapper.SysRoleMapper;
import com.sweeten.identity.model.LoginInfo;
import com.sweeten.identity.model.StaffAccount;
import com.sweeten.identity.security.BCryptUtils;
import com.sweeten.identity.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AccountMapper accountMapper;
    private final SysRoleMapper sysRoleMapper;
    private final JwtUtils jwtUtils;

    @Override
    public LoginInfo login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        StaffAccount account = accountMapper.findByUsername(username);
        if (account == null || !BCryptUtils.verifyPassword(password, account.getPassword())) {
            return null;
        }
        //1. 角色进 token；权限码仅随登录响应下发，避免 token 膨胀
        List<String> roles = sysRoleMapper.selectRoleCodesByUserId(account.getId());
        List<String> permissions = roles.stream()
                .flatMap(role -> sysRoleMapper.selectPermissionCodesByRoleCode(role).stream())
                .distinct()
                .toList();

        //2. 签发 JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", account.getId());
        claims.put("username", account.getUsername());
        claims.put("roles", roles);
        String jwt = jwtUtils.generateJwt(claims);
        return new LoginInfo(account.getId(), account.getUsername(), account.getName(), jwt,
                account.getImage(), roles, permissions);
    }

    @Override
    public boolean changePassword(Integer userId, String oldPassword, String newPassword) {
        if (userId == null || oldPassword == null || newPassword == null) {
            return false;
        }
        StaffAccount account = accountMapper.findById(userId);
        if (account == null || !BCryptUtils.verifyPassword(oldPassword, account.getPassword())) {
            return false;
        }
        return accountMapper.updatePassword(userId, BCryptUtils.hashPassword(newPassword)) > 0;
    }
}
