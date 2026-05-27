package com.example.demo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Application context load test.
 * Disabled by default — requires running MySQL and Redis.
 * Enable manually when integration testing with real infrastructure.
 */
@SpringBootTest(properties = "spring.flyway.enabled=false")
@Disabled("Requires MySQL and Redis infrastructure — enable for integration testing")
class Demo2ApplicationTests {

	@Test
	void contextLoads() {
	}

}
