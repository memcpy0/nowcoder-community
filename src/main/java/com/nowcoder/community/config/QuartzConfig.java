package com.nowcoder.community.config;

import com.nowcoder.community.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

// 配置 -> 第一次加载时写到数据库中 -> 之后调用时从数据库查询
@Configuration
public class QuartzConfig {
    // 实现FactoryBean接口,目的在于简化Bean的实例化过程
    // 1. 通过FactoryBean封装了Bean的实例化过程
    // 2. 将FactoryBean装配到Spring容器中
    // 3. 通过FactoryBean注入给其他Bean
    // 4. 该Bean得到的是FactoryBean管理的对象实例
    /*
    @Bean
    public JobDetailFactoryBean alphaJobDetail() { // 配JobDetail
        // JobDetailFactoryBean底层封装了JobDetail详细实例化的过程
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(AlphaJob.class);
        factoryBean.setName("alphaJob");
        factoryBean.setGroup("alphaJobGroup"); // 多个任务可以同属于一组
        factoryBean.setDurability(true); // 任务是否持久保存，不用删除
        factoryBean.setRequestsRecovery(true); // 任务是否可恢复，应用恢复后是否回复任务
        return factoryBean;
    }
    // 配置Trigger: SimpleTriggerFactoryBean, CronTriggerFactoryBean
    @Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail) { // 配置Trigger,依赖于JobDetail
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(alphaJobDetail);
        factoryBean.setName("alphaTrigger");
        factoryBean.setGroup("alphaTriggerGroup");
        factoryBean.setRepeatInterval(3000); // 频率，多次时间执行任务
        factoryBean.setJobDataMap(new JobDataMap()); // 存Job的状态
        return factoryBean;
    }
    */

    // 刷新帖子分数任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("communityJobGroup");
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
//        factoryBean.setRepeatInterval(1000 * 60 * 5);
        factoryBean.setRepeatInterval(1000 * 60 * 1);
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }

}
