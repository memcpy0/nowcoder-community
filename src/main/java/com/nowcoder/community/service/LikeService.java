package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    // 点赞
    // 不仅记录userId的点赞，还记录某人收到的赞
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId); // 被赞的那个人的ID即entityUserId; 查询数据库的话慢
                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId); // 提前查询
                operations.multi(); // 开启事务
                if (isMember) { // 点过赞
                    operations.opsForSet().remove(entityLikeKey, userId); // 取消赞
                    operations.opsForValue().decrement(userLikeKey); // 获得的赞-1
                } else { // 没点过
                    operations.opsForSet().add(entityLikeKey, userId); // 点赞
                    operations.opsForValue().increment(userLikeKey); // 某个用户获得的赞+1
                }
                return operations.exec(); // 提交事务
            }
        });
    }

    // 查询某实体（某个帖子或评论）点赞的数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    // 查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    // 查询某个用户获得的赞
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }
}

