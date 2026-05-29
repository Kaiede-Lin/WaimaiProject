package com.waimai.common.utils;

public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> OPENID = new ThreadLocal<>();

    public static void set(Long userId, String openid) {
        USER_ID.set(userId);
        OPENID.set(openid);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getOpenid() {
        return OPENID.get();
    }

    public static void clear() {
        USER_ID.remove();
        OPENID.remove();
    }
}
