package com.nowcoder.community.util;

public class RedisKeyUtil {
    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LINE = "like:entity";
    // 某个实体的赞
    // like:entity:entityType:entityId -> set(userId)
    public static String getEntityLikeKey(int entityType, int entityId) {
        return PREFIX_ENTITY_LINE + SPLIT + entityType + SPLIT + entityId;
    }

    private static final String PREFIX_USER_LIKE = "like:user";
    // 某个用户的赞
    // like:user:userId -> int
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

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

    private static final String PREFIX_FOLLOWEE = "followee";
    private static final String PREFIX_FOLLOWER = "follower";

    // 某个用户关注的实体(用户关注的用户/帖子/评论)
    // followee:userId:entityType -> zset(entityId,now) // now为当前时间,用它来排序
    public static String getFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    // 某个实体(帖子/评论/用户)的粉丝
    // follower:entityType:entityId -> zset(userId,now)
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }
}
