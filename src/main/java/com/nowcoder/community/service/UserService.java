package com.nowcoder.community.service;

import com.nowcoder.community.constant.ActivationStatus;
import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public User findUserById(int id) {
        User user = getCache(id); // 1. 优先从缓存中取用户信息
        if (user == null) user = initCache(id); // 2. 否则从数据库中取用户信息，并初始化缓存
        return user;
//        return userMapper.selectById(id);
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
            userMapper.updateStatus(userId, 1); // 更新激活状态
            clearCache(userId); // 修改了用户信息
            return ActivationStatus.ACTIVATION_SUCCESS;
        } else {
            return ActivationStatus.ACTIVATION_FAILURE;
        }
    }

    @Autowired
    private LoginTicketMapper loginTicketMapper;
    /**
     * 登录，生成登录凭证，存入数据库
     * @param username
     * @param password
     * @param expiredSeconds
     * @return
     */
//    public Map<String, Object> login(String username, String password, int expiredSeconds) {
//        Map<String, Object> map = new HashMap<>();
//        // 空值处理
//        if (StringUtils.isBlank(username)) {
//            map.put("usernameMsg", "账号不能为空！");
//            return map;
//        }
//        if (StringUtils.isBlank(password)) {
//            map.put("passwordMsg", "密码不能为空！");
//            return map;
//        }
//        // 验证账号
//        User user = userMapper.selectByName(username);
//        if (user == null) {
//            map.put("usernameMsg", "该账号不存在！");
//            return map;
//        }
//        // 验证状态
//        if (user.getStatus() == 0) {
//            map.put("usernameMsg", "该账号未激活！");
//            return map;
//        }
//        // 验证密码
//        password = CommunityUtil.md5(password + user.getSalt());
//        if (!user.getPassword().equals(password)) {
//            map.put("passwordMsg", "密码不正确");
//            return map;
//        }
//        // 生成登录凭证
//        LoginTicket loginTicket = new LoginTicket();
//        loginTicket.setUserId(user.getId());
//        loginTicket.setTicket(CommunityUtil.generateUUID());
//        loginTicket.setStatus(0);
//        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
//        loginTicketMapper.insertLoginTicket(loginTicket);
//        map.put("ticket", loginTicket.getTicket());
//        return map;
//    }
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();
        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }
        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活！");
            return map;
        }
        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确");
            return map;
        }
        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        String redisKey = RedisKeyUtil. getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    /**
     * 登出，将登录凭证修改为失效状态
     * @param ticket
     */
//    public void logout(String ticket) {
//        loginTicketMapper.updateStatus(ticket, 1); // 无效
//    }
    public void logout(String ticket) {
        // 退出时标记为删除
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket)redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1); // 删除
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }

    /**
     * 根据ticket查询Redis中的用户凭证
     * @param ticket
     * @return
     */
//    public LoginTicket findLoginTicket(String ticket) {
//        return loginTicketMapper.selectByTicke(ticket);
//    }
    public LoginTicket findLoginTicket(String ticket) {
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    // 1. 优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2.取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS); // 设置缓存时间
        return user;
    }

    // 3. 数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    /**
     * 更新头像文件URL
     * @param userId
     * @param headerUrl
     * @return
     */
    public int updateHeader(int userId, String headerUrl) {
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId); // 先清理缓存
        return rows;
    }

    /**
     * 更新用户密码
     * @param userId
     * @param password
     * @return
     */
    public int updatePassword(int userId, String password) {
        int rows = userMapper.updatePassword(userId, password);
        clearCache(userId); // 先清理缓存
        return rows;
    }
}
