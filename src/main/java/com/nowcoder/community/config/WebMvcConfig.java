package com.nowcoder.community.config;

import com.nowcoder.community.interceptor.LoginRequiredInterceptor;
import com.nowcoder.community.interceptor.LoginTicketInterceptor;
import com.nowcoder.community.interceptor.MessageInterceptor;
import com.nowcoder.community.interceptor.StatisticsInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;

    @Autowired
    private LoginRequiredInterceptor loginRequiredInterceptor;

    @Autowired
    private MessageInterceptor messageInterceptor;

    @Autowired
    private StatisticsInterceptor statisticsInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginTicketInterceptor) // 在这里还是拦截一切请求
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg"); // 不拦截静态资源 /**指的是/static目录下的所有文件夹
//        registry.addInterceptor(loginRequiredInterceptor)
//            .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg"); // 放行对静态资源的访问,要拦截并处理的动态资源加注解就行
        registry.addInterceptor(messageInterceptor) // 查询未读消息数目
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg"); // 放行对静态资源的访问,要拦截并处理的动态资源加注解就行
        registry.addInterceptor(statisticsInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
    }
}