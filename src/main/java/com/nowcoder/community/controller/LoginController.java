package com.nowcoder.community.controller;

import com.nowcoder.community.constant.ActivationStatus;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Controller
public class LoginController {
    @Autowired
    private UserService userService;

    /**
     * 注册功能：通过首部-注册跳转到注册界面
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

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }
}