package com.nowcoder.community.interceptor;

import com.nowcoder.community.entity.HostHolder;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class StatisticsInterceptor implements HandlerInterceptor {
    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 统计UV
        String ip = request.getRemoteHost();
        statisticsService.recordUV(ip); // 不管是否登录都要记录
        // 统计DAU
        User user = hostHolder.getUser();
        if (user != null) { // 用户登录才统计
            statisticsService.recordDAU(user.getId());
        }
        return true;
    }
}
