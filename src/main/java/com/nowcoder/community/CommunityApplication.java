package com.nowcoder.community;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@Slf4j
@SpringBootApplication
public class CommunityApplication {

	@PostConstruct
	public void init() {
		// 解决netty启动冲突问题
		// see Netty4Utils.setAvailableProcessors()
		System.setProperty("os.set.netty.runtime.available.processors", "false");
	}
	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
		log.info("项目启动成功");
	}

}