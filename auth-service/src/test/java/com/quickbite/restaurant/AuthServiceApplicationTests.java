package com.quickbite.restaurant;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Fails if MySQL/Redis aren't running locally. Run inside Docker instead.")
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
