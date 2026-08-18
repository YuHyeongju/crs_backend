package com.hyeongju.crs.crs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// 스프링 부트 애플리케이션 진입점. @EnableJpaAuditing으로 엔티티의 생성/수정 시각 자동 기록 기능을 활성화
@EnableJpaAuditing
@SpringBootApplication
public class CrsApplication {

	public static void main(String[] args) {
		// 스프링 부트 앱 실행 진입점
		SpringApplication.run(CrsApplication.class, args);
	}

}
