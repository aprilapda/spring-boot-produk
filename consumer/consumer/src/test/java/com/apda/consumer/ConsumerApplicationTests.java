package com.apda.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ConsumerApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		org.assertj.core.api.Assertions.assertThat(applicationContext.containsBean("emailConsumer")).isFalse();
		org.assertj.core.api.Assertions.assertThat(applicationContext.containsBean("emailOrderCreatedConsumer"))
				.isFalse();
	}

}
