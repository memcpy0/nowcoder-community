package com.nowcoder.community.controller;

import com.nowcoder.community.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
public class StatisticsController {
    @Autowired
    private StatisticsService statisticsService;

    /**
     * 登录且需要是管理员才能查看的统计界面
     */
    @RequestMapping(path = "/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage() {
        return "/site/admin/data";
    }

    // 统计页面上的网站UV  
    @RequestMapping(path = "/data/uv", method = RequestMethod.POST)
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long uv = statisticsService.calculateUV(start, end);
        model.addAttribute("uvResult", uv);
        model.addAttribute("uvStartDate", start);
        model.addAttribute("uvEndDate", end);
        // return "/site/admin/data";  
        return "forward:/data"; // 只能处理一部分，还需要其他同级方法继续处理; 转发是在一个请求内完成的，转给其他方法时请求类型不变  
    }

    // 统计页面上的网站UV  
    @RequestMapping(path = "/data/dau", method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                         @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long dau = statisticsService.calculateDAU(start, end);
        model.addAttribute("dauResult", dau);
        model.addAttribute("dauStartDate", start);
        model.addAttribute("dauEndDate", end);
        // return "/site/admin/data";  
        return "forward:/data"; // 只能处理一部分，还需要其他同级方法继续处理; 转发是在一个请求内完成的，转给其他方法时请求类型不变  
    }
}