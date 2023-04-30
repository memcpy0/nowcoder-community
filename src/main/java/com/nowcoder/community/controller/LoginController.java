package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.constant.ActivationStatus;
import com.nowcoder.community.constant.ExpiredTime;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController {
    @Autowired
    private UserService userService;

    /**
     * 注册功能：通过首部-注册链接跳转到注册界面
     */
    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage(Model model) { model.addAttribute("user", new User()); return "/site/register"; }

    /**
     * 注册功能：处理注册填写的表单
     */
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活");
            model.addAttribute("target", "/index"); // 中间界面中含有跳转到首页的链接
            return "/site/operate-result"; // 跳转到中间界面
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register"; // 回到注册界面
        }
    }

    /**
     * 处理激活邮件中的链接发来的请求，
     * 校验激活码，如果账号未激活则激活，如果激活过了/激活码错了则提示；都会跳转到中间界面
     * @param model
     * @param userId
     * @param code
     * @return
     */
    // http://localhost:8080/community/activation/101/code
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activate(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        ActivationStatus result = userService.activate(userId, code);
        if (result == ActivationStatus.ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功，您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login"); // 成功激活后,中间页面的链接,跳转到登录界面
        } else if (result == ActivationStatus.ACTIVATION_REPEAT) { // 重复激活
            model.addAttribute("msg", "无效操作，该账号已经激活过了！");
            model.addAttribute("target", "/index"); // 中间页面的链接,跳转到首页
        } else {
            model.addAttribute("msg", "激活失败，您提供的激活码不正确！");
            model.addAttribute("target", "/index"); // 中间页面的链接,跳转到首页
        }
        return "/site/operate-result";
    }

    /**
     * 登录功能：通过首部-登录链接跳转到登录界面
     * @return
     */
    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    @Autowired
    private Producer kaptchaProducer;
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    /**
     * 处理验证码图片请求，返回一个验证码图片，将验证码存入Session
     * @param response
     * @param session
     */
//    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
//    public void getKaptcha(HttpServletResponse response, HttpSession session) {
//        // 生成验证码
//        String text = kaptchaProducer.createText();
//        BufferedImage image = kaptchaProducer.createImage(text);
//        // 将验证码存入session
//        session.setAttribute("kaptcha", text);
//
//        // 将图片输出给浏览器
//        response.setContentType("image/png");
//        try {
//            OutputStream os = response.getOutputStream();
//            ImageIO.write(image, "png", os);
//        } catch (IOException e) {
//            logger.error("响应验证码失败", e.getMessage());
//        }
//    }
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 生成验证码
     * @param response
     * @param session
     */
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
        // 将验证码存入session
        // session.setAttribute("kaptcha", text);
        // 类似Session的JSessionID
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        // 将键:验证码存入redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS); // 设置验证码过期时间

        // 将图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败", e.getMessage());
        }
    }

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 登录接口，先判断验证码是否正确，再进行验证，如果业务层生成了登录凭证，则设置Cookie
     */
//    @RequestMapping(path = "/login", method = RequestMethod.POST) // 页面上可以勾上记住我
//    public String login(String username, String password, String code, boolean rememberme, Model model, HttpSession session, HttpServletResponse response) {
//        // 检查验证码
//        String kaptcha = (String) session.getAttribute("kaptcha");
//        if (StringUtils.isBlank(code) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
//            model.addAttribute("codeMsg", "验证码不正确！");
//            return "/site/login";
//        }
//        // 检查账号密码
//        int expiredSeconds = rememberme ? ExpiredTime.REMEMBERME_EXPIRED_SECONDS : ExpiredTime.DEFAULT_EXPIRED_SECONDS;
//        // 调用业务层登录方法
//        Map<String, Object> map = userService.login(username, password, expiredSeconds);
//
//        if (map.containsKey("ticket")) {
//            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
//            cookie.setPath(contextPath); // Cookie在整个项目都有效
//            cookie.setMaxAge(expiredSeconds); // 自己做Session,返回一个Cookie含有Ticket,并设置Cookie的过期时间(从而设置了Session的过期时间,因为Cookie失效后就找不到Session了)
//            response.addCookie(cookie); // 发送Cookie给客户端
//            return "redirect:/index"; // 重定向到首页
//        } else {
//            model.addAttribute("usernameMsg", map.get("usernameMsg"));
//            model.addAttribute("passwordMsg", map.get("passwordMsg"));
//            return "/site/login";
//        }
//    }
    @RequestMapping(path = "/login", method = RequestMethod.POST) // 页面上可以勾上记住我
    public String login(String username, String password, String code, boolean rememberme, Model model, /*HttpSession session,*/ HttpServletResponse response, @CookieValue("kaptchaOwner") String kaptchaOwner) {
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner); // 待登录用户的键
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey); // 找到Redis中存储的待登录用户的验证码
        }
        if (StringUtils.isBlank(kaptcha)) {
            model.addAttribute("codeMsg", "验证码已过期");
            return "/site/login";
        } else if (StringUtils.isBlank(code)) {
            model.addAttribute("codeMsg", "输入验证码为空");
            return "/site/login";
        } else if (!kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确");
            return "/site/login";
        }

        // 检查账号密码
        int expiredSeconds = rememberme ? ExpiredTime.REMEMBERME_EXPIRED_SECONDS : ExpiredTime.DEFAULT_EXPIRED_SECONDS;
        // 调用业务层登录方法
        Map<String, Object> map = userService.login(username, password, expiredSeconds);

        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath); // Cookie在整个项目都有效
            cookie.setMaxAge(expiredSeconds); // 自己做Session,返回一个Cookie含有Ticket,并设置Cookie的过期时间(从而设置了Session的过期时间,因为Cookie失效后就找不到Session了)
            response.addCookie(cookie); // 发送Cookie给客户端
            return "redirect:/index"; // 重定向到首页
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        SecurityContextHolder.clearContext(); // 退出时清理信息
        return "redirect:/login"; // 重定向默认是GET请求
    }
}