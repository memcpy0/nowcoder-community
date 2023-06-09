package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.constant.EntityTypes;
import com.nowcoder.community.entity.HostHolder;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath; // 上传图片存入的文件夹

    @Value("${community.path.domain}")
    private String domain; // 域名

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

//    @LoginRequired
//    @RequestMapping(path = "/setting", method = RequestMethod.GET)
//    public String getSettingPage() {
//        return "/site/setting";
//    }
    /**
     * 返回设置界面
     * @return
     */
    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        // 上传文件名称
        String fileName = CommunityUtil.generateUUID(); // 随机生成上传文件名
        // 设置响应信息，看到响应信息知道上传成功
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0)); // 异步提交，需要JSON消息
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        model.addAttribute("uploadToken", uploadToken); // 在表单中配置凭证信息，才能从客户端直接上传
        model.addAttribute("fileName", fileName);

        return "/site/setting";
    }

    /**
     * 更新当前用户的头像的路径(web访问路径)
     * @param fileName
     * @return
     */
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空!");
        }

        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);

        return CommunityUtil.getJSONString(0);
    }

    /**
     * 上传头像文件(废弃)
     * @param headerImage
     * @param model
     * @return
     */
    @Deprecated
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "/site/setting"; // 返回设置界面
        }

        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确!");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try { // 存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        // 更新当前用户的头像的路径(web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName; // 生成外部链接存入数据库用户表
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }


    /**
     * 访问头像图片(废弃)
     * @param fileName
     * @param response
     */
    @Deprecated
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);
        try (
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }
    }

    /**
     * 更新密码
     * @param oldPassword
     * @param newPassword
     * @param model
     * @return
     */
    @LoginRequired
    @RequestMapping(path = "/password", method = RequestMethod.POST)
    public String uploadPassword(String oldPassword, String newPassword, Model model) {
        model.addAttribute("oldPassword", oldPassword);
        model.addAttribute("newPassword", newPassword);
        if (StringUtils.isBlank(oldPassword)) {
            model.addAttribute("oldPasswordError", "需要输入旧密码!");
            return "/site/setting"; // 返回设置界面
        }
        if (StringUtils.isBlank(newPassword)) {
            model.addAttribute("newPasswordError", "需要输入新密码!");
            return "/site/setting"; // 返回设置界面
        }
        User user = hostHolder.getUser();
        // 验证密码
        String oldPasswordMd5 = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!user.getPassword().equals(oldPasswordMd5)) {
            model.addAttribute("oldPasswordError", "旧密码不正确!");
            model.addAttribute("oldPassword", null); // 需要重新填写旧密码
            return "/site/setting"; // 返回设置界面
        }

        if (oldPassword.equals(newPassword)) {
            model.addAttribute("newPasswordError", "旧密码与新密码不能相同!");
            model.addAttribute("newPassword", null); // 需要重新填写新密码
            return "/site/setting"; // 返回设置界面
        }
        String newPasswordMd5 = CommunityUtil.md5(newPassword + user.getSalt());
        userService.updatePassword(user.getId(), newPasswordMd5); // 更新为新密码
        return "redirect:/index"; // 重定向到首页
    }

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;
    /**
     * 查看个人主页,可看当前用户的主页或任意用户的主页
     */
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        // 用户
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        // 关注数量
        int followeeCount = (int) followService.findFolloweeCount(userId, EntityTypes.ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(EntityTypes.ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), EntityTypes.ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }
}
