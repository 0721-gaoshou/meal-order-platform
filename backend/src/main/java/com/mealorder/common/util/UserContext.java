package com.mealorder.common.util;

/**
 * 当前登录用户上下文：AuthInterceptor 写入，请求结束清理，业务层随时读取
 */
public final class UserContext {

    public record CurrentUser(Long userId, String username, String role) {
        public boolean isAdmin() {
            return "ADMIN".equals(role);
        }
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
