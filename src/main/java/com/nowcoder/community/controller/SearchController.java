package com.nowcoder.community.controller;

import com.nowcoder.community.constant.EntityTypes;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController {
    @Autowired
    private ElasticsearchService elasticsearchService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;

    /**
     * 搜索关键词，无需登录
     * @param keyword
     * @param page
     * @param model
     * @return
     */
    @RequestMapping(path = "/search", method = RequestMethod.GET) // GET请求要么用路径中的某一级来传,要么用路径后面加?keyword=xxx
    public String search(String keyword, Page page, Model model) {
        // 搜索帖子
        SearchPage<DiscussPost> searchResult = elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit()); // 自己封装的Page对象,从1开始
        // 聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (searchResult != null) {
            for (SearchHit<DiscussPost> searchHitPost : searchResult) {
                DiscussPost tempPost = searchHitPost.getContent();
                // 设置高亮内容
                Map<String, List<String>> highlightFields = searchHitPost.getHighlightFields();
                List<String> highlightTitle = highlightFields.get("title");
                if (highlightTitle != null) {
                    tempPost.setTitle(highlightTitle.get(0));
                }
                List<String> highlightContent = highlightFields.get("content");
                if (highlightContent != null)
                    tempPost.setContent(highlightContent.get(0));
                Map<String, Object> map = new HashMap<>();
                // 帖子
                map.put("post", tempPost);
                // 作者
                map.put("user", userService.findUserById(tempPost.getUserId()));
                // 点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(EntityTypes.ENTITY_TYPE_POST, tempPost.getId()));
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);
        // 分页信息
        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchResult == null ? 0 : (int)searchResult.getTotalElements());
        return "/site/search";
    }
}