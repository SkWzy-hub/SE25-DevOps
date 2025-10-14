package com.SE2025BackEnd_16.project;

import com.SE2025BackEnd_16.project.config.DashScopeConfig;
import com.SE2025BackEnd_16.project.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DashScopeConfig.class})
public class ProjectApplication implements CommandLineRunner {

	@Autowired
	private OrderService orderService;

	public static void main(String[] args) {
		SpringApplication.run(ProjectApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// 启动时预热订单缓存
		orderService.preheatOrderCache();
	}
}
