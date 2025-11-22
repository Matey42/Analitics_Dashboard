package com.example.analytics_dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAsync
public class AnalyticsDashboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnalyticsDashboardApplication.class, args);
	}

}
