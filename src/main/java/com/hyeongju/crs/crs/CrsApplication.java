package com.hyeongju.crs.crs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class CrsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrsApplication.class, args);
	}

}
