package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.constant.MessageTopicTypes;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.MessageService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);
    private static final int SYSTEM_USER_ID = 1;
    @Autowired
    private MessageService messageService;

    @KafkaListener(topics = {MessageTopicTypes.TOPIC_COMMENT, MessageTopicTypes.TOPIC_LIKE, MessageTopicTypes.TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        // 私信是用户发给另一个用户,conversation_id是userId1_userId2
        if (event == null) { // 而通知是系统发给用户的,conversation_id改为主题类型的字符串
            logger.error("消息格式错误！");
            return;
        }
        // 发送站内通知(存入数据库中)
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic()); // 存的实际上是主题
        message.setCreateTime(new Date());
        // 页面上要拼接通知的数据(谁触发的事件+评论了+实体作者+帖子/评论，[点击查看](帖子链接)！
        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        if (!event.getData().isEmpty()) { // 其他数据
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }
}
