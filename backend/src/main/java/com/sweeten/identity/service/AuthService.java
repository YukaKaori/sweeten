package com.sweeten.identity.service;

import com.sweeten.identity.model.LoginInfo;

/**
 * 员工身份认证：登录签发令牌、修改本人密码。
 * <p>
 * 顾客（商城）目前没有登录体系，按订单号 + 手机号识别；接入短信 / 微信登录时应作为独立的顾客身份能力加入本模块，
 * 而不是复用员工账号。
 */
public interface AuthService {

    /**
     * 用户名密码登录
     *
     * @return 登录信息（含 JWT、角色、权限码）；用户名不存在或密码错误时返回 null
     */
    LoginInfo login(String username, String password);

    /**
     * 修改密码（调用方必须传入当前登录用户自己的 ID，不得信任请求参数）
     *
     * @return 原密码错误或账号不存在时返回 false
     */
    boolean changePassword(Integer userId, String oldPassword, String newPassword);
}
