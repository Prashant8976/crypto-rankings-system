package com.example.crypto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CustomWebConfig {

	@Bean
	public RestTemplate resttemplate() {
		return new RestTemplate();
	}
}
