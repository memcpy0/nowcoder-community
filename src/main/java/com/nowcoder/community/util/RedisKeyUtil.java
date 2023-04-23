package com.nowcoder.community.util;

public class RedisKeyUtil {

    private static final String SPLIT = ":";
    private static final String PREFIX_KAPTCHA = "kaptcha";

    public static String getKaptchaKey(String owner) {
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    private static final String PREFIX_TICKET = "ticket"; // 登录凭证
    // 返回查询登录凭证的键
    public static String getTicketKey(String ticket) {
        return PREFIX_TICKET + SPLIT + ticket;
    }

    private static final String PREFIX_USER = "user"; // 用户信息
    // 返回查询用户的键，用于缓存用户信息
    public static String getUserKey(int userId) {
        return PREFIX_USER + SPLIT + userId;
    }
}
