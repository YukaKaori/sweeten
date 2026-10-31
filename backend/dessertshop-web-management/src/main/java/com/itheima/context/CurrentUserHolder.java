package com.itheima.context;

/**
 * 当前登录用户上下文 (ThreadLocal)
 * <p>
 * TokenFilter 解析 JWT 后写入, 请求结束时由 TokenFilter 在 finally 中清理,
 * 防止线程复用导致的身份串扰。
 */
public class CurrentUserHolder {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    /**
     * @return 当前登录用户; 白名单接口或过滤器未启用时为 null
     */
    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
