package com.itheima.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * BCrypt密码加密工具类
 */
public class BCryptUtils {

    /**
     * 加密密码
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String hashPassword(String rawPassword) {
        return BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray());
    }

    /**
     * 验证密码
     * @param rawPassword 原始密码
     * @param hashedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        BCrypt.Result result = BCrypt.verifyer()
                .verify(rawPassword.toCharArray(), hashedPassword);
        return result.verified;
    }
}
