package com.nowcoder.community.config;

import com.nowcoder.community.constant.AuthorityTypes;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers(
                "/resources/**"
        ); // 忽略静态资源的访问，以提高性能
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权
        http.authorizeRequests()
                .antMatchers(
//                        "/user/setting", // 用户设置
//                        "/user/upload", // 上传头像
//                        "/discuss/add", // 发帖子
//                        "/comment/add/**", // 发评论,下级是帖子ID
//                        "/letter/**",  // 私信
//                        "/notice/**", // 通知
//                        "/like", // 点赞
//                        "/follow", // 关注
//                        "/unfollow" // 取关
                )
                .hasAnyAuthority( // 登录后有任一如下权限都可以访问
                        AuthorityTypes.AUTHORITY_USER,
                        AuthorityTypes.AUTHORITY_ADMIN,
                        AuthorityTypes.AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        AuthorityTypes.AUTHORITY_MODERATOR // 版主可以置顶加精帖子
                )
                .antMatchers(
                        "/discuss/delete"
                )
                .hasAnyAuthority(
                        AuthorityTypes.AUTHORITY_ADMIN // 管理员才能删除讨论贴
                )
                .anyRequest().permitAll() // 对上面的请求路径之外的所有请求都放行;
                .and().csrf().disable(); // 关闭CSRF防护

        // 权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    // 没有登录时的处理
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with"); // x代表xml,虽然现在被JSON代替了
                        if ("XMLHttpRequest".equals(xRequestedWith)) { // 一个异步请求,比如点赞、关注等
                            response.setContentType("application/plain;charset=utf-8"); // 返回的数据类型
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你还没有登录哦!")); // 返回普通字符串,但要是JSON格式方便前端解析
                        } else { // 对普通的请求
                            response.sendRedirect(request.getContextPath() + "/login"); // 返回登录页面
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    // 已经登陆但权限不足
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) { // 如发布帖子
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限!"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                }); // 给一个错误提示的界面；不太合适，普通请求期待返回的是一个页面，跳转过去就行；而异步请求则需要返回JSON

        // Security底层默认会拦截/logout请求,进行退出处理.
        // 由于Security底层是用Filter实现的,请求到DispatcherServlet之前就被拦截了,在Controller之前,我们自己写的logout就不会执行。
        // 覆盖它默认的逻辑,才能执行我们自己的退出代码.
        http.logout().logoutUrl("/securitylogout");
    }

}
