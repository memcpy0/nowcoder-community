package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    private String topic; // 事件主题
    private int userId; // 事件作者
    private int entityType; // 事件发生在哪种类型的哪个实体上
    private int entityId;
    private int entityUserId; // 实体作者(帖子作者，评论作者）,将被通知的对象
    private Map<String, Object> data = new HashMap<>(); // 事件对象要具有通用性和扩展,可能以后要处理其他事件的特殊字段
    public Event setTopic(String topic) {
        this.topic = topic;
        return this; // 链式调用
    }
    public Event setUserId(int userId) {
        this.userId = userId;
        return this; // 链式调用
    }
    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this; // 链式调用
    }
    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this; // 链式调用
    }
    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this; // 链式调用
    }

    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this; // 链式调用
    }
}