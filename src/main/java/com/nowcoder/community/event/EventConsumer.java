package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.constant.MessageTopicTypes;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);
    private static final int SYSTEM_USER_ID = 1;
    @Autowired
    private MessageService messageService;

    /**
     * 消费点赞、关注、评论事件
     * @param record
     */
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

    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private ElasticsearchService elasticsearchService;

    /**
     * 消费发帖事件
     * @param record
     */
    @KafkaListener(topics = {MessageTopicTypes.TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误！");
            return;
        }
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post); // 将相关帖子（刚发布或修改过）存储在ES中
    }

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    /**
     * 消费分享事件(生成长图并定时上传图片)
     */
    @KafkaListener(topics = MessageTopicTypes.TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }
        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        String cmd = wkImageCommand + " --quality 95 --width 1300 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd); // 启动其他程序生成长图
            logger.info("生成长图成功: " + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败: " + e.getMessage());
        }

        // 启用定时器,监视该图片,一旦生成了,则上传至七牛云.
        UploadTask task = new UploadTask(fileName, suffix); // 定时任务
        Future future = taskScheduler.scheduleAtFixedRate(task, 500); // future中可查看任务状态，可以在任务完成后停止定时器
        task.setFuture(future); // 考虑可用性，命令执行失败、图片没生成，或图片上传失败，则任务永远不会停止；一旦出现这种情况，必须停掉任务
    }
    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    class UploadTask implements Runnable {
        // 文件名称
        private String fileName;
        // 文件后缀
        private String suffix;
        // 启动任务的返回值
        private Future future;
        // 开始时间
        private long startTime;
        // 上传次数
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis(); // 初始化开始时间
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            if (System.currentTimeMillis() - startTime > 30000) { // 生成失败
                logger.error("执行时间过长,终止任务:" + fileName);
                future.cancel(true);
                return;
            }
            if (uploadTimes >= 3) { // 上传失败
                logger.error("上传次数过多,终止任务:" + fileName);
                future.cancel(true);
                return;
            }
            String path = wkImageStorage + "/" + fileName + suffix; // 本地图片路径
            File file = new File(path);
            if (file.exists()) { // 看图片是否存在
                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));
                // 设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));
                // 生成上传凭证
                Auth auth = Auth.create(accessKey, secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                // 指定上传机房
                UploadManager manager = new UploadManager(new Configuration(Zone.zone1()));
                try {
                    // 开始上传图片
                    Response response = manager.put(
                            path, fileName, uploadToken, null, "image/" + suffix, false);
                    // 处理响应结果
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                    } else {
                        logger.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
                        future.cancel(true);
                    }
                } catch (QiniuException e) {
                    logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                }
            } else {
                logger.info("等待图片生成[" + fileName + "].");
            }
        }
    }
}
