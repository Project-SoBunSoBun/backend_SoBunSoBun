package com.sobunsobun.backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;


@Slf4j

@EnableMongoAuditing
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		log.info("==========================================================");
		log.info("소분소분 백엔드 서버 시작 중...");
		log.info("Log4j2 로깅 시스템 활성화");
		log.info("==========================================================");

		SpringApplication.run(BackendApplication.class, args);

		log.info("==========================================================");
		log.info("소분소분 백엔드 서버가 성공적으로 시작되었습니다!");
		log.info("==========================================================");
	}

}
