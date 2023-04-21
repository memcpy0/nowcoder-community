package com.nowcoder.community.service;

import com.nowcoder.community.constant.ActivationStatus;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public User findUserById(int id) {
        return userMapper.selectById(id);
    }


    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine;
    @Value("${community.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 注册账号，生成账号ID、密码加密存储、随机头像
     * 随机生成激活码，使用Context、activation.html和templateEngine生成激活码页面，
     * 利用mailClient发送激活码页面和激活链接给用户
     * @param user
     * @return
     */
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();
        // 空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }
        // 验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }
        // 验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }
        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0); // 默认都是普通用户
        user.setStatus(0); // 没有激活
        user.setActivationCode(CommunityUtil.generateUUID()); //设置激活码
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000))); // 设置随机头像
        userMapper.insertUser(user); // 添加到数据库, Mybatis/Mybatis—plus自动生成ID,并回填到对象

        // 发送激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content); // 发送邮件
        return map; // map为空就说明没有问题; 否则返回之前的错误信息
    }

    /**
     * 激活账号，检查重复激活和错误激活码
     * @param userId
     * @param code
     * @return
     */
    public ActivationStatus activate(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) { // 已激活
            return ActivationStatus.ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1); //更新激活状态
            return ActivationStatus.ACTIVATION_SUCCESS;
        } else {
            return ActivationStatus.ACTIVATION_FAILURE;
        }
    }
}
