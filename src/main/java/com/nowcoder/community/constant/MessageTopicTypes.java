package com.nowcoder.community.constant;

public class MessageTopicTypes {
    /**
     * 主题：评论
     */
    public static final String TOPIC_COMMENT = "comment";
    /**
     * 主题：点赞
     */
    public static final String TOPIC_LIKE = "like";
    /**
     * 主题：关注
     */
    public static final String TOPIC_FOLLOW = "follow";
    /**
     * 主题：发布帖子事件，用于ES搜索
     */
    public static final String TOPIC_PUBLISH = "publish";
    /**
     * 主题：删除帖子事件，删除时使用
     */
    public static final String TOPIC_DELETE = "delete";
}
