package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {
    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;
    // LoadingCache同步缓存：多线程访问同个数据时，会从数据库中取数据，再让多线程访问
    // Caffeine核心接口: Cache, LoadingCache, AsyncLoadingCache
    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache; // 按key缓存value，由于缓存帖子列表的一页，因此key=offset+limit

    // 帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    // DiscussPostService初始化后，初始化缓存
    @PostConstruct
    public void init() {
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception { // 初始化加载一级缓存中的数据
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误!");
                        }
                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);
                        // 可以再加二级缓存: Redis -> mysql
                        logger.debug("load post list from DB.");
                        // 最后访问数据库
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }
    /**
     * 找到某个用户发的所有讨论贴,ID为0就找到所有人的讨论帖
     * @param userId 用户ID
     * @param offset 第几页
     * @param limit 每页多少帖子
     * @param orderMode 最新0还是最热1排序
     * @return
     */
//    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
//        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
//    }
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        if (userId == 0 && orderMode == 1) { // 按热度查询系统中所有帖子
            return postListCache.get(offset + ":" + limit); // 从缓存中查找
        }
        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    /**
     * 求某个用户的发帖记录数
     * @param userId
     * @return
     */
//    public int findDiscussPostRows(int userId) {
//        return discussPostMapper.selectDiscussPostRows(userId);
//    }
    public int findDiscussPostRows(int userId) {
        if (userId == 0) { // 查询所有帖子时,从缓存中查找帖子总数
            return postRowsCache.get(userId);
        }
        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    @Autowired
    private SensitiveFilter sensitiveFilter;
    public int addDiscussPost(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 转义HTML标记
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    /**
     * 帖子详情
     * @param id
     * @return
     */
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    /**
     * 更新帖子的评论数量
     * @param id
     * @param commentCount
     * @return
     */
    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    /**
     * 设置是否置顶 0是正常,1是置顶
     * @param id
     * @param type
     * @return
     */
    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    /**
     * 设置是否精华、拉黑
     * 0-正常,1-精华,2-拉黑
     * @param id
     * @param status
     * @return
     */
    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
