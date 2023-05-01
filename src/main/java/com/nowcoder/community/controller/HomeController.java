package com.nowcoder.community.controller;

import com.nowcoder.community.constant.EntityTypes;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    /**
     * 返回社区首页，查询所有帖子
     * @param model
     * @param page
     * @return
     */
    @RequestMapping(path = "/index", method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page,
                               @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        // 方法调用栈,SpringMVC会自动实例化Model和Page,并将Page注入Model.
        // 所以,在thymeleaf中可以直接访问Page对象中的数据
        page.setRows(discussPostService.findDiscussPostRows(0)); // 查询所有帖子,page中根据总数,每页帖子数,计算页数
//        page.setPath("/index"); // 设置模板的路径,用于拼接分页的URL
        page.setPath("/index?orderMode=" + orderMode); // 最新+最热

        // 根据用户ID查到的帖子列表数据不完整
        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(), orderMode); // 当前页的起始记录行数,每页数
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId()); // 还需要'连表'查询用户数据(用户名,用户头像等)
                map.put("user", user); //
                long likeCount = likeService.findEntityLikeCount(EntityTypes.ENTITY_TYPE_POST, post.getId()); // 赞的数量
                map.put("likeCount", likeCount);
                discussPosts.add(map);
            }
        }
        model.addAttribute("orderMode", orderMode);
        model.addAttribute("discussPosts", discussPosts);
        return "/index";
    }

    /**
     * 服务器错误页面
     * @return
     */
    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }

    /**
     * 权限不足时的页面
     * @return
     */
    @RequestMapping(path = "/denied", method = RequestMethod.GET)
    public String getDeniedPage() {
        return "/error/404";
    }
}
