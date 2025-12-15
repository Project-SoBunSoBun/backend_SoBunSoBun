package com.sobunsobun.backend;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		properties = {
				"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
						"org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
						"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
		}
)
class BackendApplicationTests {

//	@Test
//	void contextLoads() {
//	}

}
